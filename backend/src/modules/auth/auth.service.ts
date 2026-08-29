import {
  Injectable,
  ConflictException,
  UnauthorizedException,
  ForbiddenException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import { PrismaService } from '../../database/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { UserRole, UserStatus, DonorType, OrgType } from '@prisma/client';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private configService: ConfigService,
  ) {}

  async register(dto: RegisterDto) {
    // 1. Check duplicate email or phone
    const existing = await this.prisma.user.findFirst({
      where: {
        OR: [{ email: dto.email.toLowerCase() }, { phone: dto.phone }],
      },
    });

    if (existing) {
      throw new ConflictException({
        error: 'ERR-AUTH-001',
        message: 'An account with this email or phone number already exists.',
      });
    }

    // 2. Hash password
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(dto.password, saltRounds);

    // 3. Admin cannot self-register
    if (dto.role === UserRole.Admin) {
      throw new ForbiddenException({
        error: 'ERR-AUTH-003',
        message: 'Admin accounts cannot be self-registered.',
      });
    }

    // 4. Create user and profile in transaction
    const user = await this.prisma.$transaction(async (tx) => {
      const newUser = await tx.user.create({
        data: {
          email: dto.email.toLowerCase(),
          phone: dto.phone,
          passwordHash,
          role: dto.role,
          status:
            dto.role === UserRole.Receiver_Org
              ? UserStatus.Pending_Verification
              : UserStatus.Active,
        },
      });

      if (dto.role === UserRole.Donor) {
        await tx.donorProfile.create({
          data: {
            userId: newUser.id,
            type: DonorType.Restaurant, // Default type until profile update
            entityName: dto.name,
            addressText: 'Default Address',
            latitude: 12.9716,
            longitude: 77.5946,
            policyAccepted: false,
          },
        });
      } else if (dto.role === UserRole.Receiver_Org) {
        await tx.receiverOrganization.create({
          data: {
            userId: newUser.id,
            type: OrgType.NGO,
            orgName: dto.name,
            registrationNumber: 'PENDING_DOCS',
            addressText: 'Default Address',
            serviceLatitude: 12.9716,
            serviceLongitude: 77.5946,
            beneficiaryCapacity: 50,
          },
        });
      } else if (dto.role === UserRole.Receiver_Vol) {
        await tx.volunteerProfile.create({
          data: {
            userId: newUser.id,
            fullName: dto.name,
            profession: 'Volunteer',
            aboutBio: 'Eager to help redistribute surplus food.',
            serviceLatitude: 12.9716,
            serviceLongitude: 77.5946,
          },
        });
      }

      return newUser;
    });

    return {
      userId: user.id,
      email: user.email,
      phone: user.phone,
      role: user.role,
      status: user.status,
      createdAt: user.createdAt,
    };
  }

  async login(dto: LoginDto) {
    const identifier = dto.identifier.toLowerCase();
    const user = await this.prisma.user.findFirst({
      where: {
        OR: [{ email: identifier }, { phone: dto.identifier }],
      },
    });

    if (!user) {
      throw new UnauthorizedException({
        error: 'ERR-AUTH-002',
        message: 'Invalid email/phone or password.',
      });
    }

    if (user.status === UserStatus.Suspended) {
      throw new ForbiddenException({
        error: 'ERR-AUTH-003',
        message: 'Your account has been suspended. Please contact support.',
      });
    }

    const passwordMatches = await bcrypt.compare(dto.password, user.passwordHash);
    if (!passwordMatches) {
      throw new UnauthorizedException({
        error: 'ERR-AUTH-002',
        message: 'Invalid email/phone or password.',
      });
    }

    // Generate tokens
    const tokens = await this.generateTokens(user);

    return {
      ...tokens,
      user: {
        id: user.id,
        email: user.email,
        phone: user.phone,
        role: user.role,
        status: user.status,
      },
    };
  }

  async refreshTokens(rawRefreshToken: string) {
    const tokenHash = crypto.createHash('sha256').update(rawRefreshToken).digest('hex');

    const storedToken = await this.prisma.refreshToken.findFirst({
      where: {
        tokenHash,
        revoked: false,
        expiresAt: {
          gt: new Date(),
        },
      },
      include: {
        user: true,
      },
    });

    if (!storedToken || !storedToken.user) {
      throw new UnauthorizedException({
        error: 'ERR-AUTH-002',
        message: 'Invalid or expired refresh token.',
      });
    }

    // Revoke used refresh token (Token rotation)
    await this.prisma.refreshToken.update({
      where: { id: storedToken.id },
      data: { revoked: true },
    });

    return this.generateTokens(storedToken.user);
  }

  async logout(userId: string) {
    await this.prisma.refreshToken.updateMany({
      where: { userId, revoked: false },
      data: { revoked: true },
    });

    return { message: 'Logged out successfully.' };
  }

  private async generateTokens(user: { id: string; email: string; role: string; status: string }) {
    const payload = {
      sub: user.id,
      email: user.email,
      role: user.role,
      status: user.status,
    };

    const accessToken = this.jwtService.sign(payload, {
      secret: this.configService.get<string>('JWT_ACCESS_SECRET') || 'mealshare_jwt_secret',
      expiresIn: '900s', // 15 minutes
    });

    const rawRefreshToken = crypto.randomBytes(40).toString('hex');
    const refreshHash = crypto.createHash('sha256').update(rawRefreshToken).digest('hex');
    const refreshExpiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7 days

    await this.prisma.refreshToken.create({
      data: {
        userId: user.id,
        tokenHash: refreshHash,
        expiresAt: refreshExpiresAt,
      },
    });

    return {
      accessToken,
      refreshToken: rawRefreshToken,
      tokenType: 'Bearer',
      expiresIn: 900,
    };
  }
}

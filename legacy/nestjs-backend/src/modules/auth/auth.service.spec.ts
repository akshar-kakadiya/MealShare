import { Test, TestingModule } from '@nestjs/testing';
import { AuthService } from './auth.service';
import { PrismaService } from '../../database/prisma.service';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { ConflictException, UnauthorizedException, ForbiddenException } from '@nestjs/common';
import { UserRole, UserStatus } from '@prisma/client';
import * as bcrypt from 'bcrypt';

describe('AuthService', () => {
  let service: AuthService;
  let prisma: PrismaService;
  let jwt: JwtService;

  const mockPrisma = {
    user: {
      findFirst: jest.fn(),
      findUnique: jest.fn(),
      create: jest.fn(),
    },
    donorProfile: {
      create: jest.fn(),
    },
    receiverOrganization: {
      create: jest.fn(),
    },
    volunteerProfile: {
      create: jest.fn(),
    },
    refreshToken: {
      create: jest.fn(),
      findFirst: jest.fn(),
      update: jest.fn(),
      updateMany: jest.fn(),
    },
    $transaction: jest.fn((callback) => callback(mockPrisma)),
  };

  const mockJwt = {
    sign: jest.fn().mockReturnValue('mocked.jwt.token'),
  };

  const mockConfig = {
    get: jest.fn((key: string) => {
      if (key === 'JWT_ACCESS_SECRET') return 'test_secret';
      return null;
    }),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: JwtService, useValue: mockJwt },
        { provide: ConfigService, useValue: mockConfig },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    prisma = module.get<PrismaService>(PrismaService);
    jwt = module.get<JwtService>(JwtService);
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('register', () => {
    const registerDto = {
      email: 'donor@mealshare.org',
      phone: '+919876543210',
      password: 'SecurePassword123!',
      name: 'Grand Banquet',
      role: UserRole.Donor,
    };

    it('should successfully register a Donor and create donor profile', async () => {
      mockPrisma.user.findFirst.mockResolvedValue(null);
      mockPrisma.user.create.mockResolvedValue({
        id: 'user-uuid-1',
        email: 'donor@mealshare.org',
        phone: '+919876543210',
        role: UserRole.Donor,
        status: UserStatus.Active,
        createdAt: new Date(),
      });
      mockPrisma.donorProfile.create.mockResolvedValue({ id: 'donor-uuid-1' });

      const result = await service.register(registerDto);

      expect(mockPrisma.user.findFirst).toHaveBeenCalledWith({
        where: {
          OR: [{ email: 'donor@mealshare.org' }, { phone: '+919876543210' }],
        },
      });
      expect(mockPrisma.user.create).toHaveBeenCalled();
      expect(mockPrisma.donorProfile.create).toHaveBeenCalled();
      expect(result.userId).toEqual('user-uuid-1');
      expect(result.role).toEqual(UserRole.Donor);
    });

    it('should throw ConflictException (ERR-AUTH-001) if email or phone exists', async () => {
      mockPrisma.user.findFirst.mockResolvedValue({ id: 'existing-user-uuid' });

      await expect(service.register(registerDto)).rejects.toThrow(ConflictException);
    });

    it('should throw ForbiddenException if trying to register as Admin', async () => {
      mockPrisma.user.findFirst.mockResolvedValue(null);

      await expect(
        service.register({ ...registerDto, role: UserRole.Admin }),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  describe('login', () => {
    const loginDto = {
      identifier: 'donor@mealshare.org',
      password: 'SecurePassword123!',
    };

    it('should login successfully and return access & refresh tokens', async () => {
      const passwordHash = await bcrypt.hash('SecurePassword123!', 10);
      mockPrisma.user.findFirst.mockResolvedValue({
        id: 'user-uuid-1',
        email: 'donor@mealshare.org',
        phone: '+919876543210',
        passwordHash,
        role: UserRole.Donor,
        status: UserStatus.Active,
      });
      mockPrisma.refreshToken.create.mockResolvedValue({ id: 'token-uuid-1' });

      const result = await service.login(loginDto);

      expect(result.accessToken).toBe('mocked.jwt.token');
      expect(result.refreshToken).toBeDefined();
      expect(result.user.email).toBe('donor@mealshare.org');
    });

    it('should throw UnauthorizedException if user not found', async () => {
      mockPrisma.user.findFirst.mockResolvedValue(null);

      await expect(service.login(loginDto)).rejects.toThrow(UnauthorizedException);
    });

    it('should throw UnauthorizedException if password incorrect', async () => {
      const passwordHash = await bcrypt.hash('DifferentPassword!', 10);
      mockPrisma.user.findFirst.mockResolvedValue({
        id: 'user-uuid-1',
        email: 'donor@mealshare.org',
        phone: '+919876543210',
        passwordHash,
        role: UserRole.Donor,
        status: UserStatus.Active,
      });

      await expect(service.login(loginDto)).rejects.toThrow(UnauthorizedException);
    });

    it('should throw ForbiddenException if account is suspended', async () => {
      mockPrisma.user.findFirst.mockResolvedValue({
        id: 'user-uuid-1',
        email: 'donor@mealshare.org',
        phone: '+919876543210',
        passwordHash: 'hash',
        role: UserRole.Donor,
        status: UserStatus.Suspended,
      });

      await expect(service.login(loginDto)).rejects.toThrow(ForbiddenException);
    });
  });
});

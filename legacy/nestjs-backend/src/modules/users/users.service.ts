import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async getMe(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        email: true,
        phone: true,
        role: true,
        status: true,
        createdAt: true,
        donorProfile: true,
        receiverOrg: {
          include: {
            verifications: {
              orderBy: { createdAt: 'desc' },
              take: 1,
            },
          },
        },
        volunteerProfile: {
          include: {
            certificates: true,
          },
        },
      },
    });

    if (!user) {
      throw new NotFoundException({
        error: 'ERR-USER-404',
        message: 'User profile not found.',
      });
    }

    return user;
  }
}

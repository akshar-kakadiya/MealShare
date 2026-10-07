import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';
import { UpdateVolunteerProfileDto } from './dto/update-volunteer-profile.dto';

@Injectable()
export class VolunteersService {
  constructor(private prisma: PrismaService) {}

  async getProfile(userId: string) {
    const profile = await this.prisma.volunteerProfile.findUnique({
      where: { userId },
      include: {
        certificates: {
          orderBy: { issuedAt: 'desc' },
        },
      },
    });

    if (!profile) {
      throw new NotFoundException({
        error: 'ERR-VOL-404',
        message: 'Volunteer profile not found.',
      });
    }

    return profile;
  }

  async updateProfile(userId: string, dto: UpdateVolunteerProfileDto) {
    const existing = await this.prisma.volunteerProfile.findUnique({
      where: { userId },
    });

    if (!existing) {
      throw new NotFoundException({
        error: 'ERR-VOL-404',
        message: 'Volunteer profile not found.',
      });
    }

    return this.prisma.volunteerProfile.update({
      where: { userId },
      data: {
        fullName: dto.fullName,
        profession: dto.profession,
        aboutBio: dto.aboutBio,
        serviceLatitude: dto.serviceLatitude,
        serviceLongitude: dto.serviceLongitude,
        serviceRadiusMeters: dto.serviceRadiusMeters,
      },
    });
  }

  async getPublicPortfolio(volunteerId: string) {
    const profile = await this.prisma.volunteerProfile.findFirst({
      where: {
        OR: [{ id: volunteerId }, { userId: volunteerId }],
      },
      include: {
        certificates: {
          select: {
            id: true,
            milestoneTier: true,
            completedDeliveriesCount: true,
            certificatePdfUrl: true,
            verificationToken: true,
            issuedAt: true,
          },
        },
      },
    });

    if (!profile) {
      throw new NotFoundException({
        error: 'ERR-VOL-404',
        message: 'Volunteer portfolio not found.',
      });
    }

    return {
      volunteerId: profile.id,
      fullName: profile.fullName,
      profession: profile.profession,
      aboutBio: profile.aboutBio,
      completedDeliveriesCount: profile.completedDeliveriesCount,
      totalMealsDelivered: profile.totalMealsDelivered,
      ratingAverage: profile.ratingAverage,
      certificates: profile.certificates,
    };
  }

  async getMyImpact(userId: string) {
    const profile = await this.getProfile(userId);
    return {
      completedDeliveriesCount: profile.completedDeliveriesCount,
      totalMealsDelivered: profile.totalMealsDelivered,
      ratingAverage: profile.ratingAverage,
      certificatesEarned: profile.certificates.length,
      impactScore: profile.totalMealsDelivered * 10 + profile.completedDeliveriesCount * 50,
    };
  }
}

import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';
import { UpdateDonorProfileDto } from './dto/update-donor-profile.dto';
import { PolicyAgreementDto } from './dto/policy-agreement.dto';

@Injectable()
export class DonorsService {
  constructor(private prisma: PrismaService) {}

  async getProfile(userId: string) {
    const profile = await this.prisma.donorProfile.findUnique({
      where: { userId },
    });

    if (!profile) {
      throw new NotFoundException({
        error: 'ERR-DONOR-404',
        message: 'Donor profile not found for this user.',
      });
    }

    return profile;
  }

  async updateProfile(userId: string, dto: UpdateDonorProfileDto) {
    const existing = await this.prisma.donorProfile.findUnique({
      where: { userId },
    });

    if (!existing) {
      throw new NotFoundException({
        error: 'ERR-DONOR-404',
        message: 'Donor profile not found.',
      });
    }

    return this.prisma.donorProfile.update({
      where: { userId },
      data: {
        type: dto.type,
        entityName: dto.entityName,
        addressText: dto.addressText,
        latitude: dto.latitude,
        longitude: dto.longitude,
      },
    });
  }

  async acceptPolicy(userId: string, dto: PolicyAgreementDto) {
    if (!dto.agreeToPolicy) {
      throw new BadRequestException({
        error: 'ERR-DON-003',
        message: 'You must explicitly agree to the Food Donation Policy to post surplus food.',
      });
    }

    const updated = await this.prisma.donorProfile.update({
      where: { userId },
      data: {
        policyAccepted: true,
        policyAcceptedAt: new Date(),
        policyVersion: dto.policyVersion,
      },
    });

    return {
      success: true,
      policyAccepted: updated.policyAccepted,
      policyAcceptedAt: updated.policyAcceptedAt,
      policyVersion: updated.policyVersion,
      message: 'Food Donation Policy agreement successfully recorded.',
    };
  }
}

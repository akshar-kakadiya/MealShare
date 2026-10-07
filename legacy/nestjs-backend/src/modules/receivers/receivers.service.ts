import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';
import { UpdateReceiverProfileDto } from './dto/update-receiver-profile.dto';
import { UploadVerificationDocDto } from './dto/upload-verification-doc.dto';
import { VerificationStatus } from '@prisma/client';

@Injectable()
export class ReceiversService {
  constructor(private prisma: PrismaService) {}

  async getProfile(userId: string) {
    const profile = await this.prisma.receiverOrganization.findUnique({
      where: { userId },
      include: {
        verifications: {
          orderBy: { createdAt: 'desc' },
        },
      },
    });

    if (!profile) {
      throw new NotFoundException({
        error: 'ERR-RECV-404',
        message: 'Receiver organization profile not found.',
      });
    }

    return profile;
  }

  async updateProfile(userId: string, dto: UpdateReceiverProfileDto) {
    const existing = await this.prisma.receiverOrganization.findUnique({
      where: { userId },
    });

    if (!existing) {
      throw new NotFoundException({
        error: 'ERR-RECV-404',
        message: 'Receiver organization profile not found.',
      });
    }

    return this.prisma.receiverOrganization.update({
      where: { userId },
      data: {
        type: dto.type,
        orgName: dto.orgName,
        registrationNumber: dto.registrationNumber,
        addressText: dto.addressText,
        serviceLatitude: dto.serviceLatitude,
        serviceLongitude: dto.serviceLongitude,
        serviceRadiusMeters: dto.serviceRadiusMeters,
        beneficiaryCapacity: dto.beneficiaryCapacity,
      },
    });
  }

  async submitVerificationDocs(userId: string, dto: UploadVerificationDocDto) {
    const org = await this.prisma.receiverOrganization.findUnique({
      where: { userId },
    });

    if (!org) {
      throw new NotFoundException({
        error: 'ERR-RECV-404',
        message: 'Receiver organization profile not found.',
      });
    }

    const verification = await this.prisma.$transaction(async (tx) => {
      const doc = await tx.organizationVerification.create({
        data: {
          orgId: org.id,
          documentType: dto.documentType,
          documentUrl: dto.documentUrl,
          status: VerificationStatus.Pending,
        },
      });

      await tx.receiverOrganization.update({
        where: { id: org.id },
        data: { verificationStatus: VerificationStatus.Pending },
      });

      return doc;
    });

    return {
      success: true,
      verificationId: verification.id,
      documentType: verification.documentType,
      status: verification.status,
      submittedAt: verification.createdAt,
      message: 'Verification document submitted successfully and enqueued for admin review.',
    };
  }
}

import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';
import { AdjudicateVerificationDto } from './dto/adjudicate-verification.dto';
import { ApplySanctionDto } from './dto/apply-sanction.dto';
import { UserStatus, VerificationStatus } from '@prisma/client';

@Injectable()
export class AdminService {
  constructor(private prisma: PrismaService) {}

  async listPendingVerifications() {
    return this.prisma.organizationVerification.findMany({
      where: { status: VerificationStatus.Pending },
      include: {
        organization: {
          include: {
            user: {
              select: {
                id: true,
                email: true,
                phone: true,
                status: true,
              },
            },
          },
        },
      },
      orderBy: { createdAt: 'asc' },
    });
  }

  async adjudicateVerification(
    verificationId: string,
    adminUserId: string,
    dto: AdjudicateVerificationDto,
  ) {
    const verification = await this.prisma.organizationVerification.findUnique({
      where: { id: verificationId },
      include: { organization: true },
    });

    if (!verification) {
      throw new NotFoundException({
        error: 'ERR-ADMIN-404',
        message: 'Verification request not found.',
      });
    }

    return this.prisma.$transaction(async (tx) => {
      const updatedVerification = await tx.organizationVerification.update({
        where: { id: verificationId },
        data: {
          status: dto.decision,
          reviewedById: adminUserId,
          adminNotes: dto.adminNotes,
          reviewedAt: new Date(),
        },
      });

      await tx.receiverOrganization.update({
        where: { id: verification.orgId },
        data: {
          verificationStatus: dto.decision,
        },
      });

      // If approved, update user status to Active
      if (dto.decision === VerificationStatus.Verified) {
        await tx.user.update({
          where: { id: verification.organization.userId },
          data: { status: UserStatus.Active },
        });
      }

      // Record in audit log
      await tx.auditLog.create({
        data: {
          actorUserId: adminUserId,
          actorRole: 'Admin',
          actionType: 'ADJUDICATE_ORGANIZATION_VERIFICATION',
          entityName: 'organization_verifications',
          entityId: verificationId,
          deltaState: {
            previousStatus: verification.status,
            newStatus: dto.decision,
            adminNotes: dto.adminNotes,
          },
        },
      });

      return updatedVerification;
    });
  }

  async applyUserSanction(targetUserId: string, adminUserId: string, dto: ApplySanctionDto) {
    const user = await this.prisma.user.findUnique({
      where: { id: targetUserId },
    });

    if (!user) {
      throw new NotFoundException({
        error: 'ERR-USER-404',
        message: 'Target user not found.',
      });
    }

    if (user.role === 'Admin') {
      throw new BadRequestException({
        error: 'ERR-ADMIN-400',
        message: 'Admin accounts cannot be sanctioned through standard endpoint.',
      });
    }

    return this.prisma.$transaction(async (tx) => {
      const updatedUser = await tx.user.update({
        where: { id: targetUserId },
        data: { status: dto.status },
      });

      // If suspended, revoke all active refresh tokens immediately
      if (dto.status === UserStatus.Suspended) {
        await tx.refreshToken.updateMany({
          where: { userId: targetUserId, revoked: false },
          data: { revoked: true },
        });
      }

      // Record in audit log
      await tx.auditLog.create({
        data: {
          actorUserId: adminUserId,
          actorRole: 'Admin',
          actionType: 'APPLY_USER_SANCTION',
          entityName: 'users',
          entityId: targetUserId,
          deltaState: {
            previousStatus: user.status,
            newStatus: dto.status,
            reason: dto.reason,
          },
        },
      });

      return {
        userId: updatedUser.id,
        email: updatedUser.email,
        status: updatedUser.status,
        reason: dto.reason,
        updatedAt: updatedUser.updatedAt,
      };
    });
  }

  async getAuditLogs(page = 1, pageSize = 20) {
    const skip = (page - 1) * pageSize;
    const [totalItems, items] = await Promise.all([
      this.prisma.auditLog.count(),
      this.prisma.auditLog.findMany({
        skip,
        take: pageSize,
        orderBy: { timestamp: 'desc' },
      }),
    ]);

    // Format bigints to strings for JSON safety
    const safeItems = items.map((log) => ({
      ...log,
      id: log.id.toString(),
    }));

    return {
      data: safeItems,
      meta: {
        page,
        pageSize,
        totalItems,
        totalPages: Math.ceil(totalItems / pageSize),
        hasNext: page * pageSize < totalItems,
        hasPrev: page > 1,
      },
    };
  }
}

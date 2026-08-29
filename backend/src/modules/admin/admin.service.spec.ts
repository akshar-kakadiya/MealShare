import { Test, TestingModule } from '@nestjs/testing';
import { AdminService } from './admin.service';
import { PrismaService } from '../../database/prisma.service';
import { NotFoundException, BadRequestException } from '@nestjs/common';
import { UserRole, UserStatus, VerificationStatus } from '@prisma/client';

describe('AdminService', () => {
  let service: AdminService;
  let prisma: PrismaService;

  const mockPrisma = {
    organizationVerification: {
      findUnique: jest.fn(),
      findMany: jest.fn(),
      update: jest.fn(),
    },
    receiverOrganization: {
      update: jest.fn(),
    },
    user: {
      findUnique: jest.fn(),
      update: jest.fn(),
    },
    refreshToken: {
      updateMany: jest.fn(),
    },
    auditLog: {
      create: jest.fn(),
      count: jest.fn(),
      findMany: jest.fn(),
    },
    $transaction: jest.fn((callback) => callback(mockPrisma)),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AdminService,
        { provide: PrismaService, useValue: mockPrisma },
      ],
    }).compile();

    service = module.get<AdminService>(AdminService);
    prisma = module.get<PrismaService>(PrismaService);
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('adjudicateVerification', () => {
    it('should approve verification and activate user status', async () => {
      mockPrisma.organizationVerification.findUnique.mockResolvedValue({
        id: 'verif-1',
        orgId: 'org-1',
        status: VerificationStatus.Pending,
        organization: { userId: 'user-org-1' },
      });

      mockPrisma.organizationVerification.update.mockResolvedValue({
        id: 'verif-1',
        status: VerificationStatus.Verified,
      });

      const result = await service.adjudicateVerification('verif-1', 'admin-user-1', {
        decision: VerificationStatus.Verified,
        adminNotes: '80G verified',
      });

      expect(mockPrisma.organizationVerification.update).toHaveBeenCalled();
      expect(mockPrisma.receiverOrganization.update).toHaveBeenCalledWith({
        where: { id: 'org-1' },
        data: { verificationStatus: VerificationStatus.Verified },
      });
      expect(mockPrisma.user.update).toHaveBeenCalledWith({
        where: { id: 'user-org-1' },
        data: { status: UserStatus.Active },
      });
      expect(mockPrisma.auditLog.create).toHaveBeenCalled();
    });

    it('should throw NotFoundException if verification does not exist', async () => {
      mockPrisma.organizationVerification.findUnique.mockResolvedValue(null);
      await expect(
        service.adjudicateVerification('non-existent', 'admin-1', {
          decision: VerificationStatus.Verified,
        }),
      ).rejects.toThrow(NotFoundException);
    });
  });

  describe('applyUserSanction', () => {
    it('should suspend user, revoke active refresh tokens, and log audit entry', async () => {
      mockPrisma.user.findUnique.mockResolvedValue({
        id: 'user-to-suspend',
        role: UserRole.Donor,
        status: UserStatus.Active,
      });
      mockPrisma.user.update.mockResolvedValue({
        id: 'user-to-suspend',
        email: 'donor@bad.com',
        status: UserStatus.Suspended,
        updatedAt: new Date(),
      });

      const result = await service.applyUserSanction('user-to-suspend', 'admin-1', {
        status: UserStatus.Suspended,
        reason: 'Spoilage complaint',
      });

      expect(mockPrisma.user.update).toHaveBeenCalledWith({
        where: { id: 'user-to-suspend' },
        data: { status: UserStatus.Suspended },
      });
      expect(mockPrisma.refreshToken.updateMany).toHaveBeenCalledWith({
        where: { userId: 'user-to-suspend', revoked: false },
        data: { revoked: true },
      });
      expect(mockPrisma.auditLog.create).toHaveBeenCalled();
      expect(result.status).toBe(UserStatus.Suspended);
    });

    it('should prevent sanctioning an Admin user', async () => {
      mockPrisma.user.findUnique.mockResolvedValue({
        id: 'other-admin',
        role: UserRole.Admin,
      });

      await expect(
        service.applyUserSanction('other-admin', 'admin-1', {
          status: UserStatus.Suspended,
          reason: 'Test',
        }),
      ).rejects.toThrow(BadRequestException);
    });
  });
});

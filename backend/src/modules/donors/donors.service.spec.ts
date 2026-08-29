import { Test, TestingModule } from '@nestjs/testing';
import { DonorsService } from './donors.service';
import { PrismaService } from '../../database/prisma.service';
import { BadRequestException, NotFoundException } from '@nestjs/common';
import { DonorType } from '@prisma/client';

describe('DonorsService', () => {
  let service: DonorsService;
  let prisma: PrismaService;

  const mockPrisma = {
    donorProfile: {
      findUnique: jest.fn(),
      update: jest.fn(),
    },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        DonorsService,
        { provide: PrismaService, useValue: mockPrisma },
      ],
    }).compile();

    service = module.get<DonorsService>(DonorsService);
    prisma = module.get<PrismaService>(PrismaService);
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('acceptPolicy', () => {
    it('should successfully record policy agreement when agreeToPolicy is true', async () => {
      mockPrisma.donorProfile.update.mockResolvedValue({
        id: 'donor-1',
        policyAccepted: true,
        policyAcceptedAt: new Date(),
        policyVersion: 'v1.0-2026',
      });

      const result = await service.acceptPolicy('user-1', {
        agreeToPolicy: true,
        policyVersion: 'v1.0-2026',
      });

      expect(result.success).toBe(true);
      expect(result.policyAccepted).toBe(true);
      expect(mockPrisma.donorProfile.update).toHaveBeenCalled();
    });

    it('should throw BadRequestException (ERR-DON-003) if agreeToPolicy is false', async () => {
      await expect(
        service.acceptPolicy('user-1', {
          agreeToPolicy: false,
          policyVersion: 'v1.0-2026',
        }),
      ).rejects.toThrow(BadRequestException);
    });
  });

  describe('getProfile', () => {
    it('should return donor profile if exists', async () => {
      mockPrisma.donorProfile.findUnique.mockResolvedValue({
        id: 'donor-1',
        type: DonorType.Restaurant,
        entityName: 'Grand Palace',
      });

      const result = await service.getProfile('user-1');
      expect(result.entityName).toBe('Grand Palace');
    });

    it('should throw NotFoundException if profile does not exist', async () => {
      mockPrisma.donorProfile.findUnique.mockResolvedValue(null);
      await expect(service.getProfile('user-1')).rejects.toThrow(NotFoundException);
    });
  });
});

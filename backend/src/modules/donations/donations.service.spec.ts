import { Test, TestingModule } from '@nestjs/testing';
import { DonationsService } from './donations.service';
import { PrismaService } from '../../database/prisma.service';
import { ForbiddenException, BadRequestException } from '@nestjs/common';
import { DonationStatus, StorageCondition } from '@prisma/client';

describe('DonationsService', () => {
  let service: DonationsService;
  let prisma: PrismaService;

  const mockPrismaService = {
    donorProfile: {
      findUnique: jest.fn(),
    },
    donation: {
      create: jest.fn(),
      findUnique: jest.fn(),
      update: jest.fn(),
    },
    auditLog: {
      create: jest.fn(),
    },
    $queryRaw: jest.fn(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        DonationsService,
        { provide: PrismaService, useValue: mockPrismaService },
      ],
    }).compile();

    service = module.get<DonationsService>(DonationsService);
    prisma = module.get<PrismaService>(PrismaService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('create', () => {
    it('should create a donation if policy is accepted and safety rules pass', async () => {
      mockPrismaService.donorProfile.findUnique.mockResolvedValue({ id: 'donor-id', policyAccepted: true });
      const now = new Date();
      const prepTime = new Date(now.getTime() - 1000).toISOString();
      const deadline = new Date(now.getTime() + 60 * 60000).toISOString(); // 1 hour from now

      const dto = {
        foodType: 'Test Food',
        quantityServings: 10,
        prepDateTime: prepTime,
        storageCondition: StorageCondition.Refrigerated,
        consumptionDeadline: deadline,
        isVegetarian: true,
        pickupAddress: 'Address',
        pickupLatitude: 10,
        pickupLongitude: 20,
        foodPhotoUrl: 'http://example.com/photo.jpg',
      };

      mockPrismaService.donation.create.mockResolvedValue({
        id: 'don-1',
        ...dto,
        prepDateTime: new Date(dto.prepDateTime),
        consumptionDeadline: new Date(dto.consumptionDeadline),
        status: DonationStatus.Posted,
        createdAt: new Date(),
      });

      const res = await service.create('user-id', dto as any);
      expect(res.success).toBe(true);
      expect(res.data.status).toBe('Posted');
      expect(mockPrismaService.donation.create).toHaveBeenCalled();
    });

    it('should throw ForbiddenException if policy not accepted', async () => {
      mockPrismaService.donorProfile.findUnique.mockResolvedValue({ id: 'donor-id', policyAccepted: false });
      
      await expect(service.create('user-id', {} as any)).rejects.toThrow(ForbiddenException);
    });

    it('should throw BadRequestException if consumption deadline is too soon', async () => {
      mockPrismaService.donorProfile.findUnique.mockResolvedValue({ id: 'donor-id', policyAccepted: true });
      
      const now = new Date();
      const prepTime = new Date(now.getTime() - 1000).toISOString();
      const deadline = new Date(now.getTime() + 15 * 60000).toISOString(); // only 15 mins

      const dto = {
        foodType: 'Test Food',
        quantityServings: 10,
        prepDateTime: prepTime,
        storageCondition: StorageCondition.Refrigerated,
        consumptionDeadline: deadline,
        isVegetarian: true,
        pickupAddress: 'Address',
        pickupLatitude: 10,
        pickupLongitude: 20,
        foodPhotoUrl: 'http://example.com/photo.jpg',
      };

      await expect(service.create('user-id', dto as any)).rejects.toThrow(BadRequestException);
    });
  });

  describe('cancel', () => {
    it('should cancel donation if user is the donor and status is cancellable', async () => {
      mockPrismaService.donation.findUnique.mockResolvedValue({
        id: 'don-1',
        status: DonationStatus.Posted,
        donor: { userId: 'user-id' }
      });
      mockPrismaService.donation.update.mockResolvedValue({ id: 'don-1', status: DonationStatus.Cancelled });

      const res = await service.cancel('don-1', 'user-id');
      expect(res.success).toBe(true);
      expect(mockPrismaService.donation.update).toHaveBeenCalledWith({
        where: { id: 'don-1' },
        data: { status: DonationStatus.Cancelled },
      });
    });

    it('should throw ForbiddenException if user is not the donor', async () => {
      mockPrismaService.donation.findUnique.mockResolvedValue({
        id: 'don-1',
        status: DonationStatus.Posted,
        donor: { userId: 'other-user-id' }
      });

      await expect(service.cancel('don-1', 'user-id')).rejects.toThrow(ForbiddenException);
    });
  });
});

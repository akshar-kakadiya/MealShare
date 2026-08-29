import { Test, TestingModule } from '@nestjs/testing';
import { RequestsService } from './requests.service';
import { PrismaService } from '../../database/prisma.service';
import {
  NotFoundException,
  ForbiddenException,
  BadRequestException,
  ConflictException,
} from '@nestjs/common';
import { UserRole, DonationStatus, RequestStatus } from '@prisma/client';

describe('RequestsService', () => {
  let service: RequestsService;
  let prisma: PrismaService;

  const mockTx = {
    user: {
      findUnique: jest.fn(),
    },
    $queryRaw: jest.fn(),
    donationRequest: {
      findFirst: jest.fn(),
      findMany: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      count: jest.fn(),
    },
    donation: {
      findUnique: jest.fn(),
      update: jest.fn(),
    },
    donorProfile: {
      findUnique: jest.fn(),
    },
    notification: {
      create: jest.fn(),
    },
    auditLog: {
      create: jest.fn(),
    },
  };

  const mockPrismaService = {
    $transaction: jest.fn((callback) => callback(mockTx)),
    donation: {
      findUnique: jest.fn(),
    },
    donationRequest: {
      findMany: jest.fn(),
    },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RequestsService,
        { provide: PrismaService, useValue: mockPrismaService },
      ],
    }).compile();

    service = module.get<RequestsService>(RequestsService);
    prisma = module.get<PrismaService>(PrismaService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('enqueueRequest', () => {
    it('should successfully enqueue a request for a Posted donation', async () => {
      mockTx.user.findUnique.mockResolvedValue({
        id: 'recv-1',
        role: UserRole.Receiver_Org,
      });

      const futureDate = new Date(Date.now() + 3600000);
      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Posted,
          consumptionDeadline: futureDate,
        },
      ]);

      mockTx.donationRequest.findFirst.mockResolvedValue(null); // No duplicate

      const serverTimestamp = new Date();
      mockTx.donationRequest.create.mockResolvedValue({
        id: 'req-1',
        donationId: 'don-1',
        receiverId: 'recv-1',
        notes: 'Feeding children',
        status: RequestStatus.Pending,
        queueTimestamp: serverTimestamp,
      });

      mockTx.donation.update.mockResolvedValue({
        id: 'don-1',
        status: DonationStatus.Requested,
      });

      mockTx.donationRequest.count.mockResolvedValue(1);
      mockTx.donorProfile.findUnique.mockResolvedValue({ userId: 'donor-user-1' });

      const res = await service.enqueueRequest('don-1', 'recv-1', {
        notes: 'Feeding children',
      });

      expect(res.requestId).toBe('req-1');
      expect(res.queuePosition).toBe(1);
      expect(res.status).toBe(RequestStatus.Pending);
      expect(mockTx.donation.update).toHaveBeenCalledWith({
        where: { id: 'don-1' },
        data: { status: DonationStatus.Requested },
      });
      expect(mockTx.notification.create).toHaveBeenCalled();
      expect(mockTx.auditLog.create).toHaveBeenCalled();
    });

    it('should reject duplicate request with ERR-REQ-001 (Conflict)', async () => {
      mockTx.user.findUnique.mockResolvedValue({
        id: 'recv-1',
        role: UserRole.Receiver_Org,
      });

      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Posted,
          consumptionDeadline: new Date(Date.now() + 3600000),
        },
      ]);

      mockTx.donationRequest.findFirst.mockResolvedValue({
        id: 'existing-req',
        status: RequestStatus.Pending,
      });

      await expect(
        service.enqueueRequest('don-1', 'recv-1', { notes: 'Duplicate' }),
      ).rejects.toThrow(ConflictException);
    });

    it('should reject request for expired donation with ERR-REQ-002 (BadRequest)', async () => {
      mockTx.user.findUnique.mockResolvedValue({
        id: 'recv-1',
        role: UserRole.Receiver_Org,
      });

      const pastDate = new Date(Date.now() - 3600000);
      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Posted,
          consumptionDeadline: pastDate,
        },
      ]);

      await expect(
        service.enqueueRequest('don-1', 'recv-1', {}),
      ).rejects.toThrow(BadRequestException);
    });

    it('should reject request for unrequestable status with ERR-REQ-003', async () => {
      mockTx.user.findUnique.mockResolvedValue({
        id: 'recv-1',
        role: UserRole.Receiver_Org,
      });

      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Approved,
          consumptionDeadline: new Date(Date.now() + 3600000),
        },
      ]);

      await expect(
        service.enqueueRequest('don-1', 'recv-1', {}),
      ).rejects.toThrow(ConflictException);
    });

    it('should reject non-receiver role with ForbiddenException', async () => {
      mockTx.user.findUnique.mockResolvedValue({
        id: 'donor-1',
        role: UserRole.Donor,
      });

      await expect(
        service.enqueueRequest('don-1', 'donor-1', {}),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  describe('getQueueForDonation', () => {
    it('should return FCFS ordered requests for donation owner', async () => {
      mockPrismaService.donation.findUnique.mockResolvedValue({
        id: 'don-1',
        donor: { userId: 'donor-user-1' },
      });

      mockPrismaService.donationRequest.findMany.mockResolvedValue([
        {
          id: 'req-1',
          queueTimestamp: new Date('2026-08-29T10:00:00Z'),
          status: RequestStatus.Pending,
          notes: 'Note 1',
          receiver: {
            id: 'recv-user-1',
            receiverOrg: {
              orgName: 'Food Bank NGO',
              type: 'Shelter',
              verificationStatus: 'Verified',
              ratingAverage: 4.9,
              beneficiaryCapacity: 100,
            },
            volunteerProfile: null,
          },
        },
      ]);

      const queue = await service.getQueueForDonation('don-1', 'donor-user-1');
      expect(queue.length).toBe(1);
      expect(queue[0].queuePosition).toBe(1);
      expect(queue[0].receiver.name).toBe('Food Bank NGO');
      expect(queue[0].receiver.verificationStatus).toBe('Verified');
    });

    it('should throw ForbiddenException if user is not the donation owner', async () => {
      mockPrismaService.donation.findUnique.mockResolvedValue({
        id: 'don-1',
        donor: { userId: 'different-donor' },
      });

      await expect(
        service.getQueueForDonation('don-1', 'donor-user-1'),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  describe('approveRequest', () => {
    it('should approve receiver, generate 6-digit OTP, and update donation to Approved', async () => {
      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Requested,
          donorUserId: 'donor-user-1',
        },
      ]);

      mockTx.donationRequest.findFirst.mockResolvedValue({
        id: 'req-1',
        donationId: 'don-1',
        receiverId: 'recv-user-1',
        status: RequestStatus.Pending,
      });

      mockTx.donationRequest.update.mockResolvedValue({
        id: 'req-1',
        status: RequestStatus.Accepted,
      });

      mockTx.donation.update.mockResolvedValue({
        id: 'don-1',
        status: DonationStatus.Approved,
      });

      const res = await service.approveRequest('don-1', 'req-1', 'donor-user-1');

      expect(res.status).toBe('Approved');
      expect(res.acceptedReceiverId).toBe('recv-user-1');
      expect(res.handoverOtp).toBeDefined();
      expect(res.handoverOtp.length).toBe(6);
      expect(mockTx.donationRequest.update).toHaveBeenCalledWith({
        where: { id: 'req-1' },
        data: { status: RequestStatus.Accepted },
      });
      expect(mockTx.notification.create).toHaveBeenCalled();
      expect(mockTx.auditLog.create).toHaveBeenCalled();
    });

    it('should throw ForbiddenException if non-owner tries to approve', async () => {
      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Requested,
          donorUserId: 'donor-user-1',
        },
      ]);

      await expect(
        service.approveRequest('don-1', 'req-1', 'intruder-user'),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  describe('rejectRequest', () => {
    it('should reject request and revert donation to Posted if no pending requests remain', async () => {
      mockTx.$queryRaw.mockResolvedValue([
        {
          id: 'don-1',
          donorId: 'donor-profile-1',
          status: DonationStatus.Requested,
          donorUserId: 'donor-user-1',
        },
      ]);

      mockTx.donationRequest.findFirst.mockResolvedValue({
        id: 'req-1',
        donationId: 'don-1',
        receiverId: 'recv-user-1',
        status: RequestStatus.Pending,
      });

      mockTx.donationRequest.update.mockResolvedValue({
        id: 'req-1',
        status: RequestStatus.Rejected,
      });

      mockTx.donationRequest.count.mockResolvedValue(0); // 0 remaining

      const res = await service.rejectRequest(
        'don-1',
        'req-1',
        'donor-user-1',
        { rejectionReason: 'No capacity' },
      );

      expect(res.status).toBe('Rejected');
      expect(res.donationStatus).toBe(DonationStatus.Posted);
      expect(mockTx.donation.update).toHaveBeenCalledWith({
        where: { id: 'don-1' },
        data: { status: DonationStatus.Posted },
      });
    });
  });

  describe('withdrawRequest', () => {
    it('should mark request as Withdrawn and revert donation to Posted if no pending remain', async () => {
      mockTx.donationRequest.findFirst.mockResolvedValue({
        id: 'req-1',
        donationId: 'don-1',
        receiverId: 'recv-user-1',
        status: RequestStatus.Pending,
      });

      mockTx.donationRequest.update.mockResolvedValue({
        id: 'req-1',
        status: RequestStatus.Withdrawn,
      });

      mockTx.donationRequest.count.mockResolvedValue(0);
      mockTx.donation.findUnique.mockResolvedValue({
        id: 'don-1',
        status: DonationStatus.Requested,
      });

      const res = await service.withdrawRequest('don-1', 'req-1', 'recv-user-1');

      expect(res.status).toBe('Withdrawn');
      expect(mockTx.donation.update).toHaveBeenCalledWith({
        where: { id: 'don-1' },
        data: { status: DonationStatus.Posted },
      });
    });
  });
});

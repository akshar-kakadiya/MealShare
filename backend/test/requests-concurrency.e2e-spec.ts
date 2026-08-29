import { Test, TestingModule } from '@nestjs/testing';
import { PrismaService } from '../src/database/prisma.service';
import { RequestsService } from '../src/modules/requests/requests.service';
import { UserRole, UserStatus, DonationStatus, StorageCondition, RequestStatus, OrgType } from '@prisma/client';
import * as bcrypt from 'bcrypt';

describe('Phase 3: FCFS Concurrency & Transactional Locking (PostgreSQL)', () => {
  let prisma: PrismaService;
  let requestsService: RequestsService;

  let donorUser: any;
  let donorProfile: any;
  let receiverA: any;
  let receiverB: any;
  let donation: any;

  beforeAll(async () => {
    const moduleRef: TestingModule = await Test.createTestingModule({
      providers: [PrismaService, RequestsService],
    }).compile();

    prisma = moduleRef.get<PrismaService>(PrismaService);
    requestsService = moduleRef.get<RequestsService>(RequestsService);

    // Connect to real PostgreSQL database
    await prisma.$connect();

    // Clean up test data if left from previous runs
    await prisma.donationRequest.deleteMany({
      where: { donation: { pickupAddress: 'Concurrency Test Street 100' } },
    });
    await prisma.donation.deleteMany({
      where: { pickupAddress: 'Concurrency Test Street 100' },
    });
    await prisma.user.deleteMany({
      where: {
        email: {
          in: [
            'donor-fcfs-test@mealshare.org',
            'recva-fcfs-test@mealshare.org',
            'recvb-fcfs-test@mealshare.org',
          ],
        },
      },
    });

    const passwordHash = await bcrypt.hash('Password123!', 10);

    // Create Donor
    donorUser = await prisma.user.create({
      data: {
        email: 'donor-fcfs-test@mealshare.org',
        phone: '+919999900001',
        passwordHash,
        role: UserRole.Donor,
        status: UserStatus.Active,
        donorProfile: {
          create: {
            type: 'Restaurant',
            entityName: 'FCFS Test Bistro',
            addressText: '100 Main St',
            latitude: 12.9716,
            longitude: 77.5946,
            policyAccepted: true,
          },
        },
      },
      include: { donorProfile: true },
    });
    donorProfile = donorUser.donorProfile;

    // Create Receiver A
    receiverA = await prisma.user.create({
      data: {
        email: 'recva-fcfs-test@mealshare.org',
        phone: '+919999900002',
        passwordHash,
        role: UserRole.Receiver_Org,
        status: UserStatus.Active,
        receiverOrg: {
          create: {
            orgName: 'Receiver A Foundation',
            type: OrgType.Shelter,
            registrationNumber: 'REG-A-100',
            addressText: '200 A St',
            serviceLatitude: 12.9716,
            serviceLongitude: 77.5946,
            serviceRadiusMeters: 15000,
            verificationStatus: 'Verified',
          },
        },
      },
    });

    // Create Receiver B
    receiverB = await prisma.user.create({
      data: {
        email: 'recvb-fcfs-test@mealshare.org',
        phone: '+919999900003',
        passwordHash,
        role: UserRole.Receiver_Org,
        status: UserStatus.Active,
        receiverOrg: {
          create: {
            orgName: 'Receiver B Foundation',
            type: OrgType.NGO,
            registrationNumber: 'REG-B-200',
            addressText: '300 B St',
            serviceLatitude: 12.9716,
            serviceLongitude: 77.5946,
            serviceRadiusMeters: 15000,
            verificationStatus: 'Verified',
          },
        },
      },
    });
  });

  afterAll(async () => {
    // Cleanup
    if (donation) {
      await prisma.notification.deleteMany({ where: { referenceId: donation.id } });
      await prisma.auditLog.deleteMany({ where: { entityId: donation.id } });
      await prisma.donationRequest.deleteMany({ where: { donationId: donation.id } });
      await prisma.donation.deleteMany({ where: { id: donation.id } });
    }
    if (donorUser) {
      await prisma.auditLog.deleteMany({
        where: {
          actorUserId: {
            in: [donorUser.id, receiverA.id, receiverB.id],
          },
        },
      });
      await prisma.user.deleteMany({
        where: {
          id: { in: [donorUser.id, receiverA.id, receiverB.id] },
        },
      });
    }
    await prisma.$disconnect();
  });

  beforeEach(async () => {
    // Create fresh Posted donation for each test
    donation = await prisma.donation.create({
      data: {
        donorId: donorProfile.id,
        foodType: 'Fresh Hot Meals',
        quantityServings: 50,
        prepDateTime: new Date(Date.now() - 3600000),
        storageCondition: StorageCondition.Heated_Container,
        consumptionDeadline: new Date(Date.now() + 4 * 3600000),
        isVegetarian: true,
        pickupAddress: 'Concurrency Test Street 100',
        pickupLatitude: 12.9716,
        pickupLongitude: 77.5946,
        foodPhotoUrl: 'https://storage.mealshare.org/food.jpg',
        status: DonationStatus.Posted,
      },
    });
  });

  afterEach(async () => {
    if (donation) {
      await prisma.notification.deleteMany({ where: { referenceId: donation.id } });
      await prisma.auditLog.deleteMany({ where: { entityId: donation.id } });
      await prisma.donationRequest.deleteMany({ where: { donationId: donation.id } });
      await prisma.donation.deleteMany({ where: { id: donation.id } });
    }
  });

  it('Concurrent Enqueue: Two simultaneous receiver requests are serialized by PostgreSQL transaction without race condition', async () => {
    // Fire concurrent request submissions targeting the same Posted donation
    const [resultA, resultB] = await Promise.all([
      requestsService.enqueueRequest(donation.id, receiverA.id, {
        notes: 'Receiver A immediate dispatch',
      }),
      requestsService.enqueueRequest(donation.id, receiverB.id, {
        notes: 'Receiver B immediate dispatch',
      }),
    ]);

    expect(resultA.donationId).toBe(donation.id);
    expect(resultB.donationId).toBe(donation.id);

    // Verify both requests are present in PostgreSQL database
    const dbRequests = await prisma.donationRequest.findMany({
      where: { donationId: donation.id },
      orderBy: { queueTimestamp: 'asc' },
    });

    expect(dbRequests.length).toBe(2);
    expect(dbRequests[0].status).toBe(RequestStatus.Pending);
    expect(dbRequests[1].status).toBe(RequestStatus.Pending);

    // Verify queue positions are 1 and 2
    const queue = await requestsService.getQueueForDonation(donation.id, donorUser.id);
    expect(queue.length).toBe(2);
    expect(queue[0].queuePosition).toBe(1);
    expect(queue[1].queuePosition).toBe(2);

    // Verify donation status transitioned to Requested
    const updatedDonation = await prisma.donation.findUnique({
      where: { id: donation.id },
    });
    expect(updatedDonation?.status).toBe(DonationStatus.Requested);
  });

  it('Duplicate Request Prevention: Same receiver cannot enqueue duplicate active request', async () => {
    // First request
    await requestsService.enqueueRequest(donation.id, receiverA.id, { notes: 'Req 1' });

    // Second duplicate request
    await expect(
      requestsService.enqueueRequest(donation.id, receiverA.id, { notes: 'Req 2 duplicate' }),
    ).rejects.toThrow();
  });

  it('Mutual Exclusion on Approval: Concurrent approvals on same donation result in exactly ONE winner and ZERO double-allocation', async () => {
    // 1. Enqueue both receivers
    const reqA = await requestsService.enqueueRequest(donation.id, receiverA.id, { notes: 'A' });
    const reqB = await requestsService.enqueueRequest(donation.id, receiverB.id, { notes: 'B' });

    // 2. Simulate two simultaneous approval transactions executed against the donation
    const results = await Promise.allSettled([
      requestsService.approveRequest(donation.id, reqA.requestId, donorUser.id),
      requestsService.approveRequest(donation.id, reqB.requestId, donorUser.id),
    ]);

    // Count fulfilled vs rejected
    const fulfilled = results.filter((r) => r.status === 'fulfilled');
    const rejected = results.filter((r) => r.status === 'rejected');

    // Exactly one approval succeeds
    expect(fulfilled.length).toBe(1);
    // Competing approval transaction is blocked and rejected by row lock guard
    expect(rejected.length).toBe(1);

    // Check final state in PostgreSQL database
    const finalDonation = await prisma.donation.findUnique({
      where: { id: donation.id },
    });

    expect(finalDonation?.status).toBe(DonationStatus.Approved);
    expect(finalDonation?.handoverOtpHash).toBeDefined();
    expect(finalDonation?.acceptedReceiverId).toBeDefined();

    // Check request statuses
    const acceptedCount = await prisma.donationRequest.count({
      where: { donationId: donation.id, status: RequestStatus.Accepted },
    });
    expect(acceptedCount).toBe(1);

    // The other request remains Pending (waitlisted in FCFS order)
    const pendingCount = await prisma.donationRequest.count({
      where: { donationId: donation.id, status: RequestStatus.Pending },
    });
    expect(pendingCount).toBe(1);
  });
});

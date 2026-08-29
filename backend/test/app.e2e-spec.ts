import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';
import { PrismaService } from '../src/database/prisma.service';
import { UserRole, UserStatus, DonorType, VerificationStatus, StorageCondition } from '@prisma/client';
import * as bcrypt from 'bcrypt';

describe('MealShare Auth & RBAC (e2e)', () => {
  let app: INestApplication;

  // In-memory data store for mocking Prisma in e2e
  const usersDb = new Map<string, any>();
  const donorProfilesDb = new Map<string, any>();
  const receiverOrgsDb = new Map<string, any>();
  const volunteerProfilesDb = new Map<string, any>();
  const refreshTokensDb = new Map<string, any>();
  const verificationsDb = new Map<string, any>();
  const auditLogsDb: any[] = [];
  const donationsDb = new Map<string, any>();
  const donationRequestsDb = new Map<string, any>();
  const notificationsDb: any[] = [];

  const mockPrismaService = {
    $connect: jest.fn(),
    $disconnect: jest.fn(),
    $transaction: jest.fn((callback) => callback(mockPrismaService)),
    user: {
      findFirst: jest.fn(({ where }) => {
        for (const user of usersDb.values()) {
          if (where.OR) {
            for (const cond of where.OR) {
              if (cond.email && user.email === cond.email) return Promise.resolve(user);
              if (cond.phone && user.phone === cond.phone) return Promise.resolve(user);
            }
          }
          if (where.id && user.id === where.id) return Promise.resolve(user);
          if (where.email && user.email === where.email) return Promise.resolve(user);
        }
        return Promise.resolve(null);
      }),
      findUnique: jest.fn(({ where }) => {
        const user = usersDb.get(where.id);
        if (!user) return Promise.resolve(null);
        return Promise.resolve({
          ...user,
          donorProfile: donorProfilesDb.get(user.id),
          receiverOrg: receiverOrgsDb.get(user.id),
          volunteerProfile: volunteerProfilesDb.get(user.id),
        });
      }),
      create: jest.fn(({ data }) => {
        const id = `user-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`;
        const user = { id, ...data, createdAt: new Date(), updatedAt: new Date() };
        usersDb.set(id, user);
        return Promise.resolve(user);
      }),
      update: jest.fn(({ where, data }) => {
        const user = usersDb.get(where.id);
        if (!user) return Promise.resolve(null);
        const updated = { ...user, ...data, updatedAt: new Date() };
        usersDb.set(where.id, updated);
        return Promise.resolve(updated);
      }),
    },
    donorProfile: {
      findUnique: jest.fn(({ where }) => Promise.resolve(donorProfilesDb.get(where.userId) || null)),
      create: jest.fn(({ data }) => {
        const profile = { id: `donor-${Date.now()}`, ...data, createdAt: new Date(), updatedAt: new Date() };
        donorProfilesDb.set(data.userId, profile);
        return Promise.resolve(profile);
      }),
      update: jest.fn(({ where, data }) => {
        const profile = donorProfilesDb.get(where.userId);
        const updated = { ...profile, ...data, updatedAt: new Date() };
        donorProfilesDb.set(where.userId, updated);
        return Promise.resolve(updated);
      }),
    },
    receiverOrganization: {
      findUnique: jest.fn(({ where }) => Promise.resolve(receiverOrgsDb.get(where.userId) || null)),
      create: jest.fn(({ data }) => {
        const org = { id: `org-${Date.now()}`, ...data, createdAt: new Date(), updatedAt: new Date() };
        receiverOrgsDb.set(data.userId, org);
        return Promise.resolve(org);
      }),
      update: jest.fn(({ where, data }) => {
        let org: any = null;
        if (where.userId) {
          org = receiverOrgsDb.get(where.userId);
        } else if (where.id) {
          for (const o of receiverOrgsDb.values()) {
            if (o.id === where.id) { org = o; break; }
          }
        }
        if (!org) return Promise.resolve(null);
        const updated = { ...org, ...data, updatedAt: new Date() };
        receiverOrgsDb.set(org.userId, updated);
        return Promise.resolve(updated);
      }),
    },
    volunteerProfile: {
      findUnique: jest.fn(({ where }) => Promise.resolve(volunteerProfilesDb.get(where.userId) || null)),
      findFirst: jest.fn(({ where }) => {
        for (const vol of volunteerProfilesDb.values()) {
          if (where.OR) {
            for (const cond of where.OR) {
              if (cond.id && vol.id === cond.id) return Promise.resolve({ ...vol, certificates: [] });
              if (cond.userId && vol.userId === cond.userId) return Promise.resolve({ ...vol, certificates: [] });
            }
          }
        }
        return Promise.resolve(null);
      }),
      create: jest.fn(({ data }) => {
        const vol = { id: `vol-${Date.now()}`, ...data, certificates: [], createdAt: new Date(), updatedAt: new Date() };
        volunteerProfilesDb.set(data.userId, vol);
        return Promise.resolve(vol);
      }),
      update: jest.fn(({ where, data }) => {
        const vol = volunteerProfilesDb.get(where.userId);
        const updated = { ...vol, ...data, updatedAt: new Date() };
        volunteerProfilesDb.set(where.userId, updated);
        return Promise.resolve(updated);
      }),
    },
    
    donation: {
      create: jest.fn(({ data }) => {
        const id = `don-${Date.now()}`;
        const donation = { id, ...data, createdAt: new Date(), updatedAt: new Date() };
        donationsDb.set(id, donation);
        return Promise.resolve(donation);
      }),
      findUnique: jest.fn(({ where, include }) => {
        const donation = donationsDb.get(where.id);
        if (!donation) return Promise.resolve(null);
        if (include?.donor) {
           let d = null;
           for (const [k, v] of donorProfilesDb.entries()) {
             if (v.id === donation.donorId) d = v;
           }
           return Promise.resolve({ ...donation, donor: d });
        }
        return Promise.resolve(donation);
      }),
      findMany: jest.fn(({ where }) => {
        const res: any[] = [];
        for (const don of donationsDb.values()) {
          if (don.donorId === where.donorId) res.push(don);
        }
        return Promise.resolve(res);
      }),
      update: jest.fn(({ where, data }) => {
        const don = donationsDb.get(where.id);
        const cleanData = {};
        for (const [k, v] of Object.entries(data)) {
          if (v !== undefined) cleanData[k] = v;
        }
        const updated = { ...don, ...cleanData, updatedAt: new Date() };
        donationsDb.set(where.id, updated);
        return Promise.resolve(updated);
      }),
    },
        $queryRaw: jest.fn((strings, ...values) => {
      // Check if it's a SELECT FOR UPDATE on donations
      const sql = Array.isArray(strings) ? strings.join('?') : String(strings);
      if (sql.includes('FROM donations') && sql.includes('FOR UPDATE')) {
        const donId = values[0];
        const don = donationsDb.get(donId);
        if (!don) return Promise.resolve([]);
        let donorUserId: string | null = null;
        for (const [uid, p] of donorProfilesDb.entries()) {
          if (p.id === don.donorId) donorUserId = uid;
        }
        return Promise.resolve([{
          id: don.id,
          donorId: don.donorId,
          status: don.status,
          consumptionDeadline: don.consumptionDeadline,
          donorUserId,
        }]);
      }

      // Default to nearby donations spatial mock
      const res: any[] = [];
      for (const don of donationsDb.values()) {
        if (don.status === 'Posted' && new Date(don.consumptionDeadline) > new Date()) {
          res.push({
            donationId: don.id,
            foodType: don.foodType,
            quantityServings: don.quantityServings,
            storageCondition: don.storageCondition,
            consumptionDeadline: don.consumptionDeadline,
            isVegetarian: don.isVegetarian,
            allergens: don.allergens,
            foodPhotoUrl: don.foodPhotoUrl,
            pickupAddress: don.pickupAddress,
            status: don.status,
            distanceMeters: 500,
            entityName: 'Test Donor',
            donorType: 'Restaurant',
            ratingAverage: 5.0,
            completedDonations: 10,
          });
        }
      }
      return Promise.resolve(res);
    }),

    
    donationRequest: {
      create: jest.fn(({ data }) => {
        const id = `req-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`;
        const req = {
          id,
          ...data,
          queueTimestamp: data.queueTimestamp || new Date(),
          createdAt: new Date(),
        };
        donationRequestsDb.set(id, req);
        return Promise.resolve(req);
      }),
      findFirst: jest.fn(({ where }) => {
        for (const req of donationRequestsDb.values()) {
          if (where.id && req.id !== where.id) continue;
          if (where.donationId && req.donationId !== where.donationId) continue;
          if (where.receiverId && req.receiverId !== where.receiverId) continue;
          if (where.status) {
            if (typeof where.status === 'string' && req.status !== where.status) continue;
            if (where.status.in && !where.status.in.includes(req.status)) continue;
          }
          return Promise.resolve(req);
        }
        return Promise.resolve(null);
      }),
      findMany: jest.fn(({ where, orderBy }) => {
        const res: any[] = [];
        for (const req of donationRequestsDb.values()) {
          if (where.donationId && req.donationId !== where.donationId) continue;
          if (where.receiverId && req.receiverId !== where.receiverId) continue;
          
          // Attach receiver relations
          const receiverUser = usersDb.get(req.receiverId);
          const receiverOrg = receiverOrgsDb.get(req.receiverId) || null;
          const volunteerProfile = volunteerProfilesDb.get(req.receiverId) || null;
          const donation = donationsDb.get(req.donationId);
          let donor = null;
          if (donation) {
            for (const d of donorProfilesDb.values()) {
              if (d.id === donation.donorId) donor = d;
            }
          }

          res.push({
            ...req,
            receiver: {
              ...receiverUser,
              receiverOrg,
              volunteerProfile,
            },
            donation: donation ? { ...donation, donor } : null,
          });
        }
        res.sort((a, b) => new Date(a.queueTimestamp).getTime() - new Date(b.queueTimestamp).getTime());
        return Promise.resolve(res);
      }),
      update: jest.fn(({ where, data }) => {
        const req = donationRequestsDb.get(where.id);
        if (!req) return Promise.resolve(null);
        const updated = { ...req, ...data };
        donationRequestsDb.set(where.id, updated);
        return Promise.resolve(updated);
      }),
      count: jest.fn(({ where }) => {
        let count = 0;
        for (const req of donationRequestsDb.values()) {
          if (where.donationId && req.donationId !== where.donationId) continue;
          if (where.status && req.status !== where.status) continue;
          if (where.queueTimestamp?.lte && new Date(req.queueTimestamp) > new Date(where.queueTimestamp.lte)) continue;
          count++;
        }
        return Promise.resolve(count);
      }),
    },
    notification: {
      create: jest.fn(({ data }) => {
        const notif = { id: `notif-${Date.now()}`, ...data, createdAt: new Date() };
        notificationsDb.push(notif);
        return Promise.resolve(notif);
      }),
    },
    organizationVerification: {
      create: jest.fn(({ data }) => {
        const verif = { id: `verif-${Date.now()}`, ...data, createdAt: new Date() };
        verificationsDb.set(verif.id, verif);
        return Promise.resolve(verif);
      }),
      findUnique: jest.fn(({ where }) => {
        const verif = verificationsDb.get(where.id);
        if (!verif) return Promise.resolve(null);
        let organization: any = null;
        for (const org of receiverOrgsDb.values()) {
          if (org.id === verif.orgId) { organization = org; break; }
        }
        return Promise.resolve({ ...verif, organization });
      }),
      findMany: jest.fn(() => Promise.resolve(Array.from(verificationsDb.values()))),
      update: jest.fn(({ where, data }) => {
        const verif = verificationsDb.get(where.id);
        const updated = { ...verif, ...data };
        verificationsDb.set(where.id, updated);
        return Promise.resolve(updated);
      }),
    },
    refreshToken: {
      create: jest.fn(({ data }) => {
        const token = { id: `token-${Date.now()}`, ...data, createdAt: new Date() };
        refreshTokensDb.set(token.id, token);
        return Promise.resolve(token);
      }),
      findFirst: jest.fn(({ where }) => {
        for (const token of refreshTokensDb.values()) {
          if (token.tokenHash === where.tokenHash && !token.revoked) {
            const user = usersDb.get(token.userId);
            return Promise.resolve({ ...token, user });
          }
        }
        return Promise.resolve(null);
      }),
      update: jest.fn(({ where, data }) => {
        const token = refreshTokensDb.get(where.id);
        if (token) Object.assign(token, data);
        return Promise.resolve(token);
      }),
      updateMany: jest.fn(({ where, data }) => {
        for (const token of refreshTokensDb.values()) {
          if (token.userId === where.userId) Object.assign(token, data);
        }
        return Promise.resolve({ count: 1 });
      }),
    },
    auditLog: {
      create: jest.fn(({ data }) => {
        const log = { id: BigInt(auditLogsDb.length + 1), ...data, timestamp: new Date() };
        auditLogsDb.push(log);
        return Promise.resolve(log);
      }),
      count: jest.fn(() => Promise.resolve(auditLogsDb.length)),
      findMany: jest.fn(() => Promise.resolve(auditLogsDb)),
    },
  };

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider(PrismaService)
      .useValue(mockPrismaService)
      .compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('api/v1');
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        transform: true,
        forbidNonWhitelisted: true,
      }),
    );
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  let donorAccessToken: string;
  let receiverAccessToken: string;
  let adminAccessToken: string;

  describe('1. Authentication Flows', () => {
    it('POST /api/v1/auth/register -> should register a new Donor account', async () => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send({
          email: 'donor.e2e@mealshare.org',
          phone: '+919988776655',
          password: 'Password123!',
          name: 'Grand Spice Kitchen',
          role: 'Donor',
        })
        .expect(201);

      expect(res.body.success).toBe(true);
      expect(res.body.data.email).toBe('donor.e2e@mealshare.org');
      expect(res.body.data.role).toBe('Donor');
    });

    it('POST /api/v1/auth/register -> should reject duplicate email (ERR-AUTH-001 / 409 Conflict)', async () => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send({
          email: 'donor.e2e@mealshare.org',
          phone: '+919988776654',
          password: 'Password123!',
          name: 'Duplicate Kitchen',
          role: 'Donor',
        })
        .expect(409);

      expect(res.body.success).toBe(false);
      expect(res.body.error.code).toBe('ERR-AUTH-001');
    });

    it('POST /api/v1/auth/login -> should authenticate Donor and return JWT tokens', async () => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({
          identifier: 'donor.e2e@mealshare.org',
          password: 'Password123!',
        })
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.accessToken).toBeDefined();
      expect(res.body.data.refreshToken).toBeDefined();
      donorAccessToken = res.body.data.accessToken;
    });

    it('POST /api/v1/auth/register -> should register Receiver Organization', async () => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send({
          email: 'shelter.e2e@mealshare.org',
          phone: '+919911223344',
          password: 'Password123!',
          name: 'Hope Children Shelter',
          role: 'Receiver_Org',
        })
        .expect(201);

      expect(res.body.data.role).toBe('Receiver_Org');
      expect(res.body.data.status).toBe('Pending_Verification');
    });

    it('POST /api/v1/auth/login -> should authenticate Receiver Organization', async () => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({
          identifier: 'shelter.e2e@mealshare.org',
          password: 'Password123!',
        })
        .expect(200);

      receiverAccessToken = res.body.data.accessToken;
    });

    it('Seed Admin user and login', async () => {
      const adminPasswordHash = await bcrypt.hash('AdminPassword123!', 10);
      const adminUser = {
        id: 'admin-fixed-uuid',
        email: 'admin@mealshare.org',
        phone: '+919000000000',
        passwordHash: adminPasswordHash,
        role: UserRole.Admin,
        status: UserStatus.Active,
        createdAt: new Date(),
        updatedAt: new Date(),
      };
      usersDb.set('admin-fixed-uuid', adminUser);

      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({
          identifier: 'admin@mealshare.org',
          password: 'AdminPassword123!',
        })
        .expect(200);

      adminAccessToken = res.body.data.accessToken;
    });
  });

  describe('2. User & Profile Management', () => {
    it('GET /api/v1/users/me -> should return current donor user profile', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/users/me')
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.email).toBe('donor.e2e@mealshare.org');
      expect(res.body.data.donorProfile).toBeDefined();
    });

    it('PUT /api/v1/donors/policy-agreement -> should record food safety policy acceptance', async () => {
      const res = await request(app.getHttpServer())
        .put('/api/v1/donors/policy-agreement')
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({
          agreeToPolicy: true,
          policyVersion: 'v1.0-2026',
        })
        .expect(200);

      expect(res.body.data.policyAccepted).toBe(true);
    });

    it('PUT /api/v1/donors/policy-agreement -> should reject if called by non-donor (403 Forbidden)', async () => {
      await request(app.getHttpServer())
        .put('/api/v1/donors/policy-agreement')
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .send({
          agreeToPolicy: true,
          policyVersion: 'v1.0-2026',
        })
        .expect(403);
    });
  });

  describe('3. Receiver Organization Verification & Admin Adjudication', () => {
    let verificationId: string;

    it('POST /api/v1/receivers/organizations/verification-docs -> should submit NGO verification docs', async () => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/receivers/organizations/verification-docs')
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .send({
          documentType: '80G Tax Exemption Certificate',
          documentUrl: 'https://storage.mealshare.org/evidence/80g.pdf',
        })
        .expect(201);

      expect(res.body.data.status).toBe('Pending');
      verificationId = res.body.data.verificationId;
    });

    it('GET /api/v1/admin/verifications -> should allow Admin to inspect pending queue', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/admin/verifications')
        .set('Authorization', `Bearer ${adminAccessToken}`)
        .expect(200);

      expect(res.body.data.length).toBeGreaterThan(0);
    });

    it('GET /api/v1/admin/verifications -> should block non-admins (403 Forbidden)', async () => {
      await request(app.getHttpServer())
        .get('/api/v1/admin/verifications')
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .expect(403);
    });

    it('POST /api/v1/admin/verifications/:id/adjudicate -> should approve NGO registration and activate user', async () => {
      const res = await request(app.getHttpServer())
        .post(`/api/v1/admin/verifications/${verificationId}/adjudicate`)
        .set('Authorization', `Bearer ${adminAccessToken}`)
        .send({
          decision: 'Verified',
          adminNotes: 'Documents verified and approved.',
        })
        .expect(201);

      expect(res.body.data.status).toBe('Verified');
    });
  });


  describe('4. Donations & Discovery Phase 2', () => {
    let donationId: string;

    it('POST /api/v1/donations -> reject invalid donation (past prep time)', async () => {
      const now = new Date();
      const futureDate = new Date(now.getTime() + 60000000); // far future
      const res = await request(app.getHttpServer())
        .post('/api/v1/donations')
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({
          foodType: 'Test',
          quantityServings: 10,
          prepDateTime: futureDate.toISOString(), // Invalid: prep is in the future
          storageCondition: StorageCondition.Refrigerated,
          consumptionDeadline: new Date(now.getTime() + 2 * 3600000).toISOString(),
          isVegetarian: true,
          pickupAddress: 'Address',
          pickupLatitude: 10.0,
          pickupLongitude: 20.0,
          foodPhotoUrl: 'http://test.com/photo.jpg',
        });
      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe('ERR-DON-001');
    });

    it('POST /api/v1/donations -> should create a valid donation', async () => {
      const now = new Date();
      const res = await request(app.getHttpServer())
        .post('/api/v1/donations')
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({
          foodType: 'Test Food',
          quantityServings: 15,
          prepDateTime: new Date(now.getTime() - 60000).toISOString(), // valid prep
          storageCondition: StorageCondition.Refrigerated,
          consumptionDeadline: new Date(now.getTime() + 2 * 3600000).toISOString(), // valid deadline
          isVegetarian: true,
          pickupAddress: 'Test Address 123',
          pickupLatitude: 12.0,
          pickupLongitude: 77.0,
          foodPhotoUrl: 'http://test.com/photo.jpg',
        });

      expect(res.status).toBe(201);

      expect(res.body.success).toBe(true);
      expect(res.body.data.status).toBe('Posted');
      donationId = res.body.data.donationId;
    });

    it('GET /api/v1/donations -> retrieve own donations', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/donations')
        .set('Authorization', `Bearer ${donorAccessToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0].id).toBe(donationId);
    });

    it('PUT /api/v1/donations/:id -> update donation', async () => {
      const res = await request(app.getHttpServer())
        .put(`/api/v1/donations/${donationId}`)
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({
          quantityServings: 20,
        });
      expect(res.status).toBe(200);
      expect(res.body.data.quantityServings).toBe(20);
    });

    it('GET /api/v1/donations/nearby -> receiver discovers nearby donations', async () => {
      // Need a valid receiver token. We have recvAccessToken from earlier tests.
      const res = await request(app.getHttpServer())
        .get('/api/v1/donations/nearby?latitude=12.001&longitude=77.001&radiusKm=10')
        .set('Authorization', `Bearer ${receiverAccessToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].donationId).toBe(donationId);
      expect(res.body.data[0].approximateZone).toBe('Masked Address Zone');
    });

    it('POST /api/v1/donations/:id/cancel -> donor cancels donation', async () => {
      const res = await request(app.getHttpServer())
        .post(`/api/v1/donations/${donationId}/cancel`)
        .set('Authorization', `Bearer ${donorAccessToken}`);
      expect(res.status).toBe(200);
    });

    it('GET /api/v1/donations/nearby -> cancelled donations are excluded', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/donations/nearby?latitude=12.001&longitude=77.001&radiusKm=10')
        .set('Authorization', `Bearer ${receiverAccessToken}`);
      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(0); // cancelled, so status is 'Cancelled', not 'Posted'
    });
  });


  describe('5. FCFS Requests & Donor Approval Phase 3', () => {
    let activeDonationId: string;
    let createdRequestId: string;

    beforeAll(async () => {
      // Create a fresh Posted donation for testing Phase 3
      const now = new Date();
      const res = await request(app.getHttpServer())
        .post('/api/v1/donations')
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({
          foodType: 'Hot Biryani & Curry',
          quantityServings: 40,
          prepDateTime: new Date(now.getTime() - 60000).toISOString(),
          storageCondition: StorageCondition.Heated_Container,
          consumptionDeadline: new Date(now.getTime() + 3 * 3600000).toISOString(),
          isVegetarian: true,
          pickupAddress: 'FCFS Test Kitchen, 45 Central Road',
          pickupLatitude: 12.9716,
          pickupLongitude: 77.5946,
          foodPhotoUrl: 'https://storage.mealshare.org/food.jpg',
        });
      activeDonationId = res.body.data.donationId;
    });

    it('POST /api/v1/donations/:id/requests -> receiver submits valid FCFS request', async () => {
      const res = await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests`)
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .send({
          notes: 'Can pickup in 20 minutes for local children shelter',
        })
        .expect(201);

      expect(res.body.success).toBe(true);
      expect(res.body.data.donationId).toBe(activeDonationId);
      expect(res.body.data.queuePosition).toBe(1);
      expect(res.body.data.status).toBe('Pending');
      createdRequestId = res.body.data.requestId;
    });

    it('POST /api/v1/donations/:id/requests -> duplicate request rejected (409 Conflict / ERR-REQ-001)', async () => {
      const res = await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests`)
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .send({
          notes: 'Duplicate request attempt',
        })
        .expect(409);

      expect(res.body.error.code).toBe('ERR-REQ-001');
    });

    it('POST /api/v1/donations/:id/requests -> donor cannot submit receiver request (403 Forbidden)', async () => {
      await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests`)
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({
          notes: 'Invalid role request',
        })
        .expect(403);
    });

    it('GET /api/v1/donations/:id/requests -> donor reviews FCFS queue with reputation metrics', async () => {
      const res = await request(app.getHttpServer())
        .get(`/api/v1/donations/${activeDonationId}/requests`)
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0].queuePosition).toBe(1);
      expect(res.body.data[0].receiver.name).toBe('Hope Children Shelter');
      expect(res.body.data[0].receiver.verificationStatus).toBe('Verified');
    });

    it('GET /api/v1/donations/requests/my-requests -> receiver lists submitted requests', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/donations/requests/my-requests')
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0].donationId).toBe(activeDonationId);
    });

    it('POST /api/v1/donations/:id/requests/:requestId/approve -> donor approves request, generating 6-digit OTP', async () => {
      const res = await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests/${createdRequestId}/approve`)
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.status).toBe('Approved');
      expect(res.body.data.handoverOtp).toBeDefined();
      expect(res.body.data.handoverOtp.length).toBe(6);
    });

    it('POST /api/v1/donations/:id/requests/:requestId/approve -> receiver cannot approve requests (403 Forbidden)', async () => {
      await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests/${createdRequestId}/approve`)
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .expect(403);
    });

    it('POST /api/v1/donations/:id/requests/:requestId/reject -> donor can reject request', async () => {
      // Create a second request to test rejection
      const newReq = {
        id: `req-reject-${Date.now()}`,
        donationId: activeDonationId,
        receiverId: 'temp-receiver-id',
        status: 'Pending',
        queueTimestamp: new Date(),
      };
      donationRequestsDb.set(newReq.id, newReq);

      const res = await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests/${newReq.id}/reject`)
        .set('Authorization', `Bearer ${donorAccessToken}`)
        .send({ rejectionReason: 'Operational constraints' })
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.status).toBe('Rejected');
    });

    it('POST /api/v1/donations/:id/requests/:requestId/withdraw -> receiver can withdraw pending request', async () => {
      // Find receiver user id from usersDb
      let recvId = '';
      for (const u of usersDb.values()) {
        if (u.email === 'shelter.e2e@mealshare.org') recvId = u.id;
      }

      // Create a pending request for receiver
      const withdrawReq = {
        id: `req-withdraw-${Date.now()}`,
        donationId: activeDonationId,
        receiverId: recvId,
        status: 'Pending',
        queueTimestamp: new Date(),
      };
      donationRequestsDb.set(withdrawReq.id, withdrawReq);

      const res = await request(app.getHttpServer())
        .post(`/api/v1/donations/${activeDonationId}/requests/${withdrawReq.id}/withdraw`)
        .set('Authorization', `Bearer ${receiverAccessToken}`)
        .expect(200);

      expect(res.body.success).toBe(true);
      expect(res.body.data.status).toBe('Withdrawn');
    });
  });
});

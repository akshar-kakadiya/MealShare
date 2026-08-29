const fs = require('fs');
const path = require('path');

const e2ePath = path.join(__dirname, 'test', 'app.e2e-spec.ts');
let content = fs.readFileSync(e2ePath, 'utf8');

// 1. Add donationsDb Map
content = content.replace(
  'const auditLogsDb: any[] = [];',
  'const auditLogsDb: any[] = [];\n  const donationsDb = new Map<string, any>();'
);

// 2. Add donation mock
const donationMock = `
    donation: {
      create: jest.fn(({ data }) => {
        const id = \`don-\${Date.now()}\`;
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
        const updated = { ...don, ...data, updatedAt: new Date() };
        donationsDb.set(where.id, updated);
        return Promise.resolve(updated);
      }),
    },
    $queryRaw: jest.fn(() => {
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
`;

content = content.replace(
  'organizationVerification: {',
  `${donationMock}\n    organizationVerification: {`
);

// 3. Append Tests
const tests = `

  describe('4. Donations & Discovery Phase 2', () => {
    let donationId: string;

    it('POST /api/v1/donations -> reject invalid donation (past prep time)', async () => {
      const now = new Date();
      const futureDate = new Date(now.getTime() + 60000000); // far future
      const res = await request(app.getHttpServer())
        .post('/api/v1/donations')
        .set('Authorization', \`Bearer \${donorAccessToken}\`)
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
      expect(res.body.error).toBe('ERR-DON-001');
    });

    it('POST /api/v1/donations -> should create a valid donation', async () => {
      const now = new Date();
      const res = await request(app.getHttpServer())
        .post('/api/v1/donations')
        .set('Authorization', \`Bearer \${donorAccessToken}\`)
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
        .set('Authorization', \`Bearer \${donorAccessToken}\`);
      expect(res.status).toBe(200);
      expect(res.body.data.length).toBeGreaterThan(0);
      expect(res.body.data[0].id).toBe(donationId);
    });

    it('PUT /api/v1/donations/:id -> update donation', async () => {
      const res = await request(app.getHttpServer())
        .put(\`/api/v1/donations/\${donationId}\`)
        .set('Authorization', \`Bearer \${donorAccessToken}\`)
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
        .set('Authorization', \`Bearer \${recvAccessToken}\`);
      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(1);
      expect(res.body.data[0].donationId).toBe(donationId);
      expect(res.body.data[0].approximateZone).toBe('Masked Address Zone');
    });

    it('POST /api/v1/donations/:id/cancel -> donor cancels donation', async () => {
      const res = await request(app.getHttpServer())
        .post(\`/api/v1/donations/\${donationId}/cancel\`)
        .set('Authorization', \`Bearer \${donorAccessToken}\`);
      expect(res.status).toBe(200);
    });

    it('GET /api/v1/donations/nearby -> cancelled donations are excluded', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/donations/nearby?latitude=12.001&longitude=77.001&radiusKm=10')
        .set('Authorization', \`Bearer \${recvAccessToken}\`);
      expect(res.status).toBe(200);
      expect(res.body.data.length).toBe(0); // cancelled, so status is 'Cancelled', not 'Posted'
    });
  });
});
`;

content = content.replace('});\n', tests);

// We need StorageCondition imported from @prisma/client
content = content.replace(
  'UserRole, UserStatus, DonorType, VerificationStatus',
  'UserRole, UserStatus, DonorType, VerificationStatus, StorageCondition'
);

fs.writeFileSync(e2ePath, content);
console.log('e2e tests patched.');

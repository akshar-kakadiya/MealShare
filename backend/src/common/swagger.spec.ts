import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from '../app.module';
import { PrismaService } from '../database/prisma.service';

describe('Swagger OpenAPI Specification (Security Scheme Verification)', () => {
  let app: INestApplication;

  const mockPrismaService = {
    $connect: jest.fn(),
    $disconnect: jest.fn(),
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
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('should generate OpenAPI document with JWT-auth security requirement on /api/v1/users/me and other protected routes', () => {
    const config = new DocumentBuilder()
      .setTitle('MealShare Surplus-Food Redistribution API')
      .addBearerAuth(
        {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          name: 'JWT',
          description: 'Enter JWT access token',
          in: 'header',
        },
        'JWT-auth',
      )
      .build();

    const document = SwaggerModule.createDocument(app, config);

    // 1. Verify components.securitySchemes has JWT-auth
    expect(document.components?.securitySchemes).toBeDefined();
    expect(document.components?.securitySchemes?.['JWT-auth']).toBeDefined();

    // 2. Verify /api/v1/users/me has security requirement [{ 'JWT-auth': [] }]
    const usersMePath = document.paths['/api/v1/users/me'];
    expect(usersMePath).toBeDefined();
    expect(usersMePath?.get).toBeDefined();
    expect(usersMePath?.get?.security).toEqual([{ 'JWT-auth': [] }]);

    // 3. Verify other protected endpoints
    const donorsProfilePath = document.paths['/api/v1/donors/profile'];
    expect(donorsProfilePath?.get?.security).toEqual([{ 'JWT-auth': [] }]);

    const adminVerifsPath = document.paths['/api/v1/admin/verifications'];
    expect(adminVerifsPath?.get?.security).toEqual([{ 'JWT-auth': [] }]);

    const authLogoutPath = document.paths['/api/v1/auth/logout'];
    expect(authLogoutPath?.post?.security).toEqual([{ 'JWT-auth': [] }]);
  });
});

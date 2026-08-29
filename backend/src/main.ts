import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const logger = new Logger('MealShareBootstrap');
  const app = await NestFactory.create(AppModule);

  // Enable CORS
  app.enableCors({
    origin: true,
    credentials: true,
  });

  // Global Prefix: /api/v1
  const globalPrefix = process.env.API_PREFIX || 'api/v1';
  app.setGlobalPrefix(globalPrefix);

  // Global Validation Pipe
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
      forbidNonWhitelisted: true,
      transformOptions: {
        enableImplicitConversion: true,
      },
    }),
  );

  // Swagger OpenAPI 3.0 Documentation
  const config = new DocumentBuilder()
    .setTitle('MealShare Surplus-Food Redistribution API')
    .setDescription(
      'REST & Real-Time API specification for MealShare platform based on PRD v1.0, SRS v1.0, and SADD v1.0',
    )
    .setVersion('1.0.0')
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
    .addTag('Auth', 'Authentication, Registration, and Token Lifecycle')
    .addTag('Users', 'User Profile & Identity Resolution')
    .addTag('Donors', 'Donor Profile Management & Food Policy Agreement')
    .addTag('Receivers', 'Receiver Organization Profile & Document Verification')
    .addTag('Volunteers', 'Volunteer Profile, Portfolio & Milestone Impact')
    .addTag('Admin', 'Administrative Oversight, Verifications & Account Sanctions')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document, {
    swaggerOptions: {
      persistAuthorization: true,
    },
    customSiteTitle: 'MealShare API Documentation',
  });

  const port = process.env.PORT || 3000;
  await app.listen(port);

  logger.log(`=======================================================`);
  logger.log(` MealShare API Server running at: http://localhost:${port}/${globalPrefix}`);
  logger.log(` Swagger OpenAPI Documentation: http://localhost:${port}/api/docs`);
  logger.log(`=======================================================`);
}

bootstrap();

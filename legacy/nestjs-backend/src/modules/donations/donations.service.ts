import { Injectable, NotFoundException, BadRequestException, ForbiddenException, Logger } from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';
import { CreateDonationDto } from './dto/create-donation.dto';
import { UpdateDonationDto } from './dto/update-donation.dto';
import { NearbyDonationsDto } from './dto/nearby-donations.dto';
import { UserRole, DonationStatus, Prisma } from '@prisma/client';

@Injectable()
export class DonationsService {
  private readonly logger = new Logger(DonationsService.name);

  constructor(private prisma: PrismaService) {}

  async create(userId: string, dto: CreateDonationDto) {
    const donorProfile = await this.prisma.donorProfile.findUnique({
      where: { userId },
    });

    if (!donorProfile) {
      throw new ForbiddenException({
        error: 'ERR-DON-003',
        message: 'Only registered donors can post donations.',
      });
    }

    if (!donorProfile.policyAccepted) {
      throw new ForbiddenException({
        error: 'ERR-DON-003',
        message: 'Food Donation Policy not accepted.',
      });
    }

    this.validateSafetyRules(dto);

    const donation = await this.prisma.donation.create({
      data: {
        donorId: donorProfile.id,
        foodType: dto.foodType,
        quantityServings: dto.quantityServings,
        prepDateTime: new Date(dto.prepDateTime),
        storageCondition: dto.storageCondition,
        consumptionDeadline: new Date(dto.consumptionDeadline),
        isVegetarian: dto.isVegetarian,
        allergens: dto.allergens || [],
        pickupAddress: dto.pickupAddress,
        pickupLatitude: dto.pickupLatitude,
        pickupLongitude: dto.pickupLongitude,
        foodPhotoUrl: dto.foodPhotoUrl,
        notes: dto.notes,
        status: DonationStatus.Posted, // Created and published
      },
    });

    await this.prisma.auditLog.create({
      data: {
        actorUserId: userId,
        actorRole: 'Donor',
        actionType: 'DONATION_CREATED_AND_PUBLISHED',
        entityName: 'Donation',
        entityId: donation.id,
      },
    });

    return {
      donationId: donation.id,
      status: donation.status,
      foodType: donation.foodType,
      quantityServings: donation.quantityServings,
      consumptionDeadline: donation.consumptionDeadline.toISOString(),
      eligibilityChecksPassed: true,
      createdAt: donation.createdAt.toISOString(),
    };
  }

  async findAll(userId: string) {
    const donorProfile = await this.prisma.donorProfile.findUnique({
      where: { userId },
    });

    if (!donorProfile) {
      throw new ForbiddenException('User is not a donor');
    }

    const donations = await this.prisma.donation.findMany({
      where: { donorId: donorProfile.id },
      orderBy: { createdAt: 'desc' },
    });

    return donations;
  }

  async findOne(id: string) {
    const donation = await this.prisma.donation.findUnique({
      where: { id },
      include: { donor: true },
    });

    if (!donation) {
      throw new NotFoundException('Donation not found');
    }

    return donation;
  }

  async update(id: string, userId: string, dto: UpdateDonationDto) {
    const donation = await this.prisma.donation.findUnique({
      where: { id },
      include: { donor: true },
    });

    if (!donation) {
      throw new NotFoundException('Donation not found');
    }

    if (donation.donor.userId !== userId) {
      throw new ForbiddenException('You are not authorized to modify this donation.');
    }

    if (donation.status !== DonationStatus.Draft && donation.status !== DonationStatus.Posted) {
      throw new BadRequestException(`Cannot modify donation in status: ${donation.status}`);
    }

    if (dto.prepDateTime || dto.consumptionDeadline) {
      // Validate updated dates
      const mockDto: any = {
        prepDateTime: dto.prepDateTime || donation.prepDateTime.toISOString(),
        consumptionDeadline: dto.consumptionDeadline || donation.consumptionDeadline.toISOString(),
      };
      this.validateSafetyRules(mockDto as CreateDonationDto);
    }

    const updated = await this.prisma.donation.update({
      where: { id },
      data: {
        foodType: dto.foodType,
        quantityServings: dto.quantityServings,
        prepDateTime: dto.prepDateTime ? new Date(dto.prepDateTime) : undefined,
        storageCondition: dto.storageCondition,
        consumptionDeadline: dto.consumptionDeadline ? new Date(dto.consumptionDeadline) : undefined,
        isVegetarian: dto.isVegetarian,
        allergens: dto.allergens,
        pickupAddress: dto.pickupAddress,
        pickupLatitude: dto.pickupLatitude,
        pickupLongitude: dto.pickupLongitude,
        foodPhotoUrl: dto.foodPhotoUrl,
        notes: dto.notes,
      },
    });

    await this.prisma.auditLog.create({
      data: {
        actorUserId: userId,
        actorRole: 'Donor',
        actionType: 'DONATION_MODIFIED',
        entityName: 'Donation',
        entityId: updated.id,
      },
    });

    return updated;
  }

  async cancel(id: string, userId: string) {
    const donation = await this.prisma.donation.findUnique({
      where: { id },
      include: { donor: true },
    });

    if (!donation) {
      throw new NotFoundException('Donation not found');
    }

    if (donation.donor.userId !== userId) {
      throw new ForbiddenException('You are not authorized to cancel this donation.');
    }

    const cancellableStatuses: DonationStatus[] = [
      DonationStatus.Draft,
      DonationStatus.Posted,
      DonationStatus.Requested,
      DonationStatus.Approved,
    ];

    if (!cancellableStatuses.includes(donation.status)) {
      throw new BadRequestException(`Cannot cancel donation in status: ${donation.status}`);
    }

    const updated = await this.prisma.donation.update({
      where: { id },
      data: { status: DonationStatus.Cancelled },
    });

    await this.prisma.auditLog.create({
      data: {
        actorUserId: userId,
        actorRole: 'Donor',
        actionType: 'DONATION_CANCELLED',
        entityName: 'Donation',
        entityId: updated.id,
      },
    });

    return {
      success: true,
      message: 'Donation successfully cancelled',
    };
  }

  async findNearby(query: NearbyDonationsDto) {
    const { latitude, longitude, radiusKm = 10, isVegetarian, page = 1, pageSize = 20 } = query;
    const offset = (page - 1) * pageSize;
    const radiusMeters = radiusKm * 1000;

    // Use raw PostGIS query to find donations within radius
    // Status must be 'Posted'
    // Consumption deadline must be in the future
    // Optional filter for isVegetarian
    const now = new Date();

    const donations = await this.prisma.$queryRaw<any[]>`
      SELECT 
        d.id as "donationId",
        d.food_type as "foodType",
        d.quantity_servings as "quantityServings",
        d.storage_condition as "storageCondition",
        d.consumption_deadline as "consumptionDeadline",
        d.is_vegetarian as "isVegetarian",
        d.allergens as "allergens",
        d.food_photo_url as "foodPhotoUrl",
        d.pickup_address as "pickupAddress",
        d.status as "status",
        ST_Distance(
          ST_SetSRID(ST_MakePoint(d.pickup_longitude, d.pickup_latitude), 4326)::geography,
          ST_SetSRID(ST_MakePoint(${longitude}, ${latitude}), 4326)::geography
        ) as "distanceMeters",
        p.entity_name as "entityName",
        p.type as "donorType",
        p.rating_average as "ratingAverage",
        p.completed_donations_count as "completedDonations"
      FROM donations d
      JOIN donor_profiles p ON d.donor_id = p.id
      WHERE d.status = 'Posted'
        AND d.consumption_deadline > ${now}
        AND ST_DWithin(
          ST_SetSRID(ST_MakePoint(d.pickup_longitude, d.pickup_latitude), 4326)::geography,
          ST_SetSRID(ST_MakePoint(${longitude}, ${latitude}), 4326)::geography,
          ${radiusMeters}
        )
        ${isVegetarian !== undefined ? Prisma.sql`AND d.is_vegetarian = ${isVegetarian}` : Prisma.sql``}
      ORDER BY "distanceMeters" ASC
      LIMIT ${pageSize} OFFSET ${offset}
    `;

    // Format response and mask pickup address
    const data = donations.map((d) => {
      // Simple mask: returning approximate zone instead of full address
      const parts = d.pickupAddress ? d.pickupAddress.split(',') : [];
      const approximateZone = parts.length > 1 ? parts.slice(1).join(',').trim() : 'Masked Address Zone';

      return {
        donationId: d.donationId,
        foodType: d.foodType,
        quantityServings: d.quantityServings,
        storageCondition: d.storageCondition,
        consumptionDeadline: d.consumptionDeadline,
        isVegetarian: d.isVegetarian,
        allergens: d.allergens,
        foodPhotoUrl: d.foodPhotoUrl,
        approximateZone,
        distanceMeters: Math.round(Number(d.distanceMeters) * 10) / 10,
        status: d.status,
        donor: {
          entityName: d.entityName,
          donorType: d.donorType,
          ratingAverage: d.ratingAverage,
          completedDonations: d.completedDonations,
        },
      };
    });

    return {
      success: true,
      data,
      meta: {
        page,
        pageSize,
        count: data.length,
      },
    };
  }

  private validateSafetyRules(dto: CreateDonationDto) {
    const now = new Date();
    
    // prepDateTime <= UTC_NOW() + 5 mins
    const prepLimit = new Date(now.getTime() + 5 * 60000);
    if (new Date(dto.prepDateTime) > prepLimit) {
      throw new BadRequestException({
        error: 'ERR-DON-001',
        message: 'Preparation time cannot be in the future.',
      });
    }

    // consumptionDeadline > UTC_NOW() + 30 mins
    const deadlineLimit = new Date(now.getTime() + 30 * 60000);
    if (new Date(dto.consumptionDeadline) <= deadlineLimit) {
      throw new BadRequestException({
        error: 'ERR-DON-001',
        message: 'Consumption deadline must be at least 30 minutes in the future.',
      });
    }
  }
}

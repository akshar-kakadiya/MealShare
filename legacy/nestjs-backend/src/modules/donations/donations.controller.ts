import { Controller, Get, Post, Put, Body, Param, Query, UseGuards, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { DonationsService } from './donations.service';
import { CreateDonationDto } from './dto/create-donation.dto';
import { UpdateDonationDto } from './dto/update-donation.dto';
import { NearbyDonationsDto } from './dto/nearby-donations.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { UserRole } from '@prisma/client';

@ApiTags('Donations')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('donations')
export class DonationsController {
  constructor(private readonly donationsService: DonationsService) {}

  @Post()
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'Creates and publishes a surplus food listing with mandatory safety metadata' })
  @ApiResponse({ status: 201, description: 'Donation created and published' })
  async create(
    @CurrentUser('id') userId: string,
    @Body() dto: CreateDonationDto,
  ) {
    return this.donationsService.create(userId, dto);
  }

  @Get()
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'List all donations created by the current donor' })
  @ApiResponse({ status: 200, description: 'List of donations' })
  async findAll(@CurrentUser('id') userId: string) {
    return this.donationsService.findAll(userId);
  }

  @Get('nearby')
  @Roles(UserRole.Receiver_Org, UserRole.Receiver_Vol, UserRole.Admin)
  @ApiOperation({ summary: 'Spatial radius query discovering active surplus food within receiver operating radius' })
  @ApiResponse({ status: 200, description: 'List of nearby available donations' })
  async findNearby(@Query() query: NearbyDonationsDto) {
    return this.donationsService.findNearby(query);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Retrieve specific donation details' })
  @ApiResponse({ status: 200, description: 'Donation details' })
  async findOne(@Param('id') id: string) {
    return this.donationsService.findOne(id);
  }

  @Put(':id')
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'Update an existing donation listing' })
  @ApiResponse({ status: 200, description: 'Donation updated' })
  async update(
    @Param('id') id: string,
    @CurrentUser('id') userId: string,
    @Body() dto: UpdateDonationDto,
  ) {
    return this.donationsService.update(id, userId, dto);
  }

  @Post(':id/cancel')
  @Roles(UserRole.Donor)
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Cancel an active or pending donation' })
  @ApiResponse({ status: 200, description: 'Donation cancelled' })
  async cancel(
    @Param('id') id: string,
    @CurrentUser('id') userId: string,
  ) {
    return this.donationsService.cancel(id, userId);
  }
}

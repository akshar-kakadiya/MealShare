import { Controller, Get, Put, Body, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { DonorsService } from './donors.service';
import { UpdateDonorProfileDto } from './dto/update-donor-profile.dto';
import { PolicyAgreementDto } from './dto/policy-agreement.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { UserRole } from '@prisma/client';

@ApiTags('Donors')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('donors')
export class DonorsController {
  constructor(private readonly donorsService: DonorsService) {}

  @Get('profile')
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'Retrieve donor profile and activity stats' })
  @ApiResponse({ status: 200, description: 'Donor profile returned' })
  @ApiResponse({ status: 403, description: 'Forbidden: Requires Donor role' })
  async getProfile(@CurrentUser('id') userId: string) {
    return this.donorsService.getProfile(userId);
  }

  @Put('profile')
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'Update donor organization/individual metadata & address' })
  @ApiResponse({ status: 200, description: 'Donor profile updated' })
  async updateProfile(
    @CurrentUser('id') userId: string,
    @Body() dto: UpdateDonorProfileDto,
  ) {
    return this.donorsService.updateProfile(userId, dto);
  }

  @Put('policy-agreement')
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'Sign and record agreement to the Food Donation Safety Policy' })
  @ApiResponse({ status: 200, description: 'Policy agreement recorded' })
  @ApiResponse({ status: 400, description: 'Policy agreement was false' })
  async acceptPolicy(
    @CurrentUser('id') userId: string,
    @Body() dto: PolicyAgreementDto,
  ) {
    return this.donorsService.acceptPolicy(userId, dto);
  }
}

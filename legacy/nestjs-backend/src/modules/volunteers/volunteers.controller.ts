import { Controller, Get, Put, Body, Param, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { VolunteersService } from './volunteers.service';
import { UpdateVolunteerProfileDto } from './dto/update-volunteer-profile.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { Public } from '../../common/decorators/public.decorator';
import { UserRole } from '@prisma/client';

@ApiTags('Volunteers')
@Controller('volunteers')
export class VolunteersController {
  constructor(private readonly volunteersService: VolunteersService) {}

  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Get('profile')
  @Roles(UserRole.Receiver_Vol)
  @ApiOperation({ summary: 'Retrieve volunteer personal profile, service zone, and stats' })
  @ApiResponse({ status: 200, description: 'Volunteer profile returned' })
  async getProfile(@CurrentUser('id') userId: string) {
    return this.volunteersService.getProfile(userId);
  }

  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Put('profile')
  @Roles(UserRole.Receiver_Vol)
  @ApiOperation({ summary: 'Update volunteer personal details, bio, and service radius' })
  @ApiResponse({ status: 200, description: 'Volunteer profile updated' })
  async updateProfile(
    @CurrentUser('id') userId: string,
    @Body() dto: UpdateVolunteerProfileDto,
  ) {
    return this.volunteersService.updateProfile(userId, dto);
  }

  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Get('me/impact')
  @Roles(UserRole.Receiver_Vol)
  @ApiOperation({ summary: 'Retrieve detailed personal delivery impact and milestones' })
  @ApiResponse({ status: 200, description: 'Personal impact stats returned' })
  async getMyImpact(@CurrentUser('id') userId: string) {
    return this.volunteersService.getMyImpact(userId);
  }

  @Public()
  @Get(':id/portfolio')
  @ApiOperation({ summary: 'Public volunteer impact portfolio with milestone certificates' })
  @ApiResponse({ status: 200, description: 'Public portfolio returned' })
  async getPublicPortfolio(@Param('id') id: string) {
    return this.volunteersService.getPublicPortfolio(id);
  }
}

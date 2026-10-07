import { Controller, Get, Put, Post, Body, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { ReceiversService } from './receivers.service';
import { UpdateReceiverProfileDto } from './dto/update-receiver-profile.dto';
import { UploadVerificationDocDto } from './dto/upload-verification-doc.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { UserRole } from '@prisma/client';

@ApiTags('Receivers')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('receivers/organizations')
export class ReceiversController {
  constructor(private readonly receiversService: ReceiversService) {}

  @Get('profile')
  @Roles(UserRole.Receiver_Org)
  @ApiOperation({ summary: 'Retrieve receiver organization profile, capacity, and verification status' })
  @ApiResponse({ status: 200, description: 'Organization profile returned' })
  async getProfile(@CurrentUser('id') userId: string) {
    return this.receiversService.getProfile(userId);
  }

  @Put('profile')
  @Roles(UserRole.Receiver_Org)
  @ApiOperation({ summary: 'Update receiver organization details, capacity, and operating area' })
  @ApiResponse({ status: 200, description: 'Organization profile updated' })
  async updateProfile(
    @CurrentUser('id') userId: string,
    @Body() dto: UpdateReceiverProfileDto,
  ) {
    return this.receiversService.updateProfile(userId, dto);
  }

  @Post('verification-docs')
  @Roles(UserRole.Receiver_Org)
  @ApiOperation({ summary: 'Upload NGO/Shelter legal registration proof for admin adjudication' })
  @ApiResponse({ status: 201, description: 'Verification documents queued for admin review' })
  async uploadDocs(
    @CurrentUser('id') userId: string,
    @Body() dto: UploadVerificationDocDto,
  ) {
    return this.receiversService.submitVerificationDocs(userId, dto);
  }
}

import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { AdjudicateVerificationDto } from './dto/adjudicate-verification.dto';
import { ApplySanctionDto } from './dto/apply-sanction.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { UserRole } from '@prisma/client';

@ApiTags('Admin')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.Admin)
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('verifications')
  @ApiOperation({ summary: 'List all pending NGO registration verification requests' })
  @ApiResponse({ status: 200, description: 'Verification queue returned' })
  async listPendingVerifications() {
    return this.adminService.listPendingVerifications();
  }

  @Post('verifications/:id/adjudicate')
  @ApiOperation({ summary: 'Approve or reject organization registration documents' })
  @ApiResponse({ status: 200, description: 'Verification decision recorded' })
  async adjudicateVerification(
    @Param('id') verificationId: string,
    @CurrentUser('id') adminUserId: string,
    @Body() dto: AdjudicateVerificationDto,
  ) {
    return this.adminService.adjudicateVerification(verificationId, adminUserId, dto);
  }

  @Post('users/:id/sanctions')
  @ApiOperation({ summary: 'Apply administrative sanctions (Warning, Restricted, Suspended) on user account' })
  @ApiResponse({ status: 200, description: 'Sanction applied and recorded in audit log' })
  async applySanction(
    @Param('id') targetUserId: string,
    @CurrentUser('id') adminUserId: string,
    @Body() dto: ApplySanctionDto,
  ) {
    return this.adminService.applyUserSanction(targetUserId, adminUserId, dto);
  }

  @Get('audit-logs')
  @ApiOperation({ summary: 'Retrieve system-wide audit trail with delta snapshots' })
  @ApiResponse({ status: 200, description: 'Audit logs returned' })
  async getAuditLogs(
    @Query('page') page = 1,
    @Query('pageSize') pageSize = 20,
  ) {
    return this.adminService.getAuditLogs(+page, +pageSize);
  }
}

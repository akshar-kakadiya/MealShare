import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiParam,
} from '@nestjs/swagger';
import { RequestsService } from './requests.service';
import { CreateDonationRequestDto } from './dto/create-request.dto';
import { RejectDonationRequestDto } from './dto/reject-request.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { UserRole } from '@prisma/client';

@ApiTags('Requests')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('donations')
export class RequestsController {
  constructor(private readonly requestsService: RequestsService) {}

  @Get('requests/my-requests')
  @Roles(UserRole.Receiver_Org, UserRole.Receiver_Vol)
  @ApiOperation({ summary: 'List all donation requests submitted by the authenticated receiver' })
  @ApiResponse({ status: 200, description: 'List of submitted requests' })
  async getMyRequests(@CurrentUser('id') userId: string) {
    return this.requestsService.getMyRequests(userId);
  }

  @Post(':id/requests')
  @Roles(UserRole.Receiver_Org, UserRole.Receiver_Vol)
  @ApiOperation({ summary: 'Submit FCFS timestamped request for a donation listing' })
  @ApiParam({ name: 'id', description: 'Donation UUID' })
  @ApiResponse({ status: 201, description: 'Request enqueued in FCFS sequence' })
  @ApiResponse({ status: 400, description: 'Donation expired (ERR-REQ-002)' })
  @ApiResponse({ status: 409, description: 'Duplicate request (ERR-REQ-001) or donation unavailable (ERR-REQ-003)' })
  async createRequest(
    @Param('id') donationId: string,
    @CurrentUser('id') userId: string,
    @Body() dto: CreateDonationRequestDto,
  ) {
    return this.requestsService.enqueueRequest(donationId, userId, dto);
  }

  @Get(':id/requests')
  @Roles(UserRole.Donor)
  @ApiOperation({ summary: 'Donor inspects the FCFS-ordered queue of requesters' })
  @ApiParam({ name: 'id', description: 'Donation UUID' })
  @ApiResponse({ status: 200, description: 'List of prospective requesters in FCFS order' })
  async getQueue(
    @Param('id') donationId: string,
    @CurrentUser('id') userId: string,
  ) {
    return this.requestsService.getQueueForDonation(donationId, userId);
  }

  @Post(':id/requests/:requestId/approve')
  @Roles(UserRole.Donor)
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Donor approves specific receiver request and generates Handover OTP' })
  @ApiParam({ name: 'id', description: 'Donation UUID' })
  @ApiParam({ name: 'requestId', description: 'DonationRequest UUID' })
  @ApiResponse({ status: 200, description: 'Receiver approved; OTP generated and returned to donor' })
  @ApiResponse({ status: 404, description: 'Donation or Request not found' })
  @ApiResponse({ status: 409, description: 'Donation is not in a reviewable request state' })
  async approveRequest(
    @Param('id') donationId: string,
    @Param('requestId') requestId: string,
    @CurrentUser('id') userId: string,
  ) {
    return this.requestsService.approveRequest(donationId, requestId, userId);
  }

  @Post(':id/requests/:requestId/reject')
  @Roles(UserRole.Donor)
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Donor rejects specific receiver request' })
  @ApiParam({ name: 'id', description: 'Donation UUID' })
  @ApiParam({ name: 'requestId', description: 'DonationRequest UUID' })
  @ApiResponse({ status: 200, description: 'Request rejected; donation status updated' })
  async rejectRequest(
    @Param('id') donationId: string,
    @Param('requestId') requestId: string,
    @CurrentUser('id') userId: string,
    @Body() dto: RejectDonationRequestDto,
  ) {
    return this.requestsService.rejectRequest(donationId, requestId, userId, dto);
  }

  @Post(':id/requests/:requestId/withdraw')
  @Roles(UserRole.Receiver_Org, UserRole.Receiver_Vol)
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Receiver withdraws own pending request prior to donor decision' })
  @ApiParam({ name: 'id', description: 'Donation UUID' })
  @ApiParam({ name: 'requestId', description: 'DonationRequest UUID' })
  @ApiResponse({ status: 200, description: 'Request marked as Withdrawn' })
  async withdrawRequest(
    @Param('id') donationId: string,
    @Param('requestId') requestId: string,
    @CurrentUser('id') userId: string,
  ) {
    return this.requestsService.withdrawRequest(donationId, requestId, userId);
  }
}

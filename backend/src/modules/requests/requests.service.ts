import {
  Injectable,
  NotFoundException,
  ForbiddenException,
  BadRequestException,
  ConflictException,
} from '@nestjs/common';
import { PrismaService } from '../../database/prisma.service';
import { CreateDonationRequestDto } from './dto/create-request.dto';
import { RejectDonationRequestDto } from './dto/reject-request.dto';
import { DonationStatus, RequestStatus, UserRole } from '@prisma/client';
import * as crypto from 'crypto';

@Injectable()
export class RequestsService {
  constructor(private readonly prisma: PrismaService) {}

  /**
   * Submits a server-timestamped FCFS request for a surplus food listing (FR-DISC-002).
   * Enforces row-level locking, duplicate request prevention, and active eligibility.
   */
  async enqueueRequest(
    donationId: string,
    receiverUserId: string,
    dto: CreateDonationRequestDto,
  ) {
    return await this.prisma.$transaction(async (tx) => {
      // 1. Verify receiver profile / eligibility
      const user = await tx.user.findUnique({
        where: { id: receiverUserId },
        include: { receiverOrg: true, volunteerProfile: true },
      });

      if (!user) {
        throw new NotFoundException('User not found');
      }

      if (user.role !== UserRole.Receiver_Org && user.role !== UserRole.Receiver_Vol) {
        throw new ForbiddenException('Only verified receivers or volunteers may submit requests.');
      }

      // 2. Lock donation record with row-level lock (FOR UPDATE)
      const donations = await tx.$queryRaw<any[]>`
        SELECT id, donor_id as "donorId", status, consumption_deadline as "consumptionDeadline"
        FROM donations
        WHERE id = ${donationId}
        FOR UPDATE
      `;

      const donation = donations[0];
      if (!donation) {
        throw new NotFoundException('Donation not found');
      }

      if (
        donation.status !== DonationStatus.Posted &&
        donation.status !== DonationStatus.Requested
      ) {
        throw new ConflictException({
          code: 'ERR-REQ-003',
          message: `Donation is not available for requesting (current status: ${donation.status}).`,
        });
      }

      if (new Date() > new Date(donation.consumptionDeadline)) {
        throw new BadRequestException({
          code: 'ERR-REQ-002',
          message: 'Donation consumption deadline has passed.',
        });
      }

      // 3. Prevent duplicate active request from same receiver
      const existingRequest = await tx.donationRequest.findFirst({
        where: {
          donationId,
          receiverId: receiverUserId,
          status: { in: [RequestStatus.Pending, RequestStatus.Accepted] },
        },
      });

      if (existingRequest) {
        throw new ConflictException({
          code: 'ERR-REQ-001',
          message: 'User has already submitted an active request for this donation.',
        });
      }

      // 4. Create donation request record with server monotonic timestamp
      const serverTimestamp = new Date();
      const request = await tx.donationRequest.create({
        data: {
          donationId,
          receiverId: receiverUserId,
          notes: dto?.notes,
          status: RequestStatus.Pending,
          queueTimestamp: serverTimestamp,
        },
      });

      // 5. Update donation status from Posted -> Requested
      if (donation.status === DonationStatus.Posted) {
        await tx.donation.update({
          where: { id: donationId },
          data: { status: DonationStatus.Requested },
        });
      }

      // 6. Calculate deterministic queue position
      const queuePosition = await tx.donationRequest.count({
        where: {
          donationId,
          status: RequestStatus.Pending,
          queueTimestamp: { lte: request.queueTimestamp },
        },
      });

      // 7. Dispatch notification to donor
      const donorProfile = await tx.donorProfile.findUnique({
        where: { id: donation.donorId },
        select: { userId: true },
      });

      if (donorProfile) {
        await tx.notification.create({
          data: {
            userId: donorProfile.userId,
            title: 'New Donation Request',
            message: 'A receiver has requested your surplus food donation.',
            eventType: 'DONATION_REQUEST_RECEIVED',
            referenceId: donationId,
          },
        });
      }

      // 8. Record audit log
      await tx.auditLog.create({
        data: {
          actorUserId: receiverUserId,
          actorRole: user.role,
          actionType: 'REQUEST_SUBMITTED',
          entityName: 'DonationRequest',
          entityId: request.id,
        },
      });

      return {
        requestId: request.id,
        donationId,
        queuePosition,
        queueTimestamp: request.queueTimestamp.toISOString(),
        status: request.status,
      };
    });
  }

  /**
   * Retrieves the FCFS-ordered queue of requesters for a donor's donation (FR-APPR-001).
   */
  async getQueueForDonation(donationId: string, donorUserId: string) {
    const donation = await this.prisma.donation.findUnique({
      where: { id: donationId },
      include: { donor: true },
    });

    if (!donation) {
      throw new NotFoundException('Donation not found');
    }

    if (donation.donor.userId !== donorUserId) {
      throw new ForbiddenException('You are not authorized to view requests for this donation.');
    }

    const requests = await this.prisma.donationRequest.findMany({
      where: { donationId },
      orderBy: { queueTimestamp: 'asc' },
      include: {
        receiver: {
          include: {
            receiverOrg: true,
            volunteerProfile: true,
          },
        },
      },
    });

    return requests.map((req, index) => {
      const org = req.receiver.receiverOrg;
      const vol = req.receiver.volunteerProfile;
      const isOrg = !!org;

      return {
        requestId: req.id,
        queuePosition: index + 1,
        queueTimestamp: req.queueTimestamp.toISOString(),
        status: req.status,
        notes: req.notes,
        receiver: {
          id: req.receiver.id,
          type: isOrg ? 'Organization' : 'Volunteer',
          name: isOrg ? org.orgName : vol?.fullName || 'Volunteer',
          orgType: isOrg ? org.type : undefined,
          verificationStatus: isOrg
            ? org.verificationStatus
            : 'Verified',
          ratingAverage: isOrg ? org.ratingAverage : vol?.ratingAverage ?? 5.0,
          beneficiaryCapacity: isOrg ? org.beneficiaryCapacity : undefined,
          completedDonationsCount: isOrg
            ? 0
            : vol?.completedDeliveriesCount ?? 0,
        },
      };
    });
  }

  /**
   * Retrieves all requests submitted by the current authenticated receiver.
   */
  async getMyRequests(receiverUserId: string) {
    const requests = await this.prisma.donationRequest.findMany({
      where: { receiverId: receiverUserId },
      orderBy: { queueTimestamp: 'desc' },
      include: {
        donation: {
          include: {
            donor: true,
          },
        },
      },
    });

    return requests.map((req) => ({
      requestId: req.id,
      donationId: req.donationId,
      status: req.status,
      queueTimestamp: req.queueTimestamp.toISOString(),
      notes: req.notes,
      donation: {
        id: req.donation.id,
        foodType: req.donation.foodType,
        quantityServings: req.donation.quantityServings,
        status: req.donation.status,
        consumptionDeadline: req.donation.consumptionDeadline.toISOString(),
        pickupAddress: req.donation.pickupAddress,
        donorName: req.donation.donor.entityName,
      },
    }));
  }

  /**
   * Donor approves a specific receiver from the queue, generating the 6-digit Handover OTP (FR-APPR-002).
   */
  async approveRequest(donationId: string, requestId: string, donorUserId: string) {
    return await this.prisma.$transaction(async (tx) => {
      // 1. Lock donation row and verify donor ownership
      const donations = await tx.$queryRaw<any[]>`
        SELECT d.id, d.donor_id as "donorId", d.status, dp.user_id as "donorUserId"
        FROM donations d
        JOIN donor_profiles dp ON d.donor_id = dp.id
        WHERE d.id = ${donationId}
        FOR UPDATE OF d
      `;

      const donation = donations[0];
      if (!donation) {
        throw new NotFoundException('Donation not found');
      }

      if (donation.donorUserId !== donorUserId) {
        throw new ForbiddenException('You are not authorized to approve requests for this donation.');
      }

      if (
        donation.status !== DonationStatus.Requested &&
        donation.status !== DonationStatus.Posted
      ) {
        throw new ConflictException(
          `Donation is not in a reviewable request state (current status: ${donation.status}).`,
        );
      }

      // 2. Lock and retrieve target request
      const targetRequest = await tx.donationRequest.findFirst({
        where: {
          id: requestId,
          donationId,
          status: RequestStatus.Pending,
        },
      });

      if (!targetRequest) {
        throw new NotFoundException('Selected request is no longer valid or pending.');
      }

      // 3. Generate secure 6-digit CSPRNG OTP & SHA-256 hash
      const rawOtp = crypto.randomInt(100000, 1000000).toString();
      const otpHash = crypto.createHash('sha256').update(rawOtp).digest('hex');
      const otpExpiry = new Date(Date.now() + 60 * 60 * 1000); // 60 minutes expiry

      // 4. Update target request to Accepted
      await tx.donationRequest.update({
        where: { id: requestId },
        data: { status: RequestStatus.Accepted },
      });

      // 5. Update donation to Approved with security metadata
      await tx.donation.update({
        where: { id: donationId },
        data: {
          status: DonationStatus.Approved,
          acceptedReceiverId: targetRequest.receiverId,
          handoverOtpHash: otpHash,
          otpExpiresAt: otpExpiry,
          failedOtpAttempts: 0,
        },
      });

      // 6. Dispatch approval notification to accepted receiver
      await tx.notification.create({
        data: {
          userId: targetRequest.receiverId,
          title: 'Donation Request Approved',
          message: 'Your request for food donation has been approved by the donor. Please proceed to pickup.',
          eventType: 'DONATION_REQUEST_APPROVED',
          referenceId: donationId,
        },
      });

      // 7. Record audit log
      await tx.auditLog.create({
        data: {
          actorUserId: donorUserId,
          actorRole: 'Donor',
          actionType: 'DONATION_REQUEST_APPROVED',
          entityName: 'Donation',
          entityId: donationId,
        },
      });

      return {
        donationId,
        status: 'Approved',
        acceptedReceiverId: targetRequest.receiverId,
        handoverOtp: rawOtp,
        otpExpiresAt: otpExpiry.toISOString(),
        message: 'Receiver approved. Share the 6-digit OTP only after on-site food inspection.',
      };
    });
  }

  /**
   * Donor rejects a specific receiver request (FR-APPR-002).
   * If zero pending requests remain, transitions donation status back to Posted.
   */
  async rejectRequest(
    donationId: string,
    requestId: string,
    donorUserId: string,
    dto?: RejectDonationRequestDto,
  ) {
    return await this.prisma.$transaction(async (tx) => {
      // 1. Lock donation and verify donor ownership
      const donations = await tx.$queryRaw<any[]>`
        SELECT d.id, d.donor_id as "donorId", d.status, dp.user_id as "donorUserId"
        FROM donations d
        JOIN donor_profiles dp ON d.donor_id = dp.id
        WHERE d.id = ${donationId}
        FOR UPDATE OF d
      `;

      const donation = donations[0];
      if (!donation) {
        throw new NotFoundException('Donation not found');
      }

      if (donation.donorUserId !== donorUserId) {
        throw new ForbiddenException('You are not authorized to reject requests for this donation.');
      }

      // 2. Lock target request
      const targetRequest = await tx.donationRequest.findFirst({
        where: {
          id: requestId,
          donationId,
          status: RequestStatus.Pending,
        },
      });

      if (!targetRequest) {
        throw new NotFoundException('Selected request is no longer pending or found.');
      }

      // 3. Mark request as Rejected
      await tx.donationRequest.update({
        where: { id: requestId },
        data: { status: RequestStatus.Rejected },
      });

      // 4. Check if any remaining pending requests exist
      const remainingPending = await tx.donationRequest.count({
        where: {
          donationId,
          status: RequestStatus.Pending,
        },
      });

      let resultingDonationStatus = donation.status;
      if (remainingPending === 0 && donation.status === DonationStatus.Requested) {
        await tx.donation.update({
          where: { id: donationId },
          data: { status: DonationStatus.Posted },
        });
        resultingDonationStatus = DonationStatus.Posted;
      }

      // 5. Notify receiver of polite rejection
      await tx.notification.create({
        data: {
          userId: targetRequest.receiverId,
          title: 'Donation Request Rejected',
          message: dto?.rejectionReason
            ? `Your request was declined: ${dto.rejectionReason}`
            : 'Your request for food donation was declined by the donor.',
          eventType: 'DONATION_REQUEST_REJECTED',
          referenceId: donationId,
        },
      });

      // 6. Record audit log
      await tx.auditLog.create({
        data: {
          actorUserId: donorUserId,
          actorRole: 'Donor',
          actionType: 'DONATION_REQUEST_REJECTED',
          entityName: 'DonationRequest',
          entityId: requestId,
        },
      });

      return {
        requestId,
        status: 'Rejected',
        donationStatus: resultingDonationStatus,
        message: 'Request rejected successfully.',
      };
    });
  }

  /**
   * Receiver withdraws their own pending request prior to donor decision (BR-DISC-05).
   */
  async withdrawRequest(
    donationId: string,
    requestId: string,
    receiverUserId: string,
  ) {
    return await this.prisma.$transaction(async (tx) => {
      // 1. Retrieve request
      const request = await tx.donationRequest.findFirst({
        where: {
          id: requestId,
          donationId,
          receiverId: receiverUserId,
          status: RequestStatus.Pending,
        },
      });

      if (!request) {
        throw new NotFoundException('Pending request not found or unauthorized.');
      }

      // 2. Mark request as Withdrawn
      await tx.donationRequest.update({
        where: { id: requestId },
        data: { status: RequestStatus.Withdrawn },
      });

      // 3. If no other pending requests exist, revert donation from Requested -> Posted
      const remainingPending = await tx.donationRequest.count({
        where: {
          donationId,
          status: RequestStatus.Pending,
        },
      });

      if (remainingPending === 0) {
        const donation = await tx.donation.findUnique({ where: { id: donationId } });
        if (donation && donation.status === DonationStatus.Requested) {
          await tx.donation.update({
            where: { id: donationId },
            data: { status: DonationStatus.Posted },
          });
        }
      }

      // 4. Record audit log
      await tx.auditLog.create({
        data: {
          actorUserId: receiverUserId,
          actorRole: 'Receiver',
          actionType: 'REQUEST_WITHDRAWN',
          entityName: 'DonationRequest',
          entityId: requestId,
        },
      });

      return {
        requestId,
        status: 'Withdrawn',
        message: 'Request withdrawn successfully.',
      };
    });
  }
}

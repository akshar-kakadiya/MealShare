package com.mealshare.pickup;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.notification.event.DonationCompletedEvent;
import com.mealshare.notification.event.OtpVerifiedEvent;
import com.mealshare.notification.event.PickupCompletedEvent;
import com.mealshare.notification.event.PickupScheduledEvent;
import com.mealshare.pickup.dto.AdminAssignPickupResponse;
import com.mealshare.pickup.dto.AssignPickupRequest;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.pickup.otp.PickupOtpService;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.pickup.dto.DonorPickupOtpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PickupService {

    private final PickupRepository pickupRepository;
    private final DonationRequestRepository donationRequestRepository;
    private final DonationRepository donationRepository;
    private final ReceiverOrganizationRepository receiverOrganizationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final PickupOtpService pickupOtpService;
    private final AuditService auditService;
    private ApplicationEventPublisher eventPublisher;

    public PickupService(PickupRepository pickupRepository,
                         DonationRequestRepository donationRequestRepository,
                         DonationRepository donationRepository,
                         ReceiverOrganizationRepository receiverOrganizationRepository,
                         DonorProfileRepository donorProfileRepository,
                         UserRepository userRepository,
                         PickupOtpService pickupOtpService,
                         AuditService auditService) {
        this.pickupRepository = pickupRepository;
        this.donationRequestRepository = donationRequestRepository;
        this.donationRepository = donationRepository;
        this.receiverOrganizationRepository = receiverOrganizationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.pickupOtpService = pickupOtpService;
        this.auditService = auditService;
    }

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AdminAssignPickupResponse schedulePickup(String adminFirebaseUid,
                                                     AssignPickupRequest requestDto,
                                                     HttpServletRequest httpRequest) {
        User adminUser = validateAndGetAdminUser(adminFirebaseUid);

        DonationRequest request = donationRequestRepository.findById(requestDto.donationRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestDto.donationRequestId()));

        if (request.getStatus() != DonationRequestStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED donation requests can be assigned a pickup. Current status: " + request.getStatus());
        }

        if (pickupRepository.existsByDonationRequestId(request.getId())) {
            throw new IllegalStateException("A pickup has already been assigned for this donation request.");
        }

        Donation donation = request.getDonation();
        if (donation == null) {
            throw new ResourceNotFoundException("Associated donation not found for request.");
        }

        if (donation.getStatus() == DonationStatus.CANCELLED ||
            donation.getStatus() == DonationStatus.EXPIRED ||
            donation.getStatus() == DonationStatus.COMPLETED) {
            throw new IllegalStateException("Donation is not in an assignable state. Current status: " + donation.getStatus());
        }

        if (donation.getConsumeBefore() != null && donation.getConsumeBefore().isBefore(Instant.now())) {
            throw new IllegalStateException("Donation has passed its consume-before deadline.");
        }

        if (requestDto.scheduledAt() == null) {
            throw new IllegalArgumentException("scheduledAt must not be null.");
        }

        if (requestDto.scheduledAt().isBefore(Instant.now().minusSeconds(60))) {
            throw new IllegalArgumentException("scheduledAt cannot be in the past.");
        }

        if (donation.getPickupAvailableFrom() != null && requestDto.scheduledAt().isBefore(donation.getPickupAvailableFrom())) {
            throw new IllegalArgumentException("scheduledAt cannot be before the donation pickup available from time.");
        }

        if (donation.getPickupAvailableUntil() != null && requestDto.scheduledAt().isAfter(donation.getPickupAvailableUntil())) {
            throw new IllegalArgumentException("scheduledAt cannot be after the donation pickup available until time.");
        }

        if (donation.getConsumeBefore() != null && requestDto.scheduledAt().isAfter(donation.getConsumeBefore())) {
            throw new IllegalArgumentException("scheduledAt cannot be after the donation consume-before deadline.");
        }

        ReceiverOrganization receiver = request.getReceiverOrganization();
        if (receiver == null) {
            throw new ResourceNotFoundException("Receiver organization not found for donation request.");
        }

        if (receiver.getUser() != null && receiver.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Receiver account is not active.");
        }

        // Generate secure OTP
        String rawOtp = pickupOtpService.generateSecureOtp();
        String otpHash = pickupOtpService.hashOtp(rawOtp);
        Instant otpExpiresAt = pickupOtpService.calculateExpiry(
                requestDto.scheduledAt(),
                donation.getPickupAvailableUntil(),
                donation.getConsumeBefore()
        );

        // Create pickup
        Pickup pickup = new Pickup();
        pickup.setDonationRequest(request);
        pickup.setReceiver(receiver);
        pickup.setStatus(PickupStatus.ASSIGNED);
        pickup.setScheduledAt(requestDto.scheduledAt());
        pickup.setAssignedAt(Instant.now());
        pickup.setOtpHash(otpHash);
        pickup.setOtpExpiresAt(otpExpiresAt);
        pickup.setOtpAttempts(0);

        pickup = pickupRepository.save(pickup);

        // Store ephemeral plaintext OTP in memory for secure retrieval by authorized donor only
        pickupOtpService.storeCachedOtp(pickup.getId(), rawOtp, otpExpiresAt);

        // Update donation status if currently AVAILABLE or APPROVED
        if (donation.getStatus() == DonationStatus.AVAILABLE || donation.getStatus() == DonationStatus.APPROVED) {
            donation.setStatus(DonationStatus.PICKUP_ASSIGNED);
            donationRepository.save(donation);
        }

        // Log audit events (NEVER log raw OTP or hash)
        auditService.logEvent(
                adminUser.getId().toString(),
                "ADMIN",
                "PICKUP_SCHEDULED",
                "Pickup",
                pickup.getId().toString(),
                "{\"receiverId\":\"" + receiver.getId() + "\",\"donationRequestId\":\"" + request.getId() + "\"}",
                httpRequest
        );

        if (eventPublisher != null && receiver.getUser() != null) {
            eventPublisher.publishEvent(new PickupScheduledEvent(
                    pickup.getId(),
                    receiver.getUser().getId(),
                    donation.getTitle(),
                    donation.getPickupAddress(),
                    pickup.getScheduledAt()
            ));
        }

        auditService.logEvent(
                adminUser.getId().toString(),
                "ADMIN",
                "OTP_GENERATED",
                "Pickup",
                pickup.getId().toString(),
                "{\"expiresAt\":\"" + otpExpiresAt + "\"}",
                httpRequest
        );

        return AdminAssignPickupResponse.fromEntity(pickup);
    }

    @Transactional
    public AdminAssignPickupResponse assignVolunteer(String adminFirebaseUid,
                                                     AssignPickupRequest requestDto,
                                                     HttpServletRequest httpRequest) {
        return schedulePickup(adminFirebaseUid, requestDto, httpRequest);
    }

    public Page<PickupResponse> getPickupsForReceiver(String firebaseUid, Pageable pageable) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        return pickupRepository.findByReceiverIdOrderByScheduledAtDesc(receiver.getId(), pageable)
                .map(PickupResponse::fromEntity);
    }

    public PickupResponse getPickupDetailForReceiver(String firebaseUid, UUID pickupId) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to view this pickup.");
        }

        return PickupResponse.fromEntity(pickup);
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public PickupResponse verifyOtp(String firebaseUid, UUID pickupId, String rawOtp, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);

        // Pessimistic write lock on pickup row
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to verify OTP for this pickup.");
        }

        if (pickup.getStatus() != PickupStatus.ASSIGNED) {
            throw new IllegalStateException("Pickup is not in ASSIGNED state. Current status: " + pickup.getStatus());
        }

        if (pickup.getOtpAttempts() >= pickupOtpService.getMaxAttempts()) {
            throw new IllegalStateException("Maximum verification attempts exceeded. Further verification is blocked.");
        }

        if (Instant.now().isAfter(pickup.getOtpExpiresAt())) {
            throw new IllegalStateException("Invalid or expired pickup OTP.");
        }

        boolean matches = pickupOtpService.verifyOtp(rawOtp, pickup.getOtpHash());
        if (!matches) {
            pickup.setOtpAttempts(pickup.getOtpAttempts() + 1);
            pickupRepository.save(pickup);

            auditService.logEvent(
                    user.getId().toString(),
                    "RECEIVER",
                    "OTP_VERIFICATION_FAILED",
                    "Pickup",
                    pickup.getId().toString(),
                    "{\"attempts\":" + pickup.getOtpAttempts() + "}",
                    httpRequest
            );

            throw new IllegalStateException("Invalid or expired pickup OTP.");
        }

        // OTP verified successfully -> transition to PICKED_UP
        pickupOtpService.invalidateOtp(pickupId);
        pickup.setOtpVerifiedAt(Instant.now());
        pickup.setPickedUpAt(Instant.now());
        pickup.setStatus(PickupStatus.PICKED_UP);
        pickup = pickupRepository.save(pickup);

        // Update donation status if appropriate
        Donation donation = pickup.getDonationRequest().getDonation();
        if (donation != null) {
            List<Pickup> donationPickups = pickupRepository.findByDonationId(donation.getId());
            boolean allPickedUp = donationPickups.stream()
                    .allMatch(p -> p.getStatus() == PickupStatus.PICKED_UP || p.getStatus() == PickupStatus.COMPLETED);
            if (allPickedUp && donation.getStatus() == DonationStatus.PICKUP_ASSIGNED) {
                donation.setStatus(DonationStatus.PICKED_UP);
                donationRepository.save(donation);
            }
        }

        if (eventPublisher != null && donation != null && donation.getDonor() != null
                && donation.getDonor().getUser() != null) {
            eventPublisher.publishEvent(new OtpVerifiedEvent(
                    pickup.getId(),
                    donation.getDonor().getUser().getId(),
                    donation.getTitle(),
                    receiver.getOrgName()
            ));
        }

        auditService.logEvent(
                user.getId().toString(),
                "RECEIVER",
                "OTP_VERIFIED",
                "Pickup",
                pickup.getId().toString(),
                "{\"status\":\"PICKED_UP\"}",
                httpRequest
        );

        return PickupResponse.fromEntity(pickup);
    }

    @Transactional
    public PickupResponse completePickup(String firebaseUid, UUID pickupId, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);

        // Pessimistic write lock on pickup row
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to complete this pickup.");
        }

        if (pickup.getStatus() != PickupStatus.PICKED_UP) {
            throw new IllegalStateException("Only PICKED_UP pickups can be completed. Current status: " + pickup.getStatus());
        }

        // Transition pickup to COMPLETED
        pickup.setStatus(PickupStatus.COMPLETED);
        pickup.setCompletedAt(Instant.now());
        pickup = pickupRepository.save(pickup);
        pickupOtpService.invalidateOtp(pickupId);

        // Transition DonationRequest to COMPLETED
        DonationRequest request = pickup.getDonationRequest();
        request.setStatus(DonationRequestStatus.COMPLETED);
        donationRequestRepository.save(request);

        // Check if entire parent Donation is completed
        Donation donation = request.getDonation();
        boolean donationMarkedCompleted = false;
        if (donation != null) {
            List<DonationRequest> pendingApprovedRequests = donationRequestRepository
                    .findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.APPROVED);

            boolean noPendingApprovals = pendingApprovedRequests.isEmpty();
            boolean fullyAllocated = (donation.getRemainingQuantity() <= 0.0);

            // Donation only becomes COMPLETED when all approved requests are COMPLETED and no quantity remains unallocated
            if (noPendingApprovals && fullyAllocated) {
                donation.setStatus(DonationStatus.COMPLETED);
                donationRepository.save(donation);
                donationMarkedCompleted = true;
            }
        }

        if (eventPublisher != null) {
            UUID donorUserId = (donation != null && donation.getDonor() != null && donation.getDonor().getUser() != null)
                    ? donation.getDonor().getUser().getId() : null;
            UUID receiverUserId = (request.getReceiverOrganization() != null && request.getReceiverOrganization().getUser() != null)
                    ? request.getReceiverOrganization().getUser().getId() : null;

            if (donorUserId != null) {
                eventPublisher.publishEvent(new PickupCompletedEvent(
                        pickup.getId(),
                        donorUserId,
                        receiverUserId,
                        donation != null ? donation.getTitle() : "Donation",
                        receiver.getOrgName()
                ));

                if (donationMarkedCompleted) {
                    eventPublisher.publishEvent(new DonationCompletedEvent(
                            donation.getId(),
                            donorUserId,
                            donation.getTitle()
                    ));
                }
            }
        }

        auditService.logEvent(
                user.getId().toString(),
                "RECEIVER",
                "PICKUP_COMPLETED",
                "Pickup",
                pickup.getId().toString(),
                "{\"donationRequestId\":\"" + request.getId() + "\"}",
                httpRequest
        );

        return PickupResponse.fromEntity(pickup);
    }

    public Page<PickupResponse> getAdminPickups(PickupStatus status, Pageable pageable) {
        if (status != null) {
            return pickupRepository.findByStatusOrderByScheduledAtDesc(status, pageable)
                    .map(PickupResponse::fromEntity);
        }
        return pickupRepository.findAllByOrderByScheduledAtDesc(pageable)
                .map(PickupResponse::fromEntity);
    }

    public PickupResponse getAdminPickupDetail(UUID pickupId) {
        return pickupRepository.findById(pickupId)
                .map(PickupResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));
    }

    public Page<PickupResponse> getPickupsForDonor(String donorFirebaseUid, Pageable pageable) {
        User donorUser = validateAndGetDonorUser(donorFirebaseUid);
        DonorProfile donorProfile = getDonorProfile(donorUser);
        return pickupRepository.findByDonorIdOrderByScheduledAtDesc(donorProfile.getId(), pageable)
                .map(PickupResponse::fromEntity);
    }

    public List<PickupResponse> getPickupsForDonation(String donorFirebaseUid, UUID donationId) {
        User donorUser = validateAndGetDonorUser(donorFirebaseUid);
        DonorProfile donorProfile = getDonorProfile(donorUser);
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));
        if (donation.getDonor() == null || !donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to view pickups for this donation.");
        }
        return pickupRepository.findByDonationId(donationId).stream()
                .map(PickupResponse::fromEntity)
                .toList();
    }

    public PickupResponse getPickupDetailForDonor(String donorFirebaseUid, UUID pickupId) {
        User donorUser = validateAndGetDonorUser(donorFirebaseUid);
        DonorProfile donorProfile = getDonorProfile(donorUser);
        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));
        Donation donation = pickup.getDonationRequest().getDonation();
        if (donation == null || donation.getDonor() == null || !donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to view this pickup.");
        }
        return PickupResponse.fromEntity(pickup);
    }

    @Transactional
    public DonorPickupOtpResponse getOtpForDonor(String donorFirebaseUid, UUID pickupId, HttpServletRequest httpRequest) {
        User donorUser = validateAndGetDonorUser(donorFirebaseUid);
        DonorProfile donorProfile = getDonorProfile(donorUser);
        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));
        Donation donation = pickup.getDonationRequest().getDonation();
        if (donation == null || donation.getDonor() == null || !donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to view the OTP for this pickup.");
        }

        if (pickup.getStatus() == PickupStatus.PICKED_UP || pickup.getStatus() == PickupStatus.COMPLETED) {
            throw new IllegalStateException("Food collection has already occurred. Handover OTP is no longer active.");
        }
        if (pickup.getStatus() == PickupStatus.CANCELLED) {
            throw new IllegalStateException("Pickup has been cancelled.");
        }
        if (pickup.getOtpExpiresAt() != null && Instant.now().isAfter(pickup.getOtpExpiresAt())) {
            throw new IllegalStateException("Pickup OTP has expired.");
        }

        String otp = pickupOtpService.getCachedOtp(pickupId)
                .orElseThrow(() -> new IllegalStateException("Active OTP is not available or has expired."));

        auditService.logEvent(
                donorUser.getId().toString(),
                "DONOR",
                "OTP_VIEWED",
                "Pickup",
                pickup.getId().toString(),
                "{\"expiresAt\":\"" + pickup.getOtpExpiresAt() + "\"}",
                httpRequest
        );

        return new DonorPickupOtpResponse(
                pickup.getId(),
                donation.getId(),
                donation.getTitle(),
                pickup.getReceiver().getOrgName(),
                otp,
                pickup.getOtpExpiresAt()
        );
    }

    private User validateAndGetAdminUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("User does not possess ADMIN permissions.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Admin account is suspended.");
        }

        return user;
    }

    private User validateAndGetReceiverUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.RECEIVER) {
            throw new AccessDeniedException("User does not possess receiver permissions.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Receiver account is suspended.");
        }

        return user;
    }

    private User validateAndGetDonorUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.DONOR) {
            throw new AccessDeniedException("User does not possess donor permissions.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Donor account is suspended.");
        }

        return user;
    }

    private DonorProfile getDonorProfile(User user) {
        return donorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user."));
    }

    private ReceiverOrganization getReceiverOrganization(User user) {
        return receiverOrganizationRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver organization profile not found for user."));
    }
}

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
import com.mealshare.notification.NotificationService;
import com.mealshare.notification.NotificationType;
import com.mealshare.pickup.dto.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private PickupLocationUpdateRepository pickupLocationUpdateRepository;

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

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setPickupLocationUpdateRepository(PickupLocationUpdateRepository pickupLocationUpdateRepository) {
        this.pickupLocationUpdateRepository = pickupLocationUpdateRepository;
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

    @Transactional
    public Page<PickupResponse> getPickupsForReceiver(String firebaseUid, Pageable pageable) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);

        List<DonationRequest> approvedRequests = donationRequestRepository.findByReceiverOrganizationIdAndStatus(receiver.getId(), DonationRequestStatus.APPROVED);
        for (DonationRequest req : approvedRequests) {
            if (!pickupRepository.existsByDonationRequestId(req.getId())) {
                try {
                    createPickupForApprovedRequest(req.getDonation(), req, null);
                } catch (Exception ignored) {
                }
            }
        }

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

    @Transactional
    public Pickup createPickupForApprovedRequest(Donation donation, DonationRequest request, HttpServletRequest httpRequest) {
        if (pickupRepository.existsByDonationRequestId(request.getId())) {
            return pickupRepository.findByDonationRequestId(request.getId()).orElse(null);
        }

        ReceiverOrganization receiver = request.getReceiverOrganization();
        if (receiver == null) {
            throw new ResourceNotFoundException("Receiver organization not found for donation request.");
        }

        String rawOtp = pickupOtpService.generateSecureOtp();
        String otpHash = pickupOtpService.hashOtp(rawOtp);
        Instant scheduledAt = donation.getPickupAvailableFrom() != null && donation.getPickupAvailableFrom().isAfter(Instant.now())
                ? donation.getPickupAvailableFrom() : Instant.now();
        Instant otpExpiresAt = pickupOtpService.calculateExpiry(
                scheduledAt,
                donation.getPickupAvailableUntil(),
                donation.getConsumeBefore()
        );
        if (otpExpiresAt.isBefore(Instant.now())) {
            otpExpiresAt = Instant.now().plus(4, java.time.temporal.ChronoUnit.HOURS);
        }

        Pickup pickup = new Pickup();
        pickup.setDonationRequest(request);
        pickup.setReceiver(receiver);
        pickup.setStatus(PickupStatus.PICKUP_PENDING);
        pickup.setScheduledAt(scheduledAt);
        pickup.setAssignedAt(Instant.now());
        pickup.setOtpHash(otpHash);
        pickup.setOtpExpiresAt(otpExpiresAt);
        pickup.setOtpAttempts(0);
        pickup.setTrackingActive(false);

        pickup = pickupRepository.save(pickup);
        pickupOtpService.storeCachedOtp(pickup.getId(), rawOtp, otpExpiresAt);

        if (donation.getStatus() == DonationStatus.AVAILABLE || donation.getStatus() == DonationStatus.APPROVED) {
            donation.setStatus(DonationStatus.PICKUP_ASSIGNED);
            donationRepository.save(donation);
        }

        if (notificationService != null && receiver.getUser() != null) {
            notificationService.createNotification(
                    receiver.getUser().getId(),
                    NotificationType.REQUEST_APPROVED,
                    "Pickup Ready: " + donation.getTitle(),
                    "Your request for " + request.getRequestedQuantity() + " " + donation.getQuantityUnit() + " of '" + donation.getTitle() + "' was approved! You can now start your pickup journey.",
                    "PICKUP",
                    pickup.getId()
            );
        }

        return pickup;
    }

    @Transactional
    public Pickup ensurePickupForRequest(DonationRequest request) {
        return pickupRepository.findByDonationRequestId(request.getId())
                .orElseGet(() -> createPickupForApprovedRequest(request.getDonation(), request, null));
    }

    @Transactional
    public Pickup ensurePickupForRequestId(UUID requestId) {
        DonationRequest request = donationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestId));
        return ensurePickupForRequest(request);
    }

    @Transactional
    public PickupResponse startPickupJourney(String firebaseUid, UUID pickupId, LocationUpdateDto location, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to manage this pickup.");
        }

        if (pickup.getStatus() != PickupStatus.PICKUP_PENDING && pickup.getStatus() != PickupStatus.ASSIGNED) {
            throw new IllegalStateException("Pickup cannot start journey in status: " + pickup.getStatus());
        }

        pickup.setStatus(PickupStatus.EN_ROUTE_TO_PICKUP);
        pickup.setStartedPickupAt(Instant.now());
        pickup.setTrackingActive(true);

        if (location != null && location.latitude() != null && location.longitude() != null) {
            pickup.setCurrentLatitude(location.latitude());
            pickup.setCurrentLongitude(location.longitude());
            pickup.setLocationUpdatedAt(Instant.now());
            if (pickupLocationUpdateRepository != null) {
                pickupLocationUpdateRepository.save(new PickupLocationUpdate(
                        pickup, location.latitude(), location.longitude(), location.speed(), location.heading(), "APPROACHING_PICKUP"
                ));
            }
        }

        pickup = pickupRepository.save(pickup);

        Donation donation = pickup.getDonationRequest().getDonation();
        if (notificationService != null && donation != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
            notificationService.createNotification(
                    donation.getDonor().getUser().getId(),
                    NotificationType.PICKUP_EN_ROUTE,
                    "Receiver En Route",
                    receiver.getOrgName() + " has started their pickup journey and is heading to collect '" + donation.getTitle() + "'.",
                    "PICKUP",
                    pickup.getId()
            );
        }

        auditService.logEvent(user.getId().toString(), "RECEIVER", "PICKUP_JOURNEY_STARTED", "Pickup", pickup.getId().toString(), "{}", httpRequest);
        return PickupResponse.fromEntity(pickup);
    }

    @Transactional
    public Map<String, Object> recordLocationUpdate(String firebaseUid, UUID pickupId, LocationUpdateDto location, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to update location for this pickup.");
        }

        if (!Boolean.TRUE.equals(pickup.getTrackingActive())) {
            throw new IllegalStateException("Location tracking is not active for this pickup.");
        }

        pickup.setCurrentLatitude(location.latitude());
        pickup.setCurrentLongitude(location.longitude());
        pickup.setLocationUpdatedAt(Instant.now());

        String phase = (pickup.getStatus() == PickupStatus.IN_TRANSIT) ? "IN_TRANSIT_DELIVERY" : "APPROACHING_PICKUP";
        if (pickupLocationUpdateRepository != null) {
            pickupLocationUpdateRepository.save(new PickupLocationUpdate(
                    pickup, location.latitude(), location.longitude(), location.speed(), location.heading(), phase
            ));
        }
        pickupRepository.save(pickup);

        double distanceMeters = 0.0;
        Donation donation = pickup.getDonationRequest().getDonation();
        if (pickup.getStatus() == PickupStatus.EN_ROUTE_TO_PICKUP || pickup.getStatus() == PickupStatus.AT_PICKUP) {
            if (donation != null && donation.getLatitude() != null && donation.getLongitude() != null) {
                distanceMeters = calculateHaversineDistanceMeters(
                        location.latitude(), location.longitude(),
                        donation.getLatitude(), donation.getLongitude()
                );
            }
        } else if (pickup.getStatus() == PickupStatus.IN_TRANSIT) {
            if (receiver.getServiceLatitude() != null && receiver.getServiceLongitude() != null) {
                distanceMeters = calculateHaversineDistanceMeters(
                        location.latitude(), location.longitude(),
                        receiver.getServiceLatitude(), receiver.getServiceLongitude()
                );
            }
        }

        boolean withinGeofence = (distanceMeters <= 200.0);

        return Map.of(
                "pickupId", pickup.getId(),
                "status", pickup.getStatus(),
                "distanceMeters", Math.round(distanceMeters * 10.0) / 10.0,
                "withinGeofence", withinGeofence,
                "recordedAt", Instant.now()
        );
    }

    @Transactional
    public PickupResponse confirmReachedLocation(String firebaseUid, UUID pickupId, LocationUpdateDto location, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to update this pickup.");
        }

        pickup.setStatus(PickupStatus.AT_PICKUP);
        pickup.setArrivedAtPickupAt(Instant.now());
        if (location != null && location.latitude() != null && location.longitude() != null) {
            pickup.setCurrentLatitude(location.latitude());
            pickup.setCurrentLongitude(location.longitude());
            pickup.setLocationUpdatedAt(Instant.now());
        }

        pickup = pickupRepository.save(pickup);

        Donation donation = pickup.getDonationRequest().getDonation();
        if (notificationService != null && donation != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
            notificationService.createNotification(
                    donation.getDonor().getUser().getId(),
                    NotificationType.PICKUP_ARRIVED,
                    "Receiver Arrived",
                    receiver.getOrgName() + " has arrived at your location to collect '" + donation.getTitle() + "'. Please prepare the handover OTP.",
                    "PICKUP",
                    pickup.getId()
            );
        }

        auditService.logEvent(user.getId().toString(), "RECEIVER", "REACHED_PICKUP_LOCATION", "Pickup", pickup.getId().toString(), "{}", httpRequest);
        return PickupResponse.fromEntity(pickup);
    }

    @Transactional
    public PickupResponse inspectFood(String firebaseUid, UUID pickupId, FoodInspectionDto dto, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to inspect this food.");
        }

        pickup.setFoodInspectedAt(Instant.now());
        Donation donation = pickup.getDonationRequest().getDonation();

        if (Boolean.TRUE.equals(dto.accepted())) {
            pickup.setInspectionRejected(false);
            pickup.setInspectionNotes(dto.notes() != null ? dto.notes().trim() : "Food inspected and accepted.");
        } else {
            pickup.setInspectionRejected(true);
            pickup.setInspectionNotes(dto.rejectionReason() != null ? dto.rejectionReason().trim() : "Declined during on-site inspection");
            pickup.setStatus(PickupStatus.CANCELLED);
            pickup.setTrackingActive(false);

            DonationRequest request = pickup.getDonationRequest();
            request.setStatus(DonationRequestStatus.REJECTED);
            request.setRequestReason("Declined at pickup: " + pickup.getInspectionNotes());
            donationRequestRepository.save(request);

            if (donation != null) {
                double returnedQty = request.getAllocatedQuantity() != null ? request.getAllocatedQuantity() : 0.0;
                donation.setAllocatedQuantity(Math.max(0.0, donation.getAllocatedQuantity() - returnedQty));
                if (donation.getStatus() == DonationStatus.APPROVED || donation.getStatus() == DonationStatus.PICKUP_ASSIGNED) {
                    donation.setStatus(DonationStatus.AVAILABLE);
                }
                donationRepository.save(donation);

                if (notificationService != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
                    notificationService.createNotification(
                            donation.getDonor().getUser().getId(),
                            NotificationType.REQUEST_REJECTED,
                            "Pickup Inspection Declined",
                            receiver.getOrgName() + " inspected '" + donation.getTitle() + "' and declined pickup: " + pickup.getInspectionNotes(),
                            "PICKUP",
                            pickup.getId()
                    );
                }
            }
        }

        pickup = pickupRepository.save(pickup);
        auditService.logEvent(user.getId().toString(), "RECEIVER", "FOOD_INSPECTED", "Pickup", pickup.getId().toString(), "{\"accepted\":" + dto.accepted() + "}", httpRequest);
        return PickupResponse.fromEntity(pickup);
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public PickupResponse verifyAndPickup(String firebaseUid, UUID pickupId, VerifyAndPickupDto dto, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to verify OTP for this pickup.");
        }

        if (pickup.getStatus() != PickupStatus.AT_PICKUP && pickup.getStatus() != PickupStatus.EN_ROUTE_TO_PICKUP && pickup.getStatus() != PickupStatus.ASSIGNED && pickup.getStatus() != PickupStatus.PICKUP_PENDING) {
            throw new IllegalStateException("Pickup is not in a collectible state. Current status: " + pickup.getStatus());
        }

        if (pickup.getOtpAttempts() >= pickupOtpService.getMaxAttempts()) {
            throw new IllegalStateException("Maximum verification attempts exceeded. Further verification is blocked.");
        }

        if (pickup.getOtpExpiresAt() != null && Instant.now().isAfter(pickup.getOtpExpiresAt())) {
            throw new IllegalStateException("Invalid or expired pickup OTP.");
        }

        boolean matches = pickupOtpService.verifyOtp(dto.otp(), pickup.getOtpHash());
        if (!matches) {
            pickup.setOtpAttempts(pickup.getOtpAttempts() + 1);
            pickupRepository.save(pickup);
            auditService.logEvent(user.getId().toString(), "RECEIVER", "OTP_VERIFICATION_FAILED", "Pickup", pickup.getId().toString(), "{\"attempts\":" + pickup.getOtpAttempts() + "}", httpRequest);
            throw new IllegalStateException("Invalid or expired pickup OTP.");
        }

        pickupOtpService.invalidateOtp(pickupId);
        pickup.setOtpVerifiedAt(Instant.now());
        pickup.setPickedUpAt(Instant.now());
        pickup.setStatus(PickupStatus.PICKED_UP);

        if (dto.latitude() != null) pickup.setPickupLatitude(dto.latitude());
        if (dto.longitude() != null) pickup.setPickupLongitude(dto.longitude());
        if (dto.photoUrl() != null && !dto.photoUrl().isBlank()) pickup.setPickupPhotoUrl(dto.photoUrl().trim());

        pickup = pickupRepository.save(pickup);

        Donation donation = pickup.getDonationRequest().getDonation();
        if (donation != null) {
            donation.setStatus(DonationStatus.PICKED_UP);
            donationRepository.save(donation);

            if (notificationService != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
                notificationService.createNotification(
                        donation.getDonor().getUser().getId(),
                        NotificationType.OTP_VERIFIED,
                        "Food Picked Up",
                        receiver.getOrgName() + " verified OTP and collected '" + donation.getTitle() + "'.",
                        "PICKUP",
                        pickup.getId()
                );
            }
        }

        auditService.logEvent(user.getId().toString(), "RECEIVER", "PICKUP_VERIFIED", "Pickup", pickup.getId().toString(), "{\"status\":\"PICKED_UP\"}", httpRequest);
        return PickupResponse.fromEntity(pickup);
    }

    @Transactional
    public PickupResponse startDelivery(String firebaseUid, UUID pickupId, LocationUpdateDto location, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to manage this delivery.");
        }

        if (pickup.getStatus() != PickupStatus.PICKED_UP) {
            throw new IllegalStateException("Only PICKED_UP shipments can begin delivery transit. Current status: " + pickup.getStatus());
        }

        pickup.setStatus(PickupStatus.IN_TRANSIT);
        pickup.setStartedDeliveryAt(Instant.now());
        pickup.setTrackingActive(true);

        if (location != null && location.latitude() != null && location.longitude() != null) {
            pickup.setCurrentLatitude(location.latitude());
            pickup.setCurrentLongitude(location.longitude());
            pickup.setLocationUpdatedAt(Instant.now());
            if (pickupLocationUpdateRepository != null) {
                pickupLocationUpdateRepository.save(new PickupLocationUpdate(
                        pickup, location.latitude(), location.longitude(), location.speed(), location.heading(), "IN_TRANSIT_DELIVERY"
                ));
            }
        }

        pickup = pickupRepository.save(pickup);

        Donation donation = pickup.getDonationRequest().getDonation();
        if (notificationService != null && donation != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
            notificationService.createNotification(
                    donation.getDonor().getUser().getId(),
                    NotificationType.DELIVERY_IN_TRANSIT,
                    "Delivery In Transit",
                    receiver.getOrgName() + " has started transit to distribute '" + donation.getTitle() + "'. You can monitor live delivery.",
                    "PICKUP",
                    pickup.getId()
            );
        }

        auditService.logEvent(user.getId().toString(), "RECEIVER", "DELIVERY_STARTED", "Pickup", pickup.getId().toString(), "{}", httpRequest);
        return PickupResponse.fromEntity(pickup);
    }

    @Transactional
    public PickupResponse completeDelivery(String firebaseUid, UUID pickupId, CompleteDeliveryDto dto, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization receiver = getReceiverOrganization(user);
        Pickup pickup = pickupRepository.findByIdWithLock(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        if (!pickup.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("You do not have permission to complete this delivery.");
        }

        if (pickup.getStatus() != PickupStatus.IN_TRANSIT && pickup.getStatus() != PickupStatus.PICKED_UP) {
            throw new IllegalStateException("Only in-transit or collected deliveries can be completed. Current status: " + pickup.getStatus());
        }

        pickup.setStatus(PickupStatus.COMPLETED);
        pickup.setCompletedAt(Instant.now());
        pickup.setArrivedAtDeliveryAt(Instant.now());
        pickup.setTrackingActive(false); // LIVE TRACKING TERMINATES!

        if (dto.latitude() != null) pickup.setDeliveryLatitude(dto.latitude());
        if (dto.longitude() != null) pickup.setDeliveryLongitude(dto.longitude());
        if (dto.deliveredQuantity() != null) pickup.setDeliveredQuantity(dto.deliveredQuantity());
        if (dto.quantityDiscrepancyReason() != null) pickup.setQuantityDiscrepancyReason(dto.quantityDiscrepancyReason().trim());
        if (dto.photoUrl() != null && !dto.photoUrl().isBlank()) pickup.setDeliveryPhotoUrl(dto.photoUrl().trim());

        pickup = pickupRepository.save(pickup);

        DonationRequest request = pickup.getDonationRequest();
        request.setStatus(DonationRequestStatus.COMPLETED);
        donationRequestRepository.save(request);

        Donation donation = request.getDonation();
        if (donation != null) {
            donation.setStatus(DonationStatus.COMPLETED);
            donationRepository.save(donation);

            if (notificationService != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
                notificationService.createNotification(
                        donation.getDonor().getUser().getId(),
                        NotificationType.DELIVERY_COMPLETED,
                        "Donation Distributed Successfully",
                        receiver.getOrgName() + " has completed distribution of " + pickup.getDeliveredQuantity() + " " + donation.getQuantityUnit() + " of '" + donation.getTitle() + "'. Please share your review!",
                        "PICKUP",
                        pickup.getId()
                );
            }
        }

        if (notificationService != null) {
            notificationService.createNotification(
                    user.getId(),
                    NotificationType.DELIVERY_COMPLETED,
                    "Delivery Completed",
                    "Delivery successfully completed for '" + (donation != null ? donation.getTitle() : "Donation") + "'. Please leave a review for the donor.",
                    "PICKUP",
                    pickup.getId()
            );
        }

        auditService.logEvent(user.getId().toString(), "RECEIVER", "DELIVERY_COMPLETED", "Pickup", pickup.getId().toString(), "{\"deliveredQuantity\":" + pickup.getDeliveredQuantity() + "}", httpRequest);
        return PickupResponse.fromEntity(pickup);
    }

    public LiveTrackingDto getLiveTracking(String userFirebaseUid, UUID pickupId) {
        User user = userRepository.findByFirebaseUid(userFirebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        DonationRequest req = pickup.getDonationRequest();
        Donation donation = req != null ? req.getDonation() : null;
        DonorProfile donor = donation != null ? donation.getDonor() : null;
        ReceiverOrganization receiver = pickup.getReceiver();

        boolean isDonor = (donor != null && donor.getUser().getId().equals(user.getId()));
        boolean isReceiver = (receiver != null && receiver.getUser().getId().equals(user.getId()));
        boolean isAdmin = (user.getRole() == UserRole.ADMIN);

        if (!isDonor && !isReceiver && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to track this pickup.");
        }

        double distanceMeters = 0.0;
        String phase = "IDLE";
        if (pickup.getStatus() == PickupStatus.EN_ROUTE_TO_PICKUP) {
            phase = "RECEIVER_APPROACHING_DONOR";
            if (pickup.getCurrentLatitude() != null && pickup.getCurrentLongitude() != null &&
                donation != null && donation.getLatitude() != null && donation.getLongitude() != null) {
                distanceMeters = calculateHaversineDistanceMeters(
                        pickup.getCurrentLatitude(), pickup.getCurrentLongitude(),
                        donation.getLatitude(), donation.getLongitude()
                );
            }
        } else if (pickup.getStatus() == PickupStatus.AT_PICKUP) {
            phase = "RECEIVER_AT_DONOR_LOCATION";
        } else if (pickup.getStatus() == PickupStatus.IN_TRANSIT) {
            phase = "IN_TRANSIT_TO_DESTINATION";
            if (pickup.getCurrentLatitude() != null && pickup.getCurrentLongitude() != null &&
                receiver != null && receiver.getServiceLatitude() != null && receiver.getServiceLongitude() != null) {
                distanceMeters = calculateHaversineDistanceMeters(
                        pickup.getCurrentLatitude(), pickup.getCurrentLongitude(),
                        receiver.getServiceLatitude(), receiver.getServiceLongitude()
                );
            }
        } else if (pickup.getStatus() == PickupStatus.COMPLETED) {
            phase = "DELIVERY_COMPLETED";
        }

        boolean withinGeofence = (distanceMeters <= 200.0);

        return new LiveTrackingDto(
                pickup.getId(),
                pickup.getStatus(),
                pickup.getTrackingActive(),
                pickup.getCurrentLatitude(),
                pickup.getCurrentLongitude(),
                pickup.getLocationUpdatedAt(),
                donation != null ? donation.getLatitude() : null,
                donation != null ? donation.getLongitude() : null,
                donation != null ? donation.getPickupAddress() : null,
                receiver != null ? receiver.getServiceLatitude() : null,
                receiver != null ? receiver.getServiceLongitude() : null,
                receiver != null ? receiver.getAddressText() : null,
                Math.round(distanceMeters * 10.0) / 10.0,
                withinGeofence,
                phase,
                receiver != null ? receiver.getOrgName() : null,
                donor != null ? donor.getEntityName() : null,
                donation != null ? donation.getTitle() : null
        );
    }

    public static double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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

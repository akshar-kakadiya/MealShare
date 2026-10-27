package com.mealshare.receiver.request;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationService;
import com.mealshare.receiver.request.dto.CreateDonationRequestDto;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import com.mealshare.notification.event.DonationRequestCreatedEvent;
import com.mealshare.notification.event.DonationRequestWithdrawnEvent;
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
public class DonationRequestService {

    private final DonationRequestRepository donationRequestRepository;
    private final DonationRepository donationRepository;
    private final ReceiverOrganizationService receiverOrganizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private ApplicationEventPublisher eventPublisher;

    public DonationRequestService(DonationRequestRepository donationRequestRepository,
                                  DonationRepository donationRepository,
                                  ReceiverOrganizationService receiverOrganizationService,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.donationRequestRepository = donationRequestRepository;
        this.donationRepository = donationRepository;
        this.receiverOrganizationService = receiverOrganizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DonationRequestResponse createRequest(String firebaseUid,
                                                 UUID donationId,
                                                 CreateDonationRequestDto requestDto,
                                                 HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverOrgUser(firebaseUid);
        ReceiverOrganization receiverOrg = receiverOrganizationService.getOrCreateProfileForUser(
                user, "Not Specified", 12.9716, 77.5946
        );

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        // Prevent donor from requesting their own donation
        if (donation.getDonor() != null && donation.getDonor().getUser() != null
                && donation.getDonor().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot request your own donation.");
        }

        // Validate donation status
        if (donation.getStatus() == DonationStatus.EXPIRED) {
            throw new IllegalStateException("Donation has expired and is no longer available.");
        }
        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Donation is no longer available for requests.");
        }

        // Validate expiration deadline
        Instant now = Instant.now();
        if (donation.getConsumeBefore() != null && donation.getConsumeBefore().isBefore(now)) {
            throw new IllegalStateException("Donation has passed its consume-before expiration time.");
        }
        if (donation.getPickupAvailableUntil() != null && donation.getPickupAvailableUntil().isBefore(now)) {
            throw new IllegalStateException("Donation has passed its pickup availability deadline.");
        }

        // Validate requested quantity
        if (requestDto.requestedQuantity() == null || requestDto.requestedQuantity() <= 0) {
            throw new IllegalArgumentException("Requested quantity must be strictly greater than 0.");
        }

        if (requestDto.requestedQuantity() > donation.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity (" + requestDto.requestedQuantity()
                    + ") exceeds available donation quantity (" + donation.getQuantity() + ").");
        }

        // Prevent duplicate active request
        boolean alreadyActive = donationRequestRepository.existsByDonationIdAndReceiverOrganizationIdAndStatusIn(
                donationId,
                receiverOrg.getId(),
                List.of(DonationRequestStatus.PENDING, DonationRequestStatus.APPROVED)
        );

        if (alreadyActive) {
            throw new IllegalStateException("An active request already exists for this donation by your organization.");
        }

        // Create request in PENDING status
        DonationRequest request = new DonationRequest();
        request.setDonation(donation);
        request.setReceiverOrganization(receiverOrg);
        request.setRequestedQuantity(requestDto.requestedQuantity());
        request.setRequestReason(requestDto.requestReason() != null ? requestDto.requestReason().trim() : null);
        request.setStatus(DonationRequestStatus.PENDING);
        request.setRequestedAt(Instant.now());

        request = donationRequestRepository.save(request);

        if (eventPublisher != null && donation.getDonor() != null && donation.getDonor().getUser() != null) {
            eventPublisher.publishEvent(new DonationRequestCreatedEvent(
                    request.getId(),
                    donation.getId(),
                    donation.getDonor().getUser().getId(),
                    donation.getTitle(),
                    receiverOrg.getOrgName(),
                    request.getRequestedQuantity(),
                    donation.getQuantityUnit()
            ));
        }

        auditService.logEvent(
                user.getId().toString(),
                "RECEIVER",
                "REQUEST_CREATED",
                "DonationRequest",
                request.getId().toString(),
                "{\"donationId\":\"" + donationId + "\",\"quantity\":" + request.getRequestedQuantity() + "}",
                httpRequest
        );

        return DonationRequestResponse.fromEntity(request);
    }

    public Page<DonationRequestResponse> getMyRequests(String firebaseUid, Pageable pageable) {
        User user = validateAndGetReceiverOrgUser(firebaseUid);
        ReceiverOrganization receiverOrg = getReceiverOrgOrThrow(user);

        return donationRequestRepository.findByReceiverOrganizationIdOrderByRequestedAtDesc(receiverOrg.getId(), pageable)
                .map(DonationRequestResponse::fromEntity);
    }

    public DonationRequestResponse getRequestById(String firebaseUid, UUID requestId) {
        User user = validateAndGetReceiverOrgUser(firebaseUid);
        ReceiverOrganization receiverOrg = getReceiverOrgOrThrow(user);

        DonationRequest request = donationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestId));

        if (!request.getReceiverOrganization().getId().equals(receiverOrg.getId())) {
            throw new AccessDeniedException("You do not have permission to view this donation request.");
        }

        return DonationRequestResponse.fromEntity(request);
    }

    @Transactional
    public DonationRequestResponse withdrawRequest(String firebaseUid, UUID requestId, HttpServletRequest httpRequest) {
        User user = validateAndGetReceiverOrgUser(firebaseUid);
        ReceiverOrganization receiverOrg = getReceiverOrgOrThrow(user);

        DonationRequest request = donationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestId));

        if (!request.getReceiverOrganization().getId().equals(receiverOrg.getId())) {
            throw new AccessDeniedException("You do not have permission to withdraw this donation request.");
        }

        if (request.getStatus() != DonationRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be withdrawn. Current status: " + request.getStatus());
        }

        request.setStatus(DonationRequestStatus.WITHDRAWN);
        request = donationRequestRepository.save(request);

        if (eventPublisher != null && request.getDonation() != null && request.getDonation().getDonor() != null
                && request.getDonation().getDonor().getUser() != null) {
            eventPublisher.publishEvent(new DonationRequestWithdrawnEvent(
                    request.getId(),
                    request.getDonation().getId(),
                    request.getDonation().getDonor().getUser().getId(),
                    request.getDonation().getTitle(),
                    receiverOrg.getOrgName()
            ));
        }

        auditService.logEvent(
                user.getId().toString(),
                "RECEIVER",
                "REQUEST_WITHDRAWN",
                "DonationRequest",
                request.getId().toString(),
                "{\"status\":\"WITHDRAWN\"}",
                httpRequest
        );

        return DonationRequestResponse.fromEntity(request);
    }

    public List<DonationRequestResponse> getRequestsForDonationInFcfsOrder(UUID donationId) {
        return donationRequestRepository.findByDonationIdOrderByRequestedAtAscIdAsc(donationId)
                .stream()
                .map(DonationRequestResponse::fromEntity)
                .toList();
    }

    private User validateAndGetReceiverOrgUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.RECEIVER) {
            throw new AccessDeniedException("Only verified receivers can manage donation requests.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Account is suspended.");
        }

        return user;
    }

    private ReceiverOrganization getReceiverOrgOrThrow(User user) {
        return receiverOrganizationService.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver organization profile not found for user."));
    }
}

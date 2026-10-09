package com.mealshare.donor.request;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileService;
import com.mealshare.donor.request.dto.ApproveDonationRequestResponse;
import com.mealshare.donor.request.dto.DonorDonationRequestResponse;
import com.mealshare.donor.request.dto.RejectDonationRequestDto;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import com.mealshare.notification.event.DonationRequestApprovedEvent;
import com.mealshare.notification.event.DonationRequestRejectedEvent;
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
public class DonorRequestService {

    private final DonationRequestRepository donationRequestRepository;
    private final DonationRepository donationRepository;
    private final DonorProfileService donorProfileService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private ApplicationEventPublisher eventPublisher;

    public DonorRequestService(DonationRequestRepository donationRequestRepository,
                               DonationRepository donationRepository,
                               DonorProfileService donorProfileService,
                               UserRepository userRepository,
                               AuditService auditService) {
        this.donationRequestRepository = donationRequestRepository;
        this.donationRepository = donationRepository;
        this.donorProfileService = donorProfileService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Autowired(required = false)
    private com.mealshare.pickup.PickupService pickupService;

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setPickupService(com.mealshare.pickup.PickupService pickupService) {
        this.pickupService = pickupService;
    }

    public Page<DonorDonationRequestResponse> getRequestsForDonation(String firebaseUid,
                                                                    UUID donationId,
                                                                    Pageable pageable) {
        User user = validateAndGetDonorUser(firebaseUid);
        DonorProfile donorProfile = getDonorProfileOrThrow(user);

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (!donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to view requests for this donation.");
        }

        return donationRequestRepository.findByDonationIdOrderByRequestedAtAscIdAsc(donationId, pageable)
                .map(DonorDonationRequestResponse::fromEntity);
    }

    @Transactional
    public ApproveDonationRequestResponse approveRequest(String firebaseUid,
                                                         UUID requestId,
                                                         HttpServletRequest httpRequest) {
        User user = validateAndGetDonorUser(firebaseUid);
        DonorProfile donorProfile = getDonorProfileOrThrow(user);

        DonationRequest request = donationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestId));

        if (request.getStatus() != DonationRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + request.getStatus());
        }

        // Concurrency protection: Pessimistic write lock on the parent donation
        Donation donation = donationRepository.findByIdWithLock(request.getDonation().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated donation not found"));

        if (!donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to approve requests for this donation.");
        }

        // Validate donation state
        if (donation.getStatus() == DonationStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled donation cannot be approved.");
        }
        if (donation.getStatus() == DonationStatus.COMPLETED) {
            throw new IllegalStateException("Completed donation cannot be approved.");
        }
        if (donation.getStatus() == DonationStatus.EXPIRED) {
            throw new IllegalStateException("Expired donation cannot be approved.");
        }
        if (donation.getStatus() == DonationStatus.APPROVED) {
            throw new IllegalStateException("Donation has already been fully allocated and approved.");
        }
        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Donation is not available for allocation. Current status: " + donation.getStatus());
        }
        if (donation.getConsumeBefore() != null && donation.getConsumeBefore().isBefore(Instant.now())) {
            throw new IllegalStateException("Donation has passed its consume-before deadline.");
        }

        double remainingQuantity = donation.getRemainingQuantity();

        // Validate FCFS rule
        List<DonationRequest> pendingRequests = donationRequestRepository
                .findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING);

        for (DonationRequest earlierPending : pendingRequests) {
            if (earlierPending.getId().equals(requestId)) {
                break; // Target request reached without unserved eligible earlier requests
            }
            // If earlier pending request can be fulfilled with current remaining quantity, FCFS blocks later request
            if (earlierPending.getRequestedQuantity() <= remainingQuantity) {
                throw new IllegalStateException("Cannot approve request out of FCFS order. Earlier eligible pending request ("
                        + earlierPending.getId() + ") must be processed first.");
            }
        }

        // Validate quantity
        if (request.getRequestedQuantity() <= 0) {
            throw new IllegalArgumentException("Requested quantity must be strictly greater than 0.");
        }

        if (request.getRequestedQuantity() > remainingQuantity) {
            throw new IllegalStateException("Insufficient remaining quantity (" + remainingQuantity
                    + ") to allocate requested quantity (" + request.getRequestedQuantity() + ").");
        }

        // Atomically allocate quantity and transition status
        double newAllocated = donation.getAllocatedQuantity() + request.getRequestedQuantity();
        donation.setAllocatedQuantity(newAllocated);
        request.setStatus(DonationRequestStatus.APPROVED);
        request.setAllocatedQuantity(request.getRequestedQuantity());

        if (donation.getRemainingQuantity() <= 0.0) {
            donation.setStatus(DonationStatus.APPROVED);
            // Auto-reject any remaining pending requests for this fully allocated donation
            List<DonationRequest> remainingPending = donationRequestRepository
                    .findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING);
            for (DonationRequest pending : remainingPending) {
                if (!pending.getId().equals(requestId)) {
                    pending.setStatus(DonationRequestStatus.REJECTED);
                    pending.setAllocatedQuantity(0.0);
                    pending.setRequestReason(pending.getRequestReason() != null && !pending.getRequestReason().isBlank()
                            ? pending.getRequestReason() + " | Donation quantity fully allocated"
                            : "Donation quantity fully allocated");
                    donationRequestRepository.save(pending);
                }
            }
        } else {
            donation.setStatus(DonationStatus.AVAILABLE);
        }

        donationRepository.save(donation);
        donationRequestRepository.save(request);

        // Auto-initialize Pickup in PICKUP_PENDING status so receiver can directly initiate pickup journey
        if (pickupService != null) {
            try {
                pickupService.createPickupForApprovedRequest(donation, request, httpRequest);
            } catch (Exception e) {
                // Log and continue to preserve transaction integrity
            }
        }

        if (eventPublisher != null && request.getReceiverOrganization() != null
                && request.getReceiverOrganization().getUser() != null) {
            eventPublisher.publishEvent(new DonationRequestApprovedEvent(
                    request.getId(),
                    donation.getId(),
                    request.getReceiverOrganization().getUser().getId(),
                    donation.getTitle(),
                    request.getRequestedQuantity(),
                    donation.getQuantityUnit()
            ));
        }

        auditService.logEvent(
                user.getId().toString(),
                "DONOR",
                "REQUEST_APPROVED",
                "DonationRequest",
                requestId.toString(),
                "{\"allocatedQuantity\":" + request.getRequestedQuantity() + ",\"donationId\":\"" + donation.getId() + "\"}",
                httpRequest
        );

        return new ApproveDonationRequestResponse(
                request.getId(),
                donation.getId(),
                request.getRequestedQuantity(),
                donation.getAllocatedQuantity(),
                donation.getRemainingQuantity(),
                donation.getStatus(),
                request.getStatus(),
                Instant.now()
        );
    }

    @Transactional
    public DonorDonationRequestResponse rejectRequest(String firebaseUid,
                                                      UUID requestId,
                                                      RejectDonationRequestDto dto,
                                                      HttpServletRequest httpRequest) {
        User user = validateAndGetDonorUser(firebaseUid);
        DonorProfile donorProfile = getDonorProfileOrThrow(user);

        DonationRequest request = donationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestId));

        if (!request.getDonation().getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to reject requests for this donation.");
        }

        if (request.getStatus() != DonationRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + request.getStatus());
        }

        request.setStatus(DonationRequestStatus.REJECTED);
        request.setAllocatedQuantity(0.0);
        if (dto != null && dto.reason() != null && !dto.reason().isBlank()) {
            String currentReason = request.getRequestReason();
            request.setRequestReason(currentReason != null && !currentReason.isBlank()
                    ? currentReason + " | Rejection reason: " + dto.reason().trim()
                    : "Rejection reason: " + dto.reason().trim());
        }
        request = donationRequestRepository.save(request);

        if (eventPublisher != null && request.getReceiverOrganization() != null
                && request.getReceiverOrganization().getUser() != null) {
            eventPublisher.publishEvent(new DonationRequestRejectedEvent(
                    request.getId(),
                    request.getDonation().getId(),
                    request.getReceiverOrganization().getUser().getId(),
                    request.getDonation().getTitle(),
                    (dto != null) ? dto.reason() : null
            ));
        }

        String auditDetail = dto != null && dto.reason() != null && !dto.reason().isBlank()
                ? "{\"reason\":\"" + dto.reason().trim() + "\"}"
                : "{\"reason\":\"Rejected by donor\"}";

        auditService.logEvent(
                user.getId().toString(),
                "DONOR",
                "REQUEST_REJECTED",
                "DonationRequest",
                requestId.toString(),
                auditDetail,
                httpRequest
        );

        return DonorDonationRequestResponse.fromEntity(request);
    }

    private User validateAndGetDonorUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.DONOR) {
            throw new AccessDeniedException("User does not possess DONOR permissions.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Account is suspended.");
        }

        return user;
    }

    private DonorProfile getDonorProfileOrThrow(User user) {
        return donorProfileService.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user."));
    }
}

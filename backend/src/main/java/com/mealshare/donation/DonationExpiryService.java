package com.mealshare.donation;

import com.mealshare.audit.AuditService;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class DonationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(DonationExpiryService.class);

    private static final Set<DonationStatus> CANDIDATE_DONATION_STATUSES = Set.of(
            DonationStatus.AVAILABLE,
            DonationStatus.REQUESTED,
            DonationStatus.APPROVED,
            DonationStatus.PICKUP_ASSIGNED
    );

    private static final Set<PickupStatus> ACTIVE_PICKUP_STATUSES = Set.of(
            PickupStatus.EN_ROUTE_TO_PICKUP,
            PickupStatus.AT_PICKUP,
            PickupStatus.PICKED_UP,
            PickupStatus.IN_TRANSIT,
            PickupStatus.COMPLETED
    );

    private final DonationRepository donationRepository;
    private final DonationRequestRepository donationRequestRepository;
    private final PickupRepository pickupRepository;
    private final AuditService auditService;

    public DonationExpiryService(
            DonationRepository donationRepository,
            DonationRequestRepository donationRequestRepository,
            PickupRepository pickupRepository,
            AuditService auditService
    ) {
        this.donationRepository = donationRepository;
        this.donationRequestRepository = donationRequestRepository;
        this.pickupRepository = pickupRepository;
        this.auditService = auditService;
    }

    /**
     * Identifies donations past their pickup availability deadline (pickupAvailableUntil or consumeBefore),
     * verifies whether an active pickup journey has started, and transitions eligible donations
     * and their pending requests to EXPIRED.
     *
     * @return count of newly expired donations
     */
    @Transactional
    public int expireOverdueDonations() {
        Instant now = Instant.now();
        List<Donation> overdueCandidates = donationRepository.findOverdueCandidateDonations(
                CANDIDATE_DONATION_STATUSES,
                now
        );

        if (overdueCandidates.isEmpty()) {
            return 0;
        }

        int expiredCount = 0;

        for (Donation donation : overdueCandidates) {
            // Check if there are active pickups for this donation
            List<Pickup> pickups = pickupRepository.findByDonationId(donation.getId());
            boolean hasActivePickup = pickups.stream()
                    .anyMatch(p -> ACTIVE_PICKUP_STATUSES.contains(p.getStatus()));

            if (hasActivePickup) {
                log.debug("Donation {} is past deadline but has active/completed pickup journey. Skipping expiry.", donation.getId());
                continue;
            }

            // Cancel any non-started scheduled pickups (ASSIGNED, PICKUP_PENDING)
            for (Pickup pickup : pickups) {
                if (pickup.getStatus() == PickupStatus.ASSIGNED || pickup.getStatus() == PickupStatus.PICKUP_PENDING) {
                    pickup.setStatus(PickupStatus.CANCELLED);
                    pickup.setTrackingActive(false);
                    pickupRepository.save(pickup);
                    log.info("Cancelled non-started pickup {} because donation {} expired.", pickup.getId(), donation.getId());
                }
            }

            // Transition donation to EXPIRED
            DonationStatus oldStatus = donation.getStatus();
            donation.setStatus(DonationStatus.EXPIRED);
            donationRepository.save(donation);
            expiredCount++;

            // Expire all pending requests for this donation
            List<DonationRequest> pendingRequests = donationRequestRepository
                    .findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING);

            for (DonationRequest req : pendingRequests) {
                req.setStatus(DonationRequestStatus.EXPIRED);
                req.setAllocatedQuantity(0.0);
                donationRequestRepository.save(req);
            }

            log.info("Donation {} transitioned from {} to EXPIRED (deadline: pickupUntil={}, consumeBefore={}). Expired {} pending requests.",
                    donation.getId(), oldStatus, donation.getPickupAvailableUntil(), donation.getConsumeBefore(), pendingRequests.size());

            try {
                auditService.logEvent(
                        "SYSTEM",
                        "SYSTEM",
                        "DONATION_EXPIRED",
                        "Donation",
                        donation.getId().toString(),
                        "{\"previousStatus\":\"" + oldStatus + "\",\"pendingRequestsExpired\":" + pendingRequests.size() + "}",
                        null
                );
            } catch (Exception e) {
                // Keep transaction intact if audit fails
            }
        }

        return expiredCount;
    }
}

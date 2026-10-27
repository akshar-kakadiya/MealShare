package com.mealshare.donation;

import com.mealshare.audit.AuditService;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationExpiryServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonationRequestRepository donationRequestRepository;

    @Mock
    private PickupRepository pickupRepository;

    @Mock
    private AuditService auditService;

    private DonationExpiryService expiryService;

    private Donation overdueDonation;
    private final UUID donationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        expiryService = new DonationExpiryService(
                donationRepository,
                donationRequestRepository,
                pickupRepository,
                auditService
        );

        overdueDonation = new Donation();
        overdueDonation.setId(donationId);
        overdueDonation.setTitle("Surplus Biryani");
        overdueDonation.setQuantity(50.0);
        overdueDonation.setStatus(DonationStatus.AVAILABLE);
        overdueDonation.setPickupAvailableFrom(Instant.now().minus(4, ChronoUnit.HOURS));
        overdueDonation.setPickupAvailableUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        overdueDonation.setConsumeBefore(Instant.now().plus(2, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Overdue available donation without pickups is expired along with pending requests")
    void expireOverdueDonations_whenOverdueAvailable_expiresDonationAndPendingRequests() {
        when(donationRepository.findOverdueCandidateDonations(anyCollection(), any(Instant.class)))
                .thenReturn(List.of(overdueDonation));
        when(pickupRepository.findByDonationId(donationId))
                .thenReturn(Collections.emptyList());

        DonationRequest req1 = new DonationRequest();
        req1.setId(UUID.randomUUID());
        req1.setDonation(overdueDonation);
        req1.setStatus(DonationRequestStatus.PENDING);
        req1.setRequestedQuantity(20.0);

        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donationId, DonationRequestStatus.PENDING))
                .thenReturn(List.of(req1));

        int expiredCount = expiryService.expireOverdueDonations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(overdueDonation.getStatus()).isEqualTo(DonationStatus.EXPIRED);
        assertThat(req1.getStatus()).isEqualTo(DonationRequestStatus.EXPIRED);
        assertThat(req1.getAllocatedQuantity()).isEqualTo(0.0);

        verify(donationRepository).save(overdueDonation);
        verify(donationRequestRepository).save(req1);
    }

    @Test
    @DisplayName("Active pickup journey prevents donation from expiring past availability window")
    void expireOverdueDonations_whenActivePickupJourneyExists_doesNotExpire() {
        when(donationRepository.findOverdueCandidateDonations(anyCollection(), any(Instant.class)))
                .thenReturn(List.of(overdueDonation));

        Pickup activePickup = new Pickup();
        activePickup.setId(UUID.randomUUID());
        activePickup.setStatus(PickupStatus.EN_ROUTE_TO_PICKUP);

        when(pickupRepository.findByDonationId(donationId))
                .thenReturn(List.of(activePickup));

        int expiredCount = expiryService.expireOverdueDonations();

        assertThat(expiredCount).isEqualTo(0);
        assertThat(overdueDonation.getStatus()).isEqualTo(DonationStatus.AVAILABLE);

        verify(donationRepository, never()).save(any(Donation.class));
        verify(donationRequestRepository, never()).save(any(DonationRequest.class));
    }

    @Test
    @DisplayName("Overdue approved donation with un-started pickup cancels pickup and marks donation expired")
    void expireOverdueDonations_whenPickupNeverStarted_cancelsPickupAndExpiresDonation() {
        overdueDonation.setStatus(DonationStatus.APPROVED);

        when(donationRepository.findOverdueCandidateDonations(anyCollection(), any(Instant.class)))
                .thenReturn(List.of(overdueDonation));

        Pickup pendingPickup = new Pickup();
        pendingPickup.setId(UUID.randomUUID());
        pendingPickup.setStatus(PickupStatus.ASSIGNED);
        pendingPickup.setTrackingActive(false);

        when(pickupRepository.findByDonationId(donationId))
                .thenReturn(List.of(pendingPickup));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donationId, DonationRequestStatus.PENDING))
                .thenReturn(Collections.emptyList());

        int expiredCount = expiryService.expireOverdueDonations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(overdueDonation.getStatus()).isEqualTo(DonationStatus.EXPIRED);
        assertThat(pendingPickup.getStatus()).isEqualTo(PickupStatus.CANCELLED);
        assertThat(pendingPickup.getTrackingActive()).isFalse();

        verify(pickupRepository).save(pendingPickup);
        verify(donationRepository).save(overdueDonation);
    }

    @Test
    @DisplayName("Returns 0 when no overdue candidate donations exist")
    void expireOverdueDonations_whenNoCandidates_returnsZero() {
        when(donationRepository.findOverdueCandidateDonations(anyCollection(), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        int count = expiryService.expireOverdueDonations();

        assertThat(count).isEqualTo(0);
        verifyNoInteractions(pickupRepository);
    }
}

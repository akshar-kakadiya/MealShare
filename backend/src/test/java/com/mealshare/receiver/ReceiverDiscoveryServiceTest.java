package com.mealshare.receiver;

import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReceiverDiscoveryServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReceiverDiscoveryService receiverDiscoveryService;

    private User receiverUser;
    private User donorUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("recv-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByFirebaseUid("recv-uid")).thenReturn(Optional.of(receiverUser));
        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));
    }

    @Test
    void test2_donorCannotAccessReceiverDiscovery() {
        assertThrows(AccessDeniedException.class, () ->
                receiverDiscoveryService.findNearbyDonations("donor-uid", 12.9716, 77.5946, 10.0, null, PageRequest.of(0, 20)));
    }

    @Test
    void test6_invalidLatitude_rejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                receiverDiscoveryService.findNearbyDonations("recv-uid", 120.0, 77.5946, 10.0, null, PageRequest.of(0, 20)));
        assertTrue(ex.getMessage().contains("Latitude"));
    }

    @Test
    void test7_invalidLongitude_rejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                receiverDiscoveryService.findNearbyDonations("recv-uid", 12.9716, 200.0, 10.0, null, PageRequest.of(0, 20)));
        assertTrue(ex.getMessage().contains("Longitude"));
    }

    @Test
    void test8_invalidRadius_zeroOrNegative_rejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                receiverDiscoveryService.findNearbyDonations("recv-uid", 12.9716, 77.5946, 0.0, null, PageRequest.of(0, 20)));
        assertTrue(ex.getMessage().contains("strictly positive"));
    }

    @Test
    void test9_radiusAboveMaximum_rejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                receiverDiscoveryService.findNearbyDonations("recv-uid", 12.9716, 77.5946, 150.0, null, PageRequest.of(0, 20)));
        assertTrue(ex.getMessage().contains("maximum limit of 100"));
    }

    @Test
    void test18_receiverCanViewEligibleDonationDetails() {
        UUID donationId = UUID.randomUUID();
        Donation d = new Donation();
        d.setId(donationId);
        d.setTitle("Rice and Curry");
        d.setStatus(DonationStatus.AVAILABLE);
        d.setConsumeBefore(Instant.now().plus(5, ChronoUnit.HOURS));

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(d));

        DonationResponse response = receiverDiscoveryService.getDonationDetailsForReceiver("recv-uid", donationId);
        assertNotNull(response);
        assertEquals("Rice and Curry", response.title());
    }

    @Test
    void testReceiverViewingCancelledDonation_returnsNotFound() {
        UUID donationId = UUID.randomUUID();
        Donation d = new Donation();
        d.setId(donationId);
        d.setTitle("Rice and Curry");
        d.setStatus(DonationStatus.CANCELLED);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(d));

        assertThrows(ResourceNotFoundException.class,
                () -> receiverDiscoveryService.getDonationDetailsForReceiver("recv-uid", donationId));
    }
}

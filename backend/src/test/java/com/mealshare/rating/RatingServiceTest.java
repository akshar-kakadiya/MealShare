package com.mealshare.rating;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.rating.dto.CreateRatingRequest;
import com.mealshare.rating.dto.PickupRatingStatusResponse;
import com.mealshare.rating.dto.RatingResponse;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private PickupRepository pickupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private ReceiverOrganizationRepository receiverOrganizationRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RatingService ratingService;

    private User donorUser;
    private User receiverUser;
    private User unrelatedUser;
    private User adminUser;

    private DonorProfile donorProfile;
    private ReceiverOrganization receiverOrg;
    private Donation donation;
    private DonationRequest donationRequest;
    private Pickup completedPickup;
    private Pickup assignedPickup;

    private UUID pickupId;
    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        httpRequest = new MockHttpServletRequest();

        pickupId = UUID.randomUUID();

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-firebase-uid");
        donorUser.setName("Green Kitchen Donor");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-firebase-uid");
        receiverUser.setName("Hope Community Receiver");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        unrelatedUser = new User();
        unrelatedUser.setId(UUID.randomUUID());
        unrelatedUser.setFirebaseUid("unrelated-uid");
        unrelatedUser.setName("Unrelated Person");
        unrelatedUser.setRole(UserRole.DONOR);
        unrelatedUser.setStatus(UserStatus.ACTIVE);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirebaseUid("admin-uid");
        adminUser.setName("Platform Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);
        donorProfile.setEntityName("Green Kitchen");
        donorProfile.setRatingAverage(5.0);

        receiverOrg = new ReceiverOrganization();
        receiverOrg.setId(UUID.randomUUID());
        receiverOrg.setUser(receiverUser);
        receiverOrg.setOrgName("Hope Community");
        receiverOrg.setRatingAverage(5.0);

        donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setTitle("Surplus Meals");
        donation.setDonor(donorProfile);

        donationRequest = new DonationRequest();
        donationRequest.setId(UUID.randomUUID());
        donationRequest.setDonation(donation);
        donationRequest.setReceiverOrganization(receiverOrg);

        completedPickup = new Pickup();
        completedPickup.setId(pickupId);
        completedPickup.setDonationRequest(donationRequest);
        completedPickup.setReceiver(receiverOrg);
        completedPickup.setStatus(PickupStatus.COMPLETED);

        assignedPickup = new Pickup();
        assignedPickup.setId(UUID.randomUUID());
        assignedPickup.setDonationRequest(donationRequest);
        assignedPickup.setReceiver(receiverOrg);
        assignedPickup.setStatus(PickupStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Donor rates Receiver successfully on COMPLETED pickup")
    void testDonorRatesReceiverSuccess() {
        when(userRepository.findByFirebaseUid("donor-firebase-uid")).thenReturn(Optional.of(donorUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(completedPickup));
        when(ratingRepository.existsByPickupIdAndRaterUserId(pickupId, donorUser.getId())).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(ratingRepository.calculateAverageRatingForUser(receiverUser.getId())).thenReturn(4.8);
        when(receiverOrganizationRepository.findByUserId(receiverUser.getId())).thenReturn(Optional.of(receiverOrg));

        CreateRatingRequest request = new CreateRatingRequest(pickupId, 5, "Great communication and punctual collection.");
        RatingResponse response = ratingService.submitRating("donor-firebase-uid", request, httpRequest);

        assertNotNull(response);
        assertEquals(5, response.rating());
        assertEquals("Great communication and punctual collection.", response.feedback());
        assertEquals(donorUser.getId(), response.raterUserId());
        assertEquals(receiverUser.getId(), response.ratedUserId());
        verify(ratingRepository).save(any(Rating.class));
        verify(receiverOrganizationRepository).save(receiverOrg);
        assertEquals(4.8, receiverOrg.getRatingAverage());
    }

    @Test
    @DisplayName("Receiver rates Donor successfully on COMPLETED pickup")
    void testReceiverRatesDonorSuccess() {
        when(userRepository.findByFirebaseUid("receiver-firebase-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(completedPickup));
        when(ratingRepository.existsByPickupIdAndRaterUserId(pickupId, receiverUser.getId())).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(ratingRepository.calculateAverageRatingForUser(donorUser.getId())).thenReturn(4.5);
        when(donorProfileRepository.findByUserId(donorUser.getId())).thenReturn(Optional.of(donorProfile));

        CreateRatingRequest request = new CreateRatingRequest(pickupId, 4, "Food was well packed and fresh.");
        RatingResponse response = ratingService.submitRating("receiver-firebase-uid", request, httpRequest);

        assertNotNull(response);
        assertEquals(4, response.rating());
        assertEquals("Food was well packed and fresh.", response.feedback());
        assertEquals(receiverUser.getId(), response.raterUserId());
        assertEquals(donorUser.getId(), response.ratedUserId());
        verify(ratingRepository).save(any(Rating.class));
        verify(donorProfileRepository).save(donorProfile);
        assertEquals(4.5, donorProfile.getRatingAverage());
    }

    @Test
    @DisplayName("Reject rating when pickup is NOT completed")
    void testRejectRatingWhenPickupNotCompleted() {
        UUID assignedId = assignedPickup.getId();
        when(userRepository.findByFirebaseUid("donor-firebase-uid")).thenReturn(Optional.of(donorUser));
        when(pickupRepository.findById(assignedId)).thenReturn(Optional.of(assignedPickup));

        CreateRatingRequest request = new CreateRatingRequest(assignedId, 5, "Too early");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ratingService.submitRating("donor-firebase-uid", request, httpRequest)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("completed"));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject duplicate rating submission")
    void testRejectDuplicateRating() {
        when(userRepository.findByFirebaseUid("donor-firebase-uid")).thenReturn(Optional.of(donorUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(completedPickup));
        when(ratingRepository.existsByPickupIdAndRaterUserId(pickupId, donorUser.getId())).thenReturn(true);

        CreateRatingRequest request = new CreateRatingRequest(pickupId, 5, "Duplicate");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ratingService.submitRating("donor-firebase-uid", request, httpRequest)
        );
        assertTrue(ex.getMessage().contains("already submitted"));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject rating from unrelated user")
    void testRejectUnrelatedUserRating() {
        when(userRepository.findByFirebaseUid("unrelated-uid")).thenReturn(Optional.of(unrelatedUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(completedPickup));

        CreateRatingRequest request = new CreateRatingRequest(pickupId, 5, "Intruder");
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ratingService.submitRating("unrelated-uid", request, httpRequest)
        );
        assertTrue(ex.getMessage().contains("not authorized"));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject rating from Admin")
    void testRejectAdminRating() {
        when(userRepository.findByFirebaseUid("admin-uid")).thenReturn(Optional.of(adminUser));

        CreateRatingRequest request = new CreateRatingRequest(pickupId, 5, "Admin trying to rate");
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ratingService.submitRating("admin-uid", request, httpRequest)
        );
        assertTrue(ex.getMessage().contains("Only Donors and Receivers"));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject invalid rating values (< 1 or > 5)")
    void testRejectInvalidRatingValues() {
        CreateRatingRequest lowRequest = new CreateRatingRequest(pickupId, 0, "Too low");
        assertThrows(IllegalArgumentException.class, () ->
                ratingService.submitRating("donor-firebase-uid", lowRequest, httpRequest)
        );

        CreateRatingRequest highRequest = new CreateRatingRequest(pickupId, 6, "Too high");
        assertThrows(IllegalArgumentException.class, () ->
                ratingService.submitRating("donor-firebase-uid", highRequest, httpRequest)
        );
    }

    @Test
    @DisplayName("Get Pickup Rating Status returns correct eligibility and submitted ratings")
    void testGetPickupRatingStatus() {
        when(userRepository.findByFirebaseUid("donor-firebase-uid")).thenReturn(Optional.of(donorUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(completedPickup));
        when(ratingRepository.existsByPickupIdAndRaterUserId(pickupId, donorUser.getId())).thenReturn(false);
        when(ratingRepository.findByPickupId(pickupId)).thenReturn(List.of());

        PickupRatingStatusResponse status = ratingService.getPickupRatingStatus("donor-firebase-uid", pickupId);

        assertNotNull(status);
        assertEquals(pickupId, status.pickupId());
        assertEquals("COMPLETED", status.pickupStatus());
        assertTrue(status.eligibleToRate());
        assertFalse(status.alreadyRated());
        assertEquals("DONOR", status.roleInPickup());
        assertNull(status.myRating());
    }
}

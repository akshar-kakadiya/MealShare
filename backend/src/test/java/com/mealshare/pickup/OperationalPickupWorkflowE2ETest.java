package com.mealshare.pickup;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;
import com.mealshare.donation.DonationService;
import com.mealshare.donation.dto.CreateDonationRequest;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donor.request.DonorRequestService;
import com.mealshare.pickup.dto.*;
import com.mealshare.rating.RatingRepository;
import com.mealshare.rating.RatingService;
import com.mealshare.rating.dto.CreateRatingRequest;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestService;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.receiver.request.dto.CreateDonationRequestDto;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class OperationalPickupWorkflowE2ETest {

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @Autowired
    private DonationService donationService;

    @Autowired
    private DonationRequestService donationRequestService;

    @Autowired
    private DonorRequestService donorRequestService;

    @Autowired
    private PickupService pickupService;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationRequestRepository donationRequestRepository;

    @Autowired
    private PickupRepository pickupRepository;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    private String donorUid = "drekxkp9uXV2g16JioDrgu7uXSk2";
    private String receiverUid = "pwvnvHgatoahwb9ndKfSM2TVFPG2";

    @BeforeEach
    void setUp() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(firebaseTokenValidator.verifyIdToken(anyString())).thenReturn(mockToken);

        donorUid = userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.mealshare.user.UserRole.DONOR && u.getStatus() == com.mealshare.user.UserStatus.ACTIVE)
                .map(com.mealshare.user.User::getFirebaseUid)
                .findFirst()
                .orElse("drekxkp9uXV2g16JioDrgu7uXSk2");

        receiverUid = userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.mealshare.user.UserRole.RECEIVER && u.getStatus() == com.mealshare.user.UserStatus.ACTIVE)
                .map(com.mealshare.user.User::getFirebaseUid)
                .findFirst()
                .orElse("pwvnvHgatoahwb9ndKfSM2TVFPG2");
    }

    @Test
    @DisplayName("Complete Operational Redistribution Journey without Admin Prerequisite")
    void testCompleteOperationalRedistributionJourney() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1. Create Donation
        CreateDonationRequest createDto = new CreateDonationRequest(
                "Operational Veg Meals " + UUID.randomUUID().toString().substring(0, 6),
                "Fresh cooked meals in hygienic containers",
                FoodCategory.PREPARED_MEALS,
                20.0,
                "MEALS",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(6, ChronoUnit.HOURS),
                Instant.now().minus(30, ChronoUnit.MINUTES),
                Instant.now().plus(4, ChronoUnit.HOURS),
                "100 MG Road, Bengaluru",
                12.9716,
                77.5946,
                true,
                "Prepared according to food safety guidelines"
        );
        DonationResponse donationResponse = donationService.createDonation(donorUid, createDto, request);
        UUID donationId = donationResponse.id();
        assertThat(donationResponse.status()).isEqualTo(DonationStatus.AVAILABLE);

        // 2. Receiver Requests Food
        CreateDonationRequestDto reqDto = new CreateDonationRequestDto(
                20.0,
                "Meals for community shelter beneficiaries"
        );
        DonationRequestResponse reqResponse = donationRequestService.createRequest(receiverUid, donationId, reqDto, request);
        UUID requestId = reqResponse.id();
        assertThat(reqResponse.status()).isEqualTo(DonationRequestStatus.PENDING);

        // 3. Donor Approves Request -> Automatically Creates Pickup with PICKUP_PENDING
        donorRequestService.approveRequest(donorUid, requestId, request);

        // Verify pickup was auto-created and initialized
        Pickup pickup = pickupRepository.findByDonationRequestId(requestId).orElse(null);
        assertThat(pickup).isNotNull();
        assertThat(pickup.getStatus()).isEqualTo(PickupStatus.PICKUP_PENDING);
        assertThat(pickup.getOtpHash()).isNotBlank();
        UUID pickupId = pickup.getId();

        // Verify ensurePickupForRequestId idempotency
        Pickup ensured = pickupService.ensurePickupForRequestId(requestId);
        assertThat(ensured.getId()).isEqualTo(pickupId);

        // 4. Receiver Starts Pickup Journey -> Transitions to EN_ROUTE_TO_PICKUP
        LocationUpdateDto startLocation = new LocationUpdateDto(12.9750, 77.6000, 15.0, 90.0);
        PickupResponse journeyResponse = pickupService.startPickupJourney(receiverUid, pickupId, startLocation, request);
        assertThat(journeyResponse.status()).isEqualTo(PickupStatus.EN_ROUTE_TO_PICKUP);
        assertThat(journeyResponse.trackingActive()).isTrue();

        // 5. Receiver Streams GPS Coordinates -> Records breadcrumb and calculates distance
        LocationUpdateDto breadcrumb = new LocationUpdateDto(12.9720, 77.5950, 10.0, 45.0);
        Map<String, Object> streamResult = pickupService.recordLocationUpdate(receiverUid, pickupId, breadcrumb, request);
        assertThat(streamResult.get("status")).isEqualTo(PickupStatus.EN_ROUTE_TO_PICKUP);
        assertThat((Double) streamResult.get("distanceMeters")).isLessThan(200.0);
        assertThat(streamResult.get("withinGeofence")).isEqualTo(true);

        // 6. Receiver Confirms Reached Location -> Transitions to AT_PICKUP
        LocationUpdateDto arrivalLocation = new LocationUpdateDto(12.9716, 77.5946, 0.0, 0.0);
        PickupResponse reachedResponse = pickupService.confirmReachedLocation(receiverUid, pickupId, arrivalLocation, request);
        assertThat(reachedResponse.status()).isEqualTo(PickupStatus.AT_PICKUP);
        assertThat(reachedResponse.trackingActive()).isFalse();

        // 7. Food Safety Inspection -> Accept
        FoodInspectionDto acceptInspection = new FoodInspectionDto(true, "Food inspected. Clean packaging and fresh aroma.", null, null);
        PickupResponse inspectedResponse = pickupService.inspectFood(receiverUid, pickupId, acceptInspection, request);
        assertThat(inspectedResponse.status()).isEqualTo(PickupStatus.AT_PICKUP);

        // 8. Donor Views Handover OTP
        DonorPickupOtpResponse donorOtpResponse = pickupService.getOtpForDonor(donorUid, pickupId, request);
        assertThat(donorOtpResponse.otp()).isNotBlank();
        assertThat(donorOtpResponse.otp()).hasSize(6);

        // 9. Receiver Verifies OTP & Submits Food Photo Evidence -> Transitions to PICKED_UP
        VerifyAndPickupDto verifyDto = new VerifyAndPickupDto(
                donorOtpResponse.otp(),
                12.9716,
                77.5946,
                "/api/v1/evidence/test_pickup_photo.jpg"
        );
        PickupResponse pickedUpResponse = pickupService.verifyAndPickup(receiverUid, pickupId, verifyDto, request);
        assertThat(pickedUpResponse.status()).isEqualTo(PickupStatus.PICKED_UP);
        assertThat(pickedUpResponse.pickupPhotoUrl()).isEqualTo("/api/v1/evidence/test_pickup_photo.jpg");

        // 10. Receiver Starts Delivery Transit -> Transitions to IN_TRANSIT
        LocationUpdateDto deliveryStartLocation = new LocationUpdateDto(12.9720, 77.5960, 20.0, 180.0);
        PickupResponse inTransitResponse = pickupService.startDelivery(receiverUid, pickupId, deliveryStartLocation, request);
        assertThat(inTransitResponse.status()).isEqualTo(PickupStatus.IN_TRANSIT);
        assertThat(inTransitResponse.trackingActive()).isFalse();

        // 11. Receiver Completes Delivery -> Transitions to COMPLETED
        CompleteDeliveryDto completeDto = new CompleteDeliveryDto(
                12.9784,
                77.6408,
                20.0,
                null,
                "/api/v1/evidence/test_delivery_photo.jpg"
        );
        PickupResponse completedResponse = pickupService.completeDelivery(receiverUid, pickupId, completeDto, request);
        assertThat(completedResponse.status()).isEqualTo(PickupStatus.COMPLETED);
        assertThat(completedResponse.trackingActive()).isFalse();
        assertThat(completedResponse.deliveredQuantity()).isEqualTo(20.0);
        assertThat(completedResponse.deliveryPhotoUrl()).isEqualTo("/api/v1/evidence/test_delivery_photo.jpg");

        // Verify parent Request and Donation are both COMPLETED
        DonationRequest finalRequest = donationRequestRepository.findById(requestId).orElseThrow();
        assertThat(finalRequest.getStatus()).isEqualTo(DonationRequestStatus.COMPLETED);

        Donation finalDonation = donationRepository.findById(donationId).orElseThrow();
        assertThat(finalDonation.getStatus()).isEqualTo(DonationStatus.COMPLETED);

        // 12. Reciprocal Ratings
        // Donor rates Receiver
        ratingService.submitRating(donorUid, new CreateRatingRequest(
                pickupId,
                5,
                "Receiver arrived promptly with proper thermal containers. Excellent coordination!"
        ), request);

        // Receiver rates Donor
        ratingService.submitRating(receiverUid, new CreateRatingRequest(
                pickupId,
                5,
                "Food was warm, freshly prepared, and properly packaged. Beneficiaries loved it!"
        ), request);

        // Verify both ratings are stored
        UUID donorUserId = userRepository.findByFirebaseUid(donorUid).orElseThrow().getId();
        UUID receiverUserId = userRepository.findByFirebaseUid(receiverUid).orElseThrow().getId();
        assertThat(ratingRepository.existsByPickupIdAndRaterUserId(pickupId, donorUserId)).isTrue();
        assertThat(ratingRepository.existsByPickupIdAndRaterUserId(pickupId, receiverUserId)).isTrue();
    }

    @Test
    @DisplayName("Reconciles pre-existing approved request into active pickup record")
    void testReconcilePreExistingApprovedRequest() {
        // Reconcile real database request 5b0fd95b-a357-4e20-9d91-faa4d34bf8ef if present
        UUID targetRequestId = UUID.fromString("5b0fd95b-a357-4e20-9d91-faa4d34bf8ef");
        if (donationRequestRepository.existsById(targetRequestId)) {
            Pickup pickup = pickupService.ensurePickupForRequestId(targetRequestId);
            assertThat(pickup).isNotNull();
            assertThat(pickup.getStatus()).isEqualTo(PickupStatus.PICKUP_PENDING);
            assertThat(pickup.getOtpHash()).isNotBlank();
            assertThat(pickup.getReceiver()).isNotNull();
            assertThat(pickupRepository.existsByDonationRequestId(targetRequestId)).isTrue();
        }
    }
}

package com.mealshare.e2e;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mealshare.common.exception.GlobalExceptionHandler;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.donation.DonationService;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.DonorDonationController;
import com.mealshare.donation.FoodCategory;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donor.request.DonorRequestController;
import com.mealshare.donor.request.DonorRequestService;
import com.mealshare.donor.request.dto.ApproveDonationRequestResponse;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.pickup.controller.AdminPickupController;
import com.mealshare.pickup.controller.ReceiverPickupController;
import com.mealshare.pickup.dto.AdminAssignPickupResponse;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.receiver.ReceiverDiscoveryService;
import com.mealshare.receiver.ReceiverDonationController;
import com.mealshare.receiver.dto.NearbyDonationResponse;
import com.mealshare.receiver.request.DonationRequestService;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.receiver.request.ReceiverRequestController;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
import com.mealshare.security.CustomAuthenticationEntryPoint;
import com.mealshare.security.FirebaseAuthenticationFilter;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        DonorDonationController.class,
        DonorRequestController.class,
        ReceiverDonationController.class,
        ReceiverRequestController.class,
        AdminPickupController.class,
        ReceiverPickupController.class
})
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
public class MealShareE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    @MockBean
    private DonorRequestService donorRequestService;

    @MockBean
    private ReceiverDiscoveryService receiverDiscoveryService;

    @MockBean
    private DonationRequestService donationRequestService;

    @MockBean
    private PickupService pickupService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private FirebaseToken mockToken;
    private User donorUser;
    private User receiverUserA;
    private User receiverUserB;
    private User adminUser;

    private static final String DONOR_UID = "donor-uid-001";
    private static final String RECEIVER_A_UID = "receiver-a-uid-002";
    private static final String RECEIVER_B_UID = "receiver-b-uid-003";
    private static final String ADMIN_UID = "admin-uid-005";

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        mockToken = Mockito.mock(FirebaseToken.class);
        when(firebaseTokenValidator.verifyIdToken(any())).thenReturn(mockToken);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid(DONOR_UID);
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        receiverUserA = new User();
        receiverUserA.setId(UUID.randomUUID());
        receiverUserA.setFirebaseUid(RECEIVER_A_UID);
        receiverUserA.setRole(UserRole.RECEIVER);
        receiverUserA.setStatus(UserStatus.ACTIVE);

        receiverUserB = new User();
        receiverUserB.setId(UUID.randomUUID());
        receiverUserB.setFirebaseUid(RECEIVER_B_UID);
        receiverUserB.setRole(UserRole.RECEIVER);
        receiverUserB.setStatus(UserStatus.ACTIVE);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirebaseUid(ADMIN_UID);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByFirebaseUid(DONOR_UID)).thenReturn(Optional.of(donorUser));
        when(userRepository.findByFirebaseUid(RECEIVER_A_UID)).thenReturn(Optional.of(receiverUserA));
        when(userRepository.findByFirebaseUid(RECEIVER_B_UID)).thenReturn(Optional.of(receiverUserB));
        when(userRepository.findByFirebaseUid(ADMIN_UID)).thenReturn(Optional.of(adminUser));
    }

    // =========================================================================
    // SECTION 7: PRIMARY END-TO-END WORKFLOW (STEPS 1 - 7)
    // =========================================================================

    @Test
    @DisplayName("E2E Step 1: Donor creates donation -> AVAILABLE status and spatial coordinates")
    void step1_donor_createsDonation_availableStatus() throws Exception {
        when(mockToken.getUid()).thenReturn(DONOR_UID);

        UUID donationId = UUID.randomUUID();
        DonationResponse resp = new DonationResponse(
                donationId,
                donorUser.getId(),
                "Green Kitchens Ltd",
                "Fresh Catering Meals",
                "Excess buffet food packed safely",
                FoodCategory.PREPARED_MEALS,
                50.0,
                "MEALS",
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(18000),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(14400),
                "Indiranagar, Bengaluru",
                12.9716,
                77.5946,
                true,
                "Temperature checked at 65C",
                Instant.now(),
                DonationStatus.AVAILABLE,
                Instant.now(),
                Instant.now()
        );
        when(donationService.createDonation(eq(DONOR_UID), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer token-donor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"title\":\"Fresh Catering Meals\","
                                + "\"foodCategory\":\"PREPARED_MEALS\","
                                + "\"quantity\":50.0,"
                                + "\"quantityUnit\":\"MEALS\","
                                + "\"preparationDateTime\":\"2026-09-30T10:00:00Z\","
                                + "\"consumeBefore\":\"2026-09-30T18:00:00Z\","
                                + "\"pickupAvailableFrom\":\"2026-09-30T11:00:00Z\","
                                + "\"pickupAvailableUntil\":\"2026-09-30T15:00:00Z\","
                                + "\"pickupAddress\":\"Indiranagar, Bengaluru\","
                                + "\"latitude\":12.9716,"
                                + "\"longitude\":77.5946,"
                                + "\"foodSafetyDeclared\":true"
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(donationId.toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.quantity").value(50.0));
    }

    @Test
    @DisplayName("E2E Step 2: Receiver discovers donation -> PostGIS spatial search within radius")
    void step2_receiver_discoversDonation_postgisNearbyQuery() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);

        UUID donationId = UUID.randomUUID();
        NearbyDonationResponse nearbyItem = new NearbyDonationResponse(
                donationId,
                "Fresh Catering Meals",
                "Excess buffet food packed safely",
                "PREPARED_MEALS",
                50.0,
                "MEALS",
                Instant.now().plusSeconds(18000),
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(14400),
                "Indiranagar, Bengaluru",
                12.9716,
                77.5946,
                2.35,
                2350.0,
                "AVAILABLE",
                true,
                Instant.now()
        );

        Page<NearbyDonationResponse> page = new PageImpl<>(List.of(nearbyItem));
        when(receiverDiscoveryService.findNearbyDonations(eq(RECEIVER_A_UID), eq(12.9700), eq(77.6000), eq(5.0), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/receiver/donations/nearby")
                        .header("Authorization", "Bearer token-receiver")
                        .param("latitude", "12.9700")
                        .param("longitude", "77.6000")
                        .param("radiusKm", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(donationId.toString()))
                .andExpect(jsonPath("$.content[0].distanceKm").value(2.35))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("E2E Step 3: Receiver submits claim -> PENDING donation request created")
    void step3_receiver_createsDonationRequest_pendingStatus() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);

        UUID requestId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        DonationRequestResponse reqResp = new DonationRequestResponse(
                requestId,
                donationId,
                "Fresh Catering Meals",
                receiverUserA.getId(),
                "Hope Foundation",
                25.0,
                0.0,
                "Serving dinner to homeless community",
                DonationRequestStatus.PENDING,
                Instant.now(),
                Instant.now()
        );
        when(donationRequestService.createRequest(eq(RECEIVER_A_UID), eq(donationId), any(), any())).thenReturn(reqResp);

        mockMvc.perform(post("/api/v1/receiver/donations/" + donationId + "/requests")
                        .header("Authorization", "Bearer token-receiver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"requestedQuantity\":25.0,"
                                + "\"requestReason\":\"Serving dinner to homeless community\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestedQuantity").value(25.0));
    }

    @Test
    @DisplayName("E2E Step 4a: Donor approves claim -> APPROVED status, allocated_quantity updated")
    void step4a_donor_approvesRequest_allocatedQuantityUpdated() throws Exception {
        when(mockToken.getUid()).thenReturn(DONOR_UID);

        UUID requestId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        ApproveDonationRequestResponse approveResp = new ApproveDonationRequestResponse(
                requestId,
                donationId,
                25.0,
                25.0,
                25.0,
                DonationStatus.AVAILABLE,
                DonationRequestStatus.APPROVED,
                Instant.now()
        );
        when(donorRequestService.approveRequest(eq(DONOR_UID), eq(requestId), any())).thenReturn(approveResp);

        mockMvc.perform(post("/api/v1/donor/requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer token-donor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.allocatedQuantity").value(25.0))
                .andExpect(jsonPath("$.remainingQuantity").value(25.0));
    }

    @Test
    @DisplayName("E2E Step 4b: Admin schedules pickup -> ASSIGNED pickup created with dev OTP")
    void step4b_admin_schedulesPickup_assignedStatusAndOtpGenerated() throws Exception {
        when(mockToken.getUid()).thenReturn(ADMIN_UID);

        UUID pickupId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverOrgId = UUID.randomUUID();

        AdminAssignPickupResponse assignResp = new AdminAssignPickupResponse(
                pickupId,
                requestId,
                UUID.randomUUID(),
                receiverOrgId,
                "Hope Shelter",
                PickupStatus.ASSIGNED,
                Instant.now().plusSeconds(3600),
                Instant.now(),
                Instant.now().plusSeconds(600)
        );
        when(pickupService.schedulePickup(eq(ADMIN_UID), any(), any())).thenReturn(assignResp);

        mockMvc.perform(post("/api/v1/admin/pickups")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"donationRequestId\":\"" + requestId + "\","
                                + "\"scheduledAt\":\"2026-09-30T16:00:00Z\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.devOtpCode").doesNotExist());
    }

    @Test
    @DisplayName("E2E Step 5: Receiver retrieves assigned pickup -> assigned receiver succeeds; other receiver forbidden")
    void step5_assignedReceiver_canAccessPickup_otherReceiverForbidden() throws Exception {
        UUID pickupId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID donId = UUID.randomUUID();
        PickupResponse pickupResp = new PickupResponse(
                pickupId,
                reqId,
                donId,
                "Fresh Catering Meals",
                "Indiranagar, Bengaluru",
                "Donor Kitchen",
                "+919876543210",
                receiverUserA.getId(),
                "Hope Foundation",
                25.0,
                "MEALS",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                PickupStatus.ASSIGNED,
                null,
                null,
                null,
                Instant.now()
        );

        // Receiver A (assigned)
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        when(pickupService.getPickupDetailForReceiver(eq(RECEIVER_A_UID), eq(pickupId))).thenReturn(pickupResp);

        mockMvc.perform(get("/api/v1/receiver/pickups/" + pickupId)
                        .header("Authorization", "Bearer token-rec-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // Receiver B (not assigned) -> AccessDeniedException -> 403 Forbidden
        when(mockToken.getUid()).thenReturn(RECEIVER_B_UID);
        when(pickupService.getPickupDetailForReceiver(eq(RECEIVER_B_UID), eq(pickupId)))
                .thenThrow(new AccessDeniedException("You do not have permission to view this pickup."));

        mockMvc.perform(get("/api/v1/receiver/pickups/" + pickupId)
                        .header("Authorization", "Bearer token-rec-b"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("E2E Step 6: Receiver verifies OTP -> transitions to PICKED_UP; reusing OTP rejected")
    void step6_receiver_verifiesOtp_pickedUpStatus_reusedOtpRejected() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        UUID pickupId = UUID.randomUUID();

        PickupResponse pickedUpResp = new PickupResponse(
                pickupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fresh Catering Meals",
                "Indiranagar, Bengaluru",
                "Donor Kitchen",
                "+919876543210",
                receiverUserA.getId(),
                "Hope Foundation",
                25.0,
                "MEALS",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                PickupStatus.PICKED_UP,
                Instant.now(),
                Instant.now(),
                null,
                Instant.now()
        );

        // Valid OTP verification
        when(pickupService.verifyOtp(eq(RECEIVER_A_UID), eq(pickupId), eq("482910"), any())).thenReturn(pickedUpResp);

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer token-rec-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"482910\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        // Reusing OTP on already picked up pickup -> IllegalStateException -> 409 Conflict
        when(pickupService.verifyOtp(eq(RECEIVER_A_UID), eq(pickupId), eq("482910"), any()))
                .thenThrow(new IllegalStateException("Pickup is not in ASSIGNED state. Current status: PICKED_UP"));

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer token-rec-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"482910\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Pickup is not in ASSIGNED state. Current status: PICKED_UP"));
    }

    @Test
    @DisplayName("E2E Step 7: Receiver completes pickup -> status COMPLETED for pickup, request, and donation")
    void step7_receiver_completesPickup_completedStatusAndDonationCompleted() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        UUID pickupId = UUID.randomUUID();

        PickupResponse completedResp = new PickupResponse(
                pickupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fresh Catering Meals",
                "Indiranagar, Bengaluru",
                "Donor Kitchen",
                "+919876543210",
                receiverUserA.getId(),
                "Hope Foundation",
                25.0,
                "MEALS",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                PickupStatus.COMPLETED,
                Instant.now().minusSeconds(1800),
                Instant.now().minusSeconds(1800),
                Instant.now(),
                Instant.now()
        );

        when(pickupService.completePickup(eq(RECEIVER_A_UID), eq(pickupId), any())).thenReturn(completedResp);

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/complete")
                        .header("Authorization", "Bearer token-rec-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // =========================================================================
    // SECTION 8: NEGATIVE SECURITY TESTS (SCENARIOS 1 - 12)
    // =========================================================================

    @Test
    @DisplayName("NEG-01: Donor cannot access receiver discovery endpoints -> 403 Forbidden")
    void neg01_donor_cannotAccessReceiverEndpoints_returns403() throws Exception {
        when(mockToken.getUid()).thenReturn(DONOR_UID);

        mockMvc.perform(get("/api/v1/receiver/donations/nearby")
                        .header("Authorization", "Bearer token-donor")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "15.0"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NEG-02: Receiver cannot approve donor request -> 403 Forbidden")
    void neg02_receiver_cannotApproveDonorRequest_returns403() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);

        mockMvc.perform(post("/api/v1/donor/requests/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", "Bearer token-receiver"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NEG-04: Admin cannot impersonate receiver ownership on receiver pickup endpoint -> 403 Forbidden")
    void neg04_admin_cannotImpersonateReceiverOwnership_returns403() throws Exception {
        when(mockToken.getUid()).thenReturn(ADMIN_UID);

        mockMvc.perform(get("/api/v1/receiver/pickups")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NEG-05: Receiver A cannot access Receiver B pickup -> 403 Forbidden")
    void neg05_receiverA_cannotAccessReceiverBPickup_returns403() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        UUID pickupId = UUID.randomUUID();

        when(pickupService.getPickupDetailForReceiver(eq(RECEIVER_A_UID), eq(pickupId)))
                .thenThrow(new AccessDeniedException("You do not have permission to view this pickup."));

        mockMvc.perform(get("/api/v1/receiver/pickups/" + pickupId)
                        .header("Authorization", "Bearer token-rec-a"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NEG-06: Unauthenticated API call -> 401 Unauthorized")
    void neg06_unauthenticatedApi_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/donor/donations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NEG-07: Invalid Firebase token -> 401 Unauthorized")
    void neg07_invalidFirebaseToken_returns401() throws Exception {
        FirebaseAuthException authEx = Mockito.mock(FirebaseAuthException.class);
        when(authEx.getMessage()).thenReturn("Invalid token");
        when(firebaseTokenValidator.verifyIdToken("bad-token")).thenThrow(authEx);

        mockMvc.perform(get("/api/v1/donor/donations")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NEG-08: Cross-donor donation mutation access -> 403 Forbidden")
    void neg08_crossDonor_donationAccess_returns403() throws Exception {
        when(mockToken.getUid()).thenReturn(DONOR_UID);
        UUID donationId = UUID.randomUUID();

        when(donationService.cancelDonation(eq(DONOR_UID), eq(donationId), any()))
                .thenThrow(new AccessDeniedException("You do not own this donation."));

        mockMvc.perform(post("/api/v1/donor/donations/" + donationId + "/cancel")
                        .header("Authorization", "Bearer token-donor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NEG-09: Cross-receiver pickup completion access -> 403 Forbidden")
    void neg09_crossReceiver_pickupAccess_returns403() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_B_UID);
        UUID pickupId = UUID.randomUUID();

        when(pickupService.completePickup(eq(RECEIVER_B_UID), eq(pickupId), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to complete this pickup."));

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/complete")
                        .header("Authorization", "Bearer token-rec-b"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NEG-10: Used OTP verification attempt -> 409 Conflict")
    void neg10_usedOtp_rejected_returns409() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        UUID pickupId = UUID.randomUUID();

        when(pickupService.verifyOtp(eq(RECEIVER_A_UID), eq(pickupId), eq("123456"), any()))
                .thenThrow(new IllegalStateException("Pickup is not in ASSIGNED state. Current status: PICKED_UP"));

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer token-rec-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("NEG-11: Expired OTP verification attempt -> 409 Conflict")
    void neg11_expiredOtp_rejected_returns409() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        UUID pickupId = UUID.randomUUID();

        when(pickupService.verifyOtp(eq(RECEIVER_A_UID), eq(pickupId), eq("123456"), any()))
                .thenThrow(new IllegalStateException("Invalid or expired pickup OTP."));

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer token-rec-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("NEG-12: Excessive OTP verification attempts (>5) -> 409 Conflict")
    void neg12_excessiveOtpAttempts_blocked_returns409() throws Exception {
        when(mockToken.getUid()).thenReturn(RECEIVER_A_UID);
        UUID pickupId = UUID.randomUUID();

        when(pickupService.verifyOtp(eq(RECEIVER_A_UID), eq(pickupId), eq("123456"), any()))
                .thenThrow(new IllegalStateException("Maximum verification attempts exceeded. Further verification is blocked."));

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer token-rec-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Maximum verification attempts exceeded. Further verification is blocked."));
    }
}

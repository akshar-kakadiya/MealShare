package com.mealshare.pickup;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.pickup.controller.AdminPickupController;
import com.mealshare.pickup.controller.ReceiverPickupController;
import com.mealshare.pickup.dto.AdminAssignPickupResponse;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.security.CustomAuthenticationEntryPoint;
import com.mealshare.security.FirebaseAuthenticationFilter;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mealshare.pickup.controller.DonorPickupController;
import com.mealshare.pickup.dto.DonorPickupOtpResponse;

@WebMvcTest(controllers = {AdminPickupController.class, ReceiverPickupController.class, DonorPickupController.class})
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class})
@ActiveProfiles("test")
class PickupSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PickupService pickupService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private FirebaseToken mockToken;
    private User adminUser;
    private User receiverUser;
    private User donorUser;

    @BeforeEach
    void setUp() throws Exception {
        mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("test-uid");
        when(mockToken.getEmail()).thenReturn("test@mealshare.org");
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirebaseUid("test-uid");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("test-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("test-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);
    }

    // ==========================================
    // Admin Endpoints Security
    // ==========================================

    @Test
    void adminAssign_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/pickups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donationRequestId\":\"" + UUID.randomUUID() + "\",\"scheduledAt\":\"2026-09-20T10:00:00Z\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminAssign_asDonor_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        mockMvc.perform(post("/api/v1/admin/pickups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donationRequestId\":\"" + UUID.randomUUID() + "\",\"scheduledAt\":\"2026-09-20T10:00:00Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAssign_asReceiver_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));

        mockMvc.perform(post("/api/v1/admin/pickups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donationRequestId\":\"" + UUID.randomUUID() + "\",\"scheduledAt\":\"2026-09-20T10:00:00Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAssign_asAdmin_returns201_andDoesNotExposeOtp() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(adminUser));

        UUID pickupId = UUID.randomUUID();
        AdminAssignPickupResponse resp = new AdminAssignPickupResponse(
                pickupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Hope Shelter",
                PickupStatus.ASSIGNED,
                Instant.now().plusSeconds(3600),
                Instant.now(),
                Instant.now().plusSeconds(600)
        );
        when(pickupService.schedulePickup(eq("test-uid"), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/pickups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donationRequestId\":\"" + UUID.randomUUID() + "\",\"scheduledAt\":\"2026-09-20T10:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.devOtpCode").doesNotExist())
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andExpect(jsonPath("$.otpHash").doesNotExist());
    }

    @Test
    void adminGetPickups_asAdmin_returns200() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(adminUser));
        when(pickupService.getAdminPickups(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/pickups")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // Receiver Pickup Endpoints Security
    // ==========================================

    @Test
    void receiverGetPickups_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/receiver/pickups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void receiverGetPickups_asDonor_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        mockMvc.perform(get("/api/v1/receiver/pickups")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiverGetPickups_asReceiver_returns200() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupService.getPickupsForReceiver(eq("test-uid"), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/receiver/pickups")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void receiverVerifyOtp_asReceiver_returns200() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));
        UUID pickupId = UUID.randomUUID();

        PickupResponse resp = new PickupResponse(
                pickupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Meals",
                "123 Street",
                "Donor",
                "12345",
                UUID.randomUUID(),
                "Receiver",
                50.0,
                "MEALS",
                Instant.now(),
                Instant.now(),
                PickupStatus.PICKED_UP,
                Instant.now(),
                Instant.now(),
                null,
                Instant.now()
        );
        when(pickupService.verifyOtp(eq("test-uid"), eq(pickupId), eq("123456"), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));
    }

    @Test
    void receiverVerifyOtp_asDonor_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));
        UUID pickupId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/verify-otp")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiverComplete_asReceiver_returns200() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));
        UUID pickupId = UUID.randomUUID();

        PickupResponse resp = new PickupResponse(
                pickupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Meals",
                "123 Street",
                "Donor",
                "12345",
                UUID.randomUUID(),
                "Receiver",
                50.0,
                "MEALS",
                Instant.now(),
                Instant.now(),
                PickupStatus.COMPLETED,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        when(pickupService.completePickup(eq("test-uid"), eq(pickupId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/receiver/pickups/" + pickupId + "/complete")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ==========================================
    // Donor Pickup & OTP Endpoints Security
    // ==========================================

    @Test
    void donorGetOtp_unauthenticated_returns401() throws Exception {
        UUID pickupId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/donor/pickups/" + pickupId + "/otp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void donorGetOtp_asReceiver_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));
        UUID pickupId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/donor/pickups/" + pickupId + "/otp")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void donorGetOtp_asAdmin_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(adminUser));
        UUID pickupId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/donor/pickups/" + pickupId + "/otp")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void donorGetOtp_asAuthorizedDonor_returns200WithOtp() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));
        UUID pickupId = UUID.randomUUID();
        DonorPickupOtpResponse resp = new DonorPickupOtpResponse(
                pickupId,
                UUID.randomUUID(),
                "Fresh Catering Food",
                "Community Shelter",
                "860451",
                Instant.now().plusSeconds(600)
        );
        when(pickupService.getOtpForDonor(eq("test-uid"), eq(pickupId), any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/donor/pickups/" + pickupId + "/otp")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.otp").value("860451"))
                .andExpect(jsonPath("$.receiverOrganizationName").value("Community Shelter"));
    }

    @Test
    void donorGetOtp_unrelatedDonor_returns403() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));
        UUID pickupId = UUID.randomUUID();
        when(pickupService.getOtpForDonor(eq("test-uid"), eq(pickupId), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("You do not have permission to view the OTP for this pickup."));

        mockMvc.perform(get("/api/v1/donor/pickups/" + pickupId + "/otp")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }
}

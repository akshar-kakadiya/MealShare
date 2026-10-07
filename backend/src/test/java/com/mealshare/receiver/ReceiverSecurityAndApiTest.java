package com.mealshare.receiver;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.receiver.request.DonationRequestService;
import com.mealshare.receiver.request.ReceiverRequestController;
import com.mealshare.receiver.request.dto.CreateDonationRequestDto;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ReceiverDonationController.class, ReceiverProfileController.class, ReceiverRequestController.class})
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class})
@ActiveProfiles("test")
class ReceiverSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceiverDiscoveryService receiverDiscoveryService;

    @MockBean
    private DonationRequestService donationRequestService;

    @MockBean
    private ReceiverOrganizationService receiverOrganizationService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private FirebaseToken mockToken;
    private User receiverOrgUser;
    private User donorUser;

    @BeforeEach
    void setUp() throws Exception {
        mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("test-uid");
        when(mockToken.getEmail()).thenReturn("receiver@org.com");
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);

        receiverOrgUser = new User();
        receiverOrgUser.setId(UUID.randomUUID());
        receiverOrgUser.setFirebaseUid("test-uid");
        receiverOrgUser.setRole(UserRole.RECEIVER);
        receiverOrgUser.setStatus(UserStatus.ACTIVE);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("test-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void test1_receiverCanAccessNearbyDonations() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverOrgUser));
        when(receiverDiscoveryService.findNearbyDonations(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/receiver/donations/nearby")
                        .header("Authorization", "Bearer valid-token")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "10.0"))
                .andExpect(status().isOk());
    }

    @Test
    void test2_donorCannotAccessReceiverDiscovery() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        mockMvc.perform(get("/api/v1/receiver/donations/nearby")
                        .header("Authorization", "Bearer valid-token")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "10.0"))
                .andExpect(status().isForbidden());
    }

    @Test
    void test3_donorCannotCreateDonationRequest() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        UUID donationId = UUID.randomUUID();
        String body = "{\"requestedQuantity\": 10.0, \"requestReason\": \"Need food\"}";
        mockMvc.perform(post("/api/v1/receiver/donations/" + donationId + "/requests")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void test4_missingAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/receiver/donations/nearby")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "10.0"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void test5_invalidToken_returns401() throws Exception {
        when(firebaseTokenValidator.verifyIdToken("bad-token")).thenThrow(new RuntimeException("Bad token"));

        mockMvc.perform(get("/api/v1/receiver/donations/nearby")
                        .header("Authorization", "Bearer bad-token")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "10.0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test20_receiverOrgCanCreateDonationRequest() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverOrgUser));

        UUID donationId = UUID.randomUUID();
        String body = "{\"requestedQuantity\": 25.0, \"requestReason\": \"Community kitchen\"}";

        mockMvc.perform(post("/api/v1/receiver/donations/" + donationId + "/requests")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void test25_requestedQuantityZero_returns400() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverOrgUser));

        UUID donationId = UUID.randomUUID();
        String body = "{\"requestedQuantity\": 0.0, \"requestReason\": \"Invalid\"}";

        mockMvc.perform(post("/api/v1/receiver/donations/" + donationId + "/requests")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

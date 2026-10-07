package com.mealshare.donor.request;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.donor.request.dto.ApproveDonationRequestResponse;
import com.mealshare.donor.request.dto.DonorDonationRequestResponse;
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

@WebMvcTest(controllers = DonorRequestController.class)
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class})
@ActiveProfiles("test")
class DonorRequestSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonorRequestService donorRequestService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private FirebaseToken mockToken;
    private User donorUser;
    private User receiverUser;

    @BeforeEach
    void setUp() throws Exception {
        mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("test-uid");
        when(mockToken.getEmail()).thenReturn("donor@org.com");
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("test-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("test-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void test1_donorAccessesOwnRequests() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));
        when(donorRequestService.getRequestsForDonation(any(), any(), any())).thenReturn(Page.empty());

        UUID donationId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/donor/donations/" + donationId + "/requests")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void test3_receiverCannotAccessDonorRequestEndpoint() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));

        UUID donationId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/donor/donations/" + donationId + "/requests")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void test5_unauthenticated_returns401() throws Exception {
        UUID donationId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/donor/donations/" + donationId + "/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void test6_donorCanApproveRequest() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        UUID reqId = UUID.randomUUID();
        ApproveDonationRequestResponse response = new ApproveDonationRequestResponse(
                reqId, UUID.randomUUID(), 50.0, 50.0, 50.0,
                com.mealshare.donation.DonationStatus.APPROVED,
                com.mealshare.receiver.request.DonationRequestStatus.APPROVED,
                Instant.now()
        );

        when(donorRequestService.approveRequest(eq("test-uid"), eq(reqId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/donor/requests/" + reqId + "/approve")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocatedQuantity").value(50.0))
                .andExpect(jsonPath("$.requestStatus").value("APPROVED"));
    }

    @Test
    void testReceiverCannotApproveRequest() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));

        UUID reqId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/donor/requests/" + reqId + "/approve")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void test25_donorCanRejectRequest() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        UUID reqId = UUID.randomUUID();
        DonorDonationRequestResponse response = new DonorDonationRequestResponse(
                reqId, UUID.randomUUID(), "Lunch Meals", UUID.randomUUID(), "Care Org",
                30.0, "MEALS", "Insufficient capacity",
                com.mealshare.receiver.request.DonationRequestStatus.REJECTED,
                Instant.now(), Instant.now()
        );

        when(donorRequestService.rejectRequest(eq("test-uid"), eq(reqId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/donor/requests/" + reqId + "/reject")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Insufficient capacity\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}

package com.mealshare.rating;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.admin.controller.AdminRatingController;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.rating.dto.AdminRatingResponse;
import com.mealshare.rating.dto.CreateRatingRequest;
import com.mealshare.rating.dto.PickupRatingStatusResponse;
import com.mealshare.rating.dto.RatingResponse;
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

@WebMvcTest(controllers = {RatingController.class, AdminRatingController.class})
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class})
@ActiveProfiles("test")
class RatingSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RatingService ratingService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private User donorUser;
    private User receiverUser;
    private User adminUser;
    private UUID pickupId;

    @BeforeEach
    void setUp() throws Exception {
        pickupId = UUID.randomUUID();

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setName("Donor User");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid");
        receiverUser.setName("Receiver User");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirebaseUid("admin-uid");
        adminUser.setName("Admin User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(userRepository.findByFirebaseUid("admin-uid")).thenReturn(Optional.of(adminUser));
    }

    private void mockValidToken(String uid) throws Exception {
        FirebaseToken token = Mockito.mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(firebaseTokenValidator.verifyIdToken("Bearer-token-" + uid)).thenReturn(token);
    }

    @Test
    @DisplayName("Donor can submit valid rating -> HTTP 201 Created")
    void testDonorSubmitsRating() throws Exception {
        mockValidToken("donor-uid");

        RatingResponse response = new RatingResponse(
                UUID.randomUUID(), pickupId, donorUser.getId(), "Donor User", "DONOR",
                receiverUser.getId(), "Receiver User", "RECEIVER", 5, "Excellent coordination", Instant.now()
        );
        when(ratingService.submitRating(eq("donor-uid"), any(), any())).thenReturn(response);

        String json = String.format("{\"pickupId\":\"%s\",\"rating\":5,\"feedback\":\"Excellent coordination\"}", pickupId);

        mockMvc.perform(post("/api/v1/ratings")
                        .header("Authorization", "Bearer Bearer-token-donor-uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.raterRole").value("DONOR"));
    }

    @Test
    @DisplayName("Receiver can submit valid rating -> HTTP 201 Created")
    void testReceiverSubmitsRating() throws Exception {
        mockValidToken("receiver-uid");

        RatingResponse response = new RatingResponse(
                UUID.randomUUID(), pickupId, receiverUser.getId(), "Receiver User", "RECEIVER",
                donorUser.getId(), "Donor User", "DONOR", 4, "Food was fresh", Instant.now()
        );
        when(ratingService.submitRating(eq("receiver-uid"), any(), any())).thenReturn(response);

        String json = String.format("{\"pickupId\":\"%s\",\"rating\":4,\"feedback\":\"Food was fresh\"}", pickupId);

        mockMvc.perform(post("/api/v1/ratings")
                        .header("Authorization", "Bearer Bearer-token-receiver-uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.raterRole").value("RECEIVER"));
    }

    @Test
    @DisplayName("Unauthenticated request to submit rating -> HTTP 401 Unauthorized")
    void testUnauthenticatedSubmitRating() throws Exception {
        String json = String.format("{\"pickupId\":\"%s\",\"rating\":5,\"feedback\":\"Great\"}", pickupId);

        mockMvc.perform(post("/api/v1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin blocked from submitting rating -> HTTP 403 Forbidden")
    void testAdminBlockedFromRating() throws Exception {
        mockValidToken("admin-uid");

        String json = String.format("{\"pickupId\":\"%s\",\"rating\":5,\"feedback\":\"Admin rating\"}", pickupId);

        mockMvc.perform(post("/api/v1/ratings")
                        .header("Authorization", "Bearer Bearer-token-admin-uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invalid rating range (0 or 6) -> HTTP 400 Bad Request")
    void testInvalidRatingRange() throws Exception {
        mockValidToken("donor-uid");

        String jsonLow = String.format("{\"pickupId\":\"%s\",\"rating\":0,\"feedback\":\"Invalid\"}", pickupId);
        mockMvc.perform(post("/api/v1/ratings")
                        .header("Authorization", "Bearer Bearer-token-donor-uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLow))
                .andExpect(status().isBadRequest());

        String jsonHigh = String.format("{\"pickupId\":\"%s\",\"rating\":6,\"feedback\":\"Invalid\"}", pickupId);
        mockMvc.perform(post("/api/v1/ratings")
                        .header("Authorization", "Bearer Bearer-token-donor-uid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonHigh))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get Pickup Rating Status -> HTTP 200 OK")
    void testGetPickupRatingStatus() throws Exception {
        mockValidToken("donor-uid");

        PickupRatingStatusResponse statusResponse = new PickupRatingStatusResponse(
                pickupId, "COMPLETED", true, false, "DONOR", null, null, List.of()
        );
        when(ratingService.getPickupRatingStatus("donor-uid", pickupId)).thenReturn(statusResponse);

        mockMvc.perform(get("/api/v1/ratings/pickup/" + pickupId)
                        .header("Authorization", "Bearer Bearer-token-donor-uid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupId").value(pickupId.toString()))
                .andExpect(jsonPath("$.eligibleToRate").value(true))
                .andExpect(jsonPath("$.alreadyRated").value(false));
    }

    @Test
    @DisplayName("Admin view all ratings -> HTTP 200 OK")
    void testAdminViewRatings() throws Exception {
        mockValidToken("admin-uid");

        AdminRatingResponse adminRating = new AdminRatingResponse(
                UUID.randomUUID(), pickupId, UUID.randomUUID(), "Surplus Bread",
                donorUser.getId(), "Donor User", "DONOR",
                receiverUser.getId(), "Receiver User", "RECEIVER",
                5, "Splendid delivery", Instant.now()
        );
        Page<AdminRatingResponse> page = new PageImpl<>(List.of(adminRating));
        when(ratingService.getAllRatingsForAdmin(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/ratings")
                        .header("Authorization", "Bearer Bearer-token-admin-uid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].raterName").value("Donor User"));
    }

    @Test
    @DisplayName("Donor blocked from Admin ratings endpoint -> HTTP 403 Forbidden")
    void testDonorBlockedFromAdminRatings() throws Exception {
        mockValidToken("donor-uid");

        mockMvc.perform(get("/api/v1/admin/ratings")
                        .header("Authorization", "Bearer Bearer-token-donor-uid"))
                .andExpect(status().isForbidden());
    }
}

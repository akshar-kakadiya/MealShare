package com.mealshare.donation;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.donor.DonorProfileController;
import com.mealshare.donor.DonorProfileService;
import com.mealshare.security.CustomAuthenticationEntryPoint;
import com.mealshare.security.FirebaseAuthenticationFilter;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DonorDonationController.class, DonorProfileController.class})
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class})
@ActiveProfiles("test")
class DonorDonationSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonationService donationService;

    @MockBean
    private DonorProfileService donorProfileService;

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
        when(mockToken.getEmail()).thenReturn("test@mealshare.org");
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

    private String getValidJsonPayload() {
        Instant now = Instant.now();
        Instant prep = now.minus(1, ChronoUnit.HOURS);
        Instant pickupFrom = now.plus(10, ChronoUnit.MINUTES);
        Instant pickupUntil = now.plus(3, ChronoUnit.HOURS);
        Instant consume = now.plus(5, ChronoUnit.HOURS);

        return String.format("""
            {
                "title": "Vegetarian Pulao",
                "description": "Hot and fresh meals",
                "foodCategory": "PREPARED_MEALS",
                "quantity": 30.0,
                "quantityUnit": "MEALS",
                "preparationDateTime": "%s",
                "consumeBefore": "%s",
                "pickupAvailableFrom": "%s",
                "pickupAvailableUntil": "%s",
                "pickupAddress": "Indiranagar, Bengaluru",
                "latitude": 12.9784,
                "longitude": 77.6408,
                "foodSafetyDeclared": true,
                "foodSafetyDeclarationText": "Hygienically packed"
            }
            """, prep, consume, pickupFrom, pickupUntil);
    }

    @Test
    void test1_donorCanCreateDonation() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload()))
                .andExpect(status().isCreated());
    }

    @Test
    void test2_receiverCannotCreateDonorDonation() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(receiverUser));

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload()))
                .andExpect(status().isForbidden());
    }



    @Test
    void test4_missingAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/donor/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void test5_invalidToken_returns401() throws Exception {
        when(firebaseTokenValidator.verifyIdToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test6_missingFoodSafetyDeclaration_returns400() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        Instant now = Instant.now();
        String jsonWithoutSafety = String.format("""
            {
                "title": "Meals",
                "foodCategory": "PREPARED_MEALS",
                "quantity": 10.0,
                "quantityUnit": "MEALS",
                "preparationDateTime": "%s",
                "consumeBefore": "%s",
                "pickupAvailableFrom": "%s",
                "pickupAvailableUntil": "%s",
                "pickupAddress": "Address",
                "latitude": 12.0,
                "longitude": 77.0
            }
            """, now.minus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), now, now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutSafety))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test7_invalidQuantity_returns400() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        Instant now = Instant.now();
        String jsonInvalidQty = String.format("""
            {
                "title": "Meals",
                "foodCategory": "PREPARED_MEALS",
                "quantity": -5.0,
                "quantityUnit": "MEALS",
                "preparationDateTime": "%s",
                "consumeBefore": "%s",
                "pickupAvailableFrom": "%s",
                "pickupAvailableUntil": "%s",
                "pickupAddress": "Address",
                "latitude": 12.0,
                "longitude": 77.0,
                "foodSafetyDeclared": true
            }
            """, now.minus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), now, now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalidQty))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test8_invalidCoordinates_returns400() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));

        Instant now = Instant.now();
        String jsonInvalidCoords = String.format("""
            {
                "title": "Meals",
                "foodCategory": "PREPARED_MEALS",
                "quantity": 10.0,
                "quantityUnit": "MEALS",
                "preparationDateTime": "%s",
                "consumeBefore": "%s",
                "pickupAvailableFrom": "%s",
                "pickupAvailableUntil": "%s",
                "pickupAddress": "Address",
                "latitude": 120.0,
                "longitude": 77.0,
                "foodSafetyDeclared": true
            }
            """, now.minus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS), now, now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/v1/donor/donations")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalidCoords))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test23_and_test24_paginationAndDefaultSortingNewestFirst() throws Exception {
        when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Optional.of(donorUser));
        when(donationService.getMyDonations(eq("test-uid"), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/donor/donations")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(donationService).getMyDonations(eq("test-uid"), captor.capture());

        Pageable capturedPageable = captor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(20, capturedPageable.getPageSize());

        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertNotNull(order, "Default sort must be on createdAt");
        assertEquals(Sort.Direction.DESC, order.getDirection(), "Default sort direction must be DESC");
    }
}

package com.mealshare.notification;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.common.exception.GlobalExceptionHandler;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.notification.dto.DeviceTokenRequest;
import com.mealshare.notification.dto.MarkAllReadResponse;
import com.mealshare.notification.dto.NotificationResponse;
import com.mealshare.notification.dto.UnreadCountResponse;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class NotificationSecurityAndApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private FirebaseToken mockTokenA;
    private FirebaseToken mockTokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() throws Exception {
        mockTokenA = Mockito.mock(FirebaseToken.class);
        when(mockTokenA.getUid()).thenReturn("user-a-uid");

        mockTokenB = Mockito.mock(FirebaseToken.class);
        when(mockTokenB.getUid()).thenReturn("user-b-uid");

        when(firebaseTokenValidator.verifyIdToken("token-user-a")).thenReturn(mockTokenA);
        when(firebaseTokenValidator.verifyIdToken("token-user-b")).thenReturn(mockTokenB);

        userA = new User();
        userA.setId(UUID.randomUUID());
        userA.setFirebaseUid("user-a-uid");
        userA.setRole(UserRole.DONOR);
        userA.setStatus(UserStatus.ACTIVE);

        userB = new User();
        userB.setId(UUID.randomUUID());
        userB.setFirebaseUid("user-b-uid");
        userB.setRole(UserRole.RECEIVER);
        userB.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByFirebaseUid("user-a-uid")).thenReturn(Optional.of(userA));
        when(userRepository.findByFirebaseUid("user-b-uid")).thenReturn(Optional.of(userB));
    }

    @Test
    @DisplayName("SEC-01: Unauthenticated request to /api/v1/notifications returns 401 Unauthorized")
    void test1_unauthenticatedNotificationRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-02: User A can retrieve their own notifications; User A only receives notifications for User A")
    void test2_userA_canRetrieveOwnNotifications() throws Exception {
        UUID notifId = UUID.randomUUID();
        NotificationResponse resp = new NotificationResponse(
                notifId,
                NotificationType.REQUEST_CREATED,
                "New Donation Request",
                "Hope Shelter requested 25 MEALS of 'Fresh Bread'.",
                "DONATION_REQUEST",
                UUID.randomUUID(),
                false,
                Instant.now(),
                null
        );

        when(notificationService.getMyNotifications(eq("user-a-uid"), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer token-user-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notifId.toString()))
                .andExpect(jsonPath("$.content[0].type").value("REQUEST_CREATED"))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    @DisplayName("SEC-03: User A attempting to mark User B's notification as read returns 403 Forbidden")
    void test3_userA_markUserBNotificationRead_returns403() throws Exception {
        UUID notifBId = UUID.randomUUID();
        when(notificationService.markAsRead(eq("user-a-uid"), eq(notifBId)))
                .thenThrow(new AccessDeniedException("You do not have permission to access this notification."));

        mockMvc.perform(patch("/api/v1/notifications/" + notifBId + "/read")
                        .header("Authorization", "Bearer token-user-a"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-04: Nonexistent notification returns 404 Not Found")
    void test4_nonexistentNotification_returns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(notificationService.markAsRead(eq("user-a-uid"), eq(randomId)))
                .thenThrow(new ResourceNotFoundException("Notification not found with id: " + randomId));

        mockMvc.perform(patch("/api/v1/notifications/" + randomId + "/read")
                        .header("Authorization", "Bearer token-user-a"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SEC-05: Unread count returns accurate count for authenticated user")
    void test5_unreadCount_returnsAccurateCount() throws Exception {
        when(notificationService.getUnreadCount("user-a-uid")).thenReturn(new UnreadCountResponse(5));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer token-user-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    @DisplayName("SEC-06: Mark all notifications as read returns count of marked items")
    void test6_markAllAsRead_returnsCount() throws Exception {
        when(notificationService.markAllAsRead("user-a-uid")).thenReturn(new MarkAllReadResponse(3));

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer token-user-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(3));
    }

    @Test
    @DisplayName("SEC-07: Register device token validates non-blank token input; blank token returns 400")
    void test7_blankDeviceToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/device-token")
                        .header("Authorization", "Bearer token-user-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SEC-08: Register valid device token succeeds with 200 OK")
    void test8_validDeviceToken_returns200() throws Exception {
        doNothing().when(notificationService).registerDeviceToken(eq("user-a-uid"), any());

        mockMvc.perform(post("/api/v1/notifications/device-token")
                        .header("Authorization", "Bearer token-user-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fcm-device-token-12345\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isOk());

        verify(notificationService).registerDeviceToken(eq("user-a-uid"), any());
    }

    @Test
    @DisplayName("SEC-09: Unregister device token calls service for authenticated user")
    void test9_unregisterDeviceToken_returns204() throws Exception {
        doNothing().when(notificationService).unregisterDeviceToken(eq("user-a-uid"), eq("fcm-device-token-12345"));

        mockMvc.perform(delete("/api/v1/notifications/device-token")
                        .header("Authorization", "Bearer token-user-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fcm-device-token-12345\"}"))
                .andExpect(status().isNoContent());

        verify(notificationService).unregisterDeviceToken(eq("user-a-uid"), eq("fcm-device-token-12345"));
    }

    @Test
    @DisplayName("SEC-10: Notifications response does not expose raw OTPs, tokens, or credentials")
    void test10_noSensitiveDataInNotificationResponse() throws Exception {
        UUID notifId = UUID.randomUUID();
        NotificationResponse resp = new NotificationResponse(
                notifId,
                NotificationType.OTP_VERIFIED,
                "Donation Handed Over",
                "Volunteer John Doe verified the pickup code and collected 'Catering Meals'.",
                "PICKUP",
                UUID.randomUUID(),
                true,
                Instant.now(),
                Instant.now()
        );

        when(notificationService.getMyNotifications(eq("user-a-uid"), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer token-user-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("otp"))))
                .andExpect(jsonPath("$.content[0].message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("password"))))
                .andExpect(jsonPath("$.content[0].message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("firebase"))))
                .andExpect(jsonPath("$.content[0].title").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456"))));
    }
}

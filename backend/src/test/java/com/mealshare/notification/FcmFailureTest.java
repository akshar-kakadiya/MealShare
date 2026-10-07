package com.mealshare.notification;

import com.mealshare.notification.fcm.FcmService;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmFailureTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmService fcmService;

    private NotificationService notificationService;
    private User donorUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userDeviceTokenRepository,
                userRepository,
                fcmService
        );

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(donorUser.getId())).thenReturn(Optional.of(donorUser));
    }

    @Test
    @DisplayName("FCM-FAIL: When FCM throws an exception, notification is still persisted and method returns successfully")
    void testFcmFailure_doesNotPreventNotificationPersistence() {
        UUID entityId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(donorUser.getId(), NotificationType.REQUEST_CREATED, entityId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        // Simulate FCM push failure (e.g., network error or FirebaseException)
        doThrow(new RuntimeException("Simulated FCM connection timeout / invalid token error"))
                .when(fcmService).sendPushNotification(any(), any(), any(), any(), any(), any());

        // Call createNotification: MUST NOT throw exception
        Notification result = assertDoesNotThrow(() -> notificationService.createNotification(
                donorUser.getId(),
                NotificationType.REQUEST_CREATED,
                "New Request",
                "Hope Shelter requested food",
                "DONATION_REQUEST",
                entityId
        ));

        // 1. Notification was successfully persisted
        assertNotNull(result, "In-app notification must be persisted even if FCM fails");
        verify(notificationRepository).save(any(Notification.class));

        // 2. FCM was attempted
        verify(fcmService).sendPushNotification(eq(donorUser.getId()), eq(NotificationType.REQUEST_CREATED),
                eq("New Request"), eq("Hope Shelter requested food"), eq("DONATION_REQUEST"), eq(entityId));

        // 3. Notification properties remain valid
        assertEquals(donorUser.getId(), result.getRecipientUser().getId());
        assertEquals(NotificationType.REQUEST_CREATED, result.getType());
        assertFalse(result.isRead());
    }
}

package com.mealshare.notification;

import com.mealshare.notification.event.DonationRequestApprovedEvent;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateNotificationEventTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmService fcmService;

    private NotificationService notificationService;
    private NotificationEventListener listener;

    private User receiverUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userDeviceTokenRepository,
                userRepository,
                fcmService
        );
        listener = new NotificationEventListener(notificationService);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("recv-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("DUP-01: Processing the exact same domain event twice suppresses the duplicate notification")
    void testDuplicateEvent_isSuppressedIdempotently() {
        UUID requestId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        DonationRequestApprovedEvent event = new DonationRequestApprovedEvent(
                requestId,
                donationId,
                receiverUser.getId(),
                "Fresh Catering Meals",
                25.0,
                "MEALS"
        );

        // First event execution: notification does not exist yet -> save succeeds
        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(receiverUser.getId(), NotificationType.REQUEST_APPROVED, requestId))
                .thenReturn(false)
                .thenReturn(true); // On second check, exists returns true

        when(userRepository.findById(receiverUser.getId())).thenReturn(Optional.of(receiverUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        // 1. Process event first time
        listener.handleRequestApproved(event);

        // 2. Process event second time (replay/duplicate event delivery)
        listener.handleRequestApproved(event);

        // Verify save was called EXACTLY ONCE
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(fcmService, times(1)).sendPushNotification(any(), any(), any(), any(), any(), any());
    }
}

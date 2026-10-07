package com.mealshare.notification.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mealshare.notification.NotificationType;
import com.mealshare.notification.UserDeviceToken;
import com.mealshare.notification.UserDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final UserDeviceTokenRepository userDeviceTokenRepository;

    public FcmService(UserDeviceTokenRepository userDeviceTokenRepository) {
        this.userDeviceTokenRepository = userDeviceTokenRepository;
    }

    /**
     * Attempts to send push notifications to all registered device tokens for the user.
     * FCM failures are safely trapped and will NEVER cause this method to throw an exception
     * or roll back the caller's business transaction.
     */
    @Transactional
    public void sendPushNotification(UUID userId, NotificationType type, String title, String body,
                                     String entityType, UUID entityId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseApp not initialized. Skipping FCM push notification.");
            return;
        }

        List<UserDeviceToken> tokens;
        try {
            tokens = userDeviceTokenRepository.findByUserId(userId);
        } catch (Exception e) {
            log.warn("Could not query device tokens for user {}: {}", userId, e.getMessage());
            return;
        }

        if (tokens.isEmpty()) {
            log.debug("No registered device tokens for user {}", userId);
            return;
        }

        FirebaseMessaging messaging;
        try {
            messaging = FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.warn("Could not retrieve FirebaseMessaging instance: {}", e.getMessage());
            return;
        }

        for (UserDeviceToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type.name())
                        .putData("entityType", entityType != null ? entityType : "")
                        .putData("entityId", entityId != null ? entityId.toString() : "")
                        .build();

                messaging.send(message);
                token.setLastUsedAt(Instant.now());
                userDeviceTokenRepository.save(token);
                log.debug("Push notification sent successfully to user {}", userId);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM delivery failed for user {}: messagingErrorCode={}", userId, e.getMessagingErrorCode());
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    try {
                        userDeviceTokenRepository.delete(token);
                        log.info("Purged invalid/unregistered FCM token for user {}", userId);
                    } catch (Exception ex) {
                        log.warn("Failed to purge stale token: {}", ex.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Unexpected error sending FCM notification for user {}: {}", userId, e.getMessage());
            }
        }
    }
}

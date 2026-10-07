package com.mealshare.notification;

import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.notification.dto.DeviceTokenRequest;
import com.mealshare.notification.dto.MarkAllReadResponse;
import com.mealshare.notification.dto.NotificationResponse;
import com.mealshare.notification.dto.UnreadCountResponse;
import com.mealshare.notification.fcm.FcmService;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserDeviceTokenRepository userDeviceTokenRepository,
                               UserRepository userRepository,
                               FcmService fcmService) {
        this.notificationRepository = notificationRepository;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
        this.userRepository = userRepository;
        this.fcmService = fcmService;
    }

    /**
     * Persists an in-app notification with idempotency protection, then triggers FCM delivery.
     * FCM failures will not prevent notification persistence or roll back transactions.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(UUID recipientUserId, NotificationType type, String title,
                                           String message, String entityType, UUID entityId) {
        if (entityId != null && notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(recipientUserId, type, entityId)) {
            log.info("Duplicate notification suppressed for recipient={}, type={}, entityId={}",
                    recipientUserId, type, entityId);
            return null;
        }

        User recipient = userRepository.findById(recipientUserId)
                .orElse(null);

        if (recipient == null) {
            log.warn("Recipient user {} not found in database. Notification dropped.", recipientUserId);
            return null;
        }

        Notification notification = new Notification(recipient, type, title, message, entityType, entityId);
        notification = notificationRepository.save(notification);
        log.info("Created in-app notification id={} for user={}, type={}",
                notification.getId(), recipientUserId, type);

        // Attempt push notification delivery safely
        try {
            fcmService.sendPushNotification(recipientUserId, type, title, message, entityType, entityId);
        } catch (Exception e) {
            log.warn("Error triggering FCM delivery for user {}: {}", recipientUserId, e.getMessage());
        }

        return notification;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String firebaseUid, Pageable pageable) {
        User user = getUserByFirebaseUid(firebaseUid);
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String firebaseUid) {
        User user = getUserByFirebaseUid(firebaseUid);
        long count = notificationRepository.countByRecipientUserIdAndIsReadFalse(user.getId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(String firebaseUid, UUID notificationId) {
        User user = getUserByFirebaseUid(firebaseUid);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to access this notification.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllAsRead(String firebaseUid) {
        User user = getUserByFirebaseUid(firebaseUid);
        int marked = notificationRepository.markAllAsRead(user.getId(), Instant.now());
        return new MarkAllReadResponse(marked);
    }

    @Transactional
    public void registerDeviceToken(String firebaseUid, DeviceTokenRequest request) {
        User user = getUserByFirebaseUid(firebaseUid);
        Optional<UserDeviceToken> existing = userDeviceTokenRepository.findByUserIdAndFcmToken(user.getId(), request.token());

        if (existing.isPresent()) {
            UserDeviceToken token = existing.get();
            token.setPlatform(request.platform());
            token.setLastUsedAt(Instant.now());
            userDeviceTokenRepository.save(token);
        } else {
            UserDeviceToken token = new UserDeviceToken(user, request.token(), request.platform());
            userDeviceTokenRepository.save(token);
        }
        log.debug("Registered FCM device token for user {}", user.getId());
    }

    @Transactional
    public void unregisterDeviceToken(String firebaseUid, String fcmToken) {
        User user = getUserByFirebaseUid(firebaseUid);
        userDeviceTokenRepository.deleteByUserIdAndFcmToken(user.getId(), fcmToken);
        log.debug("Unregistered FCM device token for user {}", user.getId());
    }

    private User getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

package com.mealshare.notification;

import com.mealshare.notification.event.*;
import com.mealshare.notification.fcm.FcmService;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventIntegrationTest {

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

    private User donorUser;
    private User receiverUser;
    private User secondReceiverUser;
    private User unrelatedUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userDeviceTokenRepository,
                userRepository,
                fcmService
        );
        listener = new NotificationEventListener(notificationService);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        secondReceiverUser = new User();
        secondReceiverUser.setId(UUID.randomUUID());
        secondReceiverUser.setFirebaseUid("second-receiver-uid");
        secondReceiverUser.setRole(UserRole.RECEIVER);
        secondReceiverUser.setStatus(UserStatus.ACTIVE);

        unrelatedUser = new User();
        unrelatedUser.setId(UUID.randomUUID());
        unrelatedUser.setFirebaseUid("unrelated-uid");
        unrelatedUser.setRole(UserRole.DONOR);
        unrelatedUser.setStatus(UserStatus.ACTIVE);

        lenient().when(userRepository.findById(donorUser.getId())).thenReturn(Optional.of(donorUser));
        lenient().when(userRepository.findById(receiverUser.getId())).thenReturn(Optional.of(receiverUser));
        lenient().when(userRepository.findById(secondReceiverUser.getId())).thenReturn(Optional.of(secondReceiverUser));
    }

    @Test
    @DisplayName("EVT-01: REQUEST_CREATED event creates in-app notification for the donor only")
    void test1_requestCreated_notifiesDonorOnly() {
        UUID requestId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(donorUser.getId(), NotificationType.REQUEST_CREATED, requestId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        DonationRequestCreatedEvent event = new DonationRequestCreatedEvent(
                requestId,
                donationId,
                donorUser.getId(),
                "Fresh Catering Meals",
                "Hope Shelter Foundation",
                25.0,
                "MEALS"
        );

        listener.handleRequestCreated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(donorUser.getId(), saved.getRecipientUser().getId());
        assertEquals(NotificationType.REQUEST_CREATED, saved.getType());
        assertEquals("New Donation Request", saved.getTitle());
        assertTrue(saved.getMessage().contains("Hope Shelter Foundation"));
        assertTrue(saved.getMessage().contains("Fresh Catering Meals"));
        assertEquals("DONATION_REQUEST", saved.getEntityType());
        assertEquals(requestId, saved.getEntityId());
        assertFalse(saved.isRead());
        assertNotNull(saved.getCreatedAt());

        // Verify unrelated user receives nothing
        verify(notificationRepository, never()).findByRecipientUserIdOrderByCreatedAtDesc(eq(unrelatedUser.getId()), any());
    }

    @Test
    @DisplayName("EVT-02: REQUEST_APPROVED event creates in-app notification for the receiver organization only")
    void test2_requestApproved_notifiesReceiverOnly() {
        UUID requestId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(receiverUser.getId(), NotificationType.REQUEST_APPROVED, requestId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        DonationRequestApprovedEvent event = new DonationRequestApprovedEvent(
                requestId,
                donationId,
                receiverUser.getId(),
                "Fresh Catering Meals",
                25.0,
                "MEALS"
        );

        listener.handleRequestApproved(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(receiverUser.getId(), saved.getRecipientUser().getId());
        assertEquals(NotificationType.REQUEST_APPROVED, saved.getType());
        assertEquals("Donation Request Approved", saved.getTitle());
        assertTrue(saved.getMessage().contains("approved"));
        assertEquals("DONATION_REQUEST", saved.getEntityType());
        assertEquals(requestId, saved.getEntityId());
        assertFalse(saved.isRead());
    }

    @Test
    @DisplayName("EVT-03: REQUEST_REJECTED event creates in-app notification for the receiver organization only")
    void test3_requestRejected_notifiesReceiverOnly() {
        UUID requestId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(receiverUser.getId(), NotificationType.REQUEST_REJECTED, requestId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        DonationRequestRejectedEvent event = new DonationRequestRejectedEvent(
                requestId,
                donationId,
                receiverUser.getId(),
                "Fresh Catering Meals",
                "Quantity already reserved"
        );

        listener.handleRequestRejected(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(receiverUser.getId(), saved.getRecipientUser().getId());
        assertEquals(NotificationType.REQUEST_REJECTED, saved.getType());
        assertEquals("Donation Request Declined", saved.getTitle());
        assertTrue(saved.getMessage().contains("Quantity already reserved"));
    }

    @Test
    @DisplayName("EVT-04: PICKUP_SCHEDULED event creates in-app notification for the receiver")
    void test4_pickupScheduled_notifiesReceiver() {
        UUID pickupId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(receiverUser.getId(), NotificationType.PICKUP_SCHEDULED, pickupId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        PickupScheduledEvent event = new PickupScheduledEvent(
                pickupId,
                receiverUser.getId(),
                "Fresh Catering Meals",
                "Indiranagar, Bengaluru",
                Instant.now().plusSeconds(3600)
        );

        listener.handlePickupScheduled(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(receiverUser.getId(), saved.getRecipientUser().getId());
        assertEquals(NotificationType.PICKUP_SCHEDULED, saved.getType());
        assertEquals("Pickup Scheduled", saved.getTitle());
        assertTrue(saved.getMessage().contains("Indiranagar, Bengaluru"));
        assertEquals("PICKUP", saved.getEntityType());
        assertEquals(pickupId, saved.getEntityId());
    }

    @Test
    @DisplayName("EVT-05: OTP_VERIFIED event creates in-app notification for donor without exposing OTP")
    void test5_otpVerified_notifiesDonor_noOtpExposed() {
        UUID pickupId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(donorUser.getId(), NotificationType.OTP_VERIFIED, pickupId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        OtpVerifiedEvent event = new OtpVerifiedEvent(
                pickupId,
                donorUser.getId(),
                "Fresh Catering Meals",
                "Receiver John"
        );

        listener.handleOtpVerified(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(donorUser.getId(), saved.getRecipientUser().getId());
        assertEquals(NotificationType.OTP_VERIFIED, saved.getType());
        assertEquals("Donation Handed Over", saved.getTitle());
        assertTrue(saved.getMessage().contains("Receiver John"));
        assertFalse(saved.getMessage().matches(".*\\b\\d{6}\\b.*"), "Notification must never contain raw OTP code!");
    }

    @Test
    @DisplayName("EVT-06: PICKUP_COMPLETED event creates in-app notification for donor and receiver")
    void test6_pickupCompleted_notifiesDonorAndReceiver() {
        UUID pickupId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(any(), eq(NotificationType.PICKUP_COMPLETED), eq(pickupId)))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        PickupCompletedEvent event = new PickupCompletedEvent(
                pickupId,
                donorUser.getId(),
                receiverUser.getId(),
                "Fresh Catering Meals",
                "Receiver John"
        );

        listener.handlePickupCompleted(event);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("EVT-07: DONATION_COMPLETED event creates in-app notification for donor")
    void test7_donationCompleted_notifiesDonor() {
        UUID donationId = UUID.randomUUID();

        when(notificationRepository.existsByRecipientUserIdAndTypeAndEntityId(donorUser.getId(), NotificationType.DONATION_COMPLETED, donationId))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        DonationCompletedEvent event = new DonationCompletedEvent(
                donationId,
                donorUser.getId(),
                "Fresh Catering Meals"
        );

        listener.handleDonationCompleted(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(donorUser.getId(), saved.getRecipientUser().getId());
        assertEquals(NotificationType.DONATION_COMPLETED, saved.getType());
        assertEquals("Donation Completed", saved.getTitle());
        assertEquals("DONATION", saved.getEntityType());
        assertEquals(donationId, saved.getEntityId());
    }
}

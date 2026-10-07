package com.mealshare.notification;

import com.mealshare.notification.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleRequestCreated(DonationRequestCreatedEvent event) {
        log.info("Processing DonationRequestCreatedEvent: requestId={}, donorId={}", event.requestId(), event.donorUserId());
        String title = "New Donation Request";
        String message = event.receiverOrgName() + " requested " + event.quantity() + " " + event.unit()
                + " of '" + event.donationTitle() + "'.";
        notificationService.createNotification(
                event.donorUserId(),
                NotificationType.REQUEST_CREATED,
                title,
                message,
                "DONATION_REQUEST",
                event.requestId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleRequestApproved(DonationRequestApprovedEvent event) {
        log.info("Processing DonationRequestApprovedEvent: requestId={}, receiverId={}", event.requestId(), event.receiverUserId());
        String title = "Donation Request Approved";
        String message = "Your request for " + event.allocatedQuantity() + " " + event.unit()
                + " of '" + event.donationTitle() + "' has been approved!";
        notificationService.createNotification(
                event.receiverUserId(),
                NotificationType.REQUEST_APPROVED,
                title,
                message,
                "DONATION_REQUEST",
                event.requestId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleRequestRejected(DonationRequestRejectedEvent event) {
        log.info("Processing DonationRequestRejectedEvent: requestId={}, receiverId={}", event.requestId(), event.receiverUserId());
        String title = "Donation Request Declined";
        String reasonStr = (event.rejectionReason() != null && !event.rejectionReason().isBlank())
                ? ": " + event.rejectionReason() : ".";
        String message = "Your request for '" + event.donationTitle() + "' was not accepted" + reasonStr;
        notificationService.createNotification(
                event.receiverUserId(),
                NotificationType.REQUEST_REJECTED,
                title,
                message,
                "DONATION_REQUEST",
                event.requestId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleRequestWithdrawn(DonationRequestWithdrawnEvent event) {
        log.info("Processing DonationRequestWithdrawnEvent: requestId={}, donorId={}", event.requestId(), event.donorUserId());
        String title = "Donation Request Withdrawn";
        String message = event.receiverOrgName() + " withdrew their request for '" + event.donationTitle() + "'.";
        notificationService.createNotification(
                event.donorUserId(),
                NotificationType.REQUEST_WITHDRAWN,
                title,
                message,
                "DONATION_REQUEST",
                event.requestId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePickupScheduled(PickupScheduledEvent event) {
        log.info("Processing PickupScheduledEvent: pickupId={}, receiverId={}", event.pickupId(), event.receiverUserId());
        String title = "Pickup Scheduled";
        String message = "A pickup has been scheduled for '" + event.donationTitle() + "' at " + event.pickupAddress() + ".";
        notificationService.createNotification(
                event.receiverUserId(),
                NotificationType.PICKUP_SCHEDULED,
                title,
                message,
                "PICKUP",
                event.pickupId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOtpVerified(OtpVerifiedEvent event) {
        log.info("Processing OtpVerifiedEvent: pickupId={}, donorId={}", event.pickupId(), event.donorUserId());
        String title = "Donation Handed Over";
        String message = "Receiver " + event.receiverName() + " verified the pickup code and collected '"
                + event.donationTitle() + "'.";
        notificationService.createNotification(
                event.donorUserId(),
                NotificationType.OTP_VERIFIED,
                title,
                message,
                "PICKUP",
                event.pickupId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePickupCompleted(PickupCompletedEvent event) {
        log.info("Processing PickupCompletedEvent: pickupId={}, donorId={}", event.pickupId(), event.donorUserId());
        String title = "Pickup Delivery Completed";
        String message = "Receiver " + event.receiverName() + " successfully completed pickup of '"
                + event.donationTitle() + "'.";
        notificationService.createNotification(
                event.donorUserId(),
                NotificationType.PICKUP_COMPLETED,
                title,
                message,
                "PICKUP",
                event.pickupId()
        );

        if (event.receiverUserId() != null) {
            notificationService.createNotification(
                    event.receiverUserId(),
                    NotificationType.PICKUP_COMPLETED,
                    "Food Pickup Completed",
                    "Your food pickup for '" + event.donationTitle() + "' has been successfully completed!",
                    "PICKUP",
                    event.pickupId()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDonationCompleted(DonationCompletedEvent event) {
        log.info("Processing DonationCompletedEvent: donationId={}, donorId={}", event.donationId(), event.donorUserId());
        String title = "Donation Completed";
        String message = "All allocated portions of '" + event.donationTitle() + "' have been delivered to recipients.";
        notificationService.createNotification(
                event.donorUserId(),
                NotificationType.DONATION_COMPLETED,
                title,
                message,
                "DONATION",
                event.donationId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDonationCancelled(DonationCancelledEvent event) {
        log.info("Processing DonationCancelledEvent: donationId={}", event.donationId());
        String title = "Donation Cancelled";
        String message = "The donation listing for '" + event.donationTitle() + "' was cancelled by the donor.";
        for (java.util.UUID receiverUserId : event.receiverUserIds()) {
            notificationService.createNotification(
                    receiverUserId,
                    NotificationType.DONATION_CANCELLED,
                    title,
                    message,
                    "DONATION",
                    event.donationId()
            );
        }
    }
}

package com.mealshare.notification.event;

import java.util.UUID;

public record DonationRequestRejectedEvent(
        UUID requestId,
        UUID donationId,
        UUID receiverUserId,
        String donationTitle,
        String rejectionReason
) {}

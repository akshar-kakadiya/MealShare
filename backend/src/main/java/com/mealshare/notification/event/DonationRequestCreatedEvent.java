package com.mealshare.notification.event;

import java.util.UUID;

public record DonationRequestCreatedEvent(
        UUID requestId,
        UUID donationId,
        UUID donorUserId,
        String donationTitle,
        String receiverOrgName,
        Double quantity,
        String unit
) {}

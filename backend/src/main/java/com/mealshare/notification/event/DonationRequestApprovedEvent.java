package com.mealshare.notification.event;

import java.util.UUID;

public record DonationRequestApprovedEvent(
        UUID requestId,
        UUID donationId,
        UUID receiverUserId,
        String donationTitle,
        Double allocatedQuantity,
        String unit
) {}

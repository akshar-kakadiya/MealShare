package com.mealshare.notification.event;

import java.util.List;
import java.util.UUID;

public record DonationCancelledEvent(
        UUID donationId,
        List<UUID> receiverUserIds,
        String donationTitle
) {}

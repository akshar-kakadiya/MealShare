package com.mealshare.notification.event;

import java.time.Instant;
import java.util.UUID;

public record PickupScheduledEvent(
        UUID pickupId,
        UUID receiverUserId,
        String donationTitle,
        String pickupAddress,
        Instant scheduledAt
) {}

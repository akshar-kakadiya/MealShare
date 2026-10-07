package com.mealshare.notification.event;

import java.util.UUID;

public record PickupCompletedEvent(
        UUID pickupId,
        UUID donorUserId,
        UUID receiverUserId,
        String donationTitle,
        String receiverName
) {}

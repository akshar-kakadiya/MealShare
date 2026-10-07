package com.mealshare.notification.event;

import java.util.UUID;

public record OtpVerifiedEvent(
        UUID pickupId,
        UUID donorUserId,
        String donationTitle,
        String receiverName
) {}

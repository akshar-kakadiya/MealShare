package com.mealshare.notification.event;

import java.util.UUID;

public record DonationCompletedEvent(
        UUID donationId,
        UUID donorUserId,
        String donationTitle
) {}

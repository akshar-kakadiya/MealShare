package com.mealshare.pickup.dto;

import java.time.Instant;
import java.util.UUID;

public record DonorPickupOtpResponse(
        UUID pickupId,
        UUID donationId,
        String donationTitle,
        String receiverOrganizationName,
        String otp,
        Instant expiresAt
) {}

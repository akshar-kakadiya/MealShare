package com.mealshare.pickup.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AssignPickupRequest(
        @NotNull(message = "donationRequestId is required")
        UUID donationRequestId,

        @NotNull(message = "scheduledAt is required")
        Instant scheduledAt
) {}

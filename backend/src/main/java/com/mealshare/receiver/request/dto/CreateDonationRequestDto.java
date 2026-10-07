package com.mealshare.receiver.request.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDonationRequestDto(
        @NotNull(message = "Requested quantity is required")
        @Positive(message = "Requested quantity must be strictly greater than 0")
        Double requestedQuantity,

        String requestReason
) {}

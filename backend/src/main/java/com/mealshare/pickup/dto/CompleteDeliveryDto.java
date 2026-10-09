package com.mealshare.pickup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteDeliveryDto(
        Double latitude,
        Double longitude,
        @NotNull(message = "Delivered quantity is required")
        @Positive(message = "Delivered quantity must be greater than 0")
        Double deliveredQuantity,
        String quantityDiscrepancyReason,
        String photoUrl
) {}

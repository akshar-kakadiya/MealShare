package com.mealshare.pickup.dto;

import jakarta.validation.constraints.NotNull;

public record FoodInspectionDto(
        @NotNull(message = "Field 'accepted' is required") Boolean accepted,
        String notes,
        String rejectionReason,
        String photoUrl
) {}

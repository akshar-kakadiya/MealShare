package com.mealshare.pickup.dto;

import jakarta.validation.constraints.NotNull;

public record LocationUpdateDto(
        @NotNull(message = "Latitude is required") Double latitude,
        @NotNull(message = "Longitude is required") Double longitude,
        Double speed,
        Double heading
) {}

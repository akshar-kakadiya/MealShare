package com.mealshare.donation.dto;

import com.mealshare.donation.FoodCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CreateDonationRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Food category is required")
        FoodCategory foodCategory,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        Double quantity,

        @NotBlank(message = "Quantity unit is required")
        String quantityUnit,

        @NotNull(message = "Preparation date/time is required")
        Instant preparationDateTime,

        @NotNull(message = "Consume-before date/time is required")
        Instant consumeBefore,

        @NotNull(message = "Pickup available from date/time is required")
        Instant pickupAvailableFrom,

        @NotNull(message = "Pickup available until date/time is required")
        Instant pickupAvailableUntil,

        @NotBlank(message = "Pickup address is required")
        String pickupAddress,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude,

        @NotNull(message = "Food safety declaration is required")
        Boolean foodSafetyDeclared,

        String foodSafetyDeclarationText
) {}

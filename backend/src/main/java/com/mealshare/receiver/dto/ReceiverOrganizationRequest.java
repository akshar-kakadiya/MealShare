package com.mealshare.receiver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReceiverOrganizationRequest(
        @NotBlank(message = "Organization type is required")
        String orgType,

        @NotBlank(message = "Organization name is required")
        String orgName,

        @NotBlank(message = "Registration number is required")
        String registrationNumber,

        @NotBlank(message = "Address text is required")
        String addressText,

        @NotNull(message = "Service latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double serviceLatitude,

        @NotNull(message = "Service longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double serviceLongitude,

        @Positive(message = "Service radius must be positive")
        Double serviceRadiusMeters,

        @Positive(message = "Beneficiary capacity must be positive")
        Integer beneficiaryCapacity
) {}

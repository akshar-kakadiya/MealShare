package com.mealshare.pickup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyAndPickupDto(
        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 numeric digits")
        String otp,
        Double latitude,
        Double longitude,
        String photoUrl
) {}

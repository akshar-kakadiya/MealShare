package com.mealshare.pickup.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank(message = "OTP cannot be blank")
        String otp
) {}

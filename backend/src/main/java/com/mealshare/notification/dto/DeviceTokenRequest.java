package com.mealshare.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenRequest(
        @NotBlank(message = "Device token is required")
        String token,

        String platform
) {}

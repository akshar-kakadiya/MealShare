package com.mealshare.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVerificationStatusRequest(
        @NotBlank(message = "Status is required")
        String status,
        String notes
) {}

package com.mealshare.admin.dto;

import com.mealshare.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status,
        String reason
) {}

package com.mealshare.admin.dto;

import com.mealshare.user.User;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID id,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        boolean verified,
        Instant createdAt,
        Instant updatedAt,
        Map<String, Object> profileDetails
) {
    public static AdminUserDetailResponse fromEntity(User user, Map<String, Object> profileDetails) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getStatus() == UserStatus.ACTIVE,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                profileDetails
        );
    }
}

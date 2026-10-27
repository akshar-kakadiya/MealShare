package com.mealshare.admin.dto;

import com.mealshare.user.User;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        boolean verified,
        String organization,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserResponse fromEntity(User user, String organization) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getStatus() == UserStatus.ACTIVE && user.getVerificationStatus() == com.mealshare.user.VerificationStatus.APPROVED,
                organization,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

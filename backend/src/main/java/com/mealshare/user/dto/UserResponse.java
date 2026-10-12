package com.mealshare.user.dto;

import com.mealshare.user.User;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String phone,
        String name,
        UserRole role,
        UserStatus status,
        String category,
        String profileStatus,
        String verificationStatus,
        String rejectionReason,
        String actionRequiredNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getCategory(),
                user.getProfileStatus() != null ? user.getProfileStatus().name() : "INCOMPLETE",
                user.getVerificationStatus() != null ? user.getVerificationStatus().name() : "PENDING",
                user.getRejectionReason(),
                user.getActionRequiredNotes(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

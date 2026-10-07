package com.mealshare.donor.dto;

import com.mealshare.donor.DonorProfile;

import java.time.Instant;
import java.util.UUID;

public record DonorProfileResponse(
        UUID id,
        UUID userId,
        String donorType,
        String entityName,
        String addressText,
        Double latitude,
        Double longitude,
        Boolean policyAccepted,
        Double ratingAverage,
        Integer completedDonationsCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static DonorProfileResponse fromEntity(DonorProfile profile) {
        return new DonorProfileResponse(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getDonorType(),
                profile.getEntityName(),
                profile.getAddressText(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getPolicyAccepted(),
                profile.getRatingAverage(),
                profile.getCompletedDonationsCount(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

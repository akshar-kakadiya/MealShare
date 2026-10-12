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
        String contactPerson,
        String businessType,
        String eventInfo,
        String photos,
        String fullName,
        String email,
        String phone,
        String profileStatus,
        String verificationStatus,
        String rejectionReason,
        String actionRequiredNotes,
        Instant verifiedAt,
        String verifiedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static DonorProfileResponse fromEntity(DonorProfile profile) {
        var user = profile.getUser();
        return new DonorProfileResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                profile.getDonorType(),
                profile.getEntityName(),
                profile.getAddressText(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getPolicyAccepted(),
                profile.getRatingAverage(),
                profile.getCompletedDonationsCount(),
                profile.getContactPerson(),
                profile.getBusinessType(),
                profile.getEventInfo(),
                profile.getPhotos(),
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                profile.getProfileStatus() != null ? profile.getProfileStatus().name() : "INCOMPLETE",
                profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : "PENDING",
                profile.getRejectionReason(),
                profile.getActionRequiredNotes(),
                profile.getVerifiedAt(),
                profile.getVerifiedBy(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

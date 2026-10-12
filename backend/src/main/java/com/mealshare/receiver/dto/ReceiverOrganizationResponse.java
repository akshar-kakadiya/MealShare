package com.mealshare.receiver.dto;

import com.mealshare.receiver.ReceiverOrganization;

import java.time.Instant;
import java.util.UUID;

public record ReceiverOrganizationResponse(
        UUID id,
        UUID userId,
        String orgType,
        String orgName,
        String registrationNumber,
        String addressText,
        Double serviceLatitude,
        Double serviceLongitude,
        Double serviceRadiusMeters,
        String verificationStatus,
        Integer beneficiaryCapacity,
        Double ratingAverage,
        String contactPerson,
        String description,
        String photos,
        String profileStatus,
        String rejectionReason,
        String actionRequiredNotes,
        Instant verifiedAt,
        String verifiedBy,
        String fullName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
    public ReceiverOrganizationResponse(
            UUID id,
            UUID userId,
            String orgType,
            String orgName,
            String registrationNumber,
            String addressText,
            Double serviceLatitude,
            Double serviceLongitude,
            Double serviceRadiusMeters,
            String verificationStatus,
            Integer beneficiaryCapacity,
            Double ratingAverage,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, userId, orgType, orgName, registrationNumber, addressText, serviceLatitude, serviceLongitude, serviceRadiusMeters, verificationStatus, beneficiaryCapacity, ratingAverage, null, null, null, "INCOMPLETE", null, null, null, null, null, null, null, createdAt, updatedAt);
    }

    public static ReceiverOrganizationResponse fromEntity(ReceiverOrganization org) {
        var user = org.getUser();
        return new ReceiverOrganizationResponse(
                org.getId(),
                user != null ? user.getId() : null,
                org.getOrgType(),
                org.getOrgName(),
                org.getRegistrationNumber(),
                org.getAddressText(),
                org.getServiceLatitude(),
                org.getServiceLongitude(),
                org.getServiceRadiusMeters(),
                org.getVerificationStatus(),
                org.getBeneficiaryCapacity(),
                org.getRatingAverage(),
                org.getContactPerson(),
                org.getDescription(),
                org.getPhotos(),
                org.getProfileStatus() != null ? org.getProfileStatus().name() : "INCOMPLETE",
                org.getRejectionReason(),
                org.getActionRequiredNotes(),
                org.getVerifiedAt(),
                org.getVerifiedBy(),
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}

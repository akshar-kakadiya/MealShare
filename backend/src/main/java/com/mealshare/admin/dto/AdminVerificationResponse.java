package com.mealshare.admin.dto;

import com.mealshare.donor.DonorProfile;
import com.mealshare.receiver.ReceiverOrganization;

import java.time.Instant;
import java.util.UUID;

public record AdminVerificationResponse(
        UUID id,
        UUID userId,
        String role,
        String category,
        String organizationName,
        String organizationType,
        String contactPerson,
        String email,
        String phone,
        String address,
        Double latitude,
        Double longitude,
        String registrationNumber,
        Integer beneficiaryCapacity,
        String description,
        String businessType,
        String eventInfo,
        String photos,
        String profileStatus,
        String status,
        String rejectionReason,
        String actionRequiredNotes,
        Instant submittedAt,
        Instant verifiedAt,
        String verifiedBy,
        String notes
) {
    public AdminVerificationResponse(
            UUID id,
            UUID userId,
            String organizationName,
            String organizationType,
            String contactPerson,
            String email,
            String phone,
            String address,
            String registrationNumber,
            Integer beneficiaryCapacity,
            String status,
            Instant submittedAt,
            String notes
    ) {
        this(
                id,
                userId,
                "RECEIVER",
                organizationType,
                organizationName,
                organizationType,
                contactPerson,
                email,
                phone,
                address,
                null,
                null,
                registrationNumber,
                beneficiaryCapacity,
                null,
                null,
                null,
                null,
                "COMPLETE",
                status,
                null,
                null,
                submittedAt,
                null,
                null,
                notes
        );
    }

    public static AdminVerificationResponse fromEntity(ReceiverOrganization org) {
        return fromEntity(org, null);
    }

    public static AdminVerificationResponse fromEntity(ReceiverOrganization org, com.mealshare.user.User userOverride) {
        var user = userOverride != null ? userOverride : org.getUser();
        String status = user != null && user.getVerificationStatus() != null
                ? user.getVerificationStatus().name()
                : ("VERIFIED".equalsIgnoreCase(org.getVerificationStatus()) ? "APPROVED" : (org.getVerificationStatus() != null ? org.getVerificationStatus() : "PENDING"));
        String profileStatus = org.getProfileStatus() != null
                ? org.getProfileStatus().name()
                : (user != null && user.getProfileStatus() != null ? user.getProfileStatus().name() : "INCOMPLETE");

        return new AdminVerificationResponse(
                org.getId(),
                user != null ? user.getId() : null,
                "RECEIVER",
                org.getOrgType(),
                org.getOrgName(),
                org.getOrgType(),
                org.getContactPerson() != null && !org.getContactPerson().isBlank() ? org.getContactPerson() : (user != null ? user.getName() : "Unknown"),
                user != null ? user.getEmail() : "",
                user != null ? user.getPhone() : "",
                org.getAddressText(),
                org.getServiceLatitude(),
                org.getServiceLongitude(),
                org.getRegistrationNumber(),
                org.getBeneficiaryCapacity(),
                org.getDescription(),
                null,
                null,
                org.getPhotos(),
                profileStatus,
                status,
                org.getRejectionReason() != null ? org.getRejectionReason() : (user != null ? user.getRejectionReason() : null),
                org.getActionRequiredNotes() != null ? org.getActionRequiredNotes() : (user != null ? user.getActionRequiredNotes() : null),
                org.getCreatedAt(),
                org.getVerifiedAt() != null ? org.getVerifiedAt() : (user != null ? user.getVerifiedAt() : null),
                org.getVerifiedBy() != null ? org.getVerifiedBy() : (user != null ? user.getVerifiedBy() : null),
                org.getActionRequiredNotes() != null ? org.getActionRequiredNotes() : org.getRejectionReason()
        );
    }

    public static AdminVerificationResponse fromEntity(DonorProfile donor) {
        return fromEntity(donor, null);
    }

    public static AdminVerificationResponse fromEntity(DonorProfile donor, com.mealshare.user.User userOverride) {
        var user = userOverride != null ? userOverride : donor.getUser();
        String status = user != null && user.getVerificationStatus() != null
                ? user.getVerificationStatus().name()
                : (donor.getVerificationStatus() != null ? donor.getVerificationStatus().name() : "PENDING");
        String profileStatus = donor.getProfileStatus() != null
                ? donor.getProfileStatus().name()
                : (user != null && user.getProfileStatus() != null ? user.getProfileStatus().name() : "INCOMPLETE");

        return new AdminVerificationResponse(
                donor.getId(),
                user != null ? user.getId() : null,
                "DONOR",
                donor.getDonorType(),
                donor.getEntityName(),
                donor.getDonorType(),
                donor.getContactPerson() != null && !donor.getContactPerson().isBlank() ? donor.getContactPerson() : (user != null ? user.getName() : "Unknown"),
                user != null ? user.getEmail() : "",
                user != null ? user.getPhone() : "",
                donor.getAddressText(),
                donor.getLatitude(),
                donor.getLongitude(),
                null,
                null,
                null,
                donor.getBusinessType(),
                donor.getEventInfo(),
                donor.getPhotos(),
                profileStatus,
                status,
                donor.getRejectionReason() != null ? donor.getRejectionReason() : (user != null ? user.getRejectionReason() : null),
                donor.getActionRequiredNotes() != null ? donor.getActionRequiredNotes() : (user != null ? user.getActionRequiredNotes() : null),
                donor.getCreatedAt(),
                donor.getVerifiedAt() != null ? donor.getVerifiedAt() : (user != null ? user.getVerifiedAt() : null),
                donor.getVerifiedBy() != null ? donor.getVerifiedBy() : (user != null ? user.getVerifiedBy() : null),
                donor.getActionRequiredNotes() != null ? donor.getActionRequiredNotes() : donor.getRejectionReason()
        );
    }
}

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
        var user = org.getUser();
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
                org.getProfileStatus() != null ? org.getProfileStatus().name() : "INCOMPLETE",
                org.getVerificationStatus(),
                org.getRejectionReason(),
                org.getActionRequiredNotes(),
                org.getCreatedAt(),
                org.getVerifiedAt(),
                org.getVerifiedBy(),
                org.getActionRequiredNotes() != null ? org.getActionRequiredNotes() : org.getRejectionReason()
        );
    }

    public static AdminVerificationResponse fromEntity(DonorProfile donor) {
        var user = donor.getUser();
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
                donor.getProfileStatus() != null ? donor.getProfileStatus().name() : "INCOMPLETE",
                donor.getVerificationStatus() != null ? donor.getVerificationStatus().name() : "PENDING",
                donor.getRejectionReason(),
                donor.getActionRequiredNotes(),
                donor.getCreatedAt(),
                donor.getVerifiedAt(),
                donor.getVerifiedBy(),
                donor.getActionRequiredNotes() != null ? donor.getActionRequiredNotes() : donor.getRejectionReason()
        );
    }
}

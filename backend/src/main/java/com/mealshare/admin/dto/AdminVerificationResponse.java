package com.mealshare.admin.dto;

import com.mealshare.receiver.ReceiverOrganization;

import java.time.Instant;
import java.util.UUID;

public record AdminVerificationResponse(
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
    public static AdminVerificationResponse fromEntity(ReceiverOrganization org) {
        var user = org.getUser();
        return new AdminVerificationResponse(
                org.getId(),
                user != null ? user.getId() : null,
                org.getOrgName(),
                org.getOrgType(),
                user != null ? user.getName() : "Unknown",
                user != null ? user.getEmail() : "",
                user != null ? user.getPhone() : "",
                org.getAddressText(),
                org.getRegistrationNumber(),
                org.getBeneficiaryCapacity(),
                org.getVerificationStatus(),
                org.getCreatedAt(),
                null
        );
    }
}

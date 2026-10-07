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
        Instant createdAt,
        Instant updatedAt
) {
    public static ReceiverOrganizationResponse fromEntity(ReceiverOrganization org) {
        return new ReceiverOrganizationResponse(
                org.getId(),
                org.getUser() != null ? org.getUser().getId() : null,
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
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}

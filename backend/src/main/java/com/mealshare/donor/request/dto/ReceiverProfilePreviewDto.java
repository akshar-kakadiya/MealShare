package com.mealshare.donor.request.dto;

import com.mealshare.receiver.ReceiverOrganization;

import java.time.Instant;
import java.util.UUID;

public record ReceiverProfilePreviewDto(
        UUID receiverId,
        String orgName,
        String orgType,
        String verificationStatus,
        String contactPerson,
        String phone,
        String email,
        String addressText,
        Double serviceLatitude,
        Double serviceLongitude,
        Double serviceRadiusMeters,
        Integer beneficiaryCapacity,
        Double ratingAverage,
        String description,
        String photos,
        Instant verifiedAt
) {
    public static ReceiverProfilePreviewDto fromEntity(ReceiverOrganization org) {
        String phone = (org.getUser() != null) ? org.getUser().getPhone() : null;
        String email = (org.getUser() != null) ? org.getUser().getEmail() : null;
        return new ReceiverProfilePreviewDto(
                org.getId(),
                org.getOrgName(),
                org.getOrgType(),
                org.getVerificationStatus(),
                org.getContactPerson(),
                phone,
                email,
                org.getAddressText(),
                org.getServiceLatitude(),
                org.getServiceLongitude(),
                org.getServiceRadiusMeters(),
                org.getBeneficiaryCapacity(),
                org.getRatingAverage(),
                org.getDescription(),
                org.getPhotos(),
                org.getVerifiedAt()
        );
    }
}

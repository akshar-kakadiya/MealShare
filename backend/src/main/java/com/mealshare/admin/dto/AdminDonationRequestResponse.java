package com.mealshare.admin.dto;

import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminDonationRequestResponse(
        UUID id,
        UUID donationId,
        String donationTitle,
        UUID donorId,
        String donorName,
        UUID receiverOrgId,
        String receiverOrgName,
        Double requestedQuantity,
        Double allocatedQuantity,
        String unit,
        String requestReason,
        DonationRequestStatus status,
        Instant requestedAt,
        Instant updatedAt
) {
    public AdminDonationRequestResponse(
            UUID id,
            UUID donationId,
            String donationTitle,
            UUID donorId,
            String donorName,
            UUID receiverOrgId,
            String receiverOrgName,
            Double requestedQuantity,
            String unit,
            String requestReason,
            DonationRequestStatus status,
            Instant requestedAt,
            Instant updatedAt
    ) {
        this(id, donationId, donationTitle, donorId, donorName, receiverOrgId, receiverOrgName,
                requestedQuantity,
                (status == DonationRequestStatus.APPROVED || status == DonationRequestStatus.COMPLETED) ? requestedQuantity : 0.0,
                unit, requestReason, status, requestedAt, updatedAt);
    }

    public static AdminDonationRequestResponse fromEntity(DonationRequest request) {
        var donation = request.getDonation();
        var donorProfile = donation != null ? donation.getDonor() : null;
        var donorUser = donorProfile != null ? donorProfile.getUser() : null;
        String donorName = donorUser != null ? donorUser.getName() : (donorProfile != null ? donorProfile.getEntityName() : "Unknown Donor");

        var receiverOrg = request.getReceiverOrganization();
        String receiverOrgName = receiverOrg != null ? receiverOrg.getOrgName() : "Unknown Receiver";

        return new AdminDonationRequestResponse(
                request.getId(),
                donation != null ? donation.getId() : null,
                donation != null ? donation.getTitle() : "Unknown Donation",
                donorProfile != null ? donorProfile.getId() : null,
                donorName,
                receiverOrg != null ? receiverOrg.getId() : null,
                receiverOrgName,
                request.getRequestedQuantity(),
                request.getAllocatedQuantity(),
                donation != null ? donation.getQuantityUnit() : "units",
                request.getRequestReason(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getUpdatedAt()
        );
    }
}

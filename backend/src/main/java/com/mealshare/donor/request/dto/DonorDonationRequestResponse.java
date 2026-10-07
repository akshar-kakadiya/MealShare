package com.mealshare.donor.request.dto;

import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record DonorDonationRequestResponse(
        UUID requestId,
        UUID donationId,
        String donationTitle,
        UUID receiverOrganizationId,
        String receiverOrganizationName,
        Double requestedQuantity,
        Double allocatedQuantity,
        String quantityUnit,
        String requestReason,
        DonationRequestStatus status,
        Instant requestedAt,
        Instant updatedAt
) {
    public DonorDonationRequestResponse(
            UUID requestId,
            UUID donationId,
            String donationTitle,
            UUID receiverOrganizationId,
            String receiverOrganizationName,
            Double requestedQuantity,
            String quantityUnit,
            String requestReason,
            DonationRequestStatus status,
            Instant requestedAt,
            Instant updatedAt
    ) {
        this(requestId, donationId, donationTitle, receiverOrganizationId, receiverOrganizationName,
                requestedQuantity,
                (status == DonationRequestStatus.APPROVED || status == DonationRequestStatus.COMPLETED) ? requestedQuantity : 0.0,
                quantityUnit, requestReason, status, requestedAt, updatedAt);
    }

    public static DonorDonationRequestResponse fromEntity(DonationRequest request) {
        return new DonorDonationRequestResponse(
                request.getId(),
                request.getDonation() != null ? request.getDonation().getId() : null,
                request.getDonation() != null ? request.getDonation().getTitle() : null,
                request.getReceiverOrganization() != null ? request.getReceiverOrganization().getId() : null,
                request.getReceiverOrganization() != null ? request.getReceiverOrganization().getOrgName() : null,
                request.getRequestedQuantity(),
                request.getAllocatedQuantity(),
                request.getDonation() != null ? request.getDonation().getQuantityUnit() : null,
                request.getRequestReason(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getUpdatedAt()
        );
    }
}

package com.mealshare.receiver.request.dto;

import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record DonationRequestResponse(
        UUID id,
        UUID donationId,
        String donationTitle,
        UUID receiverOrgId,
        String receiverOrgName,
        Double requestedQuantity,
        Double allocatedQuantity,
        String requestReason,
        DonationRequestStatus status,
        Instant requestedAt,
        Instant updatedAt
) {
    public DonationRequestResponse(
            UUID id,
            UUID donationId,
            String donationTitle,
            UUID receiverOrgId,
            String receiverOrgName,
            Double requestedQuantity,
            String requestReason,
            DonationRequestStatus status,
            Instant requestedAt,
            Instant updatedAt
    ) {
        this(id, donationId, donationTitle, receiverOrgId, receiverOrgName, requestedQuantity,
                (status == DonationRequestStatus.APPROVED || status == DonationRequestStatus.COMPLETED) ? requestedQuantity : 0.0,
                requestReason, status, requestedAt, updatedAt);
    }

    public static DonationRequestResponse fromEntity(DonationRequest req) {
        return new DonationRequestResponse(
                req.getId(),
                req.getDonation() != null ? req.getDonation().getId() : null,
                req.getDonation() != null ? req.getDonation().getTitle() : null,
                req.getReceiverOrganization() != null ? req.getReceiverOrganization().getId() : null,
                req.getReceiverOrganization() != null ? req.getReceiverOrganization().getOrgName() : null,
                req.getRequestedQuantity(),
                req.getAllocatedQuantity(),
                req.getRequestReason(),
                req.getStatus(),
                req.getRequestedAt(),
                req.getUpdatedAt()
        );
    }
}

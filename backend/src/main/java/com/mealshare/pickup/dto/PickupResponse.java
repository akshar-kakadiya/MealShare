package com.mealshare.pickup.dto;

import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupStatus;

import java.time.Instant;
import java.util.UUID;

public record PickupResponse(
        UUID pickupId,
        UUID donationRequestId,
        UUID donationId,
        String donationTitle,
        String pickupAddress,
        String donorName,
        String donorPhone,
        UUID receiverOrganizationId,
        String receiverOrganizationName,
        Double requestedQuantity,
        String quantityUnit,
        Instant scheduledAt,
        Instant assignedAt,
        PickupStatus status,
        Instant otpVerifiedAt,
        Instant pickedUpAt,
        Instant completedAt,
        Instant createdAt
) {
    public static PickupResponse fromEntity(Pickup pickup) {
        var req = pickup.getDonationRequest();
        var donation = req != null ? req.getDonation() : null;
        var donorUser = (donation != null && donation.getDonor() != null) ? donation.getDonor().getUser() : null;
        var receiverOrg = req != null ? req.getReceiverOrganization() : null;

        return new PickupResponse(
                pickup.getId(),
                req != null ? req.getId() : null,
                donation != null ? donation.getId() : null,
                donation != null ? donation.getTitle() : null,
                donation != null ? donation.getPickupAddress() : null,
                donorUser != null ? donorUser.getName() : null,
                donorUser != null ? donorUser.getPhone() : null,
                receiverOrg != null ? receiverOrg.getId() : null,
                receiverOrg != null ? receiverOrg.getOrgName() : null,
                req != null ? req.getRequestedQuantity() : null,
                donation != null ? donation.getQuantityUnit() : null,
                pickup.getScheduledAt(),
                pickup.getAssignedAt(),
                pickup.getStatus(),
                pickup.getOtpVerifiedAt(),
                pickup.getPickedUpAt(),
                pickup.getCompletedAt(),
                pickup.getCreatedAt()
        );
    }
}

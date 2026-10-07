package com.mealshare.pickup.dto;

import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminAssignPickupResponse(
        UUID pickupId,
        UUID donationRequestId,
        UUID donationId,
        UUID receiverId,
        String receiverName,
        PickupStatus status,
        Instant scheduledAt,
        Instant assignedAt,
        Instant otpExpiresAt
) {
    public static AdminAssignPickupResponse fromEntity(Pickup pickup) {
        return new AdminAssignPickupResponse(
                pickup.getId(),
                pickup.getDonationRequest().getId(),
                pickup.getDonationRequest().getDonation().getId(),
                pickup.getReceiver().getId(),
                pickup.getReceiver().getOrgName(),
                pickup.getStatus(),
                pickup.getScheduledAt(),
                pickup.getAssignedAt(),
                pickup.getOtpExpiresAt()
        );
    }
}

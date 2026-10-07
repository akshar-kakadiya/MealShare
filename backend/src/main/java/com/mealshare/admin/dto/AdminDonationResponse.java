package com.mealshare.admin.dto;

import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;

import java.time.Instant;
import java.util.UUID;

public record AdminDonationResponse(
        UUID id,
        UUID donorId,
        String donorName,
        String title,
        String description,
        FoodCategory foodCategory,
        Double quantity,
        Double allocatedQuantity,
        Double remainingQuantity,
        String quantityUnit,
        Instant preparationDateTime,
        Instant consumeBefore,
        Instant pickupAvailableFrom,
        Instant pickupAvailableUntil,
        String pickupAddress,
        Double latitude,
        Double longitude,
        Boolean foodSafetyDeclared,
        DonationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminDonationResponse fromEntity(Donation donation) {
        String donorName = donation.getDonor() != null && donation.getDonor().getUser() != null
                ? donation.getDonor().getUser().getName()
                : (donation.getDonor() != null ? donation.getDonor().getEntityName() : "Unknown Donor");

        double allocated = donation.getAllocatedQuantity() != null ? donation.getAllocatedQuantity() : 0.0;
        double total = donation.getQuantity() != null ? donation.getQuantity() : 0.0;
        double remaining = Math.max(0.0, total - allocated);

        return new AdminDonationResponse(
                donation.getId(),
                donation.getDonor() != null ? donation.getDonor().getId() : null,
                donorName,
                donation.getTitle(),
                donation.getDescription(),
                donation.getFoodCategory(),
                total,
                allocated,
                remaining,
                donation.getQuantityUnit(),
                donation.getPreparationDateTime(),
                donation.getConsumeBefore(),
                donation.getPickupAvailableFrom(),
                donation.getPickupAvailableUntil(),
                donation.getPickupAddress(),
                donation.getLatitude(),
                donation.getLongitude(),
                donation.getFoodSafetyDeclared(),
                donation.getStatus(),
                donation.getCreatedAt(),
                donation.getUpdatedAt()
        );
    }
}

package com.mealshare.donation.dto;

import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;

import java.time.Instant;
import java.util.UUID;

public record DonationResponse(
        UUID id,
        UUID donorId,
        String donorEntityName,
        String title,
        String description,
        FoodCategory foodCategory,
        Double quantity,
        String quantityUnit,
        Instant preparationDateTime,
        Instant consumeBefore,
        Instant pickupAvailableFrom,
        Instant pickupAvailableUntil,
        String pickupAddress,
        Double latitude,
        Double longitude,
        Boolean foodSafetyDeclared,
        String foodSafetyDeclarationText,
        Instant foodSafetyDeclaredAt,
        DonationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static DonationResponse fromEntity(Donation donation) {
        return new DonationResponse(
                donation.getId(),
                donation.getDonor() != null ? donation.getDonor().getId() : null,
                donation.getDonor() != null ? donation.getDonor().getEntityName() : null,
                donation.getTitle(),
                donation.getDescription(),
                donation.getFoodCategory(),
                donation.getQuantity(),
                donation.getQuantityUnit(),
                donation.getPreparationDateTime(),
                donation.getConsumeBefore(),
                donation.getPickupAvailableFrom(),
                donation.getPickupAvailableUntil(),
                donation.getPickupAddress(),
                donation.getLatitude(),
                donation.getLongitude(),
                donation.getFoodSafetyDeclared(),
                donation.getFoodSafetyDeclarationText(),
                donation.getFoodSafetyDeclaredAt(),
                donation.getStatus(),
                donation.getCreatedAt(),
                donation.getUpdatedAt()
        );
    }
}

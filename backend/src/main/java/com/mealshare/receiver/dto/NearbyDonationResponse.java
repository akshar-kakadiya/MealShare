package com.mealshare.receiver.dto;

import com.mealshare.donation.projection.NearbyDonationProjection;

import java.time.Instant;
import java.util.UUID;

public record NearbyDonationResponse(
        UUID id,
        String title,
        String description,
        String foodCategory,
        Double quantity,
        String quantityUnit,
        Instant consumeBefore,
        Instant pickupAvailableFrom,
        Instant pickupAvailableUntil,
        String pickupAddress,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Double distanceMeters,
        String status,
        Boolean foodSafetyDeclared,
        Instant createdAt
) {
    public static NearbyDonationResponse fromProjection(NearbyDonationProjection proj) {
        Double distKm = proj.getDistanceKm();
        Double distMeters = proj.getDistanceMeters();
        if (distKm == null && distMeters != null) {
            distKm = distMeters / 1000.0;
        } else if (distMeters == null && distKm != null) {
            distMeters = distKm * 1000.0;
        }
        if (distKm != null) {
            distKm = Math.round(distKm * 100.0) / 100.0; // round to 2 decimal places
        }

        return new NearbyDonationResponse(
                proj.getId(),
                proj.getTitle(),
                proj.getDescription(),
                proj.getFoodCategory(),
                proj.getQuantity(),
                proj.getQuantityUnit(),
                proj.getConsumeBefore(),
                proj.getPickupAvailableFrom(),
                proj.getPickupAvailableUntil(),
                proj.getPickupAddress(),
                proj.getLatitude(),
                proj.getLongitude(),
                distKm,
                distMeters,
                proj.getStatus(),
                proj.getFoodSafetyDeclared(),
                proj.getCreatedAt()
        );
    }
}

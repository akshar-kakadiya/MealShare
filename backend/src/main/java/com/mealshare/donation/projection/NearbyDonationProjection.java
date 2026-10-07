package com.mealshare.donation.projection;

import java.time.Instant;
import java.util.UUID;

public interface NearbyDonationProjection {
    UUID getId();
    String getTitle();
    String getDescription();
    String getFoodCategory();
    Double getQuantity();
    String getQuantityUnit();
    Instant getPreparationDateTime();
    Instant getConsumeBefore();
    Instant getPickupAvailableFrom();
    Instant getPickupAvailableUntil();
    String getPickupAddress();
    Double getLatitude();
    Double getLongitude();
    Boolean getFoodSafetyDeclared();
    String getStatus();
    Instant getCreatedAt();
    Double getDistanceKm();
    Double getDistanceMeters();
}

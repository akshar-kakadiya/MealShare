package com.mealshare.pickup.dto;

import com.mealshare.pickup.PickupStatus;
import java.time.Instant;
import java.util.UUID;

public record LiveTrackingDto(
        UUID pickupId,
        PickupStatus status,
        Boolean trackingActive,
        Double currentLatitude,
        Double currentLongitude,
        Instant locationUpdatedAt,
        Double donorLatitude,
        Double donorLongitude,
        String pickupAddress,
        Double destinationLatitude,
        Double destinationLongitude,
        String destinationAddress,
        Double distanceToTargetMeters,
        Boolean withinGeofence,
        String phase,
        String receiverOrgName,
        String donorName,
        String donationTitle
) {}

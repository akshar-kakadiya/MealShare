package com.mealshare.pickup.dto;

import java.time.Instant;
import java.util.UUID;

public record PickupRouteDto(
        UUID pickupId,
        String encodedPolyline,
        Long distanceMeters,
        Long durationSeconds,
        String formattedDistance,
        String formattedDuration,
        Double originLatitude,
        Double originLongitude,
        Double destinationLatitude,
        Double destinationLongitude,
        Instant calculatedAt,
        boolean available,
        String message
) {
    public static PickupRouteDto unavailable(UUID pickupId, String message) {
        return new PickupRouteDto(
                pickupId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                false,
                message
        );
    }

    public static PickupRouteDto available(
            UUID pickupId,
            String encodedPolyline,
            Long distanceMeters,
            Long durationSeconds,
            Double originLat,
            Double originLng,
            Double destLat,
            Double destLng
    ) {
        String formattedDist = formatDistance(distanceMeters);
        String formattedDur = formatDuration(durationSeconds);
        return new PickupRouteDto(
                pickupId,
                encodedPolyline,
                distanceMeters,
                durationSeconds,
                formattedDist,
                formattedDur,
                originLat,
                originLng,
                destLat,
                destLng,
                Instant.now(),
                true,
                "OK"
        );
    }

    private static String formatDistance(Long meters) {
        if (meters == null) return null;
        if (meters < 1000) {
            return meters + " m";
        }
        return String.format(java.util.Locale.US, "%.1f km", meters / 1000.0);
    }

    private static String formatDuration(Long seconds) {
        if (seconds == null) return null;
        long mins = Math.max(1, (seconds + 30) / 60);
        if (mins < 60) {
            return mins + " mins";
        }
        long hours = mins / 60;
        long remainingMins = mins % 60;
        return remainingMins > 0 ? hours + " hr " + remainingMins + " mins" : hours + " hr";
    }
}

package com.mealshare.pickup.routing;

import com.mealshare.pickup.dto.PickupRouteDto;

import java.util.UUID;

public interface PickupRoutingService {

    /**
     * Calculates an authentic road-following route from the receiver's current GPS location
     * to the pickup destination (or delivery shelter) via Google Maps Routes API.
     *
     * @param userFirebaseUid authenticated caller's Firebase UID
     * @param pickupId the pickup identifier
     * @param requestLat optional client-reported current latitude for up-to-the-second routing
     * @param requestLng optional client-reported current longitude for up-to-the-second routing
     * @return PickupRouteDto with encoded polyline, distance, and duration, or graceful unavailable state
     */
    PickupRouteDto getRoute(String userFirebaseUid, UUID pickupId, Double requestLat, Double requestLng);
}

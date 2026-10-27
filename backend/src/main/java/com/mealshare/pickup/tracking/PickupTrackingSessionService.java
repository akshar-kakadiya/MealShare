package com.mealshare.pickup.tracking;

import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dedicated backend service managing the lifecycle of live logistics tracking sessions
 * in Firebase Realtime Database.
 *
 * Spring Boot and PostgreSQL remain the absolute authoritative sources of truth for:
 * 1. Participant assignments (donor UID and receiver UID)
 * 2. Pickup status transitions
 * 3. Lifecycle authorization
 */
public interface PickupTrackingSessionService {

    /**
     * Activates a live tracking session in Firebase Realtime Database for the pickup.
     * Must ONLY be invoked after the receiver has legitimately started the pickup journey
     * (status is EN_ROUTE_TO_PICKUP and trackingActive is true).
     *
     * @param pickup the active Pickup entity
     */
    void activateSession(Pickup pickup);

    /**
     * Deactivates the live tracking session upon confirmed arrival (AT_PICKUP) or cancellation (CANCELLED).
     * Sets trackingActive to false and updates status, immediately preventing further client location writes.
     *
     * @param pickupId       the target pickup UUID
     * @param terminalStatus the terminal/deactivation status (e.g. AT_PICKUP, CANCELLED)
     */
    void deactivateSession(UUID pickupId, PickupStatus terminalStatus);

    /**
     * Completely removes the session node from active_tracking.
     *
     * @param pickupId the target pickup UUID
     */
    void cleanupSession(UUID pickupId);

    /**
     * Retrieves the current tracking session snapshot from the live tracking node.
     *
     * @param pickupId the target pickup UUID
     * @return map of session properties if present
     */
    Optional<Map<String, Object>> getSession(UUID pickupId);

    /**
     * Checks if a session is currently active in the live tracking node.
     *
     * @param pickupId the target pickup UUID
     * @return true if session exists and trackingActive is true
     */
    boolean isSessionActive(UUID pickupId);
}

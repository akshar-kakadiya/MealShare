package com.mealshare.pickup.tracking;

import java.util.Map;
import java.util.Optional;

/**
 * Low-level transport gateway for writing, updating, reading, and clearing
 * real-time live tracking nodes in Firebase Realtime Database.
 */
public interface RealtimeTrackingGateway {

    /**
     * Writes or overwrites a tracking session under the specified path.
     *
     * @param path target path in RTDB (e.g. "active_tracking/{pickupId}")
     * @param data payload map
     */
    void writeSession(String path, Map<String, Object> data);

    /**
     * Atomically updates specific keys under the specified path.
     *
     * @param path    target path in RTDB (e.g. "active_tracking/{pickupId}")
     * @param updates key-value updates map
     */
    void updateSession(String path, Map<String, Object> updates);

    /**
     * Deletes the session at the specified path.
     *
     * @param path target path in RTDB
     */
    void deleteSession(String path);

    /**
     * Retrieves the current snapshot at the specified path.
     *
     * @param path target path in RTDB
     * @return map of properties if present
     */
    Optional<Map<String, Object>> readSession(String path);
}

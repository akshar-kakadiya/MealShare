package com.mealshare.pickup.tracking;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory gateway used for automated test suites, offline development,
 * and deterministic unit testing without requiring an active external Firebase connection.
 */
public class InMemoryRealtimeTrackingGateway implements RealtimeTrackingGateway {

    private final Map<String, Map<String, Object>> storage = new ConcurrentHashMap<>();

    @Override
    public void writeSession(String path, Map<String, Object> data) {
        storage.put(path, new ConcurrentHashMap<>(data));
    }

    @Override
    public void updateSession(String path, Map<String, Object> updates) {
        storage.compute(path, (k, existing) -> {
            Map<String, Object> map = (existing != null) ? existing : new ConcurrentHashMap<>();
            map.putAll(updates);
            return map;
        });
    }

    @Override
    public void deleteSession(String path) {
        storage.remove(path);
    }

    @Override
    public Optional<Map<String, Object>> readSession(String path) {
        Map<String, Object> data = storage.get(path);
        return Optional.ofNullable(data != null ? new ConcurrentHashMap<>(data) : null);
    }

    public void clear() {
        storage.clear();
    }
}

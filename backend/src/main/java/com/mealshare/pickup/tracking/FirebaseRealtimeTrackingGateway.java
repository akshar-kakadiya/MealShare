package com.mealshare.pickup.tracking;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class FirebaseRealtimeTrackingGateway implements RealtimeTrackingGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseRealtimeTrackingGateway.class);

    private final FirebaseDatabase firebaseDatabase;

    @Autowired
    public FirebaseRealtimeTrackingGateway(@Nullable FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
        if (this.firebaseDatabase != null) {
            log.info("FirebaseRealtimeTrackingGateway initialized with live FirebaseDatabase.");
        } else {
            log.warn("FirebaseRealtimeTrackingGateway initialized without FirebaseDatabase (offline/mock mode active).");
        }
    }

    @Override
    public void writeSession(String path, Map<String, Object> data) {
        if (firebaseDatabase == null) {
            log.debug("Skipping writeSession to '{}' because FirebaseDatabase is not configured.", path);
            return;
        }
        try {
            DatabaseReference ref = firebaseDatabase.getReference(path);
            ref.setValueAsync(data);
            log.debug("Dispatched writeSession to RTDB at '{}'.", path);
        } catch (Exception e) {
            log.error("Failed to write tracking session to RTDB at '{}': {}", path, e.getMessage(), e);
        }
    }

    @Override
    public void updateSession(String path, Map<String, Object> updates) {
        if (firebaseDatabase == null) {
            log.debug("Skipping updateSession to '{}' because FirebaseDatabase is not configured.", path);
            return;
        }
        try {
            DatabaseReference ref = firebaseDatabase.getReference(path);
            ref.updateChildrenAsync(updates);
            log.debug("Dispatched updateSession to RTDB at '{}'.", path);
        } catch (Exception e) {
            log.error("Failed to update tracking session in RTDB at '{}': {}", path, e.getMessage(), e);
        }
    }

    @Override
    public void deleteSession(String path) {
        if (firebaseDatabase == null) {
            log.debug("Skipping deleteSession at '{}' because FirebaseDatabase is not configured.", path);
            return;
        }
        try {
            DatabaseReference ref = firebaseDatabase.getReference(path);
            ref.removeValueAsync();
            log.debug("Dispatched deleteSession at '{}'.", path);
        } catch (Exception e) {
            log.error("Failed to delete tracking session from RTDB at '{}': {}", path, e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> readSession(String path) {
        if (firebaseDatabase == null) {
            return Optional.empty();
        }
        try {
            DatabaseReference ref = firebaseDatabase.getReference(path);
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() instanceof Map) {
                        future.complete((Map<String, Object>) snapshot.getValue());
                    } else {
                        future.complete(null);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(error.toException());
                }
            });

            Map<String, Object> val = future.get(3, TimeUnit.SECONDS);
            return Optional.ofNullable(val);
        } catch (Exception e) {
            log.warn("Could not synchronously read RTDB session at '{}': {}", path, e.getMessage());
            return Optional.empty();
        }
    }
}

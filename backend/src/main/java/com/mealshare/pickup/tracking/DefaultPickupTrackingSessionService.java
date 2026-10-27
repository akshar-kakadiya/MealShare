package com.mealshare.pickup.tracking;

import com.mealshare.donation.Donation;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DefaultPickupTrackingSessionService implements PickupTrackingSessionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPickupTrackingSessionService.class);
    private static final String ACTIVE_TRACKING_ROOT = "active_tracking/";

    private final RealtimeTrackingGateway gateway;

    public DefaultPickupTrackingSessionService(RealtimeTrackingGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void activateSession(Pickup pickup) {
        if (pickup == null || pickup.getId() == null) {
            throw new IllegalArgumentException("Pickup cannot be null and must possess a valid ID.");
        }

        if (pickup.getStatus() != PickupStatus.EN_ROUTE_TO_PICKUP) {
            throw new IllegalStateException("Cannot activate tracking session for pickup in status: " + pickup.getStatus()
                    + ". Tracking sessions may ONLY be activated when status is EN_ROUTE_TO_PICKUP.");
        }

        if (!Boolean.TRUE.equals(pickup.getTrackingActive())) {
            throw new IllegalStateException("Cannot activate tracking session when pickup trackingActive flag is false.");
        }

        String donorUid = null;
        Donation donation = null;
        DonorProfile donor = null;
        if (pickup.getDonationRequest() != null) {
            donation = pickup.getDonationRequest().getDonation();
            if (donation != null) {
                donor = donation.getDonor();
                if (donor != null && donor.getUser() != null) {
                    donorUid = donor.getUser().getFirebaseUid();
                }
            }
        }

        String receiverUid = null;
        if (pickup.getReceiver() != null && pickup.getReceiver().getUser() != null) {
            receiverUid = pickup.getReceiver().getUser().getFirebaseUid();
        }

        if (donorUid == null || donorUid.isBlank()) {
            throw new IllegalStateException("Cannot activate tracking session: Donor Firebase UID is missing or unassigned.");
        }
        if (receiverUid == null || receiverUid.isBlank()) {
            throw new IllegalStateException("Cannot activate tracking session: Receiver Firebase UID is missing or unassigned.");
        }

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("pickupId", pickup.getId().toString());
        session.put("donorUid", donorUid);
        session.put("receiverUid", receiverUid);
        session.put("status", PickupStatus.EN_ROUTE_TO_PICKUP.name());
        session.put("trackingActive", true);

        Map<String, Object> donorLoc = new LinkedHashMap<>();
        donorLoc.put("latitude", donation != null ? donation.getLatitude() : null);
        donorLoc.put("longitude", donation != null ? donation.getLongitude() : null);
        donorLoc.put("address", donation != null ? donation.getPickupAddress() : null);
        donorLoc.put("donorName", donor != null ? donor.getEntityName() : null);
        session.put("donorLocation", donorLoc);

        if (pickup.getCurrentLatitude() != null && pickup.getCurrentLongitude() != null) {
            Map<String, Object> initialLoc = new LinkedHashMap<>();
            initialLoc.put("latitude", pickup.getCurrentLatitude());
            initialLoc.put("longitude", pickup.getCurrentLongitude());
            initialLoc.put("timestamp", System.currentTimeMillis());
            session.put("receiverLocation", initialLoc);
        }

        long now = System.currentTimeMillis();
        session.put("activatedAt", now);
        session.put("lastUpdatedAt", now);

        String path = ACTIVE_TRACKING_ROOT + pickup.getId();
        gateway.writeSession(path, session);
        log.info("Activated live tracking session in RTDB at '{}' for donor={}, receiver={}",
                path, donorUid, receiverUid);
    }

    @Override
    public void deactivateSession(UUID pickupId, PickupStatus terminalStatus) {
        if (pickupId == null) {
            throw new IllegalArgumentException("pickupId cannot be null.");
        }

        if (terminalStatus == null || terminalStatus == PickupStatus.EN_ROUTE_TO_PICKUP) {
            throw new IllegalArgumentException("terminalStatus must be an inactive/terminal status (e.g. AT_PICKUP, CANCELLED, COMPLETED).");
        }

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("trackingActive", false);
        updates.put("status", terminalStatus.name());
        long now = System.currentTimeMillis();
        updates.put("deactivatedAt", now);
        updates.put("lastUpdatedAt", now);

        String path = ACTIVE_TRACKING_ROOT + pickupId;
        gateway.updateSession(path, updates);
        log.info("Deactivated live tracking session in RTDB at '{}' with status={}", path, terminalStatus);
    }

    @Override
    public void cleanupSession(UUID pickupId) {
        if (pickupId == null) {
            return;
        }
        String path = ACTIVE_TRACKING_ROOT + pickupId;
        gateway.deleteSession(path);
        log.info("Cleaned up live tracking session in RTDB at '{}'", path);
    }

    @Override
    public Optional<Map<String, Object>> getSession(UUID pickupId) {
        if (pickupId == null) {
            return Optional.empty();
        }
        return gateway.readSession(ACTIVE_TRACKING_ROOT + pickupId);
    }

    @Override
    public boolean isSessionActive(UUID pickupId) {
        if (pickupId == null) {
            return false;
        }
        return getSession(pickupId)
                .map(session -> Boolean.TRUE.equals(session.get("trackingActive")))
                .orElse(false);
    }
}

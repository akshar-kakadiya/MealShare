package com.mealshare.pickup.controller;

import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.dto.LiveTrackingDto;
import com.mealshare.pickup.dto.LocationUpdateDto;
import com.mealshare.security.FirebaseAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class PickupTrackingWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(PickupTrackingWebSocketController.class);

    private final PickupService pickupService;
    private final SimpMessagingTemplate messagingTemplate;

    public PickupTrackingWebSocketController(PickupService pickupService,
                                             SimpMessagingTemplate messagingTemplate) {
        this.pickupService = pickupService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/pickups/{pickupId}/location")
    public void handleLocationUpdate(@DestinationVariable UUID pickupId,
                                     @Payload LocationUpdateDto location,
                                     Principal principal) {
        String firebaseUid = extractFirebaseUid(principal);
        log.debug("Received STOMP location update for pickup {} from user {}: lat={}, lon={}",
                pickupId, firebaseUid, location.latitude(), location.longitude());

        pickupService.recordLocationUpdate(firebaseUid, pickupId, location, null);

        // Fetch refreshed full LiveTrackingDto and broadcast to subscribers
        LiveTrackingDto trackingDto = pickupService.getLiveTracking(firebaseUid, pickupId);
        messagingTemplate.convertAndSend("/topic/pickups/" + pickupId + "/tracking", trackingDto);
    }

    private String extractFirebaseUid(Principal principal) {
        if (principal instanceof FirebaseAuthenticationToken token) {
            return token.getPrincipal().uid();
        }
        return principal.getName();
    }
}

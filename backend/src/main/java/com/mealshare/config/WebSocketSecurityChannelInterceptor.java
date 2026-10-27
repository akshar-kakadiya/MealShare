package com.mealshare.config;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.security.FirebaseAuthenticationToken;
import com.mealshare.security.FirebasePrincipal;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class WebSocketSecurityChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityChannelInterceptor.class);

    private final FirebaseTokenValidator tokenValidator;
    private final UserRepository userRepository;
    private final PickupRepository pickupRepository;

    public WebSocketSecurityChannelInterceptor(FirebaseTokenValidator tokenValidator,
                                               UserRepository userRepository,
                                               PickupRepository pickupRepository) {
        this.tokenValidator = tokenValidator;
        this.userRepository = userRepository;
        this.pickupRepository = pickupRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("token");
        }
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("X-Authorization");
        }

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("STOMP connect attempt without Authorization header.");
            throw new AccessDeniedException("Authentication required to connect to WebSocket broker.");
        }

        String idToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();

        try {
            FirebaseToken decodedToken = tokenValidator.verifyIdToken(idToken);
            FirebasePrincipal principal = new FirebasePrincipal(
                    decodedToken.getUid(),
                    decodedToken.getEmail(),
                    decodedToken.getName(),
                    decodedToken.getClaims() != null ? decodedToken.getClaims() : Map.of()
            );

            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional<User> userOpt = userRepository.findByFirebaseUid(principal.uid());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getStatus() == UserStatus.SUSPENDED) {
                    log.warn("STOMP connect rejected: user {} is SUSPENDED.", user.getId());
                    throw new AccessDeniedException("User account is suspended.");
                }
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_UNREGISTERED"));
            }

            FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(principal, authorities);
            accessor.setUser(authentication);
            log.debug("STOMP CONNECT authenticated for user: {}", principal.uid());

        } catch (Exception e) {
            log.warn("STOMP token authentication failed: {}", e.getMessage());
            throw new AccessDeniedException("Invalid or expired authentication token: " + e.getMessage());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/topic/pickups/") && destination.endsWith("/tracking")) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new AccessDeniedException("Authentication required to subscribe to live tracking channel.");
            }

            UUID pickupId = extractPickupIdFromDestination(destination);
            validateTrackingAccess(principal, pickupId);
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/app/pickups/") && destination.endsWith("/location")) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new AccessDeniedException("Authentication required to publish location updates.");
            }

            UUID pickupId = extractPickupIdFromDestination(destination);
            validateLocationPublishAccess(principal, pickupId);
        }
    }

    private UUID extractPickupIdFromDestination(String destination) {
        try {
            // Pattern: /topic/pickups/{pickupId}/tracking or /app/pickups/{pickupId}/location
            String[] parts = destination.split("/");
            // parts: ["", "topic" or "app", "pickups", "{pickupId}", "tracking" or "location"]
            if (parts.length >= 4) {
                return UUID.fromString(parts[3]);
            }
        } catch (Exception e) {
            log.warn("Failed to extract pickupId from destination: {}", destination);
        }
        throw new IllegalArgumentException("Malformed tracking destination: " + destination);
    }

    private void validateTrackingAccess(Principal principal, UUID pickupId) {
        String firebaseUid = getFirebaseUid(principal);
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User not found for subscription."));

        if (user.getRole() == UserRole.ADMIN) {
            return; // Admins can monitor any pickup
        }

        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new AccessDeniedException("Pickup not found with id: " + pickupId));

        boolean authorized = false;

        // Check if user is the donor of this pickup's donation
        if (pickup.getDonationRequest() != null &&
            pickup.getDonationRequest().getDonation() != null &&
            pickup.getDonationRequest().getDonation().getDonor() != null) {
            DonorProfile donor = pickup.getDonationRequest().getDonation().getDonor();
            if (donor.getUser() != null && donor.getUser().getId().equals(user.getId())) {
                authorized = true;
            }
        }

        // Check if user is the receiver organization of this pickup
        if (!authorized && pickup.getReceiver() != null && pickup.getReceiver().getUser() != null) {
            ReceiverOrganization receiver = pickup.getReceiver();
            if (receiver.getUser().getId().equals(user.getId())) {
                authorized = true;
            }
        }

        if (!authorized) {
            log.warn("Unauthorized subscription attempt to pickup {} by user {}", pickupId, user.getId());
            throw new AccessDeniedException("You are not authorized to subscribe to live tracking for this pickup.");
        }
    }

    private void validateLocationPublishAccess(Principal principal, UUID pickupId) {
        String firebaseUid = getFirebaseUid(principal);
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User not found for location publishing."));

        if (user.getRole() != UserRole.RECEIVER) {
            throw new AccessDeniedException("Only authorized receivers can publish location updates.");
        }

        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new AccessDeniedException("Pickup not found with id: " + pickupId));

        if (pickup.getReceiver() == null || pickup.getReceiver().getUser() == null ||
            !pickup.getReceiver().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not the assigned receiver for this pickup.");
        }

        if (!Boolean.TRUE.equals(pickup.getTrackingActive())) {
            throw new IllegalStateException("Location tracking is inactive for this pickup.");
        }
    }

    private String getFirebaseUid(Principal principal) {
        if (principal instanceof FirebaseAuthenticationToken token) {
            return token.getPrincipal().uid();
        }
        return principal.getName();
    }
}

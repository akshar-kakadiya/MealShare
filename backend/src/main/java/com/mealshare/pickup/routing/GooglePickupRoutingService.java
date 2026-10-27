package com.mealshare.pickup.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.pickup.dto.PickupRouteDto;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GooglePickupRoutingService implements PickupRoutingService {

    private static final Logger log = LoggerFactory.getLogger(GooglePickupRoutingService.class);

    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.maps.api-key:}")
    private String apiKey;

    @Value("${google.maps.routes-url:https://routes.googleapis.com/directions/v2:computeRoutes}")
    private String routesUrl;

    // Cache to avoid aggressive billing / rate limits on identical coordinates
    private final Map<UUID, CachedRoute> routeCache = new ConcurrentHashMap<>();

    private record CachedRoute(
            PickupRouteDto routeDto,
            double originLat,
            double originLng,
            double destLat,
            double destLng,
            Instant calculatedAt
    ) {}

    public GooglePickupRoutingService(
            PickupRepository pickupRepository,
            UserRepository userRepository,
            @Autowired(required = false) RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setRoutesUrl(String routesUrl) {
        this.routesUrl = routesUrl;
    }

    @Override
    public PickupRouteDto getRoute(String userFirebaseUid, UUID pickupId, Double requestLat, Double requestLng) {
        User user = userRepository.findByFirebaseUid(userFirebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with id: " + pickupId));

        validateAuthorization(user, pickup);

        // Determine destination coordinates
        Double destLat = null;
        Double destLng = null;

        DonationRequest donationRequest = pickup.getDonationRequest();
        Donation donation = donationRequest != null ? donationRequest.getDonation() : null;
        ReceiverOrganization receiver = pickup.getReceiver();

        if (pickup.getStatus() == PickupStatus.IN_TRANSIT) {
            if (receiver != null) {
                destLat = receiver.getServiceLatitude();
                destLng = receiver.getServiceLongitude();
            }
        } else {
            if (donation != null) {
                destLat = donation.getLatitude();
                destLng = donation.getLongitude();
            }
        }

        if (destLat == null || destLng == null) {
            return PickupRouteDto.unavailable(pickupId, "Target destination coordinates are not configured.");
        }

        // Determine origin coordinates (receiver position)
        Double originLat = requestLat != null ? requestLat : pickup.getCurrentLatitude();
        Double originLng = requestLng != null ? requestLng : pickup.getCurrentLongitude();

        if (originLat == null || originLng == null) {
            return PickupRouteDto.unavailable(pickupId, "Receiver current location is not yet available.");
        }

        // Check cache (reuse if receiver has moved < 75m and calculation was within 45s)
        CachedRoute cached = routeCache.get(pickupId);
        if (cached != null) {
            long secondsSince = Duration.between(cached.calculatedAt(), Instant.now()).getSeconds();
            double originDeviation = PickupService.calculateHaversineDistanceMeters(
                    originLat, originLng, cached.originLat(), cached.originLng()
            );
            if (secondsSince < 45 && originDeviation < 75.0 &&
                    Math.abs(destLat - cached.destLat()) < 0.0001 &&
                    Math.abs(destLng - cached.destLng()) < 0.0001) {
                return cached.routeDto();
            }
        }

        // Validate API Key presence
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equalsIgnoreCase("test-api-key")) {
            log.info("Google Maps Routes API key is not configured or is placeholder; returning unavailable route.");
            return PickupRouteDto.unavailable(pickupId, "Google Maps Routes service is not configured.");
        }

        // Execute Routes API call
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", apiKey.trim());
            headers.set("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline");

            Map<String, Object> body = Map.of(
                    "origin", Map.of(
                            "location", Map.of(
                                    "latLng", Map.of("latitude", originLat, "longitude", originLng)
                            )
                    ),
                    "destination", Map.of(
                            "location", Map.of(
                                    "latLng", Map.of("latitude", destLat, "longitude", destLng)
                            )
                    ),
                    "travelMode", "DRIVE",
                    "routingPreference", "TRAFFIC_AWARE"
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    routesUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode routes = root.path("routes");
                if (routes.isArray() && !routes.isEmpty()) {
                    JsonNode route0 = routes.get(0);
                    long distanceMeters = route0.path("distanceMeters").asLong(0L);
                    String durationRaw = route0.path("duration").asText("");
                    long durationSeconds = parseDurationSeconds(durationRaw);
                    String encodedPolyline = route0.path("polyline").path("encodedPolyline").asText("");

                    if (!encodedPolyline.isEmpty()) {
                        PickupRouteDto result = PickupRouteDto.available(
                                pickupId,
                                encodedPolyline,
                                distanceMeters,
                                durationSeconds,
                                originLat,
                                originLng,
                                destLat,
                                destLng
                        );
                        routeCache.put(pickupId, new CachedRoute(result, originLat, originLng, destLat, destLng, Instant.now()));
                        return result;
                    }
                }
            }
            log.warn("Google Maps Routes API returned empty or unrecognized route body for pickup {}", pickupId);
            return PickupRouteDto.unavailable(pickupId, "No route found between receiver and destination.");
        } catch (RestClientException e) {
            log.warn("Failed to retrieve Google Maps route for pickup {}: {}", pickupId, e.getMessage());
            return PickupRouteDto.unavailable(pickupId, "Google Routes API call failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error parsing Google Maps Routes response for pickup {}", pickupId, e);
            return PickupRouteDto.unavailable(pickupId, "Routing computation error.");
        }
    }

    private void validateAuthorization(User user, Pickup pickup) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        DonationRequest donationRequest = pickup.getDonationRequest();
        Donation donation = donationRequest != null ? donationRequest.getDonation() : null;
        DonorProfile donor = donation != null ? donation.getDonor() : null;
        ReceiverOrganization receiver = pickup.getReceiver();

        boolean isDonor = donor != null && donor.getUser().getId().equals(user.getId());
        boolean isReceiver = receiver != null && receiver.getUser().getId().equals(user.getId());

        if (!isDonor && !isReceiver) {
            throw new AccessDeniedException("You do not have permission to view routing for this pickup.");
        }
    }

    private long parseDurationSeconds(String durationRaw) {
        if (durationRaw == null || durationRaw.isEmpty()) return 0L;
        String clean = durationRaw.replace("s", "").trim();
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

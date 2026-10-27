package com.mealshare.pickup.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealshare.donation.Donation;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.pickup.dto.PickupRouteDto;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GooglePickupRoutingServiceTest {

    @Mock
    private PickupRepository pickupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GooglePickupRoutingService routingService;

    private User donorUser;
    private User receiverUser;
    private User strangerUser;
    private Pickup pickup;
    private Donation donation;
    private ReceiverOrganization receiver;
    private final UUID pickupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        routingService = new GooglePickupRoutingService(
                pickupRepository,
                userRepository,
                restTemplate,
                objectMapper
        );
        routingService.setApiKey("test-real-api-key");
        routingService.setRoutesUrl("https://routes.googleapis.com/directions/v2:computeRoutes");

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setRole(UserRole.DONOR);

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid");
        receiverUser.setRole(UserRole.RECEIVER);

        receiver = new ReceiverOrganization();
        receiver.setId(UUID.randomUUID());
        receiver.setUser(receiverUser);
        receiver.setServiceLatitude(12.9200);
        receiver.setServiceLongitude(77.6100);

        strangerUser = new User();
        strangerUser.setId(UUID.randomUUID());
        strangerUser.setFirebaseUid("stranger-uid");
        strangerUser.setRole(UserRole.DONOR);

        donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setDonor(donorProfile);
        donation.setLatitude(12.9352);
        donation.setLongitude(77.6245);

        DonationRequest request = new DonationRequest();
        request.setId(UUID.randomUUID());
        request.setDonation(donation);
        request.setReceiverOrganization(receiver);

        pickup = new Pickup();
        pickup.setId(pickupId);
        pickup.setStatus(PickupStatus.EN_ROUTE_TO_PICKUP);
        pickup.setDonationRequest(request);
        pickup.setReceiver(receiver);
        pickup.setCurrentLatitude(12.9716);
        pickup.setCurrentLongitude(77.5946);
    }

    @Test
    @DisplayName("Authorized donor receives road route successfully when Google Routes API responds")
    void getRoute_authorizedDonor_success() {
        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));

        String mockResponseBody = """
                {
                  "routes": [
                    {
                      "distanceMeters": 5420,
                      "duration": "840s",
                      "polyline": {
                        "encodedPolyline": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
                      }
                    }
                  ]
                }
                """;
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        PickupRouteDto route = routingService.getRoute("donor-uid", pickupId, 12.9716, 77.5946);

        assertThat(route).isNotNull();
        assertThat(route.available()).isTrue();
        assertThat(route.encodedPolyline()).isEqualTo("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        assertThat(route.distanceMeters()).isEqualTo(5420L);
        assertThat(route.durationSeconds()).isEqualTo(840L);
        assertThat(route.formattedDistance()).isEqualTo("5.4 km");
        assertThat(route.formattedDuration()).isEqualTo("14 mins");
    }

    @Test
    @DisplayName("Stranger user is blocked with AccessDeniedException")
    void getRoute_strangerUser_throwsAccessDenied() {
        when(userRepository.findByFirebaseUid("stranger-uid")).thenReturn(Optional.of(strangerUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> routingService.getRoute("stranger-uid", pickupId, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission");

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Gracefully returns unavailable when API key is unconfigured")
    void getRoute_missingApiKey_returnsUnavailableGracefully() {
        routingService.setApiKey("");
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));

        PickupRouteDto route = routingService.getRoute("receiver-uid", pickupId, 12.9716, 77.5946);

        assertThat(route.available()).isFalse();
        assertThat(route.message()).contains("not configured");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Gracefully handles RestClientException without throwing 500 error")
    void getRoute_apiFailure_returnsUnavailableGracefully() {
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        PickupRouteDto route = routingService.getRoute("receiver-uid", pickupId, 12.9716, 77.5946);

        assertThat(route.available()).isFalse();
        assertThat(route.message()).contains("Google Routes API call failed");
    }

    @Test
    @DisplayName("Small GPS jitter (<75m) returns cached route without calling Routes API again")
    void getRoute_cacheHit_preventsRedundantNetworkCalls() {
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));

        String mockResponseBody = """
                {
                  "routes": [
                    {
                      "distanceMeters": 3000,
                      "duration": "360s",
                      "polyline": {
                        "encodedPolyline": "cached_polyline_str"
                      }
                    }
                  ]
                }
                """;
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        // First call
        PickupRouteDto route1 = routingService.getRoute("receiver-uid", pickupId, 12.9716, 77.5946);
        assertThat(route1.available()).isTrue();

        // Second call with 20 meters difference (jitter)
        PickupRouteDto route2 = routingService.getRoute("receiver-uid", pickupId, 12.9717, 77.5947);
        assertThat(route2.available()).isTrue();
        assertThat(route2.encodedPolyline()).isEqualTo("cached_polyline_str");

        // Verify exchange was called only once!
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }
}

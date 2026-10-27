package com.mealshare.pickup;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.config.WebSocketSecurityChannelInterceptor;
import com.mealshare.donation.Donation;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.dto.FoodInspectionDto;
import com.mealshare.pickup.dto.LocationUpdateDto;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.security.FirebaseAuthenticationToken;
import com.mealshare.security.FirebasePrincipal;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupTrackingSecurityAndLifecycleTest {

    @Mock
    private FirebaseTokenValidator tokenValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PickupRepository pickupRepository;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketSecurityChannelInterceptor interceptor;

    private User donorUser;
    private User receiverUser;
    private User strangerUser;
    private User suspendedUser;

    private Pickup testPickup;
    private UUID pickupId;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketSecurityChannelInterceptor(tokenValidator, userRepository, pickupRepository);

        pickupId = UUID.randomUUID();

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setEmail("donor@mealshare.org");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid");
        receiverUser.setEmail("receiver@mealshare.org");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        strangerUser = new User();
        strangerUser.setId(UUID.randomUUID());
        strangerUser.setFirebaseUid("stranger-uid");
        strangerUser.setEmail("stranger@mealshare.org");
        strangerUser.setRole(UserRole.DONOR);
        strangerUser.setStatus(UserStatus.ACTIVE);

        suspendedUser = new User();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setFirebaseUid("suspended-uid");
        suspendedUser.setEmail("suspended@mealshare.org");
        suspendedUser.setRole(UserRole.RECEIVER);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);
        donorProfile.setEntityName("Green Grocer");

        Donation donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setDonor(donorProfile);
        donation.setTitle("Fresh Vegetables");
        donation.setLatitude(12.9716);
        donation.setLongitude(77.5946);

        ReceiverOrganization receiverOrg = new ReceiverOrganization();
        receiverOrg.setId(UUID.randomUUID());
        receiverOrg.setUser(receiverUser);
        receiverOrg.setOrgName("Care Foundation");
        receiverOrg.setServiceLatitude(12.9750);
        receiverOrg.setServiceLongitude(77.5980);

        DonationRequest request = new DonationRequest();
        request.setId(UUID.randomUUID());
        request.setDonation(donation);
        request.setReceiverOrganization(receiverOrg);
        request.setStatus(DonationRequestStatus.APPROVED);

        testPickup = new Pickup();
        testPickup.setId(pickupId);
        testPickup.setDonationRequest(request);
        testPickup.setReceiver(receiverOrg);
        testPickup.setStatus(PickupStatus.EN_ROUTE_TO_PICKUP);
        testPickup.setTrackingActive(true);
    }

    // ─── STOMP CONNECT TESTS ───

    @Test
    @DisplayName("STOMP connect without token throws AccessDeniedException")
    void testConnectWithoutToken_throwsAccessDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required to connect to WebSocket broker");
    }

    @Test
    @DisplayName("STOMP connect with suspended account throws AccessDeniedException")
    void testConnectWithSuspendedAccount_throwsAccessDenied() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("suspended-uid");
        when(tokenValidator.verifyIdToken("token-suspended")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("suspended-uid")).thenReturn(Optional.of(suspendedUser));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token-suspended");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User account is suspended");
    }

    @Test
    @DisplayName("STOMP connect with valid token establishes user principal")
    void testConnectWithValidToken_establishesPrincipal() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("donor-uid");
        when(mockToken.getEmail()).thenReturn("donor@mealshare.org");
        when(tokenValidator.verifyIdToken("token-donor")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer token-donor");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertThat(result).isNotNull();

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isInstanceOf(FirebaseAuthenticationToken.class);
        FirebaseAuthenticationToken authToken = (FirebaseAuthenticationToken) resultAccessor.getUser();
        assertThat(authToken.getPrincipal().uid()).isEqualTo("donor-uid");
    }

    // ─── STOMP SUBSCRIBE TESTS (Tracking Privacy) ───

    @Test
    @DisplayName("Unauthorized stranger cannot subscribe to pickup live tracking")
    void testSubscribeTracking_unauthorizedStranger_throwsAccessDenied() {
        when(userRepository.findByFirebaseUid("stranger-uid")).thenReturn(Optional.of(strangerUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(testPickup));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/pickups/" + pickupId + "/tracking");
        FirebasePrincipal principal = new FirebasePrincipal("stranger-uid", "stranger@mealshare.org", "Stranger", Map.of());
        accessor.setUser(new FirebaseAuthenticationToken(principal, List.of()));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to subscribe to live tracking for this pickup");
    }

    @Test
    @DisplayName("Owning donor can subscribe to pickup live tracking")
    void testSubscribeTracking_authorizedDonor_succeeds() {
        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(testPickup));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/pickups/" + pickupId + "/tracking");
        FirebasePrincipal principal = new FirebasePrincipal("donor-uid", "donor@mealshare.org", "Donor", Map.of());
        accessor.setUser(new FirebaseAuthenticationToken(principal, List.of()));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Assigned receiver can subscribe to pickup live tracking")
    void testSubscribeTracking_assignedReceiver_succeeds() {
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(testPickup));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/pickups/" + pickupId + "/tracking");
        FirebasePrincipal principal = new FirebasePrincipal("receiver-uid", "receiver@mealshare.org", "Receiver", Map.of());
        accessor.setUser(new FirebaseAuthenticationToken(principal, List.of()));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertThat(result).isNotNull();
    }

    // ─── STOMP SEND TESTS (Publishing Location) ───

    @Test
    @DisplayName("Non-receiver cannot publish live GPS location update")
    void testPublishLocation_nonReceiver_throwsAccessDenied() {
        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/pickups/" + pickupId + "/location");
        FirebasePrincipal principal = new FirebasePrincipal("donor-uid", "donor@mealshare.org", "Donor", Map.of());
        accessor.setUser(new FirebaseAuthenticationToken(principal, List.of()));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only authorized receivers can publish location updates");
    }

    @Test
    @DisplayName("Receiver cannot publish location if tracking is inactive")
    void testPublishLocation_trackingInactive_throwsIllegalState() {
        testPickup.setTrackingActive(false);
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(testPickup));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/pickups/" + pickupId + "/location");
        FirebasePrincipal principal = new FirebasePrincipal("receiver-uid", "receiver@mealshare.org", "Receiver", Map.of());
        accessor.setUser(new FirebaseAuthenticationToken(principal, List.of()));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Location tracking is inactive for this pickup");
    }

    @Test
    @DisplayName("Assigned receiver can publish location during active tracking")
    void testPublishLocation_activeTracking_succeeds() {
        testPickup.setTrackingActive(true);
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(testPickup));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/pickups/" + pickupId + "/location");
        FirebasePrincipal principal = new FirebasePrincipal("receiver-uid", "receiver@mealshare.org", "Receiver", Map.of());
        accessor.setUser(new FirebaseAuthenticationToken(principal, List.of()));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertThat(result).isNotNull();
    }
}

package com.mealshare.pickup.tracking;

import com.mealshare.audit.AuditService;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.pickup.dto.FoodInspectionDto;
import com.mealshare.pickup.dto.LocationUpdateDto;
import com.mealshare.pickup.otp.PickupOtpService;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupTrackingSessionServiceTest {

    private InMemoryRealtimeTrackingGateway inMemoryGateway;
    private DefaultPickupTrackingSessionService trackingSessionService;

    @Mock
    private PickupRepository pickupRepository;

    @Mock
    private DonationRequestRepository donationRequestRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private ReceiverOrganizationRepository receiverOrganizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PickupOtpService pickupOtpService;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private AuditService auditService;

    private PickupService pickupService;

    private User donorUser;
    private DonorProfile donorProfile;
    private Donation donation;

    private User receiverUser;
    private ReceiverOrganization receiverOrg;

    private DonationRequest donationRequest;
    private Pickup pickup;
    private UUID pickupId;

    @BeforeEach
    void setUp() {
        inMemoryGateway = new InMemoryRealtimeTrackingGateway();
        trackingSessionService = new DefaultPickupTrackingSessionService(inMemoryGateway);

        pickupId = UUID.randomUUID();

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid-123");
        donorUser.setEmail("donor@mealshare.org");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);
        donorUser.setName("Community Kitchen");

        donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);
        donorProfile.setEntityName("Community Kitchen");

        donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setDonor(donorProfile);
        donation.setStatus(DonationStatus.APPROVED);
        donation.setLatitude(12.9716);
        donation.setLongitude(77.5946);
        donation.setPickupAddress("123 MG Road, Bengaluru");

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid-456");
        receiverUser.setEmail("receiver@mealshare.org");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        receiverOrg = new ReceiverOrganization();
        receiverOrg.setId(UUID.randomUUID());
        receiverOrg.setUser(receiverUser);
        receiverOrg.setOrgName("Helping Hands Foundation");

        donationRequest = new DonationRequest();
        donationRequest.setId(UUID.randomUUID());
        donationRequest.setDonation(donation);
        donationRequest.setReceiverOrganization(receiverOrg);
        donationRequest.setStatus(DonationRequestStatus.APPROVED);

        pickup = new Pickup();
        pickup.setId(pickupId);
        pickup.setDonationRequest(donationRequest);
        pickup.setReceiver(receiverOrg);
        pickup.setStatus(PickupStatus.EN_ROUTE_TO_PICKUP);
        pickup.setTrackingActive(true);
        pickup.setCurrentLatitude(12.9352);
        pickup.setCurrentLongitude(77.6245);

        pickupService = new PickupService(
                pickupRepository,
                donationRequestRepository,
                donationRepository,
                receiverOrganizationRepository,
                donorProfileRepository,
                userRepository,
                pickupOtpService,
                auditService
        );
        pickupService.setTrackingSessionService(trackingSessionService);
    }

    // -------------------------------------------------------------
    // Lifecycle Activation Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("activateSession creates active RTDB session with valid donor and receiver UIDs")
    void testActivateSession_Success() {
        trackingSessionService.activateSession(pickup);

        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();

        Map<String, Object> session = sessionOpt.get();
        assertThat(session.get("pickupId")).isEqualTo(pickupId.toString());
        assertThat(session.get("donorUid")).isEqualTo("donor-uid-123");
        assertThat(session.get("receiverUid")).isEqualTo("receiver-uid-456");
        assertThat(session.get("status")).isEqualTo("EN_ROUTE_TO_PICKUP");
        assertThat(session.get("trackingActive")).isEqualTo(true);
        assertThat(session.get("activatedAt")).isNotNull();
        assertThat(session.get("lastUpdatedAt")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> donorLoc = (Map<String, Object>) session.get("donorLocation");
        assertThat(donorLoc).isNotNull();
        assertThat(donorLoc.get("latitude")).isEqualTo(12.9716);
        assertThat(donorLoc.get("longitude")).isEqualTo(77.5946);
        assertThat(donorLoc.get("address")).isEqualTo("123 MG Road, Bengaluru");

        @SuppressWarnings("unchecked")
        Map<String, Object> receiverLoc = (Map<String, Object>) session.get("receiverLocation");
        assertThat(receiverLoc).isNotNull();
        assertThat(receiverLoc.get("latitude")).isEqualTo(12.9352);
        assertThat(receiverLoc.get("longitude")).isEqualTo(77.6245);

        assertThat(trackingSessionService.isSessionActive(pickupId)).isTrue();
    }

    @Test
    @DisplayName("activateSession throws IllegalStateException when status is not EN_ROUTE_TO_PICKUP")
    void testActivateSession_InvalidStatus_ThrowsException() {
        pickup.setStatus(PickupStatus.PICKUP_PENDING);

        assertThatThrownBy(() -> trackingSessionService.activateSession(pickup))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot activate tracking session for pickup in status: PICKUP_PENDING");
    }

    @Test
    @DisplayName("activateSession throws IllegalStateException when trackingActive is false")
    void testActivateSession_TrackingNotActive_ThrowsException() {
        pickup.setTrackingActive(false);

        assertThatThrownBy(() -> trackingSessionService.activateSession(pickup))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot activate tracking session when pickup trackingActive flag is false");
    }

    @Test
    @DisplayName("activateSession throws IllegalStateException when donor UID is missing")
    void testActivateSession_MissingDonorUid_ThrowsException() {
        donorUser.setFirebaseUid(null);

        assertThatThrownBy(() -> trackingSessionService.activateSession(pickup))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Donor Firebase UID is missing");
    }

    @Test
    @DisplayName("activateSession throws IllegalStateException when receiver UID is missing")
    void testActivateSession_MissingReceiverUid_ThrowsException() {
        receiverUser.setFirebaseUid("");

        assertThatThrownBy(() -> trackingSessionService.activateSession(pickup))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Receiver Firebase UID is missing");
    }

    @Test
    @DisplayName("activateSession throws IllegalArgumentException when pickup is null or has no ID")
    void testActivateSession_NullPickup_ThrowsException() {
        assertThatThrownBy(() -> trackingSessionService.activateSession(null))
                .isInstanceOf(IllegalArgumentException.class);

        pickup.setId(null);
        assertThatThrownBy(() -> trackingSessionService.activateSession(pickup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------
    // Lifecycle Deactivation Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("deactivateSession sets trackingActive=false and updates terminal status AT_PICKUP")
    void testDeactivateSession_AtPickup_Success() {
        trackingSessionService.activateSession(pickup);
        assertThat(trackingSessionService.isSessionActive(pickupId)).isTrue();

        trackingSessionService.deactivateSession(pickupId, PickupStatus.AT_PICKUP);

        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();
        Map<String, Object> session = sessionOpt.get();
        assertThat(session.get("trackingActive")).isEqualTo(false);
        assertThat(session.get("status")).isEqualTo("AT_PICKUP");
        assertThat(session.get("deactivatedAt")).isNotNull();

        assertThat(trackingSessionService.isSessionActive(pickupId)).isFalse();
    }

    @Test
    @DisplayName("deactivateSession sets trackingActive=false on CANCELLED")
    void testDeactivateSession_Cancelled_Success() {
        trackingSessionService.activateSession(pickup);

        trackingSessionService.deactivateSession(pickupId, PickupStatus.CANCELLED);

        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();
        Map<String, Object> session = sessionOpt.get();
        assertThat(session.get("trackingActive")).isEqualTo(false);
        assertThat(session.get("status")).isEqualTo("CANCELLED");
        assertThat(trackingSessionService.isSessionActive(pickupId)).isFalse();
    }

    @Test
    @DisplayName("deactivateSession throws IllegalArgumentException when terminalStatus is invalid")
    void testDeactivateSession_InvalidTerminalStatus_ThrowsException() {
        assertThatThrownBy(() -> trackingSessionService.deactivateSession(pickupId, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> trackingSessionService.deactivateSession(pickupId, PickupStatus.EN_ROUTE_TO_PICKUP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an inactive/terminal status");
    }

    @Test
    @DisplayName("cleanupSession removes session from RTDB")
    void testCleanupSession_DeletesSession() {
        trackingSessionService.activateSession(pickup);
        assertThat(trackingSessionService.getSession(pickupId)).isPresent();

        trackingSessionService.cleanupSession(pickupId);

        assertThat(trackingSessionService.getSession(pickupId)).isEmpty();
    }

    // -------------------------------------------------------------
    // PickupService Integration with Tracking Session Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("startPickupJourney activates RTDB tracking session")
    void testStartPickupJourney_ActivatesTrackingSession() {
        pickup.setStatus(PickupStatus.PICKUP_PENDING);
        pickup.setTrackingActive(false);

        when(userRepository.findByFirebaseUid("receiver-uid-456")).thenReturn(Optional.of(receiverUser));
        when(receiverOrganizationRepository.findByUserId(receiverUser.getId())).thenReturn(Optional.of(receiverOrg));
        when(pickupRepository.findByIdWithLock(pickupId)).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationUpdateDto startLocation = new LocationUpdateDto(12.9352, 77.6245, 0.0, 0.0);

        pickupService.startPickupJourney("receiver-uid-456", pickupId, startLocation, null);

        assertThat(trackingSessionService.isSessionActive(pickupId)).isTrue();
        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().get("status")).isEqualTo("EN_ROUTE_TO_PICKUP");
        assertThat(sessionOpt.get().get("donorUid")).isEqualTo("donor-uid-123");
        assertThat(sessionOpt.get().get("receiverUid")).isEqualTo("receiver-uid-456");
    }

    @Test
    @DisplayName("confirmReachedLocation deactivates RTDB tracking session with AT_PICKUP")
    void testConfirmReachedLocation_DeactivatesTrackingSession() {
        trackingSessionService.activateSession(pickup);
        assertThat(trackingSessionService.isSessionActive(pickupId)).isTrue();

        pickup.setStatus(PickupStatus.EN_ROUTE_TO_PICKUP);
        when(userRepository.findByFirebaseUid("receiver-uid-456")).thenReturn(Optional.of(receiverUser));
        when(receiverOrganizationRepository.findByUserId(receiverUser.getId())).thenReturn(Optional.of(receiverOrg));
        when(pickupRepository.findByIdWithLock(pickupId)).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationUpdateDto reachedLocation = new LocationUpdateDto(12.9716, 77.5946, 0.0, 0.0);

        pickupService.confirmReachedLocation("receiver-uid-456", pickupId, reachedLocation, null);

        assertThat(trackingSessionService.isSessionActive(pickupId)).isFalse();
        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().get("status")).isEqualTo("AT_PICKUP");
        assertThat(sessionOpt.get().get("trackingActive")).isEqualTo(false);
    }

    @Test
    @DisplayName("cancelPickup by receiver deactivates RTDB tracking session with CANCELLED")
    void testCancelPickup_Receiver_DeactivatesTrackingSession() {
        trackingSessionService.activateSession(pickup);
        assertThat(trackingSessionService.isSessionActive(pickupId)).isTrue();

        when(userRepository.findByFirebaseUid("receiver-uid-456")).thenReturn(Optional.of(receiverUser));
        when(pickupRepository.findByIdWithLock(pickupId)).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(inv -> inv.getArgument(0));

        pickupService.cancelPickup("receiver-uid-456", pickupId, "Vehicle breakdown", null);

        assertThat(pickup.getStatus()).isEqualTo(PickupStatus.CANCELLED);
        assertThat(pickup.getTrackingActive()).isFalse();
        assertThat(trackingSessionService.isSessionActive(pickupId)).isFalse();

        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().get("status")).isEqualTo("CANCELLED");
        assertThat(sessionOpt.get().get("trackingActive")).isEqualTo(false);
    }

    @Test
    @DisplayName("cancelPickup by unauthorized user throws AccessDeniedException and does not cancel session")
    void testCancelPickup_UnauthorizedUser_ThrowsAccessDenied() {
        trackingSessionService.activateSession(pickup);

        User stranger = new User();
        stranger.setId(UUID.randomUUID());
        stranger.setFirebaseUid("stranger-uid-999");
        stranger.setRole(UserRole.RECEIVER);

        when(userRepository.findByFirebaseUid("stranger-uid-999")).thenReturn(Optional.of(stranger));
        when(pickupRepository.findByIdWithLock(pickupId)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> pickupService.cancelPickup("stranger-uid-999", pickupId, "Attempting hijack", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to cancel this pickup");

        // Session must remain intact and active
        assertThat(trackingSessionService.isSessionActive(pickupId)).isTrue();
    }

    @Test
    @DisplayName("inspectFood rejected deactivates RTDB tracking session with CANCELLED")
    void testInspectFood_Rejected_DeactivatesTrackingSession() {
        trackingSessionService.activateSession(pickup);
        pickup.setStatus(PickupStatus.AT_PICKUP);

        when(userRepository.findByFirebaseUid("receiver-uid-456")).thenReturn(Optional.of(receiverUser));
        when(receiverOrganizationRepository.findByUserId(receiverUser.getId())).thenReturn(Optional.of(receiverOrg));
        when(pickupRepository.findByIdWithLock(pickupId)).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(inv -> inv.getArgument(0));

        FoodInspectionDto inspectionDto = new FoodInspectionDto(false, "Food spoilage detected", "Food spoilage detected", null);

        pickupService.inspectFood("receiver-uid-456", pickupId, inspectionDto, null);

        assertThat(pickup.getStatus()).isEqualTo(PickupStatus.CANCELLED);
        assertThat(pickup.getTrackingActive()).isFalse();
        assertThat(trackingSessionService.isSessionActive(pickupId)).isFalse();

        Optional<Map<String, Object>> sessionOpt = trackingSessionService.getSession(pickupId);
        assertThat(sessionOpt).isPresent();
        assertThat(sessionOpt.get().get("status")).isEqualTo("CANCELLED");
        assertThat(sessionOpt.get().get("trackingActive")).isEqualTo(false);
    }
}

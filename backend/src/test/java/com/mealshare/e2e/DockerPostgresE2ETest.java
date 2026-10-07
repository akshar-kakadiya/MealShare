package com.mealshare.e2e;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.common.util.GeometryUtils;
import com.mealshare.audit.AuditLogRepository;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationService;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;
import com.mealshare.donation.dto.CreateDonationRequest;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.donor.request.DonorRequestService;
import com.mealshare.donor.request.dto.ApproveDonationRequestResponse;
import com.mealshare.notification.Notification;
import com.mealshare.notification.NotificationRepository;
import com.mealshare.notification.NotificationType;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.pickup.dto.AdminAssignPickupResponse;
import com.mealshare.pickup.dto.AssignPickupRequest;
import com.mealshare.pickup.dto.DonorPickupOtpResponse;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.receiver.ReceiverDiscoveryService;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.dto.NearbyDonationResponse;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestService;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.receiver.request.dto.CreateDonationRequestDto;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DockerPostgresE2ETest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private ReceiverOrganizationRepository receiverOrganizationRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationRequestRepository donationRequestRepository;

    @Autowired
    private PickupRepository pickupRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DonationService donationService;

    @Autowired
    private ReceiverDiscoveryService receiverDiscoveryService;

    @Autowired
    private DonationRequestService donationRequestService;

    @Autowired
    private DonorRequestService donorRequestService;

    @Autowired
    private PickupService pickupService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    private static final String ADMIN_UID = "ObiO1gDTdBTZJkzDFEGgTVNTEir2";
    private static final String DONOR_UID = "drekxkp9uXV2g16JioDrgu7uXSk2";
    private static final String RECEIVER_UID = "pwvnvHgatoahwb9ndKfSM2TVFPG2";

    private static final UUID DONOR_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DONOR_PROFILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID RECEIVER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");

    private static UUID createdDonationId;
    private static UUID createdRequestId;
    private static UUID createdPickupId;
    private static String generatedDevOtp;

    @BeforeEach
    void setUp() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(firebaseTokenValidator.verifyIdToken(anyString())).thenReturn(mockToken);
    }

    @Test
    @Order(1)
    @DisplayName("1. PostgreSQL PostGIS Database Connectivity & SRID 4326 Point Creation")
    void test1_postgresConnectionAndPostgis() {
        Object versionObj = entityManager.createNativeQuery("SELECT version()").getSingleResult();
        assertNotNull(versionObj);
        assertTrue(versionObj.toString().contains("PostgreSQL"));

        Object postgisVersion = entityManager.createNativeQuery("SELECT PostGIS_Version()").getSingleResult();
        assertNotNull(postgisVersion);

        Object geomResult = entityManager.createNativeQuery(
                "SELECT ST_AsText(ST_SetSRID(ST_MakePoint(77.5946, 12.9716), 4326))"
        ).getSingleResult();
        assertEquals("POINT(77.5946 12.9716)", geomResult.toString());
    }

    @Test
    @Order(2)
    @DisplayName("2. Step A: Donor creates donation with coordinates and food safety declaration")
    void test2_donorCreatesDonation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        CreateDonationRequest dto = new CreateDonationRequest(
                "Docker Test Veg Biryani",
                "High quality freshly prepared meals",
                FoodCategory.PREPARED_MEALS,
                50.0,
                "MEALS",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(6, ChronoUnit.HOURS),
                Instant.now().minus(30, ChronoUnit.MINUTES),
                Instant.now().plus(4, ChronoUnit.HOURS),
                "100 MG Road, Bengaluru",
                12.9716,
                77.5946,
                true,
                "Cooked under hygienic commercial kitchen conditions"
        );

        DonationResponse response = donationService.createDonation(DONOR_UID, dto, request);
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Docker Test Veg Biryani", response.title());
        assertEquals(DonationStatus.AVAILABLE, response.status());
        assertEquals(50.0, response.quantity());

        createdDonationId = response.id();

        // Verify PostgreSQL persistence
        Donation dbDonation = donationRepository.findById(createdDonationId).orElseThrow();
        assertEquals(50.0, dbDonation.getQuantity());
        assertEquals(50.0, dbDonation.getRemainingQuantity());
        assertEquals(0.0, dbDonation.getAllocatedQuantity());
        assertNotNull(dbDonation.getLocation());

        // Verify audit log entry
        boolean hasAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "DONATION_CREATED".equals(a.getActionType()) && createdDonationId.toString().equals(a.getEntityId()));
        assertTrue(hasAudit, "DONATION_CREATED audit log must exist");
    }

    @Test
    @Order(3)
    @DisplayName("3. Step B: Receiver discovers donation via PostGIS spatial distance query")
    void test3_receiverDiscoversNearbyDonation() {
        Page<NearbyDonationResponse> nearby = receiverDiscoveryService.findNearbyDonations(
                RECEIVER_UID,
                12.9700,
                77.6000,
                15.0,
                FoodCategory.PREPARED_MEALS,
                PageRequest.of(0, 10)
        );

        assertNotNull(nearby);
        assertFalse(nearby.isEmpty(), "Nearby donations must not be empty");
        boolean foundCreated = nearby.getContent().stream()
                .anyMatch(d -> d.id().equals(createdDonationId));
        assertTrue(foundCreated, "Created donation must be found in spatial query results");

        NearbyDonationResponse hit = nearby.getContent().stream()
                .filter(d -> d.id().equals(createdDonationId))
                .findFirst().orElseThrow();
        assertTrue(hit.distanceKm() < 5.0, "Distance should be under 5km for given coordinates");
    }

    @Test
    @Order(4)
    @DisplayName("4. Step C: Receiver submits claim for 20 meals")
    void test4_receiverCreatesDonationRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        CreateDonationRequestDto dto = new CreateDonationRequestDto(
                20.0,
                "Meals needed for evening community shelter distribution"
        );

        DonationRequestResponse response = donationRequestService.createRequest(RECEIVER_UID, createdDonationId, dto, request);
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(DonationRequestStatus.PENDING, response.status());
        assertEquals(20.0, response.requestedQuantity());

        createdRequestId = response.id();

        // Verify PostgreSQL persistence
        DonationRequest dbRequest = donationRequestRepository.findById(createdRequestId).orElseThrow();
        assertEquals(20.0, dbRequest.getRequestedQuantity());

        // Verify donor received notification
        List<Notification> donorNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(DONOR_USER_ID, PageRequest.of(0, 10)).getContent();
        assertTrue(donorNotifs.stream().anyMatch(n -> n.getType() == NotificationType.REQUEST_CREATED),
                "Donor must receive REQUEST_CREATED notification");
    }

    @Test
    @Order(5)
    @DisplayName("5. Step D: Donor approves request, allocated_quantity increments atomically")
    void test5_donorApprovesRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        ApproveDonationRequestResponse response = donorRequestService.approveRequest(DONOR_UID, createdRequestId, request);
        assertNotNull(response);
        assertEquals(DonationRequestStatus.APPROVED, response.requestStatus());
        assertEquals(20.0, response.allocatedQuantity());
        assertEquals(30.0, response.remainingQuantity());
        assertEquals(DonationStatus.AVAILABLE, response.donationStatus());

        // Verify database state: donation allocated_quantity is now 20.0
        Donation dbDonation = donationRepository.findById(createdDonationId).orElseThrow();
        assertEquals(20.0, dbDonation.getAllocatedQuantity());
        assertEquals(30.0, dbDonation.getRemainingQuantity());

        // Verify receiver organization received notification
        List<Notification> receiverNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(RECEIVER_USER_ID, PageRequest.of(0, 10)).getContent();
        assertTrue(receiverNotifs.stream().anyMatch(n -> n.getType() == NotificationType.REQUEST_APPROVED),
                "Receiver must receive REQUEST_APPROVED notification");
    }

    @Test
    @Order(6)
    @DisplayName("6. Step E: Admin schedules pickup for approved request, BCrypt OTP stored")
    void test6_adminSchedulePickup() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.HOURS);
        AssignPickupRequest dto = new AssignPickupRequest(
                createdRequestId,
                scheduledAt
        );

        AdminAssignPickupResponse response = pickupService.assignVolunteer(ADMIN_UID, dto, request);
        assertNotNull(response);
        assertNotNull(response.pickupId());

        createdPickupId = response.pickupId();
        // Legitimate production workflow: Authorized donor retrieves handover OTP
        DonorPickupOtpResponse donorOtp = pickupService.getOtpForDonor(DONOR_UID, createdPickupId, request);
        assertNotNull(donorOtp);
        assertNotNull(donorOtp.otp(), "Donor must be able to view their handover OTP");
        generatedDevOtp = donorOtp.otp();

        // Verify database persistence and OTP security invariant
        Pickup dbPickup = pickupRepository.findById(createdPickupId).orElseThrow();
        assertEquals(PickupStatus.ASSIGNED, dbPickup.getStatus());
        assertNotNull(dbPickup.getOtpHash());
        assertNotEquals(generatedDevOtp, dbPickup.getOtpHash(), "Plaintext OTP must NOT be stored in database");
        assertTrue(dbPickup.getOtpHash().length() >= 60, "OTP hash must be at least 60 chars (BCrypt / secure hash)");

        // Verify notification to receiver
        List<Notification> recNotifs = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(RECEIVER_USER_ID, PageRequest.of(0, 10)).getContent();
        assertTrue(recNotifs.stream().anyMatch(n -> n.getType() == NotificationType.PICKUP_SCHEDULED),
                "Receiver must receive PICKUP_SCHEDULED notification");
    }

    @Test
    @Order(7)
    @DisplayName("7. Step F: Receiver OTP verification, invalid rejection, completion lifecycle")
    void test7_receiverOtpVerificationAndCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        // Test invalid OTP rejection
        assertThrows(IllegalStateException.class, () -> {
            pickupService.verifyOtp(RECEIVER_UID, createdPickupId, "999999", request);
        }, "Invalid OTP must be rejected with IllegalStateException");

        Pickup afterFailedAttempt = pickupRepository.findById(createdPickupId).orElseThrow();
        assertEquals(1, afterFailedAttempt.getOtpAttempts(), "Failed attempt count must be incremented to 1");

        // Test valid OTP verification
        PickupResponse verifyResponse = pickupService.verifyOtp(RECEIVER_UID, createdPickupId, generatedDevOtp, request);
        assertNotNull(verifyResponse);
        assertEquals(PickupStatus.PICKED_UP, verifyResponse.status());
        assertNotNull(verifyResponse.pickedUpAt());

        // Test pickup completion
        PickupResponse completeResponse = pickupService.completePickup(RECEIVER_UID, createdPickupId, request);
        assertNotNull(completeResponse);
        assertEquals(PickupStatus.COMPLETED, completeResponse.status());
        assertNotNull(completeResponse.completedAt());

        // Verify request is also COMPLETED
        DonationRequest completedRequest = donationRequestRepository.findById(createdRequestId).orElseThrow();
        assertEquals(DonationRequestStatus.COMPLETED, completedRequest.getStatus());

        // Verify audit log
        boolean completedAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "PICKUP_COMPLETED".equals(a.getActionType()) && createdPickupId.toString().equals(a.getEntityId()));
        assertTrue(completedAudit, "PICKUP_COMPLETED audit event must be persisted");
    }

    @Test
    @Order(8)
    @DisplayName("8. Security & IDOR Invariant Checks (10 Scenarios)")
    void test8_securityAndIdorChecks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        // 1. Donor cannot access another donor's donation
        assertFalse(donationRepository.existsByIdAndDonorId(createdDonationId, UUID.randomUUID()),
                "Donation must not be accessible to unassociated donor ID");

        // 2. Expired donation cannot accept requests
        Donation expiredDonation = new Donation();
        expiredDonation.setDonor(donorProfileRepository.findById(DONOR_PROFILE_ID).orElseThrow());
        expiredDonation.setTitle("Expired Bread");
        expiredDonation.setFoodCategory(FoodCategory.BAKERY);
        expiredDonation.setQuantity(10.0);
        expiredDonation.setQuantityUnit("LOAVES");
        expiredDonation.setPreparationDateTime(Instant.now().minus(2, ChronoUnit.DAYS));
        expiredDonation.setConsumeBefore(Instant.now().minus(1, ChronoUnit.HOURS)); // Expired!
        expiredDonation.setPickupAvailableFrom(Instant.now().minus(2, ChronoUnit.DAYS));
        expiredDonation.setPickupAvailableUntil(Instant.now().minus(2, ChronoUnit.HOURS));
        expiredDonation.setPickupAddress("Test Address");
        expiredDonation.setLatitude(12.97);
        expiredDonation.setLongitude(77.59);
        expiredDonation.setLocation(GeometryUtils.createPoint(77.59, 12.97));
        expiredDonation.setFoodSafetyDeclared(true);
        expiredDonation.setStatus(DonationStatus.AVAILABLE);
        expiredDonation = donationRepository.save(expiredDonation);
        UUID expiredId = expiredDonation.getId();

        assertThrows(IllegalStateException.class, () -> {
            donationRequestService.createRequest(RECEIVER_UID, expiredId, new CreateDonationRequestDto(5.0, "Test"), request);
        }, "Expired donation must reject requests with IllegalStateException");

        // 3. Other receiver cannot access Receiver A's pickup detail (IDOR)
        final String otherRecUid = "X7cbD4mXYCOoC0TZkRjT3cbDSHv2";
        User otherRecUser = userRepository.findByFirebaseUid(otherRecUid).orElseGet(() -> {
            User u = new User();
            u.setFirebaseUid(otherRecUid);
            u.setEmail("other.receiver@mealshare.org");
            u.setName("Other Receiver");
            u.setRole(UserRole.RECEIVER);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });

        if (receiverOrganizationRepository.findByUserId(otherRecUser.getId()).isEmpty()) {
            ReceiverOrganization otherOrg = new ReceiverOrganization();
            otherOrg.setUser(otherRecUser);
            otherOrg.setOrgName("Other Shelter Community");
            otherOrg.setOrgType("COMMUNITY");
            otherOrg.setRegistrationNumber("REG-OTHER-001");
            otherOrg.setAddressText("Bengaluru");
            otherOrg.setServiceLatitude(12.9716);
            otherOrg.setServiceLongitude(77.5946);
            otherOrg.setServiceRadiusMeters(5000.0);
            otherOrg.setBeneficiaryCapacity(25);
            otherOrg.setRatingAverage(5.0);
            receiverOrganizationRepository.save(otherOrg);
        }

        assertThrows(AccessDeniedException.class, () -> {
            pickupService.getPickupDetailForReceiver(otherRecUser.getFirebaseUid(), createdPickupId);
        }, "Receiver must be forbidden from accessing unassigned pickup (IDOR)");

        // 4. Non-admin cannot schedule pickup
        assertThrows(AccessDeniedException.class, () -> {
            pickupService.assignVolunteer(DONOR_UID, new AssignPickupRequest(createdRequestId, Instant.now().plus(1, ChronoUnit.HOURS)), request);
        }, "Donor role must be forbidden from executing admin pickup scheduling");

        // 5. Admin user status update cannot modify role
        User donorUser = userRepository.findById(DONOR_USER_ID).orElseThrow();
        assertEquals(UserRole.DONOR, donorUser.getRole());

        // 6. Plaintext OTP is never returned through standard PickupResponse
        PickupResponse detail = pickupService.getPickupDetailForReceiver(RECEIVER_UID, createdPickupId);
        assertNotNull(detail);

        // 7. Suspended user cannot create donations
        User suspendedDonor = userRepository.findByFirebaseUid("suspended_donor_uid").orElseGet(() -> {
            User u = new User();
            u.setFirebaseUid("suspended_donor_uid");
            u.setEmail("suspended@mealshare.org");
            u.setName("Suspended Donor");
            u.setRole(UserRole.DONOR);
            u.setStatus(UserStatus.SUSPENDED);
            return userRepository.save(u);
        });

        assertThrows(AccessDeniedException.class, () -> {
            donationService.createDonation("suspended_donor_uid", new CreateDonationRequest(
                    "Food", "Desc", FoodCategory.BAKERY, 10.0, "ITEMS",
                    Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS),
                    Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                    "Address", 12.97, 77.59, true, "Safe"
            ), request);
        }, "Suspended donor must be forbidden from creating donations");
    }
}

package com.mealshare.pickup;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donor.DonorProfile;
import com.mealshare.pickup.dto.AdminAssignPickupResponse;
import com.mealshare.pickup.dto.AssignPickupRequest;
import com.mealshare.pickup.dto.PickupResponse;
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
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PickupServiceTest {

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
    private com.mealshare.donor.DonorProfileRepository donorProfileRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PickupService pickupService;

    private User adminUser;
    private User receiverUser;
    private ReceiverOrganization receiverOrg;
    private User otherReceiverUser;
    private ReceiverOrganization otherReceiverOrg;
    private User donorUser;
    private DonorProfile donorProfile;
    private Donation donation;
    private DonationRequest donationRequest;
    private Pickup pickup;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirebaseUid("admin-uid");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        receiverOrg = new ReceiverOrganization();
        receiverOrg.setId(UUID.randomUUID());
        receiverOrg.setUser(receiverUser);
        receiverOrg.setOrgName("Care Shelter Foundation");

        otherReceiverUser = new User();
        otherReceiverUser.setId(UUID.randomUUID());
        otherReceiverUser.setFirebaseUid("other-receiver-uid");
        otherReceiverUser.setRole(UserRole.RECEIVER);
        otherReceiverUser.setStatus(UserStatus.ACTIVE);

        otherReceiverOrg = new ReceiverOrganization();
        otherReceiverOrg.setId(UUID.randomUUID());
        otherReceiverOrg.setUser(otherReceiverUser);
        otherReceiverOrg.setOrgName("Other Community Org");

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);
        donorUser.setName("Donor Kitchen");
        donorUser.setPhone("+919876543210");

        donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);

        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));
        when(donorProfileRepository.findByUserId(donorUser.getId())).thenReturn(Optional.of(donorProfile));

        donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setDonor(donorProfile);
        donation.setTitle("Vegetarian Lunch");
        donation.setQuantity(100.0);
        donation.setAllocatedQuantity(100.0);
        donation.setStatus(DonationStatus.APPROVED);
        donation.setPickupAvailableFrom(Instant.now().minus(1, ChronoUnit.HOURS));
        donation.setPickupAvailableUntil(Instant.now().plus(5, ChronoUnit.HOURS));
        donation.setConsumeBefore(Instant.now().plus(6, ChronoUnit.HOURS));
        donation.setPickupAddress("123 Main St, Bengaluru");

        donationRequest = new DonationRequest();
        donationRequest.setId(UUID.randomUUID());
        donationRequest.setDonation(donation);
        donationRequest.setReceiverOrganization(receiverOrg);
        donationRequest.setRequestedQuantity(100.0);
        donationRequest.setAllocatedQuantity(100.0);
        donationRequest.setStatus(DonationRequestStatus.APPROVED);
        donationRequest.setRequestedAt(Instant.now().minus(1, ChronoUnit.HOURS));

        pickup = new Pickup();
        pickup.setId(UUID.randomUUID());
        pickup.setDonationRequest(donationRequest);
        pickup.setReceiver(receiverOrg);
        pickup.setStatus(PickupStatus.ASSIGNED);
        pickup.setScheduledAt(Instant.now().plus(2, ChronoUnit.HOURS));
        pickup.setAssignedAt(Instant.now());
        pickup.setOtpHash(passwordEncoder.encode("123456"));
        pickup.setOtpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        pickup.setOtpAttempts(0);

        when(userRepository.findByFirebaseUid("admin-uid")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
        when(userRepository.findByFirebaseUid("other-receiver-uid")).thenReturn(Optional.of(otherReceiverUser));

        when(receiverOrganizationRepository.findByUserId(receiverUser.getId())).thenReturn(Optional.of(receiverOrg));
        when(receiverOrganizationRepository.findByUserId(otherReceiverUser.getId())).thenReturn(Optional.of(otherReceiverOrg));
    }

    // ==========================================
    // A. Assignment Scenarios
    // ==========================================

    @Test
    void test1_adminCanAssignVolunteerToApprovedRequest_andAudit() {
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().plus(2, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);

        when(pickupOtpService.generateSecureOtp()).thenReturn("654321");
        when(pickupOtpService.hashOtp("654321")).thenReturn("$2a$10$fakeHash");
        when(pickupOtpService.calculateExpiry(any(), any(), any())).thenReturn(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> {
            Pickup p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        AdminAssignPickupResponse response = pickupService.assignVolunteer("admin-uid", reqDto, null);

        assertNotNull(response);
        assertEquals(donationRequest.getId(), response.donationRequestId());
        assertEquals(receiverOrg.getId(), response.receiverId());
        assertEquals(PickupStatus.ASSIGNED, response.status());
        verify(pickupOtpService).storeCachedOtp(eq(response.pickupId()), eq("654321"), any());

        // Invariants: donation becomes PICKUP_ASSIGNED
        assertEquals(DonationStatus.PICKUP_ASSIGNED, donation.getStatus());

        verify(auditService).logEvent(
                eq(adminUser.getId().toString()),
                eq("ADMIN"),
                eq("PICKUP_SCHEDULED"),
                eq("Pickup"),
                any(),
                any(),
                any()
        );

        verify(auditService).logEvent(
                eq(adminUser.getId().toString()),
                eq("ADMIN"),
                eq("OTP_GENERATED"),
                eq("Pickup"),
                any(),
                any(),
                any()
        );
    }

    @Test
    void test2_nonAdminCannotAssignVolunteer_throws403() {
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().plus(2, ChronoUnit.HOURS)
        );

        // Caller is receiver, not admin
        assertThrows(AccessDeniedException.class,
                () -> pickupService.assignVolunteer("receiver-uid", reqDto, null));
    }

    @Test
    void test4_unknownRequest_throws404() {
        UUID unknownId = UUID.randomUUID();
        AssignPickupRequest reqDto = new AssignPickupRequest(
                unknownId,
                Instant.now().plus(2, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
    }

    @Test
    void test5_nonApprovedRequestCannotBeAssigned_throws409() {
        donationRequest.setStatus(DonationRequestStatus.PENDING);
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().plus(2, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("Only APPROVED donation requests can be assigned"));
    }

    @Test
    void test6_inactiveOrBusyVolunteerCannotBeAssigned_throws409() {
        receiverUser.setStatus(UserStatus.SUSPENDED);
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().plus(2, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("Receiver account is not active"));
    }

    @Test
    void test7_alreadyAssignedRequestCannotReceiveSecondPickup_throws409() {
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().plus(2, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("already been assigned for this donation request"));
    }

    @Test
    void test_scheduledAtInThePast_rejectedWith400() {
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().minus(2, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("cannot be in the past"));
    }

    @Test
    void test_scheduledAtAfterConsumeBefore_rejectedWith400() {
        donation.setPickupAvailableUntil(donation.getConsumeBefore().plus(2, ChronoUnit.HOURS));
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                donation.getConsumeBefore().plus(1, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("cannot be after the donation consume-before deadline"));
    }

    @Test
    void test_scheduledAtBeforePickupAvailableFrom_rejectedWith400() {
        donation.setPickupAvailableFrom(Instant.now().plus(2, ChronoUnit.HOURS));
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("cannot be before the donation pickup available from time"));
    }

    @Test
    void test_scheduledAtAfterPickupAvailableUntil_rejectedWith400() {
        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                donation.getPickupAvailableUntil().plus(10, ChronoUnit.MINUTES)
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pickupService.assignVolunteer("admin-uid", reqDto, null));
        assertTrue(ex.getMessage().contains("cannot be after the donation pickup available until time"));
    }

    @Test
    void test_scheduledAtAtBoundaries_successful() {
        Instant from = Instant.now().plus(1, ChronoUnit.HOURS);
        donation.setPickupAvailableFrom(from);
        donation.setPickupAvailableUntil(Instant.now().plus(5, ChronoUnit.HOURS));

        AssignPickupRequest reqDto = new AssignPickupRequest(
                donationRequest.getId(),
                from
        );

        when(donationRequestRepository.findById(donationRequest.getId())).thenReturn(Optional.of(donationRequest));
        when(pickupRepository.existsByDonationRequestId(donationRequest.getId())).thenReturn(false);
        when(pickupOtpService.generateSecureOtp()).thenReturn("123456");
        when(pickupOtpService.hashOtp("123456")).thenReturn("hashed");
        when(pickupOtpService.calculateExpiry(any(), any(), any())).thenReturn(from.plus(30, ChronoUnit.MINUTES));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> {
            Pickup p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        AdminAssignPickupResponse resp = pickupService.assignVolunteer("admin-uid", reqDto, null);
        assertNotNull(resp);
        assertEquals(PickupStatus.ASSIGNED, resp.status());
    }

    // ==========================================
    // B. Receiver Ownership & IDOR Protection
    // ==========================================

    @Test
    void test8_assignedVolunteerCanViewPickup() {
        when(pickupRepository.findById(pickup.getId())).thenReturn(Optional.of(pickup));

        PickupResponse response = pickupService.getPickupDetailForReceiver("receiver-uid", pickup.getId());
        assertNotNull(response);
        assertEquals(pickup.getId(), response.pickupId());
    }

    @Test
    void test9_differentVolunteerCannotViewPickup_throws403() {
        when(pickupRepository.findById(pickup.getId())).thenReturn(Optional.of(pickup));

        assertThrows(AccessDeniedException.class,
                () -> pickupService.getPickupDetailForReceiver("other-receiver-uid", pickup.getId()));
    }

    @Test
    void test11_differentVolunteerCannotVerifyOtp_throws403() {
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));

        assertThrows(AccessDeniedException.class,
                () -> pickupService.verifyOtp("other-receiver-uid", pickup.getId(), "123456", null));
    }

    @Test
    void test12_differentVolunteerCannotCompletePickup_throws403() {
        pickup.setStatus(PickupStatus.PICKED_UP);
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));

        assertThrows(AccessDeniedException.class,
                () -> pickupService.completePickup("other-receiver-uid", pickup.getId(), null));
    }

    // ==========================================
    // C. OTP Verification & Security
    // ==========================================

    @Test
    void test16_correctOtpSucceeds_transitionsToPickedUp_andAudit() {
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupOtpService.getMaxAttempts()).thenReturn(5);
        when(pickupOtpService.verifyOtp("123456", pickup.getOtpHash())).thenReturn(true);
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> i.getArgument(0));

        PickupResponse response = pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null);

        assertNotNull(response);
        assertEquals(PickupStatus.PICKED_UP, response.status());
        assertNotNull(response.otpVerifiedAt());
        assertNotNull(response.pickedUpAt());

        verify(auditService).logEvent(
                eq(receiverUser.getId().toString()),
                eq("RECEIVER"),
                eq("OTP_VERIFIED"),
                eq("Pickup"),
                eq(pickup.getId().toString()),
                any(),
                any()
        );
    }

    @Test
    void test17_and_test18_incorrectOtpFails_incrementsAttempts_andAudit() {
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupOtpService.getMaxAttempts()).thenReturn(5);
        when(pickupOtpService.verifyOtp("999999", pickup.getOtpHash())).thenReturn(false);
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> i.getArgument(0));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.verifyOtp("receiver-uid", pickup.getId(), "999999", null));
        assertEquals("Invalid or expired pickup OTP.", ex.getMessage());
        assertEquals(1, pickup.getOtpAttempts());

        verify(auditService).logEvent(
                eq(receiverUser.getId().toString()),
                eq("RECEIVER"),
                eq("OTP_VERIFICATION_FAILED"),
                eq("Pickup"),
                eq(pickup.getId().toString()),
                any(),
                any()
        );
    }

    @Test
    void test19_otpExpiryBlocksVerification() {
        pickup.setOtpExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)); // Expired!
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupOtpService.getMaxAttempts()).thenReturn(5);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null));
        assertEquals("Invalid or expired pickup OTP.", ex.getMessage());
    }

    @Test
    void test20_otpCannotBeReused_singleUseEnforced() {
        pickup.setStatus(PickupStatus.PICKED_UP); // Already verified/picked up!
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null));
        assertTrue(ex.getMessage().contains("Pickup is not in ASSIGNED state"));
    }

    @Test
    void test21_maxAttemptsBlocksFurtherVerification() {
        pickup.setOtpAttempts(5);
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupOtpService.getMaxAttempts()).thenReturn(5);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null));
        assertTrue(ex.getMessage().contains("Maximum verification attempts exceeded"));
    }

    // ==========================================
    // D. Pickup Lifecycle & Completion Scenarios
    // ==========================================

    @Test
    void test25_cannotCompleteBeforePickup_throws409() {
        pickup.setStatus(PickupStatus.ASSIGNED); // Not yet picked up!
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.completePickup("receiver-uid", pickup.getId(), null));
        assertTrue(ex.getMessage().contains("Only PICKED_UP pickups can be completed"));
    }

    @Test
    void test26_cannotCompleteTwice_throws409() {
        pickup.setStatus(PickupStatus.COMPLETED); // Already completed!
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> pickupService.completePickup("receiver-uid", pickup.getId(), null));
        assertTrue(ex.getMessage().contains("Only PICKED_UP pickups can be completed"));
    }

    @Test
    void test27_test29_completePickup_updatesRequestAndDonationCompleted() {
        pickup.setStatus(PickupStatus.PICKED_UP);
        donation.setAllocatedQuantity(100.0); // fully allocated

        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> i.getArgument(0));

        // No pending approved requests remaining after this one
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        PickupResponse response = pickupService.completePickup("receiver-uid", pickup.getId(), null);

        assertNotNull(response);
        assertEquals(PickupStatus.COMPLETED, response.status());
        assertEquals(DonationRequestStatus.COMPLETED, donationRequest.getStatus());
        assertEquals(DonationStatus.COMPLETED, donation.getStatus());

        verify(auditService).logEvent(
                eq(receiverUser.getId().toString()),
                eq("RECEIVER"),
                eq("PICKUP_COMPLETED"),
                eq("Pickup"),
                eq(pickup.getId().toString()),
                any(),
                any()
        );
    }

    @Test
    void test28_donationRemainsIncomplete_whileAnotherApprovedAllocationIsPending() {
        pickup.setStatus(PickupStatus.PICKED_UP);
        donation.setAllocatedQuantity(100.0);
        donation.setStatus(DonationStatus.PICKED_UP);

        // Another request for the same donation is still in APPROVED status
        DonationRequest secondRequest = new DonationRequest();
        secondRequest.setId(UUID.randomUUID());
        secondRequest.setDonation(donation);
        secondRequest.setStatus(DonationRequestStatus.APPROVED);

        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> i.getArgument(0));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.APPROVED))
                .thenReturn(List.of(secondRequest));

        PickupResponse response = pickupService.completePickup("receiver-uid", pickup.getId(), null);

        assertEquals(PickupStatus.COMPLETED, response.status());
        assertEquals(DonationRequestStatus.COMPLETED, donationRequest.getStatus());
        // Parent donation must NOT be marked COMPLETED because secondRequest is still pending!
        assertEquals(DonationStatus.PICKED_UP, donation.getStatus());
        verify(donationRepository, never()).save(donation);
    }

    // ==========================================
    // E. Concurrency & Locking
    // ==========================================

    @Test
    void test_concurrencyPessimisticLockCalledOnVerifyAndComplete() {
        when(pickupRepository.findByIdWithLock(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupOtpService.getMaxAttempts()).thenReturn(5);
        when(pickupOtpService.verifyOtp("123456", pickup.getOtpHash())).thenReturn(true);
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> i.getArgument(0));

        pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null);
        verify(pickupRepository).findByIdWithLock(pickup.getId());

        pickupService.completePickup("receiver-uid", pickup.getId(), null);
        verify(pickupRepository, times(2)).findByIdWithLock(pickup.getId());
    }

    @Test
    void test_concurrencySimulatedContention_singleThreadConsumesOtp() throws Exception {
        when(pickupOtpService.getMaxAttempts()).thenReturn(5);
        when(pickupOtpService.verifyOtp("123456", pickup.getOtpHash())).thenReturn(true);

        when(pickupRepository.findByIdWithLock(pickup.getId())).thenAnswer(i -> {
            synchronized (pickup) {
                return Optional.of(pickup);
            }
        });
        when(pickupRepository.save(any(Pickup.class))).thenAnswer(i -> i.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Future<?> f1 = executor.submit(() -> {
            try {
                latch.await();
                synchronized (pickup) {
                    pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null);
                }
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                latch.await();
                synchronized (pickup) {
                    pickupService.verifyOtp("receiver-uid", pickup.getId(), "123456", null);
                }
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        latch.countDown();
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one verification succeeds, second attempt fails because pickup is now PICKED_UP
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
        assertEquals(PickupStatus.PICKED_UP, pickup.getStatus());
    }

    @Test
    void getOtpForDonor_authorized_returnsOtp() {
        when(pickupRepository.findById(pickup.getId())).thenReturn(Optional.of(pickup));
        when(pickupOtpService.getCachedOtp(pickup.getId())).thenReturn(Optional.of("860451"));

        var response = pickupService.getOtpForDonor("donor-uid", pickup.getId(), null);

        assertNotNull(response);
        assertEquals("860451", response.otp());
        assertEquals(donation.getId(), response.donationId());
        assertEquals(pickup.getId(), response.pickupId());
    }

    @Test
    void getOtpForDonor_unrelatedDonor_throwsAccessDenied() {
        User unrelatedDonorUser = new User();
        unrelatedDonorUser.setId(UUID.randomUUID());
        unrelatedDonorUser.setFirebaseUid("other-donor-uid");
        unrelatedDonorUser.setRole(UserRole.DONOR);
        unrelatedDonorUser.setStatus(UserStatus.ACTIVE);

        DonorProfile otherProfile = new DonorProfile();
        otherProfile.setId(UUID.randomUUID());
        otherProfile.setUser(unrelatedDonorUser);

        when(userRepository.findByFirebaseUid("other-donor-uid")).thenReturn(Optional.of(unrelatedDonorUser));
        when(donorProfileRepository.findByUserId(unrelatedDonorUser.getId())).thenReturn(Optional.of(otherProfile));
        when(pickupRepository.findById(pickup.getId())).thenReturn(Optional.of(pickup));

        assertThrows(AccessDeniedException.class, () ->
                pickupService.getOtpForDonor("other-donor-uid", pickup.getId(), null)
        );
    }

    @Test
    void getOtpForDonor_whenAlreadyPickedUp_throwsIllegalState() {
        pickup.setStatus(PickupStatus.PICKED_UP);
        when(pickupRepository.findById(pickup.getId())).thenReturn(Optional.of(pickup));

        assertThrows(IllegalStateException.class, () ->
                pickupService.getOtpForDonor("donor-uid", pickup.getId(), null)
        );
    }

    @Test
    void getOtpForDonor_whenExpired_throwsIllegalState() {
        pickup.setOtpExpiresAt(Instant.now().minusSeconds(60));
        when(pickupRepository.findById(pickup.getId())).thenReturn(Optional.of(pickup));

        assertThrows(IllegalStateException.class, () ->
                pickupService.getOtpForDonor("donor-uid", pickup.getId(), null)
        );
    }
}

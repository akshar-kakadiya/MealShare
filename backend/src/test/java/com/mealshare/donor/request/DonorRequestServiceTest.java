package com.mealshare.donor.request;

import com.mealshare.audit.AuditService;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileService;
import com.mealshare.donor.request.dto.ApproveDonationRequestResponse;
import com.mealshare.donor.request.dto.DonorDonationRequestResponse;
import com.mealshare.donor.request.dto.RejectDonationRequestDto;
import com.mealshare.receiver.ReceiverOrganization;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

class DonorRequestServiceTest {

    @Mock
    private DonationRequestRepository donationRequestRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonorProfileService donorProfileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DonorRequestService donorRequestService;

    private User donorUser;
    private DonorProfile donorProfile;
    private User otherDonorUser;
    private DonorProfile otherDonorProfile;
    private Donation donation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);
        donorProfile.setEntityName("Grand Banquet");

        otherDonorUser = new User();
        otherDonorUser.setId(UUID.randomUUID());
        otherDonorUser.setFirebaseUid("other-donor-uid");
        otherDonorUser.setRole(UserRole.DONOR);
        otherDonorUser.setStatus(UserStatus.ACTIVE);

        otherDonorProfile = new DonorProfile();
        otherDonorProfile.setId(UUID.randomUUID());
        otherDonorProfile.setUser(otherDonorUser);
        otherDonorProfile.setEntityName("Other Kitchen");

        donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setDonor(donorProfile);
        donation.setTitle("Vegetarian Lunch");
        donation.setQuantity(100.0);
        donation.setAllocatedQuantity(0.0);
        donation.setStatus(DonationStatus.AVAILABLE);
        donation.setConsumeBefore(Instant.now().plus(4, ChronoUnit.HOURS));

        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));
        when(userRepository.findByFirebaseUid("other-donor-uid")).thenReturn(Optional.of(otherDonorUser));
        when(donorProfileService.findByUserId(donorUser.getId())).thenReturn(Optional.of(donorProfile));
        when(donorProfileService.findByUserId(otherDonorUser.getId())).thenReturn(Optional.of(otherDonorProfile));
    }

    private DonationRequest createRequest(UUID id, double qty, DonationRequestStatus status, Instant requestedAt) {
        DonationRequest req = new DonationRequest();
        req.setId(id);
        req.setDonation(donation);
        req.setRequestedQuantity(qty);
        req.setStatus(status);
        req.setAllocatedQuantity(status == DonationRequestStatus.APPROVED || status == DonationRequestStatus.COMPLETED ? qty : 0.0);
        req.setRequestedAt(requestedAt);

        ReceiverOrganization org = new ReceiverOrganization();
        org.setId(UUID.randomUUID());
        org.setOrgName("Care Shelter");
        req.setReceiverOrganization(org);

        return req;
    }

    @Test
    void test1_donorViewsRequestsForTheirDonation_paginated() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 25.0, DonationRequestStatus.PENDING, Instant.now());
        when(donationRepository.findById(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdOrderByRequestedAtAscIdAsc(eq(donation.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(req)));

        Page<DonorDonationRequestResponse> page = donorRequestService.getRequestsForDonation(
                "donor-uid", donation.getId(), PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(reqId, page.getContent().get(0).requestId());
    }

    @Test
    void test2_donorAccessingAnotherDonorsDonationRequests_rejected() {
        UUID otherDonationId = UUID.randomUUID();
        donation.setDonor(otherDonorProfile);
        when(donationRepository.findById(otherDonationId)).thenReturn(Optional.of(donation));

        assertThrows(AccessDeniedException.class, () ->
                donorRequestService.getRequestsForDonation("donor-uid", otherDonationId, PageRequest.of(0, 20)));
    }

    @Test
    void test6_test20_test28_pendingRequestApproved_exactQuantityAllocated_andAudit_autoRejectsRemaining() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 100.0, DonationRequestStatus.PENDING, Instant.now());

        UUID extraPendingReqId = UUID.randomUUID();
        DonationRequest extraPendingReq = createRequest(extraPendingReqId, 20.0, DonationRequestStatus.PENDING, Instant.now().plus(1, ChronoUnit.MINUTES));

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(req, extraPendingReq));

        ApproveDonationRequestResponse response = donorRequestService.approveRequest("donor-uid", reqId, null);

        assertNotNull(response);
        assertEquals(DonationRequestStatus.APPROVED, response.requestStatus());
        assertEquals(DonationStatus.APPROVED, response.donationStatus());
        assertEquals(100.0, response.allocatedQuantity());
        assertEquals(100.0, response.totalAllocatedQuantity());
        assertEquals(0.0, response.remainingQuantity());
        assertEquals(100.0, req.getAllocatedQuantity());

        // Verify extra pending request was automatically rejected due to full allocation
        assertEquals(DonationRequestStatus.REJECTED, extraPendingReq.getStatus());
        assertEquals(0.0, extraPendingReq.getAllocatedQuantity());
        assertTrue(extraPendingReq.getRequestReason().contains("Donation quantity fully allocated"));
        verify(donationRequestRepository).save(extraPendingReq);

        verify(auditService).logEvent(
                eq(donorUser.getId().toString()),
                eq("DONOR"),
                eq("REQUEST_APPROVED"),
                eq("DonationRequest"),
                eq(reqId.toString()),
                any(),
                any()
        );
    }

    @Test
    void test21_partialRemainingQuantityAllocationSucceeds_donationRemainsAvailable() {
        UUID reqId = UUID.randomUUID();
        donation.setAllocatedQuantity(30.0);
        donation.setStatus(DonationStatus.AVAILABLE);

        DonationRequest req = createRequest(reqId, 40.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(req));

        ApproveDonationRequestResponse response = donorRequestService.approveRequest("donor-uid", reqId, null);

        assertEquals(40.0, response.allocatedQuantity());
        assertEquals(70.0, response.totalAllocatedQuantity());
        assertEquals(30.0, response.remainingQuantity());
        assertEquals(40.0, req.getAllocatedQuantity());
        // Crucial state machine verification: remains AVAILABLE on partial allocation
        assertEquals(DonationStatus.AVAILABLE, response.donationStatus());
        assertEquals(DonationStatus.AVAILABLE, donation.getStatus());
    }

    @Test
    void test22_insufficientRemainingQuantity_rejected() {
        UUID reqId = UUID.randomUUID();
        donation.setAllocatedQuantity(90.0); // 10 remaining
        DonationRequest req = createRequest(reqId, 20.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Insufficient remaining quantity"));
    }

    @Test
    void test7_approvedRequestCannotBeApprovedAgain() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.APPROVED, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be approved"));
    }

    @Test
    void test8_rejectedRequestCannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.REJECTED, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be approved"));
    }

    @Test
    void test9_withdrawnRequestCannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.WITHDRAWN, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be approved"));
    }

    @Test
    void test10_completedRequestCannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.COMPLETED, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be approved"));
    }

    @Test
    void test10b_withdrawnRequestCannotBeRejected() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.WITHDRAWN, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.rejectRequest("donor-uid", reqId, null, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be rejected"));
    }

    @Test
    void test10c_completedRequestCannotBeRejected() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.COMPLETED, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.rejectRequest("donor-uid", reqId, null, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be rejected"));
    }

    @Test
    void test11_expiredDonationByTimestamp_cannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        donation.setConsumeBefore(Instant.now().minus(1, ChronoUnit.HOURS)); // Expired timestamp
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("consume-before deadline"));
    }

    @Test
    void test11b_expiredDonationByStatus_cannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        donation.setStatus(DonationStatus.EXPIRED);
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Expired donation cannot be approved"));
    }

    @Test
    void test12_cancelledDonationCannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        donation.setStatus(DonationStatus.CANCELLED);
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Cancelled donation cannot be approved"));
    }

    @Test
    void test13_completedDonationCannotBeApproved() {
        UUID reqId = UUID.randomUUID();
        donation.setStatus(DonationStatus.COMPLETED);
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("Completed donation cannot be approved"));
    }

    @Test
    void test14_alreadyApprovedDonationCannotBeApprovedAgain() {
        UUID reqId = UUID.randomUUID();
        donation.setStatus(DonationStatus.APPROVED);
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("already been fully allocated and approved"));
    }

    @Test
    void test15_donorCannotApproveAnotherDonorsRequest() {
        UUID reqId = UUID.randomUUID();
        donation.setDonor(otherDonorProfile); // Owned by another donor
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        assertThrows(AccessDeniedException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
    }

    @Test
    void test16_laterRequestCannotBypassEarlierEligiblePendingRequest() {
        Instant now = Instant.now();
        UUID reqAId = UUID.randomUUID();
        UUID reqBId = UUID.randomUUID();

        DonationRequest reqA = createRequest(reqAId, 50.0, DonationRequestStatus.PENDING, now.minus(10, ChronoUnit.MINUTES));
        DonationRequest reqB = createRequest(reqBId, 30.0, DonationRequestStatus.PENDING, now.minus(5, ChronoUnit.MINUTES));

        // Donor tries to approve B first!
        when(donationRequestRepository.findById(reqBId)).thenReturn(Optional.of(reqB));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(reqA, reqB));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqBId, null));
        assertTrue(ex.getMessage().contains("Cannot approve request out of FCFS order"));
    }

    @Test
    void test17_and_test18_earlierRejectedOrWithdrawnRequestDoesNotBlockLaterRequest() {
        Instant now = Instant.now();
        UUID reqBId = UUID.randomUUID();

        // Earlier request was already rejected/withdrawn, so findByDonationIdAndStatus only returns PENDING requests
        DonationRequest reqB = createRequest(reqBId, 30.0, DonationRequestStatus.PENDING, now);

        when(donationRequestRepository.findById(reqBId)).thenReturn(Optional.of(reqB));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(reqB));

        ApproveDonationRequestResponse response = donorRequestService.approveRequest("donor-uid", reqBId, null);
        assertNotNull(response);
        assertEquals(DonationRequestStatus.APPROVED, response.requestStatus());
    }

    @Test
    void test19_earlierPendingRequestExceedingRemainingQuantity_doesNotBlockSmallerEligibleRequest() {
        Instant now = Instant.now();
        donation.setQuantity(100.0);
        donation.setAllocatedQuantity(80.0); // remaining: 20.0

        UUID reqAId = UUID.randomUUID(); // asks for 50.0 (exceeds remaining 20.0)
        UUID reqBId = UUID.randomUUID(); // asks for 15.0 (fits in remaining 20.0)

        DonationRequest reqA = createRequest(reqAId, 50.0, DonationRequestStatus.PENDING, now.minus(10, ChronoUnit.MINUTES));
        DonationRequest reqB = createRequest(reqBId, 15.0, DonationRequestStatus.PENDING, now.minus(5, ChronoUnit.MINUTES));

        when(donationRequestRepository.findById(reqBId)).thenReturn(Optional.of(reqB));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(reqA, reqB));

        // Approving B succeeds because A cannot be satisfied with remaining quantity
        ApproveDonationRequestResponse response = donorRequestService.approveRequest("donor-uid", reqBId, null);
        assertNotNull(response);
        assertEquals(15.0, response.allocatedQuantity());
        assertEquals(95.0, response.totalAllocatedQuantity());
        assertEquals(5.0, response.remainingQuantity());
        assertEquals(DonationStatus.AVAILABLE, response.donationStatus());
    }

    @Test
    void test23_sequentialApprovalsUntilFullyAllocated_transitionsFromAvailableToApproved() {
        Instant now = Instant.now();
        UUID req1Id = UUID.randomUUID();
        UUID req2Id = UUID.randomUUID();

        DonationRequest req1 = createRequest(req1Id, 40.0, DonationRequestStatus.PENDING, now.minus(10, ChronoUnit.MINUTES));
        DonationRequest req2 = createRequest(req2Id, 60.0, DonationRequestStatus.PENDING, now.minus(5, ChronoUnit.MINUTES));

        // First approval: partial allocation (40 / 100)
        when(donationRequestRepository.findById(req1Id)).thenReturn(Optional.of(req1));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(req1, req2));

        ApproveDonationRequestResponse resp1 = donorRequestService.approveRequest("donor-uid", req1Id, null);
        assertEquals(DonationStatus.AVAILABLE, resp1.donationStatus());
        assertEquals(40.0, resp1.totalAllocatedQuantity());
        assertEquals(60.0, resp1.remainingQuantity());

        // Second approval: final allocation (60 / 60 remaining)
        when(donationRequestRepository.findById(req2Id)).thenReturn(Optional.of(req2));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(req2));

        ApproveDonationRequestResponse resp2 = donorRequestService.approveRequest("donor-uid", req2Id, null);
        assertEquals(DonationStatus.APPROVED, resp2.donationStatus());
        assertEquals(100.0, resp2.totalAllocatedQuantity());
        assertEquals(0.0, resp2.remainingQuantity());
    }

    @Test
    void test24_onceRemainingQuantityZero_subsequentPendingRequestCannotBeApproved() {
        donation.setAllocatedQuantity(100.0);
        donation.setStatus(DonationStatus.APPROVED);

        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 10.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqId, null));
        assertTrue(ex.getMessage().contains("already been fully allocated and approved"));
    }

    @Test
    void test25_and_test29_pendingRequestCanBeRejected_andAudit() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 30.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRequestRepository.save(any(DonationRequest.class))).thenAnswer(i -> i.getArgument(0));

        DonorDonationRequestResponse response = donorRequestService.rejectRequest(
                "donor-uid", reqId, new RejectDonationRequestDto("Insufficient transport vessels"), null
        );

        assertEquals(DonationRequestStatus.REJECTED, response.status());
        assertEquals(0.0, response.allocatedQuantity());
        assertEquals(0.0, req.getAllocatedQuantity());
        assertTrue(response.requestReason().contains("Insufficient transport vessels"));

        verify(auditService).logEvent(
                eq(donorUser.getId().toString()),
                eq("DONOR"),
                eq("REQUEST_REJECTED"),
                eq("DonationRequest"),
                eq(reqId.toString()),
                any(),
                any()
        );
    }

    @Test
    void test26_approvedRequestCannotBeRejected() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 30.0, DonationRequestStatus.APPROVED, Instant.now());
        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.rejectRequest("donor-uid", reqId, null, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be rejected"));
    }

    @Test
    void test31_fcfsTieBreaking_withSameRequestedAt_andUuidId() {
        Instant identicalTimestamp = Instant.parse("2026-09-19T10:00:00Z");

        // UUID A is lexicographically smaller than UUID B
        UUID reqAId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID reqBId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        DonationRequest reqA = createRequest(reqAId, 50.0, DonationRequestStatus.PENDING, identicalTimestamp);
        DonationRequest reqB = createRequest(reqBId, 50.0, DonationRequestStatus.PENDING, identicalTimestamp);

        // Ordered by requestedAt ASC, id ASC -> [reqA, reqB]
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(reqA, reqB));

        // 1. Attempting to approve reqB first should be blocked because reqA has tie-break precedence!
        when(donationRequestRepository.findById(reqBId)).thenReturn(Optional.of(reqB));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donorRequestService.approveRequest("donor-uid", reqBId, null));
        assertTrue(ex.getMessage().contains("Cannot approve request out of FCFS order"));
        assertTrue(ex.getMessage().contains(reqAId.toString()));

        // 2. Approving reqA first succeeds!
        when(donationRequestRepository.findById(reqAId)).thenReturn(Optional.of(reqA));
        ApproveDonationRequestResponse respA = donorRequestService.approveRequest("donor-uid", reqAId, null);
        assertNotNull(respA);
        assertEquals(DonationRequestStatus.APPROVED, respA.requestStatus());
        assertEquals(50.0, respA.allocatedQuantity());
    }

    @Test
    void test_allocatedQuantityInvariant_nonNegativeAndLeQuantity() {
        // Invariant 1: cannot set negative allocated quantity
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> donation.setAllocatedQuantity(-10.0));
        assertTrue(ex1.getMessage().contains("cannot be null or negative"));

        // Invariant 2: cannot set allocated quantity greater than total quantity
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> donation.setAllocatedQuantity(150.0));
        assertTrue(ex2.getMessage().contains("cannot exceed total quantity"));

        // Invariant 3: valid within bounds [0, quantity]
        donation.setAllocatedQuantity(0.0);
        assertEquals(0.0, donation.getAllocatedQuantity());
        donation.setAllocatedQuantity(50.0);
        assertEquals(50.0, donation.getAllocatedQuantity());
        donation.setAllocatedQuantity(100.0);
        assertEquals(100.0, donation.getAllocatedQuantity());
    }

    @Test
    void test_remainingQuantityInvariant_nonNegativeAndMathAccuracy() {
        donation.setQuantity(100.0);
        donation.setAllocatedQuantity(0.0);
        assertEquals(100.0, donation.getRemainingQuantity());

        donation.setAllocatedQuantity(45.5);
        assertEquals(54.5, donation.getRemainingQuantity());

        donation.setAllocatedQuantity(100.0);
        assertEquals(0.0, donation.getRemainingQuantity());
    }

    @Test
    void test_concurrencyPessimisticLockingVerification() {
        UUID reqId = UUID.randomUUID();
        DonationRequest req = createRequest(reqId, 50.0, DonationRequestStatus.PENDING, Instant.now());

        when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(donationRepository.findByIdWithLock(donation.getId())).thenReturn(Optional.of(donation));
        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenReturn(List.of(req));

        donorRequestService.approveRequest("donor-uid", reqId, null);

        // Verify findByIdWithLock was explicitly used instead of standard findById
        verify(donationRepository).findByIdWithLock(donation.getId());
        verify(donationRepository, never()).findById(donation.getId());
    }

    @Test
    void test_concurrencySimulatedContention_firstThreadConsumesRemaining_secondThreadRejected() throws Exception {
        donation.setQuantity(50.0);
        donation.setAllocatedQuantity(0.0);
        donation.setStatus(DonationStatus.AVAILABLE);

        UUID req1Id = UUID.randomUUID();
        UUID req2Id = UUID.randomUUID();
        DonationRequest req1 = createRequest(req1Id, 50.0, DonationRequestStatus.PENDING, Instant.now());
        DonationRequest req2 = createRequest(req2Id, 50.0, DonationRequestStatus.PENDING, Instant.now().plus(1, ChronoUnit.MILLIS));

        // When locking query is executed, synchronize access so only one thread enters critical section at a time
        when(donationRepository.findByIdWithLock(donation.getId())).thenAnswer(invocation -> {
            synchronized (donation) {
                return Optional.of(donation);
            }
        });
        when(donationRequestRepository.findById(req1Id)).thenReturn(Optional.of(req1));
        when(donationRequestRepository.findById(req2Id)).thenReturn(Optional.of(req2));

        when(donationRequestRepository.findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(donation.getId(), DonationRequestStatus.PENDING))
                .thenAnswer(i -> {
                    synchronized (donation) {
                        List<DonationRequest> list = new ArrayList<>();
                        if (req1.getStatus() == DonationRequestStatus.PENDING) list.add(req1);
                        if (req2.getStatus() == DonationRequestStatus.PENDING) list.add(req2);
                        return list;
                    }
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Future<?> f1 = executor.submit(() -> {
            try {
                latch.await();
                synchronized (donation) {
                    donorRequestService.approveRequest("donor-uid", req1Id, null);
                }
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                latch.await();
                synchronized (donation) {
                    donorRequestService.approveRequest("donor-uid", req2Id, null);
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

        // Exactly one approval succeeds, and the other is rejected (cannot over-allocate)
        assertEquals(1, successCount.get(), "Only one approval should succeed");
        assertEquals(1, failureCount.get(), "The competing approval must fail due to zero remaining quantity or fully allocated status");
        assertEquals(50.0, donation.getAllocatedQuantity());
        assertEquals(0.0, donation.getRemainingQuantity());
        assertEquals(DonationStatus.APPROVED, donation.getStatus());
    }
}

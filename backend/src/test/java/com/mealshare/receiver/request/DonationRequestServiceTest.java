package com.mealshare.receiver.request;

import com.mealshare.audit.AuditService;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donor.DonorProfile;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationService;
import com.mealshare.receiver.request.dto.CreateDonationRequestDto;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationRequestServiceTest {

    @Mock
    private DonationRequestRepository donationRequestRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private ReceiverOrganizationService receiverOrganizationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DonationRequestService donationRequestService;

    private User receiverUser;
    private ReceiverOrganization receiverOrg;
    private User otherReceiverUser;
    private ReceiverOrganization otherReceiverOrg;
    private Donation donation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("recv-uid");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);

        receiverOrg = new ReceiverOrganization();
        receiverOrg.setId(UUID.randomUUID());
        receiverOrg.setUser(receiverUser);
        receiverOrg.setOrgName("Hope Shelter");

        otherReceiverUser = new User();
        otherReceiverUser.setId(UUID.randomUUID());
        otherReceiverUser.setFirebaseUid("other-recv-uid");
        otherReceiverUser.setRole(UserRole.RECEIVER);
        otherReceiverUser.setStatus(UserStatus.ACTIVE);

        otherReceiverOrg = new ReceiverOrganization();
        otherReceiverOrg.setId(UUID.randomUUID());
        otherReceiverOrg.setUser(otherReceiverUser);
        otherReceiverOrg.setOrgName("Other Shelter");

        donation = new Donation();
        donation.setId(UUID.randomUUID());
        donation.setTitle("Vegetarian Meals");
        donation.setQuantity(50.0);
        donation.setStatus(DonationStatus.AVAILABLE);
        donation.setConsumeBefore(Instant.now().plus(4, ChronoUnit.HOURS));

        DonorProfile donor = new DonorProfile();
        User donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donor.setUser(donorUser);
        donation.setDonor(donor);

        when(userRepository.findByFirebaseUid("recv-uid")).thenReturn(Optional.of(receiverUser));
        when(userRepository.findByFirebaseUid("other-recv-uid")).thenReturn(Optional.of(otherReceiverUser));
        when(receiverOrganizationService.findByUserId(receiverUser.getId())).thenReturn(Optional.of(receiverOrg));
        when(receiverOrganizationService.findByUserId(otherReceiverUser.getId())).thenReturn(Optional.of(otherReceiverOrg));
        when(receiverOrganizationService.getOrCreateProfileForUser(any(), any(), any(), any())).thenReturn(receiverOrg);
    }

    @Test
    void test20_test21_test32_receiverCanCreateRequest_startsAsPending_andGeneratesAudit() {
        UUID donationId = donation.getId();
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRequestRepository.existsByDonationIdAndReceiverOrganizationIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(donationRequestRepository.save(any(DonationRequest.class))).thenAnswer(i -> {
            DonationRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateDonationRequestDto dto = new CreateDonationRequestDto(20.0, "Feeding 20 families");
        DonationRequestResponse response = donationRequestService.createRequest("recv-uid", donationId, dto, null);

        assertNotNull(response);
        assertEquals(DonationRequestStatus.PENDING, response.status());
        assertEquals(20.0, response.requestedQuantity());
        assertEquals("Feeding 20 families", response.requestReason());

        // Verify audit event
        verify(auditService).logEvent(
                eq(receiverUser.getId().toString()),
                eq("RECEIVER"),
                eq("REQUEST_CREATED"),
                eq("DonationRequest"),
                eq(response.id().toString()),
                any(),
                any()
        );
    }

    @Test
    void test24_requestedQuantityExceedingDonationQuantity_rejected() {
        UUID donationId = donation.getId();
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

        CreateDonationRequestDto dto = new CreateDonationRequestDto(60.0, "Too much");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donationRequestService.createRequest("recv-uid", donationId, dto, null));
        assertTrue(ex.getMessage().contains("exceeds available donation quantity"));
    }

    @Test
    void test25_requestedQuantityZeroOrNegative_rejected() {
        UUID donationId = donation.getId();
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

        CreateDonationRequestDto dto = new CreateDonationRequestDto(-5.0, "Invalid");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donationRequestService.createRequest("recv-uid", donationId, dto, null));
        assertTrue(ex.getMessage().contains("strictly greater than 0"));
    }

    @Test
    void test26_duplicateActiveRequest_rejected() {
        UUID donationId = donation.getId();
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));
        when(donationRequestRepository.existsByDonationIdAndReceiverOrganizationIdAndStatusIn(
                eq(donationId), eq(receiverOrg.getId()), any())).thenReturn(true);

        CreateDonationRequestDto dto = new CreateDonationRequestDto(10.0, "Second request");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donationRequestService.createRequest("recv-uid", donationId, dto, null));
        assertTrue(ex.getMessage().contains("active request already exists"));
    }

    @Test
    void testRequestOnNonAvailableDonation_rejected() {
        UUID donationId = donation.getId();
        donation.setStatus(DonationStatus.COMPLETED);
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

        CreateDonationRequestDto dto = new CreateDonationRequestDto(10.0, "Request");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donationRequestService.createRequest("recv-uid", donationId, dto, null));
        assertTrue(ex.getMessage().contains("no longer available"));
    }

    @Test
    void testRequestOnExpiredDonation_rejected() {
        UUID donationId = donation.getId();
        donation.setConsumeBefore(Instant.now().minus(1, ChronoUnit.HOURS)); // Expired
        when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

        CreateDonationRequestDto dto = new CreateDonationRequestDto(10.0, "Request");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donationRequestService.createRequest("recv-uid", donationId, dto, null));
        assertTrue(ex.getMessage().contains("consume-before"));
    }

    @Test
    void test28_receiverCannotAccessAnotherReceiversRequest() {
        UUID requestId = UUID.randomUUID();
        DonationRequest request = new DonationRequest();
        request.setId(requestId);
        request.setReceiverOrganization(otherReceiverOrg); // Owned by another org

        when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class,
                () -> donationRequestService.getRequestById("recv-uid", requestId));
    }

    @Test
    void test29_requestOrderingIsFcfs() {
        UUID donationId = donation.getId();
        Instant now = Instant.now();

        DonationRequest r1 = new DonationRequest();
        r1.setId(UUID.randomUUID());
        r1.setRequestedQuantity(10.0);
        r1.setRequestedAt(now.minus(10, ChronoUnit.MINUTES));

        DonationRequest r2 = new DonationRequest();
        r2.setId(UUID.randomUUID());
        r2.setRequestedQuantity(15.0);
        r2.setRequestedAt(now.minus(5, ChronoUnit.MINUTES));

        when(donationRequestRepository.findByDonationIdOrderByRequestedAtAscIdAsc(donationId))
                .thenReturn(List.of(r1, r2));

        List<DonationRequestResponse> list = donationRequestService.getRequestsForDonationInFcfsOrder(donationId);
        assertEquals(2, list.size());
        assertEquals(r1.getId(), list.get(0).id());
        assertEquals(r2.getId(), list.get(1).id());
    }

    @Test
    void test30_validWithdrawal_andAudit() {
        UUID requestId = UUID.randomUUID();
        DonationRequest req = new DonationRequest();
        req.setId(requestId);
        req.setReceiverOrganization(receiverOrg);
        req.setStatus(DonationRequestStatus.PENDING);

        when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(donationRequestRepository.save(any(DonationRequest.class))).thenAnswer(i -> i.getArgument(0));

        DonationRequestResponse response = donationRequestService.withdrawRequest("recv-uid", requestId, null);
        assertEquals(DonationRequestStatus.WITHDRAWN, response.status());

        verify(auditService).logEvent(
                eq(receiverUser.getId().toString()),
                eq("RECEIVER"),
                eq("REQUEST_WITHDRAWN"),
                eq("DonationRequest"),
                eq(requestId.toString()),
                any(),
                any()
        );
    }

    @Test
    void test31_invalidWithdrawal_rejected() {
        UUID requestId = UUID.randomUUID();
        DonationRequest req = new DonationRequest();
        req.setId(requestId);
        req.setReceiverOrganization(receiverOrg);
        req.setStatus(DonationRequestStatus.APPROVED); // Already approved!

        when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(req));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donationRequestService.withdrawRequest("recv-uid", requestId, null));
        assertTrue(ex.getMessage().contains("Only PENDING requests can be withdrawn"));
    }
}

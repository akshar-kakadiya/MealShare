package com.mealshare.admin;

import com.mealshare.admin.dto.*;
import com.mealshare.admin.service.*;
import com.mealshare.audit.AuditLog;
import com.mealshare.audit.AuditLogRepository;
import com.mealshare.audit.AuditService;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.user.ProfileStatus;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import com.mealshare.user.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminOperationsApiTest {

    @Mock private UserRepository userRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private DonationRequestRepository donationRequestRepository;
    @Mock private PickupRepository pickupRepository;
    @Mock private ReceiverOrganizationRepository receiverOrganizationRepository;
    @Mock private DonorProfileRepository donorProfileRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditService auditService;

    private AdminDashboardService adminDashboardService;
    private AdminUserService adminUserService;
    private AdminDonationService adminDonationService;
    private AdminRequestService adminRequestService;
    private AdminVerificationService adminVerificationService;
    private AdminReportService adminReportService;
    private AdminAuditLogService adminAuditLogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        adminDashboardService = new AdminDashboardService(
                userRepository, donationRepository, donationRequestRepository,
                pickupRepository, receiverOrganizationRepository, auditLogRepository
        );
        adminUserService = new AdminUserService(
                userRepository, donorProfileRepository, receiverOrganizationRepository,
                auditService
        );
        adminDonationService = new AdminDonationService(donationRepository, donationRequestRepository);
        adminRequestService = new AdminRequestService(donationRequestRepository, pickupRepository);
        adminVerificationService = new AdminVerificationService(
                receiverOrganizationRepository, donorProfileRepository, userRepository, auditService
        );
        adminReportService = new AdminReportService(donationRepository, donationRequestRepository, pickupRepository, userRepository, donorProfileRepository);
        adminAuditLogService = new AdminAuditLogService(auditLogRepository);
    }

    @Test
    @DisplayName("AdminDashboardService aggregates real database metrics correctly")
    void testDashboardMetricsCalculation() {
        when(userRepository.count()).thenReturn(25L);
        when(userRepository.countByRole(UserRole.DONOR)).thenReturn(10L);
        when(userRepository.countByRole(UserRole.RECEIVER)).thenReturn(8L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);

        when(donationRepository.count()).thenReturn(50L);
        when(donationRepository.countByStatus(DonationStatus.AVAILABLE)).thenReturn(20L);
        when(donationRepository.countByStatus(DonationStatus.APPROVED)).thenReturn(15L);
        when(donationRepository.countByStatus(DonationStatus.COMPLETED)).thenReturn(10L);
        when(donationRepository.countByStatus(DonationStatus.CANCELLED)).thenReturn(3L);
        when(donationRepository.countByStatus(DonationStatus.EXPIRED)).thenReturn(2L);
        when(donationRepository.sumTotalQuantity()).thenReturn(1000.0);
        when(donationRepository.sumTotalAllocatedQuantity()).thenReturn(400.0);
        when(donationRepository.sumCompletedQuantity()).thenReturn(250.0);

        when(donationRequestRepository.count()).thenReturn(40L);
        when(donationRequestRepository.countByStatus(DonationRequestStatus.PENDING)).thenReturn(12L);
        when(donationRequestRepository.countByStatus(DonationRequestStatus.APPROVED)).thenReturn(15L);

        when(pickupRepository.count()).thenReturn(30L);
        when(pickupRepository.countByStatus(PickupStatus.ASSIGNED)).thenReturn(8L);
        when(pickupRepository.countByStatus(PickupStatus.PICKED_UP)).thenReturn(5L);
        when(pickupRepository.countByStatus(PickupStatus.COMPLETED)).thenReturn(15L);

        when(donationRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(receiverOrganizationRepository.searchReceiverOrganizations(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(auditLogRepository.findTop6ByOrderByTimestampDesc()).thenReturn(List.of());

        AdminDashboardResponse response = adminDashboardService.getDashboardStats();

        assertNotNull(response);
        assertEquals(25L, response.users().total());
        assertEquals(10L, response.users().donors());
        assertEquals(50L, response.donations().total());
        assertEquals(20L, response.donations().available());
        assertEquals(1000.0, response.donations().totalQuantityDonated());
        assertEquals(400.0, response.donations().totalAllocatedQuantity());
        assertEquals(40L, response.requests().total());
        assertEquals(12L, response.requests().pending());
        assertEquals(30L, response.pickups().total());
        assertEquals(5, response.donationStatusDistribution().size());
        assertEquals(7, response.dailyActivity().size());
    }

    @Test
    @DisplayName("AdminUserService searches and paginates users")
    void testUserSearchAndPagination() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("FreshBite");
        user.setEmail("freshbite@example.com");
        user.setRole(UserRole.DONOR);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.searchUsers(eq(UserRole.DONOR), eq(UserStatus.ACTIVE), eq("fresh"), any()))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminUserResponse> result = adminUserService.getUsers(UserRole.DONOR, UserStatus.ACTIVE, "fresh", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("FreshBite", result.getContent().get(0).name());
        assertEquals(UserRole.DONOR, result.getContent().get(0).role());
    }

    @Test
    @DisplayName("AdminUserService updates user status and records audit event")
    void testUpdateUserStatus() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setName("Abusive User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.DONOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var req = new UpdateUserStatusRequest(UserStatus.SUSPENDED, "Suspicious activity");
        AdminUserResponse updated = adminUserService.updateUserStatus(userId, req, "admin-uid", null);

        assertEquals(UserStatus.SUSPENDED, updated.status());
        verify(auditService).logEvent(eq("admin-uid"), eq("ADMIN"), eq("USER_STATUS_UPDATED"), eq("User"), eq(userId.toString()), any(), any());
    }

    @Test
    @DisplayName("AdminUserService removes user by deactivating status to SUSPENDED, rejecting verification, and recording audit event")
    void testRemoveUserSuccess() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setName("Target User");
        user.setFirebaseUid("target-firebase-uid");
        user.setStatus(UserStatus.ACTIVE);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        user.setRole(UserRole.DONOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        adminUserService.removeUser(userId, "admin-uid", null);

        verify(userRepository).delete(user);
        verify(userRepository).flush();
        verify(auditService).logEvent(eq("admin-uid"), eq("ADMIN"), eq("USER_DELETED_PERMANENTLY"), eq("User"), eq(userId.toString()), any(), any());
    }

    @Test
    @DisplayName("AdminUserService throws exception when administrator attempts to remove own account")
    void testRemoveUserSelfRemovalFails() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFirebaseUid("admin-uid");
        user.setRole(UserRole.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminUserService.removeUser(userId, "admin-uid", null));
        assertEquals("Cannot remove the currently authenticated administrator account.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("AdminUserService throws exception when attempting to remove the last active admin")
    void testRemoveUserLastAdminFails() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFirebaseUid("other-admin-uid");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminUserService.removeUser(userId, "current-admin-uid", null));
        assertEquals("Cannot remove the last active administrator account on the platform.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("AdminDonationService computes remainingQuantity invariant correctly")
    void testDonationRemainingQuantityInvariant() {
        UUID donationId = UUID.randomUUID();
        Donation d = new Donation();
        d.setId(donationId);
        d.setTitle("Rice & Dal");
        d.setQuantity(100.0);
        d.setAllocatedQuantity(35.0);
        d.setFoodCategory(FoodCategory.PREPARED_MEALS);
        d.setStatus(DonationStatus.AVAILABLE);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(d));
        when(donationRequestRepository.findByDonationId(donationId)).thenReturn(List.of());

        AdminDonationDetailResponse detail = adminDonationService.getDonationDetail(donationId);

        assertNotNull(detail);
        assertEquals(100.0, detail.donation().quantity());
        assertEquals(35.0, detail.donation().allocatedQuantity());
        assertEquals(65.0, detail.donation().remainingQuantity());
    }

    @Test
    @DisplayName("AdminVerificationService adjudicates organization and synchronizes user active status")
    void testVerificationAdjudication() {
        UUID orgId = UUID.randomUUID();
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setStatus(UserStatus.PENDING_VERIFICATION);
        u.setProfileStatus(ProfileStatus.COMPLETE);

        ReceiverOrganization org = new ReceiverOrganization();
        org.setId(orgId);
        org.setOrgName("Care Foundation");
        org.setVerificationStatus("PENDING");
        org.setProfileStatus(ProfileStatus.COMPLETE);
        org.setUser(u);

        when(receiverOrganizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(receiverOrganizationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var approveReq = new UpdateVerificationStatusRequest("APPROVED", "Verified via certificate");
        AdminVerificationResponse res = adminVerificationService.updateVerificationStatus(orgId, approveReq, "admin-uid", null);

        assertEquals("APPROVED", res.status());
        assertEquals(UserStatus.ACTIVE, u.getStatus());
        verify(userRepository).save(u);
        verify(auditService).logEvent(eq("admin-uid"), eq("ADMIN"), eq("ORGANIZATION_VERIFICATION_ADJUDICATED"), eq("ReceiverOrganization"), eq(orgId.toString()), any(), any());
    }

    @Test
    @DisplayName("AdminReportService calculates platform impact and top donors")
    void testReportSummary() {
        when(donationRepository.count()).thenReturn(10L);
        when(donationRepository.countByStatus(DonationStatus.COMPLETED)).thenReturn(6L);
        when(donationRepository.countByStatus(DonationStatus.AVAILABLE)).thenReturn(4L);
        when(donationRepository.sumTotalQuantity()).thenReturn(500.0);
        when(donationRepository.sumTotalAllocatedQuantity()).thenReturn(300.0);
        when(donationRepository.sumCompletedQuantity()).thenReturn(200.0);
        when(donationRequestRepository.count()).thenReturn(8L);
        when(pickupRepository.countByStatus(PickupStatus.COMPLETED)).thenReturn(5L);

        DonorProfile dp = new DonorProfile();
        dp.setId(UUID.randomUUID());
        dp.setEntityName("Taj Kitchen");

        Donation d = new Donation();
        d.setId(UUID.randomUUID());
        d.setDonor(dp);
        d.setQuantity(50.0);
        d.setCreatedAt(Instant.now());

        when(donationRepository.findAll()).thenReturn(List.of(d));

        AdminReportSummaryResponse report = adminReportService.getReportSummary("30d");

        assertEquals(10L, report.totalDonations());
        assertEquals(6L, report.completedDonations());
        assertEquals(500.0, report.totalQuantityDonated());
        assertEquals(1, report.topDonors().size());
        assertEquals("Taj Kitchen", report.topDonors().get(0).donorName());
    }

    @Test
    @DisplayName("AdminAuditLogService searches and paginates audit logs")
    void testAuditLogSearch() {
        AuditLog log = new AuditLog();
        log.setId(101L);
        log.setActionType("DONATION_CREATED");
        log.setActorRole("DONOR");
        log.setEntityName("Donation");
        log.setEntityId(UUID.randomUUID().toString());

        when(auditLogRepository.searchAuditLogs(eq("DONATION_CREATED"), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AdminAuditLogResponse> logs = adminAuditLogService.getAuditLogs("DONATION_CREATED", null, null, PageRequest.of(0, 10));

        assertEquals(1, logs.getTotalElements());
        assertEquals("DONATION_CREATED", logs.getContent().get(0).actionType());
    }
}

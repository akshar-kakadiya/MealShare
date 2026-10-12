package com.mealshare.user;

import com.mealshare.admin.dto.AdminVerificationResponse;
import com.mealshare.admin.dto.UpdateVerificationStatusRequest;
import com.mealshare.admin.service.AdminVerificationService;
import com.mealshare.audit.AuditService;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.donor.DonorProfileService;
import com.mealshare.donor.dto.DonorProfileRequest;
import com.mealshare.donor.dto.DonorProfileResponse;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.ReceiverOrganizationService;
import com.mealshare.receiver.dto.ReceiverOrganizationRequest;
import com.mealshare.receiver.dto.ReceiverOrganizationResponse;
import com.mealshare.security.FirebasePrincipal;
import com.mealshare.user.dto.RegistrationRequest;
import com.mealshare.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RoleRegistrationAndVerificationTest {

    @Mock private UserRepository userRepository;
    @Mock private DonorProfileRepository donorProfileRepository;
    @Mock private ReceiverOrganizationRepository receiverOrganizationRepository;
    @Mock private AuditService auditService;

    private UserService userService;
    private DonorProfileService donorProfileService;
    private ReceiverOrganizationService receiverOrganizationService;
    private AdminVerificationService adminVerificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, donorProfileRepository, receiverOrganizationRepository);
        donorProfileService = new DonorProfileService(donorProfileRepository, userRepository);
        receiverOrganizationService = new ReceiverOrganizationService(receiverOrganizationRepository, userRepository);
        adminVerificationService = new AdminVerificationService(
                receiverOrganizationRepository,
                donorProfileRepository,
                userRepository,
                auditService
        );
    }

    @Test
    @DisplayName("Donor registration saves role, category, and sets profile INCOMPLETE and verification PENDING")
    void testDonorRegistrationSetsRoleCategoryAndInitialStates() {
        FirebasePrincipal principal = new FirebasePrincipal("donor-uid", "donor@restaurant.com", "Chef Luigi", Map.of());
        RegistrationRequest req = new RegistrationRequest(
                "Chef Luigi",
                "donor@restaurant.com",
                "9876543210",
                UserRole.DONOR,
                "HOTEL_RESTAURANT"
        );

        when(userRepository.existsByFirebaseUid("donor-uid")).thenReturn(false);
        when(userRepository.existsByEmail("donor@restaurant.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse res = userService.registerUser(principal, req);

        assertNotNull(res);
        assertEquals(UserRole.DONOR, res.role());
        assertEquals("HOTEL_RESTAURANT", res.category());
        assertEquals("INCOMPLETE", res.profileStatus());
        assertEquals("PENDING", res.verificationStatus());
        assertEquals(UserStatus.ACTIVE, res.status());
        verify(donorProfileRepository).save(any(DonorProfile.class));
    }

    @Test
    @DisplayName("Receiver registration saves role, category, and sets profile INCOMPLETE and verification PENDING")
    void testReceiverRegistrationSetsRoleCategoryAndInitialStates() {
        FirebasePrincipal principal = new FirebasePrincipal("receiver-uid", "care@ngo.org", "Hope Shelter", Map.of());
        RegistrationRequest req = new RegistrationRequest(
                "Hope Shelter",
                "care@ngo.org",
                "9876543211",
                UserRole.RECEIVER,
                "COMMUNITY_SHELTER"
        );

        when(userRepository.existsByFirebaseUid("receiver-uid")).thenReturn(false);
        when(userRepository.existsByEmail("care@ngo.org")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse res = userService.registerUser(principal, req);

        assertNotNull(res);
        assertEquals(UserRole.RECEIVER, res.role());
        assertEquals("COMMUNITY_SHELTER", res.category());
        assertEquals("INCOMPLETE", res.profileStatus());
        assertEquals("PENDING", res.verificationStatus());
        verify(receiverOrganizationRepository).save(any(ReceiverOrganization.class));
    }

    @Test
    @DisplayName("Donor profile completion updates category fields, marks COMPLETE, and resubmits if ACTION_REQUIRED")
    void testDonorProfileCompletionAndResubmission() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFirebaseUid("donor-uid");
        user.setRole(UserRole.DONOR);
        user.setCategory("HOTEL_RESTAURANT");
        user.setProfileStatus(ProfileStatus.INCOMPLETE);
        user.setVerificationStatus(VerificationStatus.ACTION_REQUIRED);
        user.setActionRequiredNotes("Please upload valid kitchen photos");

        DonorProfile profile = new DonorProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        profile.setProfileStatus(ProfileStatus.INCOMPLETE);
        profile.setVerificationStatus(VerificationStatus.ACTION_REQUIRED);
        profile.setActionRequiredNotes("Please upload valid kitchen photos");

        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(user));
        when(donorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(donorProfileRepository.save(any(DonorProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        DonorProfileRequest req = new DonorProfileRequest(
                "HOTEL_RESTAURANT",
                "Luigi Trattoria",
                "123 MG Road, Bengaluru",
                12.9716,
                77.5946,
                true,
                "Luigi Mario",
                "Fine Dining",
                null,
                "/api/v1/evidence/kitchen.jpg",
                "Chef Luigi",
                "9876543210"
        );

        DonorProfileResponse res = donorProfileService.updateProfile("donor-uid", req);

        assertEquals("COMPLETE", res.profileStatus());
        assertEquals("PENDING", res.verificationStatus());
        assertNull(res.actionRequiredNotes());
        assertEquals("/api/v1/evidence/kitchen.jpg", res.photos());
        assertEquals("Luigi Mario", res.contactPerson());
        assertEquals(VerificationStatus.PENDING, user.getVerificationStatus());
        assertEquals(ProfileStatus.COMPLETE, user.getProfileStatus());
    }

    @Test
    @DisplayName("Receiver organization completion marks COMPLETE and resubmits if REJECTED")
    void testReceiverProfileCompletionAndResubmission() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFirebaseUid("receiver-uid");
        user.setRole(UserRole.RECEIVER);
        user.setCategory("NGO");
        user.setProfileStatus(ProfileStatus.INCOMPLETE);
        user.setVerificationStatus(VerificationStatus.REJECTED);
        user.setRejectionReason("Incomplete mission statement");

        ReceiverOrganization org = new ReceiverOrganization();
        org.setId(UUID.randomUUID());
        org.setUser(user);
        org.setProfileStatus(ProfileStatus.INCOMPLETE);
        org.setVerificationStatus("REJECTED");
        org.setRejectionReason("Incomplete mission statement");

        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(user));
        when(receiverOrganizationRepository.findByUserId(userId)).thenReturn(Optional.of(org));
        when(receiverOrganizationRepository.save(any(ReceiverOrganization.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiverOrganizationRequest req = new ReceiverOrganizationRequest(
                "NGO",
                "Hope Food Foundation",
                "NGO-12345",
                "45 Indiranagar, Bengaluru",
                12.9780,
                77.6400,
                15000.0,
                100,
                "Sister Mary",
                "Feeding underprivileged communities daily",
                "/api/v1/evidence/shelter.jpg",
                "Hope Organization",
                "9876543222"
        );

        ReceiverOrganizationResponse res = receiverOrganizationService.updateProfile("receiver-uid", req);

        assertEquals("COMPLETE", res.profileStatus());
        assertEquals("PENDING", res.verificationStatus());
        assertNull(res.rejectionReason());
        assertEquals("Sister Mary", res.contactPerson());
        assertEquals("Feeding underprivileged communities daily", res.description());
        assertEquals(VerificationStatus.PENDING, user.getVerificationStatus());
    }

    @Test
    @DisplayName("Admin adjudicates request with ACTION_REQUIRED and reason notes")
    void testAdminAdjudicatesActionRequired() {
        UUID orgId = UUID.randomUUID();
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setStatus(UserStatus.ACTIVE);
        u.setVerificationStatus(VerificationStatus.PENDING);

        ReceiverOrganization org = new ReceiverOrganization();
        org.setId(orgId);
        org.setOrgName("Care Trust");
        org.setVerificationStatus("PENDING");
        org.setUser(u);

        when(receiverOrganizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(receiverOrganizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateVerificationStatusRequest req = new UpdateVerificationStatusRequest(
                "ACTION_REQUIRED",
                "Please upload registration document",
                null,
                "Please upload registration document"
        );

        AdminVerificationResponse res = adminVerificationService.updateVerificationStatus(orgId, req, "admin-uid", null);

        assertEquals("ACTION_REQUIRED", res.status());
        assertEquals("Please upload registration document", res.actionRequiredNotes());
        assertEquals(UserStatus.ACTIVE, u.getStatus());
        assertEquals(VerificationStatus.ACTION_REQUIRED, u.getVerificationStatus());
        verify(auditService).logEvent(eq("admin-uid"), eq("ADMIN"), eq("ORGANIZATION_VERIFICATION_ADJUDICATED"), eq("ReceiverOrganization"), eq(orgId.toString()), any(), any());
    }

    @Test
    @DisplayName("Admin adjudicates DonorProfile with APPROVAL and synchronizes user state")
    void testAdminAdjudicatesDonorApproval() {
        UUID donorId = UUID.randomUUID();
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setStatus(UserStatus.ACTIVE);
        u.setVerificationStatus(VerificationStatus.PENDING);

        DonorProfile dp = new DonorProfile();
        dp.setId(donorId);
        dp.setEntityName("Grand Palace Hotel");
        dp.setDonorType("HOTEL_RESTAURANT");
        dp.setVerificationStatus(VerificationStatus.PENDING);
        dp.setUser(u);

        when(receiverOrganizationRepository.findById(donorId)).thenReturn(Optional.empty());
        when(donorProfileRepository.findById(donorId)).thenReturn(Optional.of(dp));
        when(donorProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateVerificationStatusRequest req = new UpdateVerificationStatusRequest("APPROVED", "Verified FSSAI license");

        AdminVerificationResponse res = adminVerificationService.updateVerificationStatus(donorId, req, "admin-uid", null);

        assertEquals("APPROVED", res.status());
        assertEquals("DONOR", res.role());
        assertEquals(UserStatus.ACTIVE, u.getStatus());
        assertEquals(VerificationStatus.APPROVED, u.getVerificationStatus());
        verify(auditService).logEvent(eq("admin-uid"), eq("ADMIN"), eq("ORGANIZATION_VERIFICATION_ADJUDICATED"), eq("DonorProfile"), eq(donorId.toString()), any(), any());
    }

    @Test
    @DisplayName("Admin searches verifications with role, category, and status filters")
    void testAdminSearchesVerificationsWithFilters() {
        User u1 = new User();
        u1.setId(UUID.randomUUID());
        u1.setName("Chef Mario");
        u1.setRole(UserRole.DONOR);
        u1.setCategory("HOTEL_RESTAURANT");
        u1.setVerificationStatus(VerificationStatus.PENDING);

        when(userRepository.searchVerifications(any(), eq(UserRole.DONOR), eq(VerificationStatus.PENDING), eq("HOTEL_RESTAURANT"), eq("Mario"), any()))
                .thenReturn(new PageImpl<>(List.of(u1), PageRequest.of(0, 10), 1));

        DonorProfile dp = new DonorProfile();
        dp.setId(UUID.randomUUID());
        dp.setEntityName("Mario Bistro");
        dp.setDonorType("HOTEL_RESTAURANT");
        dp.setUser(u1);
        when(donorProfileRepository.findByUserId(u1.getId())).thenReturn(Optional.of(dp));

        Page<AdminVerificationResponse> result = adminVerificationService.getVerifications(
                "PENDING",
                "DONOR",
                "HOTEL_RESTAURANT",
                "Mario",
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        AdminVerificationResponse item = result.getContent().get(0);
        assertEquals("DONOR", item.role());
        assertEquals("HOTEL_RESTAURANT", item.category());
        assertEquals("Mario Bistro", item.organizationName());
    }
}

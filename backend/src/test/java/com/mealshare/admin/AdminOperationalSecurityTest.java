package com.mealshare.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseToken;
import com.mealshare.admin.controller.*;
import com.mealshare.admin.dto.*;
import com.mealshare.admin.service.*;
import com.mealshare.common.exception.GlobalExceptionHandler;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.config.CorsConfig;
import com.mealshare.config.SecurityConfig;
import com.mealshare.security.CustomAuthenticationEntryPoint;
import com.mealshare.security.FirebaseAuthenticationFilter;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AdminDashboardController.class,
        AdminUserController.class,
        AdminDonationController.class,
        AdminRequestController.class,
        AdminVerificationController.class,
        AdminReportController.class,
        AdminAuditLogController.class
})
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AdminOperationalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AdminDonationService adminDonationService;

    @MockBean
    private AdminRequestService adminRequestService;

    @MockBean
    private AdminVerificationService adminVerificationService;

    @MockBean
    private AdminReportService adminReportService;

    @MockBean
    private AdminAuditLogService adminAuditLogService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private UserRepository userRepository;

    private User adminUser;
    private User donorUser;
    private User receiverUser;

    @BeforeEach
    void setUp() throws Exception {
        // Admin
        FirebaseToken adminToken = Mockito.mock(FirebaseToken.class);
        when(adminToken.getUid()).thenReturn("admin-uid");
        when(firebaseTokenValidator.verifyIdToken("token-admin")).thenReturn(adminToken);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFirebaseUid("admin-uid");
        adminUser.setEmail("admin@mealshare.org");
        adminUser.setName("Admin User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByFirebaseUid("admin-uid")).thenReturn(Optional.of(adminUser));

        // Donor
        FirebaseToken donorToken = Mockito.mock(FirebaseToken.class);
        when(donorToken.getUid()).thenReturn("donor-uid");
        when(firebaseTokenValidator.verifyIdToken("token-donor")).thenReturn(donorToken);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-uid");
        donorUser.setEmail("donor@mealshare.org");
        donorUser.setName("Donor User");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByFirebaseUid("donor-uid")).thenReturn(Optional.of(donorUser));

        // Receiver Org
        FirebaseToken receiverToken = Mockito.mock(FirebaseToken.class);
        when(receiverToken.getUid()).thenReturn("receiver-uid");
        when(firebaseTokenValidator.verifyIdToken("token-receiver")).thenReturn(receiverToken);

        receiverUser = new User();
        receiverUser.setId(UUID.randomUUID());
        receiverUser.setFirebaseUid("receiver-uid");
        receiverUser.setEmail("receiver@mealshare.org");
        receiverUser.setName("Receiver Org");
        receiverUser.setRole(UserRole.RECEIVER);
        receiverUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByFirebaseUid("receiver-uid")).thenReturn(Optional.of(receiverUser));
    }

    @Test
    @DisplayName("Unauthenticated request to admin dashboard returns 401 Unauthorized")
    void unauthenticated_dashboard_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Non-admin DONOR role accessing dashboard returns 403 Forbidden")
    void donor_dashboard_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer token-donor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin RECEIVER role accessing users returns 403 Forbidden")
    void receiver_users_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer token-receiver"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin RECEIVER role accessing donations returns 403 Forbidden")
    void receiver_donations_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/donations")
                        .header("Authorization", "Bearer token-receiver"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin DONOR role accessing requests returns 403 Forbidden")
    void donor_requests_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/requests")
                        .header("Authorization", "Bearer token-donor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin RECEIVER accessing verification returns 403 Forbidden")
    void receiver_verification_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/verification")
                        .header("Authorization", "Bearer token-receiver"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin RECEIVER accessing reports returns 403 Forbidden")
    void receiver_reports_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .header("Authorization", "Bearer token-receiver"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-admin DONOR accessing audit logs returns 403 Forbidden")
    void donor_auditLogs_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer token-donor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin accessing dashboard succeeds with 200 OK")
    void admin_dashboard_returns200() throws Exception {
        var dashboard = new AdminDashboardResponse(
                new AdminDashboardResponse.UserMetrics(10, 5, 4, 1),
                new AdminDashboardResponse.DonationMetrics(20, 10, 2, 5, 3, 1, 1, 500.0, 200.0, 150.0),
                new AdminDashboardResponse.RequestMetrics(15, 5, 6, 2, 1, 1),
                new AdminDashboardResponse.PickupMetrics(8, 3, 2, 3, 0),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        when(adminDashboardService.getDashboardStats()).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.total").value(10))
                .andExpect(jsonPath("$.donations.total").value(20));
    }

    @Test
    @DisplayName("Admin requesting non-existent user returns 404 Not Found")
    void admin_userNotFound_returns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(adminUserService.getUserDetail(randomId))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + randomId));

        mockMvc.perform(get("/api/v1/admin/users/" + randomId)
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin requesting non-existent donation returns 404 Not Found")
    void admin_donationNotFound_returns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(adminDonationService.getDonationDetail(randomId))
                .thenThrow(new ResourceNotFoundException("Donation not found with id: " + randomId));

        mockMvc.perform(get("/api/v1/admin/donations/" + randomId)
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin requesting non-existent request returns 404 Not Found")
    void admin_requestNotFound_returns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(adminRequestService.getRequestDetail(randomId))
                .thenThrow(new ResourceNotFoundException("Donation request not found with id: " + randomId));

        mockMvc.perform(get("/api/v1/admin/requests/" + randomId)
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin requesting non-existent verification returns 404 Not Found")
    void admin_verificationNotFound_returns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(adminVerificationService.getVerificationDetail(randomId))
                .thenThrow(new ResourceNotFoundException("Verification not found with id: " + randomId));

        mockMvc.perform(get("/api/v1/admin/verification/" + randomId)
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin updating user status succeeds with 200 OK")
    void admin_updateUserStatus_success() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        var updateReq = new UpdateUserStatusRequest(UserStatus.SUSPENDED, "Terms violation");
        var updatedResponse = new AdminUserResponse(
                targetUserId,
                "Target User",
                "target@example.com",
                "+919876543210",
                UserRole.DONOR,
                UserStatus.SUSPENDED,
                false,
                "Target Org",
                Instant.now(),
                Instant.now()
        );

        when(adminUserService.updateUserStatus(eq(targetUserId), any(), eq("admin-uid"), any()))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/v1/admin/users/" + targetUserId + "/status")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("Security invariant: Admin User response never leaks secrets, firebase tokens or raw passwords")
    void securityInvariant_noSensitiveDataExposed() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        var userDetail = new AdminUserDetailResponse(
                targetUserId,
                "Donor Org",
                "donor@example.com",
                "+919876543210",
                UserRole.DONOR,
                UserStatus.ACTIVE,
                true,
                Instant.now(),
                Instant.now(),
                Map.of("entityName", "Donor Org", "address", "42 MG Road")
        );
        when(adminUserService.getUserDetail(targetUserId)).thenReturn(userDetail);

        mockMvc.perform(get("/api/v1/admin/users/" + targetUserId)
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("firebaseUid"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    @DisplayName("Admin removing user succeeds with 204 No Content")
    void admin_removeUser_success() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        doNothing().when(adminUserService).removeUser(eq(targetUserId), eq("admin-uid"), any());

        mockMvc.perform(delete("/api/v1/admin/users/" + targetUserId)
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isNoContent());

        verify(adminUserService).removeUser(eq(targetUserId), eq("admin-uid"), any());
    }

    @Test
    @DisplayName("Non-admin role cannot delete user: returns 403 Forbidden")
    void donor_removeUser_forbidden() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/users/" + targetUserId)
                        .header("Authorization", "Bearer token-donor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to remove user returns 401 Unauthorized")
    void unauthenticated_removeUser_unauthorized() throws Exception {
        UUID targetUserId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/users/" + targetUserId))
                .andExpect(status().isUnauthorized());
    }
}

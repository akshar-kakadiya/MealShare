package com.mealshare.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseToken;
import com.mealshare.admin.dto.AdminVerificationResponse;
import com.mealshare.admin.service.AdminVerificationService;
import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.security.FirebaseTokenValidator;
import com.mealshare.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminVerificationService adminVerificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private ReceiverOrganizationRepository receiverOrganizationRepository;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    private static final String ADMIN_UID = "uSkzPzgPUVQBRES9AaDsF1ONqEG2";

    @BeforeEach
    void setUp() throws Exception {
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(ADMIN_UID);
        when(firebaseTokenValidator.verifyIdToken(anyString())).thenReturn(mockToken);
    }

    @Test
    @DisplayName("GET /api/v1/admin/verification returns 200 OK and valid paginated structure")
    void testGetVerificationsHttpEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/verification")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer valid-admin-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    @DisplayName("Service getVerifications handles null, empty, and 'All' filter parameters")
    void testGetVerificationsNullAndAllFilters() {
        // Default with nulls
        Page<AdminVerificationResponse> p1 = adminVerificationService.getVerifications(
                null, null, null, null, PageRequest.of(0, 20)
        );
        assertNotNull(p1);

        // "All" string values from frontend dropdowns
        Page<AdminVerificationResponse> p2 = adminVerificationService.getVerifications(
                "All", "All", "All", "", PageRequest.of(0, 20)
        );
        assertNotNull(p2);
        assertEquals(p1.getTotalElements(), p2.getTotalElements());
    }

    @Test
    @DisplayName("Service getVerifications handles status filters correctly")
    void testGetVerificationsStatusFilters() {
        for (String st : new String[]{"PENDING", "APPROVED", "REJECTED", "ACTION_REQUIRED"}) {
            Page<AdminVerificationResponse> page = adminVerificationService.getVerifications(
                    st, null, null, null, PageRequest.of(0, 20)
            );
            assertNotNull(page);
            page.getContent().forEach(item -> assertEquals(st, item.status()));
        }
    }

    @Test
    @DisplayName("Service getVerifications handles role filters correctly")
    void testGetVerificationsRoleFilters() {
        Page<AdminVerificationResponse> donors = adminVerificationService.getVerifications(
                null, "DONOR", null, null, PageRequest.of(0, 20)
        );
        assertNotNull(donors);
        donors.getContent().forEach(item -> assertEquals("DONOR", item.role()));

        Page<AdminVerificationResponse> receivers = adminVerificationService.getVerifications(
                null, "RECEIVER", null, null, PageRequest.of(0, 20)
        );
        assertNotNull(receivers);
        receivers.getContent().forEach(item -> assertEquals("RECEIVER", item.role()));
    }

    @Test
    @DisplayName("Service getVerifications handles category and search filters correctly")
    void testGetVerificationsCategoryAndSearchFilters() {
        Page<AdminVerificationResponse> categoryResults = adminVerificationService.getVerifications(
                null, null, "HOTEL_RESTAURANT", null, PageRequest.of(0, 20)
        );
        assertNotNull(categoryResults);

        Page<AdminVerificationResponse> searchResults = adminVerificationService.getVerifications(
                null, null, null, "admin", PageRequest.of(0, 20)
        );
        assertNotNull(searchResults);
    }

    @Test
    @DisplayName("Service getVerifications paginates correctly")
    void testGetVerificationsPagination() {
        Page<AdminVerificationResponse> page0 = adminVerificationService.getVerifications(
                null, null, null, null, PageRequest.of(0, 2)
        );
        assertNotNull(page0);
        assertTrue(page0.getSize() <= 2);

        Page<AdminVerificationResponse> page1 = adminVerificationService.getVerifications(
                null, null, null, null, PageRequest.of(1, 2)
        );
        assertNotNull(page1);
    }
}

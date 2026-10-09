package com.mealshare.security;

import com.google.firebase.auth.FirebaseToken;
import com.mealshare.user.User;
import com.mealshare.user.UserController;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import com.mealshare.user.UserService;
import com.mealshare.config.SecurityConfig;
import com.mealshare.config.CorsConfig;
import com.mealshare.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, CustomAuthenticationEntryPoint.class, CorsConfig.class})
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private FirebaseTokenValidator firebaseTokenValidator;

    @MockBean
    private com.mealshare.user.UserRepository userRepository;

    private FirebaseToken mockToken;
    private User mockUser;

    @BeforeEach
    void setUp() throws Exception {
        mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("valid-uid");
        when(mockToken.getEmail()).thenReturn("test@example.com");
        
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setFirebaseUid("valid-uid");
        mockUser.setRole(UserRole.DONOR);
        mockUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void protectedEndpointShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void invalidBearerTokenShouldReturn401() throws Exception {
        when(firebaseTokenValidator.verifyIdToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validToken_UserNotFound_ShouldReturn404ForMe() throws Exception {
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.empty());
        when(userService.findByFirebaseUid("valid-uid")).thenReturn(Optional.empty());

        // /me allows any authenticated Firebase user; UserController returns 404 if not registered in PostgreSQL
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validToken_UserFound_ShouldReturn200ForMe() throws Exception {
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));
        
        UserResponse response = UserResponse.fromEntity(mockUser);
        when(userService.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }
    
    @Test
    void suspendedUser_ShouldReturn403() throws Exception {
        mockUser.setStatus(UserStatus.SUSPENDED);
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));
        when(userService.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(UserResponse.fromEntity(mockUser)));

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void donorCannotAccessAdminEndpoint() throws Exception {
        mockUser.setRole(UserRole.DONOR);
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/v1/admin/test")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiverCannotAccessAdminEndpoint() throws Exception {
        mockUser.setRole(UserRole.RECEIVER);
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/v1/admin/test")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiverCannotAccessDonorEndpoint() throws Exception {
        mockUser.setRole(UserRole.RECEIVER);
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/v1/donor/test")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoint_ButReturns404BecauseItDoesNotExist() throws Exception {
        mockUser.setRole(UserRole.ADMIN);
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/v1/admin/test")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void legacyVolunteerEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/volunteer/pickups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyVolunteerEndpoint_authenticated_returns404() throws Exception {
        mockUser.setRole(UserRole.RECEIVER);
        when(firebaseTokenValidator.verifyIdToken("valid-token")).thenReturn(mockToken);
        when(userRepository.findByFirebaseUid("valid-uid")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/v1/volunteer/pickups")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }
}

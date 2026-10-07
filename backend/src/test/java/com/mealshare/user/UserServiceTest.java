package com.mealshare.user;

import com.mealshare.security.FirebasePrincipal;
import com.mealshare.user.dto.RegistrationRequest;
import com.mealshare.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.mealshare.donor.DonorProfileRepository donorProfileRepository;

    @Mock
    private com.mealshare.receiver.ReceiverOrganizationRepository receiverOrganizationRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_WhenAdminRole_ThrowsException() {
        FirebasePrincipal principal = new FirebasePrincipal("uid-1", "test@test.com", "Test User", Map.of());
        RegistrationRequest request = new RegistrationRequest("Test", "test@test.com", "123", UserRole.ADMIN);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser(principal, request));
        assertEquals("ADMIN role cannot be publicly registered", ex.getMessage());
    }

    @Test
    void registerUser_WhenEmailMismatch_ThrowsException() {
        FirebasePrincipal principal = new FirebasePrincipal("uid-1", "test@test.com", "Test User", Map.of());
        RegistrationRequest request = new RegistrationRequest("Test", "spoofed@test.com", "123", UserRole.DONOR);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser(principal, request));
        assertEquals("Requested email does not match authenticated Firebase token email", ex.getMessage());
    }

    @Test
    void registerUser_WhenDuplicateFirebaseUid_ThrowsException() {
        FirebasePrincipal principal = new FirebasePrincipal("uid-1", "test@test.com", "Test User", Map.of());
        RegistrationRequest request = new RegistrationRequest("Test", "test@test.com", "123", UserRole.DONOR);
        when(userRepository.existsByFirebaseUid("uid-1")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, 
            () -> userService.registerUser(principal, request));
        assertEquals("User profile already exists for this Firebase account", ex.getMessage());
    }

    @Test
    void registerUser_WhenDuplicateEmail_ThrowsException() {
        FirebasePrincipal principal = new FirebasePrincipal("uid-1", "test@test.com", "Test User", Map.of());
        RegistrationRequest request = new RegistrationRequest("Test", "test@test.com", "123", UserRole.DONOR);
        when(userRepository.existsByFirebaseUid("uid-1")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, 
            () -> userService.registerUser(principal, request));
        assertEquals("Email is already registered", ex.getMessage());
    }

    @Test
    void registerUser_Success() {
        FirebasePrincipal principal = new FirebasePrincipal("uid-1", "test@test.com", "Test User", Map.of());
        RegistrationRequest request = new RegistrationRequest("Test User", "test@test.com", "1234567890", UserRole.DONOR);
        when(userRepository.existsByFirebaseUid("uid-1")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setFirebaseUid("uid-1");
        savedUser.setEmail("test@test.com");
        savedUser.setName("Test User");
        savedUser.setRole(UserRole.DONOR);
        savedUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.registerUser(principal, request);
        assertNotNull(response);
        assertEquals("test@test.com", response.email());
        assertEquals(UserRole.DONOR, response.role());
    }
}

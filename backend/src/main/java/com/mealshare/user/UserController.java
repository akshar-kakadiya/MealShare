package com.mealshare.user;

import com.mealshare.security.FirebasePrincipal;
import com.mealshare.user.dto.RegistrationRequest;
import com.mealshare.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile based on their Firebase UID")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal FirebasePrincipal principal) {
        return userService.findByFirebaseUid(principal.uid())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user profile", description = "Creates a MealShare profile for a newly authenticated Firebase user")
    public ResponseEntity<?> registerUser(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody RegistrationRequest request) {
        
        try {
            UserResponse response = userService.registerUser(principal, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}

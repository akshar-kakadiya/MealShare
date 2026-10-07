package com.mealshare.donor;

import com.mealshare.donor.dto.DonorProfileRequest;
import com.mealshare.donor.dto.DonorProfileResponse;
import com.mealshare.security.FirebasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/donor/profile")
@Tag(name = "Donor Profile", description = "Endpoints for managing donor profile details")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("hasRole('DONOR')")
public class DonorProfileController {

    private final DonorProfileService donorProfileService;

    public DonorProfileController(DonorProfileService donorProfileService) {
        this.donorProfileService = donorProfileService;
    }

    @GetMapping
    @Operation(summary = "Get current donor profile", description = "Retrieves the profile information for the authenticated donor.")
    public ResponseEntity<DonorProfileResponse> getProfile(@AuthenticationPrincipal FirebasePrincipal principal) {
        DonorProfileResponse response = donorProfileService.getProfile(principal.uid());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update donor profile", description = "Updates profile metadata, address, and coordinates for the authenticated donor.")
    public ResponseEntity<DonorProfileResponse> updateProfile(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody DonorProfileRequest request) {
        DonorProfileResponse response = donorProfileService.updateProfile(principal.uid(), request);
        return ResponseEntity.ok(response);
    }
}

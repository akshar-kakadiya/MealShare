package com.mealshare.receiver;

import com.mealshare.receiver.dto.ReceiverOrganizationRequest;
import com.mealshare.receiver.dto.ReceiverOrganizationResponse;
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
@RequestMapping("/api/v1/receiver/profile")
@Tag(name = "Receiver Profile", description = "Endpoints for managing institutional receiver organization profile")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverProfileController {

    private final ReceiverOrganizationService receiverOrganizationService;

    public ReceiverProfileController(ReceiverOrganizationService receiverOrganizationService) {
        this.receiverOrganizationService = receiverOrganizationService;
    }

    @GetMapping
    @Operation(summary = "Get receiver profile", description = "Retrieves the receiver organization profile for the authenticated receiver.")
    public ResponseEntity<ReceiverOrganizationResponse> getProfile(@AuthenticationPrincipal FirebasePrincipal principal) {
        ReceiverOrganizationResponse response = receiverOrganizationService.getProfile(principal.uid());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update receiver profile", description = "Updates organization profile details, coordinates, and operating capacity.")
    public ResponseEntity<ReceiverOrganizationResponse> updateProfile(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody ReceiverOrganizationRequest request) {
        ReceiverOrganizationResponse response = receiverOrganizationService.updateProfile(principal.uid(), request);
        return ResponseEntity.ok(response);
    }
}

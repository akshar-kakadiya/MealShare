package com.mealshare.donation;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.donation.dto.CreateDonationRequest;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donation.dto.UpdateDonationRequest;
import com.mealshare.security.FirebasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donor/donations")
@Tag(name = "Donor Donations", description = "Endpoints for creating and managing surplus food donations")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("hasRole('DONOR')")
public class DonorDonationController {

    private final DonationService donationService;

    public DonorDonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    @Operation(summary = "Create surplus food donation", description = "Publishes a new surplus food donation listing with mandatory food safety declaration.")
    public ResponseEntity<DonationResponse> createDonation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody CreateDonationRequest request,
            HttpServletRequest httpRequest) {
        DonationResponse response = donationService.createDonation(principal.uid(), request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List donor donations", description = "Returns a paginated list of donations published by the authenticated donor, sorted by newest first by default.")
    public ResponseEntity<PaginatedResponse<DonationResponse>> listMyDonations(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DonationResponse> page = donationService.getMyDonations(principal.uid(), pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donation details", description = "Retrieves details of a specific donation owned by the authenticated donor.")
    public ResponseEntity<DonationResponse> getDonation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id) {
        DonationResponse response = donationService.getDonationById(principal.uid(), id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update donation", description = "Updates editable details of an active AVAILABLE donation owned by the authenticated donor.")
    public ResponseEntity<DonationResponse> updateDonation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDonationRequest request,
            HttpServletRequest httpRequest) {
        DonationResponse response = donationService.updateDonation(principal.uid(), id, request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel donation", description = "Cancels an active AVAILABLE donation owned by the authenticated donor.")
    public ResponseEntity<DonationResponse> cancelDonation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        DonationResponse response = donationService.cancelDonation(principal.uid(), id, httpRequest);
        return ResponseEntity.ok(response);
    }
}

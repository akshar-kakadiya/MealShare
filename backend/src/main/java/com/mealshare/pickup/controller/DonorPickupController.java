package com.mealshare.pickup.controller;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.dto.DonorPickupOtpResponse;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.security.FirebasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donor/pickups")
@Tag(name = "Donor Pickups & Handover", description = "Endpoints for donors to track scheduled collections and view secure handover OTPs")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("hasRole('DONOR')")
public class DonorPickupController {

    private final PickupService pickupService;

    public DonorPickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @GetMapping
    @Operation(summary = "List donor pickups", description = "Retrieves all scheduled pickups for donations published by the authenticated donor.")
    public ResponseEntity<PaginatedResponse<PickupResponse>> getMyPickups(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PickupResponse> page = pickupService.getPickupsForDonor(principal.uid(), pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/by-donation/{donationId}")
    @Operation(summary = "List pickups for donation", description = "Retrieves all pickups associated with a specific donation owned by the authenticated donor.")
    public ResponseEntity<List<PickupResponse>> getPickupsForDonation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID donationId) {
        List<PickupResponse> list = pickupService.getPickupsForDonation(principal.uid(), donationId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{pickupId}")
    @Operation(summary = "Get donor pickup detail", description = "Retrieves details of a specific pickup for the authenticated donor.")
    public ResponseEntity<PickupResponse> getPickupDetail(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId) {
        PickupResponse response = pickupService.getPickupDetailForDonor(principal.uid(), pickupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pickupId}/otp")
    @Operation(summary = "View pickup handover OTP", description = "Retrieves the active 6-digit handover OTP for this pickup. Available ONLY to the owning donor before physical pickup occurs.")
    public ResponseEntity<DonorPickupOtpResponse> getPickupOtp(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            HttpServletRequest httpRequest) {
        DonorPickupOtpResponse response = pickupService.getOtpForDonor(principal.uid(), pickupId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pickupId}/tracking")
    @Operation(summary = "Get live tracking for active pickup journey")
    public ResponseEntity<com.mealshare.pickup.dto.LiveTrackingDto> getLiveTracking(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId) {
        com.mealshare.pickup.dto.LiveTrackingDto tracking = pickupService.getLiveTracking(principal.uid(), pickupId);
        return ResponseEntity.ok(tracking);
    }
}

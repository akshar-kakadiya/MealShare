package com.mealshare.pickup.controller;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.pickup.dto.VerifyOtpRequest;
import com.mealshare.security.FirebasePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receiver/pickups")
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverPickupController {

    private final PickupService pickupService;

    public ReceiverPickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<PickupResponse>> getMyPickups(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PickupResponse> page = pickupService.getPickupsForReceiver(principal.uid(), pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{pickupId}")
    public ResponseEntity<PickupResponse> getPickupDetail(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId) {
        PickupResponse response = pickupService.getPickupDetailForReceiver(principal.uid(), pickupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/verify-otp")
    public ResponseEntity<PickupResponse> verifyOtp(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.verifyOtp(principal.uid(), pickupId, request.otp(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/complete")
    public ResponseEntity<PickupResponse> completePickup(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.completePickup(principal.uid(), pickupId, httpRequest);
        return ResponseEntity.ok(response);
    }
}

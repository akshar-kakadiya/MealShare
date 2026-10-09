package com.mealshare.pickup.controller;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.dto.*;
import com.mealshare.security.FirebasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receiver/pickups")
@Tag(name = "Receiver Pickups & Operations", description = "Endpoints for receiver pickup lifecycle: journey, geofence, inspection, OTP, delivery, and completion")
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverPickupController {

    private final PickupService pickupService;

    public ReceiverPickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @GetMapping
    @Operation(summary = "List receiver pickups")
    public ResponseEntity<PaginatedResponse<PickupResponse>> getMyPickups(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PickupResponse> page = pickupService.getPickupsForReceiver(principal.uid(), pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{pickupId}")
    @Operation(summary = "Get receiver pickup details")
    public ResponseEntity<PickupResponse> getPickupDetail(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId) {
        PickupResponse response = pickupService.getPickupDetailForReceiver(principal.uid(), pickupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ensure-for-request/{requestId}")
    @Operation(summary = "Ensure pickup exists for approved request", description = "Initializes pickup for an approved request if not yet generated")
    public ResponseEntity<PickupResponse> ensurePickupForRequest(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID requestId) {
        Pickup pickup = pickupService.ensurePickupForRequestId(requestId);
        return ResponseEntity.ok(PickupResponse.fromEntity(pickup));
    }

    @PostMapping("/{pickupId}/start-journey")
    @Operation(summary = "Start pickup journey", description = "Receiver starts navigating toward the donor pickup location. Activates real GPS tracking.")
    public ResponseEntity<PickupResponse> startJourney(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @RequestBody(required = false) LocationUpdateDto location,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.startPickupJourney(principal.uid(), pickupId, location, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/location")
    @Operation(summary = "Send live GPS update", description = "Streams current receiver GPS coordinates and computes geofence proximity")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @Valid @RequestBody LocationUpdateDto location,
            HttpServletRequest httpRequest) {
        Map<String, Object> result = pickupService.recordLocationUpdate(principal.uid(), pickupId, location, httpRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{pickupId}/reached-location")
    @Operation(summary = "Confirm reached pickup location", description = "Receiver confirms physical arrival at donor premises")
    public ResponseEntity<PickupResponse> reachedLocation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @RequestBody(required = false) LocationUpdateDto location,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.confirmReachedLocation(principal.uid(), pickupId, location, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/inspect-food")
    @Operation(summary = "Record food inspection outcome", description = "Receiver records on-site food safety/quality acceptance or rejection")
    public ResponseEntity<PickupResponse> inspectFood(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @Valid @RequestBody FoodInspectionDto dto,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.inspectFood(principal.uid(), pickupId, dto, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/verify-and-pickup")
    @Operation(summary = "Verify OTP and complete physical pickup", description = "Validates 6-digit handover OTP, records food photo evidence and pickup GPS")
    public ResponseEntity<PickupResponse> verifyAndPickup(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @Valid @RequestBody VerifyAndPickupDto dto,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.verifyAndPickup(principal.uid(), pickupId, dto, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/start-delivery")
    @Operation(summary = "Start in-transit delivery", description = "Transitions pickup to IN_TRANSIT as receiver travels to distribution destination")
    public ResponseEntity<PickupResponse> startDelivery(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @RequestBody(required = false) LocationUpdateDto location,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.startDelivery(principal.uid(), pickupId, location, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/complete-delivery")
    @Operation(summary = "Complete delivery", description = "Records destination delivery photo evidence, confirmed quantity, and terminates live tracking")
    public ResponseEntity<PickupResponse> completeDelivery(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @Valid @RequestBody CompleteDeliveryDto dto,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.completeDelivery(principal.uid(), pickupId, dto, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pickupId}/tracking")
    @Operation(summary = "Get live journey tracking status")
    public ResponseEntity<LiveTrackingDto> getLiveTracking(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId) {
        LiveTrackingDto tracking = pickupService.getLiveTracking(principal.uid(), pickupId);
        return ResponseEntity.ok(tracking);
    }

    // Backwards-compatibility endpoints
    @PostMapping("/{pickupId}/verify-otp")
    @Operation(summary = "Verify OTP (Legacy)")
    public ResponseEntity<PickupResponse> verifyOtp(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.verifyOtp(principal.uid(), pickupId, request.otp(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pickupId}/complete")
    @Operation(summary = "Complete pickup (Legacy)")
    public ResponseEntity<PickupResponse> completePickup(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId,
            HttpServletRequest httpRequest) {
        PickupResponse response = pickupService.completePickup(principal.uid(), pickupId, httpRequest);
        return ResponseEntity.ok(response);
    }
}

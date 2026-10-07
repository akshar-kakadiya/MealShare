package com.mealshare.pickup.controller;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.pickup.PickupService;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.pickup.dto.AdminAssignPickupResponse;
import com.mealshare.pickup.dto.AssignPickupRequest;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.security.FirebasePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pickups")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPickupController {

    private final PickupService pickupService;

    public AdminPickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @PostMapping
    public ResponseEntity<AdminAssignPickupResponse> schedulePickup(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody AssignPickupRequest requestDto,
            HttpServletRequest httpRequest) {
        AdminAssignPickupResponse response = pickupService.schedulePickup(principal.uid(), requestDto, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<PickupResponse>> getPickups(
            @RequestParam(required = false) PickupStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PickupResponse> page = pickupService.getAdminPickups(status, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{pickupId}")
    public ResponseEntity<PickupResponse> getPickupDetail(
            @PathVariable UUID pickupId) {
        PickupResponse response = pickupService.getAdminPickupDetail(pickupId);
        return ResponseEntity.ok(response);
    }
}

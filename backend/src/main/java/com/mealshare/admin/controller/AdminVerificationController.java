package com.mealshare.admin.controller;

import com.mealshare.admin.dto.AdminVerificationResponse;
import com.mealshare.admin.dto.UpdateVerificationStatusRequest;
import com.mealshare.admin.service.AdminVerificationService;
import com.mealshare.common.dto.PaginatedResponse;
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
@RequestMapping("/api/v1/admin/verification")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    public AdminVerificationController(AdminVerificationService adminVerificationService) {
        this.adminVerificationService = adminVerificationService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminVerificationResponse>> getVerifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminVerificationResponse> page = adminVerificationService.getVerifications(status, search, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminVerificationResponse> getVerificationDetail(@PathVariable UUID id) {
        AdminVerificationResponse response = adminVerificationService.getVerificationDetail(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminVerificationResponse> updateVerificationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVerificationStatusRequest request,
            @AuthenticationPrincipal FirebasePrincipal principal,
            HttpServletRequest httpRequest
    ) {
        AdminVerificationResponse response = adminVerificationService.updateVerificationStatus(id, request, principal.uid(), httpRequest);
        return ResponseEntity.ok(response);
    }
}

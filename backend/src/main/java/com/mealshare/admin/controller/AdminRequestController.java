package com.mealshare.admin.controller;

import com.mealshare.admin.dto.AdminDonationRequestDetailResponse;
import com.mealshare.admin.dto.AdminDonationRequestResponse;
import com.mealshare.admin.service.AdminRequestService;
import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.receiver.request.DonationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRequestController {

    private final AdminRequestService adminRequestService;

    public AdminRequestController(AdminRequestService adminRequestService) {
        this.adminRequestService = adminRequestService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminDonationRequestResponse>> getRequests(
            @RequestParam(required = false) DonationRequestStatus status,
            @RequestParam(required = false) UUID donationId,
            @RequestParam(required = false) UUID receiverOrgId,
            @RequestParam(required = false) UUID donorId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminDonationRequestResponse> page = adminRequestService.getRequests(status, donationId, receiverOrgId, donorId, search, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDonationRequestDetailResponse> getRequestDetail(@PathVariable UUID id) {
        AdminDonationRequestDetailResponse response = adminRequestService.getRequestDetail(id);
        return ResponseEntity.ok(response);
    }
}

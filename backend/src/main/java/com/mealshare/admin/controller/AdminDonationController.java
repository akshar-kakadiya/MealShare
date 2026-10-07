package com.mealshare.admin.controller;

import com.mealshare.admin.dto.AdminDonationDetailResponse;
import com.mealshare.admin.dto.AdminDonationResponse;
import com.mealshare.admin.service.AdminDonationService;
import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/donations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDonationController {

    private final AdminDonationService adminDonationService;

    public AdminDonationController(AdminDonationService adminDonationService) {
        this.adminDonationService = adminDonationService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminDonationResponse>> getDonations(
            @RequestParam(required = false) DonationStatus status,
            @RequestParam(required = false) FoodCategory category,
            @RequestParam(required = false) UUID donorId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminDonationResponse> page = adminDonationService.getDonations(status, category, donorId, search, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDonationDetailResponse> getDonationDetail(@PathVariable UUID id) {
        AdminDonationDetailResponse response = adminDonationService.getDonationDetail(id);
        return ResponseEntity.ok(response);
    }
}

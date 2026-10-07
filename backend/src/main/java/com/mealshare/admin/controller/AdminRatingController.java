package com.mealshare.admin.controller;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.rating.RatingService;
import com.mealshare.rating.dto.AdminRatingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ratings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRatingController {

    private final RatingService ratingService;

    public AdminRatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminRatingResponse>> getAllRatings(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminRatingResponse> page = ratingService.getAllRatingsForAdmin(pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }
}

package com.mealshare.rating;

import com.mealshare.rating.dto.CreateRatingRequest;
import com.mealshare.rating.dto.PickupRatingStatusResponse;
import com.mealshare.rating.dto.RatingResponse;
import com.mealshare.security.FirebasePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DONOR', 'RECEIVER')")
    public ResponseEntity<RatingResponse> submitRating(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody CreateRatingRequest request,
            HttpServletRequest httpRequest) {
        RatingResponse response = ratingService.submitRating(principal.uid(), request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pickup/{pickupId}")
    @PreAuthorize("hasAnyRole('DONOR', 'RECEIVER', 'ADMIN')")
    public ResponseEntity<PickupRatingStatusResponse> getPickupRatingStatus(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID pickupId) {
        PickupRatingStatusResponse response = ratingService.getPickupRatingStatus(principal.uid(), pickupId);
        return ResponseEntity.ok(response);
    }
}

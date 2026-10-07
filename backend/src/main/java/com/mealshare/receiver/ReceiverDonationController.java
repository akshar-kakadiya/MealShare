package com.mealshare.receiver;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.donation.FoodCategory;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.receiver.dto.NearbyDonationResponse;
import com.mealshare.receiver.request.DonationRequestService;
import com.mealshare.receiver.request.dto.CreateDonationRequestDto;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
import com.mealshare.security.FirebasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/receiver/donations")
@Tag(name = "Receiver Donations & Discovery", description = "Endpoints for nearby donation discovery, details inspection, and request creation")
@SecurityRequirement(name = "firebase-auth")
public class ReceiverDonationController {

    private final ReceiverDiscoveryService receiverDiscoveryService;
    private final DonationRequestService donationRequestService;

    public ReceiverDonationController(ReceiverDiscoveryService receiverDiscoveryService,
                                      DonationRequestService donationRequestService) {
        this.receiverDiscoveryService = receiverDiscoveryService;
        this.donationRequestService = donationRequestService;
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('RECEIVER')")
    @Operation(summary = "Discover nearby donations", description = "Executes native PostGIS spatial query to find active AVAILABLE non-expired surplus donations within a given radius in kilometers.")
    public ResponseEntity<PaginatedResponse<NearbyDonationResponse>> findNearby(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Parameter(description = "Center latitude (-90 to 90)", required = true)
            @RequestParam Double latitude,
            @Parameter(description = "Center longitude (-180 to 180)", required = true)
            @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometers (max 100 km)", required = true)
            @RequestParam Double radiusKm,
            @Parameter(description = "Optional food category filter")
            @RequestParam(required = false) FoodCategory foodCategory,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NearbyDonationResponse> page = receiverDiscoveryService.findNearbyDonations(
                principal.uid(), latitude, longitude, radiusKm, foodCategory, pageable
        );
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RECEIVER')")
    @Operation(summary = "View donation details", description = "Retrieves complete food safety, pickup timing, and location details of an active donation.")
    public ResponseEntity<DonationResponse> getDonationDetails(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id) {
        DonationResponse response = receiverDiscoveryService.getDonationDetailsForReceiver(principal.uid(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{donationId}/requests")
    @PreAuthorize("hasRole('RECEIVER')")
    @Operation(summary = "Request donation", description = "Submits a formal donation claim on behalf of an authenticated receiver organization. Strictly restricted to institutional receivers.")
    public ResponseEntity<DonationRequestResponse> createRequest(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID donationId,
            @Valid @RequestBody CreateDonationRequestDto requestDto,
            HttpServletRequest httpRequest) {
        DonationRequestResponse response = donationRequestService.createRequest(
                principal.uid(), donationId, requestDto, httpRequest
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

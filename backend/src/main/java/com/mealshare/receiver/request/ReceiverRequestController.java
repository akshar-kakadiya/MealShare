package com.mealshare.receiver.request;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.receiver.request.dto.DonationRequestResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receiver/requests")
@Tag(name = "Receiver Requests", description = "Endpoints for managing institutional receiver donation requests and withdrawals")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("hasRole('RECEIVER')")
public class ReceiverRequestController {

    private final DonationRequestService donationRequestService;

    public ReceiverRequestController(DonationRequestService donationRequestService) {
        this.donationRequestService = donationRequestService;
    }

    @GetMapping
    @Operation(summary = "List receiver requests", description = "Retrieves a paginated list of donation requests submitted by the authenticated receiver organization.")
    public ResponseEntity<PaginatedResponse<DonationRequestResponse>> listMyRequests(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DonationRequestResponse> page = donationRequestService.getMyRequests(principal.uid(), pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request details", description = "Retrieves details of a specific donation request owned by the authenticated receiver organization.")
    public ResponseEntity<DonationRequestResponse> getRequest(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id) {
        DonationRequestResponse response = donationRequestService.getRequestById(principal.uid(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw request", description = "Withdraws an active PENDING donation request submitted by the authenticated receiver organization.")
    public ResponseEntity<DonationRequestResponse> withdrawRequest(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        DonationRequestResponse response = donationRequestService.withdrawRequest(principal.uid(), id, httpRequest);
        return ResponseEntity.ok(response);
    }
}

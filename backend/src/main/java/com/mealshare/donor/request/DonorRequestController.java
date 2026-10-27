package com.mealshare.donor.request;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.donor.request.dto.ApproveDonationRequestResponse;
import com.mealshare.donor.request.dto.DonorDonationRequestResponse;
import com.mealshare.donor.request.dto.ReceiverProfilePreviewDto;
import com.mealshare.donor.request.dto.RejectDonationRequestDto;
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
@RequestMapping("/api/v1/donor")
@Tag(name = "Donor Request Review & Allocation", description = "Endpoints for reviewing receiver donation claims, FCFS allocation approval, and rejection")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("hasRole('DONOR')")
public class DonorRequestController {

    private final DonorRequestService donorRequestService;

    public DonorRequestController(DonorRequestService donorRequestService) {
        this.donorRequestService = donorRequestService;
    }

    @GetMapping("/donations/{donationId}/requests")
    @Operation(summary = "List requests for donation", description = "Retrieves a paginated list of receiver requests for a donation owned by the authenticated donor, ordered FCFS (requestedAt ASC, id ASC).")
    public ResponseEntity<PaginatedResponse<DonorDonationRequestResponse>> getRequestsForDonation(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID donationId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DonorDonationRequestResponse> page = donorRequestService.getRequestsForDonation(
                principal.uid(), donationId, pageable
        );
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/requests/{requestId}/receiver-profile")
    @Operation(summary = "View receiver organization profile preview for request",
               description = "Enables donors to inspect genuine receiver organization profile details before approving or rejecting a request")
    public ResponseEntity<ReceiverProfilePreviewDto> getReceiverProfilePreview(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID requestId) {
        ReceiverProfilePreviewDto response = donorRequestService.getReceiverProfilePreview(
                principal.uid(), requestId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{requestId}/approve")
    @Operation(summary = "Approve donation request", description = "Atomically allocates quantity and approves a pending donation claim following strict FCFS arrival ordering. Enforces pessimistic locking to prevent over-allocation.")
    public ResponseEntity<ApproveDonationRequestResponse> approveRequest(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID requestId,
            HttpServletRequest httpRequest) {
        ApproveDonationRequestResponse response = donorRequestService.approveRequest(
                principal.uid(), requestId, httpRequest
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{requestId}/reject")
    @Operation(summary = "Reject donation request", description = "Rejects a pending donation request submitted by a receiver organization, optionally recording a rejection reason.")
    public ResponseEntity<DonorDonationRequestResponse> rejectRequest(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) RejectDonationRequestDto rejectDto,
            HttpServletRequest httpRequest) {
        DonorDonationRequestResponse response = donorRequestService.rejectRequest(
                principal.uid(), requestId, rejectDto, httpRequest
        );
        return ResponseEntity.ok(response);
    }
}

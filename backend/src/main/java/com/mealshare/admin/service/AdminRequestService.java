package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminDonationRequestDetailResponse;
import com.mealshare.admin.dto.AdminDonationRequestResponse;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminRequestService {

    private final DonationRequestRepository donationRequestRepository;
    private final PickupRepository pickupRepository;

    public AdminRequestService(
            DonationRequestRepository donationRequestRepository,
            PickupRepository pickupRepository
    ) {
        this.donationRequestRepository = donationRequestRepository;
        this.pickupRepository = pickupRepository;
    }

    public Page<AdminDonationRequestResponse> getRequests(
            DonationRequestStatus status,
            UUID donationId,
            UUID receiverOrgId,
            UUID donorId,
            String search,
            Pageable pageable
    ) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<DonationRequest> requestsPage = donationRequestRepository.searchRequests(status, donationId, receiverOrgId, donorId, cleanSearch, pageable);
        return requestsPage.map(AdminDonationRequestResponse::fromEntity);
    }

    public AdminDonationRequestDetailResponse getRequestDetail(UUID requestId) {
        DonationRequest request = donationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found with id: " + requestId));

        Optional<Pickup> pickupOpt = pickupRepository.findByDonationRequestId(requestId);
        PickupResponse pickupResponse = pickupOpt.map(PickupResponse::fromEntity).orElse(null);

        return AdminDonationRequestDetailResponse.of(request, pickupResponse);
    }
}

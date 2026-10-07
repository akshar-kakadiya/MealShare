package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminDonationDetailResponse;
import com.mealshare.admin.dto.AdminDonationRequestResponse;
import com.mealshare.admin.dto.AdminDonationResponse;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.receiver.request.DonationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminDonationService {

    private final DonationRepository donationRepository;
    private final DonationRequestRepository donationRequestRepository;

    public AdminDonationService(
            DonationRepository donationRepository,
            DonationRequestRepository donationRequestRepository
    ) {
        this.donationRepository = donationRepository;
        this.donationRequestRepository = donationRequestRepository;
    }

    public Page<AdminDonationResponse> getDonations(
            DonationStatus status,
            FoodCategory category,
            UUID donorId,
            String search,
            Pageable pageable
    ) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Donation> donationsPage = donationRepository.searchDonations(status, category, donorId, cleanSearch, pageable);
        return donationsPage.map(AdminDonationResponse::fromEntity);
    }

    public AdminDonationDetailResponse getDonationDetail(UUID donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        List<DonationRequest> requests = donationRequestRepository.findByDonationId(donationId);
        List<AdminDonationRequestResponse> requestResponses = requests.stream()
                .map(AdminDonationRequestResponse::fromEntity)
                .toList();

        return AdminDonationDetailResponse.of(donation, requestResponses);
    }
}

package com.mealshare.donor.request.dto;

import com.mealshare.donation.DonationStatus;
import com.mealshare.receiver.request.DonationRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ApproveDonationRequestResponse(
        UUID requestId,
        UUID donationId,
        Double allocatedQuantity,
        Double totalAllocatedQuantity,
        Double remainingQuantity,
        DonationStatus donationStatus,
        DonationRequestStatus requestStatus,
        Instant allocatedAt
) {}

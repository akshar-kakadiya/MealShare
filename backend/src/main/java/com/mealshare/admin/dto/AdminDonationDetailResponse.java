package com.mealshare.admin.dto;

import com.mealshare.donation.Donation;

import java.util.List;

public record AdminDonationDetailResponse(
        AdminDonationResponse donation,
        List<AdminDonationRequestResponse> requests
) {
    public static AdminDonationDetailResponse of(Donation donation, List<AdminDonationRequestResponse> requests) {
        return new AdminDonationDetailResponse(
                AdminDonationResponse.fromEntity(donation),
                requests
        );
    }
}

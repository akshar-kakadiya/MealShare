package com.mealshare.admin.dto;

import com.mealshare.pickup.dto.PickupResponse;
import com.mealshare.receiver.request.DonationRequest;

public record AdminDonationRequestDetailResponse(
        AdminDonationRequestResponse request,
        PickupResponse pickup
) {
    public static AdminDonationRequestDetailResponse of(DonationRequest request, PickupResponse pickup) {
        return new AdminDonationRequestDetailResponse(
                AdminDonationRequestResponse.fromEntity(request),
                pickup
        );
    }
}

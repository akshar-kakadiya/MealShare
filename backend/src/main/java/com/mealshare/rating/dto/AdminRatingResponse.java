package com.mealshare.rating.dto;

import com.mealshare.rating.Rating;
import java.time.Instant;
import java.util.UUID;

public record AdminRatingResponse(
        UUID id,
        UUID pickupId,
        UUID donationId,
        String donationTitle,
        UUID raterUserId,
        String raterName,
        String raterRole,
        UUID ratedUserId,
        String ratedName,
        String ratedRole,
        Integer rating,
        String feedback,
        Instant createdAt
) {
    public static AdminRatingResponse fromEntity(Rating rating) {
        String donationTitle = null;
        UUID donationId = null;
        if (rating.getPickup() != null &&
            rating.getPickup().getDonationRequest() != null &&
            rating.getPickup().getDonationRequest().getDonation() != null) {
            donationTitle = rating.getPickup().getDonationRequest().getDonation().getTitle();
            donationId = rating.getPickup().getDonationRequest().getDonation().getId();
        }
        return new AdminRatingResponse(
                rating.getId(),
                rating.getPickup().getId(),
                donationId,
                donationTitle,
                rating.getRaterUser().getId(),
                rating.getRaterUser().getName(),
                rating.getRaterUser().getRole() != null ? rating.getRaterUser().getRole().name() : null,
                rating.getRatedUser().getId(),
                rating.getRatedUser().getName(),
                rating.getRatedUser().getRole() != null ? rating.getRatedUser().getRole().name() : null,
                rating.getRating(),
                rating.getFeedback(),
                rating.getCreatedAt()
        );
    }
}

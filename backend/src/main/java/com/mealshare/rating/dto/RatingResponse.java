package com.mealshare.rating.dto;

import com.mealshare.rating.Rating;
import java.time.Instant;
import java.util.UUID;

public record RatingResponse(
        UUID id,
        UUID pickupId,
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
    public static RatingResponse fromEntity(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getPickup().getId(),
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

package com.mealshare.rating.dto;

import java.util.List;
import java.util.UUID;

public record PickupRatingStatusResponse(
        UUID pickupId,
        String pickupStatus,
        boolean eligibleToRate,
        boolean alreadyRated,
        String roleInPickup, // "DONOR", "RECEIVER", or null
        RatingResponse myRating,
        RatingResponse counterpartRating,
        List<RatingResponse> allRatings
) {}

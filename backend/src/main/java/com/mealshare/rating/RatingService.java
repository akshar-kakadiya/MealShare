package com.mealshare.rating;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.pickup.Pickup;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.rating.dto.AdminRatingResponse;
import com.mealshare.rating.dto.CreateRatingRequest;
import com.mealshare.rating.dto.PickupRatingStatusResponse;
import com.mealshare.rating.dto.RatingResponse;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.receiver.request.DonationRequest;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RatingRepository ratingRepository;
    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final ReceiverOrganizationRepository receiverOrganizationRepository;
    private final AuditService auditService;

    public RatingService(RatingRepository ratingRepository,
                         PickupRepository pickupRepository,
                         UserRepository userRepository,
                         DonorProfileRepository donorProfileRepository,
                         ReceiverOrganizationRepository receiverOrganizationRepository,
                         AuditService auditService) {
        this.ratingRepository = ratingRepository;
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.receiverOrganizationRepository = receiverOrganizationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RatingResponse submitRating(String firebaseUid, CreateRatingRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.pickupId() == null) {
            throw new IllegalArgumentException("Pickup ID is required.");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        // 1. Authenticate user
        User rater = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UID: " + firebaseUid));

        // 2. Validate rater role: must be DONOR or RECEIVER
        if (rater.getRole() != UserRole.DONOR && rater.getRole() != UserRole.RECEIVER) {
            throw new AccessDeniedException("Only Donors and Receivers can submit transaction ratings.");
        }

        // 3. Look up pickup
        Pickup pickup = pickupRepository.findById(request.pickupId())
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with ID: " + request.pickupId()));

        // 4. Validate pickup status: must be COMPLETED
        if (pickup.getStatus() != PickupStatus.COMPLETED) {
            throw new IllegalStateException("Ratings can only be submitted after pickup is completed. Current status: " + pickup.getStatus());
        }

        // 5. Identify pickup participants
        DonationRequest donationRequest = pickup.getDonationRequest();
        if (donationRequest == null || donationRequest.getDonation() == null || donationRequest.getDonation().getDonor() == null) {
            throw new IllegalStateException("Incomplete donation hierarchy for pickup: " + pickup.getId());
        }
        DonorProfile donorProfile = donationRequest.getDonation().getDonor();
        User donorUser = donorProfile.getUser();

        ReceiverOrganization receiverOrg = pickup.getReceiver();
        if (receiverOrg == null || receiverOrg.getUser() == null) {
            throw new IllegalStateException("Incomplete receiver hierarchy for pickup: " + pickup.getId());
        }
        User receiverUser = receiverOrg.getUser();

        // 6. Verify rater is a participant and determine counterpart
        User ratedUser;
        if (rater.getId().equals(donorUser.getId())) {
            ratedUser = receiverUser;
        } else if (rater.getId().equals(receiverUser.getId())) {
            ratedUser = donorUser;
        } else {
            throw new AccessDeniedException("You are not authorized to rate this pickup as you were not a participant.");
        }

        // 7. Prevent self-rating
        if (rater.getId().equals(ratedUser.getId())) {
            throw new IllegalArgumentException("Self-rating is strictly prohibited.");
        }

        // 8. Prevent duplicate rating
        if (ratingRepository.existsByPickupIdAndRaterUserId(pickup.getId(), rater.getId())) {
            throw new IllegalStateException("You have already submitted a rating for this pickup.");
        }

        // 9. Clean feedback string
        String feedback = request.feedback() != null ? request.feedback().trim() : null;
        if (feedback != null && feedback.isEmpty()) {
            feedback = null;
        }

        // 10. Persist rating
        Rating rating = new Rating(pickup, rater, ratedUser, request.rating(), feedback);
        Rating saved = ratingRepository.save(rating);

        // 11. Re-calculate and update rating average on rated entity
        updateRatedUserAverage(ratedUser);

        // 12. Audit log
        if (auditService != null) {
            String deltaState = String.format("{\"pickupId\":\"%s\",\"ratedUserId\":\"%s\",\"rating\":%d}",
                    pickup.getId(), ratedUser.getId(), saved.getRating());
            auditService.logEvent(
                    rater.getFirebaseUid(),
                    rater.getRole().name(),
                    "RATING_SUBMITTED",
                    "Rating",
                    saved.getId().toString(),
                    deltaState,
                    httpRequest
            );
        }

        log.info("Rating {} submitted by user {} ({}) for user {} ({}) on pickup {}",
                saved.getRating(), rater.getId(), rater.getRole(), ratedUser.getId(), ratedUser.getRole(), pickup.getId());

        return RatingResponse.fromEntity(saved);
    }

    public PickupRatingStatusResponse getPickupRatingStatus(String firebaseUid, UUID pickupId) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UID: " + firebaseUid));

        Pickup pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup not found with ID: " + pickupId));

        DonorProfile donorProfile = pickup.getDonationRequest().getDonation().getDonor();
        User donorUser = donorProfile.getUser();
        User receiverUser = pickup.getReceiver().getUser();

        boolean isDonor = user.getId().equals(donorUser.getId());
        boolean isReceiver = user.getId().equals(receiverUser.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isDonor && !isReceiver && !isAdmin) {
            throw new AccessDeniedException("Access denied: You are not authorized to view ratings for this pickup.");
        }

        String roleInPickup = isDonor ? "DONOR" : (isReceiver ? "RECEIVER" : (isAdmin ? "ADMIN" : null));
        boolean alreadyRated = ratingRepository.existsByPickupIdAndRaterUserId(pickup.getId(), user.getId());
        boolean eligibleToRate = (pickup.getStatus() == PickupStatus.COMPLETED) && (isDonor || isReceiver) && !alreadyRated;

        List<Rating> allRatings = ratingRepository.findByPickupId(pickup.getId());
        RatingResponse myRating = allRatings.stream()
                .filter(r -> r.getRaterUser().getId().equals(user.getId()))
                .findFirst()
                .map(RatingResponse::fromEntity)
                .orElse(null);

        RatingResponse counterpartRating = allRatings.stream()
                .filter(r -> !r.getRaterUser().getId().equals(user.getId()))
                .findFirst()
                .map(RatingResponse::fromEntity)
                .orElse(null);

        List<RatingResponse> allResponses = allRatings.stream()
                .map(RatingResponse::fromEntity)
                .toList();

        return new PickupRatingStatusResponse(
                pickup.getId(),
                pickup.getStatus().name(),
                eligibleToRate,
                alreadyRated,
                roleInPickup,
                myRating,
                counterpartRating,
                allResponses
        );
    }

    public Page<AdminRatingResponse> getAllRatingsForAdmin(Pageable pageable) {
        return ratingRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminRatingResponse::fromEntity);
    }

    private void updateRatedUserAverage(User ratedUser) {
        try {
            Double avg = ratingRepository.calculateAverageRatingForUser(ratedUser.getId());
            if (avg != null) {
                double rounded = Math.round(avg * 10.0) / 10.0;
                if (ratedUser.getRole() == UserRole.DONOR) {
                    donorProfileRepository.findByUserId(ratedUser.getId()).ifPresent(dp -> {
                        dp.setRatingAverage(rounded);
                        donorProfileRepository.save(dp);
                    });
                } else if (ratedUser.getRole() == UserRole.RECEIVER) {
                    receiverOrganizationRepository.findByUserId(ratedUser.getId()).ifPresent(ro -> {
                        ro.setRatingAverage(rounded);
                        receiverOrganizationRepository.save(ro);
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update rating average for user {}: {}", ratedUser.getId(), e.getMessage());
        }
    }
}

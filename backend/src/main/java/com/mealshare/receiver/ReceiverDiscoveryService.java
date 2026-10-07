package com.mealshare.receiver;

import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donation.FoodCategory;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donation.projection.NearbyDonationProjection;
import com.mealshare.receiver.dto.NearbyDonationResponse;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReceiverDiscoveryService {

    public static final double MAX_RADIUS_KM = 100.0;

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;

    public ReceiverDiscoveryService(DonationRepository donationRepository, UserRepository userRepository) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
    }

    public Page<NearbyDonationResponse> findNearbyDonations(
            String firebaseUid,
            Double latitude,
            Double longitude,
            Double radiusKm,
            FoodCategory foodCategory,
            Pageable pageable) {

        validateReceiverUser(firebaseUid);
        validateCoordinatesAndRadius(latitude, longitude, radiusKm);

        Double radiusMeters = radiusKm * 1000.0;
        String categoryStr = foodCategory != null ? foodCategory.name() : null;
        Instant now = Instant.now();

        Page<NearbyDonationProjection> projections = donationRepository.findNearbyAvailable(
                latitude,
                longitude,
                radiusMeters,
                categoryStr,
                now,
                pageable
        );

        return projections.map(NearbyDonationResponse::fromProjection);
    }

    public DonationResponse getDonationDetailsForReceiver(String firebaseUid, UUID donationId) {
        validateReceiverUser(firebaseUid);

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (donation.getStatus() == DonationStatus.CANCELLED || donation.getStatus() == DonationStatus.EXPIRED) {
            throw new ResourceNotFoundException("Donation is no longer active or available.");
        }

        return DonationResponse.fromEntity(donation);
    }

    private void validateReceiverUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.RECEIVER) {
            throw new AccessDeniedException("User does not have permission to access receiver discovery.");
        }
    }

    private void validateCoordinatesAndRadius(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
        if (radiusKm == null || radiusKm <= 0.0) {
            throw new IllegalArgumentException("Radius in kilometers must be strictly positive.");
        }
        if (radiusKm > MAX_RADIUS_KM) {
            throw new IllegalArgumentException("Radius cannot exceed maximum limit of " + (int) MAX_RADIUS_KM + " km.");
        }
    }
}

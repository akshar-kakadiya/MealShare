package com.mealshare.donor;

import com.mealshare.donor.dto.DonorProfileRequest;
import com.mealshare.donor.dto.DonorProfileResponse;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DonorProfileService {

    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;

    public DonorProfileService(DonorProfileRepository donorProfileRepository, UserRepository userRepository) {
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
    }

    public Optional<DonorProfile> findByUserFirebaseUid(String firebaseUid) {
        return donorProfileRepository.findByUserFirebaseUid(firebaseUid);
    }

    public Optional<DonorProfile> findByUserId(java.util.UUID userId) {
        return donorProfileRepository.findByUserId(userId);
    }

    @Transactional
    public DonorProfile getOrCreateProfileForUser(User user, String defaultAddress, Double latitude, Double longitude) {
        return donorProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            DonorProfile profile = new DonorProfile();
            profile.setUser(user);
            profile.setDonorType("INDIVIDUAL");
            profile.setEntityName(user.getName() != null && !user.getName().isBlank() ? user.getName() : "Anonymous Donor");
            profile.setAddressText(defaultAddress != null && !defaultAddress.isBlank() ? defaultAddress : "Not Specified");
            profile.setLatitude(latitude != null ? latitude : 0.0);
            profile.setLongitude(longitude != null ? longitude : 0.0);
            profile.setPolicyAccepted(true);
            profile.setRatingAverage(5.0);
            profile.setCompletedDonationsCount(0);
            return donorProfileRepository.save(profile);
        });
    }

    public DonorProfileResponse getProfile(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found for authenticated account"));
        DonorProfile profile = donorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Donor profile not found"));
        return DonorProfileResponse.fromEntity(profile);
    }

    @Transactional
    public DonorProfileResponse updateProfile(String firebaseUid, DonorProfileRequest request) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found for authenticated account"));

        DonorProfile profile = donorProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            DonorProfile newProfile = new DonorProfile();
            newProfile.setUser(user);
            return newProfile;
        });

        profile.setDonorType(request.donorType());
        profile.setEntityName(request.entityName());
        profile.setAddressText(request.addressText());
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        if (request.policyAccepted() != null) {
            profile.setPolicyAccepted(request.policyAccepted());
        }

        profile = donorProfileRepository.save(profile);
        return DonorProfileResponse.fromEntity(profile);
    }
}

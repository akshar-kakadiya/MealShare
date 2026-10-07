package com.mealshare.user;

import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.user.dto.RegistrationRequest;
import com.mealshare.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final ReceiverOrganizationRepository receiverOrganizationRepository;

    public UserService(UserRepository userRepository,
                       DonorProfileRepository donorProfileRepository,
                       ReceiverOrganizationRepository receiverOrganizationRepository) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.receiverOrganizationRepository = receiverOrganizationRepository;
    }

    public Optional<UserResponse> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .map(UserResponse::fromEntity);
    }

    public boolean existsByFirebaseUid(String firebaseUid) {
        return userRepository.existsByFirebaseUid(firebaseUid);
    }

    @Transactional
    public UserResponse registerUser(com.mealshare.security.FirebasePrincipal principal, RegistrationRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new IllegalArgumentException("ADMIN role cannot be publicly registered");
        }

        if (principal.email() != null && !principal.email().isEmpty()) {
            if (!principal.email().equalsIgnoreCase(request.email())) {
                throw new IllegalArgumentException("Requested email does not match authenticated Firebase token email");
            }
        }

        String firebaseUid = principal.uid();

        if (userRepository.existsByFirebaseUid(firebaseUid)) {
            throw new IllegalStateException("User profile already exists for this Firebase account");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email is already registered");
        }

        User user = new User();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);

        // Provision initial role profile entity
        if (request.role() == UserRole.DONOR && donorProfileRepository != null) {
            if (donorProfileRepository.findByUserId(user.getId()).isEmpty()) {
                DonorProfile dp = new DonorProfile();
                dp.setUser(user);
                dp.setDonorType("INDIVIDUAL");
                dp.setEntityName(user.getName() != null && !user.getName().isBlank() ? user.getName() : "Surplus Donor");
                dp.setAddressText("Not Specified");
                dp.setLatitude(12.9716);
                dp.setLongitude(77.5946);
                dp.setPolicyAccepted(true);
                dp.setRatingAverage(5.0);
                dp.setCompletedDonationsCount(0);
                donorProfileRepository.save(dp);
            }
        } else if (request.role() == UserRole.RECEIVER && receiverOrganizationRepository != null) {
            if (receiverOrganizationRepository.findByUserId(user.getId()).isEmpty()) {
                ReceiverOrganization ro = new ReceiverOrganization();
                ro.setUser(user);
                ro.setOrgType("COMMUNITY");
                ro.setOrgName(user.getName() != null && !user.getName().isBlank() ? user.getName() : "Community Shelter");
                ro.setRegistrationNumber("REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                ro.setAddressText("Not Specified");
                ro.setServiceLatitude(12.9716);
                ro.setServiceLongitude(77.5946);
                ro.setServiceRadiusMeters(10000.0);
                ro.setVerificationStatus("VERIFIED");
                ro.setBeneficiaryCapacity(50);
                ro.setRatingAverage(5.0);
                receiverOrganizationRepository.save(ro);
            }
        }

        return UserResponse.fromEntity(user);
    }
}

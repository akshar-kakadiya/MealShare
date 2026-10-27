package com.mealshare.receiver;

import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.receiver.dto.ReceiverOrganizationRequest;
import com.mealshare.receiver.dto.ReceiverOrganizationResponse;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReceiverOrganizationService {

    private final ReceiverOrganizationRepository receiverOrganizationRepository;
    private final UserRepository userRepository;

    public ReceiverOrganizationService(ReceiverOrganizationRepository receiverOrganizationRepository,
                                       UserRepository userRepository) {
        this.receiverOrganizationRepository = receiverOrganizationRepository;
        this.userRepository = userRepository;
    }

    public Optional<ReceiverOrganization> findByUserFirebaseUid(String firebaseUid) {
        return receiverOrganizationRepository.findByUserFirebaseUid(firebaseUid);
    }

    public Optional<ReceiverOrganization> findByUserId(UUID userId) {
        return receiverOrganizationRepository.findByUserId(userId);
    }

    @Transactional
    public ReceiverOrganization getOrCreateProfileForUser(User user, String defaultAddress, Double latitude, Double longitude) {
        return receiverOrganizationRepository.findByUserId(user.getId()).orElseGet(() -> {
            ReceiverOrganization org = new ReceiverOrganization();
            org.setUser(user);
            org.setOrgType("NGO");
            org.setOrgName(user.getName() != null && !user.getName().isBlank() ? user.getName() : "Community Shelter");
            org.setRegistrationNumber("REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            org.setAddressText(defaultAddress != null && !defaultAddress.isBlank() ? defaultAddress : "Not Specified");
            org.setServiceLatitude(latitude != null ? latitude : 12.9716);
            org.setServiceLongitude(longitude != null ? longitude : 77.5946);
            org.setServiceRadiusMeters(10000.0);
            org.setVerificationStatus("PENDING");
            org.setProfileStatus(com.mealshare.user.ProfileStatus.INCOMPLETE);
            org.setBeneficiaryCapacity(50);
            org.setRatingAverage(5.0);
            return receiverOrganizationRepository.save(org);
        });
    }

    public ReceiverOrganizationResponse getProfile(String firebaseUid) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization org = receiverOrganizationRepository.findByUserId(user.getId())
                .orElseGet(() -> getOrCreateProfileForUser(user, "Not Specified", 12.9716, 77.5946));
        return ReceiverOrganizationResponse.fromEntity(org);
    }

    @Transactional
    public ReceiverOrganizationResponse updateProfile(String firebaseUid, ReceiverOrganizationRequest request) {
        User user = validateAndGetReceiverUser(firebaseUid);
        ReceiverOrganization org = receiverOrganizationRepository.findByUserId(user.getId()).orElseGet(() -> {
            ReceiverOrganization newOrg = new ReceiverOrganization();
            newOrg.setUser(user);
            return newOrg;
        });

        org.setOrgType(request.orgType());
        org.setOrgName(request.orgName());
        org.setRegistrationNumber(request.registrationNumber());
        org.setAddressText(request.addressText());
        org.setServiceLatitude(request.serviceLatitude());
        org.setServiceLongitude(request.serviceLongitude());
        if (request.serviceRadiusMeters() != null) {
            org.setServiceRadiusMeters(request.serviceRadiusMeters());
        }
        if (request.beneficiaryCapacity() != null) {
            org.setBeneficiaryCapacity(request.beneficiaryCapacity());
        }
        org.setContactPerson(request.contactPerson());
        org.setDescription(request.description());
        org.setPhotos(request.photos());

        // Update user entity information
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setName(request.fullName());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        }
        user.setCategory(request.orgType());

        // Mark profile as COMPLETE when required fields are present
        boolean isComplete = request.orgName() != null && !request.orgName().isBlank()
                && request.addressText() != null && !request.addressText().isBlank()
                && request.serviceLatitude() != null && request.serviceLongitude() != null;
        if (isComplete) {
            org.setProfileStatus(com.mealshare.user.ProfileStatus.COMPLETE);
            user.setProfileStatus(com.mealshare.user.ProfileStatus.COMPLETE);
        } else {
            org.setProfileStatus(com.mealshare.user.ProfileStatus.INCOMPLETE);
            user.setProfileStatus(com.mealshare.user.ProfileStatus.INCOMPLETE);
        }

        // Resubmit verification if previously ACTION_REQUIRED or REJECTED
        if ("ACTION_REQUIRED".equalsIgnoreCase(org.getVerificationStatus()) ||
            "REJECTED".equalsIgnoreCase(org.getVerificationStatus()) ||
            user.getVerificationStatus() == com.mealshare.user.VerificationStatus.ACTION_REQUIRED ||
            user.getVerificationStatus() == com.mealshare.user.VerificationStatus.REJECTED) {
            org.setVerificationStatus("PENDING");
            org.setRejectionReason(null);
            org.setActionRequiredNotes(null);
            user.setVerificationStatus(com.mealshare.user.VerificationStatus.PENDING);
            user.setRejectionReason(null);
            user.setActionRequiredNotes(null);
        }

        userRepository.save(user);
        org = receiverOrganizationRepository.save(org);
        return ReceiverOrganizationResponse.fromEntity(org);
    }

    private User validateAndGetReceiverUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User account not found."));

        if (user.getRole() != UserRole.RECEIVER) {
            throw new AccessDeniedException("User does not possess receiver permissions.");
        }

        return user;
    }
}

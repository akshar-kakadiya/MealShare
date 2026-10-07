package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminUserDetailResponse;
import com.mealshare.admin.dto.AdminUserResponse;
import com.mealshare.admin.dto.UpdateUserStatusRequest;
import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final ReceiverOrganizationRepository receiverOrganizationRepository;
    private final AuditService auditService;

    public AdminUserService(
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository,
            ReceiverOrganizationRepository receiverOrganizationRepository,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.receiverOrganizationRepository = receiverOrganizationRepository;
        this.auditService = auditService;
    }

    public Page<AdminUserResponse> getUsers(UserRole role, UserStatus status, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<User> usersPage = userRepository.searchUsers(role, status, cleanSearch, pageable);

        return usersPage.map(user -> {
            String orgName = resolveOrganizationName(user);
            return AdminUserResponse.fromEntity(user, orgName);
        });
    }

    public AdminUserDetailResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> profileDetails = new HashMap<>();
        if (user.getRole() == UserRole.DONOR) {
            donorProfileRepository.findByUserId(user.getId()).ifPresent(dp -> {
                profileDetails.put("entityName", dp.getEntityName());
                profileDetails.put("donorType", dp.getDonorType());
                profileDetails.put("address", dp.getAddressText());
                profileDetails.put("completedDonations", dp.getCompletedDonationsCount());
            });
        } else if (user.getRole() == UserRole.RECEIVER) {
            receiverOrganizationRepository.findByUserId(user.getId()).ifPresent(ro -> {
                profileDetails.put("orgName", ro.getOrgName());
                profileDetails.put("orgType", ro.getOrgType());
                profileDetails.put("registrationNumber", ro.getRegistrationNumber());
                profileDetails.put("verificationStatus", ro.getVerificationStatus());
                profileDetails.put("capacity", ro.getBeneficiaryCapacity());
            });
        }

        return AdminUserDetailResponse.fromEntity(user, profileDetails);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request, String adminUid, HttpServletRequest httpRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserStatus previousStatus = user.getStatus();
        user.setStatus(request.status());
        user = userRepository.save(user);

        auditService.logEvent(
                adminUid,
                "ADMIN",
                "USER_STATUS_UPDATED",
                "User",
                user.getId().toString(),
                "{\"previousStatus\":\"" + previousStatus + "\",\"newStatus\":\"" + request.status() + "\",\"reason\":\"" + (request.reason() != null ? request.reason() : "") + "\"}",
                httpRequest
        );

        String orgName = resolveOrganizationName(user);
        return AdminUserResponse.fromEntity(user, orgName);
    }

    private String resolveOrganizationName(User user) {
        if (user.getRole() == UserRole.DONOR) {
            return donorProfileRepository.findByUserId(user.getId())
                    .map(com.mealshare.donor.DonorProfile::getEntityName)
                    .orElse(null);
        } else if (user.getRole() == UserRole.RECEIVER) {
            return receiverOrganizationRepository.findByUserId(user.getId())
                    .map(com.mealshare.receiver.ReceiverOrganization::getOrgName)
                    .orElse(null);
        }
        return null;
    }
}

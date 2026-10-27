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
import com.mealshare.user.VerificationStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminUserService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminUserService.class);

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

    @Transactional
    public void deleteUser(UUID userId, String adminUid, HttpServletRequest httpRequest) {
        removeUser(userId, adminUid, httpRequest);
    }

    @Transactional
    public void removeUser(UUID userId, String adminUid, HttpServletRequest httpRequest) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. Prevent removing the currently authenticated admin account
        Optional<User> adminUserOpt = userRepository.findByFirebaseUid(adminUid);
        if (targetUser.getFirebaseUid().equals(adminUid) ||
                (adminUserOpt.isPresent() && adminUserOpt.get().getId().equals(userId))) {
            throw new IllegalArgumentException("Cannot remove the currently authenticated administrator account.");
        }

        // 2. Prevent removing the last active administrator
        if (targetUser.getRole() == UserRole.ADMIN && targetUser.getStatus() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot remove the last active administrator account on the platform.");
            }
        }

        String targetFirebaseUid = targetUser.getFirebaseUid();
        String targetEmail = targetUser.getEmail();
        UserRole targetRole = targetUser.getRole();
        UserStatus targetStatus = targetUser.getStatus();

        // 3. Audit log event before deletion
        auditService.logEvent(
                adminUid,
                "ADMIN",
                "USER_DELETED_PERMANENTLY",
                "User",
                targetUser.getId().toString(),
                "{\"targetEmail\":\"" + targetEmail + "\",\"firebaseUid\":\"" + targetFirebaseUid + "\",\"role\":\"" + targetRole + "\",\"previousStatus\":\"" + targetStatus + "\"}",
                httpRequest
        );

        // 4. Delete user from PostgreSQL database (dependent records cascade via DB ON DELETE CASCADE)
        userRepository.delete(targetUser);
        userRepository.flush();

        // 5. Delete user identity from Firebase Authentication if initialized and available
        deleteFirebaseUserQuietly(targetFirebaseUid);
    }

    private void deleteFirebaseUserQuietly(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return;
        }
        try {
            if (!com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                com.google.firebase.auth.FirebaseAuth.getInstance().deleteUser(firebaseUid);
                log.info("Successfully deleted Firebase Auth user with UID: {}", firebaseUid);
            } else {
                log.warn("FirebaseApp is not initialized; skipped Firebase Auth deletion for UID: {}", firebaseUid);
            }
        } catch (com.google.firebase.auth.FirebaseAuthException fae) {
            if ("user-not-found".equals(fae.getErrorCode())) {
                log.warn("Firebase Auth user with UID {} not found in Firebase; skipping deletion.", firebaseUid);
            } else {
                log.error("Failed to delete Firebase Auth user with UID {}: {}", firebaseUid, fae.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error deleting Firebase Auth user with UID {}: {}", firebaseUid, e.getMessage());
        }
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

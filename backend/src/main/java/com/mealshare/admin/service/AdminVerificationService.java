package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminVerificationResponse;
import com.mealshare.admin.dto.UpdateVerificationStatusRequest;
import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.receiver.ReceiverOrganization;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminVerificationService {

    private final ReceiverOrganizationRepository receiverOrganizationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminVerificationService(
            ReceiverOrganizationRepository receiverOrganizationRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this(receiverOrganizationRepository, null, userRepository, auditService);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminVerificationService(
            ReceiverOrganizationRepository receiverOrganizationRepository,
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.receiverOrganizationRepository = receiverOrganizationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public Page<AdminVerificationResponse> getVerifications(String status, String search, Pageable pageable) {
        return getVerifications(status, null, null, search, pageable);
    }

    public Page<AdminVerificationResponse> getVerifications(String status, String role, String category, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) ? status.trim().toUpperCase() : null;
        String cleanRole = (role != null && !role.isBlank() && !role.equalsIgnoreCase("All")) ? role.trim().toUpperCase() : null;
        String cleanCategory = (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) ? category.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        if (donorProfileRepository == null) {
            Page<ReceiverOrganization> orgsPage = receiverOrganizationRepository.searchReceiverOrganizations(cleanStatus, cleanSearch, pageable);
            return orgsPage.map(AdminVerificationResponse::fromEntity);
        }

        UserRole roleFilter = null;
        if (cleanRole != null) {
            try {
                roleFilter = UserRole.valueOf(cleanRole);
            } catch (Exception ignored) {}
        }

        VerificationStatus verificationStatusFilter = null;
        if (cleanStatus != null) {
            try {
                verificationStatusFilter = VerificationStatus.valueOf(cleanStatus);
            } catch (Exception ignored) {}
        }

        List<UserRole> targetRoles = List.of(UserRole.DONOR, UserRole.RECEIVER);
        Page<User> usersPage = userRepository.searchVerifications(
                targetRoles,
                roleFilter,
                verificationStatusFilter,
                cleanCategory,
                cleanSearch,
                pageable
        );

        return usersPage.map(u -> {
            if (u.getRole() == UserRole.DONOR) {
                return donorProfileRepository.findByUserId(u.getId())
                        .map(dp -> AdminVerificationResponse.fromEntity(dp, u))
                        .orElseGet(() -> new AdminVerificationResponse(
                                u.getId(),
                                u.getId(),
                                "DONOR",
                                u.getCategory(),
                                u.getName(),
                                u.getCategory(),
                                u.getName(),
                                u.getEmail(),
                                u.getPhone(),
                                "Not specified",
                                null, null, null, null, null, null, null, null,
                                u.getProfileStatus() != null ? u.getProfileStatus().name() : "INCOMPLETE",
                                u.getVerificationStatus() != null ? u.getVerificationStatus().name() : "PENDING",
                                u.getRejectionReason(),
                                u.getActionRequiredNotes(),
                                u.getCreatedAt(),
                                u.getVerifiedAt(),
                                u.getVerifiedBy(),
                                null
                        ));
            } else {
                return receiverOrganizationRepository.findByUserId(u.getId())
                        .map(ro -> AdminVerificationResponse.fromEntity(ro, u))
                        .orElseGet(() -> new AdminVerificationResponse(
                                u.getId(),
                                u.getId(),
                                "RECEIVER",
                                u.getCategory(),
                                u.getName(),
                                u.getCategory(),
                                u.getName(),
                                u.getEmail(),
                                u.getPhone(),
                                "Not specified",
                                null, null, null, null, null, null, null, null,
                                u.getProfileStatus() != null ? u.getProfileStatus().name() : "INCOMPLETE",
                                u.getVerificationStatus() != null ? u.getVerificationStatus().name() : "PENDING",
                                u.getRejectionReason(),
                                u.getActionRequiredNotes(),
                                u.getCreatedAt(),
                                u.getVerifiedAt(),
                                u.getVerifiedBy(),
                                null
                        ));
            }
        });
    }

    public AdminVerificationResponse getVerificationDetail(UUID id) {
        Optional<ReceiverOrganization> org = receiverOrganizationRepository.findById(id);
        if (org.isPresent()) {
            return AdminVerificationResponse.fromEntity(org.get());
        }

        if (donorProfileRepository != null) {
            Optional<DonorProfile> donor = donorProfileRepository.findById(id);
            if (donor.isPresent()) {
                return AdminVerificationResponse.fromEntity(donor.get());
            }
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if (u.getRole() == UserRole.DONOR && donorProfileRepository != null) {
                return donorProfileRepository.findByUserId(u.getId())
                        .map(AdminVerificationResponse::fromEntity)
                        .orElseThrow(() -> new ResourceNotFoundException("Verification record not found with id: " + id));
            } else if (u.getRole() == UserRole.RECEIVER) {
                return receiverOrganizationRepository.findByUserId(u.getId())
                        .map(AdminVerificationResponse::fromEntity)
                        .orElseThrow(() -> new ResourceNotFoundException("Verification record not found with id: " + id));
            }
        }

        throw new ResourceNotFoundException("Verification record not found with id: " + id);
    }

    @Transactional
    public AdminVerificationResponse updateVerificationStatus(
            UUID id,
            UpdateVerificationStatusRequest request,
            String adminUid,
            HttpServletRequest httpRequest
    ) {
        String newStatus = request.status().toUpperCase();
        String rejectionReason = request.rejectionReason();
        String actionRequiredNotes = request.actionRequiredNotes() != null ? request.actionRequiredNotes() : request.notes();

        if ("REJECTED".equals(newStatus)) {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new IllegalArgumentException("A rejection reason is required when rejecting verification");
            }
        }

        if ("ACTION_REQUIRED".equals(newStatus)) {
            if (actionRequiredNotes == null || actionRequiredNotes.isBlank()) {
                throw new IllegalArgumentException("Action required notes or instructions are required when requesting changes");
            }
        }

        Optional<ReceiverOrganization> orgOpt = receiverOrganizationRepository.findById(id);
        if (orgOpt.isPresent()) {
            ReceiverOrganization org = orgOpt.get();
            if ("APPROVED".equals(newStatus) && org.getProfileStatus() != com.mealshare.user.ProfileStatus.COMPLETE) {
                throw new IllegalStateException("Cannot approve verification: profile is incomplete and not ready for review");
            }
            org.setVerificationStatus(newStatus);
            if ("APPROVED".equals(newStatus)) {
                org.setRejectionReason(null);
                org.setActionRequiredNotes(null);
            } else if ("REJECTED".equals(newStatus)) {
                org.setRejectionReason(rejectionReason);
                org.setActionRequiredNotes(null);
            } else if ("ACTION_REQUIRED".equals(newStatus)) {
                org.setActionRequiredNotes(actionRequiredNotes);
                org.setRejectionReason(null);
            }
            org.setVerifiedAt(Instant.now());
            org.setVerifiedBy(adminUid);
            org = receiverOrganizationRepository.save(org);

            if (org.getUser() != null) {
                User u = org.getUser();
                updateUserVerificationState(u, newStatus, rejectionReason, actionRequiredNotes, adminUid);
                userRepository.save(u);
            }

            auditService.logEvent(
                    adminUid,
                    "ADMIN",
                    "ORGANIZATION_VERIFICATION_ADJUDICATED",
                    "ReceiverOrganization",
                    org.getId().toString(),
                    "{\"status\":\"" + newStatus + "\",\"notes\":\"" + (request.notes() != null ? request.notes() : "") + "\"}",
                    httpRequest
            );

            return AdminVerificationResponse.fromEntity(org);
        }

        if (donorProfileRepository != null) {
            Optional<DonorProfile> donorOpt = donorProfileRepository.findById(id);
            if (donorOpt.isPresent()) {
                DonorProfile donor = donorOpt.get();
                if ("APPROVED".equals(newStatus) && donor.getProfileStatus() != com.mealshare.user.ProfileStatus.COMPLETE) {
                    throw new IllegalStateException("Cannot approve verification: profile is incomplete and not ready for review");
                }
                try {
                    donor.setVerificationStatus(VerificationStatus.valueOf(newStatus));
                } catch (Exception e) {
                    donor.setVerificationStatus(VerificationStatus.PENDING);
                }
                if ("APPROVED".equals(newStatus)) {
                    donor.setRejectionReason(null);
                    donor.setActionRequiredNotes(null);
                } else if ("REJECTED".equals(newStatus)) {
                    donor.setRejectionReason(rejectionReason);
                    donor.setActionRequiredNotes(null);
                } else if ("ACTION_REQUIRED".equals(newStatus)) {
                    donor.setActionRequiredNotes(actionRequiredNotes);
                    donor.setRejectionReason(null);
                }
                donor.setVerifiedAt(Instant.now());
                donor.setVerifiedBy(adminUid);
                donor = donorProfileRepository.save(donor);

                if (donor.getUser() != null) {
                    User u = donor.getUser();
                    updateUserVerificationState(u, newStatus, rejectionReason, actionRequiredNotes, adminUid);
                    userRepository.save(u);
                }

                auditService.logEvent(
                        adminUid,
                        "ADMIN",
                        "ORGANIZATION_VERIFICATION_ADJUDICATED",
                        "DonorProfile",
                        donor.getId().toString(),
                        "{\"status\":\"" + newStatus + "\",\"notes\":\"" + (request.notes() != null ? request.notes() : "") + "\"}",
                        httpRequest
                );

                return AdminVerificationResponse.fromEntity(donor);
            }
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if ("APPROVED".equals(newStatus) && u.getProfileStatus() != com.mealshare.user.ProfileStatus.COMPLETE) {
                throw new IllegalStateException("Cannot approve verification: profile is incomplete and not ready for review");
            }
            updateUserVerificationState(u, newStatus, rejectionReason, actionRequiredNotes, adminUid);
            u = userRepository.save(u);

            if (u.getRole() == UserRole.DONOR && donorProfileRepository != null) {
                Optional<DonorProfile> dp = donorProfileRepository.findByUserId(u.getId());
                if (dp.isPresent()) {
                    DonorProfile donor = dp.get();
                    if ("APPROVED".equals(newStatus) && donor.getProfileStatus() != com.mealshare.user.ProfileStatus.COMPLETE) {
                        throw new IllegalStateException("Cannot approve verification: profile is incomplete and not ready for review");
                    }
                    try {
                        donor.setVerificationStatus(VerificationStatus.valueOf(newStatus));
                    } catch (Exception ignored) {}
                    if ("APPROVED".equals(newStatus)) {
                        donor.setRejectionReason(null);
                        donor.setActionRequiredNotes(null);
                    } else if ("REJECTED".equals(newStatus)) {
                        donor.setRejectionReason(rejectionReason);
                        donor.setActionRequiredNotes(null);
                    } else if ("ACTION_REQUIRED".equals(newStatus)) {
                        donor.setActionRequiredNotes(actionRequiredNotes);
                        donor.setRejectionReason(null);
                    }
                    donor.setVerifiedAt(Instant.now());
                    donor.setVerifiedBy(adminUid);
                    donor = donorProfileRepository.save(donor);
                    return AdminVerificationResponse.fromEntity(donor);
                }
            } else if (u.getRole() == UserRole.RECEIVER) {
                Optional<ReceiverOrganization> ro = receiverOrganizationRepository.findByUserId(u.getId());
                if (ro.isPresent()) {
                    ReceiverOrganization org = ro.get();
                    if ("APPROVED".equals(newStatus) && org.getProfileStatus() != com.mealshare.user.ProfileStatus.COMPLETE) {
                        throw new IllegalStateException("Cannot approve verification: profile is incomplete and not ready for review");
                    }
                    org.setVerificationStatus(newStatus);
                    if ("APPROVED".equals(newStatus)) {
                        org.setRejectionReason(null);
                        org.setActionRequiredNotes(null);
                    } else if ("REJECTED".equals(newStatus)) {
                        org.setRejectionReason(rejectionReason);
                        org.setActionRequiredNotes(null);
                    } else if ("ACTION_REQUIRED".equals(newStatus)) {
                        org.setActionRequiredNotes(actionRequiredNotes);
                        org.setRejectionReason(null);
                    }
                    org.setVerifiedAt(Instant.now());
                    org.setVerifiedBy(adminUid);
                    org = receiverOrganizationRepository.save(org);
                    return AdminVerificationResponse.fromEntity(org);
                }
            }

            return new AdminVerificationResponse(
                    u.getId(),
                    u.getId(),
                    u.getRole().name(),
                    u.getCategory(),
                    u.getName(),
                    u.getCategory(),
                    u.getName(),
                    u.getEmail(),
                    u.getPhone(),
                    "Not specified",
                    null, null, null, null, null, null, null, null,
                    u.getProfileStatus() != null ? u.getProfileStatus().name() : "INCOMPLETE",
                    u.getVerificationStatus() != null ? u.getVerificationStatus().name() : "PENDING",
                    u.getRejectionReason(),
                    u.getActionRequiredNotes(),
                    u.getCreatedAt(),
                    u.getVerifiedAt(),
                    u.getVerifiedBy(),
                    null
            );
        }

        throw new ResourceNotFoundException("Verification record not found with id: " + id);
    }

    private void updateUserVerificationState(User u, String newStatus, String rejectionReason, String actionRequiredNotes, String adminUid) {
        try {
            u.setVerificationStatus(VerificationStatus.valueOf(newStatus));
        } catch (Exception ignored) {}
        u.setVerifiedAt(Instant.now());
        u.setVerifiedBy(adminUid);

        if ("APPROVED".equals(newStatus)) {
            u.setStatus(UserStatus.ACTIVE);
            u.setRejectionReason(null);
            u.setActionRequiredNotes(null);
        } else if ("REJECTED".equals(newStatus)) {
            u.setStatus(UserStatus.RESTRICTED);
            u.setRejectionReason(rejectionReason);
        } else if ("ACTION_REQUIRED".equals(newStatus)) {
            u.setStatus(UserStatus.ACTIVE);
            u.setActionRequiredNotes(actionRequiredNotes);
        }
    }
}

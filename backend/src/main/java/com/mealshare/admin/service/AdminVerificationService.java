package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminVerificationResponse;
import com.mealshare.admin.dto.UpdateVerificationStatusRequest;
import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.ReceiverOrganizationRepository;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminVerificationService {

    private final ReceiverOrganizationRepository receiverOrganizationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminVerificationService(
            ReceiverOrganizationRepository receiverOrganizationRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.receiverOrganizationRepository = receiverOrganizationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public Page<AdminVerificationResponse> getVerifications(String status, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("All")) ? status.trim().toUpperCase() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<ReceiverOrganization> orgsPage = receiverOrganizationRepository.searchReceiverOrganizations(cleanStatus, cleanSearch, pageable);
        return orgsPage.map(AdminVerificationResponse::fromEntity);
    }

    public AdminVerificationResponse getVerificationDetail(UUID id) {
        ReceiverOrganization org = receiverOrganizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verification record not found with id: " + id));
        return AdminVerificationResponse.fromEntity(org);
    }

    @Transactional
    public AdminVerificationResponse updateVerificationStatus(
            UUID id,
            UpdateVerificationStatusRequest request,
            String adminUid,
            HttpServletRequest httpRequest
    ) {
        ReceiverOrganization org = receiverOrganizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verification record not found with id: " + id));

        String newStatus = request.status().toUpperCase();
        org.setVerificationStatus(newStatus);
        org = receiverOrganizationRepository.save(org);

        if (org.getUser() != null) {
            if ("APPROVED".equals(newStatus)) {
                org.getUser().setStatus(UserStatus.ACTIVE);
                userRepository.save(org.getUser());
            } else if ("REJECTED".equals(newStatus)) {
                org.getUser().setStatus(UserStatus.RESTRICTED);
                userRepository.save(org.getUser());
            }
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
}

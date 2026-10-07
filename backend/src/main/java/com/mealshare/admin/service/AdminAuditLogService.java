package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminAuditLogResponse;
import com.mealshare.audit.AuditLog;
import com.mealshare.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AdminAuditLogResponse> getAuditLogs(
            String actionType,
            String actorUserId,
            String entityName,
            Pageable pageable
    ) {
        String cleanAction = (actionType != null && !actionType.isBlank()) ? actionType.trim() : null;
        String cleanActor = (actorUserId != null && !actorUserId.isBlank()) ? actorUserId.trim() : null;
        String cleanEntity = (entityName != null && !entityName.isBlank()) ? entityName.trim() : null;

        Page<AuditLog> page = auditLogRepository.searchAuditLogs(cleanAction, cleanActor, cleanEntity, pageable);
        return page.map(AdminAuditLogResponse::fromEntity);
    }
}

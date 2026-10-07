package com.mealshare.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog logEvent(String actorUserId,
                             String actorRole,
                             String actionType,
                             String entityName,
                             String entityId,
                             String deltaState,
                             HttpServletRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actorUserId);
        auditLog.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
        auditLog.setActionType(actionType);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setDeltaState(deltaState);

        if (request != null) {
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isBlank()) {
                clientIp = request.getRemoteAddr();
            }
            auditLog.setClientIp(clientIp);
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log recorded: action={}, entity={}, entityId={}, actor={}",
                actionType, entityName, entityId, actorUserId);
        return saved;
    }
}

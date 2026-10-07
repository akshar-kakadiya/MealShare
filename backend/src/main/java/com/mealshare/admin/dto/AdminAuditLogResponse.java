package com.mealshare.admin.dto;

import com.mealshare.audit.AuditLog;

import java.time.Instant;

public record AdminAuditLogResponse(
        Long id,
        String actorUserId,
        String actorRole,
        String actionType,
        String entityName,
        String entityId,
        String deltaState,
        Instant timestamp
) {
    public static AdminAuditLogResponse fromEntity(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorRole(),
                log.getActionType(),
                log.getEntityName(),
                log.getEntityId(),
                log.getDeltaState(),
                log.getTimestamp()
        );
    }
}

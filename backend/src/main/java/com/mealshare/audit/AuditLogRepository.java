package com.mealshare.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, String entityId);

    List<AuditLog> findByActorUserIdOrderByTimestampDesc(String actorUserId);

    List<AuditLog> findTop6ByOrderByTimestampDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:actorUserId IS NULL OR a.actorUserId = :actorUserId) AND " +
           "(:entityName IS NULL OR a.entityName = :entityName)")
    Page<AuditLog> searchAuditLogs(
            @Param("actionType") String actionType,
            @Param("actorUserId") String actorUserId,
            @Param("entityName") String entityName,
            Pageable pageable
    );
}

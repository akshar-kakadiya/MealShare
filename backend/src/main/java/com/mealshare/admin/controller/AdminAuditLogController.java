package com.mealshare.admin.controller;

import com.mealshare.admin.dto.AdminAuditLogResponse;
import com.mealshare.admin.service.AdminAuditLogService;
import com.mealshare.common.dto.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminAuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) String entityName,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminAuditLogResponse> page = adminAuditLogService.getAuditLogs(actionType, actorUserId, entityName, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }
}

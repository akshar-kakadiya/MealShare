package com.mealshare.admin.controller;

import com.mealshare.admin.dto.AdminReportSummaryResponse;
import com.mealshare.admin.service.AdminReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminReportSummaryResponse> getReportSummary(
            @RequestParam(required = false, defaultValue = "30d") String timeRange
    ) {
        AdminReportSummaryResponse response = adminReportService.getReportSummary(timeRange);
        return ResponseEntity.ok(response);
    }
}

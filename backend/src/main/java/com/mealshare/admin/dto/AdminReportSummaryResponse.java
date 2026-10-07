package com.mealshare.admin.dto;

import java.util.List;

public record AdminReportSummaryResponse(
        long totalDonations,
        long completedDonations,
        long activeDonations,
        double totalQuantityDonated,
        double totalAllocatedQuantity,
        double totalCompletedQuantity,
        long totalRequests,
        long pendingRequests,
        long completedPickups,
        long activeDonorsCount,
        long activeReceiversCount,
        List<AdminDashboardResponse.StatusCount> statusDistribution,
        List<TopDonorSummary> topDonors,
        List<MonthlyMetrics> monthlyData
) {
    public record TopDonorSummary(
            String donorName,
            long donationsCount,
            double totalQuantity
    ) {}

    public record MonthlyMetrics(
            String month,
            long donations,
            long pickups,
            double meals
    ) {}
}

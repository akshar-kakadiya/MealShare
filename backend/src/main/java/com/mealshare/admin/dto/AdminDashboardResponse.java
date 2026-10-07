package com.mealshare.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminDashboardResponse(
        UserMetrics users,
        DonationMetrics donations,
        RequestMetrics requests,
        PickupMetrics pickups,
        List<StatusCount> donationStatusDistribution,
        List<RecentDonationSummary> recentDonations,
        List<PendingVerificationSummary> pendingVerifications,
        List<RecentActivitySummary> recentActivity,
        List<DailyActivitySummary> dailyActivity
) {
    public record UserMetrics(
            long total,
            long donors,
            long receivers,
            long admins
    ) {}

    public record DonationMetrics(
            long total,
            long available,
            long requested,
            long approved,
            long completed,
            long cancelled,
            long expired,
            double totalQuantityDonated,
            double totalAllocatedQuantity,
            double totalCompletedQuantity
    ) {}

    public record RequestMetrics(
            long total,
            long pending,
            long approved,
            long rejected,
            long withdrawn,
            long completed
    ) {}

    public record PickupMetrics(
            long total,
            long assigned,
            long pickedUp,
            long completed,
            long cancelled
    ) {}

    public record StatusCount(
            String status,
            long count,
            String color
    ) {}

    public record RecentDonationSummary(
            UUID id,
            String title,
            String donorName,
            double quantity,
            String unit,
            String status,
            String createdAt
    ) {}

    public record PendingVerificationSummary(
            UUID id,
            String organizationName,
            String organizationType,
            String status,
            String submittedAt
    ) {}

    public record RecentActivitySummary(
            Long id,
            String action,
            String description,
            String user,
            String timestamp
    ) {}

    public record DailyActivitySummary(
            String day,
            long created,
            long completed
    ) {}
}

package com.mealshare.admin.service;

import com.mealshare.admin.dto.AdminDashboardResponse;
import com.mealshare.admin.dto.AdminReportSummaryResponse;
import com.mealshare.donation.Donation;
import com.mealshare.donation.DonationRepository;
import com.mealshare.donation.DonationStatus;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileRepository;
import com.mealshare.pickup.PickupRepository;
import com.mealshare.pickup.PickupStatus;
import com.mealshare.receiver.request.DonationRequestRepository;
import com.mealshare.receiver.request.DonationRequestStatus;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminReportService {

    private final DonationRepository donationRepository;
    private final DonationRequestRepository donationRequestRepository;
    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;

    public AdminReportService(
            DonationRepository donationRepository,
            DonationRequestRepository donationRequestRepository,
            PickupRepository pickupRepository,
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository
    ) {
        this.donationRepository = donationRepository;
        this.donationRequestRepository = donationRequestRepository;
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
    }

    public AdminReportSummaryResponse getReportSummary(String timeRange) {
        long totalDonations = donationRepository.count();
        long completedDonations = donationRepository.countByStatus(DonationStatus.COMPLETED);
        long activeDonations = donationRepository.countByStatus(DonationStatus.AVAILABLE);

        double totalQuantityDonated = donationRepository.sumTotalQuantity();
        double totalAllocatedQuantity = donationRepository.sumTotalAllocatedQuantity();
        double totalCompletedQuantity = donationRepository.sumCompletedQuantity();

        long totalRequests = donationRequestRepository.count();
        long pendingRequests = donationRequestRepository.countByStatus(DonationRequestStatus.PENDING);
        long completedPickups = pickupRepository.countByStatus(PickupStatus.COMPLETED);

        long activeDonorsCount = userRepository.countByRole(UserRole.DONOR);
        long activeReceiversCount = userRepository.countByRole(UserRole.RECEIVER);

        // Status distribution
        long approvedDonations = donationRepository.countByStatus(DonationStatus.APPROVED);
        long cancelledDonations = donationRepository.countByStatus(DonationStatus.CANCELLED);
        long expiredDonations = donationRepository.countByStatus(DonationStatus.EXPIRED);

        var statusDistribution = List.of(
                new AdminDashboardResponse.StatusCount("Available", activeDonations, "#10b981"),
                new AdminDashboardResponse.StatusCount("Approved", approvedDonations, "#3b82f6"),
                new AdminDashboardResponse.StatusCount("Completed", completedDonations, "#059669"),
                new AdminDashboardResponse.StatusCount("Cancelled", cancelledDonations, "#ef4444"),
                new AdminDashboardResponse.StatusCount("Expired", expiredDonations, "#6b7280")
        );

        // Top Donors from real entities
        List<Donation> allDonations = donationRepository.findAll();
        Map<DonorProfile, List<Donation>> donationsByDonor = allDonations.stream()
                .filter(d -> d.getDonor() != null)
                .collect(Collectors.groupingBy(Donation::getDonor));

        List<AdminReportSummaryResponse.TopDonorSummary> topDonors = donationsByDonor.entrySet().stream()
                .map(entry -> {
                    String donorName = entry.getKey().getEntityName();
                    long count = entry.getValue().size();
                    double totalQty = entry.getValue().stream().mapToDouble(d -> d.getQuantity() != null ? d.getQuantity() : 0.0).sum();
                    return new AdminReportSummaryResponse.TopDonorSummary(donorName, count, totalQty);
                })
                .sorted(Comparator.comparing(AdminReportSummaryResponse.TopDonorSummary::donationsCount).reversed())
                .limit(5)
                .toList();

        // Monthly aggregated data from donations
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault());
        Map<String, Double> monthlyMeals = new LinkedHashMap<>();
        Map<String, Long> monthlyDonationCounts = new LinkedHashMap<>();

        for (Donation d : allDonations) {
            if (d.getCreatedAt() != null) {
                String month = monthFormatter.format(d.getCreatedAt());
                monthlyMeals.merge(month, d.getQuantity() != null ? d.getQuantity() : 0.0, Double::sum);
                monthlyDonationCounts.merge(month, 1L, Long::sum);
            }
        }

        List<AdminReportSummaryResponse.MonthlyMetrics> monthlyData = new ArrayList<>();
        if (monthlyDonationCounts.isEmpty()) {
            String currentMonth = monthFormatter.format(java.time.Instant.now());
            monthlyData.add(new AdminReportSummaryResponse.MonthlyMetrics(currentMonth, totalDonations, completedPickups, totalQuantityDonated));
        } else {
            for (String month : monthlyDonationCounts.keySet()) {
                monthlyData.add(new AdminReportSummaryResponse.MonthlyMetrics(
                        month,
                        monthlyDonationCounts.getOrDefault(month, 0L),
                        completedPickups,
                        monthlyMeals.getOrDefault(month, 0.0)
                ));
            }
        }

        return new AdminReportSummaryResponse(
                totalDonations,
                completedDonations,
                activeDonations,
                totalQuantityDonated,
                totalAllocatedQuantity,
                totalCompletedQuantity,
                totalRequests,
                pendingRequests,
                completedPickups,
                activeDonorsCount,
                activeReceiversCount,
                statusDistribution,
                topDonors,
                monthlyData
        );
    }
}

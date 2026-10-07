import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/timeline_widget.dart';

class ReceiverPickupTrackingScreen extends StatelessWidget {
  final String requestId;
  const ReceiverPickupTrackingScreen({super.key, required this.requestId});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final req = state.requests.where((r) => r.id == requestId).firstOrNull;

    if (req == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Pickup Tracking')),
        body: const Center(child: Text('Request not found')),
      );
    }

    final donation = state.donations.where((d) => d.id == req.donationId).firstOrNull;
    final pickup = state.pickups.where((p) => p.donationId == req.donationId).firstOrNull;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Pickup Tracking'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          // Header
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Column(
                children: [
                  Container(
                    width: 60,
                    height: 60,
                    decoration: BoxDecoration(
                      color: AppColors.primarySurface,
                      borderRadius: BorderRadius.circular(AppRadius.lg),
                    ),
                    child: Icon(donation?.categoryIcon ?? Icons.restaurant, color: AppColors.primary, size: 30),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Text(donation?.title ?? 'Unknown', style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 4),
                  Text(donation?.id ?? '', style: Theme.of(context).textTheme.bodySmall),
                  const SizedBox(height: AppSpacing.sm),
                  StatusChip(status: req.status),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // Route
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Column(
                children: [
                  Row(
                    children: [
                      Container(
                        width: 36,
                        height: 36,
                        decoration: BoxDecoration(
                          color: AppColors.successSurface,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(Icons.store, size: 18, color: AppColors.success),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('From', style: Theme.of(context).textTheme.labelSmall),
                            Text(donation?.donorName ?? 'Donor', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                            Text(pickup?.pickupAddress ?? donation?.pickupAddress ?? '', style: Theme.of(context).textTheme.bodySmall),
                          ],
                        ),
                      ),
                    ],
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 18),
                    child: Container(
                      width: 2,
                      height: 24,
                      color: AppColors.borderLight,
                    ),
                  ),
                  Row(
                    children: [
                      Container(
                        width: 36,
                        height: 36,
                        decoration: BoxDecoration(
                          color: AppColors.infoSurface,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(Icons.location_on, size: 18, color: AppColors.info),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('To', style: Theme.of(context).textTheme.labelSmall),
                            Text(state.currentUser.name, style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                            Text(pickup?.deliveryAddress ?? state.currentUser.address, style: Theme.of(context).textTheme.bodySmall),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // Pickup Details
          if (pickup != null)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Pickup Details', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                    const SizedBox(height: AppSpacing.md),
                    _DetailRow(Icons.person, 'Receiver', pickup.receiverName.isNotEmpty ? pickup.receiverName : state.currentUser.name),
                    _DetailRow(Icons.schedule, 'Scheduled', '${pickup.scheduledTime.hour}:${pickup.scheduledTime.minute.toString().padLeft(2, '0')}'),
                    _DetailRow(Icons.location_on, 'Pickup Location', pickup.pickupAddress),
                    const SizedBox(height: AppSpacing.md),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        icon: const Icon(Icons.qr_code, size: 18),
                        label: const Text('Manage Pickup & Verify OTP'),
                        onPressed: () => context.push('/receiver/pickups'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: AppSpacing.lg),

          // Timeline
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Tracking Timeline', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                  const SizedBox(height: AppSpacing.lg),
                  TimelineWidget(
                    steps: const [
                      'Request Approved',
                      'Pickup Scheduled',
                      'Ready for Pickup',
                      'Pickup In Progress',
                      'Picked Up',
                      'Delivered',
                      'Completed',
                    ],
                    currentStep: _getPickupStep(pickup?.status ?? req.status),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.xxxl),
        ],
      ),
    );
  }

  int _getPickupStep(String status) {
    switch (status) {
      case 'APPROVED': return 1;
      case 'ASSIGNED': return 2;
      case 'READY': return 3;
      case 'PICKUP_IN_PROGRESS': return 4;
      case 'PICKED_UP': return 5;
      case 'DELIVERED': return 6;
      case 'COMPLETED': return 7;
      default: return 1;
    }
  }
}

class _DetailRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  const _DetailRow(this.icon, this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Icon(icon, size: 18, color: AppColors.textHint),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: Theme.of(context).textTheme.labelSmall),
              Text(value, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.textPrimary)),
            ],
          ),
        ],
      ),
    );
  }
}

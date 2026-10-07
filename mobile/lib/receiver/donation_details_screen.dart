import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/api_config.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/status_chip.dart';

class ReceiverDonationDetailsScreen extends StatelessWidget {
  final String id;
  const ReceiverDonationDetailsScreen({super.key, required this.id});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final donation = state.donations.where((d) => d.id == id).firstOrNull;

    if (donation == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Food Details')),
        body: const Center(child: Text('Donation not found')),
      );
    }

    final hasRequested = ApiConfig.isRealMode
        ? state.requests.any((r) => r.donationId == id)
        : state.requests.any((r) => r.donationId == id && r.receiverId == state.currentUser.id);
    final myRequest = ApiConfig.isRealMode
        ? state.requests.where((r) => r.donationId == id).firstOrNull
        : state.requests.where((r) => r.donationId == id && r.receiverId == state.currentUser.id).firstOrNull;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Food Details'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          // Header with icon
          Center(
            child: Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: AppColors.primarySurface,
                borderRadius: BorderRadius.circular(AppRadius.xl),
              ),
              child: Icon(donation.categoryIcon, color: AppColors.primary, size: 40),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          Center(
            child: Text(donation.title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w600), textAlign: TextAlign.center),
          ),
          const SizedBox(height: AppSpacing.sm),
          Center(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.store, size: 16, color: AppColors.textSecondary),
                const SizedBox(width: 4),
                Text(donation.donorName, style: Theme.of(context).textTheme.bodyMedium),
                if (donation.distance != null) ...[
                  const SizedBox(width: 12),
                  Icon(Icons.location_on, size: 16, color: AppColors.textSecondary),
                  const SizedBox(width: 2),
                  Text('${donation.distance} km', style: Theme.of(context).textTheme.bodyMedium),
                ],
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Center(child: StatusChip(status: hasRequested ? (myRequest?.status ?? donation.status) : donation.status)),
          const SizedBox(height: AppSpacing.xxl),

          // About This Food
          _Section(
            title: 'About This Food',
            icon: Icons.info_outline,
            children: [
              _InfoRow('Category', donation.category),
              _InfoRow('Quantity', '${donation.quantity} ${donation.unit}'),
              _InfoRow('Description', donation.description),
            ],
          ),

          // Food Safety
          _Section(
            title: 'Food Safety Information',
            icon: Icons.health_and_safety,
            children: [
              _InfoRow('Consume Before', donation.consumeBefore),
              _InfoRow('Storage', donation.storageCondition),
              _InfoRow('Packaging', donation.packagingCondition),
              if (donation.hasAllergens)
                _InfoRow('Allergens', donation.allergenInfo, isWarning: true),
              if (!donation.hasAllergens)
                _InfoRow('Allergens', 'No known allergens'),
            ],
          ),

          // Safety Declaration
          if (donation.foodSafetyDeclared)
            Container(
              margin: const EdgeInsets.only(top: AppSpacing.md),
              padding: const EdgeInsets.all(AppSpacing.md),
              decoration: BoxDecoration(
                color: AppColors.successSurface,
                borderRadius: BorderRadius.circular(AppRadius.md),
                border: Border.all(color: AppColors.success.withValues(alpha: 0.3)),
              ),
              child: Row(
                children: [
                  Icon(Icons.verified_user, color: AppColors.success, size: 20),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      'Donor has declared this food safe for consumption',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.success),
                    ),
                  ),
                ],
              ),
            ),

          const SizedBox(height: AppSpacing.md),

          // Pickup Information
          _Section(
            title: 'Pickup Information',
            icon: Icons.location_on,
            children: [
              if (donation.pickupAddress.isNotEmpty) _InfoRow('Address', donation.pickupAddress),
              if (donation.pickupWindowStart.isNotEmpty) _InfoRow('Pickup Window', '${donation.pickupWindowStart} - ${donation.pickupWindowEnd}'),
              if (donation.pickupInstructions.isNotEmpty) _InfoRow('Instructions', donation.pickupInstructions),
            ],
          ),

          const SizedBox(height: AppSpacing.xxxl + AppSpacing.xxxl),
        ],
      ),
      // CTA Button
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: hasRequested
              ? Container(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  decoration: BoxDecoration(
                    color: AppColors.infoSurface,
                    borderRadius: BorderRadius.circular(AppRadius.button),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.check_circle, color: AppColors.info),
                      const SizedBox(width: 8),
                      Text(
                        'Request ${myRequest?.status ?? "Submitted"}',
                        style: TextStyle(color: AppColors.info, fontWeight: FontWeight.w600, fontSize: 16),
                      ),
                    ],
                  ),
                )
              : ElevatedButton(
                  onPressed: donation.status == 'AVAILABLE'
                      ? () {
                          final qtyCtrl = TextEditingController(text: donation.quantity.toString());
                          final notesCtrl = TextEditingController();
                          showDialog(
                            context: context,
                            builder: (ctx) {
                              bool isSubmitting = false;
                              return StatefulBuilder(
                                builder: (dialogCtx, setDialogState) => AlertDialog(
                                  title: const Text('Request Food'),
                                  content: Column(
                                    mainAxisSize: MainAxisSize.min,
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Requesting from ${donation.donorName}: "${donation.title}"',
                                        style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                                      ),
                                      const SizedBox(height: 16),
                                      TextField(
                                        controller: qtyCtrl,
                                        keyboardType: TextInputType.number,
                                        enabled: !isSubmitting,
                                        decoration: InputDecoration(
                                          labelText: 'Quantity (${donation.unit})',
                                          border: const OutlineInputBorder(),
                                          helperText: 'Available: ${donation.quantity} ${donation.unit}',
                                        ),
                                      ),
                                      const SizedBox(height: 12),
                                      TextField(
                                        controller: notesCtrl,
                                        enabled: !isSubmitting,
                                        decoration: const InputDecoration(
                                          labelText: 'Notes / Reason for request',
                                          border: OutlineInputBorder(),
                                          hintText: 'e.g. Evening distribution for 50 people',
                                        ),
                                      ),
                                    ],
                                  ),
                                  actions: [
                                    TextButton(
                                      onPressed: isSubmitting ? null : () => Navigator.pop(dialogCtx),
                                      child: const Text('Cancel'),
                                    ),
                                    FilledButton(
                                      onPressed: isSubmitting
                                          ? null
                                          : () async {
                                              final q = double.tryParse(qtyCtrl.text.trim()) ?? donation.quantity.toDouble();
                                              setDialogState(() => isSubmitting = true);
                                              try {
                                                await state.requestDonationRemote(
                                                  donation.id,
                                                  quantity: q,
                                                  notes: notesCtrl.text.trim(),
                                                );
                                                if (ctx.mounted) {
                                                  Navigator.pop(ctx);
                                                  ScaffoldMessenger.of(context).showSnackBar(
                                                    const SnackBar(
                                                      content: Text('Request submitted successfully!'),
                                                      backgroundColor: AppColors.success,
                                                    ),
                                                  );
                                                }
                                              } catch (e) {
                                                if (ctx.mounted) {
                                                  setDialogState(() => isSubmitting = false);
                                                  ScaffoldMessenger.of(context).showSnackBar(
                                                    SnackBar(
                                                      content: Text('Failed to submit request: $e'),
                                                      backgroundColor: AppColors.error,
                                                    ),
                                                  );
                                                }
                                              }
                                            },
                                      child: isSubmitting
                                          ? const SizedBox(
                                              width: 16,
                                              height: 16,
                                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                            )
                                          : const Text('Submit Request'),
                                    ),
                                  ],
                                ),
                              );
                            },
                          );
                        }
                      : null,
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size.fromHeight(52),
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.restaurant),
                      const SizedBox(width: 8),
                      const Text('Request Food', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                    ],
                  ),
                ),
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  final String title;
  final IconData icon;
  final List<Widget> children;

  const _Section({required this.title, required this.icon, required this.children});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: AppSpacing.lg, bottom: AppSpacing.md),
          child: Row(
            children: [
              Icon(icon, size: 20, color: AppColors.primary),
              const SizedBox(width: 8),
              Text(title, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600, color: AppColors.primary)),
            ],
          ),
        ),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(children: children),
          ),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isWarning;

  const _InfoRow(this.label, this.value, {this.isWarning = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(label, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.textHint)),
          ),
          Expanded(
            child: Row(
              children: [
                if (isWarning) ...[
                  Icon(Icons.warning_amber, size: 14, color: AppColors.warning),
                  const SizedBox(width: 4),
                ],
                Flexible(
                  child: Text(value, style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: isWarning ? AppColors.warning : AppColors.textPrimary,
                  )),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

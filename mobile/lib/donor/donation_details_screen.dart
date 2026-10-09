import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../mock/app_state.dart';
import '../mock/models.dart';
import '../shared/api_client.dart';
import '../config/api_config.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/timeline_widget.dart';
import '../shared/widgets/rating_dialog.dart';

class DonorDonationDetailsScreen extends StatefulWidget {
  final String id;
  const DonorDonationDetailsScreen({super.key, required this.id});

  @override
  State<DonorDonationDetailsScreen> createState() => _DonorDonationDetailsScreenState();
}

class _DonorDonationDetailsScreenState extends State<DonorDonationDetailsScreen> {
  List<Map<String, dynamic>> _pickups = [];
  final Map<String, Map<String, dynamic>> _pickupRatingStatuses = {};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchDonorRequestsForDonation(widget.id);
      _loadPickups();
    });
  }

  Future<void> _loadPickups() async {
    try {
      final pickups = await context.read<AppState>().fetchDonorPickupsForDonationRemote(widget.id);
      if (mounted) {
        setState(() => _pickups = pickups);
        for (final p in pickups) {
          final pid = p['pickupId']?.toString() ?? p['id']?.toString() ?? '';
          final pStatus = p['status']?.toString() ?? '';
          if (pid.isNotEmpty && pStatus == 'COMPLETED') {
            _loadRatingStatus(pid);
          }
        }
      }
    } catch (_) {}
  }

  Future<void> _loadRatingStatus(String pickupId) async {
    try {
      final client = context.read<ApiClient>();
      final res = await client.get(ApiConfig.pickupRatingStatusUrl(pickupId));
      if (res is Map<String, dynamic> && mounted) {
        setState(() {
          _pickupRatingStatuses[pickupId] = res;
        });
      }
    } catch (_) {}
  }

  Future<void> _viewOtpDialog(String pickupId, String receiverName) async {
    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.vpn_key_outlined, color: AppColors.primary),
            SizedBox(width: 8),
            Text('Handover OTP'),
          ],
        ),
        content: FutureBuilder<Map<String, dynamic>>(
          future: context.read<AppState>().fetchDonorPickupOtpRemote(pickupId),
          builder: (ctx, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const SizedBox(
                height: 120,
                child: Center(child: CircularProgressIndicator()),
              );
            }
            if (snapshot.hasError) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Text('Error: ${snapshot.error}', style: const TextStyle(color: AppColors.error)),
              );
            }
            final data = snapshot.data ?? {};
            final otp = data['otp']?.toString() ?? '------';
            return Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Share this 6-digit code with $receiverName ONLY when they arrive in person to collect the food:',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                const SizedBox(height: 16),
                Center(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                    decoration: BoxDecoration(
                      color: AppColors.primarySurface,
                      borderRadius: BorderRadius.circular(AppRadius.md),
                      border: Border.all(color: AppColors.primary, width: 2),
                    ),
                    child: Text(
                      otp,
                      style: const TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 8,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'This code verifies handover and completes custody transfer.',
                  style: TextStyle(fontSize: 12, color: AppColors.textSecondary, fontStyle: FontStyle.italic),
                ),
              ],
            );
          },
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  Future<void> _cancelDonation(AppState state, String donationId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel Donation?'),
        content: const Text('Are you sure you want to cancel this donation listing? This action cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Keep Listing')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Cancel Donation'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await state.cancelDonationRemote(donationId);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Donation cancelled.'), backgroundColor: AppColors.textSecondary),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to cancel: $e'), backgroundColor: AppColors.error),
        );
      }
    }
  }

  Future<void> _editDonation(AppState state, Donation donation) async {
    final titleCtrl = TextEditingController(text: donation.title);
    final descCtrl = TextEditingController(text: donation.description);
    final qtyCtrl = TextEditingController(text: donation.quantity.toString());
    final addrCtrl = TextEditingController(text: donation.pickupAddress);
    DateTime? newPickupUntil;
    DateTime? newConsumeBefore;
    bool saving = false;
    String? errorMsg;

    await showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Edit Donation'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (errorMsg != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Text(errorMsg!, style: const TextStyle(color: AppColors.error, fontSize: 12)),
                  ),
                TextField(
                  controller: titleCtrl,
                  decoration: const InputDecoration(labelText: 'Title'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  decoration: const InputDecoration(labelText: 'Description'),
                  maxLines: 2,
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: qtyCtrl,
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(labelText: 'Quantity (${donation.unit})'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: addrCtrl,
                  decoration: const InputDecoration(labelText: 'Pickup Address'),
                  maxLines: 2,
                ),
                const SizedBox(height: 16),
                const Text(
                  'Extend Availability & Freshness Window:',
                  style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                ),
                const SizedBox(height: 6),
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: [
                    ActionChip(
                      label: const Text('+2 Hours'),
                      onPressed: () {
                        setDialogState(() {
                          newPickupUntil = DateTime.now().add(const Duration(hours: 2));
                          newConsumeBefore = DateTime.now().add(const Duration(hours: 4));
                        });
                      },
                    ),
                    ActionChip(
                      label: const Text('+4 Hours'),
                      onPressed: () {
                        setDialogState(() {
                          newPickupUntil = DateTime.now().add(const Duration(hours: 4));
                          newConsumeBefore = DateTime.now().add(const Duration(hours: 6));
                        });
                      },
                    ),
                    ActionChip(
                      label: const Text('+8 Hours'),
                      onPressed: () {
                        setDialogState(() {
                          newPickupUntil = DateTime.now().add(const Duration(hours: 8));
                          newConsumeBefore = DateTime.now().add(const Duration(hours: 12));
                        });
                      },
                    ),
                  ],
                ),
                if (newPickupUntil != null && newConsumeBefore != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      'New Pickup Until: ${DateFormat('hh:mm a').format(newPickupUntil!)}\nNew Safe Consume: ${DateFormat('hh:mm a').format(newConsumeBefore!)}',
                      style: const TextStyle(fontSize: 12, color: AppColors.success, fontWeight: FontWeight.w500),
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: saving ? null : () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: saving
                  ? null
                  : () async {
                      setDialogState(() {
                        saving = true;
                        errorMsg = null;
                      });
                      try {
                        final qty = double.tryParse(qtyCtrl.text.trim());
                        await state.updateDonationRemote(
                          donation.id,
                          title: titleCtrl.text.trim(),
                          description: descCtrl.text.trim(),
                          quantity: qty,
                          pickupAddress: addrCtrl.text.trim(),
                          pickupAvailableUntil: newPickupUntil,
                          consumeBefore: newConsumeBefore,
                        );
                        if (ctx.mounted) {
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Donation updated successfully!'), backgroundColor: AppColors.success),
                          );
                        }
                      } catch (e) {
                        setDialogState(() {
                          saving = false;
                          errorMsg = 'Failed to update: $e';
                        });
                      }
                    },
              child: saving
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final donation = state.donations.where((d) => d.id == widget.id).firstOrNull;

    if (donation == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Donation Details')),
        body: const Center(child: Text('Donation not found')),
      );
    }

    final requests = state.requests.where((r) => r.donationId == widget.id).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Donation Details'),
        actions: [
          if (donation.status == 'AVAILABLE' || donation.status == 'APPROVED') ...[
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              tooltip: 'Edit Donation',
              onPressed: () => _editDonation(state, donation),
            ),
            if (donation.status == 'AVAILABLE')
              IconButton(
                icon: const Icon(Icons.cancel_outlined, color: AppColors.error),
                tooltip: 'Cancel Donation',
                onPressed: () => _cancelDonation(state, donation.id),
              ),
          ],
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          // Header
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: AppColors.primarySurface,
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
                child: Icon(donation.categoryIcon, color: AppColors.primary, size: 28),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(donation.title, style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 4),
                    StatusChip(status: donation.status),
                    const SizedBox(height: 4),
                    Text(donation.id, style: Theme.of(context).textTheme.bodySmall),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.xxl),

          // Food Information
          _DetailSection(
            title: 'Food Information',
            icon: Icons.restaurant,
            children: [
              _DetailRow('Category', donation.category),
              _DetailRow('Quantity', '${donation.quantity} ${donation.unit}'),
              _DetailRow('Description', donation.description),
            ],
          ),

          // Food Safety
          _DetailSection(
            title: 'Food Safety',
            icon: Icons.health_and_safety,
            children: [
              _DetailRow('Consume Before', donation.consumeBefore),
              _DetailRow('Storage', donation.storageCondition),
              _DetailRow('Packaging', donation.packagingCondition),
              if (donation.hasAllergens)
                _DetailRow('Allergens', donation.allergenInfo, valueColor: AppColors.warning),
              _DetailRow('Safety Declaration', donation.foodSafetyDeclared ? 'Declared \u2713' : 'Not declared'),
            ],
          ),

          // Pickup Information
          _DetailSection(
            title: 'Pickup Information',
            icon: Icons.location_on,
            children: [
              if (donation.pickupAddress.isNotEmpty) _DetailRow('Address', donation.pickupAddress),
              if (donation.pickupWindowStart.isNotEmpty) _DetailRow('Window', '${donation.pickupWindowStart} - ${donation.pickupWindowEnd}'),
              if (donation.pickupInstructions.isNotEmpty) _DetailRow('Instructions', donation.pickupInstructions),
            ],
          ),

          // Requests
          if (requests.isNotEmpty) ...[
            Padding(
              padding: const EdgeInsets.only(top: AppSpacing.lg, bottom: AppSpacing.md),
              child: Row(
                children: [
                  Icon(Icons.inbox, size: 20, color: AppColors.primary),
                  const SizedBox(width: 8),
                  Text('Requests (${requests.length})', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600, color: AppColors.primary)),
                ],
              ),
            ),
            ...requests.map((r) => Card(
              margin: const EdgeInsets.only(bottom: AppSpacing.sm),
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        CircleAvatar(
                          radius: 18,
                          backgroundColor: AppColors.infoSurface,
                          child: Text(r.receiverName.isNotEmpty ? r.receiverName[0] : 'R', style: TextStyle(color: AppColors.info, fontWeight: FontWeight.w600)),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(r.receiverName, style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                              Text('${r.quantity} ${donation.unit} requested', style: Theme.of(context).textTheme.bodySmall),
                            ],
                          ),
                        ),
                        StatusChip(status: r.status),
                      ],
                    ),
                    if (r.message != null && r.message!.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.sm),
                      Text('"${r.message}"', style: Theme.of(context).textTheme.bodySmall?.copyWith(fontStyle: FontStyle.italic)),
                    ],
                    if (r.status == 'PENDING') ...[
                      const SizedBox(height: AppSpacing.md),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          OutlinedButton.icon(
                            onPressed: () {
                              showDialog(
                                context: context,
                                builder: (ctx) {
                                  bool isProcessing = false;
                                  return StatefulBuilder(
                                    builder: (dialogCtx, setDialogState) => AlertDialog(
                                      title: const Text('Reject Request?'),
                                      content: Text('Reject request from ${r.receiverName}?'),
                                      actions: [
                                        TextButton(
                                          onPressed: isProcessing ? null : () => Navigator.pop(dialogCtx),
                                          child: const Text('Cancel'),
                                        ),
                                        FilledButton(
                                          onPressed: isProcessing
                                              ? null
                                              : () async {
                                                  setDialogState(() => isProcessing = true);
                                                  try {
                                                    await state.rejectDonorRequestRemote(r.id);
                                                    if (ctx.mounted) {
                                                      Navigator.pop(ctx);
                                                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Request rejected')));
                                                    }
                                                  } catch (e) {
                                                    if (ctx.mounted) {
                                                      Navigator.pop(ctx);
                                                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to reject: $e'), backgroundColor: AppColors.error));
                                                    }
                                                  }
                                                },
                                          style: FilledButton.styleFrom(backgroundColor: AppColors.error),
                                          child: isProcessing
                                              ? const SizedBox(
                                                  width: 16,
                                                  height: 16,
                                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                                )
                                              : const Text('Reject'),
                                        ),
                                      ],
                                    ),
                                  );
                                },
                              );
                            },
                            icon: const Icon(Icons.close, size: 18),
                            label: const Text('Reject'),
                            style: OutlinedButton.styleFrom(foregroundColor: AppColors.error, side: BorderSide(color: AppColors.error)),
                          ),
                          const SizedBox(width: AppSpacing.sm),
                          FilledButton.icon(
                            onPressed: () {
                              showDialog(
                                context: context,
                                builder: (ctx) {
                                  bool isProcessing = false;
                                  return StatefulBuilder(
                                    builder: (dialogCtx, setDialogState) => AlertDialog(
                                      title: const Text('Approve Request?'),
                                      content: Text('Approve request from ${r.receiverName} for ${r.quantity} ${donation.unit}? Allocated via FCFS.'),
                                      actions: [
                                        TextButton(
                                          onPressed: isProcessing ? null : () => Navigator.pop(dialogCtx),
                                          child: const Text('Cancel'),
                                        ),
                                        FilledButton(
                                          onPressed: isProcessing
                                              ? null
                                              : () async {
                                                  setDialogState(() => isProcessing = true);
                                                  try {
                                                    await state.approveDonorRequestRemote(r.id);
                                                    if (ctx.mounted) {
                                                      Navigator.pop(ctx);
                                                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Request approved! Quantity allocated.'), backgroundColor: AppColors.success));
                                                    }
                                                  } catch (e) {
                                                    if (ctx.mounted) {
                                                      Navigator.pop(ctx);
                                                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Approval failed: $e'), backgroundColor: AppColors.error));
                                                    }
                                                  }
                                                },
                                          child: isProcessing
                                              ? const SizedBox(
                                                  width: 16,
                                                  height: 16,
                                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                                )
                                              : const Text('Approve'),
                                        ),
                                      ],
                                    ),
                                  );
                                },
                              );
                            },
                            icon: const Icon(Icons.check, size: 18),
                            label: const Text('Approve'),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            )),
          ],

          // Scheduled Pickups & Handover OTP
          if (_pickups.isNotEmpty) ...[
            Padding(
              padding: const EdgeInsets.only(top: AppSpacing.lg, bottom: AppSpacing.md),
              child: Row(
                children: [
                  Icon(Icons.local_shipping, size: 20, color: AppColors.primary),
                  const SizedBox(width: 8),
                  Text('Scheduled Pickups (${_pickups.length})', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600, color: AppColors.primary)),
                ],
              ),
            ),
            ..._pickups.map((p) {
              final pickupId = p['pickupId']?.toString() ?? p['id']?.toString() ?? '';
              final receiverName = p['receiverOrganizationName']?.toString() ?? 'Receiver Organization';
              final status = p['status']?.toString() ?? 'ASSIGNED';
              final scheduledAt = p['scheduledAt']?.toString() ?? '';
              final canViewOtp = status == 'ASSIGNED' || status == 'PICKUP_PENDING' || status == 'EN_ROUTE_TO_PICKUP' || status == 'AT_PICKUP';
              return Card(
                margin: const EdgeInsets.only(bottom: AppSpacing.sm),
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(receiverName, style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                                if (scheduledAt.isNotEmpty)
                                  Text('Scheduled: $scheduledAt', style: Theme.of(context).textTheme.bodySmall),
                              ],
                            ),
                          ),
                          StatusChip(status: status),
                        ],
                      ),
                      const SizedBox(height: AppSpacing.md),
                      if (canViewOtp)
                        FilledButton.icon(
                          onPressed: () => _viewOtpDialog(pickupId, receiverName),
                          icon: const Icon(Icons.vpn_key, size: 18),
                          label: const Text('View Handover OTP'),
                          style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
                        )
                      else if (status == 'COMPLETED') ...[
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Pickup completed \u2713',
                              style: TextStyle(color: AppColors.success, fontWeight: FontWeight.w600),
                            ),
                            const SizedBox(height: AppSpacing.sm),
                            if (_pickupRatingStatuses[pickupId]?['alreadyRated'] == true) ...[
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                                decoration: BoxDecoration(
                                  color: Colors.amber.shade50,
                                  borderRadius: BorderRadius.circular(AppRadius.sm),
                                  border: Border.all(color: Colors.amber.shade200),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    const Icon(Icons.star_rounded, color: Colors.amber, size: 20),
                                    const SizedBox(width: 4),
                                    Text(
                                      'Rated ${_pickupRatingStatuses[pickupId]?['myRating']?['rating']} \u2605',
                                      style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                                    ),
                                    const SizedBox(width: 6),
                                    const Text('(\u2713 Rating submitted)', style: TextStyle(color: AppColors.success, fontSize: 12)),
                                  ],
                                ),
                              ),
                            ] else ...[
                              FilledButton.icon(
                                onPressed: () async {
                                  final submitted = await RatingDialog.show(
                                    context,
                                    pickupId: pickupId,
                                    targetName: receiverName,
                                    targetRole: 'Receiver',
                                  );
                                  if (submitted == true) {
                                    _loadRatingStatus(pickupId);
                                  }
                                },
                                icon: const Icon(Icons.star_outline_rounded, size: 18),
                                label: const Text('Rate Receiver'),
                                style: FilledButton.styleFrom(backgroundColor: Colors.amber.shade700),
                              ),
                            ],
                          ],
                        ),
                      ] else
                        const Text(
                          'Food collected by receiver \u2713',
                          style: TextStyle(color: AppColors.success, fontWeight: FontWeight.w600),
                        ),
                    ],
                  ),
                ),
              );
            }),
          ],

          // Lifecycle Timeline
          const SizedBox(height: AppSpacing.lg),
          Row(
            children: [
              Icon(Icons.timeline, size: 20, color: AppColors.primary),
              const SizedBox(width: 8),
              Text('Lifecycle', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600, color: AppColors.primary)),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          TimelineWidget(
            steps: const ['Created', 'Available', 'Requested', 'Approved', 'Ready for Pickup', 'Picked Up', 'Delivered', 'Completed'],
            currentStep: _getTimelineStep(donation.status),
          ),
          const SizedBox(height: AppSpacing.xxxl),
        ],
      ),
    );
  }

  int _getTimelineStep(String status) {
    switch (status) {
      case 'AVAILABLE': return 2;
      case 'REQUESTED': return 3;
      case 'APPROVED': return 4;
      case 'READY_FOR_PICKUP': return 5;
      case 'PICKED_UP': return 6;
      case 'DELIVERED': return 7;
      case 'COMPLETED': return 8;
      default: return 1;
    }
  }
}

class _DetailSection extends StatelessWidget {
  final String title;
  final IconData icon;
  final List<Widget> children;

  const _DetailSection({required this.title, required this.icon, required this.children});

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
            child: Column(
              children: children,
            ),
          ),
        ),
      ],
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;
  final Color? valueColor;

  const _DetailRow(this.label, this.value, {this.valueColor});

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
            child: Text(value, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: valueColor ?? AppColors.textPrimary)),
          ),
        ],
      ),
    );
  }
}

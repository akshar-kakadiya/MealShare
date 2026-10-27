import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/api_config.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/empty_state.dart';
import 'widgets/receiver_profile_bottom_sheet.dart';

class DonorRequestsScreen extends StatefulWidget {
  const DonorRequestsScreen({super.key});

  @override
  State<DonorRequestsScreen> createState() => _DonorRequestsScreenState();
}

class _DonorRequestsScreenState extends State<DonorRequestsScreen> {
  String _filter = 'All';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchIncomingRequestsForDonor();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final myDonationIds = ApiConfig.isRealMode
        ? state.donations.map((d) => d.id).toSet()
        : state.donations.where((d) => d.donorId == state.currentUser.id).map((d) => d.id).toSet();
    final allRequests = ApiConfig.isRealMode
        ? state.requests
        : state.requests.where((r) => myDonationIds.contains(r.donationId)).toList();
    final filtered = _filter == 'All'
        ? allRequests
        : allRequests.where((r) => r.status.toUpperCase() == _filter.toUpperCase()).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Incoming Requests'),
      ),
      body: Column(
        children: [
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              children: ['All', 'Pending', 'Approved', 'Rejected', 'Completed', 'Expired'].map((f) {
                final isSelected = _filter == f;
                return Padding(
                  padding: const EdgeInsets.only(right: AppSpacing.sm),
                  child: FilterChip(
                    label: Text(f),
                    selected: isSelected,
                    onSelected: (_) => setState(() => _filter = f),
                    selectedColor: AppColors.primarySurface,
                    checkmarkColor: AppColors.primary,
                    labelStyle: TextStyle(
                      color: isSelected ? AppColors.primary : AppColors.textSecondary,
                      fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => context.read<AppState>().fetchIncomingRequestsForDonor(),
              child: filtered.isEmpty
                  ? EmptyState(
                      icon: Icons.inbox,
                      title: 'No requests',
                      subtitle: 'When a receiver requests your donation, it will appear here.',
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
                      itemCount: filtered.length,
                      itemBuilder: (context, index) {
                        final req = filtered[index];
                        final donation = state.donations.where((d) => d.id == req.donationId).firstOrNull;
                        return Card(
                          margin: const EdgeInsets.only(bottom: AppSpacing.md),
                          child: Padding(
                            padding: const EdgeInsets.all(AppSpacing.lg),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    CircleAvatar(
                                      radius: 20,
                                      backgroundColor: AppColors.infoSurface,
                                      child: Text(
                                        req.receiverName.isNotEmpty ? req.receiverName[0] : 'R',
                                        style: TextStyle(color: AppColors.info, fontWeight: FontWeight.w600),
                                      ),
                                    ),
                                    const SizedBox(width: AppSpacing.md),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(req.receiverName, style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                                          if (donation != null)
                                            Text(donation.title, style: Theme.of(context).textTheme.bodySmall),
                                        ],
                                      ),
                                    ),
                                    StatusChip(status: req.status),
                                  ],
                                ),
                                const SizedBox(height: AppSpacing.md),
                                Row(
                                  children: [
                                    _MetaChip(Icons.restaurant, '${req.quantity} ${donation?.unit ?? "items"}'),
                                    const SizedBox(width: AppSpacing.md),
                                    if (req.distance != null)
                                      _MetaChip(Icons.location_on, '${req.distance} km'),
                                    const Spacer(),
                                    Text(_formatTime(req.requestedAt), style: Theme.of(context).textTheme.labelSmall),
                                  ],
                                ),
                                if (req.message != null && req.message!.isNotEmpty) ...[
                                  const SizedBox(height: AppSpacing.sm),
                                  Text('"${req.message}"', style: Theme.of(context).textTheme.bodySmall?.copyWith(fontStyle: FontStyle.italic)),
                                ],
                                if (req.status == 'PENDING') ...[
                                  const SizedBox(height: AppSpacing.md),
                                  Wrap(
                                    alignment: WrapAlignment.end,
                                    spacing: AppSpacing.sm,
                                    runSpacing: AppSpacing.xs,
                                    children: [
                                      OutlinedButton.icon(
                                        icon: const Icon(Icons.info_outline, size: 16),
                                        label: const Text('View Profile'),
                                        onPressed: () => showReceiverProfileModal(context, req.id, req.receiverName),
                                      ),
                                      OutlinedButton(
                                        onPressed: () {
                                          showDialog(
                                            context: context,
                                            builder: (ctx) {
                                              bool isProcessing = false;
                                              return StatefulBuilder(
                                                builder: (dialogCtx, setDialogState) => AlertDialog(
                                                  title: const Text('Reject Request?'),
                                                  content: Text('Reject request from ${req.receiverName}?'),
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
                                                                await state.rejectDonorRequestRemote(req.id);
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
                                        style: OutlinedButton.styleFrom(foregroundColor: AppColors.error, side: BorderSide(color: AppColors.error)),
                                        child: const Text('Reject'),
                                      ),
                                      FilledButton(
                                        onPressed: () {
                                          showDialog(
                                            context: context,
                                            builder: (ctx) {
                                              bool isProcessing = false;
                                              return StatefulBuilder(
                                                builder: (dialogCtx, setDialogState) => AlertDialog(
                                                  title: const Text('Approve Request?'),
                                                  content: Text('Approve request from ${req.receiverName}? Quantity will be allocated via FCFS.'),
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
                                                                await state.approveDonorRequestRemote(req.id);
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
                                        child: const Text('Approve'),
                                      ),
                                    ],
                                  ),
                                ],
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime time) {
    final diff = DateTime.now().difference(time);
    if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
    if (diff.inHours < 24) return '${diff.inHours}h ago';
    return '${diff.inDays}d ago';
  }
}

class _MetaChip extends StatelessWidget {
  final IconData icon;
  final String text;
  const _MetaChip(this.icon, this.text);

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: AppColors.textHint),
        const SizedBox(width: 3),
        Text(text, style: Theme.of(context).textTheme.bodySmall),
      ],
    );
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../config/api_config.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/empty_state.dart';

class ReceiverMyRequestsScreen extends StatefulWidget {
  const ReceiverMyRequestsScreen({super.key});

  @override
  State<ReceiverMyRequestsScreen> createState() => _ReceiverMyRequestsScreenState();
}

class _ReceiverMyRequestsScreenState extends State<ReceiverMyRequestsScreen> {
  String _filter = 'All';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchReceiverRequests();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final myRequests = ApiConfig.isRealMode
        ? state.requests
        : state.requests.where((r) => r.receiverId == state.currentUser.id).toList();
    final filtered = _filter == 'All'
        ? myRequests
        : myRequests.where((r) => r.status.toUpperCase() == _filter.toUpperCase()).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Requests'),
      ),
      body: Column(
        children: [
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              children: ['All', 'Pending', 'Approved', 'Completed'].map((f) {
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
          if (state.receiverRequestsError != null)
            Container(
              margin: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.xs),
              padding: const EdgeInsets.all(AppSpacing.md),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(AppRadius.md),
                border: Border.all(color: Colors.red.shade200),
              ),
              child: Row(
                children: [
                  const Icon(Icons.cloud_off, color: Colors.red, size: 20),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      'Backend unavailable: ${state.receiverRequestsError}',
                      style: TextStyle(color: Colors.red.shade900, fontSize: 13),
                    ),
                  ),
                  TextButton(
                    onPressed: () => state.fetchReceiverRequests(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          const SizedBox(height: AppSpacing.sm),
          Expanded(
            child: state.isLoading && filtered.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: () => state.fetchReceiverRequests(),
                    child: filtered.isEmpty
                        ? EmptyState(
                            icon: state.receiverRequestsError != null ? Icons.error_outline : Icons.inbox,
                            title: state.receiverRequestsError != null ? 'Could not load requests' : 'No requests',
                            subtitle: state.receiverRequestsError != null
                                ? 'Unable to reach backend service. Please tap retry.'
                                : 'When you request food from a donor, it will appear here.',
                            actionLabel: state.receiverRequestsError != null ? 'Retry' : 'Discover Food',
                            onAction: () {
                              if (state.receiverRequestsError != null) {
                                state.fetchReceiverRequests();
                              } else {
                                context.go('/receiver/discover');
                              }
                            },
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
                            itemCount: filtered.length,
                            itemBuilder: (context, index) {
                              final req = filtered[index];
                              final donation = state.donations.where((d) => d.id == req.donationId).firstOrNull;
                      return Card(
                        margin: const EdgeInsets.only(bottom: AppSpacing.md),
                        child: InkWell(
                          onTap: req.status == 'APPROVED'
                              ? () => context.push('/receiver/pickups')
                              : null,
                          borderRadius: BorderRadius.circular(AppRadius.card),
                          child: Padding(
                            padding: const EdgeInsets.all(AppSpacing.lg),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Container(
                                      width: 44,
                                      height: 44,
                                      decoration: BoxDecoration(
                                        color: AppColors.primarySurface,
                                        borderRadius: BorderRadius.circular(AppRadius.md),
                                      ),
                                      child: Icon(donation?.categoryIcon ?? Icons.restaurant, color: AppColors.primary, size: 22),
                                    ),
                                    const SizedBox(width: AppSpacing.md),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            donation?.title ?? req.donationTitle ?? 'Donation Request',
                                            style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                                          ),
                                          Text(
                                            donation?.donorName ?? '',
                                            style: Theme.of(context).textTheme.bodySmall,
                                          ),
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
                                    _MetaChip(Icons.schedule, _formatTime(req.requestedAt)),
                                    const SizedBox(width: AppSpacing.md),
                                    if (req.status == 'APPROVED')
                                      Expanded(
                                        child: Align(
                                          alignment: Alignment.centerRight,
                                          child: TextButton.icon(
                                            onPressed: () => context.push('/receiver/pickups'),
                                            icon: const Icon(Icons.local_shipping, size: 16),
                                            label: const Text('Pickups'),
                                          ),
                                        ),
                                      )
                                    else if (req.status == 'PENDING')
                                      Expanded(
                                        child: Align(
                                          alignment: Alignment.centerRight,
                                          child: TextButton.icon(
                                            onPressed: () async {
                                              final confirm = await showDialog<bool>(
                                                context: context,
                                                builder: (ctx) => AlertDialog(
                                                  title: const Text('Withdraw Request?'),
                                                  content: const Text('Are you sure you want to withdraw this food request?'),
                                                  actions: [
                                                    TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
                                                    FilledButton(
                                                      onPressed: () => Navigator.pop(ctx, true),
                                                      style: FilledButton.styleFrom(backgroundColor: AppColors.error),
                                                      child: const Text('Withdraw'),
                                                    ),
                                                  ],
                                                ),
                                              );
                                              if (confirm == true) {
                                                try {
                                                  await state.withdrawReceiverRequestRemote(req.id);
                                                  if (context.mounted) {
                                                    ScaffoldMessenger.of(context).showSnackBar(
                                                      const SnackBar(content: Text('Request withdrawn successfully')),
                                                    );
                                                  }
                                                } catch (e) {
                                                  if (context.mounted) {
                                                    ScaffoldMessenger.of(context).showSnackBar(
                                                      SnackBar(content: Text('Failed to withdraw request: $e'), backgroundColor: AppColors.error),
                                                    );
                                                  }
                                                }
                                              }
                                            },
                                            icon: const Icon(Icons.close, size: 16, color: AppColors.error),
                                            label: const Text('Withdraw', style: TextStyle(color: AppColors.error)),
                                          ),
                                        ),
                                      ),
                                  ],
                                ),
                              ],
                            ),
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

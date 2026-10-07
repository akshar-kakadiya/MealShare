import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../config/api_config.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/stat_card.dart';
import '../shared/widgets/section_header.dart';
import '../shared/widgets/donation_card.dart';

class DonorHomeScreen extends StatefulWidget {
  const DonorHomeScreen({super.key});

  @override
  State<DonorHomeScreen> createState() => _DonorHomeScreenState();
}

class _DonorHomeScreenState extends State<DonorHomeScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _refreshData();
    });
  }

  Future<void> _refreshData() async {
    final state = context.read<AppState>();
    await state.fetchDonorDonations();
    await state.fetchIncomingRequestsForDonor();
    await state.fetchNotificationsRemote();
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final myDonations = ApiConfig.isRealMode
        ? state.donations
        : state.donations.where((d) => d.donorId == state.currentUser.id).toList();
    final activeDonations = myDonations.where((d) => d.status != 'COMPLETED' && d.status != 'EXPIRED' && d.status != 'CANCELLED').toList();
    final firstName = state.currentUser.name.split(' ').first;
    final recentActivity = state.donorRecentActivity;

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _refreshData,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            // App Bar
            SliverAppBar(
              floating: true,
              title: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Good ${_getGreeting()}, $firstName \u{1F44B}',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  Text(
                    'Your food is making a difference',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
              toolbarHeight: 64,
            ),
            SliverPadding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  // Impact Stats
                  Row(
                    children: [
                      Expanded(
                        child: StatCard(
                          label: 'Meals Donated',
                          value: '${state.donorTotalMeals}',
                          icon: Icons.restaurant,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: StatCard(
                          label: 'People Helped',
                          value: '${state.donorPeopleHelped}',
                          icon: Icons.people,
                          backgroundColor: AppColors.secondarySurface,
                          iconColor: AppColors.secondary,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.lg),

                  // Action Required
                  if (state.donorPendingRequests > 0)
                    Container(
                      margin: const EdgeInsets.only(bottom: AppSpacing.lg),
                      padding: const EdgeInsets.all(AppSpacing.lg),
                      decoration: BoxDecoration(
                        color: AppColors.secondarySurface,
                        borderRadius: BorderRadius.circular(AppRadius.card),
                        border: Border.all(color: AppColors.secondary.withValues(alpha: 0.3)),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: AppColors.secondary.withValues(alpha: 0.2),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(Icons.inbox, color: AppColors.secondary, size: 22),
                          ),
                          const SizedBox(width: AppSpacing.md),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '${state.donorPendingRequests} request${state.donorPendingRequests > 1 ? 's' : ''} waiting',
                                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                    fontWeight: FontWeight.w600,
                                    color: AppColors.textPrimary,
                                  ),
                                ),
                                Text(
                                  'Review and approve food requests',
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                              ],
                            ),
                          ),
                          FilledButton.tonal(
                            onPressed: () => context.go('/donor/requests'),
                            child: const Text('Review'),
                          ),
                        ],
                      ),
                    ),

                  // Active Donations
                  SectionHeader(
                    title: 'Active Donations',
                    actionLabel: 'See All',
                    onAction: () => context.go('/donor/donations'),
                  ),
                  if (activeDonations.isEmpty)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(AppSpacing.xxl),
                        child: Column(
                          children: [
                            Icon(Icons.volunteer_activism, size: 48, color: AppColors.textHint),
                            const SizedBox(height: AppSpacing.md),
                            Text('No active donations', style: Theme.of(context).textTheme.titleSmall),
                            const SizedBox(height: AppSpacing.xs),
                            Text('Create your first donation to start sharing food', style: Theme.of(context).textTheme.bodySmall, textAlign: TextAlign.center),
                          ],
                        ),
                      ),
                    )
                  else
                    ...activeDonations.take(3).map((d) => DonationCard(
                      donation: d,
                      onTap: () => context.push('/donor/donation/${d.id}'),
                    )),

                  const SizedBox(height: AppSpacing.sm),

                  // Recent Activity
                  const SectionHeader(title: 'Recent Activity'),
                  if (recentActivity.isEmpty)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(AppSpacing.xl),
                        child: Center(
                          child: Text(
                            'No recent activity',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ),
                      ),
                    )
                  else
                    ...recentActivity.take(4).map((activity) => Padding(
                      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                      child: Card(
                        child: ListTile(
                          leading: Container(
                            width: 36,
                            height: 36,
                            decoration: BoxDecoration(
                              color: (activity.iconColor ?? AppColors.primary).withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Icon(activity.icon, size: 18, color: activity.iconColor ?? AppColors.primary),
                          ),
                          title: Text(activity.title, style: Theme.of(context).textTheme.titleSmall),
                          subtitle: Text(activity.subtitle, style: Theme.of(context).textTheme.bodySmall),
                          trailing: Text(_formatTime(activity.time), style: Theme.of(context).textTheme.labelSmall),
                        ),
                      ),
                    )),
                  const SizedBox(height: 80),
                ]),
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/donor/create'),
        icon: const Icon(Icons.add),
        label: const Text('New Donation'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
      ),
    );
  }

  String _getGreeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'morning';
    if (hour < 17) return 'afternoon';
    return 'evening';
  }

  String _formatTime(DateTime time) {
    final diff = DateTime.now().difference(time);
    if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
    if (diff.inHours < 24) return '${diff.inHours}h ago';
    return '${diff.inDays}d ago';
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../config/api_config.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/donation_card.dart';
import '../shared/widgets/empty_state.dart';

class DonorDonationsListScreen extends StatefulWidget {
  const DonorDonationsListScreen({super.key});

  @override
  State<DonorDonationsListScreen> createState() => _DonorDonationsListScreenState();
}

class _DonorDonationsListScreenState extends State<DonorDonationsListScreen> {
  String _filter = 'All';
  final _filters = ['All', 'Available', 'Requested', 'Approved', 'Completed'];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchDonorDonations();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final myDonations = ApiConfig.isRealMode
        ? state.donations
        : state.donations.where((d) => d.donorId == state.currentUser.id).toList();
    final filtered = _filter == 'All'
        ? myDonations
        : myDonations.where((d) => d.status.toUpperCase() == _filter.toUpperCase()).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Donations'),
      ),
      body: Column(
        children: [
          // Filter chips
          SizedBox(
            height: 48,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              itemCount: _filters.length,
              separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.sm),
              itemBuilder: (context, index) {
                final filter = _filters[index];
                final isSelected = _filter == filter;
                return FilterChip(
                  label: Text(filter),
                  selected: isSelected,
                  onSelected: (_) => setState(() => _filter = filter),
                  selectedColor: AppColors.primarySurface,
                  checkmarkColor: AppColors.primary,
                  labelStyle: TextStyle(
                    color: isSelected ? AppColors.primary : AppColors.textSecondary,
                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                  ),
                );
              },
            ),
          ),
          if (state.donorDonationsError != null)
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
                      'Backend unavailable: ${state.donorDonationsError}',
                      style: TextStyle(color: Colors.red.shade900, fontSize: 13),
                    ),
                  ),
                  TextButton(
                    onPressed: () => context.read<AppState>().fetchDonorDonations(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          const SizedBox(height: AppSpacing.sm),
          // Donations list
          Expanded(
            child: state.isLoading && filtered.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: () => context.read<AppState>().fetchDonorDonations(),
                    child: filtered.isEmpty
                        ? EmptyState(
                            icon: state.donorDonationsError != null ? Icons.error_outline : Icons.volunteer_activism,
                            title: state.donorDonationsError != null ? 'Could not load donations' : 'No donations yet',
                            subtitle: state.donorDonationsError != null
                                ? 'Unable to reach the backend server. Please check your connection and tap retry.'
                                : 'Create your first donation to start sharing surplus food with those in need.',
                            actionLabel: state.donorDonationsError != null ? 'Retry' : 'Create Donation',
                            onAction: () {
                              if (state.donorDonationsError != null) {
                                context.read<AppState>().fetchDonorDonations();
                              } else {
                                context.push('/donor/create');
                              }
                            },
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
                            itemCount: filtered.length,
                            itemBuilder: (context, index) {
                              final d = filtered[index];
                              return DonationCard(
                                donation: d,
                                onTap: () => context.push('/donor/donation/${d.id}'),
                              );
                            },
                          ),
                  ),
          ),
        ],
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
}

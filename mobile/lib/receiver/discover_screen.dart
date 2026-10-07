import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/donation_card.dart';
import '../shared/widgets/empty_state.dart';

class ReceiverDiscoverScreen extends StatefulWidget {
  const ReceiverDiscoverScreen({super.key});

  @override
  State<ReceiverDiscoverScreen> createState() => _ReceiverDiscoverScreenState();
}

class _ReceiverDiscoverScreenState extends State<ReceiverDiscoverScreen> {
  String _searchQuery = '';
  String _selectedCategory = 'All';
  final _categories = ['All', 'Meals', 'Bakery', 'Fruits', 'Vegetables', 'Dairy', 'Beverages'];
  final _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchNearbyDonations(category: _selectedCategory);
    });
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final available = state.donations.where((d) => d.status == 'AVAILABLE').toList();

    // Apply filters
    var filtered = available;
    if (_selectedCategory != 'All') {
      filtered = filtered.where((d) => d.category == _selectedCategory).toList();
    }
    if (_searchQuery.isNotEmpty) {
      final q = _searchQuery.toLowerCase();
      filtered = filtered.where((d) =>
        d.title.toLowerCase().contains(q) ||
        d.donorName.toLowerCase().contains(q) ||
        d.description.toLowerCase().contains(q)
      ).toList();
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Discover Food'),
      ),
      body: Column(
        children: [
          // Search bar
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
            child: TextField(
              controller: _searchCtrl,
              onChanged: (v) => setState(() => _searchQuery = v),
              decoration: InputDecoration(
                hintText: 'Search food or donor...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchQuery.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _searchCtrl.clear();
                          setState(() => _searchQuery = '');
                        },
                      )
                    : null,
              ),
            ),
          ),

          // Category chips
          SizedBox(
            height: 48,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              itemCount: _categories.length,
              separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.sm),
              itemBuilder: (context, index) {
                final cat = _categories[index];
                final isSelected = _selectedCategory == cat;
                return FilterChip(
                  label: Text(cat),
                  selected: isSelected,
                  onSelected: (_) {
                    setState(() => _selectedCategory = cat);
                    state.fetchNearbyDonations(category: cat);
                  },
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
          const SizedBox(height: AppSpacing.sm),

          // Location radius indicator
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(Icons.location_on, size: 14, color: Colors.blue.shade700),
                  const SizedBox(width: 6),
                  Text(
                    'Searching within 15 km of Bengaluru (12.9716, 77.5946)',
                    style: TextStyle(fontSize: 11, color: Colors.blue.shade800, fontWeight: FontWeight.w500),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),

          // Results header
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            child: Row(
              children: [
                Text(
                  '${filtered.length} donation${filtered.length != 1 ? 's' : ''} found',
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w500),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.sm),

          if (state.nearbyDonationsError != null)
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
                      'Backend unavailable: ${state.nearbyDonationsError}',
                      style: TextStyle(color: Colors.red.shade900, fontSize: 13),
                    ),
                  ),
                  TextButton(
                    onPressed: () => state.fetchNearbyDonations(category: _selectedCategory),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          const SizedBox(height: AppSpacing.sm),

          // Donation list
          Expanded(
            child: state.isLoading && filtered.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: () => state.fetchNearbyDonations(category: _selectedCategory),
                    child: filtered.isEmpty
                        ? EmptyState(
                            icon: state.nearbyDonationsError != null ? Icons.error_outline : Icons.search_off,
                            title: state.nearbyDonationsError != null ? 'Could not discover food' : 'No food found',
                            subtitle: state.nearbyDonationsError != null
                                ? 'Unable to reach the backend discovery service. Please tap retry.'
                                : (_searchQuery.isNotEmpty || _selectedCategory != 'All'
                                    ? 'Try adjusting your search or filters'
                                    : 'No food donations available right now. Check back later!'),
                            actionLabel: state.nearbyDonationsError != null ? 'Retry' : null,
                            onAction: state.nearbyDonationsError != null
                                ? () => state.fetchNearbyDonations(category: _selectedCategory)
                                : null,
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
                            itemCount: filtered.length,
                            itemBuilder: (context, index) {
                              final d = filtered[index];
                              return DonationCard(
                                donation: d,
                                showDistance: true,
                                showDonor: true,
                                onTap: () => context.push('/receiver/donation/${d.id}'),
                              );
                            },
                          ),
                  ),
          ),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../mock/app_state.dart';
import '../shared/api_client.dart';
import '../config/api_config.dart';
import '../theme/app_colors.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/empty_state.dart';
import '../shared/widgets/rating_dialog.dart';

class DonorPickupsScreen extends StatefulWidget {
  const DonorPickupsScreen({super.key});

  @override
  State<DonorPickupsScreen> createState() => _DonorPickupsScreenState();
}

class _DonorPickupsScreenState extends State<DonorPickupsScreen> {
  String _selectedTab = 'All';
  final List<String> _tabs = [
    'All',
    'PICKUP_PENDING',
    'EN_ROUTE_TO_PICKUP',
    'AT_PICKUP',
    'PICKED_UP',
    'IN_TRANSIT',
    'COMPLETED',
  ];
  bool _loading = false;
  String? _errorMessage;
  List<Map<String, dynamic>> _pickups = [];
  final Map<String, Map<String, dynamic>> _pickupRatingStatuses = {};

  @override
  void initState() {
    super.initState();
    _loadPickups();
  }

  Future<void> _loadPickups() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
    });

    try {
      final appState = context.read<AppState>();
      final pickups = await appState.fetchDonorPickupsRemote();
      setState(() {
        _pickups = pickups;
      });

      for (final p in _pickups) {
        final pid = p['pickupId']?.toString() ?? p['id']?.toString() ?? '';
        final pStatus = (p['status'] ?? '').toString().toUpperCase();
        if (pid.isNotEmpty && pStatus == 'COMPLETED') {
          _loadRatingStatus(pid);
        }
      }
    } on ApiException catch (e) {
      setState(() {
        _errorMessage = e.message;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Could not load pickups. Please pull to refresh.';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
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
                      borderRadius: BorderRadius.circular(12),
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
          TextButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  Future<void> _viewTrackingDialog(String pickupId, String receiverName) async {
    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.navigation, color: Colors.blue),
            SizedBox(width: 8),
            Text('Live Approach Tracking'),
          ],
        ),
        content: FutureBuilder<Map<String, dynamic>>(
          future: context.read<ApiClient>().get(ApiConfig.donorTrackingUrl(pickupId)).then((r) => r is Map<String, dynamic> ? r : {}),
          builder: (ctx, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const SizedBox(
                height: 100,
                child: Center(child: CircularProgressIndicator()),
              );
            }
            final data = snapshot.data ?? {};
            final dist = data['distanceMeters'] as num?;
            final arrived = data['receiverArrived'] == true;
            return Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('$receiverName is currently en route.', style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.blue.shade200),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.directions_car, color: Colors.blue),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          arrived
                              ? 'Receiver has arrived at your location!'
                              : (dist != null
                                  ? 'Distance to you: ${dist.toInt()} meters'
                                  : 'Tracking active. Approaching your premises.'),
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.blue),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'When receiver arrives and inspects the food, share your 6-digit Handover OTP with them to complete custody transfer.',
                  style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
                ),
              ],
            );
          },
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  Future<void> _rateCounterpart(String pickupId, String receiverName) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => RatingDialog(
        pickupId: pickupId,
        targetName: receiverName,
        targetRole: 'Receiver',
      ),
    );

    if (result == true && mounted) {
      _loadRatingStatus(pickupId);
    }
  }

  List<Map<String, dynamic>> get _filteredPickups {
    if (_selectedTab == 'All') return _pickups;
    return _pickups.where((p) => (p['status'] ?? '').toString().toUpperCase() == _selectedTab).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pickups & Handover'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadPickups,
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter Tabs
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: _tabs.map((tab) {
                final isSelected = _selectedTab == tab;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(tab == 'All' ? 'All Pickups' : tab.replaceAll('_', ' ')),
                    selected: isSelected,
                    onSelected: (val) {
                      if (val) setState(() => _selectedTab = tab);
                    },
                    selectedColor: AppColors.primarySurface,
                    checkmarkColor: AppColors.primary,
                    labelStyle: TextStyle(
                      color: isSelected ? AppColors.primary : AppColors.textSecondary,
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      fontSize: 12,
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const Divider(height: 1),

          // Content
          Expanded(
            child: RefreshIndicator(
              onRefresh: _loadPickups,
              child: _buildBody(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading && _pickups.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null && _pickups.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.cloud_off, size: 48, color: AppColors.error),
              const SizedBox(height: 16),
              Text(_errorMessage!, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textSecondary)),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _loadPickups,
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    final items = _filteredPickups;
    if (items.isEmpty) {
      return const EmptyState(
        icon: Icons.local_shipping_outlined,
        title: 'No Scheduled Pickups',
        subtitle: 'When an admin schedules a collection for your approved donations, it will appear here.',
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final p = items[index];
        return _buildPickupCard(p);
      },
    );
  }

  Widget _buildPickupCard(Map<String, dynamic> p) {
    final pickupId = p['pickupId']?.toString() ?? p['id']?.toString() ?? '';
    final title = p['donationTitle']?.toString() ?? 'Food Donation';
    final receiverName = p['receiverOrganizationName']?.toString() ?? 'Receiver Organization';
    final address = p['pickupAddress']?.toString() ?? 'Address on file';
    final status = (p['status'] ?? 'ASSIGNED').toString().toUpperCase();
    final quantity = p['requestedQuantity'] != null ? '${p['requestedQuantity']} ${p['quantityUnit'] ?? 'meals'}' : '';

    String scheduledStr = 'Scheduled';
    if (p['scheduledAt'] != null) {
      final dt = DateTime.tryParse(p['scheduledAt'].toString());
      if (dt != null) {
        scheduledStr = DateFormat('MMM d, h:mm a').format(dt.toLocal());
      }
    }

    final ratingStatus = _pickupRatingStatuses[pickupId];
    final bool isCompleted = status == 'COMPLETED';
    final bool alreadyRated = ratingStatus?['alreadyRated'] == true;
    final bool eligibleToRate = ratingStatus?['eligibleToRate'] == true;
    final myRating = ratingStatus?['myRating'];

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top row
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
                StatusChip(status: status),
              ],
            ),
            const SizedBox(height: 8),

            // Receiver Info
            Row(
              children: [
                const Icon(Icons.business, size: 16, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Collecting Org: $receiverName',
                    style: const TextStyle(fontSize: 13, color: AppColors.textPrimary, fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),

            // Quantity & Time
            Row(
              children: [
                if (quantity.isNotEmpty) ...[
                  const Icon(Icons.inventory_2_outlined, size: 16, color: AppColors.textSecondary),
                  const SizedBox(width: 6),
                  Text(quantity, style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                  const SizedBox(width: 16),
                ],
                const Icon(Icons.schedule, size: 16, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Text(scheduledStr, style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
              ],
            ),
            const SizedBox(height: 4),

            // Address
            Row(
              children: [
                const Icon(Icons.location_on_outlined, size: 16, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    address,
                    style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Action Buttons
            if (status == 'PICKUP_PENDING' || status == 'ASSIGNED' || status == 'EN_ROUTE_TO_PICKUP' || status == 'AT_PICKUP') ...[
              if (status == 'EN_ROUTE_TO_PICKUP') ...[
                Container(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.blue.shade200),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.navigation, color: Colors.blue, size: 18),
                      const SizedBox(width: 8),
                      const Expanded(
                        child: Text(
                          'Receiver Approaching: Live GPS streaming active.',
                          style: TextStyle(fontSize: 12, color: Colors.blue, fontWeight: FontWeight.w600),
                        ),
                      ),
                      FilledButton.tonal(
                        onPressed: () => _viewTrackingDialog(pickupId, receiverName),
                        style: FilledButton.styleFrom(visualDensity: VisualDensity.compact),
                        child: const Text('Track Approach'),
                      ),
                    ],
                  ),
                ),
              ],
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.primarySurface,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.shield_outlined, color: AppColors.primary, size: 20),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'Secure Handover: Keep your OTP confidential until food is handed over.',
                        style: TextStyle(fontSize: 12, color: AppColors.primary, fontWeight: FontWeight.w500),
                      ),
                    ),
                    FilledButton.tonal(
                      onPressed: () => _viewOtpDialog(pickupId, receiverName),
                      child: const Text('View OTP'),
                    ),
                  ],
                ),
              ),
            ] else if (isCompleted) ...[
              const Divider(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (alreadyRated && myRating != null) ...[
                    Row(
                      children: [
                        const Icon(Icons.star, color: Colors.amber, size: 18),
                        const SizedBox(width: 4),
                        Text(
                          'You rated: ${myRating['rating']}/5',
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                      ],
                    ),
                  ] else if (eligibleToRate) ...[
                    const Text(
                      'Pickup Completed',
                      style: TextStyle(color: AppColors.success, fontWeight: FontWeight.bold, fontSize: 13),
                    ),
                    FilledButton.icon(
                      onPressed: () => _rateCounterpart(pickupId, receiverName),
                      icon: const Icon(Icons.star_outline, size: 16),
                      label: const Text('Rate Receiver'),
                      style: FilledButton.styleFrom(
                        backgroundColor: Colors.amber.shade700,
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ] else ...[
                    const Text(
                      'Pickup Completed & Finalized',
                      style: TextStyle(color: AppColors.success, fontWeight: FontWeight.bold, fontSize: 13),
                    ),
                  ],
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

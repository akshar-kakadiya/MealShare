import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../shared/utils/date_formatter.dart';
import '../shared/api_client.dart';
import '../config/api_config.dart';
import '../theme/app_colors.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/empty_state.dart';
import '../shared/widgets/rating_dialog.dart';
import '../shared/widgets/donation_tracking_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../shared/utils/polyline_decoder.dart';
import '../tracking/pickup_live_tracking_service.dart';

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

  Future<void> _showTrackingModal(String pickupId, String receiverName, {String phase = 'APPROACH'}) async {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetCtx) => _TrackingBottomSheet(
        pickupId: pickupId,
        receiverName: receiverName,
        phase: phase,
        onViewOtp: () {
          Navigator.pop(sheetCtx);
          _viewOtpDialog(pickupId, receiverName);
        },
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
    final foodInspected = p['foodInspectedAt'] != null;
    final inspectionRejected = p['inspectionRejected'] == true;
    final inspectionNotes = p['inspectionNotes']?.toString() ?? '';

    final scheduledStr = AppDateFormatter.formatDateTime(p['scheduledAt'], fallback: 'Scheduled');

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
            if (status == 'PICKUP_PENDING' || status == 'ASSIGNED') ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade300),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.schedule, color: AppColors.textSecondary, size: 18),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Awaiting receiver departure. Handover OTP will unlock once receiver arrives and completes food inspection.',
                        style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
                      ),
                    ),
                  ],
                ),
              ),
            ] else if (status == 'EN_ROUTE_TO_PICKUP') ...[
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
                      onPressed: () => _showTrackingModal(pickupId, receiverName, phase: 'APPROACH'),
                      style: FilledButton.styleFrom(visualDensity: VisualDensity.compact),
                      child: const Text('Track Approach'),
                    ),
                  ],
                ),
              ),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.primarySurface,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.shield_outlined, color: AppColors.primary, size: 18),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Handover OTP will unlock once the receiver arrives and accepts food safety inspection.',
                        style: TextStyle(fontSize: 12, color: AppColors.primary, fontWeight: FontWeight.w500),
                      ),
                    ),
                  ],
                ),
              ),
            ] else if (status == 'AT_PICKUP') ...[
              if (!foodInspected || inspectionRejected) ...[
                if (inspectionRejected) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.red.shade50,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.red.shade200),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.cancel, color: Colors.red, size: 18),
                            SizedBox(width: 8),
                            Text('Inspection Rejected by Receiver', style: TextStyle(fontSize: 12, color: Colors.red, fontWeight: FontWeight.bold)),
                          ],
                        ),
                        if (inspectionNotes.isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text('Reason: $inspectionNotes', style: const TextStyle(fontSize: 12, color: Colors.red)),
                        ],
                      ],
                    ),
                  ),
                ] else ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.amber.shade50,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.amber.shade300),
                    ),
                    child: const Row(
                      children: [
                        Icon(Icons.hourglass_top, color: Colors.amber, size: 18),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Receiver has arrived. Food safety inspection in progress. Handover OTP will unlock once accepted.',
                            style: TextStyle(fontSize: 12, color: Colors.black87, fontWeight: FontWeight.w500),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ] else ...[
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Colors.green.shade50,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.green.shade300),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.check_circle_outline, color: Colors.green, size: 20),
                      const SizedBox(width: 8),
                      const Expanded(
                        child: Text(
                          'Food inspected & accepted! Share your 6-digit OTP with the receiver to complete custody handover.',
                          style: TextStyle(fontSize: 12, color: Colors.green, fontWeight: FontWeight.w600),
                        ),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
                        onPressed: () => _viewOtpDialog(pickupId, receiverName),
                        child: const Text('View OTP'),
                      ),
                    ],
                  ),
                ),
              ],
            ] else if (status == 'PICKED_UP') ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.blue.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.blue.shade200),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.inventory_2_outlined, color: Colors.blue, size: 20),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Food custody transferred. Awaiting receiver departure to distribution destination.',
                        style: TextStyle(fontSize: 12, color: Colors.blue, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
              ),
            ] else if (status == 'IN_TRANSIT') ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.blue.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.blue.shade200),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.local_shipping, color: Colors.blue, size: 20),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Donation collected. Receiver is distributing food to beneficiaries.',
                        style: TextStyle(fontSize: 12, color: Colors.blue, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
              ),
            ] else if (status == 'CANCELLED') ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.red.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.red.shade200),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.cancel, color: Colors.red, size: 18),
                        SizedBox(width: 8),
                        Text('Pickup Cancelled', style: TextStyle(fontSize: 12, color: Colors.red, fontWeight: FontWeight.bold)),
                      ],
                    ),
                    if (inspectionNotes.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text('Reason: $inspectionNotes', style: const TextStyle(fontSize: 12, color: Colors.red)),
                    ],
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

class _TrackingBottomSheet extends StatefulWidget {
  final String pickupId;
  final String receiverName;
  final String phase;
  final VoidCallback onViewOtp;

  const _TrackingBottomSheet({
    required this.pickupId,
    required this.receiverName,
    required this.phase,
    required this.onViewOtp,
  });

  @override
  State<_TrackingBottomSheet> createState() => _TrackingBottomSheetState();
}

class _TrackingBottomSheetState extends State<_TrackingBottomSheet> {
  Map<String, dynamic>? _trackingData;
  bool _loading = true;
  String? _error;
  Timer? _fallbackTimer;
  StreamSubscription<TrackingSessionSnapshot?>? _rtdbSubscription;
  List<LatLng>? _roadRoutePoints;
  String? _roadDistance;
  String? _roadDuration;
  double? _lastRouteOriginLat;
  double? _lastRouteOriginLng;
  DateTime? _lastRouteCalculatedAt;
  bool _fetchingRoute = false;

  @override
  void initState() {
    super.initState();
    _fetchTracking();
    _fetchRoadRoute();
    _subscribeToLiveTracking();
    // Low-frequency fallback (25s) for backend sync
    _fallbackTimer = Timer.periodic(const Duration(seconds: 25), (_) {
      _fetchTracking(silent: true);
    });
  }

  void _subscribeToLiveTracking() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      try {
        final trackingService = context.read<PickupLiveTrackingService?>();
        if (trackingService != null) {
          _rtdbSubscription = trackingService.watchPickupSession(widget.pickupId).listen((snapshot) {
            if (!mounted || snapshot == null) return;
            final recLoc = snapshot.receiverLocation;
            setState(() {
              _trackingData = {
                ...?_trackingData,
                'pickupId': snapshot.pickupId,
                'status': snapshot.status,
                'trackingActive': snapshot.trackingActive,
                if (recLoc != null) ...{
                  'currentLatitude': recLoc.latitude,
                  'currentLongitude': recLoc.longitude,
                  'speed': recLoc.speed,
                  'heading': recLoc.heading,
                  'locationUpdatedAt': recLoc.timestamp.toIso8601String(),
                },
                if (snapshot.donorLatitude != null) 'donorLatitude': snapshot.donorLatitude,
                if (snapshot.donorLongitude != null) 'donorLongitude': snapshot.donorLongitude,
                if (snapshot.donorAddress != null) 'pickupAddress': snapshot.donorAddress,
                if (snapshot.donorName != null) 'donorName': snapshot.donorName,
              };
              _loading = false;
            });
            if (recLoc != null) {
              final now = DateTime.now();
              final secondsSince = _lastRouteCalculatedAt == null ? 999 : now.difference(_lastRouteCalculatedAt!).inSeconds;
              bool shouldRecalculate = false;
              if (_lastRouteOriginLat == null || _lastRouteOriginLng == null) {
                shouldRecalculate = true;
              } else {
                final devMeters = Geolocator.distanceBetween(
                  _lastRouteOriginLat!,
                  _lastRouteOriginLng!,
                  recLoc.latitude,
                  recLoc.longitude,
                );
                if (devMeters > 100.0 && secondsSince >= 25) {
                  shouldRecalculate = true;
                } else if (secondsSince >= 60) {
                  shouldRecalculate = true;
                }
              }
              if (shouldRecalculate) {
                _fetchRoadRoute(lat: recLoc.latitude, lng: recLoc.longitude);
              }
            }
          });
        }
      } catch (e) {
        debugPrint('[DonorTracking] RTDB subscription error: $e');
      }
    });
  }

  @override
  void dispose() {
    _rtdbSubscription?.cancel();
    _fallbackTimer?.cancel();
    super.dispose();
  }

  Future<void> _fetchTracking({bool silent = false}) async {
    if (!silent) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final client = context.read<ApiClient>();
      final res = await client.get(ApiConfig.donorTrackingUrl(widget.pickupId));
      if (mounted && res is Map<String, dynamic>) {
        setState(() {
          _trackingData = res;
          _loading = false;
        });
        if (_roadRoutePoints == null) {
          _fetchRoadRoute(
            lat: (_trackingData?['currentLatitude'] as num?)?.toDouble(),
            lng: (_trackingData?['currentLongitude'] as num?)?.toDouble(),
          );
        }
      }
    } catch (e) {
      if (mounted && !silent) {
        setState(() {
          _error = 'Failed to load tracking data: $e';
          _loading = false;
        });
      }
    }
  }

  Future<void> _fetchRoadRoute({double? lat, double? lng}) async {
    if (_fetchingRoute || !mounted) return;
    _fetchingRoute = true;
    try {
      final client = context.read<ApiClient>();
      String url = ApiConfig.donorPickupRouteUrl(widget.pickupId);
      if (lat != null && lng != null) {
        url += '?lat=$lat&lng=$lng';
      }
      final res = await client.get(url);
      if (mounted && res is Map<String, dynamic> && res['available'] == true) {
        final encoded = res['encodedPolyline']?.toString() ?? '';
        final points = decodePolyline(encoded);
        setState(() {
          _roadRoutePoints = points;
          _roadDistance = res['formattedDistance']?.toString();
          _roadDuration = res['formattedDuration']?.toString();
          _lastRouteOriginLat = lat ?? (res['originLatitude'] as num?)?.toDouble();
          _lastRouteOriginLng = lng ?? (res['originLongitude'] as num?)?.toDouble();
          _lastRouteCalculatedAt = DateTime.now();
        });
      }
    } catch (e) {
      debugPrint('[DonorTracking] Route calculation error: $e');
    } finally {
      _fetchingRoute = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDelivery = widget.phase == 'DELIVERY' || _trackingData?['phase'] == 'DELIVERY';
    final dist = (_trackingData?['distanceToTargetMeters'] ?? _trackingData?['distanceMeters']) as num?;
    final withinGeofence = _trackingData?['withinGeofence'] == true || (dist != null && dist <= 200);
    final status = (_trackingData?['status'] ?? '').toString().toUpperCase();
    final arrived = withinGeofence || status == 'AT_PICKUP';

    DateTime? lastUpdated;
    if (_trackingData?['locationUpdatedAt'] != null) {
      lastUpdated = DateTime.tryParse(_trackingData!['locationUpdatedAt'].toString())?.toLocal();
    }

    return Container(
      height: MediaQuery.of(context).size.height * 0.85,
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        children: [
          const SizedBox(height: 10),
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Colors.grey.shade300,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 12),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                Icon(
                  isDelivery ? Icons.local_shipping : Icons.navigation,
                  color: isDelivery ? Colors.blue.shade700 : AppColors.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        isDelivery ? 'Live Delivery Tracking (Journey B)' : 'Live Approach Tracking (Journey A)',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                      ),
                      Text(
                        isDelivery
                            ? 'Receiver en route to distribution destination'
                            : '${widget.receiverName} approaching your pickup location',
                        style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.pop(context),
                ),
              ],
            ),
          ),
          const Divider(height: 16),
          Expanded(
            child: _loading && _trackingData == null
                ? const Center(child: CircularProgressIndicator())
                : _error != null && _trackingData == null
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.error_outline, size: 40, color: AppColors.error),
                            const SizedBox(height: 8),
                            Text(_error!, style: const TextStyle(color: AppColors.textSecondary)),
                            const SizedBox(height: 12),
                            FilledButton(onPressed: () => _fetchTracking(), child: const Text('Retry')),
                          ],
                        ),
                      )
                    : Column(
                        children: [
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                              decoration: BoxDecoration(
                                color: arrived ? Colors.green.shade50 : Colors.blue.shade50,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(
                                  color: arrived ? Colors.green.shade300 : Colors.blue.shade200,
                                ),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    arrived ? Icons.check_circle : Icons.near_me,
                                    color: arrived ? Colors.green : Colors.blue,
                                    size: 20,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      arrived
                                          ? 'Receiver is within 200m pickup geofence / arrived!'
                                          : (dist != null
                                              ? 'Receiver is ${dist.toInt()}m away from pickup point'
                                              : 'Live GPS active. Tracking vehicle approach.'),
                                      style: TextStyle(
                                        color: arrived ? Colors.green.shade800 : Colors.blue.shade900,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 13,
                                      ),
                                    ),
                                  ),
                                  if (arrived && !isDelivery) ...[
                                    const SizedBox(width: 8),
                                    FilledButton(
                                      style: FilledButton.styleFrom(
                                        backgroundColor: AppColors.primary,
                                        visualDensity: VisualDensity.compact,
                                      ),
                                      onPressed: widget.onViewOtp,
                                      child: const Text('View OTP'),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          Expanded(
                            child: ClipRRect(
                              child: DonationTrackingMap(
                                receiverLatitude: (_trackingData?['currentLatitude'] as num?)?.toDouble(),
                                receiverLongitude: (_trackingData?['currentLongitude'] as num?)?.toDouble(),
                                donorLatitude: (_trackingData?['donorLatitude'] as num?)?.toDouble(),
                                donorLongitude: (_trackingData?['donorLongitude'] as num?)?.toDouble(),
                                donorLabel: _trackingData?['donorName']?.toString() ?? 'Pickup Point',
                                destinationLatitude: (_trackingData?['destinationLatitude'] as num?)?.toDouble(),
                                destinationLongitude: (_trackingData?['destinationLongitude'] as num?)?.toDouble(),
                                destinationLabel: _trackingData?['receiverOrgName']?.toString() ?? 'Delivery Destination',
                                phase: _trackingData?['phase']?.toString() ?? widget.phase,
                                distanceMeters: dist?.toDouble(),
                                lastUpdated: lastUpdated,
                                isReceiver: false,
                                roadRoutePoints: _roadRoutePoints,
                                roadDistance: _roadDistance,
                                roadDuration: _roadDuration,
                                onRefresh: () {
                                  _fetchTracking();
                                  _fetchRoadRoute(
                                    lat: (_trackingData?['currentLatitude'] as num?)?.toDouble(),
                                    lng: (_trackingData?['currentLongitude'] as num?)?.toDouble(),
                                  );
                                },
                              ),
                            ),
                          ),
                        ],
                      ),
          ),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../shared/api_client.dart';
import '../config/api_config.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/status_chip.dart';
import '../shared/widgets/timeline_widget.dart';
import 'dart:async';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../shared/utils/polyline_decoder.dart';
import '../shared/widgets/donation_tracking_map.dart';
import '../tracking/pickup_live_tracking_service.dart';

class ReceiverPickupTrackingScreen extends StatefulWidget {
  final String requestId;
  const ReceiverPickupTrackingScreen({super.key, required this.requestId});

  @override
  State<ReceiverPickupTrackingScreen> createState() => _ReceiverPickupTrackingScreenState();
}

class _ReceiverPickupTrackingScreenState extends State<ReceiverPickupTrackingScreen> {
  Map<String, dynamic>? _pickupData;
  Map<String, dynamic>? _trackingData;
  bool _loading = false;
  String? _error;
  StreamSubscription<TrackingSessionSnapshot?>? _rtdbSubscription;
  List<LatLng>? _roadRoutePoints;
  String? _roadDistance;
  String? _roadDuration;
  bool _fetchingRoute = false;

  Future<void> _fetchRoadRoute(String pickupId, {double? lat, double? lng}) async {
    if (_fetchingRoute || !mounted) return;
    _fetchingRoute = true;
    try {
      final client = context.read<ApiClient>();
      String url = ApiConfig.receiverPickupRouteUrl(pickupId);
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
        });
      }
    } catch (e) {
      debugPrint('[ReceiverTracking] Route calculation error: $e');
    } finally {
      _fetchingRoute = false;
    }
  }

  @override
  void dispose() {
    _rtdbSubscription?.cancel();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _fetchPickupDetails();
  }

  Future<void> _fetchPickupDetails() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final client = context.read<ApiClient>();
      // Ensure pickup exists for this request
      final res = await client.post(ApiConfig.receiverEnsurePickupForRequestUrl(widget.requestId));
      if (res is Map<String, dynamic> && mounted) {
        setState(() {
          _pickupData = res;
        });

        final pid = res['pickupId']?.toString() ?? res['id']?.toString();
        if (pid != null && pid.isNotEmpty) {
          try {
            final trackRes = await client.get(ApiConfig.receiverTrackingUrl(pid));
            if (trackRes is Map<String, dynamic> && mounted) {
              setState(() {
                _trackingData = trackRes;
              });
              _fetchRoadRoute(
                pid,
                lat: (trackRes['currentLatitude'] as num?)?.toDouble(),
                lng: (trackRes['currentLongitude'] as num?)?.toDouble(),
              );
            }
            if (!mounted) return;
            try {
              final trackingService = context.read<PickupLiveTrackingService?>();
              if (trackingService != null) {
                _rtdbSubscription?.cancel();
                _rtdbSubscription = trackingService.watchPickupSession(pid).listen((snapshot) {
                  if (!mounted || snapshot == null) return;
                  setState(() {
                    final recLoc = snapshot.receiverLocation;
                    _trackingData = {
                      ...?_trackingData,
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
                    };
                  });
                });
              }
            } catch (_) {}
          } catch (_) {}
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Could not load pickup details: $e';
        });
      }
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final req = state.requests.where((r) => r.id == widget.requestId).firstOrNull;

    final title = _pickupData?['donationTitle']?.toString() ?? req?.donationTitle ?? 'Food Redistribution';
    final donorName = _pickupData?['donorName']?.toString() ?? 'Donor';
    final pickupAddress = _pickupData?['pickupAddress']?.toString() ?? 'Donor Address';
    final status = (_pickupData?['status']?.toString() ?? req?.status ?? 'PICKUP_PENDING').toUpperCase();

    final donorLat = (_trackingData?['donorLatitude'] ?? _pickupData?['pickupLatitude'] ?? _pickupData?['donorLatitude']) as num?;
    final donorLng = (_trackingData?['donorLongitude'] ?? _pickupData?['pickupLongitude'] ?? _pickupData?['donorLongitude']) as num?;
    final receiverLat = (_trackingData?['currentLatitude'] ?? _pickupData?['currentLatitude']) as num?;
    final receiverLng = (_trackingData?['currentLongitude'] ?? _pickupData?['currentLongitude']) as num?;
    final destLat = (_trackingData?['destinationLatitude'] ?? _pickupData?['destinationLatitude'] ?? state.currentUser.latitude) as num?;
    final destLng = (_trackingData?['destinationLongitude'] ?? _pickupData?['destinationLongitude'] ?? state.currentUser.longitude) as num?;
    final dist = (_trackingData?['distanceToTargetMeters'] ?? _trackingData?['distanceMeters']) as num?;

    DateTime? lastUpdated;
    if (_trackingData?['locationUpdatedAt'] != null) {
      lastUpdated = DateTime.tryParse(_trackingData!['locationUpdatedAt'].toString())?.toLocal();
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Redistribution Tracking'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
            onPressed: _fetchPickupDetails,
          ),
        ],
      ),
      body: _loading && _pickupData == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(AppSpacing.lg),
              children: [
                if (_error != null)
                  Container(
                    padding: const EdgeInsets.all(12),
                    margin: const EdgeInsets.only(bottom: 12),
                    decoration: BoxDecoration(
                      color: Colors.red.shade50,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(_error!, style: TextStyle(color: Colors.red.shade800, fontSize: 13)),
                  ),

                // Header Card
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
                          child: const Icon(Icons.restaurant, color: AppColors.primary, size: 30),
                        ),
                        const SizedBox(height: AppSpacing.md),
                        Text(title, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
                        const SizedBox(height: 6),
                        StatusChip(status: status),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),

                // Route Overview
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
                                  Text('From (Donor)', style: Theme.of(context).textTheme.labelSmall),
                                  Text(donorName, style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                                  Text(pickupAddress, style: Theme.of(context).textTheme.bodySmall),
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
                                  Text('To (Receiver Destination)', style: Theme.of(context).textTheme.labelSmall),
                                  Text(state.currentUser.name, style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                                  Text(state.currentUser.address, style: Theme.of(context).textTheme.bodySmall),
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

                // Live Tracking Map Card
                Card(
                  clipBehavior: Clip.antiAlias,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(AppSpacing.md),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Live Redistribution Map',
                              style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                            ),
                            if (dist != null)
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                decoration: BoxDecoration(
                                  color: Colors.blue.shade50,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Text(
                                  '${dist.toInt()} m to destination',
                                  style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.blue.shade800),
                                ),
                              ),
                          ],
                        ),
                      ),
                      SizedBox(
                        height: 280,
                        child: DonationTrackingMap(
                          receiverLatitude: receiverLat?.toDouble(),
                          receiverLongitude: receiverLng?.toDouble(),
                          donorLatitude: donorLat?.toDouble(),
                          donorLongitude: donorLng?.toDouble(),
                          donorLabel: donorName,
                          destinationLatitude: destLat?.toDouble(),
                          destinationLongitude: destLng?.toDouble(),
                          destinationLabel: state.currentUser.name,
                          phase: _trackingData?['phase']?.toString() ?? (status == 'IN_TRANSIT' ? 'DELIVERY' : 'APPROACH'),
                          distanceMeters: dist?.toDouble(),
                          lastUpdated: lastUpdated,
                          isReceiver: true,
                          roadRoutePoints: _roadRoutePoints,
                          roadDistance: _roadDistance,
                          roadDuration: _roadDuration,
                          onRefresh: _fetchPickupDetails,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),

                // Timeline Widget
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(AppSpacing.lg),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Operational Timeline', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                        const SizedBox(height: AppSpacing.lg),
                        TimelineWidget(
                          steps: const [
                            'Approved',
                            'Ready for Collection',
                            'Receiver Approaching',
                            'Arrived & Inspected',
                            'OTP Verified & Picked Up',
                            'In Transit to Destination',
                            'Delivered & Completed',
                          ],
                          currentStep: _getStepIndex(status),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),

                // Direct Action Button
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    icon: const Icon(Icons.directions_car, size: 18),
                    label: const Text('Open Operations & Manage Handover'),
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                    onPressed: () => context.push('/receiver/pickups'),
                  ),
                ),
                const SizedBox(height: AppSpacing.xxxl),
              ],
            ),
    );
  }

  int _getStepIndex(String status) {
    switch (status) {
      case 'APPROVED':
        return 0;
      case 'PICKUP_PENDING':
      case 'ASSIGNED':
        return 1;
      case 'EN_ROUTE_TO_PICKUP':
        return 2;
      case 'AT_PICKUP':
        return 3;
      case 'PICKED_UP':
        return 4;
      case 'IN_TRANSIT':
        return 5;
      case 'COMPLETED':
        return 6;
      default:
        return 0;
    }
  }
}

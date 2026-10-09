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

class ReceiverPickupTrackingScreen extends StatefulWidget {
  final String requestId;
  const ReceiverPickupTrackingScreen({super.key, required this.requestId});

  @override
  State<ReceiverPickupTrackingScreen> createState() => _ReceiverPickupTrackingScreenState();
}

class _ReceiverPickupTrackingScreenState extends State<ReceiverPickupTrackingScreen> {
  Map<String, dynamic>? _pickupData;
  bool _loading = false;
  String? _error;

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

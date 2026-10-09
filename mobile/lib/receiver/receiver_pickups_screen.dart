import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/shared/device_hardware_service.dart';
import 'package:mealshare_mobile/theme/app_colors.dart';
import 'package:mealshare_mobile/shared/widgets/status_chip.dart';
import 'package:mealshare_mobile/shared/widgets/empty_state.dart';
import 'package:mealshare_mobile/shared/widgets/rating_dialog.dart';

class ReceiverPickupsScreen extends StatefulWidget {
  const ReceiverPickupsScreen({super.key});

  @override
  State<ReceiverPickupsScreen> createState() => _ReceiverPickupsScreenState();
}

class _ReceiverPickupsScreenState extends State<ReceiverPickupsScreen> {
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
  final Map<String, Map<String, dynamic>> _trackingData = {};

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
      final client = context.read<ApiClient>();
      final res = await client.get(ApiConfig.receiverPickupsUrl);
      List<Map<String, dynamic>> list = [];
      if (res is Map<String, dynamic> && res['content'] is List) {
        list = List<Map<String, dynamic>>.from(res['content']);
      } else if (res is List) {
        list = List<Map<String, dynamic>>.from(res);
      }

      // Check for any approved requests without pickups and ensure pickups exist
      try {
        final reqRes = await client.get(ApiConfig.receiverRequestsUrl);
        final reqList = (reqRes is Map<String, dynamic> && reqRes['content'] is List)
            ? reqRes['content'] as List
            : (reqRes is List ? reqRes : []);
        final existingRequestIds = list.map((p) => (p['donationRequestId'] ?? p['donationRequest']?['id'])?.toString()).where((id) => id != null).toSet();
        bool ensuredAny = false;
        for (final r in reqList) {
          if (r is Map<String, dynamic> && r['status'] == 'APPROVED' && r['id'] != null) {
            final reqId = r['id'].toString();
            if (!existingRequestIds.contains(reqId)) {
              try {
                await client.post(ApiConfig.receiverEnsurePickupForRequestUrl(reqId));
                ensuredAny = true;
              } catch (_) {}
            }
          }
        }
        if (ensuredAny) {
          final refreshed = await client.get(ApiConfig.receiverPickupsUrl);
          if (refreshed is Map<String, dynamic> && refreshed['content'] is List) {
            list = List<Map<String, dynamic>>.from(refreshed['content']);
          }
        }
      } catch (_) {}

      setState(() {
        _pickups = list;
      });

      // Load additional data for active and completed pickups
      for (final p in _pickups) {
        final pid = p['pickupId']?.toString() ?? p['id']?.toString() ?? '';
        final pStatus = (p['status'] ?? '').toString().toUpperCase();
        if (pid.isNotEmpty) {
          if (pStatus == 'COMPLETED') {
            _loadRatingStatus(pid);
          } else if (pStatus == 'EN_ROUTE_TO_PICKUP' || pStatus == 'IN_TRANSIT') {
            _loadTrackingInfo(pid);
          }
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

  Future<void> _loadTrackingInfo(String pickupId) async {
    try {
      final client = context.read<ApiClient>();
      final res = await client.get(ApiConfig.receiverTrackingUrl(pickupId));
      if (res is Map<String, dynamic> && mounted) {
        setState(() {
          _trackingData[pickupId] = res;
        });
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

  // ─── STEP 1: START PICKUP JOURNEY ───
  Future<void> _startPickupJourney(String pickupId) async {
    setState(() => _loading = true);
    try {
      final client = context.read<ApiClient>();
      final pos = await DeviceHardwareService.getCurrentLocation();
      final body = pos != null
          ? {
              'latitude': pos.latitude,
              'longitude': pos.longitude,
              'speed': pos.speed,
              'heading': pos.heading,
            }
          : null;

      await client.post(ApiConfig.receiverStartJourneyUrl(pickupId), body: body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Pickup journey started! Live tracking activated.'),
            backgroundColor: AppColors.success,
          ),
        );
        _loadPickups();
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to start journey: $e'), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    }
  }

  // ─── STREAM LIVE LOCATION UPDATE ───
  Future<void> _sendLiveLocationUpdate(String pickupId) async {
    final client = context.read<ApiClient>();
    try {
      final pos = await DeviceHardwareService.getCurrentLocation();
      if (pos == null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Device location currently unavailable')),
          );
        }
        return;
      }

      final res = await client.post(
        ApiConfig.receiverLocationUrl(pickupId),
        body: {
          'latitude': pos.latitude,
          'longitude': pos.longitude,
          'speed': pos.speed,
          'heading': pos.heading,
          'phase': 'APPROACH',
        },
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Location updated. Distance to donor: ${res['distanceMeters'] != null ? '${(res['distanceMeters'] as num).toInt()}m' : 'computed'}',
            ),
            duration: const Duration(seconds: 2),
          ),
        );
        _loadTrackingInfo(pickupId);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to stream location: $e'), backgroundColor: AppColors.error),
        );
      }
    }
  }

  // ─── STEP 2: CONFIRM REACHED DONOR LOCATION ───
  Future<void> _confirmReachedLocation(String pickupId) async {
    setState(() => _loading = true);
    try {
      final client = context.read<ApiClient>();
      final pos = await DeviceHardwareService.getCurrentLocation();
      final body = pos != null
          ? {
              'latitude': pos.latitude,
              'longitude': pos.longitude,
              'speed': pos.speed,
              'heading': pos.heading,
            }
          : null;

      await client.post(ApiConfig.receiverReachedLocationUrl(pickupId), body: body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Arrival confirmed at donor location! Please inspect the food.'),
            backgroundColor: AppColors.success,
          ),
        );
        _loadPickups();
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to confirm arrival: $e'), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    }
  }

  // ─── STEP 3: FOOD SAFETY INSPECTION DIALOG ───
  Future<void> _showFoodInspectionDialog(String pickupId, String donationTitle) async {
    bool accepted = true;
    final notesController = TextEditingController();
    String? dialogError;
    bool submitting = false;

    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.health_and_safety_outlined, color: AppColors.primary),
              SizedBox(width: 8),
              Text('Food Inspection'),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Perform physical inspection for "$donationTitle": verify smell, appearance, packaging, and temperature.',
                  style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                ),
                const SizedBox(height: 16),
                RadioListTile<bool>(
                  title: const Text('Accept (Safe & Good Condition)', style: TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: const Text('Food meets safety requirements and is suitable for redistribution.'),
                  value: true,
                  groupValue: accepted,
                  activeColor: AppColors.success,
                  onChanged: (val) => setDialogState(() => accepted = val ?? true),
                ),
                RadioListTile<bool>(
                  title: const Text('Reject (Unsafe / Defective)', style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.error)),
                  subtitle: const Text('Food is spoiled, unhygienic, expired, or improperly stored.'),
                  value: false,
                  groupValue: accepted,
                  activeColor: AppColors.error,
                  onChanged: (val) => setDialogState(() => accepted = val ?? false),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: notesController,
                  maxLines: 2,
                  decoration: InputDecoration(
                    labelText: accepted ? 'Inspection Notes (Optional)' : 'Rejection Reason (Required)',
                    hintText: accepted ? 'e.g. Well packaged, fresh condition' : 'e.g. Spoiled smell, broken packaging',
                    border: const OutlineInputBorder(),
                  ),
                ),
                if (dialogError != null) ...[
                  const SizedBox(height: 10),
                  Text(dialogError!, style: const TextStyle(color: AppColors.error, fontSize: 13)),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: submitting ? null : () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: accepted ? AppColors.success : AppColors.error,
              ),
              onPressed: submitting
                  ? null
                  : () async {
                      final notes = notesController.text.trim();
                      if (!accepted && notes.isEmpty) {
                        setDialogState(() => dialogError = 'Please provide a reason for rejecting the food.');
                        return;
                      }

                      setDialogState(() {
                        submitting = true;
                        dialogError = null;
                      });

                      try {
                        final client = context.read<ApiClient>();
                        await client.post(
                          ApiConfig.receiverInspectFoodUrl(pickupId),
                          body: {
                            'accepted': accepted,
                            'notes': notes,
                            'inspectionNotes': notes,
                            if (!accepted) 'rejectionReason': notes,
                          },
                        );

                        if (ctx.mounted) {
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              content: Text(
                                accepted
                                    ? 'Food safety verified and accepted!'
                                    : 'Food rejected. Pickup cancelled and logged.',
                              ),
                              backgroundColor: accepted ? AppColors.success : AppColors.error,
                            ),
                          );
                          _loadPickups();
                        }
                      } on ApiException catch (e) {
                        setDialogState(() {
                          submitting = false;
                          dialogError = e.message;
                        });
                      } catch (e) {
                        setDialogState(() {
                          submitting = false;
                          dialogError = 'Inspection submission failed: $e';
                        });
                      }
                    },
              child: submitting
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(accepted ? 'Confirm Accept' : 'Confirm Reject'),
            ),
          ],
        ),
      ),
    );
  }

  // ─── STEP 4: VERIFY OTP & CAPTURE EVIDENCE PHOTO ───
  Future<void> _showVerifyAndPickupDialog(String pickupId, String donationTitle) async {
    final otpController = TextEditingController();
    String? uploadedPhotoUrl;
    String? dialogError;
    bool isCapturingPhoto = false;
    bool verifying = false;

    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.verified_user_outlined, color: AppColors.primary),
              SizedBox(width: 8),
              Text('Handover & Evidence'),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Collect 6-digit OTP from donor and capture food condition photo evidence for "$donationTitle":',
                  style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                ),
                const SizedBox(height: 16),

                // OTP Input
                const Text('Donor Handover OTP:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                const SizedBox(height: 6),
                TextField(
                  controller: otpController,
                  autofocus: true,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  textAlign: TextAlign.center,
                  style: const TextStyle(letterSpacing: 12, fontSize: 22, fontWeight: FontWeight.bold),
                  decoration: const InputDecoration(
                    hintText: '000000',
                    border: OutlineInputBorder(),
                    counterText: '',
                  ),
                ),
                const SizedBox(height: 16),

                // Photo Evidence Capture
                const Text('Food Condition Photo Evidence:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                const SizedBox(height: 6),
                if (uploadedPhotoUrl != null)
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.green.shade50,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.green.shade200),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.check_circle, color: Colors.green, size: 20),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text('Photo evidence captured & uploaded', style: TextStyle(color: Colors.green, fontSize: 12, fontWeight: FontWeight.w600)),
                        ),
                        IconButton(
                          icon: const Icon(Icons.refresh, size: 18),
                          tooltip: 'Retake photo',
                          onPressed: () async {
                            final client = context.read<ApiClient>();
                            setDialogState(() => isCapturingPhoto = true);
                            try {
                              final url = await DeviceHardwareService.capturePhotoAndUpload(
                                context: context,
                                apiClient: client,
                              );
                              if (url != null) {
                                setDialogState(() => uploadedPhotoUrl = url);
                              }
                            } finally {
                              setDialogState(() => isCapturingPhoto = false);
                            }
                          },
                        ),
                      ],
                    ),
                  )
                else
                  OutlinedButton.icon(
                    icon: isCapturingPhoto
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.camera_alt),
                    label: Text(isCapturingPhoto ? 'Uploading photo...' : 'Take Food Evidence Photo'),
                    style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44)),
                    onPressed: isCapturingPhoto
                        ? null
                        : () async {
                            final client = context.read<ApiClient>();
                            setDialogState(() => isCapturingPhoto = true);
                            try {
                              final url = await DeviceHardwareService.capturePhotoAndUpload(
                                context: context,
                                apiClient: client,
                              );
                              if (url != null) {
                                setDialogState(() => uploadedPhotoUrl = url);
                              }
                            } catch (e) {
                              setDialogState(() => dialogError = 'Photo capture failed: $e');
                            } finally {
                              setDialogState(() => isCapturingPhoto = false);
                            }
                          },
                  ),

                if (dialogError != null) ...[
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(color: Colors.red.shade50, borderRadius: BorderRadius.circular(8)),
                    child: Text(dialogError!, style: TextStyle(color: Colors.red.shade800, fontSize: 12)),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: verifying ? null : () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: (verifying || isCapturingPhoto)
                  ? null
                  : () async {
                      final otp = otpController.text.trim();
                      if (otp.length != 6) {
                        setDialogState(() => dialogError = 'Please enter all 6 numeric OTP digits.');
                        return;
                      }

                      setDialogState(() {
                        verifying = true;
                        dialogError = null;
                      });

                      try {
                        final client = context.read<ApiClient>();
                        final pos = await DeviceHardwareService.getCurrentLocation();
                        await client.post(
                          ApiConfig.receiverVerifyAndPickupUrl(pickupId),
                          body: {
                            'otp': otp,
                            'pickupPhotoUrl': uploadedPhotoUrl,
                            'latitude': pos?.latitude,
                            'longitude': pos?.longitude,
                          },
                        );

                        if (ctx.mounted) {
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Handover verified! Food custody safely transferred to receiver.'),
                              backgroundColor: AppColors.success,
                            ),
                          );
                          _loadPickups();
                        }
                      } on ApiException catch (e) {
                        setDialogState(() {
                          verifying = false;
                          dialogError = e.message;
                        });
                      } catch (e) {
                        setDialogState(() {
                          verifying = false;
                          dialogError = 'Verification failed: $e';
                        });
                      }
                    },
              child: verifying
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Verify Handover'),
            ),
          ],
        ),
      ),
    );
  }

  // ─── STEP 5: START IN-TRANSIT DELIVERY ───
  Future<void> _startDelivery(String pickupId) async {
    setState(() => _loading = true);
    try {
      final client = context.read<ApiClient>();
      final pos = await DeviceHardwareService.getCurrentLocation();
      final body = pos != null
          ? {
              'latitude': pos.latitude,
              'longitude': pos.longitude,
              'speed': pos.speed,
              'heading': pos.heading,
              'phase': 'DELIVERY',
            }
          : null;

      await client.post(ApiConfig.receiverStartDeliveryUrl(pickupId), body: body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Delivery transit started! Tracking destination delivery.'),
            backgroundColor: AppColors.success,
          ),
        );
        _loadPickups();
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.message), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to start delivery: $e'), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    }
  }

  // ─── STEP 6: COMPLETE DELIVERY WITH EVIDENCE ───
  Future<void> _showCompleteDeliveryDialog(String pickupId, String defaultQuantity, String quantityUnit) async {
    final qtyController = TextEditingController(text: defaultQuantity);
    final reasonController = TextEditingController();
    String? deliveryPhotoUrl;
    String? dialogError;
    bool isCapturingPhoto = false;
    bool submitting = false;

    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.check_circle_outline, color: AppColors.success),
              SizedBox(width: 8),
              Text('Complete Delivery'),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Confirm delivered food quantity and capture delivery destination evidence photo:',
                  style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
                ),
                const SizedBox(height: 16),

                // Delivered Quantity Input
                TextField(
                  controller: qtyController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: InputDecoration(
                    labelText: 'Delivered Quantity ($quantityUnit)',
                    border: const OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),

                // Discrepancy reason
                TextField(
                  controller: reasonController,
                  decoration: const InputDecoration(
                    labelText: 'Discrepancy Reason (Optional)',
                    hintText: 'e.g. Minor transit spillage',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),

                // Destination Photo Evidence
                const Text('Delivery Evidence Photo:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                const SizedBox(height: 6),
                if (deliveryPhotoUrl != null)
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.green.shade50,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.green.shade200),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.check_circle, color: Colors.green, size: 20),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text('Delivery evidence captured', style: TextStyle(color: Colors.green, fontSize: 12, fontWeight: FontWeight.w600)),
                        ),
                        IconButton(
                          icon: const Icon(Icons.refresh, size: 18),
                          tooltip: 'Retake photo',
                          onPressed: () async {
                            final client = context.read<ApiClient>();
                            setDialogState(() => isCapturingPhoto = true);
                            try {
                              final url = await DeviceHardwareService.capturePhotoAndUpload(
                                context: context,
                                apiClient: client,
                              );
                              if (url != null) {
                                setDialogState(() => deliveryPhotoUrl = url);
                              }
                            } finally {
                              setDialogState(() => isCapturingPhoto = false);
                            }
                          },
                        ),
                      ],
                    ),
                  )
                else
                  OutlinedButton.icon(
                    icon: isCapturingPhoto
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.camera_alt),
                    label: Text(isCapturingPhoto ? 'Uploading photo...' : 'Take Delivery Evidence Photo'),
                    style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44)),
                    onPressed: isCapturingPhoto
                        ? null
                        : () async {
                            final client = context.read<ApiClient>();
                            setDialogState(() => isCapturingPhoto = true);
                            try {
                              final url = await DeviceHardwareService.capturePhotoAndUpload(
                                context: context,
                                apiClient: client,
                              );
                              if (url != null) {
                                setDialogState(() => deliveryPhotoUrl = url);
                              }
                            } catch (e) {
                              setDialogState(() => dialogError = 'Photo capture failed: $e');
                            } finally {
                              setDialogState(() => isCapturingPhoto = false);
                            }
                          },
                  ),

                if (dialogError != null) ...[
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(color: Colors.red.shade50, borderRadius: BorderRadius.circular(8)),
                    child: Text(dialogError!, style: TextStyle(color: Colors.red.shade800, fontSize: 12)),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: submitting ? null : () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              style: FilledButton.styleFrom(backgroundColor: AppColors.success),
              onPressed: (submitting || isCapturingPhoto)
                  ? null
                  : () async {
                      final qText = qtyController.text.trim();
                      final qVal = double.tryParse(qText);
                      if (qVal == null || qVal <= 0) {
                        setDialogState(() => dialogError = 'Please enter a valid delivered quantity.');
                        return;
                      }

                      setDialogState(() {
                        submitting = true;
                        dialogError = null;
                      });

                      try {
                        final client = context.read<ApiClient>();
                        final pos = await DeviceHardwareService.getCurrentLocation();
                        await client.post(
                          ApiConfig.receiverCompleteDeliveryUrl(pickupId),
                          body: {
                            'deliveredQuantity': qVal,
                            'quantityDiscrepancyReason': reasonController.text.trim(),
                            'deliveryPhotoUrl': deliveryPhotoUrl,
                            'latitude': pos?.latitude,
                            'longitude': pos?.longitude,
                          },
                        );

                        if (ctx.mounted) {
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Delivery completed successfully! Food redistribution finalized.'),
                              backgroundColor: AppColors.success,
                            ),
                          );
                          _loadPickups();
                        }
                      } on ApiException catch (e) {
                        setDialogState(() {
                          submitting = false;
                          dialogError = e.message;
                        });
                      } catch (e) {
                        setDialogState(() {
                          submitting = false;
                          dialogError = 'Completion failed: $e';
                        });
                      }
                    },
              child: submitting
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Confirm Completion'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authService = context.watch<AuthService>();

    final filtered = _pickups.where((p) {
      if (_selectedTab == 'All') return true;
      return (p['status'] ?? '').toString().toUpperCase() == _selectedTab;
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Food Redistribution Operations'),
        backgroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
            onPressed: _loadPickups,
          ),
        ],
      ),
      body: Column(
        children: [
          // Role header banner
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            color: Colors.green.shade50,
            child: Row(
              children: [
                CircleAvatar(
                  backgroundColor: Colors.green.shade200,
                  radius: 18,
                  child: const Icon(Icons.inventory_2, color: Colors.green, size: 20),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        authService.displayName ?? 'Receiver',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                      ),
                      Text(
                        authService.email ?? 'Verified Food Receiver',
                        style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.green.shade100,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.directions_car, size: 14, color: Colors.green),
                      SizedBox(width: 4),
                      Text(
                        'Direct Redistribution',
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: Colors.green),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // Filter tabs
          Container(
            height: 48,
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: _tabs.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final tab = _tabs[index];
                final isSelected = _selectedTab == tab;
                String label = tab.replaceAll('_', ' ');
                if (tab == 'PICKUP_PENDING') label = 'Pending';
                if (tab == 'EN_ROUTE_TO_PICKUP') label = 'En Route';
                if (tab == 'AT_PICKUP') label = 'At Pickup';
                if (tab == 'PICKED_UP') label = 'Picked Up';
                if (tab == 'IN_TRANSIT') label = 'In Transit';
                if (tab == 'COMPLETED') label = 'Completed';

                return FilterChip(
                  label: Text(label),
                  selected: isSelected,
                  onSelected: (_) => setState(() => _selectedTab = tab),
                  selectedColor: AppColors.primarySurface,
                  checkmarkColor: AppColors.primary,
                  labelStyle: TextStyle(
                    color: isSelected ? AppColors.primary : AppColors.textSecondary,
                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                    fontSize: 12,
                  ),
                );
              },
            ),
          ),

          if (_errorMessage != null)
            Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.red.shade700, size: 20),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(_errorMessage!, style: TextStyle(color: Colors.red.shade800, fontSize: 13)),
                  ),
                  TextButton(
                    onPressed: _loadPickups,
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),

          // Pickup List
          Expanded(
            child: _loading && _pickups.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: _loadPickups,
                    child: filtered.isEmpty
                        ? ListView(
                            children: const [
                              SizedBox(height: 80),
                              EmptyState(
                                icon: Icons.local_shipping_outlined,
                                title: 'No Pickups in this category',
                                subtitle: 'Approved redistribution requests appear here for live collection and delivery.',
                              ),
                            ],
                          )
                        : ListView.separated(
                            padding: const EdgeInsets.all(16),
                            itemCount: filtered.length,
                            separatorBuilder: (_, __) => const SizedBox(height: 14),
                            itemBuilder: (context, index) {
                              final item = filtered[index];
                              final pickupId = item['pickupId']?.toString() ?? item['id']?.toString() ?? '';
                              final title = item['donationTitle']?.toString() ?? 'Food Donation';
                              final donor = item['donorName']?.toString() ?? 'Donor';
                              final donorPhone = item['donorPhone']?.toString() ?? '';
                              final receiver = item['receiverOrganizationName']?.toString() ?? 'Receiver';
                              final address = item['pickupAddress']?.toString() ?? 'Pickup Address';
                              final quantity = item['requestedQuantity']?.toString() ?? '';
                              final unit = item['quantityUnit']?.toString() ?? 'meals';
                              final status = (item['status'] ?? 'PICKUP_PENDING').toString().toUpperCase();
                              final foodInspected = item['foodInspectedAt'] != null;
                              final inspectionRejected = item['inspectionRejected'] == true;
                              final inspectionNotes = item['inspectionNotes']?.toString() ?? '';
                              final tracking = _trackingData[pickupId];
                              final scheduled = item['scheduledAt'] != null
                                  ? DateFormat('MMM d, h:mm a').format(DateTime.parse(item['scheduledAt'].toString()).toLocal())
                                  : 'Scheduled';

                              return Card(
                                elevation: 2,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                                child: Padding(
                                  padding: const EdgeInsets.all(16),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Expanded(
                                            child: Text(
                                              title,
                                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ),
                                          StatusChip(status: status),
                                        ],
                                      ),
                                      const SizedBox(height: 12),

                                      // Route overview
                                      Row(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          const Column(
                                            children: [
                                              Icon(Icons.trip_origin, size: 16, color: Colors.green),
                                              SizedBox(height: 22),
                                              Icon(Icons.place, size: 16, color: Colors.blue),
                                            ],
                                          ),
                                          const SizedBox(width: 8),
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: [
                                                Text('Donor: $donor ($address)', style: const TextStyle(fontSize: 13, color: AppColors.textPrimary)),
                                                if (donorPhone.isNotEmpty)
                                                  Text('Contact: $donorPhone', style: const TextStyle(fontSize: 12, color: AppColors.textHint)),
                                                const SizedBox(height: 10),
                                                Text('Receiver: $receiver', style: const TextStyle(fontSize: 13, color: AppColors.textPrimary)),
                                              ],
                                            ),
                                          ),
                                        ],
                                      ),
                                      const Divider(height: 22),

                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Row(
                                            children: [
                                              const Icon(Icons.scale, size: 14, color: AppColors.textHint),
                                              const SizedBox(width: 4),
                                              Text('$quantity $unit', style: const TextStyle(fontSize: 13)),
                                              const SizedBox(width: 14),
                                              const Icon(Icons.access_time, size: 14, color: AppColors.textHint),
                                              const SizedBox(width: 4),
                                              Text(scheduled, style: const TextStyle(fontSize: 13)),
                                            ],
                                          ),
                                        ],
                                      ),

                                      // Live tracking breadcrumb box if active
                                      if ((status == 'EN_ROUTE_TO_PICKUP' || status == 'IN_TRANSIT') && tracking != null) ...[
                                        const SizedBox(height: 12),
                                        Container(
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
                                              Expanded(
                                                child: Text(
                                                  () {
                                                    final dist = (tracking['distanceToTargetMeters'] ?? tracking['distanceMeters']) as num?;
                                                    if (dist != null) {
                                                      return 'Distance to destination: ${dist.toInt()} m';
                                                    }
                                                    return 'Live GPS tracking active';
                                                  }(),
                                                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: Colors.blue),
                                                ),
                                              ),
                                              TextButton(
                                                onPressed: () => _sendLiveLocationUpdate(pickupId),
                                                child: const Text('Update GPS', style: TextStyle(fontSize: 12)),
                                              ),
                                            ],
                                          ),
                                        ),
                                      ],

                                      const SizedBox(height: 14),

                                      // ──────────────────────────────────────────
                                      // OPERATIONAL WORKFLOW BUTTONS
                                      // ──────────────────────────────────────────
                                      if (status == 'PICKUP_PENDING') ...[
                                        SizedBox(
                                          width: double.infinity,
                                          child: FilledButton.icon(
                                            icon: const Icon(Icons.directions_car, size: 18),
                                            label: const Text('Start Pickup Journey'),
                                            style: FilledButton.styleFrom(
                                              backgroundColor: AppColors.primary,
                                              padding: const EdgeInsets.symmetric(vertical: 12),
                                            ),
                                            onPressed: () => _startPickupJourney(pickupId),
                                          ),
                                        ),
                                      ] else if (status == 'EN_ROUTE_TO_PICKUP') ...[
                                        Row(
                                          children: [
                                            Expanded(
                                              child: OutlinedButton.icon(
                                                icon: const Icon(Icons.my_location, size: 18),
                                                label: const Text('Stream GPS'),
                                                onPressed: () => _sendLiveLocationUpdate(pickupId),
                                              ),
                                            ),
                                            const SizedBox(width: 8),
                                            Expanded(
                                              child: FilledButton.icon(
                                                icon: const Icon(Icons.check_circle, size: 18),
                                                label: const Text('Reached Donor'),
                                                style: FilledButton.styleFrom(backgroundColor: AppColors.success),
                                                onPressed: () => _confirmReachedLocation(pickupId),
                                              ),
                                            ),
                                          ],
                                        ),
                                      ] else if (status == 'AT_PICKUP') ...[
                                        if (!foodInspected || inspectionRejected) ...[
                                          if (inspectionRejected) ...[
                                            Container(
                                              width: double.infinity,
                                              margin: const EdgeInsets.only(bottom: 8),
                                              padding: const EdgeInsets.all(12),
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
                                                      Icon(Icons.cancel, size: 16, color: Colors.red),
                                                      SizedBox(width: 6),
                                                      Text('Food Safety Inspection Rejected', style: TextStyle(fontSize: 12, color: Colors.red, fontWeight: FontWeight.bold)),
                                                    ],
                                                  ),
                                                  if (inspectionNotes.isNotEmpty) ...[
                                                    const SizedBox(height: 4),
                                                    Text('Reason: $inspectionNotes', style: TextStyle(fontSize: 12, color: Colors.red.shade900)),
                                                  ],
                                                ],
                                              ),
                                            ),
                                          ] else ...[
                                            SizedBox(
                                              width: double.infinity,
                                              child: FilledButton.icon(
                                                icon: const Icon(Icons.health_and_safety, size: 18),
                                                label: const Text('Inspect Food Quality & Safety'),
                                                style: FilledButton.styleFrom(
                                                  backgroundColor: Colors.teal,
                                                  padding: const EdgeInsets.symmetric(vertical: 12),
                                                ),
                                                onPressed: () => _showFoodInspectionDialog(pickupId, title),
                                              ),
                                            ),
                                          ],
                                        ] else ...[
                                          Container(
                                            width: double.infinity,
                                            margin: const EdgeInsets.only(bottom: 8),
                                            padding: const EdgeInsets.all(8),
                                            decoration: BoxDecoration(
                                              color: Colors.teal.shade50,
                                              borderRadius: BorderRadius.circular(8),
                                            ),
                                            child: const Row(
                                              mainAxisAlignment: MainAxisAlignment.center,
                                              children: [
                                                Icon(Icons.check, size: 16, color: Colors.teal),
                                                SizedBox(width: 6),
                                                Text('Food inspected & accepted', style: TextStyle(fontSize: 12, color: Colors.teal, fontWeight: FontWeight.bold)),
                                              ],
                                            ),
                                          ),
                                          SizedBox(
                                            width: double.infinity,
                                            child: FilledButton.icon(
                                              icon: const Icon(Icons.camera_alt, size: 18),
                                              label: const Text('Verify Handover OTP & Photo Evidence'),
                                              style: FilledButton.styleFrom(
                                                backgroundColor: AppColors.primary,
                                                padding: const EdgeInsets.symmetric(vertical: 12),
                                              ),
                                              onPressed: () => _showVerifyAndPickupDialog(pickupId, title),
                                            ),
                                          ),
                                        ],
                                      ] else if (status == 'PICKED_UP') ...[
                                        SizedBox(
                                          width: double.infinity,
                                          child: FilledButton.icon(
                                            icon: const Icon(Icons.local_shipping, size: 18),
                                            label: const Text('Start Delivery to Destination'),
                                            style: FilledButton.styleFrom(
                                              backgroundColor: AppColors.primary,
                                              padding: const EdgeInsets.symmetric(vertical: 12),
                                            ),
                                            onPressed: () => _startDelivery(pickupId),
                                          ),
                                        ),
                                      ] else if (status == 'IN_TRANSIT') ...[
                                        SizedBox(
                                          width: double.infinity,
                                          child: FilledButton.icon(
                                            icon: const Icon(Icons.check_circle_outline, size: 18),
                                            label: const Text('Complete Destination Delivery'),
                                            style: FilledButton.styleFrom(
                                              backgroundColor: AppColors.success,
                                              padding: const EdgeInsets.symmetric(vertical: 12),
                                            ),
                                            onPressed: () => _showCompleteDeliveryDialog(pickupId, quantity, unit),
                                          ),
                                        ),
                                      ] else if (status == 'COMPLETED') ...[
                                        Column(
                                          children: [
                                            Container(
                                              width: double.infinity,
                                              padding: const EdgeInsets.symmetric(vertical: 8),
                                              decoration: BoxDecoration(
                                                color: Colors.green.shade50,
                                                borderRadius: BorderRadius.circular(8),
                                              ),
                                              child: const Center(
                                                child: Row(
                                                  mainAxisSize: MainAxisSize.min,
                                                  children: [
                                                    Icon(Icons.check, color: Colors.green, size: 16),
                                                    SizedBox(width: 6),
                                                    Text(
                                                      'Food Redistribution Successfully Completed',
                                                      style: TextStyle(color: Colors.green, fontSize: 13, fontWeight: FontWeight.w600),
                                                    ),
                                                  ],
                                                ),
                                              ),
                                            ),
                                            const SizedBox(height: 8),
                                            if (_pickupRatingStatuses[pickupId]?['alreadyRated'] == true)
                                              Container(
                                                width: double.infinity,
                                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                                                decoration: BoxDecoration(
                                                  color: Colors.amber.shade50,
                                                  borderRadius: BorderRadius.circular(8),
                                                  border: Border.all(color: Colors.amber.shade200),
                                                ),
                                                child: Row(
                                                  mainAxisAlignment: MainAxisAlignment.center,
                                                  children: [
                                                    const Icon(Icons.star_rounded, color: Colors.amber, size: 20),
                                                    const SizedBox(width: 4),
                                                    Text(
                                                      'Rated ${_pickupRatingStatuses[pickupId]?['myRating']?['rating']} \u2605',
                                                      style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                                                    ),
                                                    const SizedBox(width: 8),
                                                    const Text('(\u2713 Rating submitted)', style: TextStyle(color: AppColors.success, fontSize: 12, fontWeight: FontWeight.w500)),
                                                  ],
                                                ),
                                              )
                                            else
                                              SizedBox(
                                                width: double.infinity,
                                                child: FilledButton.icon(
                                                  icon: const Icon(Icons.star_outline_rounded, size: 18),
                                                  label: const Text('Rate Donor'),
                                                  style: FilledButton.styleFrom(
                                                    backgroundColor: Colors.amber.shade700,
                                                    padding: const EdgeInsets.symmetric(vertical: 10),
                                                  ),
                                                  onPressed: () async {
                                                    final submitted = await RatingDialog.show(
                                                      context,
                                                      pickupId: pickupId,
                                                      targetName: donor,
                                                      targetRole: 'Donor',
                                                    );
                                                    if (submitted == true) {
                                                      _loadRatingStatus(pickupId);
                                                    }
                                                  },
                                                ),
                                              ),
                                          ],
                                        ),
                                      ] else if (status == 'CANCELLED') ...[
                                        Container(
                                          width: double.infinity,
                                          padding: const EdgeInsets.all(12),
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
                                                  Icon(Icons.cancel, size: 16, color: Colors.red),
                                                  SizedBox(width: 6),
                                                  Text('Pickup Cancelled', style: TextStyle(fontSize: 13, color: Colors.red, fontWeight: FontWeight.bold)),
                                                ],
                                              ),
                                              if (inspectionNotes.isNotEmpty) ...[
                                                const SizedBox(height: 4),
                                                Text('Reason: $inspectionNotes', style: TextStyle(fontSize: 12, color: Colors.red.shade900)),
                                              ],
                                            ],
                                          ),
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
}

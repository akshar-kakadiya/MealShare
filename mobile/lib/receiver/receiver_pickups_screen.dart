import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/config/api_config.dart';
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
  final List<String> _tabs = ['All', 'ASSIGNED', 'PICKED_UP', 'COMPLETED'];
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
      final client = context.read<ApiClient>();
      final res = await client.get(ApiConfig.receiverPickupsUrl);
      if (res is Map<String, dynamic> && res['content'] is List) {
        setState(() {
          _pickups = List<Map<String, dynamic>>.from(res['content']);
        });
      } else if (res is List) {
        setState(() {
          _pickups = List<Map<String, dynamic>>.from(res);
        });
      }

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

  Future<void> _showOtpDialog(String pickupId, String donationTitle) async {
    final otpController = TextEditingController();
    String? dialogError;
    bool verifying = false;

    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Row(
                children: [
                  Icon(Icons.security, color: AppColors.primary),
                  SizedBox(width: 8),
                  Text('Enter Pickup OTP'),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Ask the donor for the 6-digit OTP code to verify food collection for "$donationTitle".',
                    style: const TextStyle(fontSize: 14, color: AppColors.textSecondary),
                  ),
                  const SizedBox(height: 16),
                  if (dialogError != null)
                    Container(
                      padding: const EdgeInsets.all(8),
                      margin: const EdgeInsets.only(bottom: 12),
                      decoration: BoxDecoration(
                        color: Colors.red.shade50,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        dialogError!,
                        style: TextStyle(color: Colors.red.shade700, fontSize: 13),
                      ),
                    ),
                  TextField(
                    controller: otpController,
                    autofocus: true,
                    keyboardType: TextInputType.number,
                    maxLength: 6,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      letterSpacing: 12,
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                    decoration: const InputDecoration(
                      hintText: '000000',
                      border: OutlineInputBorder(),
                      counterText: '',
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: verifying ? null : () => Navigator.pop(ctx),
                  child: const Text('Cancel'),
                ),
                FilledButton(
                  onPressed: verifying
                      ? null
                      : () async {
                          final otp = otpController.text.trim();
                          if (otp.length != 6) {
                            setDialogState(() {
                              dialogError = 'Please enter all 6 numeric digits';
                            });
                            return;
                          }
                          setDialogState(() {
                            verifying = true;
                            dialogError = null;
                          });
                          try {
                            final client = context.read<ApiClient>();
                            await client.post(
                              ApiConfig.receiverVerifyOtpUrl(pickupId),
                              body: {'otp': otp},
                            );
                            if (ctx.mounted) {
                              Navigator.pop(ctx);
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('OTP Verified! Food collection confirmed.'),
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
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Text('Verify OTP'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<void> _completeDelivery(String pickupId, String donationTitle) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirm Pickup Completion'),
        content: Text('Confirm that "$donationTitle" has been safely collected and received?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: AppColors.success),
            child: const Text('Confirm Received'),
          ),
        ],
      ),
    );

    if (confirmed != true || !mounted) return;

    final client = context.read<ApiClient>();
    setState(() => _loading = true);
    try {
      await client.post(ApiConfig.receiverCompletePickupUrl(pickupId));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Pickup marked COMPLETED! Food redistribution finalized.'),
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
          SnackBar(content: Text('Failed to complete: $e'), backgroundColor: AppColors.error),
        );
        setState(() => _loading = false);
      }
    }
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
        title: const Text('Scheduled Food Pickups'),
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
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: const BoxDecoration(
                          color: Colors.green,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Text(
                        'Direct Collection',
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
                return FilterChip(
                  label: Text(tab.replaceAll('_', ' ')),
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
                    child: Text(
                      _errorMessage!,
                      style: TextStyle(color: Colors.red.shade800, fontSize: 13),
                    ),
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
                                title: 'No Pickups Scheduled',
                                subtitle: 'Scheduled pickups for your approved requests will appear here.',
                              ),
                            ],
                          )
                        : ListView.separated(
                            padding: const EdgeInsets.all(16),
                            itemCount: filtered.length,
                            separatorBuilder: (_, __) => const SizedBox(height: 12),
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
                              final status = (item['status'] ?? 'ASSIGNED').toString().toUpperCase();
                              final scheduled = item['scheduledAt'] != null
                                  ? DateFormat('MMM d, h:mm a').format(DateTime.parse(item['scheduledAt'].toString()).toLocal())
                                  : 'Scheduled';

                              return Card(
                                elevation: 1,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
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
                                              SizedBox(height: 20),
                                              Icon(Icons.place, size: 16, color: Colors.blue),
                                            ],
                                          ),
                                          const SizedBox(width: 8),
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: [
                                                Text(
                                                  'Donor: $donor ($address)',
                                                  style: const TextStyle(fontSize: 13, color: AppColors.textPrimary),
                                                ),
                                                if (donorPhone.isNotEmpty)
                                                  Text(
                                                    'Contact: $donorPhone',
                                                    style: const TextStyle(fontSize: 12, color: AppColors.textHint),
                                                  ),
                                                const SizedBox(height: 10),
                                                Text(
                                                  'Receiver: $receiver',
                                                  style: const TextStyle(fontSize: 13, color: AppColors.textPrimary),
                                                ),
                                              ],
                                            ),
                                          ),
                                        ],
                                      ),
                                      const Divider(height: 24),

                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Row(
                                            children: [
                                              const Icon(Icons.scale, size: 14, color: AppColors.textHint),
                                              const SizedBox(width: 4),
                                              Text('$quantity $unit', style: const TextStyle(fontSize: 13)),
                                              const SizedBox(width: 12),
                                              const Icon(Icons.access_time, size: 14, color: AppColors.textHint),
                                              const SizedBox(width: 4),
                                              Text(scheduled, style: const TextStyle(fontSize: 13)),
                                            ],
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 12),

                                      // Workflow Action Buttons
                                      if (status == 'ASSIGNED')
                                        SizedBox(
                                          width: double.infinity,
                                          child: FilledButton.icon(
                                            icon: const Icon(Icons.key, size: 18),
                                            label: const Text('Verify Pickup OTP'),
                                            style: FilledButton.styleFrom(
                                              backgroundColor: AppColors.primary,
                                              padding: const EdgeInsets.symmetric(vertical: 12),
                                            ),
                                            onPressed: () => _showOtpDialog(pickupId, title),
                                          ),
                                        )
                                      else if (status == 'PICKED_UP')
                                        SizedBox(
                                          width: double.infinity,
                                          child: FilledButton.icon(
                                            icon: const Icon(Icons.check_circle_outline, size: 18),
                                            label: const Text('Complete Food Collection'),
                                            style: FilledButton.styleFrom(
                                              backgroundColor: AppColors.success,
                                              padding: const EdgeInsets.symmetric(vertical: 12),
                                            ),
                                            onPressed: () => _completeDelivery(pickupId, title),
                                          ),
                                        )
                                      else if (status == 'COMPLETED')
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
                                                      'Food Collection Successfully Completed',
                                                      style: TextStyle(
                                                        color: Colors.green,
                                                        fontSize: 13,
                                                        fontWeight: FontWeight.w600,
                                                      ),
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

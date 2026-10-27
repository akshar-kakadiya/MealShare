import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/api_config.dart';
import '../../shared/api_client.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_theme.dart';

/// Modal bottom sheet displaying genuine receiver organization profile data for donor review
class ReceiverProfileBottomSheet extends StatefulWidget {
  final String requestId;
  final String fallbackName;

  const ReceiverProfileBottomSheet({
    super.key,
    required this.requestId,
    required this.fallbackName,
  });

  @override
  State<ReceiverProfileBottomSheet> createState() => _ReceiverProfileBottomSheetState();
}

class _ReceiverProfileBottomSheetState extends State<ReceiverProfileBottomSheet> {
  Map<String, dynamic>? _profile;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchProfile();
  }

  Future<void> _fetchProfile() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final client = context.read<ApiClient>();
      final res = await client.get(ApiConfig.donorReceiverProfileUrl(widget.requestId));
      if (mounted && res is Map<String, dynamic>) {
        setState(() {
          _profile = res;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Could not load receiver profile: $e';
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.85,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.md),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Drag handle
          Center(
            child: Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(bottom: AppSpacing.md),
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),

          // Header
          Row(
            children: [
              const Icon(Icons.verified_user_outlined, color: AppColors.primary, size: 24),
              const SizedBox(width: AppSpacing.sm),
              Text(
                'Receiver Organization Profile',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: AppColors.textPrimary,
                    ),
              ),
              const Spacer(),
              IconButton(
                icon: const Icon(Icons.close),
                onPressed: () => Navigator.of(context).pop(),
              ),
            ],
          ),
          const Divider(),

          Expanded(
            child: _loading
                ? const Center(
                    child: Padding(
                      padding: EdgeInsets.all(AppSpacing.xl),
                      child: CircularProgressIndicator(),
                    ),
                  )
                : _error != null
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.error_outline, color: AppColors.error, size: 40),
                            const SizedBox(height: 8),
                            Text(_error!, style: const TextStyle(color: AppColors.error), textAlign: TextAlign.center),
                            const SizedBox(height: 12),
                            FilledButton.tonal(
                              onPressed: _fetchProfile,
                              child: const Text('Retry'),
                            ),
                          ],
                        ),
                      )
                    : SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const SizedBox(height: AppSpacing.sm),
                            // Organization Name & Badges
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                CircleAvatar(
                                  radius: 28,
                                  backgroundColor: AppColors.primarySurface,
                                  child: Text(
                                    (_profile?['orgName']?.toString().isNotEmpty == true)
                                        ? _profile!['orgName'][0].toUpperCase()
                                        : widget.fallbackName.isNotEmpty
                                            ? widget.fallbackName[0].toUpperCase()
                                            : 'R',
                                    style: const TextStyle(
                                      fontSize: 22,
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.primary,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: AppSpacing.md),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        _profile?['orgName']?.toString() ?? widget.fallbackName,
                                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                              fontWeight: FontWeight.bold,
                                            ),
                                      ),
                                      const SizedBox(height: 4),
                                      Wrap(
                                        spacing: 6,
                                        runSpacing: 4,
                                        children: [
                                          _buildVerificationBadge(_profile?['verificationStatus']?.toString()),
                                          _buildOrgTypeChip(_profile?['orgType']?.toString()),
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: AppSpacing.lg),

                            // Ratings & Capacity Metrics
                            Container(
                              padding: const EdgeInsets.all(AppSpacing.md),
                              decoration: BoxDecoration(
                                color: Colors.grey.shade50,
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: Colors.grey.shade200),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceAround,
                                children: [
                                  _buildMetricItem(
                                    icon: Icons.star_rounded,
                                    iconColor: Colors.amber.shade700,
                                    value: '${(_profile?['ratingAverage'] as num?)?.toStringAsFixed(1) ?? '5.0'} ★',
                                    label: 'Donor Rating',
                                  ),
                                  Container(width: 1, height: 36, color: Colors.grey.shade300),
                                  _buildMetricItem(
                                    icon: Icons.people_outline,
                                    iconColor: AppColors.primary,
                                    value: '${_profile?['beneficiaryCapacity'] ?? 50}',
                                    label: 'Beneficiaries',
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: AppSpacing.lg),

                            // Description
                            if (_profile?['description'] != null && _profile!['description'].toString().isNotEmpty) ...[
                              Text('About Organization', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                              const SizedBox(height: 4),
                              Text(
                                _profile!['description'].toString(),
                                style: TextStyle(color: Colors.grey.shade800, height: 1.4),
                              ),
                              const SizedBox(height: AppSpacing.lg),
                            ],

                            // Contact Details
                            Text('Contact Information', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                            const SizedBox(height: 8),
                            if (_profile?['contactPerson'] != null)
                              _buildInfoRow(Icons.person_outline, 'Contact Person', _profile!['contactPerson'].toString()),
                            if (_profile?['phone'] != null)
                              _buildInfoRow(Icons.phone_outlined, 'Phone', _profile!['phone'].toString()),
                            if (_profile?['email'] != null)
                              _buildInfoRow(Icons.email_outlined, 'Email', _profile!['email'].toString()),
                            const SizedBox(height: AppSpacing.lg),

                            // Location & Service Area
                            Text('Service Area & Address', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                            const SizedBox(height: 8),
                            _buildInfoRow(
                              Icons.location_on_outlined,
                              'Registered Address',
                              _profile?['addressText']?.toString() ?? 'Address not specified',
                            ),
                            const SizedBox(height: AppSpacing.xl),
                          ],
                        ),
                      ),
          ),

          // Bottom Action
          Padding(
            padding: const EdgeInsets.only(top: AppSpacing.sm, bottom: AppSpacing.sm),
            child: FilledButton.tonal(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Back to Request'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVerificationBadge(String? status) {
    final isVerified = status?.toUpperCase() == 'VERIFIED';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: isVerified ? AppColors.successSurface : AppColors.warningSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: isVerified ? AppColors.success : AppColors.warning),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            isVerified ? Icons.check_circle : Icons.hourglass_top,
            size: 12,
            color: isVerified ? AppColors.success : AppColors.warning,
          ),
          const SizedBox(width: 4),
          Text(
            isVerified ? 'Verified' : 'Pending Verification',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: isVerified ? AppColors.success : AppColors.warning,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOrgTypeChip(String? orgType) {
    final typeClean = (orgType ?? 'Organization').replaceAll('_', ' ');
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: Colors.blue.shade50,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        typeClean,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w500,
          color: Colors.blue.shade800,
        ),
      ),
    );
  }

  Widget _buildMetricItem({
    required IconData icon,
    required Color iconColor,
    required String value,
    required String label,
  }) {
    return Column(
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 18, color: iconColor),
            const SizedBox(width: 4),
            Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ],
        ),
        const SizedBox(height: 2),
        Text(label, style: TextStyle(color: Colors.grey.shade600, fontSize: 11)),
      ],
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: Colors.grey.shade600),
          const SizedBox(width: 8),
          Expanded(
            child: Text.rich(
              TextSpan(
                style: const TextStyle(color: Colors.black87, fontSize: 13),
                children: [
                  TextSpan(text: '$label: ', style: const TextStyle(fontWeight: FontWeight.w600)),
                  TextSpan(text: value),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Helper to display receiver organization profile modal bottom sheet
void showReceiverProfileModal(BuildContext context, String requestId, String receiverName) {
  showModalBottomSheet(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (modalCtx) => ReceiverProfileBottomSheet(
      requestId: requestId,
      fallbackName: receiverName,
    ),
  );
}

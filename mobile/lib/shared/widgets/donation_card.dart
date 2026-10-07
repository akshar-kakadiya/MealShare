import 'package:flutter/material.dart';
import '../../mock/models.dart';
import '../../theme/app_colors.dart';
import '../../theme/app_theme.dart';
import 'status_chip.dart';

class DonationCard extends StatelessWidget {
  final Donation donation;
  final VoidCallback? onTap;
  final bool showDistance;
  final bool showDonor;

  const DonationCard({
    super.key,
    required this.donation,
    this.onTap,
    this.showDistance = false,
    this.showDonor = false,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppRadius.card),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Food category icon
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: AppColors.primarySurface,
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
                child: Icon(
                  donation.categoryIcon,
                  color: AppColors.primary,
                  size: 26,
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              // Content
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Title + Status
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            donation.title,
                            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.w600,
                              color: AppColors.textPrimary,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(width: 8),
                        StatusChip(status: donation.status),
                      ],
                    ),
                    if (showDonor) ...[
                      const SizedBox(height: 4),
                      Text(
                        donation.donorName,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                    const SizedBox(height: 8),
                    // Metadata row
                    Wrap(
                      spacing: 12,
                      runSpacing: 4,
                      children: [
                        _MetaItem(
                          icon: Icons.restaurant,
                          text: '${donation.quantity} ${donation.unit}',
                        ),
                        _MetaItem(
                          icon: Icons.schedule,
                          text: donation.consumeBefore,
                        ),
                        if (showDistance && donation.distance != null)
                          _MetaItem(
                            icon: Icons.location_on,
                            text: '${donation.distance} km',
                          ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 4),
              Icon(Icons.chevron_right, color: AppColors.textHint, size: 20),
            ],
          ),
        ),
      ),
    );
  }
}

class _MetaItem extends StatelessWidget {
  final IconData icon;
  final String text;

  const _MetaItem({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: AppColors.textHint),
        const SizedBox(width: 3),
        Text(
          text,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: AppColors.textSecondary,
            fontSize: 12,
          ),
        ),
      ],
    );
  }
}

import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';

class StatusChip extends StatelessWidget {
  final String status;
  final double? fontSize;

  const StatusChip({super.key, required this.status, this.fontSize});

  @override
  Widget build(BuildContext context) {
    final config = _getStatusConfig(status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: config.backgroundColor,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        config.label,
        style: TextStyle(
          color: config.textColor,
          fontSize: fontSize ?? 11,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.3,
        ),
      ),
    );
  }

  _StatusConfig _getStatusConfig(String status) {
    switch (status.toUpperCase()) {
      case 'AVAILABLE':
        return _StatusConfig('Available', AppColors.success, AppColors.successSurface);
      case 'REQUESTED':
        return _StatusConfig('Requested', AppColors.warning, AppColors.warningSurface);
      case 'PENDING':
        return _StatusConfig('Pending', AppColors.warning, AppColors.warningSurface);
      case 'APPROVED':
        return _StatusConfig('Approved', AppColors.info, AppColors.infoSurface);
      case 'READY_FOR_PICKUP':
        return _StatusConfig('Ready for Pickup', AppColors.info, AppColors.infoSurface);
      case 'PICKUP_IN_PROGRESS':
        return _StatusConfig('Pickup in Progress', AppColors.inProgress, const Color(0xFFF3E5F5));
      case 'PICKED_UP':
        return _StatusConfig('Picked Up', AppColors.inProgress, const Color(0xFFF3E5F5));
      case 'DELIVERED':
        return _StatusConfig('Delivered', AppColors.success, AppColors.successSurface);
      case 'COMPLETED':
        return _StatusConfig('Completed', AppColors.success, AppColors.successSurface);
      case 'REJECTED':
        return _StatusConfig('Rejected', AppColors.error, AppColors.errorSurface);
      case 'WITHDRAWN':
        return _StatusConfig('Withdrawn', AppColors.cancelled, const Color(0xFFF5F5F5));
      case 'EXPIRED':
        return _StatusConfig('Expired', AppColors.expired, const Color(0xFFF5F5F5));
      case 'CANCELLED':
        return _StatusConfig('Cancelled', AppColors.cancelled, const Color(0xFFF5F5F5));
      case 'ASSIGNED':
        return _StatusConfig('Assigned', AppColors.info, AppColors.infoSurface);
      case 'FAILED':
        return _StatusConfig('Failed', AppColors.error, AppColors.errorSurface);
      default:
        return _StatusConfig(status, AppColors.textSecondary, const Color(0xFFF5F5F5));
    }
  }
}

class _StatusConfig {
  final String label;
  final Color textColor;
  final Color backgroundColor;
  _StatusConfig(this.label, this.textColor, this.backgroundColor);
}

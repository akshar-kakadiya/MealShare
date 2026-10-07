import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // Primary - Warm Green (sustainability, food, trust)
  static const Color primary = Color(0xFF2E7D32);
  static const Color primaryLight = Color(0xFF4CAF50);
  static const Color primaryDark = Color(0xFF1B5E20);
  static const Color primarySurface = Color(0xFFE8F5E9);

  // Secondary - Warm Orange (community, warmth, action)
  static const Color secondary = Color(0xFFFF8F00);
  static const Color secondaryLight = Color(0xFFFFB300);
  static const Color secondaryDark = Color(0xFFEF6C00);
  static const Color secondarySurface = Color(0xFFFFF3E0);

  // Background
  static const Color background = Color(0xFFF5F7F5);
  static const Color surface = Colors.white;
  static const Color surfaceVariant = Color(0xFFF0F4F0);

  // Text
  static const Color textPrimary = Color(0xFF1A1C1A);
  static const Color textSecondary = Color(0xFF5F6368);
  static const Color textHint = Color(0xFF9AA0A6);
  static const Color textOnPrimary = Colors.white;

  // Status
  static const Color success = Color(0xFF2E7D32);
  static const Color successSurface = Color(0xFFE8F5E9);
  static const Color warning = Color(0xFFF9A825);
  static const Color warningSurface = Color(0xFFFFF8E1);
  static const Color error = Color(0xFFD32F2F);
  static const Color errorSurface = Color(0xFFFFEBEE);
  static const Color info = Color(0xFF1565C0);
  static const Color infoSurface = Color(0xFFE3F2FD);

  // Specific statuses
  static const Color available = Color(0xFF2E7D32);
  static const Color pending = Color(0xFFF9A825);
  static const Color approved = Color(0xFF1565C0);
  static const Color inProgress = Color(0xFF7B1FA2);
  static const Color completed = Color(0xFF2E7D32);
  static const Color rejected = Color(0xFFD32F2F);
  static const Color cancelled = Color(0xFF757575);
  static const Color expired = Color(0xFF757575);

  // Border
  static const Color border = Color(0xFFE0E0E0);
  static const Color borderLight = Color(0xFFF0F0F0);

  // Misc
  static const Color divider = Color(0xFFEEEEEE);
  static const Color shimmer = Color(0xFFE0E0E0);
  static const Color overlay = Color(0x80000000);
}

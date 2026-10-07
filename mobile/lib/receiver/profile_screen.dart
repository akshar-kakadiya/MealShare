import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../auth/auth_service.dart';
import '../config/api_config.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/stat_card.dart';

class ReceiverProfileScreen extends StatelessWidget {
  const ReceiverProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final user = state.currentUser;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          // Profile Header
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.xl),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 40,
                    backgroundColor: AppColors.infoSurface,
                    child: Text(
                      user.name[0],
                      style: TextStyle(fontSize: 32, fontWeight: FontWeight.w700, color: AppColors.info),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(user.name, style: Theme.of(context).textTheme.titleLarge),
                      if (user.isVerified) ...[
                        const SizedBox(width: 6),
                        Icon(Icons.verified, color: AppColors.info, size: 20),
                      ],
                    ],
                  ),
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.infoSurface,
                      borderRadius: BorderRadius.circular(AppRadius.chip),
                    ),
                    child: Text(
                      'Verified Organization',
                      style: TextStyle(color: AppColors.info, fontSize: 12, fontWeight: FontWeight.w500),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // Impact Stats
          Row(
            children: [
              Expanded(child: StatCard(label: 'Meals', value: '${state.receiverMealsReceived}', icon: Icons.restaurant)),
              const SizedBox(width: AppSpacing.sm),
              Expanded(child: StatCard(label: 'Food Items', value: '${state.receiverFoodReceived}', icon: Icons.inventory_2, backgroundColor: AppColors.secondarySurface, iconColor: AppColors.secondary)),
              const SizedBox(width: AppSpacing.sm),
              Expanded(child: StatCard(label: 'Pickups', value: '${state.receiverSuccessfulPickups}', icon: Icons.local_shipping, backgroundColor: AppColors.infoSurface, iconColor: AppColors.info)),
            ],
          ),
          const SizedBox(height: AppSpacing.lg),

          // Organization Details
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Organization Details', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                  const SizedBox(height: AppSpacing.md),
                  _ProfileRow(Icons.business, 'Type', user.organizationType),
                  _ProfileRow(Icons.email, 'Email', user.email),
                  _ProfileRow(Icons.phone, 'Phone', user.phone),
                  _ProfileRow(Icons.location_on, 'Address', user.address),
                  _ProfileRow(Icons.calendar_today, 'Member since', '${_monthName(user.joinedAt.month)} ${user.joinedAt.year}'),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // Settings
          Card(
            child: Column(
              children: [
                _SettingsTile(
                  icon: Icons.notifications,
                  title: 'Notifications',
                  subtitle: 'Manage notification preferences',
                  onTap: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Not available in this version — Planned for future release')),
                    );
                  },
                ),
                const Divider(height: 1),
                _SettingsTile(
                  icon: Icons.security,
                  title: 'Account & Security',
                  subtitle: 'Password, verification',
                  onTap: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Not available in this version — Planned for future release')),
                    );
                  },
                ),
                const Divider(height: 1),
                _SettingsTile(
                  icon: Icons.help,
                  title: 'Help & Support',
                  subtitle: 'FAQ, contact us',
                  onTap: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Not available in this version — Planned for future release')),
                    );
                  },
                ),
                if (!ApiConfig.isRealMode) ...[
                  const Divider(height: 1),
                  _SettingsTile(
                    icon: Icons.swap_horiz,
                    title: 'Switch to Donor',
                    subtitle: 'View as FreshBite Restaurant',
                    onTap: () {
                      state.switchRole();
                      context.go('/donor/home');
                    },
                  ),
                ],
                const Divider(height: 1),
                _SettingsTile(
                  icon: Icons.logout,
                  title: 'Logout',
                  subtitle: 'Sign out of your account',
                  isDestructive: true,
                  onTap: () async {
                    final authService = context.read<AuthService?>();
                    if (authService != null) {
                      await authService.signOut();
                    }
                    if (context.mounted) {
                      context.go('/login');
                    }
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.xxxl),
        ],
      ),
    );
  }

  String _monthName(int month) {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return months[month - 1];
  }
}

class _ProfileRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  const _ProfileRow(this.icon, this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon, size: 18, color: AppColors.textHint),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: Theme.of(context).textTheme.labelSmall),
              Text(value, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.textPrimary)),
            ],
          ),
        ],
      ),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final bool isDestructive;
  const _SettingsTile({required this.icon, required this.title, required this.subtitle, required this.onTap, this.isDestructive = false});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: isDestructive ? AppColors.error : AppColors.textSecondary),
      title: Text(title, style: TextStyle(color: isDestructive ? AppColors.error : null, fontWeight: FontWeight.w500)),
      subtitle: Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
      trailing: Icon(Icons.chevron_right, color: AppColors.textHint),
      onTap: onTap,
    );
  }
}

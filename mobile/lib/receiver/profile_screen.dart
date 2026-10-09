import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../auth/auth_service.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/stat_card.dart';

class ReceiverProfileScreen extends StatefulWidget {
  const ReceiverProfileScreen({super.key});

  @override
  State<ReceiverProfileScreen> createState() => _ReceiverProfileScreenState();
}

class _ReceiverProfileScreenState extends State<ReceiverProfileScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchReceiverProfileRemote();
    });
  }

  void _showEditProfileDialog() {
    final state = context.read<AppState>();
    final user = state.currentUser;
    final nameCtrl = TextEditingController(text: user.name);
    final regCtrl = TextEditingController(text: user.registrationNumber ?? '');
    final typeCtrl = TextEditingController(text: user.organizationType.isNotEmpty ? user.organizationType : 'NGO');
    final addressCtrl = TextEditingController(text: user.address);
    final capacityCtrl = TextEditingController(text: (user.beneficiaryCapacity ?? 50).toString());
    bool saving = false;
    String? errorMsg;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Edit Receiver Profile'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (errorMsg != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Text(errorMsg!, style: const TextStyle(color: AppColors.error, fontSize: 12)),
                  ),
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: 'Organization Name'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: regCtrl,
                  decoration: const InputDecoration(labelText: 'Registration / Trust Number'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: ['NGO', 'SHELTER', 'COMMUNITY_KITCHEN', 'ORPHANAGE', 'OLD_AGE_HOME', 'FOOD_BANK', 'OTHER'].contains(typeCtrl.text.toUpperCase())
                      ? typeCtrl.text.toUpperCase()
                      : 'NGO',
                  decoration: const InputDecoration(labelText: 'Organization Type'),
                  items: const [
                    DropdownMenuItem(value: 'NGO', child: Text('Non-Profit (NGO)')),
                    DropdownMenuItem(value: 'SHELTER', child: Text('Homeless Shelter')),
                    DropdownMenuItem(value: 'COMMUNITY_KITCHEN', child: Text('Community Kitchen')),
                    DropdownMenuItem(value: 'ORPHANAGE', child: Text('Orphanage')),
                    DropdownMenuItem(value: 'OLD_AGE_HOME', child: Text('Senior Care Home')),
                    DropdownMenuItem(value: 'FOOD_BANK', child: Text('Food Bank')),
                    DropdownMenuItem(value: 'OTHER', child: Text('Other')),
                  ],
                  onChanged: (val) {
                    if (val != null) typeCtrl.text = val;
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: capacityCtrl,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Beneficiary Capacity (People Served)'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: addressCtrl,
                  decoration: const InputDecoration(labelText: 'Operating Address'),
                  maxLines: 2,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: saving ? null : () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: saving
                  ? null
                  : () async {
                      setDialogState(() {
                        saving = true;
                        errorMsg = null;
                      });
                      try {
                        final cap = int.tryParse(capacityCtrl.text.trim()) ?? 50;
                        await state.updateReceiverProfileRemote(
                          orgName: nameCtrl.text.trim(),
                          orgType: typeCtrl.text.trim().toUpperCase(),
                          registrationNumber: regCtrl.text.trim().isNotEmpty ? regCtrl.text.trim() : 'REG-PENDING',
                          addressText: addressCtrl.text.trim(),
                          beneficiaryCapacity: cap,
                        );
                        if (ctx.mounted) {
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Profile updated successfully!'), backgroundColor: AppColors.success),
                          );
                        }
                      } catch (e) {
                        setDialogState(() {
                          saving = false;
                          errorMsg = 'Failed to update profile: $e';
                        });
                      }
                    },
              child: saving
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final user = state.currentUser;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            tooltip: 'Edit Profile',
            onPressed: _showEditProfileDialog,
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => state.fetchReceiverProfileRemote(),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => state.fetchReceiverProfileRemote(),
        child: ListView(
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
                        user.name.isNotEmpty ? user.name[0].toUpperCase() : 'R',
                        style: TextStyle(fontSize: 32, fontWeight: FontWeight.w700, color: AppColors.info),
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Flexible(
                          child: Text(
                            user.name,
                            style: Theme.of(context).textTheme.titleLarge,
                            textAlign: TextAlign.center,
                          ),
                        ),
                        if (user.isVerified) ...[
                          const SizedBox(width: 6),
                          const Icon(Icons.verified, color: AppColors.info, size: 20),
                        ],
                      ],
                    ),
                    const SizedBox(height: 6),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                          decoration: BoxDecoration(
                            color: AppColors.infoSurface,
                            borderRadius: BorderRadius.circular(AppRadius.chip),
                          ),
                          child: Text(
                            user.isVerified ? 'Verified Organization' : 'Pending Verification',
                            style: TextStyle(color: AppColors.info, fontSize: 12, fontWeight: FontWeight.w600),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: Colors.amber.shade50,
                            borderRadius: BorderRadius.circular(AppRadius.chip),
                            border: Border.all(color: Colors.amber.shade200),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.star, color: Colors.amber, size: 14),
                              const SizedBox(width: 3),
                              Text(
                                user.ratingAverage.toStringAsFixed(1),
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.amber),
                              ),
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
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('Organization Details', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                        TextButton.icon(
                          onPressed: _showEditProfileDialog,
                          icon: const Icon(Icons.edit, size: 14),
                          label: const Text('Edit', style: TextStyle(fontSize: 12)),
                        ),
                      ],
                    ),
                    const Divider(height: 12),
                    _ProfileRow(Icons.business, 'Type', user.organizationType.isNotEmpty ? user.organizationType : 'Institutional Receiver'),
                    if (user.registrationNumber != null && user.registrationNumber!.isNotEmpty)
                      _ProfileRow(Icons.badge_outlined, 'Registration No.', user.registrationNumber!),
                    if (user.beneficiaryCapacity != null && user.beneficiaryCapacity! > 0)
                      _ProfileRow(Icons.group_outlined, 'Beneficiary Capacity', '${user.beneficiaryCapacity} persons/day'),
                    _ProfileRow(Icons.email, 'Email', user.email),
                    _ProfileRow(Icons.phone, 'Phone', user.phone.isNotEmpty ? user.phone : 'Not specified'),
                    _ProfileRow(Icons.location_on, 'Address', user.address.isNotEmpty ? user.address : 'Bengaluru, Karnataka'),
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
                    icon: Icons.edit_note,
                    title: 'Edit Organization Profile',
                    subtitle: 'Update capacity, address, registration',
                    onTap: _showEditProfileDialog,
                  ),
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: AppColors.textHint),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: Theme.of(context).textTheme.labelSmall),
                Text(value, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.textPrimary)),
              ],
            ),
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
      trailing: const Icon(Icons.chevron_right, color: AppColors.textHint),
      onTap: onTap,
    );
  }
}

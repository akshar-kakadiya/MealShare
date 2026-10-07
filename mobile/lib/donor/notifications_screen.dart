import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../mock/app_state.dart';
import '../mock/models.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/notification_card.dart';
import '../shared/widgets/empty_state.dart';

class DonorNotificationsScreen extends StatefulWidget {
  const DonorNotificationsScreen({super.key});

  @override
  State<DonorNotificationsScreen> createState() => _DonorNotificationsScreenState();
}

class _DonorNotificationsScreenState extends State<DonorNotificationsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchNotificationsRemote();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final notifications = state.donorNotifications;
    final unread = notifications.where((n) => !n.isRead).length;

    // Group by day
    final today = <AppNotification>[];
    final yesterday = <AppNotification>[];
    final earlier = <AppNotification>[];
    final now = DateTime.now();

    for (final n in notifications) {
      final diff = now.difference(n.timestamp);
      if (diff.inHours < 24) {
        today.add(n);
      } else if (diff.inHours < 48) {
        yesterday.add(n);
      } else {
        earlier.add(n);
      }
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          if (unread > 0)
            TextButton(
              onPressed: () => state.markAllNotificationsAsReadRemote(),
              child: const Text('Mark all read'),
            ),
        ],
      ),
      body: Column(
        children: [
          if (state.notificationsError != null)
            Container(
              margin: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
              padding: const EdgeInsets.all(AppSpacing.md),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(AppRadius.md),
                border: Border.all(color: Colors.red.shade200),
              ),
              child: Row(
                children: [
                  const Icon(Icons.cloud_off, color: Colors.red, size: 20),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      'Backend unavailable: ${state.notificationsError}',
                      style: TextStyle(color: Colors.red.shade900, fontSize: 13),
                    ),
                  ),
                  TextButton(
                    onPressed: () => context.read<AppState>().fetchNotificationsRemote(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          Expanded(
            child: state.isNotificationsLoading && notifications.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: () => context.read<AppState>().fetchNotificationsRemote(),
                    child: notifications.isEmpty
                        ? ListView(
                            children: [
                              SizedBox(
                                height: MediaQuery.of(context).size.height * 0.7,
                                child: EmptyState(
                                  icon: state.notificationsError != null ? Icons.error_outline : Icons.notifications_none,
                                  title: state.notificationsError != null ? 'Could not load notifications' : 'No notifications',
                                  subtitle: state.notificationsError != null
                                      ? 'Unable to reach the backend server. Please check your connection and tap retry.'
                                      : 'You\'re all caught up! New notifications will appear here.',
                                  actionLabel: state.notificationsError != null ? 'Retry' : null,
                                  onAction: state.notificationsError != null
                                      ? () => context.read<AppState>().fetchNotificationsRemote()
                                      : null,
                                ),
                              ),
                            ],
                          )
                        : ListView(
                            children: [
                              if (today.isNotEmpty) ...[
                                _GroupHeader(title: 'Today', count: today.where((n) => !n.isRead).length),
                                ...today.map((n) => NotificationCard(
                                  notification: n,
                                  onTap: () => state.markNotificationAsReadRemote(n.id),
                                )),
                              ],
                              if (yesterday.isNotEmpty) ...[
                                _GroupHeader(title: 'Yesterday', count: yesterday.where((n) => !n.isRead).length),
                                ...yesterday.map((n) => NotificationCard(
                                  notification: n,
                                  onTap: () => state.markNotificationAsReadRemote(n.id),
                                )),
                              ],
                              if (earlier.isNotEmpty) ...[
                                _GroupHeader(title: 'Earlier', count: earlier.where((n) => !n.isRead).length),
                                ...earlier.map((n) => NotificationCard(
                                  notification: n,
                                  onTap: () => state.markNotificationAsReadRemote(n.id),
                                )),
                              ],
                            ],
                          ),
                  ),
          ),
        ],
      ),
    );
  }
}

class _GroupHeader extends StatelessWidget {
  final String title;
  final int count;

  const _GroupHeader({required this.title, required this.count});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.sm),
      color: AppColors.surfaceVariant,
      child: Row(
        children: [
          Text(title, style: Theme.of(context).textTheme.labelLarge),
          if (count > 0) ...[
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text('$count', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w600)),
            ),
          ],
        ],
      ),
    );
  }
}

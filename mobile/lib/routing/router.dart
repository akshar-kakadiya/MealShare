import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/app_colors.dart';
import '../auth/screens/login_screen.dart';
import '../auth/screens/register_screen.dart';
import '../donor/donor_pickups_screen.dart';
import '../receiver/receiver_pickups_screen.dart';
import '../donor/home_screen.dart';
import '../donor/donations_list_screen.dart';
import '../donor/create_donation_screen.dart';
import '../donor/donation_details_screen.dart';
import '../donor/requests_screen.dart';
import '../donor/notifications_screen.dart';
import '../donor/profile_screen.dart';
import '../receiver/home_screen.dart';
import '../receiver/discover_screen.dart';
import '../receiver/donation_details_screen.dart';
import '../receiver/my_requests_screen.dart';
import '../receiver/pickup_tracking_screen.dart';
import '../receiver/notifications_screen.dart';
import '../receiver/profile_screen.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

final router = GoRouter(
  navigatorKey: _rootNavigatorKey,
  initialLocation: '/donor/home',
  routes: [
    GoRoute(
      path: '/login',
      parentNavigatorKey: _rootNavigatorKey,
      builder: (c, s) => const LoginScreen(),
    ),
    GoRoute(
      path: '/register',
      parentNavigatorKey: _rootNavigatorKey,
      builder: (c, s) => const RegisterScreen(),
    ),

    // ─── Donor Shell ───
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) {
        return ScaffoldWithNavBar(
          navigationShell: navigationShell,
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
            NavigationDestination(icon: Icon(Icons.volunteer_activism_outlined), selectedIcon: Icon(Icons.volunteer_activism), label: 'Donations'),
            NavigationDestination(icon: Icon(Icons.inbox_outlined), selectedIcon: Icon(Icons.inbox), label: 'Requests'),
            NavigationDestination(icon: Icon(Icons.local_shipping_outlined), selectedIcon: Icon(Icons.local_shipping), label: 'Pickup'),
            NavigationDestination(icon: Icon(Icons.notifications_outlined), selectedIcon: Icon(Icons.notifications), label: 'Alerts'),
            NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
          ],
        );
      },
      branches: [
        StatefulShellBranch(routes: [
          GoRoute(path: '/donor/home', builder: (c, s) => const DonorHomeScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/donor/donations', builder: (c, s) => const DonorDonationsListScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/donor/requests', builder: (c, s) => const DonorRequestsScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/donor/pickups', builder: (c, s) => const DonorPickupsScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/donor/notifications', builder: (c, s) => const DonorNotificationsScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/donor/profile', builder: (c, s) => const DonorProfileScreen()),
        ]),
      ],
    ),

    // ─── Receiver Shell ───
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) {
        return ScaffoldWithNavBar(
          navigationShell: navigationShell,
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
            NavigationDestination(icon: Icon(Icons.search_outlined), selectedIcon: Icon(Icons.search), label: 'Discover'),
            NavigationDestination(icon: Icon(Icons.inbox_outlined), selectedIcon: Icon(Icons.inbox), label: 'Requests'),
            NavigationDestination(icon: Icon(Icons.local_shipping_outlined), selectedIcon: Icon(Icons.local_shipping), label: 'Pickup'),
            NavigationDestination(icon: Icon(Icons.notifications_outlined), selectedIcon: Icon(Icons.notifications), label: 'Alerts'),
            NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
          ],
        );
      },
      branches: [
        StatefulShellBranch(routes: [
          GoRoute(path: '/receiver/home', builder: (c, s) => const ReceiverHomeScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/receiver/discover', builder: (c, s) => const ReceiverDiscoverScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/receiver/requests', builder: (c, s) => const ReceiverMyRequestsScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/receiver/pickups', builder: (c, s) => const ReceiverPickupsScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/receiver/notifications', builder: (c, s) => const ReceiverNotificationsScreen()),
        ]),
        StatefulShellBranch(routes: [
          GoRoute(path: '/receiver/profile', builder: (c, s) => const ReceiverProfileScreen()),
        ]),
      ],
    ),

    // ─── Detail Routes (full-screen, no bottom nav) ───
    GoRoute(
      path: '/donor/create',
      parentNavigatorKey: _rootNavigatorKey,
      builder: (c, s) => const CreateDonationScreen(),
    ),
    GoRoute(
      path: '/donor/donation/:id',
      parentNavigatorKey: _rootNavigatorKey,
      builder: (c, s) => DonorDonationDetailsScreen(id: s.pathParameters['id']!),
    ),
    GoRoute(
      path: '/receiver/donation/:id',
      parentNavigatorKey: _rootNavigatorKey,
      builder: (c, s) => ReceiverDonationDetailsScreen(id: s.pathParameters['id']!),
    ),
    GoRoute(
      path: '/receiver/pickup/:id',
      parentNavigatorKey: _rootNavigatorKey,
      builder: (c, s) => ReceiverPickupTrackingScreen(requestId: s.pathParameters['id']!),
    ),
  ],
);

class ScaffoldWithNavBar extends StatelessWidget {
  final StatefulNavigationShell navigationShell;
  final List<NavigationDestination> destinations;

  const ScaffoldWithNavBar({
    super.key,
    required this.navigationShell,
    required this.destinations,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          border: Border(top: BorderSide(color: AppColors.divider)),
        ),
        child: NavigationBar(
          selectedIndex: navigationShell.currentIndex,
          onDestinationSelected: navigationShell.goBranch,
          destinations: destinations,
        ),
      ),
    );
  }
}

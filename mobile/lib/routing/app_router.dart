
import 'package:go_router/go_router.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/auth/screens/login_screen.dart';
import 'package:mealshare_mobile/auth/screens/register_screen.dart';
import 'package:mealshare_mobile/home/donor_home_screen.dart';
import 'package:mealshare_mobile/home/receiver_home_screen.dart';
import 'package:mealshare_mobile/receiver/receiver_pickups_screen.dart';

class AppRouter {
  static final GoRouter router = GoRouter(
    initialLocation: '/login',
    routes: [
      GoRoute(
        path: '/login',
        name: 'login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/register',
        name: 'register',
        builder: (context, state) => const RegisterScreen(),
      ),
      GoRoute(
        path: '/donor',
        name: 'donor-home',
        builder: (context, state) => const DonorHomeScreen(),
      ),
      GoRoute(
        path: '/receiver',
        name: 'receiver-home',
        builder: (context, state) => const ReceiverHomeScreen(),
      ),
      GoRoute(
        path: '/receiver/pickups',
        name: 'receiver-pickups',
        builder: (context, state) => const ReceiverPickupsScreen(),
      ),

    ],
  );

  /// Navigate to the appropriate home screen based on user role
  static String getHomeRoute(UserRole role) {
    switch (role) {
      case UserRole.donor:
        return '/donor';
      case UserRole.receiver:
        return '/receiver';
      case UserRole.admin:
        return '/login';
    }
  }
}

import 'package:flutter_test/flutter_test.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';

void main() {
  group('AuthService', () {
    test('initial state should be unauthenticated', () {
      final authService = AuthService();
      expect(authService.authState, AuthState.unauthenticated);
      expect(authService.isAuthenticated, false);
      expect(authService.uid, null);
      expect(authService.email, null);
    });

    test('setRole should update role', () {
      final authService = AuthService();
      authService.setRole(UserRole.donor);
      expect(authService.role, UserRole.donor);
    });

    test('signOut should clear state', () async {
      final authService = AuthService();
      authService.setRole(UserRole.donor);
      await authService.signOut();
      expect(authService.role, null);
      expect(authService.authState, AuthState.unauthenticated);
    });
  });
}

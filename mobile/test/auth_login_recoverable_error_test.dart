import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/auth/screens/login_screen.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/shared/api_client.dart';

void main() {
  setUp(() {
    ApiConfig.dataMode = DataMode.mock;
  });

  Widget createTestWidget({
    required AuthService authService,
    required ApiClient apiClient,
  }) {
    final router = GoRouter(
      initialLocation: '/login',
      routes: [
        GoRoute(
          path: '/login',
          builder: (context, state) => const LoginScreen(),
        ),
        GoRoute(
          path: '/register',
          builder: (context, state) => const Scaffold(body: Text('Mock Register Page')),
        ),
        GoRoute(
          path: '/donor/home',
          builder: (context, state) => const Scaffold(body: Text('Mock Donor Home Page')),
        ),
        GoRoute(
          path: '/receiver/home',
          builder: (context, state) => const Scaffold(body: Text('Mock Receiver Home Page')),
        ),
      ],
    );

    return MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthService>.value(value: authService),
        Provider<ApiClient>.value(value: apiClient),
      ],
      child: MaterialApp.router(
        routerConfig: router,
      ),
    );
  }

  group('LoginScreen Recoverable vs Security Error Tests', () {
    testWidgets('404 Profile Not Found preserves session and shows Complete Registration action', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async {
        if (request.url.path.contains('users/me')) {
          return http.Response(
            jsonEncode({'status': 404, 'message': 'User profile not found in database'}),
            404,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response(jsonEncode({}), 200);
      });

      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      // Enter email and password
      await tester.enterText(find.byType(TextFormField).at(0), 'kartik@gmail.com');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pumpAndSettle();

      // 1. Session is PRESERVED (NOT signed out)
      expect(authService.isAuthenticated, isTrue);
      expect(authService.email, 'kartik@gmail.com');

      // 2. Actionable UI is displayed
      expect(find.textContaining('no MealShare profile exists yet in the database'), findsOneWidget);
      expect(find.text('Complete Registration Now'), findsOneWidget);
      expect(find.text('Sign out and use another account'), findsOneWidget);

      // 3. Tapping 'Complete Registration Now' navigates to /register
      await tester.tap(find.text('Complete Registration Now'));
      await tester.pumpAndSettle();
      expect(find.text('Mock Register Page'), findsOneWidget);
    });

    testWidgets('503 Service Unavailable preserves session and displays Retry button', (tester) async {
      final authService = AuthService();
      int callCount = 0;
      final mockClient = MockClient((request) async {
        if (request.url.path.contains('users/me')) {
          callCount++;
          return http.Response(
            jsonEncode({'status': 503, 'message': 'Service temporarily unavailable'}),
            503,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response(jsonEncode({}), 200);
      });

      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      // Enter email and password
      await tester.enterText(find.byType(TextFormField).at(0), 'donor@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pumpAndSettle();

      // 1. Session is PRESERVED
      expect(authService.isAuthenticated, isTrue);

      // 2. Retry UI is displayed
      expect(find.textContaining('MealShare backend is temporarily unavailable'), findsOneWidget);
      expect(find.text('Retry Connection'), findsOneWidget);

      // 3. User can tap Retry Connection to retry
      final initialCallCount = callCount;
      await tester.tap(find.text('Retry Connection'));
      await tester.pumpAndSettle();
      expect(callCount, greaterThan(initialCallCount));
      expect(authService.isAuthenticated, isTrue);
    });

    testWidgets('Network SocketException preserves session and allows retry', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async {
        throw const SocketException('Failed host lookup: mealshare-backend');
      });

      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'receiver@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pump();
      await tester.pump(const Duration(seconds: 5));
      await tester.pumpAndSettle();

      // Session preserved
      expect(authService.isAuthenticated, isTrue);
      expect(find.textContaining('Cannot reach MealShare backend'), findsOneWidget);
      expect(find.text('Retry Connection'), findsOneWidget);
    });

    testWidgets('401 Unauthorized permanently signs out immediately for security', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode({'status': 401, 'message': 'Token expired or revoked'}),
          401,
          headers: {'content-type': 'application/json'},
        );
      });

      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'revoked@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pumpAndSettle();

      // Must be signed out for security
      expect(authService.isAuthenticated, isFalse);
      expect(find.textContaining('Authentication failed or session expired'), findsOneWidget);
      expect(find.text('Retry Connection'), findsNothing);
      expect(find.text('Complete Registration Now'), findsNothing);
    });

    testWidgets('403 Forbidden signs out immediately for security', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode({'status': 403, 'message': 'Account suspended'}),
          403,
          headers: {'content-type': 'application/json'},
        );
      });

      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'suspended@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pumpAndSettle();

      // Must be signed out for security
      expect(authService.isAuthenticated, isFalse);
      expect(find.textContaining('Your MealShare account has been suspended'), findsOneWidget);
      expect(find.text('Retry Connection'), findsNothing);
      expect(find.text('Complete Registration Now'), findsNothing);
    });

    testWidgets('Successful profile retrieval routes user to role home screen', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async {
        if (request.url.path.contains('users/me')) {
          return http.Response(
            jsonEncode({
              'id': 'u1',
              'email': 'donor@test.org',
              'name': 'Donor Org',
              'role': 'DONOR',
              'status': 'ACTIVE',
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response(jsonEncode({}), 200);
      });

      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'donor@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pumpAndSettle();

      expect(authService.isAuthenticated, isTrue);
      expect(authService.role, UserRole.donor);
      expect(find.text('Mock Donor Home Page'), findsOneWidget);
    });

    testWidgets('Backend timeout cleans up loading state and presents actionable retry', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async {
        throw TimeoutException('Connection timed out');
      });
      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'donor@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));

      // Advance clock through bounded retry delays
      await tester.pump();
      await tester.pump(const Duration(seconds: 20));
      await tester.pumpAndSettle();

      // Loading state MUST be cleaned up (no infinite hanging spinner)
      expect(find.byType(CircularProgressIndicator), findsNothing);
      expect(find.text('Sign In'), findsOneWidget);
      expect(authService.isAuthenticated, isTrue); // Session preserved
      expect(find.text('Retry Connection'), findsOneWidget);
      expect(find.textContaining('Connection timed out'), findsOneWidget);
    });

    testWidgets('Firebase sign-in timeout cleans up loading flag and displays error', (tester) async {
      final authService = TimeoutAuthService();
      final mockClient = MockClient((request) async => http.Response('{}', 200));
      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextFormField).at(0), 'donor@test.org');
      await tester.enterText(find.byType(TextFormField).at(1), 'password123');
      await tester.tap(find.widgetWithText(FilledButton, 'Sign In'));
      await tester.pumpAndSettle();

      // Loading MUST be cleaned up in finally block
      expect(find.byType(CircularProgressIndicator), findsNothing);
      expect(find.text('Sign In'), findsOneWidget);
      expect(find.textContaining('Firebase sign-in timed out'), findsOneWidget);
    });

    testWidgets('Password visibility toggle toggles obscured text without modifying input value', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async => http.Response('{}', 200));
      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      // Enter password
      final passwordFieldFinder = find.widgetWithText(TextFormField, 'Password');
      expect(passwordFieldFinder, findsOneWidget);
      await tester.enterText(passwordFieldFinder, 'SecretPass123!');
      await tester.pumpAndSettle();

      // By default, obscureText is true
      EditableText editableText = tester.widget(find.descendant(
        of: passwordFieldFinder,
        matching: find.byType(EditableText),
      ));
      expect(editableText.obscureText, isTrue);

      // Tap eye icon to show password
      final toggleFinder = find.descendant(
        of: passwordFieldFinder,
        matching: find.byType(IconButton),
      );
      expect(toggleFinder, findsOneWidget);
      await tester.tap(toggleFinder);
      await tester.pumpAndSettle();

      // obscureText should now be false, and text must be preserved
      editableText = tester.widget(find.descendant(
        of: passwordFieldFinder,
        matching: find.byType(EditableText),
      ));
      expect(editableText.obscureText, isFalse);
      expect(editableText.controller.text, 'SecretPass123!');

      // Tap again to hide password
      await tester.tap(toggleFinder);
      await tester.pumpAndSettle();

      editableText = tester.widget(find.descendant(
        of: passwordFieldFinder,
        matching: find.byType(EditableText),
      ));
      expect(editableText.obscureText, isTrue);
      expect(editableText.controller.text, 'SecretPass123!');
    });

    testWidgets('LoginScreen has no demo mode chips and displays Create Account link', (tester) async {
      final authService = AuthService();
      final mockClient = MockClient((request) async => http.Response('{}', 200));
      final apiClient = ApiClient(authService, httpClient: mockClient);

      await tester.pumpWidget(createTestWidget(authService: authService, apiClient: apiClient));
      await tester.pumpAndSettle();

      // Demo buttons must NOT exist
      expect(find.text('Donor Demo'), findsNothing);
      expect(find.text('Receiver Demo'), findsNothing);
      expect(find.byType(ActionChip), findsNothing);

      // Separate Create Account link must be present
      expect(find.text("Don't have an account? Create Account"), findsOneWidget);
    });
  });
}

class TimeoutAuthService extends AuthService {
  @override
  Future<void> signIn(String email, String password) async {
    throw TimeoutException(
      'Firebase sign-in timed out after 15 seconds. Please check your internet connection.',
    );
  }
}

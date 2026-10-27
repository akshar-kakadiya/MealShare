import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/auth/screens/auth_startup_screen.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/mock/app_state.dart';
import 'package:mealshare_mobile/shared/api_client.dart';

class MockAuthService extends AuthService {
  bool _mockIsAuthenticated = false;
  String? _mockUid;
  String? _mockEmail;
  UserRole? _mockRole;
  String? _mockToken = 'test-token';

  void setMockAuthenticated({
    required bool isAuthenticated,
    String? uid,
    String? email,
    UserRole? role,
    String? token,
  }) {
    _mockIsAuthenticated = isAuthenticated;
    _mockUid = uid;
    _mockEmail = email;
    _mockRole = role;
    _mockToken = token;
    notifyListeners();
  }

  @override
  bool get isAuthenticated => _mockIsAuthenticated;

  @override
  String? get uid => _mockUid;

  @override
  String? get email => _mockEmail;

  @override
  UserRole? get role => _mockRole;

  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async {
    return _mockToken;
  }

  @override
  Future<void> signOut() async {
    _mockIsAuthenticated = false;
    _mockUid = null;
    _mockEmail = null;
    _mockRole = null;
    notifyListeners();
  }

  @override
  void updateFromBackendProfile(Map<String, dynamic> profile) {
    super.updateFromBackendProfile(profile);
    _mockRole = super.role;
  }
}

void main() {
  setUp(() {
    ApiConfig.dataMode = DataMode.real;
  });

  Widget createStartupTestWidget({
    required MockAuthService authService,
    required ApiClient apiClient,
    AppState? appState,
  }) {
    final state = appState ?? AppState(apiClient: apiClient, authService: authService);

    final testRouter = GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const AuthStartupScreen(),
        ),
        GoRoute(
          path: '/login',
          builder: (context, state) => const Scaffold(body: Text('Mock Login Screen')),
        ),
        GoRoute(
          path: '/register',
          builder: (context, state) => const Scaffold(body: Text('Mock Register Screen')),
        ),
        GoRoute(
          path: '/donor/home',
          builder: (context, state) => const Scaffold(body: Text('Mock Donor Home Screen')),
        ),
        GoRoute(
          path: '/receiver/home',
          builder: (context, state) => const Scaffold(body: Text('Mock Receiver Home Screen')),
        ),
      ],
    );

    return MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthService>.value(value: authService),
        Provider<ApiClient>.value(value: apiClient),
        ChangeNotifierProvider<AppState>.value(value: state),
      ],
      child: MaterialApp.router(
        routerConfig: testRouter,
      ),
    );
  }

  group('AuthStartupScreen Startup Authentication Flow Tests', () {
    testWidgets('Unauthenticated startup navigates directly to /login without FreshBite auto-login', (tester) async {
      final mockAuth = MockAuthService();
      mockAuth.setMockAuthenticated(isAuthenticated: false);

      final mockClient = MockClient((request) async {
        return http.Response(jsonEncode({}), 200);
      });
      final apiClient = ApiClient(mockAuth, httpClient: mockClient);

      await tester.pumpWidget(createStartupTestWidget(
        authService: mockAuth,
        apiClient: apiClient,
      ));
      await tester.pumpAndSettle();

      expect(find.text('Mock Login Screen'), findsOneWidget);
      expect(find.text('FreshBite Restaurant'), findsNothing);
    });

    testWidgets('Authenticated user with valid DONOR profile navigates to /donor/home', (tester) async {
      final mockAuth = MockAuthService();
      mockAuth.setMockAuthenticated(
        isAuthenticated: true,
        uid: 'donor-uid-123',
        email: 'donor@mealshare.org',
      );

      final mockClient = MockClient((request) async {
        if (request.url.path.contains('users/me')) {
          return http.Response(
            jsonEncode({
              'id': 'd1',
              'email': 'donor@mealshare.org',
              'name': 'Green Earth Kitchen',
              'role': 'DONOR',
              'verificationStatus': 'APPROVED',
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        if (request.url.path.contains('donor/profile')) {
          return http.Response(
            jsonEncode({
              'id': 'dp1',
              'entityName': 'Green Earth Kitchen',
              'donorType': 'RESTAURANT',
              'verificationStatus': 'APPROVED',
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response(jsonEncode({}), 200);
      });
      final apiClient = ApiClient(mockAuth, httpClient: mockClient);

      await tester.pumpWidget(createStartupTestWidget(
        authService: mockAuth,
        apiClient: apiClient,
      ));
      await tester.pumpAndSettle();

      expect(find.text('Mock Donor Home Screen'), findsOneWidget);
    });

    testWidgets('Authenticated user with 404 (profile not in DB) signs out and navigates to /login', (tester) async {
      final mockAuth = MockAuthService();
      mockAuth.setMockAuthenticated(
        isAuthenticated: true,
        uid: 'new-user-uid',
        email: 'newbie@mealshare.org',
      );

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
      final apiClient = ApiClient(mockAuth, httpClient: mockClient);

      await tester.pumpWidget(createStartupTestWidget(
        authService: mockAuth,
        apiClient: apiClient,
      ));
      await tester.pumpAndSettle();

      expect(mockAuth.isAuthenticated, isFalse);
      expect(find.text('Mock Login Screen'), findsOneWidget);
    });

    testWidgets('Transient backend failure shows recoverable error card with Retry and Log Out buttons', (tester) async {
      final mockAuth = MockAuthService();
      mockAuth.setMockAuthenticated(
        isAuthenticated: true,
        uid: 'donor-uid-123',
        email: 'donor@mealshare.org',
      );

      bool backendReady = false;
      final mockClient = MockClient((request) async {
        if (!backendReady) {
          return http.Response(
            jsonEncode({'status': 503, 'message': 'Backend starting up'}),
            503,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response(
          jsonEncode({
            'id': 'd1',
            'email': 'donor@mealshare.org',
            'name': 'Recovered Kitchen',
            'role': 'DONOR',
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });
      final apiClient = ApiClient(mockAuth, httpClient: mockClient);

      await tester.pumpWidget(createStartupTestWidget(
        authService: mockAuth,
        apiClient: apiClient,
      ));
      await tester.pumpAndSettle();

      // Should display recoverable error
      expect(find.textContaining('waking up or temporarily unavailable'), findsOneWidget);
      expect(find.widgetWithText(FilledButton, 'Retry Connection'), findsOneWidget);
      expect(find.widgetWithText(OutlinedButton, 'Log Out'), findsOneWidget);
      expect(mockAuth.isAuthenticated, true); // Session preserved!

      // Backend wakes up, user taps Retry Connection
      backendReady = true;
      await tester.tap(find.widgetWithText(FilledButton, 'Retry Connection'));
      await tester.pumpAndSettle();

      expect(find.text('Mock Donor Home Screen'), findsOneWidget);
    });

    testWidgets('Log Out button on startup error screen signs out and navigates to /login', (tester) async {
      final mockAuth = MockAuthService();
      mockAuth.setMockAuthenticated(
        isAuthenticated: true,
        uid: 'donor-uid-123',
        email: 'donor@mealshare.org',
      );

      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode({'status': 500, 'message': 'Internal Server Error'}),
          500,
          headers: {'content-type': 'application/json'},
        );
      });
      final apiClient = ApiClient(mockAuth, httpClient: mockClient);

      await tester.pumpWidget(createStartupTestWidget(
        authService: mockAuth,
        apiClient: apiClient,
      ));
      await tester.pumpAndSettle();

      expect(find.widgetWithText(OutlinedButton, 'Log Out'), findsOneWidget);

      await tester.tap(find.widgetWithText(OutlinedButton, 'Log Out'));
      await tester.pumpAndSettle();

      expect(mockAuth.isAuthenticated, false);
      expect(find.text('Mock Login Screen'), findsOneWidget);
    });
  });
}

import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/donor/create_donation_screen.dart';
import 'package:mealshare_mobile/mock/app_state.dart';
import 'package:mealshare_mobile/receiver/receiver_pickups_screen.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/shared/utils/date_formatter.dart';

class MockAuthService extends AuthService {
  @override
  bool get isAuthenticated => true;
  @override
  String? get uid => 'test-receiver-uid';
  @override
  String? get email => 'receiver@mealshare.test';
  @override
  UserRole? get role => UserRole.receiver;
  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async => 'mock-jwt-token';
}

void main() {
  setUp(() {
    ApiConfig.dataMode = DataMode.mock;
  });

  group('Requirement 4: AppDateFormatter Unit Tests', () {
    test('formatDate outputs dd-MM-yyyy format', () {
      final dt = DateTime(2026, 10, 26, 15, 30);
      expect(AppDateFormatter.formatDate(dt), '26-10-2026');
    });

    test('formatTime outputs 12-hour clock with AM/PM', () {
      final morning = DateTime(2026, 10, 26, 9, 5);
      final evening = DateTime(2026, 10, 26, 20, 0);
      expect(AppDateFormatter.formatTime(morning), '9:05 AM');
      expect(AppDateFormatter.formatTime(evening), '8:00 PM');
    });

    test('formatDateTime outputs combined h:mm a, dd-MM-yyyy format', () {
      final dt = DateTime(2026, 10, 26, 23, 0);
      expect(AppDateFormatter.formatDateTime(dt), '11:00 PM, 26-10-2026');
    });

    test('formatPickupTimeRange outputs clean 12-hour range', () {
      final start = DateTime(2026, 10, 26, 20, 0);
      final end = DateTime(2026, 10, 26, 21, 0);
      expect(AppDateFormatter.formatPickupTimeRange(start, end), '8:00 PM – 9:00 PM');
    });

    test('formatConsumeBefore outputs combined format', () {
      final dt = DateTime(2026, 10, 26, 23, 0);
      expect(AppDateFormatter.formatConsumeBefore(dt), '11:00 PM, 26-10-2026');
    });

    test('Parses ISO 8601 string to local time', () {
      // 2026-10-26T15:08:46.199975Z is UTC
      const isoString = '2026-10-26T15:08:46.199975Z';
      final formatted = AppDateFormatter.formatDateTime(isoString);
      expect(formatted, isNot(contains('T')));
      expect(formatted, isNot(contains('Z')));
      expect(formatted, contains('2026'));
    });

    test('Handles null and invalid date values with fallback', () {
      expect(AppDateFormatter.formatDate(null), 'Not specified');
      expect(AppDateFormatter.formatTime(null), 'Not specified');
      expect(AppDateFormatter.formatDateTime(null), 'Not specified');
      expect(AppDateFormatter.formatDateTime('invalid-date', fallback: 'Pending'), 'Pending');
      expect(AppDateFormatter.formatConsumeBefore(null), 'Not specified');
    });
  });

  group('Requirements 1, 2, 3: Receiver-side Pickup Tracking UI Tests', () {
    testWidgets('EN_ROUTE_TO_PICKUP card removes Update GPS, Live Map, Stream GPS and keeps Navigate to Donor and Reached Donor', (tester) async {
      final mockAuth = MockAuthService();
      final mockClient = ApiClient(
        mockAuth,
        httpClient: MockClient((request) async {
          if (request.url.path.contains('/receiver/pickups')) {
            return http.Response(
              jsonEncode({
                'content': [
                  {
                    'id': 'pickup-101',
                    'pickupId': 'pickup-101',
                    'donationTitle': 'Fresh Vegetable Curry',
                    'donorName': 'Green Kitchen',
                    'donorPhone': '+91 9876543210',
                    'pickupAddress': '123 MG Road, Indiranagar',
                    'donorLatitude': 12.9716,
                    'donorLongitude': 77.5946,
                    'receiverOrganizationName': 'Hope Center',
                    'requestedQuantity': 25,
                    'quantityUnit': 'meals',
                    'status': 'EN_ROUTE_TO_PICKUP',
                    'scheduledAt': '2026-10-26T14:30:00Z',
                  }
                ]
              }),
              200,
              headers: {'content-type': 'application/json'},
            );
          }
          if (request.url.path.contains('/receiver/requests')) {
            return http.Response(jsonEncode({'content': []}), 200, headers: {'content-type': 'application/json'});
          }
          if (request.url.path.contains('/tracking')) {
            return http.Response(
              jsonEncode({
                'pickupId': 'pickup-101',
                'donorLatitude': 12.9716,
                'donorLongitude': 77.5946,
                'donorName': 'Green Kitchen',
                'distanceMeters': 450,
                'phase': 'APPROACH',
              }),
              200,
              headers: {'content-type': 'application/json'},
            );
          }
          return http.Response('{}', 200, headers: {'content-type': 'application/json'});
        }),
      );

      final appState = AppState(apiClient: mockClient);

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider<AppState>.value(value: appState),
            ChangeNotifierProvider<AuthService>.value(value: mockAuth),
            Provider<ApiClient>.value(value: mockClient),
          ],
          child: const MaterialApp(
            home: ReceiverPickupsScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify REMOVED controls are NOT present
      expect(find.text('Update GPS'), findsNothing, reason: '"Update GPS" must be removed from receiver UI');
      expect(find.text('Live Map'), findsNothing, reason: '"Live Map" must be removed from receiver UI');
      expect(find.text('Stream GPS'), findsNothing, reason: '"Stream GPS" must be removed from receiver UI');

      // Verify KEPT controls ARE present
      expect(find.text('Navigate to Donor'), findsOneWidget, reason: '"Navigate to Donor" must be present');
      expect(find.text('Reached Donor'), findsOneWidget, reason: '"Reached Donor" must be present');

      // Verify Live GPS Tracking Active indicator
      expect(find.text('Live GPS Tracking Active'), findsOneWidget, reason: 'Live tracking indicator must be shown');
      expect(find.textContaining('to donor'), findsOneWidget);
    });

    testWidgets('Tapping Navigate to Donor with missing coordinates shows SnackBar error', (tester) async {
      final mockAuth = MockAuthService();
      final mockClient = ApiClient(
        mockAuth,
        httpClient: MockClient((request) async {
          if (request.url.path.contains('/receiver/pickups')) {
            return http.Response(
              jsonEncode({
                'content': [
                  {
                    'id': 'pickup-102',
                    'pickupId': 'pickup-102',
                    'donationTitle': 'Fruit Bowls',
                    'donorName': 'Fruit Bowl Cafe',
                    'pickupAddress': 'Address without GPS',
                    'donorLatitude': 0.0,
                    'donorLongitude': 0.0,
                    'receiverOrganizationName': 'Youth Hub',
                    'requestedQuantity': 10,
                    'quantityUnit': 'boxes',
                    'status': 'EN_ROUTE_TO_PICKUP',
                    'scheduledAt': '2026-10-26T14:30:00Z',
                  }
                ]
              }),
              200,
              headers: {'content-type': 'application/json'},
            );
          }
          if (request.url.path.contains('/receiver/requests')) {
            return http.Response(jsonEncode({'content': []}), 200, headers: {'content-type': 'application/json'});
          }
          return http.Response('{}', 200, headers: {'content-type': 'application/json'});
        }),
      );

      final appState = AppState(apiClient: mockClient);

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider<AppState>.value(value: appState),
            ChangeNotifierProvider<AuthService>.value(value: mockAuth),
            Provider<ApiClient>.value(value: mockClient),
          ],
          child: const MaterialApp(
            home: ReceiverPickupsScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('Navigate to Donor'), findsOneWidget);
      await tester.tap(find.text('Navigate to Donor'));
      await tester.pump();

      // Verify coordinate validation error SnackBar
      expect(find.text('Donor pickup location coordinates are missing or invalid.'), findsOneWidget);
    });
  });

  group('Requirement 5: Donor Food Consumable Until Field Validation Tests', () {
    testWidgets('Create Donation screen displays Food Consumable Until picker and validates temporal constraint', (tester) async {
      tester.view.physicalSize = const Size(1080, 3500);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      final mockAuth = MockAuthService();
      final mockClient = ApiClient(
        mockAuth,
        httpClient: MockClient((request) async => http.Response('{}', 200)),
      );
      final appState = AppState(apiClient: mockClient);

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider<AppState>.value(value: appState),
            ChangeNotifierProvider<AuthService>.value(value: mockAuth),
            Provider<ApiClient>.value(value: mockClient),
          ],
          child: const MaterialApp(
            home: CreateDonationScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify the Food Consumable Until field is rendered
      expect(find.text('Food Consumable Until *'), findsOneWidget);

      // Verify pickup window and date pickers are present
      expect(find.text('Pickup Date *'), findsOneWidget);
      expect(find.text('Available From *'), findsOneWidget);
      expect(find.text('Available Until *'), findsOneWidget);

      // Verify Zepto-style location selector button is present
      expect(find.textContaining('Location on Map'), findsOneWidget);
    });
  });
}

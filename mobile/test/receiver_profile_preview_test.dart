import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/donor/widgets/receiver_profile_bottom_sheet.dart';
import 'package:mealshare_mobile/shared/api_client.dart';

class MockAuthService extends AuthService {
  @override
  bool get isAuthenticated => true;
  @override
  String? get uid => 'test-donor-uid';
  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async => 'mock-jwt-token';
}

void main() {
  group('Phase 2: Receiver Profile Preview Tests', () {
    testWidgets('ReceiverProfileBottomSheet displays authentic organization data', (tester) async {
      final mockData = {
        'receiverId': 'rec-123',
        'orgName': 'Hope Community Shelter',
        'orgType': 'COMMUNITY_SHELTER',
        'verificationStatus': 'VERIFIED',
        'contactPerson': 'Sarah Connor',
        'phone': '+919876543210',
        'email': 'sarah@hopefoundation.org',
        'addressText': '123 Relief Road, Indiranagar',
        'beneficiaryCapacity': 120,
        'ratingAverage': 4.8,
        'description': 'Dedicated to providing hot meals to destitute communities.',
      };

      final mockClient = MockClient((request) async {
        if (request.url.path.contains('/receiver-profile')) {
          return http.Response(jsonEncode(mockData), 200, headers: {'content-type': 'application/json'});
        }
        return http.Response('Not Found', 404);
      });

      final apiClient = ApiClient(MockAuthService(), httpClient: mockClient);

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            Provider<ApiClient>.value(value: apiClient),
          ],
          child: const MaterialApp(
            home: Scaffold(
              body: ReceiverProfileBottomSheet(
                requestId: 'req-456',
                fallbackName: 'Hope Foundation',
              ),
            ),
          ),
        ),
      );

      // Initially shows loading
      expect(find.byType(CircularProgressIndicator), findsOneWidget);

      await tester.pumpAndSettle();

      // Verified badge and organization details
      expect(find.text('Hope Community Shelter'), findsOneWidget);
      expect(find.text('Verified'), findsOneWidget);
      expect(find.text('COMMUNITY SHELTER'), findsOneWidget);
      expect(find.text('4.8 ★'), findsOneWidget);
      expect(find.text('120'), findsOneWidget);
      expect(find.text('Dedicated to providing hot meals to destitute communities.'), findsOneWidget);
      expect(find.textContaining('Sarah Connor'), findsOneWidget);
      expect(find.textContaining('+919876543210'), findsOneWidget);
      expect(find.textContaining('123 Relief Road, Indiranagar'), findsOneWidget);
      expect(find.text('Back to Request'), findsOneWidget);
    });
  });
}

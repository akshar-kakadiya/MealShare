import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/donor/create_donation_screen.dart';
import 'package:mealshare_mobile/location/location_picker.dart';
import 'package:mealshare_mobile/mock/app_state.dart';
import 'package:mealshare_mobile/shared/api_client.dart';

void main() {
  setUp(() {
    ApiConfig.dataMode = DataMode.mock;
  });

  group('Zepto-Style Location Selection Flow Tests', () {
    test('LocationResult.formatFullAddress formats structured components cleanly', () {
      final addr1 = LocationResult.formatFullAddress(
        label: 'Restaurant',
        houseBuilding: 'Unit 402, Green Towers',
        streetArea: 'Indiranagar 100ft Road',
        landmark: 'Opposite Metro Station',
      );
      expect(
        addr1,
        '[Restaurant], Unit 402, Green Towers, Indiranagar 100ft Road, Near Opposite Metro Station',
      );

      final addr2 = LocationResult.formatFullAddress(
        label: 'Other',
        houseBuilding: 'Plot 12',
        streetArea: 'Whitefield Main Road',
      );
      expect(addr2, 'Plot 12, Whitefield Main Road');
    });

    testWidgets('AddressDetailsScreen validates required fields and submits LocationResult', (tester) async {
      LocationResult? returnedResult;

      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => Scaffold(
              body: ElevatedButton(
                onPressed: () async {
                  returnedResult = await Navigator.of(context).push<LocationResult>(
                    MaterialPageRoute(
                      builder: (ctx) => const AddressDetailsScreen(
                        latitude: 12.9716,
                        longitude: 77.5946,
                      ),
                    ),
                  );
                },
                child: const Text('Open Details'),
              ),
            ),
          ),
        ),
      );

      // Tap button to open screen
      await tester.tap(find.text('Open Details'));
      await tester.pumpAndSettle();

      expect(find.text('Enter Address Details'), findsOneWidget);
      expect(find.textContaining('Lat: 12.97160, Lon: 77.59460'), findsOneWidget);

      // Try submitting without entering required fields
      await tester.ensureVisible(find.text('Save & Use This Address'));
      await tester.tap(find.text('Save & Use This Address'));
      await tester.pumpAndSettle();

      expect(find.text('Please enter building / house / floor details'), findsOneWidget);

      // Enter required fields
      await tester.enterText(
        find.widgetWithText(TextFormField, 'House / Building / Flat / Floor *'),
        'Suite 5A, Apex Tech Park',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Street / Area / Sector / Locality *'),
        'Outer Ring Road, Bellandur',
      );
      await tester.enterText(
        find.widgetWithText(TextFormField, 'Nearby Landmark (Optional)'),
        'Next to Central Mall',
      );

      // Submit
      await tester.ensureVisible(find.text('Save & Use This Address'));
      await tester.tap(find.text('Save & Use This Address'));
      await tester.pumpAndSettle();

      expect(returnedResult, isNotNull);
      expect(returnedResult!.latitude, 12.9716);
      expect(returnedResult!.longitude, 77.5946);
      expect(returnedResult!.label, 'Restaurant');
      expect(returnedResult!.houseBuilding, 'Suite 5A, Apex Tech Park');
      expect(returnedResult!.streetArea, 'Outer Ring Road, Bellandur');
      expect(returnedResult!.landmark, 'Next to Central Mall');
      expect(
        returnedResult!.fullAddress,
        '[Restaurant], Suite 5A, Apex Tech Park, Outer Ring Road, Bellandur, Near Next to Central Mall',
      );
    });

    testWidgets('LocationMapPickerScreen renders map, center pin and action buttons', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: LocationMapPickerScreen(
            initialLatitude: 12.9716,
            initialLongitude: 77.5946,
          ),
        ),
      );
      await tester.pump();

      expect(find.text('Select Location on Map'), findsOneWidget);
      expect(find.text('Move map to place pin'), findsOneWidget);
      expect(find.text('Use Current Location'), findsOneWidget);
      expect(find.text('Confirm Location & Enter Details'), findsOneWidget);
      expect(find.textContaining('Lat: 12.97160, Lon: 77.59460'), findsOneWidget);
    });

    testWidgets('CreateDonationScreen renders Zepto-style location card', (tester) async {
      final authService = AuthService();
      final apiClient = ApiClient(authService);
      final appState = AppState(apiClient: apiClient, authService: authService);

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider.value(value: authService),
            Provider.value(value: apiClient),
            ChangeNotifierProvider.value(value: appState),
          ],
          child: const MaterialApp(
            home: CreateDonationScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.scrollUntilVisible(
        find.byIcon(Icons.edit_location_alt),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('Pickup Location & Schedule'), findsOneWidget);
      expect(find.textContaining('Location'), findsWidgets);
      expect(find.byIcon(Icons.edit_location_alt), findsOneWidget);
    });
  });
}

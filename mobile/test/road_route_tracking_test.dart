import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:mealshare_mobile/shared/utils/polyline_decoder.dart';
import 'package:mealshare_mobile/shared/widgets/donation_tracking_map.dart';

void main() {
  group('Phase 1: Road-Based Routing & Polyline Decoder Tests', () {
    test('decodePolyline accurately decodes standard Google encoded polyline', () {
      // Standard Google polyline test vector:
      // Points: (38.5, -120.2), (40.7, -120.95), (43.252, -126.453)
      const encoded = '_p~iF~ps|U_ulLnnqC_mqNvxq`@';
      final points = decodePolyline(encoded);

      expect(points.length, 3);
      expect(points[0].latitude, closeTo(38.5, 0.001));
      expect(points[0].longitude, closeTo(-120.2, 0.001));
      expect(points[1].latitude, closeTo(40.7, 0.001));
      expect(points[1].longitude, closeTo(-120.95, 0.001));
      expect(points[2].latitude, closeTo(43.252, 0.001));
      expect(points[2].longitude, closeTo(-126.453, 0.001));
    });

    test('decodePolyline handles empty string gracefully', () {
      final points = decodePolyline('');
      expect(points, isEmpty);
    });

    testWidgets('DonationTrackingMap renders with road route metrics and points', (tester) async {
      final routePoints = [
        const LatLng(12.9716, 77.5946),
        const LatLng(12.9500, 77.6000),
        const LatLng(12.9352, 77.6245),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SizedBox(
              height: 300,
              child: DonationTrackingMap(
                receiverLatitude: 12.9716,
                receiverLongitude: 77.5946,
                donorLatitude: 12.9352,
                donorLongitude: 77.6245,
                donorLabel: 'FreshBite Donor',
                roadRoutePoints: routePoints,
                roadDistance: '5.4 km',
                roadDuration: '14 mins',
                lastUpdated: DateTime.now(),
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // In test mode, renders headless container
      expect(find.byKey(const Key('google_maps_test_view')), findsOneWidget);
      // Metric banner shows formatted road distance & ETA
      expect(find.text('5.4 km'), findsOneWidget);
      expect(find.text('ETA: 14 mins'), findsOneWidget);
    });
  });
}

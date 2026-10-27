import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/shared/widgets/donation_tracking_map.dart';
import 'package:mealshare_mobile/tracking/pickup_live_tracking_service.dart';

class MockAuthService extends AuthService {
  @override
  bool get isAuthenticated => true;
  @override
  String? get uid => 'test-receiver-uid';
  @override
  Future<String?> getIdToken({bool forceRefresh = false}) async => 'mock-jwt-token';
}

Position createTestPosition({
  required double latitude,
  required double longitude,
  double speed = 15.0,
  double heading = 90.0,
}) {
  return Position(
    latitude: latitude,
    longitude: longitude,
    timestamp: DateTime.now(),
    accuracy: 5.0,
    altitude: 100.0,
    altitudeAccuracy: 1.0,
    heading: heading,
    headingAccuracy: 1.0,
    speed: speed,
    speedAccuracy: 1.0,
  );
}

void main() {
  group('Stage 2: Live Tracking Data Models & RTDB Schema Tests', () {
    test('TrackingLocationUpdate serializes to valid RTDB schema', () {
      final now = DateTime.now();
      final update = TrackingLocationUpdate(
        latitude: 12.9716,
        longitude: 77.5946,
        speed: 12.5,
        heading: 180.0,
        timestamp: now,
      );

      final map = update.toMap();
      expect(map['latitude'], 12.9716);
      expect(map['longitude'], 77.5946);
      expect(map['speed'], 12.5);
      expect(map['heading'], 180.0);
      expect(map['timestamp'], now.millisecondsSinceEpoch);

      // Verify deserialization
      final restored = TrackingLocationUpdate.fromMap(map);
      expect(restored.latitude, 12.9716);
      expect(restored.longitude, 77.5946);
      expect(restored.speed, 12.5);
      expect(restored.heading, 180.0);
    });

    test('TrackingSessionSnapshot parses complete RTDB active_tracking payload', () {
      final sessionMap = {
        'pickupId': 'pickup-uuid-123',
        'donorUid': 'donor-firebase-uid',
        'receiverUid': 'receiver-firebase-uid',
        'status': 'EN_ROUTE_TO_PICKUP',
        'trackingActive': true,
        'donorLocation': {
          'latitude': 12.9716,
          'longitude': 77.5946,
          'address': '123 MG Road, Bengaluru',
          'donorName': 'Central Kitchen',
        },
        'receiverLocation': {
          'latitude': 12.9352,
          'longitude': 77.6245,
          'speed': 20.0,
          'heading': 45.0,
          'timestamp': 1700000000000,
        },
        'lastUpdatedAt': 1700000000000,
      };

      final snapshot = TrackingSessionSnapshot.fromMap('pickup-uuid-123', sessionMap);

      expect(snapshot.pickupId, 'pickup-uuid-123');
      expect(snapshot.donorUid, 'donor-firebase-uid');
      expect(snapshot.receiverUid, 'receiver-firebase-uid');
      expect(snapshot.status, 'EN_ROUTE_TO_PICKUP');
      expect(snapshot.trackingActive, isTrue);

      expect(snapshot.donorLatitude, 12.9716);
      expect(snapshot.donorLongitude, 77.5946);
      expect(snapshot.donorAddress, '123 MG Road, Bengaluru');
      expect(snapshot.donorName, 'Central Kitchen');

      expect(snapshot.receiverLocation, isNotNull);
      expect(snapshot.receiverLocation!.latitude, 12.9352);
      expect(snapshot.receiverLocation!.longitude, 77.6245);
      expect(snapshot.receiverLocation!.speed, 20.0);
    });
  });

  group('Stage 2: Live Tracking Engine & Position Stream Tests', () {
    late InMemoryLiveTrackingGateway gateway;
    late PickupLiveTrackingService trackingService;
    late ApiClient mockApiClient;

    setUp(() {
      gateway = InMemoryLiveTrackingGateway();
      trackingService = PickupLiveTrackingService(gateway: gateway);
      mockApiClient = ApiClient(MockAuthService());
    });

    tearDown(() {
      trackingService.dispose();
    });

    test('Receiver position stream streams updates directly to RTDB gateway', () async {
      final positionController = StreamController<Position>();

      gateway.seedSession('pickup-101', {
        'pickupId': 'pickup-101',
        'donorUid': 'donor-uid',
        'receiverUid': 'receiver-uid',
        'status': 'EN_ROUTE_TO_PICKUP',
        'trackingActive': true,
      });

      final started = await trackingService.startPublishingLocation(
        pickupId: 'pickup-101',
        apiClient: mockApiClient,
        customPositionStream: positionController.stream,
      );

      expect(started, isTrue);
      expect(trackingService.isPublishing, isTrue);
      expect(trackingService.activePublishingPickupId, 'pickup-101');

      // Emit first position
      final pos1 = createTestPosition(latitude: 12.9300, longitude: 77.6200);
      positionController.add(pos1);
      await pumpEventQueue();

      expect(trackingService.lastPublishedPosition?.latitude, 12.9300);

      final session1 = await gateway.getSession('pickup-101');
      expect(session1?.receiverLocation?.latitude, 12.9300);
      expect(session1?.receiverLocation?.longitude, 77.6200);

      // Emit second position (receiver moving)
      final pos2 = createTestPosition(latitude: 12.9350, longitude: 77.6220);
      positionController.add(pos2);
      await pumpEventQueue();

      expect(trackingService.lastPublishedPosition?.latitude, 12.9350);

      final session2 = await gateway.getSession('pickup-101');
      expect(session2?.receiverLocation?.latitude, 12.9350);
      expect(session2?.receiverLocation?.longitude, 77.6220);

      await positionController.close();
    });

    test('Donor watchPickupSession receives real-time location stream from RTDB', () async {
      gateway.seedSession('pickup-202', {
        'pickupId': 'pickup-202',
        'donorUid': 'donor-uid-2',
        'receiverUid': 'receiver-uid-2',
        'status': 'EN_ROUTE_TO_PICKUP',
        'trackingActive': true,
        'donorLocation': {
          'latitude': 12.9716,
          'longitude': 77.5946,
          'donorName': 'Bakery Fresh',
        },
      });

      final snapshots = <TrackingSessionSnapshot>[];
      final sub = trackingService.watchPickupSession('pickup-202').listen((snapshot) {
        if (snapshot != null) {
          snapshots.add(snapshot);
        }
      });

      await pumpEventQueue();
      expect(snapshots.isNotEmpty, isTrue);
      expect(snapshots.last.donorName, 'Bakery Fresh');

      // Simulate receiver position update via gateway
      await gateway.updateReceiverLocation(
        'pickup-202',
        TrackingLocationUpdate(
          latitude: 12.9400,
          longitude: 77.6100,
          speed: 25.0,
          heading: 180.0,
          timestamp: DateTime.now(),
        ),
      );

      await pumpEventQueue();

      expect(snapshots.last.receiverLocation?.latitude, 12.9400);
      expect(snapshots.last.receiverLocation?.longitude, 77.6100);

      await sub.cancel();
    });

    test('stopPublishingLocation cancels position streaming immediately on arrival or cancellation', () async {
      final positionController = StreamController<Position>();

      await trackingService.startPublishingLocation(
        pickupId: 'pickup-303',
        apiClient: mockApiClient,
        customPositionStream: positionController.stream,
      );

      expect(trackingService.isPublishing, isTrue);

      await trackingService.stopPublishingLocation();

      expect(trackingService.isPublishing, isFalse);
      expect(trackingService.activePublishingPickupId, isNull);

      // Emitting more positions after stopping should not update published state
      positionController.add(createTestPosition(latitude: 12.9999, longitude: 77.9999));
      await pumpEventQueue();

      expect(trackingService.lastPublishedPosition, isNull);
      await positionController.close();
    });
  });

  group('Stage 2: DonationTrackingMap (Google Maps UI Widget Tests)', () {
    testWidgets('DonationTrackingMap renders successfully with donor and receiver coordinates', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SizedBox(
              height: 300,
              width: 400,
              child: DonationTrackingMap(
                donorLatitude: 12.9716,
                donorLongitude: 77.5946,
                donorLabel: 'Community Kitchen',
                receiverLatitude: 12.9352,
                receiverLongitude: 77.6245,
                distanceMeters: 4500,
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify the map widget rendered
      expect(find.byType(DonationTrackingMap), findsOneWidget);

      // Verify Distance & ETA metrics are displayed
      expect(find.text('4.5 km'), findsOneWidget);
      expect(find.textContaining('ETA:'), findsOneWidget);
      expect(find.byIcon(Icons.navigation), findsOneWidget);
      expect(find.byIcon(Icons.center_focus_strong), findsOneWidget);
    });

    testWidgets('DonationTrackingMap displays Arrived when distance <= 0', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SizedBox(
              height: 300,
              width: 400,
              child: DonationTrackingMap(
                donorLatitude: 12.9716,
                donorLongitude: 77.5946,
                receiverLatitude: 12.9716,
                receiverLongitude: 77.5946,
                distanceMeters: 0,
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.text('ETA: Arrived'), findsOneWidget);
    });
  });
}

import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_database/firebase_database.dart';
import '../config/api_config.dart';
import '../shared/api_client.dart';

/// Single location update payload compatible with backend and Firebase RTDB security rules
class TrackingLocationUpdate {
  final double latitude;
  final double longitude;
  final double? speed;
  final double? heading;
  final DateTime timestamp;

  const TrackingLocationUpdate({
    required this.latitude,
    required this.longitude,
    this.speed,
    this.heading,
    required this.timestamp,
  });

  Map<String, dynamic> toMap() => {
    'latitude': latitude,
    'longitude': longitude,
    if (speed != null) 'speed': speed,
    if (heading != null) 'heading': heading,
    'timestamp': timestamp.millisecondsSinceEpoch,
  };

  factory TrackingLocationUpdate.fromMap(Map<dynamic, dynamic> map) {
    return TrackingLocationUpdate(
      latitude: (map['latitude'] as num).toDouble(),
      longitude: (map['longitude'] as num).toDouble(),
      speed: (map['speed'] as num?)?.toDouble(),
      heading: (map['heading'] as num?)?.toDouble(),
      timestamp: map['timestamp'] is num
          ? DateTime.fromMillisecondsSinceEpoch((map['timestamp'] as num).toInt())
          : DateTime.now(),
    );
  }
}

/// Snapshot of the live tracking session under `/active_tracking/{pickupId}`
class TrackingSessionSnapshot {
  final String pickupId;
  final String? donorUid;
  final String? receiverUid;
  final String status;
  final bool trackingActive;
  final TrackingLocationUpdate? receiverLocation;
  final double? donorLatitude;
  final double? donorLongitude;
  final String? donorAddress;
  final String? donorName;
  final DateTime? lastUpdatedAt;

  const TrackingSessionSnapshot({
    required this.pickupId,
    this.donorUid,
    this.receiverUid,
    required this.status,
    required this.trackingActive,
    this.receiverLocation,
    this.donorLatitude,
    this.donorLongitude,
    this.donorAddress,
    this.donorName,
    this.lastUpdatedAt,
  });

  factory TrackingSessionSnapshot.fromMap(String pickupId, Map<dynamic, dynamic> map) {
    TrackingLocationUpdate? recLoc;
    if (map['receiverLocation'] is Map) {
      recLoc = TrackingLocationUpdate.fromMap(map['receiverLocation'] as Map);
    }

    double? donorLat;
    double? donorLng;
    String? donorAddr;
    String? donorNm;
    if (map['donorLocation'] is Map) {
      final dl = map['donorLocation'] as Map;
      donorLat = (dl['latitude'] as num?)?.toDouble();
      donorLng = (dl['longitude'] as num?)?.toDouble();
      donorAddr = dl['address']?.toString();
      donorNm = dl['donorName']?.toString();
    }

    return TrackingSessionSnapshot(
      pickupId: pickupId,
      donorUid: map['donorUid']?.toString(),
      receiverUid: map['receiverUid']?.toString(),
      status: (map['status'] ?? 'EN_ROUTE_TO_PICKUP').toString().toUpperCase(),
      trackingActive: map['trackingActive'] == true,
      receiverLocation: recLoc,
      donorLatitude: donorLat,
      donorLongitude: donorLng,
      donorAddress: donorAddr,
      donorName: donorNm,
      lastUpdatedAt: map['lastUpdatedAt'] is num
          ? DateTime.fromMillisecondsSinceEpoch((map['lastUpdatedAt'] as num).toInt())
          : null,
    );
  }
}

/// Abstract gateway for reading and writing live tracking session nodes in RTDB
abstract class LiveTrackingGateway {
  Future<void> updateReceiverLocation(String pickupId, TrackingLocationUpdate location);
  Stream<TrackingSessionSnapshot?> watchSession(String pickupId);
  Future<TrackingSessionSnapshot?> getSession(String pickupId);
}

/// Production implementation backed by Firebase Realtime Database
class FirebaseLiveTrackingGateway implements LiveTrackingGateway {
  FirebaseDatabase? _databaseInstance;

  FirebaseDatabase get _database {
    if (_databaseInstance != null) return _databaseInstance!;
    try {
      _databaseInstance = FirebaseDatabase.instanceFor(
        app: Firebase.app(),
        databaseURL: ApiConfig.firebaseDatabaseUrl,
      );
    } catch (_) {
      _databaseInstance = FirebaseDatabase.instance;
    }
    return _databaseInstance!;
  }

  @override
  Future<void> updateReceiverLocation(String pickupId, TrackingLocationUpdate location) async {
    try {
      final ref = _database.ref('active_tracking/$pickupId/receiverLocation');
      await ref.set(location.toMap());
    } catch (e) {
      debugPrint('[RTDB] Failed to update receiver location: $e');
      rethrow;
    }
  }

  @override
  Stream<TrackingSessionSnapshot?> watchSession(String pickupId) {
    try {
      final ref = _database.ref('active_tracking/$pickupId');
      return ref.onValue.map((event) {
        final val = event.snapshot.value;
        if (val is Map) {
          return TrackingSessionSnapshot.fromMap(pickupId, val);
        }
        return null;
      });
    } catch (e) {
      debugPrint('[RTDB] Failed to watch session: $e');
      return Stream.value(null);
    }
  }

  @override
  Future<TrackingSessionSnapshot?> getSession(String pickupId) async {
    try {
      final ref = _database.ref('active_tracking/$pickupId');
      final snapshot = await ref.get();
      final val = snapshot.value;
      if (val is Map) {
        return TrackingSessionSnapshot.fromMap(pickupId, val);
      }
      return null;
    } catch (e) {
      debugPrint('[RTDB] Failed to get session: $e');
      return null;
    }
  }
}

/// In-memory gateway for offline mock mode and automated unit/widget testing
class InMemoryLiveTrackingGateway implements LiveTrackingGateway {
  final Map<String, Map<String, dynamic>> _storage = {};
  final Map<String, StreamController<TrackingSessionSnapshot?>> _controllers = {};

  void seedSession(String pickupId, Map<String, dynamic> data) {
    _storage[pickupId] = Map<String, dynamic>.from(data);
    _notify(pickupId);
  }

  void _notify(String pickupId) {
    if (_controllers.containsKey(pickupId)) {
      final data = _storage[pickupId];
      if (data != null) {
        _controllers[pickupId]!.add(TrackingSessionSnapshot.fromMap(pickupId, data));
      } else {
        _controllers[pickupId]!.add(null);
      }
    }
  }

  @override
  Future<void> updateReceiverLocation(String pickupId, TrackingLocationUpdate location) async {
    final session = _storage[pickupId] ?? <String, dynamic>{
      'pickupId': pickupId,
      'status': 'EN_ROUTE_TO_PICKUP',
      'trackingActive': true,
    };
    session['receiverLocation'] = location.toMap();
    session['lastUpdatedAt'] = location.timestamp.millisecondsSinceEpoch;
    _storage[pickupId] = session;
    _notify(pickupId);
  }

  @override
  Stream<TrackingSessionSnapshot?> watchSession(String pickupId) {
    final controller = _controllers.putIfAbsent(
      pickupId,
      () => StreamController<TrackingSessionSnapshot?>.broadcast(),
    );
    // Send current state immediately if present
    scheduleMicrotask(() => _notify(pickupId));
    return controller.stream;
  }

  @override
  Future<TrackingSessionSnapshot?> getSession(String pickupId) async {
    final data = _storage[pickupId];
    if (data != null) {
      return TrackingSessionSnapshot.fromMap(pickupId, data);
    }
    return null;
  }
}

/// Service managing the live tracking lifecycle for receiver publishing and donor watching
class PickupLiveTrackingService extends ChangeNotifier {
  final LiveTrackingGateway gateway;

  PickupLiveTrackingService({LiveTrackingGateway? gateway})
      : gateway = gateway ?? (ApiConfig.isMockMode ? InMemoryLiveTrackingGateway() : FirebaseLiveTrackingGateway());

  StreamSubscription<Position>? _positionSubscription;
  String? _activePublishingPickupId;
  Position? _lastPublishedPosition;
  DateTime? _lastPublishedTime;
  String? _trackingError;
  bool _isPublishing = false;

  bool get isPublishing => _isPublishing;
  String? get activePublishingPickupId => _activePublishingPickupId;
  Position? get lastPublishedPosition => _lastPublishedPosition;
  DateTime? get lastPublishedTime => _lastPublishedTime;
  String? get trackingError => _trackingError;

  /// Start streaming receiver location updates using Geolocator position stream and Firebase RTDB.
  /// Also performs periodic throttled sync to Spring Boot backend for PostGIS audit trail.
  Future<bool> startPublishingLocation({
    required String pickupId,
    required ApiClient apiClient,
    Stream<Position>? customPositionStream,
  }) async {
    if (_isPublishing && _activePublishingPickupId == pickupId) {
      return true; // Already publishing for this pickup
    }

    // Stop any existing stream before starting a new one
    await stopPublishingLocation();

    _activePublishingPickupId = pickupId;
    _trackingError = null;

    Stream<Position> stream;
    if (customPositionStream != null) {
      stream = customPositionStream;
    } else {
      // Check location service and permissions
      try {
        final serviceEnabled = await Geolocator.isLocationServiceEnabled();
        if (!serviceEnabled) {
          _trackingError = 'Location services are disabled on device';
          notifyListeners();
          return false;
        }

        LocationPermission permission = await Geolocator.checkPermission();
        if (permission == LocationPermission.denied) {
          permission = await Geolocator.requestPermission();
          if (permission == LocationPermission.denied) {
            _trackingError = 'Location permission denied';
            notifyListeners();
            return false;
          }
        }

        if (permission == LocationPermission.deniedForever) {
          _trackingError = 'Location permissions permanently denied';
          notifyListeners();
          return false;
        }

        stream = Geolocator.getPositionStream(
          locationSettings: const LocationSettings(
            accuracy: LocationAccuracy.high,
            distanceFilter: 5, // Stream position update every 5 meters
          ),
        );
      } catch (e) {
        _trackingError = 'Failed to initialize location stream: $e';
        notifyListeners();
        return false;
      }
    }

    _isPublishing = true;
    notifyListeners();

    DateTime lastBackendSync = DateTime.fromMillisecondsSinceEpoch(0);

    _positionSubscription = stream.listen(
      (Position position) async {
        final update = TrackingLocationUpdate(
          latitude: position.latitude,
          longitude: position.longitude,
          speed: position.speed >= 0 ? position.speed : null,
          heading: position.heading >= 0 ? position.heading : null,
          timestamp: DateTime.now(),
        );

        _lastPublishedPosition = position;
        _lastPublishedTime = update.timestamp;
        _trackingError = null;
        notifyListeners();

        // 1. Instant Realtime Database Update (<100ms latency)
        try {
          await gateway.updateReceiverLocation(pickupId, update);
        } catch (e) {
          debugPrint('[TrackingService] RTDB publish error: $e');
        }

        // 2. Throttled PostGIS/Backend sync (every 15s) for audit history
        final now = DateTime.now();
        if (now.difference(lastBackendSync).inSeconds >= 15) {
          lastBackendSync = now;
          try {
            await apiClient.post(
              ApiConfig.receiverLocationUrl(pickupId),
              body: {
                'latitude': position.latitude,
                'longitude': position.longitude,
                'speed': position.speed,
                'heading': position.heading,
                'phase': 'APPROACH',
              },
            );
          } catch (e) {
            debugPrint('[TrackingService] Periodic backend sync error: $e');
          }
        }
      },
      onError: (error) {
        _trackingError = 'GPS stream error: $error';
        notifyListeners();
      },
      cancelOnError: false,
    );

    return true;
  }

  /// Stop location publishing immediately (e.g. on arrival AT_PICKUP, CANCELLED, or screen exit)
  Future<void> stopPublishingLocation() async {
    if (_positionSubscription != null) {
      await _positionSubscription!.cancel();
      _positionSubscription = null;
    }
    _isPublishing = false;
    _activePublishingPickupId = null;
    notifyListeners();
  }

  /// Watch live tracking session updates for a specific pickup (used by Donor and Receiver tracking views)
  Stream<TrackingSessionSnapshot?> watchPickupSession(String pickupId) {
    return gateway.watchSession(pickupId);
  }

  @override
  void dispose() {
    _positionSubscription?.cancel();
    super.dispose();
  }
}

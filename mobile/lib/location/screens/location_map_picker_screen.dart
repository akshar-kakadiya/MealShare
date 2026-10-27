import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import '../../shared/device_hardware_service.dart';
import '../../theme/app_colors.dart';
import '../models/location_result.dart';
import 'address_details_screen.dart';

class LocationMapPickerScreen extends StatefulWidget {
  final double? initialLatitude;
  final double? initialLongitude;
  final String? initialAddress;

  const LocationMapPickerScreen({
    super.key,
    this.initialLatitude,
    this.initialLongitude,
    this.initialAddress,
  });

  @override
  State<LocationMapPickerScreen> createState() => _LocationMapPickerScreenState();
}

class _LocationMapPickerScreenState extends State<LocationMapPickerScreen> {
  late final MapController _mapController;
  late LatLng _currentCenter;
  bool _isLoadingGps = false;
  bool _isMapReady = false;

  // Default coordinate: Bengaluru Central (12.9716, 77.5946)
  static const LatLng _defaultCenter = LatLng(12.9716, 77.5946);

  @override
  void initState() {
    super.initState();
    _mapController = MapController();
    if (widget.initialLatitude != null && widget.initialLongitude != null) {
      _currentCenter = LatLng(widget.initialLatitude!, widget.initialLongitude!);
    } else {
      _currentCenter = _defaultCenter;
      // Automatically attempt to center on real GPS coordinates if available
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _fetchInitialGps();
      });
    }
  }

  Future<void> _fetchInitialGps() async {
    try {
      final pos = await DeviceHardwareService.getCurrentLocation();
      if (pos != null && mounted) {
        final gpsLatLng = LatLng(pos.latitude, pos.longitude);
        setState(() {
          _currentCenter = gpsLatLng;
        });
        if (_isMapReady) {
          _mapController.move(gpsLatLng, 16.5);
        }
      }
    } catch (_) {}
  }

  Future<void> _onUseCurrentLocation() async {
    setState(() => _isLoadingGps = true);
    try {
      final pos = await DeviceHardwareService.getCurrentLocation();
      if (!mounted) return;
      if (pos != null) {
        final gpsLatLng = LatLng(pos.latitude, pos.longitude);
        setState(() {
          _currentCenter = gpsLatLng;
        });
        _mapController.move(gpsLatLng, 16.5);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Could not retrieve current location. Please verify GPS permissions.'),
            backgroundColor: AppColors.error,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error getting GPS location: $e'),
            backgroundColor: AppColors.error,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoadingGps = false);
    }
  }

  Future<void> _onConfirmPoint() async {
    final result = await Navigator.of(context).push<LocationResult>(
      MaterialPageRoute(
        builder: (ctx) => AddressDetailsScreen(
          latitude: _currentCenter.latitude,
          longitude: _currentCenter.longitude,
          initialStreetArea: widget.initialAddress,
        ),
      ),
    );

    if (result != null && mounted) {
      Navigator.of(context).pop(result);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Select Location on Map'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        elevation: 0,
      ),
      body: Stack(
        children: [
          // ─── 1. Interactive FlutterMap ───
          FlutterMap(
            mapController: _mapController,
            options: MapOptions(
              initialCenter: _currentCenter,
              initialZoom: 16.0,
              minZoom: 4.0,
              maxZoom: 18.5,
              onMapReady: () {
                _isMapReady = true;
              },
              onPositionChanged: (pos, hasGesture) {
                setState(() {
                  _currentCenter = pos.center;
                });
              },
            ),
            children: [
              TileLayer(
                urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'org.mealshare.mobile',
              ),
            ],
          ),

          // ─── 2. Zepto-Style Centered Pin Overlay ───
          Center(
            child: Padding(
              // Shift up so tip of pin aligns with true center
              padding: const EdgeInsets.only(bottom: 42.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Hint badge
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.8),
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.2),
                          blurRadius: 6,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: const Text(
                      'Move map to place pin',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  // Pin Icon
                  const Icon(
                    Icons.location_on,
                    size: 46,
                    color: AppColors.error,
                  ),
                  // Shadow beneath pin
                  Container(
                    width: 10,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.3),
                      borderRadius: BorderRadius.circular(5),
                    ),
                  ),
                ],
              ),
            ),
          ),

          // ─── 3. Floating "Use Current Location" Button ───
          Positioned(
            right: 16,
            bottom: 200,
            child: Material(
              elevation: 4,
              borderRadius: BorderRadius.circular(30),
              color: Colors.white,
              child: InkWell(
                borderRadius: BorderRadius.circular(30),
                onTap: _isLoadingGps ? null : _onUseCurrentLocation,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (_isLoadingGps)
                        const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      else
                        const Icon(
                          Icons.my_location,
                          color: AppColors.primary,
                          size: 18,
                        ),
                      const SizedBox(width: 8),
                      const Text(
                        'Use Current Location',
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.w600,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),

          // ─── 4. Bottom Floating Confirmation Card ───
          Positioned(
            left: 16,
            right: 16,
            bottom: 16,
            child: Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              elevation: 8,
              shadowColor: Colors.black26,
              child: Padding(
                padding: const EdgeInsets.all(18.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: AppColors.primary.withValues(alpha: 0.12),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.pin_drop, color: AppColors.primary, size: 20),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Selected Location',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                  color: AppColors.textPrimary,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Lat: ${_currentCenter.latitude.toStringAsFixed(5)}, Lon: ${_currentCenter.longitude.toStringAsFixed(5)}',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontFamily: 'monospace',
                                  color: Colors.grey.shade700,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    const Text(
                      'Pinpoint your food pickup spot on the map, then enter building & address details.',
                      style: TextStyle(fontSize: 12, color: AppColors.textSecondary, height: 1.3),
                    ),
                    const SizedBox(height: 16),
                    FilledButton.icon(
                      onPressed: _onConfirmPoint,
                      icon: const Icon(Icons.arrow_forward, size: 18),
                      label: const Text(
                        'Confirm Location & Enter Details',
                        style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                      ),
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

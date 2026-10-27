import 'package:flutter/material.dart';
import 'models/location_result.dart';
import 'screens/location_map_picker_screen.dart';

export 'models/location_result.dart';
export 'screens/location_map_picker_screen.dart';
export 'screens/address_details_screen.dart';

class LocationPicker {
  /// Opens the Zepto-style location selection flow:
  /// 1. Interactive map pin selection first (with "Use Current Location" option)
  /// 2. Address details entry (label, house/building, street/area, landmark)
  /// Returns confirmed [LocationResult] containing latitude, longitude, and formatted address.
  static Future<LocationResult?> pickLocation(
    BuildContext context, {
    double? initialLatitude,
    double? initialLongitude,
    String? initialAddress,
  }) async {
    return Navigator.of(context).push<LocationResult>(
      MaterialPageRoute(
        builder: (ctx) => LocationMapPickerScreen(
          initialLatitude: initialLatitude,
          initialLongitude: initialLongitude,
          initialAddress: initialAddress,
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';
import '../models/location_result.dart';

class AddressDetailsScreen extends StatefulWidget {
  final double latitude;
  final double longitude;
  final String? initialLabel;
  final String? initialHouseBuilding;
  final String? initialStreetArea;
  final String? initialLandmark;

  const AddressDetailsScreen({
    super.key,
    required this.latitude,
    required this.longitude,
    this.initialLabel,
    this.initialHouseBuilding,
    this.initialStreetArea,
    this.initialLandmark,
  });

  @override
  State<AddressDetailsScreen> createState() => _AddressDetailsScreenState();
}

class _AddressDetailsScreenState extends State<AddressDetailsScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _houseBuildingCtrl;
  late final TextEditingController _streetAreaCtrl;
  late final TextEditingController _landmarkCtrl;
  late final TextEditingController _customLabelCtrl;

  late String _selectedLabel;
  final List<String> _labelOptions = [
    'Restaurant',
    'Commercial Kitchen',
    'Home',
    'Office',
    'Warehouse',
    'Other',
  ];

  @override
  void initState() {
    super.initState();
    _selectedLabel = widget.initialLabel ?? 'Restaurant';
    if (!_labelOptions.contains(_selectedLabel)) {
      _selectedLabel = 'Other';
    }
    _houseBuildingCtrl = TextEditingController(text: widget.initialHouseBuilding ?? '');
    _streetAreaCtrl = TextEditingController(text: widget.initialStreetArea ?? '');
    _landmarkCtrl = TextEditingController(text: widget.initialLandmark ?? '');
    _customLabelCtrl = TextEditingController(
      text: _selectedLabel == 'Other' ? (widget.initialLabel ?? '') : '',
    );
  }

  @override
  void dispose() {
    _houseBuildingCtrl.dispose();
    _streetAreaCtrl.dispose();
    _landmarkCtrl.dispose();
    _customLabelCtrl.dispose();
    super.dispose();
  }

  void _onSaveAddress() {
    if (!_formKey.currentState!.validate()) return;

    final effectiveLabel = (_selectedLabel == 'Other' && _customLabelCtrl.text.trim().isNotEmpty)
        ? _customLabelCtrl.text.trim()
        : _selectedLabel;

    final fullAddress = LocationResult.formatFullAddress(
      label: effectiveLabel,
      houseBuilding: _houseBuildingCtrl.text.trim(),
      streetArea: _streetAreaCtrl.text.trim(),
      landmark: _landmarkCtrl.text.trim().isNotEmpty ? _landmarkCtrl.text.trim() : null,
    );

    final result = LocationResult(
      latitude: widget.latitude,
      longitude: widget.longitude,
      label: effectiveLabel,
      houseBuilding: _houseBuildingCtrl.text.trim(),
      streetArea: _streetAreaCtrl.text.trim(),
      landmark: _landmarkCtrl.text.trim().isNotEmpty ? _landmarkCtrl.text.trim() : null,
      fullAddress: fullAddress,
    );

    Navigator.of(context).pop(result);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Enter Address Details'),
        elevation: 0,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20.0),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Confirmed Location Banner
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.primary.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.check, color: Colors.white, size: 18),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Location Pinpoint Confirmed',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Lat: ${widget.latitude.toStringAsFixed(5)}, Lon: ${widget.longitude.toStringAsFixed(5)}',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey.shade700,
                                fontFamily: 'monospace',
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),

                // Section 1: Location Tag / Label
                const Text(
                  'Save Location As',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _labelOptions.map((label) {
                    final isSelected = _selectedLabel == label;
                    return ChoiceChip(
                      label: Text(label),
                      selected: isSelected,
                      selectedColor: AppColors.primary.withValues(alpha: 0.15),
                      labelStyle: TextStyle(
                        color: isSelected ? AppColors.primary : AppColors.textSecondary,
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                        fontSize: 13,
                      ),
                      side: BorderSide(
                        color: isSelected ? AppColors.primary : Colors.grey.shade300,
                      ),
                      onSelected: (selected) {
                        if (selected) {
                          setState(() {
                            _selectedLabel = label;
                          });
                        }
                      },
                    );
                  }).toList(),
                ),

                if (_selectedLabel == 'Other') ...[
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _customLabelCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Custom Label Name',
                      hintText: 'e.g. Central Pantry, Community Kitchen',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.tag),
                    ),
                  ),
                ],

                const SizedBox(height: 24),

                // Section 2: Building / House
                const Text(
                  'Address Information',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),

                TextFormField(
                  controller: _houseBuildingCtrl,
                  decoration: const InputDecoration(
                    labelText: 'House / Building / Flat / Floor *',
                    hintText: 'e.g. Unit 4B, Emerald Heights, 2nd Floor',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.apartment),
                  ),
                  textInputAction: TextInputAction.next,
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Please enter building / house / floor details';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // Section 3: Street / Area
                TextFormField(
                  controller: _streetAreaCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Street / Area / Sector / Locality *',
                    hintText: 'e.g. 100ft Road, Indiranagar, 1st Stage',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.signpost_outlined),
                  ),
                  textInputAction: TextInputAction.next,
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Please enter street / area details';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // Section 4: Landmark
                TextFormField(
                  controller: _landmarkCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nearby Landmark (Optional)',
                    hintText: 'e.g. Opposite City Hospital, Near Metro Station',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.flag_outlined),
                  ),
                  textInputAction: TextInputAction.done,
                  onFieldSubmitted: (_) => _onSaveAddress(),
                ),
                const SizedBox(height: 32),

                // Submit button
                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: FilledButton.icon(
                    onPressed: _onSaveAddress,
                    icon: const Icon(Icons.save_outlined),
                    label: const Text(
                      'Save & Use This Address',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

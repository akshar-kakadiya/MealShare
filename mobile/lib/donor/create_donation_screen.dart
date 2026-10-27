import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../location/location_picker.dart';
import '../shared/utils/date_formatter.dart';

class CreateDonationScreen extends StatefulWidget {
  const CreateDonationScreen({super.key});

  @override
  State<CreateDonationScreen> createState() => _CreateDonationScreenState();
}

class _CreateDonationScreenState extends State<CreateDonationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _quantityCtrl = TextEditingController();
  final _allergenCtrl = TextEditingController();
  final _pickupAddressCtrl = TextEditingController();
  final _pickupInstructionsCtrl = TextEditingController();
  double? _selectedLatitude;
  double? _selectedLongitude;

  String _unit = 'meals';
  String _category = 'Meals';
  String _storageCondition = 'Room Temperature';
  String _packagingCondition = 'Sealed';
  bool _hasAllergens = false;
  bool _safetyDeclared = false;
  bool _isSubmitting = false;
  late DateTime _pickupDate;
  late TimeOfDay _pickupFrom;
  late TimeOfDay _pickupUntil;
  late DateTime _consumeBeforeDate;
  late TimeOfDay _consumeBeforeTime;

  final _units = ['meals', 'kg', 'portions', 'boxes'];
  final _categories = ['Meals', 'Packaged Food', 'Bakery', 'Fruits', 'Vegetables', 'Grains', 'Dairy', 'Other'];
  final _storageConditions = ['Room Temperature', 'Refrigerated', 'Frozen'];
  final _packagingConditions = ['Sealed', 'Partially Sealed', 'Open'];

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _pickupDate = DateTime(now.year, now.month, now.day);
    _pickupFrom = TimeOfDay(hour: now.hour, minute: now.minute);
    final twoHoursLater = now.add(const Duration(hours: 2));
    _pickupUntil = TimeOfDay(hour: twoHoursLater.hour, minute: twoHoursLater.minute);
    final fiveHoursLater = now.add(const Duration(hours: 5));
    _consumeBeforeDate = DateTime(fiveHoursLater.year, fiveHoursLater.month, fiveHoursLater.day);
    _consumeBeforeTime = TimeOfDay(hour: fiveHoursLater.hour, minute: fiveHoursLater.minute);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        final state = context.read<AppState>();
        if (state.currentUser.latitude != null && state.currentUser.longitude != null) {
          setState(() {
            _selectedLatitude = state.currentUser.latitude;
            _selectedLongitude = state.currentUser.longitude;
            if (state.currentUser.address.isNotEmpty) {
              _pickupAddressCtrl.text = state.currentUser.address;
            }
          });
        }
      }
    });
  }

  Future<void> _pickLocationOnMap() async {
    final result = await LocationPicker.pickLocation(
      context,
      initialLatitude: _selectedLatitude,
      initialLongitude: _selectedLongitude,
      initialAddress: _pickupAddressCtrl.text.isNotEmpty ? _pickupAddressCtrl.text : null,
    );
    if (result != null && mounted) {
      setState(() {
        _selectedLatitude = result.latitude;
        _selectedLongitude = result.longitude;
        _pickupAddressCtrl.text = result.fullAddress;
      });
    }
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _descCtrl.dispose();
    _quantityCtrl.dispose();
    _allergenCtrl.dispose();
    _pickupAddressCtrl.dispose();
    _pickupInstructionsCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Create Donation'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            // ─── Food Details ───
            _SectionTitle(title: 'Food Details', icon: Icons.restaurant),
            const SizedBox(height: AppSpacing.md),
            TextFormField(
              controller: _titleCtrl,
              decoration: const InputDecoration(
                labelText: 'Food Name *',
                hintText: 'e.g. Vegetable Rice',
                prefixIcon: Icon(Icons.fastfood),
              ),
              validator: (v) => v == null || v.isEmpty ? 'Required' : null,
            ),
            const SizedBox(height: AppSpacing.md),
            DropdownButtonFormField<String>(
              initialValue: _category,
              decoration: const InputDecoration(
                labelText: 'Category',
                prefixIcon: Icon(Icons.category),
              ),
              items: _categories.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
              onChanged: (v) => setState(() => _category = v!),
            ),
            const SizedBox(height: AppSpacing.md),
            TextFormField(
              controller: _descCtrl,
              decoration: const InputDecoration(
                labelText: 'Description',
                hintText: 'Describe the food items...',
                prefixIcon: Icon(Icons.description),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: AppSpacing.md),
            Row(
              children: [
                Expanded(
                  flex: 2,
                  child: TextFormField(
                    controller: _quantityCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Quantity *',
                      hintText: 'e.g. 50',
                      prefixIcon: Icon(Icons.inventory_2),
                    ),
                    keyboardType: TextInputType.number,
                    validator: (v) => v == null || v.isEmpty ? 'Required' : null,
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _unit,
                    decoration: const InputDecoration(labelText: 'Unit'),
                    items: _units.map((u) => DropdownMenuItem(value: u, child: Text(u))).toList(),
                    onChanged: (v) => setState(() => _unit = v!),
                  ),
                ),
              ],
            ),

            const SizedBox(height: AppSpacing.xxl),

            // ─── Food Safety ───
            _SectionTitle(title: 'Food Safety', icon: Icons.health_and_safety),
            const SizedBox(height: AppSpacing.md),
            DropdownButtonFormField<String>(
              initialValue: _storageCondition,
              decoration: const InputDecoration(
                labelText: 'Storage Condition',
                prefixIcon: Icon(Icons.thermostat),
              ),
              items: _storageConditions.map((s) => DropdownMenuItem(value: s, child: Text(s))).toList(),
              onChanged: (v) => setState(() => _storageCondition = v!),
            ),
            const SizedBox(height: AppSpacing.md),
            DropdownButtonFormField<String>(
              initialValue: _packagingCondition,
              decoration: const InputDecoration(
                labelText: 'Packaging Condition',
                prefixIcon: Icon(Icons.inventory),
              ),
              items: _packagingConditions.map((p) => DropdownMenuItem(value: p, child: Text(p))).toList(),
              onChanged: (v) => setState(() => _packagingCondition = v!),
            ),
            const SizedBox(height: AppSpacing.md),
            SwitchListTile(
              title: const Text('Contains Allergens?'),
              subtitle: const Text('Mark if food contains common allergens'),
              value: _hasAllergens,
              onChanged: (v) => setState(() => _hasAllergens = v),
              activeThumbColor: AppColors.primary,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppRadius.md)),
              tileColor: AppColors.surfaceVariant,
            ),
            if (_hasAllergens) ...[
              const SizedBox(height: AppSpacing.md),
              TextFormField(
                controller: _allergenCtrl,
                decoration: const InputDecoration(
                  labelText: 'Allergen Information',
                  hintText: 'e.g. Contains nuts and dairy',
                  prefixIcon: Icon(Icons.warning_amber),
                ),
              ),
            ],

            const SizedBox(height: AppSpacing.xxl),

            // ─── Declaration ───
            Container(
              padding: const EdgeInsets.all(AppSpacing.lg),
              decoration: BoxDecoration(
                color: _safetyDeclared ? AppColors.successSurface : AppColors.warningSurface,
                borderRadius: BorderRadius.circular(AppRadius.card),
                border: Border.all(
                  color: _safetyDeclared ? AppColors.success.withValues(alpha: 0.3) : AppColors.warning.withValues(alpha: 0.3),
                ),
              ),
              child: Material(
                color: Colors.transparent,
                child: CheckboxListTile(
                  value: _safetyDeclared,
                  onChanged: (v) => setState(() => _safetyDeclared = v ?? false),
                  title: Text(
                    'Food Safety Declaration',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                  ),
                  subtitle: const Text(
                    'I declare that this food has been prepared, stored, and handled in accordance with food safety guidelines and is safe for consumption.',
                  ),
                  activeColor: AppColors.success,
                  contentPadding: EdgeInsets.zero,
                  controlAffinity: ListTileControlAffinity.leading,
                ),
              ),
            ),

            const SizedBox(height: AppSpacing.xxl),

            // ─── Pickup ───
            _SectionTitle(title: 'Pickup Location & Schedule', icon: Icons.location_on),
            const SizedBox(height: AppSpacing.md),

            // Zepto-style interactive location selector card
            InkWell(
              onTap: _pickLocationOnMap,
              borderRadius: BorderRadius.circular(12),
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: _selectedLatitude != null ? AppColors.primary : Colors.grey.shade300,
                    width: _selectedLatitude != null ? 1.5 : 1,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.04),
                      blurRadius: 6,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: (_selectedLatitude != null ? AppColors.primary : Colors.grey.shade600).withValues(alpha: 0.12),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            _selectedLatitude != null ? Icons.pin_drop : Icons.map_outlined,
                            color: _selectedLatitude != null ? AppColors.primary : Colors.grey.shade700,
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _selectedLatitude != null ? 'Pickup Location Pinpointed' : 'Choose Location on Map *',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                  color: _selectedLatitude != null ? AppColors.primary : AppColors.textPrimary,
                                ),
                              ),
                              if (_selectedLatitude != null && _selectedLongitude != null) ...[
                                const SizedBox(height: 2),
                                Text(
                                  'GPS: ${_selectedLatitude!.toStringAsFixed(4)}, ${_selectedLongitude!.toStringAsFixed(4)}',
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontFamily: 'monospace',
                                    color: Colors.grey.shade600,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                        OutlinedButton.icon(
                          onPressed: _pickLocationOnMap,
                          icon: const Icon(Icons.edit_location_alt, size: 16),
                          label: Text(_selectedLatitude != null ? 'Change' : 'Select'),
                          style: OutlinedButton.styleFrom(
                            visualDensity: VisualDensity.compact,
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          ),
                        ),
                      ],
                    ),
                    if (_pickupAddressCtrl.text.isNotEmpty) ...[
                      const Divider(height: 18),
                      Text(
                        _pickupAddressCtrl.text,
                        style: const TextStyle(fontSize: 13, color: AppColors.textPrimary, height: 1.3),
                      ),
                    ] else ...[
                      const SizedBox(height: 8),
                      Text(
                        'Tap to pinpoint your location on the interactive map and enter building & area details.',
                        style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            _DatePickerField(
              label: 'Pickup Date *',
              date: _pickupDate,
              onChanged: (d) => setState(() => _pickupDate = d),
            ),
            const SizedBox(height: AppSpacing.md),
            Row(
              children: [
                Expanded(
                  child: _TimePickerField(
                    label: 'Available From *',
                    time: _pickupFrom,
                    onChanged: (t) => setState(() => _pickupFrom = t),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: _TimePickerField(
                    label: 'Available Until *',
                    time: _pickupUntil,
                    onChanged: (t) => setState(() => _pickupUntil = t),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.xxl),

            // ─── Food Consumption Deadline ───
            const _SectionTitle(title: 'Food Consumption Deadline', icon: Icons.timer),
            const SizedBox(height: AppSpacing.xs),
            Text(
              'Specify when this food should no longer be consumed. Must be after the pickup window end time.',
              style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
            ),
            const SizedBox(height: AppSpacing.md),
            _DateTimePickerField(
              label: 'Food Consumable Until *',
              date: _consumeBeforeDate,
              time: _consumeBeforeTime,
              onChanged: (d, t) {
                setState(() {
                  _consumeBeforeDate = d;
                  _consumeBeforeTime = t;
                });
              },
            ),
            const SizedBox(height: AppSpacing.md),
            TextFormField(
              controller: _pickupInstructionsCtrl,
              decoration: const InputDecoration(
                labelText: 'Pickup Instructions',
                hintText: 'Any special instructions for pickup...',
                prefixIcon: Icon(Icons.info_outline),
              ),
              maxLines: 2,
            ),

            const SizedBox(height: AppSpacing.xxxl),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: ElevatedButton(
            onPressed: (_safetyDeclared && !_isSubmitting) ? _submit : null,
            style: ElevatedButton.styleFrom(
              minimumSize: const Size.fromHeight(52),
            ),
            child: _isSubmitting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Text('Submit Donation'),
          ),
        ),
      ),
    );
  }

  Future<void> _submit() async {
    if (_isSubmitting) return;
    if (!_formKey.currentState!.validate()) return;
    if (!_safetyDeclared) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('You must declare food safety compliance before submitting.'),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    final state = context.read<AppState>();
    final pickupFrom = DateTime(
      _pickupDate.year,
      _pickupDate.month,
      _pickupDate.day,
      _pickupFrom.hour,
      _pickupFrom.minute,
    );
    final pickupUntil = DateTime(
      _pickupDate.year,
      _pickupDate.month,
      _pickupDate.day,
      _pickupUntil.hour,
      _pickupUntil.minute,
    );
    final consumeBefore = DateTime(
      _consumeBeforeDate.year,
      _consumeBeforeDate.month,
      _consumeBeforeDate.day,
      _consumeBeforeTime.hour,
      _consumeBeforeTime.minute,
    );

    if (!pickupUntil.isAfter(pickupFrom)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Pickup window end time must be after start time.'),
          backgroundColor: AppColors.error,
        ),
      );
      setState(() => _isSubmitting = false);
      return;
    }

    if (!consumeBefore.isAfter(pickupUntil)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Food Consumable Until must be later than the pickup window end time.'),
          backgroundColor: AppColors.error,
        ),
      );
      setState(() => _isSubmitting = false);
      return;
    }

    if (consumeBefore.isBefore(DateTime.now())) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Food Consumable Until cannot be in the past.'),
          backgroundColor: AppColors.error,
        ),
      );
      setState(() => _isSubmitting = false);
      return;
    }

    final prep = pickupFrom.subtract(const Duration(minutes: 30));

    final lat = _selectedLatitude ?? state.currentUser.latitude;
    final lon = _selectedLongitude ?? state.currentUser.longitude;
    final address = _pickupAddressCtrl.text.trim();

    if (address.isEmpty || lat == null || lon == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please select your pickup location on the interactive map.'),
          backgroundColor: AppColors.error,
        ),
      );
      setState(() => _isSubmitting = false);
      return;
    }

    try {
      await state.createDonationRemote(
        title: _titleCtrl.text.trim(),
        description: _descCtrl.text.trim(),
        quantity: int.tryParse(_quantityCtrl.text) ?? 0,
        unit: _unit,
        category: _category,
        storageCondition: _storageCondition,
        packagingCondition: _packagingCondition,
        hasAllergens: _hasAllergens,
        allergenInfo: _allergenCtrl.text.trim(),
        foodSafetyDeclared: _safetyDeclared,
        pickupAddress: _pickupAddressCtrl.text.isNotEmpty ? _pickupAddressCtrl.text.trim() : state.currentUser.address,
        pickupAvailableFrom: pickupFrom,
        pickupAvailableUntil: pickupUntil,
        consumeBefore: consumeBefore,
        preparationDateTime: prep,
        latitude: lat,
        longitude: lon,
      );

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Donation created successfully!'),
          backgroundColor: AppColors.success,
        ),
      );
      context.pop();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to create donation: $e'),
          backgroundColor: AppColors.error,
        ),
      );
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final IconData icon;

  const _SectionTitle({required this.title, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 20, color: AppColors.primary),
        const SizedBox(width: 8),
        Text(
          title,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
            color: AppColors.primary,
          ),
        ),
      ],
    );
  }
}

class _TimePickerField extends StatelessWidget {
  final String label;
  final TimeOfDay time;
  final ValueChanged<TimeOfDay> onChanged;

  const _TimePickerField({required this.label, required this.time, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () async {
        final picked = await showTimePicker(context: context, initialTime: time);
        if (picked != null) onChanged(picked);
      },
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: const Icon(Icons.schedule),
        ),
        child: Text(time.format(context)),
      ),
    );
  }
}

class _DatePickerField extends StatelessWidget {
  final String label;
  final DateTime date;
  final ValueChanged<DateTime> onChanged;

  const _DatePickerField({required this.label, required this.date, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () async {
        final now = DateTime.now();
        final picked = await showDatePicker(
          context: context,
          initialDate: date.isBefore(now) ? now : date,
          firstDate: now.subtract(const Duration(days: 1)),
          lastDate: now.add(const Duration(days: 90)),
        );
        if (picked != null) onChanged(picked);
      },
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: const Icon(Icons.calendar_today),
        ),
        child: Text(AppDateFormatter.formatDate(date)),
      ),
    );
  }
}

class _DateTimePickerField extends StatelessWidget {
  final String label;
  final DateTime date;
  final TimeOfDay time;
  final void Function(DateTime date, TimeOfDay time) onChanged;

  const _DateTimePickerField({
    required this.label,
    required this.date,
    required this.time,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final combined = DateTime(date.year, date.month, date.day, time.hour, time.minute);
    return InkWell(
      onTap: () async {
        final now = DateTime.now();
        final pickedDate = await showDatePicker(
          context: context,
          initialDate: date.isBefore(now) ? now : date,
          firstDate: now.subtract(const Duration(days: 1)),
          lastDate: now.add(const Duration(days: 90)),
        );
        if (pickedDate == null || !context.mounted) return;

        final pickedTime = await showTimePicker(
          context: context,
          initialTime: time,
        );
        if (pickedTime != null) {
          onChanged(pickedDate, pickedTime);
        }
      },
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          hintText: 'Select date and time',
          prefixIcon: const Icon(Icons.access_time_filled),
        ),
        child: Text(
          AppDateFormatter.formatDateTime(combined),
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../mock/app_state.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

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

  String _unit = 'meals';
  String _category = 'Meals';
  String _storageCondition = 'Room Temperature';
  String _packagingCondition = 'Sealed';
  bool _hasAllergens = false;
  bool _safetyDeclared = false;
  bool _isSubmitting = false;
  TimeOfDay _pickupFrom = const TimeOfDay(hour: 16, minute: 0);
  TimeOfDay _pickupUntil = const TimeOfDay(hour: 20, minute: 0);

  final _units = ['meals', 'kg', 'portions', 'boxes'];
  final _categories = ['Meals', 'Packaged Food', 'Bakery', 'Fruits', 'Vegetables', 'Grains', 'Dairy', 'Other'];
  final _storageConditions = ['Room Temperature', 'Refrigerated', 'Frozen'];
  final _packagingConditions = ['Sealed', 'Partially Sealed', 'Open'];

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

            const SizedBox(height: AppSpacing.xxl),

            // ─── Pickup ───
            _SectionTitle(title: 'Pickup Information', icon: Icons.location_on),
            const SizedBox(height: AppSpacing.md),
            TextFormField(
              controller: _pickupAddressCtrl,
              decoration: const InputDecoration(
                labelText: 'Pickup Address',
                hintText: 'Enter pickup location',
                prefixIcon: Icon(Icons.location_on),
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            Row(
              children: [
                Expanded(
                  child: _TimePickerField(
                    label: 'Available From',
                    time: _pickupFrom,
                    onChanged: (t) => setState(() => _pickupFrom = t),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: _TimePickerField(
                    label: 'Available Until',
                    time: _pickupUntil,
                    onChanged: (t) => setState(() => _pickupUntil = t),
                  ),
                ),
              ],
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
    final now = DateTime.now();
    var pickupFrom = DateTime(now.year, now.month, now.day, _pickupFrom.hour, _pickupFrom.minute);
    if (pickupFrom.isBefore(now)) {
      pickupFrom = now.add(const Duration(minutes: 15));
    }
    var pickupUntil = DateTime(now.year, now.month, now.day, _pickupUntil.hour, _pickupUntil.minute);
    if (pickupUntil.isBefore(pickupFrom) || pickupUntil.isAtSameMomentAs(pickupFrom)) {
      pickupUntil = pickupFrom.add(const Duration(hours: 3));
    }
    final prep = now.isBefore(pickupFrom) ? now : pickupFrom.subtract(const Duration(minutes: 15));
    final consumeBefore = pickupUntil.add(const Duration(hours: 4));

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

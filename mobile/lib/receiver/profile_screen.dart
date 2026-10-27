import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import '../auth/auth_service.dart';
import '../mock/app_state.dart';
import '../location/location_picker.dart';
import '../shared/api_client.dart';
import '../shared/device_hardware_service.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../shared/widgets/stat_card.dart';

class ReceiverProfileScreen extends StatefulWidget {
  const ReceiverProfileScreen({super.key});

  @override
  State<ReceiverProfileScreen> createState() => _ReceiverProfileScreenState();
}

class _ReceiverProfileScreenState extends State<ReceiverProfileScreen> {
  static const Map<String, String> _categories = {
    'NGO': 'NGO',
    'COMMUNITY_SHELTER': 'Community Shelter',
    'COMMUNITY_GROUP': 'Community Group',
    'INDIVIDUAL': 'Individual',
  };

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().fetchReceiverProfileRemote();
    });
  }

  void _showEditProfileDialog() {
    final state = context.read<AppState>();
    final user = state.currentUser;
    final apiClient = context.read<ApiClient>();

    String selectedCategory = _categories.containsKey(user.category.toUpperCase())
        ? user.category.toUpperCase()
        : (_categories.containsKey(user.organizationType.toUpperCase())
            ? user.organizationType.toUpperCase()
            : 'NGO');

    final nameCtrl = TextEditingController(text: user.name);
    final orgNameCtrl = TextEditingController(
      text: user.organizationType.toUpperCase() != 'INDIVIDUAL' ? user.name : '',
    );
    final phoneCtrl = TextEditingController(text: user.phone);
    final regCtrl = TextEditingController(text: user.registrationNumber ?? '');
    final addressCtrl = TextEditingController(text: user.address);
    final capacityCtrl = TextEditingController(
      text: user.beneficiaryCapacity != null && user.beneficiaryCapacity! > 0
          ? user.beneficiaryCapacity.toString()
          : '50',
    );
    final contactPersonCtrl = TextEditingController(text: user.contactPerson ?? '');
    final descCtrl = TextEditingController(text: user.description ?? '');
    final photosCtrl = TextEditingController(text: user.photos ?? '');

    double? currentLat = user.latitude ?? 12.9716;
    double? currentLon = user.longitude ?? 77.5946;

    bool saving = false;
    bool uploadingPhoto = false;
    String? errorMsg;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) {
          final isIndividual = selectedCategory == 'INDIVIDUAL';
          final isNgo = selectedCategory == 'NGO';
          final isShelter = selectedCategory == 'COMMUNITY_SHELTER';
          final isGroup = selectedCategory == 'COMMUNITY_GROUP';

          return Container(
            height: MediaQuery.of(context).size.height * 0.9,
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
            ),
            padding: EdgeInsets.only(
              bottom: MediaQuery.of(context).viewInsets.bottom,
            ),
            child: Column(
              children: [
                // Modal Header
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Complete Receiver Profile',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () => Navigator.pop(ctx),
                      ),
                    ],
                  ),
                ),
                const Divider(height: 1),

                // Form body
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (errorMsg != null)
                          Container(
                            padding: const EdgeInsets.all(12),
                            margin: const EdgeInsets.only(bottom: 16),
                            decoration: BoxDecoration(
                              color: Colors.red.shade50,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(errorMsg!, style: const TextStyle(color: AppColors.error, fontSize: 13)),
                          ),

                        // Receiver Category Selector
                        const Text('Receiver Category *', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                        const SizedBox(height: 6),
                        DropdownButtonFormField<String>(
                          initialValue: selectedCategory,
                          decoration: const InputDecoration(
                            border: OutlineInputBorder(),
                            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                          ),
                          items: _categories.entries
                              .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                              .toList(),
                          onChanged: (val) {
                            if (val != null) {
                              setDialogState(() => selectedCategory = val);
                            }
                          },
                        ),
                        const SizedBox(height: 16),

                        // Full Name (All receivers: contact/representative or personal name)
                        Text(
                          isIndividual ? 'Full Name *' : 'Authorized Representative Name *',
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                        const SizedBox(height: 6),
                        TextField(
                          controller: nameCtrl,
                          decoration: InputDecoration(
                            hintText: isIndividual ? 'Enter your full name' : 'Representative or contact name',
                            border: const OutlineInputBorder(),
                            prefixIcon: const Icon(Icons.person_outline),
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Contact Number
                        const Text('Contact Number *', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                        const SizedBox(height: 6),
                        TextField(
                          controller: phoneCtrl,
                          keyboardType: TextInputType.phone,
                          decoration: const InputDecoration(
                            hintText: 'e.g. +91 98765 43210',
                            border: OutlineInputBorder(),
                            prefixIcon: Icon(Icons.phone_outlined),
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Organization / Entity Name (Required for NGO, Shelter, Group)
                        if (!isIndividual) ...[
                          Text(
                            isNgo
                                ? 'NGO / Trust Name *'
                                : (isShelter ? 'Shelter Name *' : 'Community Group Name *'),
                            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                          ),
                          const SizedBox(height: 6),
                          TextField(
                            controller: orgNameCtrl,
                            decoration: InputDecoration(
                              hintText: isNgo
                                  ? 'e.g. Akshaya Food Relief Foundation'
                                  : (isShelter ? 'e.g. City Hope Night Shelter' : 'e.g. Bengaluru Youth Food Bank'),
                              border: const OutlineInputBorder(),
                              prefixIcon: const Icon(Icons.business_outlined),
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],

                        // Registration Number (Optional for NGO)
                        if (isNgo) ...[
                          const Text('Registration / 12A / 80G Number (Optional)', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                          const SizedBox(height: 6),
                          TextField(
                            controller: regCtrl,
                            decoration: const InputDecoration(
                              hintText: 'e.g. TRUST-1234/2020 or 12AA/80G details',
                              border: OutlineInputBorder(),
                              prefixIcon: Icon(Icons.badge_outlined),
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],

                        // Contact Person (if different from representative)
                        if (!isIndividual) ...[
                          const Text('Secondary Contact Person (Optional)', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                          const SizedBox(height: 6),
                          TextField(
                            controller: contactPersonCtrl,
                            decoration: const InputDecoration(
                              hintText: 'e.g. Coordinator or Volunteer Lead',
                              border: OutlineInputBorder(),
                              prefixIcon: Icon(Icons.contact_mail_outlined),
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],

                        // Beneficiary / Serving Capacity
                        if (!isIndividual) ...[
                          Text(
                            isShelter
                                ? 'Shelter Bed / Meal Serving Capacity (Persons/Day) *'
                                : (isGroup ? 'Group Member / Beneficiary Count' : 'Daily Beneficiary Capacity (Persons Served)'),
                            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                          ),
                          const SizedBox(height: 6),
                          TextField(
                            controller: capacityCtrl,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              hintText: 'e.g. 50',
                              border: OutlineInputBorder(),
                              prefixIcon: Icon(Icons.group_outlined),
                            ),
                          ),
                          const SizedBox(height: 16),
                        ],

                        // Short Description / Mission
                        Text(
                          isIndividual
                              ? 'Household / Family Notes (Optional)'
                              : (isNgo ? 'Mission / Organization Description (Optional)' : 'Description & Distribution Focus (Optional)'),
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                        const SizedBox(height: 6),
                        TextField(
                          controller: descCtrl,
                          maxLines: 2,
                          decoration: InputDecoration(
                            hintText: isIndividual
                                ? 'e.g. Family of 4, need weekly groceries'
                                : (isNgo
                                    ? 'Briefly describe your mission and distribution community'
                                    : 'Briefly describe your shelter or community group reach'),
                            border: const OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Receiving / Operating Address
                        Text(
                          isIndividual ? 'Receiving Address *' : 'Operating Facility / Distribution Address *',
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                        const SizedBox(height: 6),
                        TextField(
                          controller: addressCtrl,
                          maxLines: 2,
                          decoration: const InputDecoration(
                            hintText: 'Door/building number, street, area, city, pincode',
                            border: OutlineInputBorder(),
                            prefixIcon: Icon(Icons.location_on_outlined),
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Map location / GPS Coordinates
                        const Text('GPS Location *', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                        const SizedBox(height: 6),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            border: Border.all(color: Colors.grey.shade300),
                            borderRadius: BorderRadius.circular(8),
                            color: Colors.grey.shade50,
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.my_location, color: AppColors.primary, size: 20),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  currentLat != null && currentLon != null
                                      ? 'Lat: ${currentLat!.toStringAsFixed(4)}, Lon: ${currentLon!.toStringAsFixed(4)}'
                                      : 'GPS location not set',
                                  style: const TextStyle(fontSize: 13),
                                ),
                              ),
                              OutlinedButton.icon(
                                onPressed: () async {
                                  final locResult = await LocationPicker.pickLocation(
                                    ctx,
                                    initialLatitude: currentLat,
                                    initialLongitude: currentLon,
                                    initialAddress: addressCtrl.text,
                                  );
                                  if (locResult != null) {
                                    setDialogState(() {
                                      currentLat = locResult.latitude;
                                      currentLon = locResult.longitude;
                                      addressCtrl.text = locResult.fullAddress;
                                    });
                                  }
                                },
                                icon: const Icon(Icons.map_outlined, size: 16),
                                label: const Text('Choose on Map', style: TextStyle(fontSize: 12)),
                                style: OutlinedButton.styleFrom(
                                  visualDensity: VisualDensity.compact,
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Optional Photos
                        if (!isIndividual) ...[
                          Text(
                            isShelter ? 'Shelter Photos (Optional)' : 'Organization / Facility Photos (Optional)',
                            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                          ),
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              Expanded(
                                child: TextField(
                                  controller: photosCtrl,
                                  decoration: const InputDecoration(
                                    hintText: 'Photo URL or capture below',
                                    border: OutlineInputBorder(),
                                    isDense: true,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              IconButton.filled(
                                icon: uploadingPhoto
                                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                    : const Icon(Icons.camera_alt, size: 20),
                                onPressed: uploadingPhoto
                                    ? null
                                    : () async {
                                        setDialogState(() => uploadingPhoto = true);
                                        try {
                                          final url = await DeviceHardwareService.capturePhotoAndUpload(
                                            context: context,
                                            apiClient: apiClient,
                                            source: ImageSource.gallery,
                                          );
                                          if (url != null) {
                                            setDialogState(() {
                                              photosCtrl.text = photosCtrl.text.isEmpty ? url : '${photosCtrl.text},$url';
                                            });
                                          }
                                        } catch (e) {
                                          setDialogState(() => errorMsg = 'Failed to upload photo: $e');
                                        } finally {
                                          setDialogState(() => uploadingPhoto = false);
                                        }
                                      },
                                tooltip: 'Upload photo from camera or gallery',
                              ),
                            ],
                          ),
                          const SizedBox(height: 24),
                        ] else
                          const SizedBox(height: 16),

                        // Action Buttons
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton(
                                onPressed: saving ? null : () => Navigator.pop(ctx),
                                child: const Text('Complete Later'),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: FilledButton(
                                onPressed: saving
                                    ? null
                                    : () async {
                                        final name = nameCtrl.text.trim();
                                        final phone = phoneCtrl.text.trim();
                                        final address = addressCtrl.text.trim();
                                        final orgName = isIndividual ? name : orgNameCtrl.text.trim();
                                        final cap = int.tryParse(capacityCtrl.text.trim()) ?? 50;

                                        if (name.isEmpty) {
                                          setDialogState(() => errorMsg = 'Name is required');
                                          return;
                                        }
                                        if (phone.isEmpty) {
                                          setDialogState(() => errorMsg = 'Contact Number is required');
                                          return;
                                        }
                                        if (address.isEmpty) {
                                          setDialogState(() => errorMsg = 'Operating / Receiving Address is required');
                                          return;
                                        }
                                        if (!isIndividual && orgName.isEmpty) {
                                          setDialogState(() => errorMsg = 'Organization / Group name is required');
                                          return;
                                        }

                                        setDialogState(() {
                                          saving = true;
                                          errorMsg = null;
                                        });

                                        try {
                                          await state.updateReceiverProfileRemote(
                                            orgName: orgName,
                                            orgType: selectedCategory,
                                            registrationNumber: regCtrl.text.trim().isNotEmpty ? regCtrl.text.trim() : null,
                                            addressText: address,
                                            serviceLatitude: currentLat ?? 12.9716,
                                            serviceLongitude: currentLon ?? 77.5946,
                                            beneficiaryCapacity: isIndividual ? 1 : cap,
                                            contactPerson: contactPersonCtrl.text.trim().isNotEmpty ? contactPersonCtrl.text.trim() : null,
                                            description: descCtrl.text.trim().isNotEmpty ? descCtrl.text.trim() : null,
                                            photos: photosCtrl.text.trim().isNotEmpty ? photosCtrl.text.trim() : null,
                                            fullName: name,
                                            phone: phone,
                                          );

                                          if (ctx.mounted) {
                                            Navigator.pop(ctx);
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text('Profile saved successfully!'),
                                                backgroundColor: AppColors.success,
                                              ),
                                            );
                                          }
                                        } catch (e) {
                                          setDialogState(() {
                                            saving = false;
                                            errorMsg = 'Failed to save profile: $e';
                                          });
                                        }
                                      },
                                child: saving
                                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                    : const Text('Save Profile'),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final user = state.currentUser;

    final verificationStatus = user.verificationStatus.toUpperCase();
    final profileStatus = user.profileStatus.toUpperCase();
    final isPending = verificationStatus == 'PENDING';
    final isActionRequired = verificationStatus == 'ACTION_REQUIRED';
    final isRejected = verificationStatus == 'REJECTED';
    final isApproved = verificationStatus == 'APPROVED';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            tooltip: 'Edit Profile',
            onPressed: _showEditProfileDialog,
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => state.fetchReceiverProfileRemote(),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => state.fetchReceiverProfileRemote(),
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            // ─── Prominent Verification & Status Banner ───────────────
            if (isActionRequired)
              Container(
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.amber.shade50,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.amber.shade300, width: 1.5),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.warning_amber_rounded, color: Colors.amber.shade800, size: 22),
                        const SizedBox(width: 8),
                        Text(
                          'Action Required on Your Profile',
                          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.amber.shade900, fontSize: 15),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      user.actionRequiredNotes?.isNotEmpty == true
                          ? user.actionRequiredNotes!
                          : 'The administrator requested updates to your profile. Please review the details and resubmit.',
                      style: TextStyle(color: Colors.amber.shade900, fontSize: 13),
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: _showEditProfileDialog,
                      icon: const Icon(Icons.edit, size: 16),
                      label: const Text('Update Profile & Resubmit'),
                      style: FilledButton.styleFrom(backgroundColor: Colors.amber.shade800),
                    ),
                  ],
                ),
              )
            else if (isRejected)
              Container(
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.red.shade50,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.red.shade300, width: 1.5),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.cancel_outlined, color: Colors.red.shade800, size: 22),
                        const SizedBox(width: 8),
                        Text(
                          'Verification Rejected',
                          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red.shade900, fontSize: 15),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      user.rejectionReason?.isNotEmpty == true
                          ? 'Reason: ${user.rejectionReason}'
                          : 'Your verification was not approved. You can update your details and resubmit for review.',
                      style: TextStyle(color: Colors.red.shade900, fontSize: 13),
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: _showEditProfileDialog,
                      icon: const Icon(Icons.refresh, size: 16),
                      label: const Text('Update Details & Resubmit'),
                      style: FilledButton.styleFrom(backgroundColor: Colors.red.shade800),
                    ),
                  ],
                ),
              )
            else if (isPending)
              Container(
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: Colors.blue.shade50,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.blue.shade200),
                ),
                child: Row(
                  children: [
                    Icon(Icons.schedule, color: Colors.blue.shade700, size: 22),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Verification Pending',
                            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.blue.shade900, fontSize: 14),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'Your profile is being reviewed. You can explore food donations and use the app normally.',
                            style: TextStyle(color: Colors.blue.shade800, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

            // ─── Profile Incomplete Notice ───────────────────────────
            if (profileStatus == 'INCOMPLETE')
              Container(
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.orange.shade200),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.orange.shade800, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          'Profile Incomplete',
                          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.orange.shade900, fontSize: 14),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Complete your profile details to help donors and administrators verify your organization.',
                      style: TextStyle(color: Colors.orange.shade900, fontSize: 12),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        FilledButton(
                          onPressed: _showEditProfileDialog,
                          style: FilledButton.styleFrom(backgroundColor: Colors.orange.shade800),
                          child: const Text('Complete Profile', style: TextStyle(fontSize: 12)),
                        ),
                        const SizedBox(width: 8),
                        TextButton(
                          onPressed: () => context.go('/receiver/home'),
                          child: const Text('Complete Later', style: TextStyle(fontSize: 12)),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

            // ─── Profile Header Card ─────────────────────────────────
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.xl),
                child: Column(
                  children: [
                    CircleAvatar(
                      radius: 40,
                      backgroundColor: AppColors.infoSurface,
                      child: Text(
                        user.name.isNotEmpty ? user.name[0].toUpperCase() : 'R',
                        style: TextStyle(fontSize: 32, fontWeight: FontWeight.w700, color: AppColors.info),
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Flexible(
                          child: Text(
                            user.name,
                            style: Theme.of(context).textTheme.titleLarge,
                            textAlign: TextAlign.center,
                          ),
                        ),
                        if (isApproved) ...[
                          const SizedBox(width: 6),
                          const Icon(Icons.verified, color: AppColors.info, size: 20),
                        ],
                      ],
                    ),
                    const SizedBox(height: 6),
                    Wrap(
                      alignment: WrapAlignment.center,
                      spacing: 8,
                      runSpacing: 4,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                          decoration: BoxDecoration(
                            color: AppColors.infoSurface,
                            borderRadius: BorderRadius.circular(AppRadius.chip),
                          ),
                          child: Text(
                            _categories[user.category] ?? (_categories[user.organizationType] ?? 'Receiver'),
                            style: TextStyle(color: AppColors.info, fontSize: 12, fontWeight: FontWeight.w600),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                          decoration: BoxDecoration(
                            color: isApproved
                                ? Colors.green.shade50
                                : (isActionRequired ? Colors.amber.shade50 : (isRejected ? Colors.red.shade50 : Colors.blue.shade50)),
                            borderRadius: BorderRadius.circular(AppRadius.chip),
                          ),
                          child: Text(
                            isApproved
                                ? 'Verified'
                                : (isActionRequired ? 'Action Required' : (isRejected ? 'Rejected' : 'Pending Verification')),
                            style: TextStyle(
                              color: isApproved
                                  ? Colors.green.shade700
                                  : (isActionRequired ? Colors.amber.shade800 : (isRejected ? Colors.red.shade700 : Colors.blue.shade700)),
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: Colors.amber.shade50,
                            borderRadius: BorderRadius.circular(AppRadius.chip),
                            border: Border.all(color: Colors.amber.shade200),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.star, color: Colors.amber, size: 14),
                              const SizedBox(width: 3),
                              Text(
                                user.ratingAverage.toStringAsFixed(1),
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.amber),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // ─── Impact Stats ─────────────────────────────────────────
            Row(
              children: [
                Expanded(child: StatCard(label: 'Meals', value: '${state.receiverMealsReceived}', icon: Icons.restaurant)),
                const SizedBox(width: AppSpacing.sm),
                Expanded(child: StatCard(label: 'Food Items', value: '${state.receiverFoodReceived}', icon: Icons.inventory_2, backgroundColor: AppColors.secondarySurface, iconColor: AppColors.secondary)),
                const SizedBox(width: AppSpacing.sm),
                Expanded(child: StatCard(label: 'Pickups', value: '${state.receiverSuccessfulPickups}', icon: Icons.local_shipping, backgroundColor: AppColors.infoSurface, iconColor: AppColors.info)),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),

            // ─── Profile Details Card ────────────────────────────────
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('Receiver Profile Details', style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600)),
                        TextButton.icon(
                          onPressed: _showEditProfileDialog,
                          icon: const Icon(Icons.edit, size: 14),
                          label: const Text('Edit', style: TextStyle(fontSize: 12)),
                        ),
                      ],
                    ),
                    const Divider(height: 12),
                    _ProfileRow(Icons.category, 'Category', _categories[user.category] ?? (_categories[user.organizationType] ?? 'Receiver')),
                    _ProfileRow(Icons.person, 'Full / Contact Name', user.name),
                    _ProfileRow(Icons.email, 'Email', user.email),
                    _ProfileRow(Icons.phone, 'Phone', user.phone.isNotEmpty ? user.phone : 'Not specified'),
                    if (user.contactPerson != null && user.contactPerson!.isNotEmpty)
                      _ProfileRow(Icons.badge, 'Contact Person', user.contactPerson!),
                    if (user.registrationNumber != null && user.registrationNumber!.isNotEmpty)
                      _ProfileRow(Icons.badge_outlined, 'Registration No.', user.registrationNumber!),
                    if (user.beneficiaryCapacity != null && user.beneficiaryCapacity! > 0)
                      _ProfileRow(Icons.group_outlined, 'Beneficiary Capacity', '${user.beneficiaryCapacity} persons/day'),
                    if (user.description != null && user.description!.isNotEmpty)
                      _ProfileRow(Icons.notes, 'Mission / Description', user.description!),
                    _ProfileRow(Icons.location_on, 'Address', user.address.isNotEmpty ? user.address : 'Bengaluru, Karnataka'),
                    if (user.latitude != null && user.longitude != null)
                      _ProfileRow(Icons.gps_fixed, 'GPS Location', '${user.latitude!.toStringAsFixed(4)}, ${user.longitude!.toStringAsFixed(4)}'),
                    _ProfileRow(Icons.check_circle_outline, 'Profile Status', profileStatus == 'COMPLETE' ? 'Completed' : 'Incomplete'),
                    _ProfileRow(Icons.calendar_today, 'Member since', '${_monthName(user.joinedAt.month)} ${user.joinedAt.year}'),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // ─── Settings & Logout ────────────────────────────────────
            Card(
              child: Column(
                children: [
                  _SettingsTile(
                    icon: Icons.edit_note,
                    title: 'Edit Organization Profile',
                    subtitle: 'Update address, category details, or capacity',
                    onTap: _showEditProfileDialog,
                  ),
                  const Divider(height: 1),
                  _SettingsTile(
                    icon: Icons.logout,
                    title: 'Logout',
                    subtitle: 'Sign out of your account',
                    isDestructive: true,
                    onTap: () async {
                      final authService = context.read<AuthService?>();
                      if (authService != null) {
                        await authService.signOut();
                      }
                      if (context.mounted) {
                        context.go('/login');
                      }
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xxxl),
          ],
        ),
      ),
    );
  }

  String _monthName(int month) {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return months[month - 1];
  }
}

class _ProfileRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _ProfileRow(this.icon, this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: AppColors.textHint),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: Theme.of(context).textTheme.labelSmall),
                Text(value, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: AppColors.textPrimary)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final bool isDestructive;

  const _SettingsTile({required this.icon, required this.title, required this.subtitle, required this.onTap, this.isDestructive = false});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: isDestructive ? AppColors.error : AppColors.textSecondary),
      title: Text(title, style: TextStyle(color: isDestructive ? AppColors.error : null, fontWeight: FontWeight.w500)),
      subtitle: Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
      trailing: const Icon(Icons.chevron_right, color: AppColors.textHint),
      onTap: onTap,
    );
  }
}

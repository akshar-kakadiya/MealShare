import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/config/api_config.dart';
import 'package:mealshare_mobile/mock/app_state.dart';
import 'package:mealshare_mobile/theme/app_colors.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  UserRole _selectedRole = UserRole.donor;
  String _selectedCategory = 'HOTEL_RESTAURANT';
  bool _loading = false;
  String? _error;

  static const Map<String, String> _donorCategories = {
    'HOTEL_RESTAURANT': 'Hotel / Restaurant',
    'HOSTEL_MESS': 'Hostel / Mess',
    'EVENT_ORGANIZER': 'Event Organizer',
    'HOUSEHOLD': 'Household',
    'OTHER_BUSINESS': 'Other Business',
  };

  static const Map<String, String> _receiverCategories = {
    'NGO': 'NGO',
    'COMMUNITY_SHELTER': 'Community Shelter',
    'COMMUNITY_GROUP': 'Community Group',
    'INDIVIDUAL': 'Individual',
  };

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _onRoleChanged(UserRole role) {
    setState(() {
      _selectedRole = role;
      if (role == UserRole.donor) {
        _selectedCategory = 'HOTEL_RESTAURANT';
      } else {
        _selectedCategory = 'NGO';
      }
    });
  }

  Future<void> _handleRegister() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final authService = context.read<AuthService>();
      final client = context.read<ApiClient>();

      // 1. Create user account via Firebase Authentication
      await authService.register(
        _emailController.text.trim(),
        _passwordController.text,
        _nameController.text.trim(),
        _selectedRole,
      );

      // 2. Register profile in PostgreSQL backend using verified Firebase token
      try {
        final phone = _phoneController.text.trim();
        await client.post(
          ApiConfig.usersRegisterUrl,
          body: {
            'name': _nameController.text.trim(),
            'email': _emailController.text.trim(),
            if (phone.isNotEmpty) 'phone': phone,
            'role': _selectedRole.backendRole,
            'category': _selectedCategory,
          },
        );
      } on ApiException catch (e) {
        if (!e.isConflict) {
          rethrow;
        }
      }

      // 3. Fetch canonical user profile from Spring Boot backend
      try {
        final me = await client.get(ApiConfig.usersMeUrl);
        if (me is Map<String, dynamic>) {
          authService.updateFromBackendProfile(me);
        }
      } catch (_) {}

      // Refresh app state profile
      if (mounted) {
        final appState = context.read<AppState>();
        if (_selectedRole == UserRole.donor) {
          await appState.fetchDonorProfileRemote();
        } else {
          await appState.fetchReceiverProfileRemote();
        }
      }

      if (!mounted) return;

      // 4. Navigate directly to the category-specific profile-completion screen
      switch (_selectedRole) {
        case UserRole.donor:
          context.go('/donor/profile');
          break;
        case UserRole.receiver:
          context.go('/receiver/profile');
          break;
        default:
          context.go('/donor/profile');
      }
    } on ApiException catch (e) {
      setState(() {
        _error = 'Registration failed: ${e.message}';
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = _selectedRole == UserRole.donor ? _donorCategories : _receiverCategories;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Create Account'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Join MealShare',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Choose your role to get started',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: 20),

                if (_error != null)
                  Container(
                    padding: const EdgeInsets.all(12),
                    margin: const EdgeInsets.only(bottom: 16),
                    decoration: BoxDecoration(
                      color: Colors.red.shade50,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      _error!,
                      style: TextStyle(color: Colors.red.shade700, fontSize: 14),
                    ),
                  ),

                // Role selection cards
                Text(
                  'I want to join as:',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: _RoleCard(
                        title: 'Donor',
                        subtitle: 'I want to share surplus food.',
                        icon: Icons.volunteer_activism,
                        isSelected: _selectedRole == UserRole.donor,
                        onTap: () => _onRoleChanged(UserRole.donor),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _RoleCard(
                        title: 'Receiver',
                        subtitle: 'I need food or want to distribute food.',
                        icon: Icons.people_alt,
                        isSelected: _selectedRole == UserRole.receiver,
                        onTap: () => _onRoleChanged(UserRole.receiver),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // Category selection dropdown
                DropdownButtonFormField<String>(
                  key: ValueKey(_selectedRole),
                  initialValue: categories.containsKey(_selectedCategory) ? _selectedCategory : categories.keys.first,
                  decoration: InputDecoration(
                    labelText: _selectedRole == UserRole.donor ? 'Donor Category' : 'Receiver Category',
                    border: const OutlineInputBorder(),
                    prefixIcon: Icon(_selectedRole == UserRole.donor ? Icons.business : Icons.group),
                  ),
                  items: categories.entries.map((entry) {
                    return DropdownMenuItem(
                      value: entry.key,
                      child: Text(entry.value),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) {
                      setState(() => _selectedCategory = val);
                    }
                  },
                ),
                const SizedBox(height: 20),

                // Essential signup fields
                Text(
                  'Account Details',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                ),
                const SizedBox(height: 12),

                // Name field
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(
                    labelText: 'Full Name',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.person_outlined),
                  ),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) return 'Name is required';
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // Email field
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Email Address',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.email_outlined),
                  ),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) return 'Email is required';
                    if (!value.contains('@')) return 'Enter a valid email';
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // Phone field (optional)
                TextFormField(
                  controller: _phoneController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Contact Number (Optional)',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.phone_outlined),
                  ),
                ),
                const SizedBox(height: 16),

                // Password field
                TextFormField(
                  controller: _passwordController,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: 'Password',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.lock_outlined),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) return 'Password is required';
                    if (value.length < 6) return 'Password must be at least 6 characters';
                    return null;
                  },
                ),
                const SizedBox(height: 24),

                // Register button
                FilledButton(
                  onPressed: _loading ? null : _handleRegister,
                  style: FilledButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                  child: _loading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Text('Create Account & Continue', style: TextStyle(fontSize: 16)),
                ),
                const SizedBox(height: 16),

                TextButton(
                  onPressed: () => context.pop(),
                  child: const Text('Already have an account? Sign In'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _RoleCard extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onTap;

  const _RoleCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected ? AppColors.primary : Colors.grey.shade300,
            width: isSelected ? 2 : 1,
          ),
          color: isSelected ? AppColors.primarySurface.withValues(alpha: 0.5) : Colors.white,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: isSelected ? AppColors.primary : Colors.grey.shade600, size: 20),
                const SizedBox(width: 6),
                Text(
                  title,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: isSelected ? AppColors.primary : Colors.black87,
                  ),
                ),
                const Spacer(),
                if (isSelected)
                  const Icon(Icons.check_circle, color: AppColors.primary, size: 18),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              subtitle,
              style: theme.textTheme.bodySmall?.copyWith(
                color: Colors.grey.shade700,
                fontSize: 11,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

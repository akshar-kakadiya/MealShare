import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:mealshare_mobile/auth/auth_service.dart';
import 'package:mealshare_mobile/shared/api_client.dart';
import 'package:mealshare_mobile/config/api_config.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final authService = context.read<AuthService>();
      await authService.signIn(
        _emailController.text.trim(),
        _passwordController.text,
      );

      // Fetch profile from Spring Boot backend
      if (mounted) {
        final client = context.read<ApiClient>();
        try {
          final me = await client.get(ApiConfig.usersMeUrl);
          if (me is Map<String, dynamic>) {
            authService.updateFromBackendProfile(me);
          }
        } on ApiException catch (e) {
          if (ApiConfig.isRealMode) {
            await authService.signOut();
            setState(() {
              _error = e.statusCode == 404
                  ? 'Account is not registered in the MealShare database. Please click Register below.'
                  : 'Backend authentication error (${e.statusCode}): ${e.message}';
            });
            return;
          }
        } catch (e) {
          if (ApiConfig.isRealMode) {
            await authService.signOut();
            setState(() {
              _error = 'Backend unavailable. Could not reach ${ApiConfig.baseUrl}. Please verify the server is running.';
            });
            return;
          }
        }
      }

      if (!mounted) return;

      if (authService.role == UserRole.admin) {
        showDialog(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Admin Portal Notice'),
            content: const Text(
              'Administrator access is exclusively supported on the MealShare React Web portal.\n\nPlease log in using the web console.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('Understood'),
              ),
            ],
          ),
        );
        return;
      }

      switch (authService.role) {
        case UserRole.donor:
          context.go('/donor/home');
          break;
        case UserRole.receiver:
          context.go('/receiver/home');
          break;
        default:
          context.go('/donor/home');
      }
    } catch (e) {
      setState(() {
        _error = e.toString();
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
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Logo
                  const Text(
                    '🍽️',
                    style: TextStyle(fontSize: 64),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'MealShare',
                    style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: Colors.green.shade700,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Surplus Food Redistribution',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.grey,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 40),

                  // Error message
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

                  // Email field
                  TextFormField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(
                      labelText: 'Email',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.email_outlined),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) return 'Email is required';
                      if (!value.contains('@')) return 'Enter a valid email';
                      return null;
                    },
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
                  const SizedBox(height: 16),

                  // Quick Demo Accounts
                  Wrap(
                    alignment: WrapAlignment.center,
                    spacing: 8,
                    children: [
                      ActionChip(
                        avatar: const Icon(Icons.restaurant, size: 16),
                        label: const Text('Donor Demo'),
                        onPressed: () {
                          _emailController.text = 'donor@mealshare.org';
                          _passwordController.text = 'DonorPassword123!';
                        },
                      ),
                      ActionChip(
                        avatar: const Icon(Icons.business, size: 16),
                        label: const Text('Receiver Demo'),
                        onPressed: () {
                          _emailController.text = 'receiver@mealshare.org';
                          _passwordController.text = 'ReceiverPassword123!';
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Login button
                  FilledButton(
                    onPressed: _loading ? null : _handleLogin,
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                    child: _loading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : const Text('Sign In', style: TextStyle(fontSize: 16)),
                  ),
                  const SizedBox(height: 16),

                  // Register link
                  TextButton(
                    onPressed: () => context.push('/register'),
                    child: const Text("Don't have an account? Register"),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

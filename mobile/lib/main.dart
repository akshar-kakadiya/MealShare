import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'app.dart';
import 'auth/auth_service.dart';
import 'shared/api_client.dart';
import 'mock/app_state.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final authService = AuthService();
  final apiClient = ApiClient(authService);
  final appState = AppState(apiClient: apiClient, authService: authService);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: authService),
        Provider.value(value: apiClient),
        ChangeNotifierProvider.value(value: appState),
      ],
      child: const MealShareApp(),
    ),
  );
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';
import 'app.dart';
import 'auth/auth_service.dart';
import 'shared/api_client.dart';
import 'mock/app_state.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
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

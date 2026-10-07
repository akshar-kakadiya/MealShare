# MealShare Mobile — Firebase Setup Guide

## Prerequisites
- A Firebase project (create at https://console.firebase.google.com)
- Firebase Authentication enabled with Email/Password provider

## Android Setup

1. Go to Firebase Console → Project Settings → General
2. Click "Add app" → Android
3. Enter package name: `com.mealshare.mealshare_mobile`
4. Download `google-services.json`
5. Place it at: `mobile/android/app/google-services.json`
6. The `android/build.gradle` and `android/app/build.gradle` files should already have the Google Services plugin configured by FlutterFire

## iOS Setup (if applicable)

1. Go to Firebase Console → Project Settings → General
2. Click "Add app" → iOS
3. Enter bundle ID: `com.mealshare.mealshareApp`
4. Download `GoogleService-Info.plist`
5. Place it at: `mobile/ios/Runner/GoogleService-Info.plist`

## FlutterFire CLI (Recommended)

Alternatively, use FlutterFire CLI for automatic configuration:
```bash
dart pub global activate flutterfire_cli
flutterfire configure --project=your-firebase-project-id
```

This generates `lib/firebase_options.dart` automatically.

## After Configuration

1. Uncomment Firebase initialization in `lib/main.dart`
2. Uncomment Firebase Auth calls in `lib/auth/auth_service.dart`
3. Run `flutter pub get`
4. Run `flutter run`

## Important
- Do NOT commit `google-services.json` or `GoogleService-Info.plist` to version control
- These files contain project-specific credentials
- Add them to `.gitignore` (already configured)

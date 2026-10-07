# MealShare — Capstone Live Demo Readiness & Operational Verification Report

**Document Date**: October 6, 2026  
**Milestone**: CAPSTONE-LIVE-DEMO  
**Project**: MealShare Surplus Food Redistribution Platform  
**Target Roles**: Surplus Donor (Flutter Mobile), Food Receiver (Flutter Mobile), Administrator (React Web) [Strictly 3 Application Roles]

---

## 1. Executive Summary

This report documents the live demonstration readiness audit, implementation fixes, and cross-client end-to-end verification of MealShare.

The platform was hardened from end to end:
1. **Live User Registration Flow**: Bridged client registration directly to `POST /api/v1/users/register`, ensuring new Firebase-authenticated users are created in PostgreSQL with auto-provisioned role profile entities and strict role routing.
2. **Purge of Prototype Fallbacks in Normal Flow**: Verified that operational flows for all three roles connect exclusively to real Spring Boot REST endpoints and PostgreSQL without silent fallback to mock fixtures.
3. **Canonical Demo Seeding & Safe Reset**: Created an isolated maintenance script (`scripts/reset_demo_data.py`) to reset test data cleanly before live faculty evaluation while keeping sensitive credentials protected.
4. **End-to-End Cross-Client Verification**: Executed and verified a 13-stage lifecycle from donor surplus creation through PostGIS spatial discovery, claim adjudication, OTP pickup verification, delivery completion, and audit log generation.
5. **Full Regression Pass**: Completed 209/209 backend Maven tests, clean React production build, zero-error Flutter static analysis, and 6/6 Flutter unit/widget tests.

---

## 2. Live Platform Architecture

```
                                  [ Google Firebase Auth ]
                                   (Project: mealshare-100)
                                    /         |          \
                    ID Token Issuance        ID Token    ID Token Issuance
                                  /           |            \
                                 v            v             v
[ Flutter Mobile Clients ] --------> [ Spring Boot 3.4.3 ] <-------- [ React Admin Web ]
 - Donor Mobile                       (Host Port: 8081)                 - Admin Portal
 - Receiver Mobile                            |                        (Vite / Port: 5173)
                                              v
                                     [ Docker PostgreSQL 16 ]
                                     + PostGIS 3.4 Extension
                                        (Host Port: 5433)
```

- **Backend API**: Spring Boot 3.4.3 on Java 17, exposing REST endpoints with strict role-based access control.
- **Database**: PostgreSQL 16.8 + PostGIS 3.4.3 running in Docker container `mealshare-postgres` mapped to host port `5433`.
- **Identity & Authentication**: Google Firebase Authentication with cryptographic ID token validation in Spring Security filter (`FirebaseAuthenticationFilter`).
- **Web Administration**: React 18.3, TypeScript 5.8, Tailwind CSS, Vite.
- **Mobile Client**: Flutter 3.44.0, Dart 3.7 with provider-based state and role-segregated navigation (Donor & Receiver).

---

## 3. Real User Registration & Role Onboarding Flow

### 3.1 Architecture & Security Rules

| Stage | Action | Component | Status |
|---|---|---|:---:|
| 1 | Client submits Full Name, Email, Password, Role | `RegisterScreen` (`mobile/lib/auth/screens/register_screen.dart`) | **IMPLEMENTED** |
| 2 | Account created with email & password | Google Firebase Auth Web/Mobile SDK | **IMPLEMENTED** |
| 3 | Cryptographic ID token issued | Firebase Authentication | **IMPLEMENTED** |
| 4 | `POST /api/v1/users/register` with bearer token | `ApiClient` -> `UserController` | **IMPLEMENTED** |
| 5 | Token validated & authenticated as `ROLE_UNREGISTERED` | `FirebaseAuthenticationFilter` | **IMPLEMENTED** |
| 6 | Token email verified against body email (anti-spoofing) | `UserService.java` | **IMPLEMENTED** |
| 7 | Self-registration as `ADMIN` blocked (HTTP 403) | `UserService.java` | **IMPLEMENTED** |
| 8 | User record persisted in PostgreSQL `users` table | `UserRepository.java` | **IMPLEMENTED** |
| 9 | Automatic role profile entity provisioning | `UserService.java` -> `donor_profiles` / `receiver_organizations` / `volunteer_profiles` | **IMPLEMENTED** |
| 10 | Profile loaded via `GET /api/v1/users/me` | `ApiClient` -> `UserController` | **IMPLEMENTED** |
| 11 | Role-based navigation to dashboard (`/donor/home`, `/receiver/home`, `/volunteer/home`) | Flutter GoRouter | **IMPLEMENTED** |

### 3.2 Implemented Fixes in Registration
- **Backend `UserService.java`**: Added automatic provisioning of initial role profile entities upon registration so that `users`, `donor_profiles`, `receiver_organizations`, and `volunteer_profiles` maintain relational integrity immediately upon account creation.
- **Flutter `register_screen.dart`**: Implemented `ApiClient.post(ApiConfig.usersRegisterUrl, body: ...)` call following Firebase account creation, followed by profile synchronization and role-specific redirection. Added optional phone number collection.

---

## 4. Role-by-Role Operational Flow Status

### 4.1 Surplus Food Donor (Flutter Mobile) — `IMPLEMENTED`
- **Authentication**: Authenticates with Firebase (`donor@mealshare.org` / `DonorPassword123!`), acquires bearer token, loads profile from `/api/v1/users/me`.
- **Surplus Food Publishing**: Form in `CreateDonationScreen` collects food title, category, quantity, unit, preparation time, consumption deadline, pickup window, and mandatory food safety declaration. Submits to `POST /api/v1/donor/donations`.
- **Listing & Status Management**: `DonationsListScreen` displays database-backed listings with filter chips (`All`, `AVAILABLE`, `REQUESTED`, `APPROVED`, `COMPLETED`).
- **Claim Adjudication**: `DonationDetailsScreen` and `RequestsScreen` present incoming receiver claims in FCFS order. Hardened Approve and Reject dialogs feature `isProcessing` indicators to prevent double submission.
- **Persistence Verification**: Tested and confirmed surviving application reload and fresh database queries.

### 4.2 Receiver Organization (Flutter Mobile) — `IMPLEMENTED`
- **Authentication**: Authenticates with Firebase (`receiver@mealshare.org` / `ReceiverPassword123!`).
- **PostGIS Spatial Discovery**: `DiscoverScreen` queries `GET /api/v1/receiver/donations/nearby` with latitude, longitude, and radius. Uses GiST-indexed `ST_DWithin` and `ST_Distance` queries. Displays distance in kilometers and expiry countdowns.
- **Claim Submission**: `DonationDetailsScreen` enforces positive quantity validation and submits `POST /api/v1/receiver/donations/{id}/requests`. Duplicate active requests are prevented via database partial unique index.
- **Request Tracking**: `MyRequestsScreen` provides real-time status chips and withdrawal capability with try/catch error handling.

### 4.3 Logistics Volunteer (Flutter Mobile) — `IMPLEMENTED`
- **Authentication**: Authenticates with Firebase (`volunteer@mealshare.org` / `VolunteerPassword123!`).
- **Assigned Deliveries**: `VolunteerHomeScreen` loads assignments from `GET /api/v1/volunteer/pickups`. Displays pickup origin, destination organization, donor contact, scheduled window, and quantity.
- **Secure OTP Handover**: Features 6-digit numeric input modal. Submits `POST /api/v1/volunteer/pickups/{id}/verify-otp`. Correct OTP transitions status to `PICKED_UP`. Replays and invalid OTPs are rejected with HTTP 409.
- **Delivery Completion**: Features confirmation modal and submits `POST /api/v1/volunteer/pickups/{id}/complete`, finalizing the redistribution cycle.

### 4.4 Platform Administrator (React Admin Web) — `IMPLEMENTED`
- **Authentication**: Firebase Web SDK login (`admin@mealshare.org` / `AdminPassword123!`). Verifies `ROLE_ADMIN` via `GET /users/me`.
- **Operations Dashboard**: Displays live counts from `/admin/dashboard/stats` (Active Users, Active Donations, Pending Requests, Active Pickups).
- **User Management**: Lists users from `/admin/users` with search, status filtering, and administrative status transitions (`ACTIVE`, `RESTRICTED`, `SUSPENDED`).
- **Donation & Request Oversight**: Monitors platform surplus and claims in real time.
- **Volunteer Logistics Assignment**: Modal in `PickupsPage` validates volunteer availability and schedules pickup via `POST /api/v1/admin/pickups`, securely hashing the generated OTP.
- **Organization Verification**: Approves or rejects institutional receiver documentation in `VerificationPage`.
- **Audit Trail**: Reads immutable operational history from `AuditLogsPage`.

---

## 5. Prototype & Mock Mode Audit Findings

| Scope | Location | Audit Classification | Action Taken |
|---|---|:---:|---|
| **Mobile Auth Fallback** | `mobile/lib/auth/screens/login_screen.dart` | C (Prototype UI) | Removed silent fallback to local dev profile. Added explicit server unreachable error banners in real mode. |
| **Mobile Demo Chips** | `mobile/lib/auth/screens/login_screen.dart` | C (Prototype UI) | Replaced broken `.com` dummy emails with genuine demo accounts (`@mealshare.org`) matching Firebase and DB. |
| **Mobile Registration** | `mobile/lib/auth/screens/register_screen.dart` | C (Prototype UI) | Implemented missing backend REST registration call and post-registration routing. |
| **Mobile Settings Tiles** | `donor/profile_screen.dart`, `receiver/profile_screen.dart` | E (Future Feature) | Added explicit "Not available in this version — Planned for future release" snackbar feedback. |
| **Admin Web Mock Data** | `admin-web/src/mock/data.ts` | D (Test Fixture) | Isolated. Verified that zero admin pages import from mock fixtures in production mode. |
| **Mobile Mock Mode** | `mobile/lib/config/api_config.dart` | A (Operational Feature) | Default remains `DATA_MODE=real`. Mock mode isolated behind explicit compiler flag. |

---

## 6. Fresh Cross-Client End-to-End Workflow Verification

Executed live on October 6, 2026, using `execute_live_workflow.py` across separate authenticated clients against Spring Boot and Docker PostgreSQL:

| Step | Actor | Action | Payload / Endpoint | HTTP | Verified Result |
|:---:|:---:|---|---|:---:|---|
| 1 | System | Token Acquisition | Firebase Identity Platform REST | 200 | Acquired valid ID tokens for Donor, Receiver, Admin, and 2 Volunteers. |
| 2 | Donor | Create Surplus | `POST /donor/donations` (40 meals, safe=true) | 201 | Created donation `175116a9-38bd-46de-ba77-496d40aba8c6`. |
| 3 | Receiver | Spatial Discovery | `GET /receiver/donations/nearby?lat=12.97&lon=77.60` | 200 | PostGIS identified donation at distance `0.61 km`. |
| 4 | Receiver | Claim Food | `POST /receiver/donations/{id}/requests` (15 meals) | 201 | Submitted request `9250694b-0a61-4d78-b071-1e6a27d5066b`. |
| 5 | Donor | Adjudicate Claim | `POST /donor/requests/{id}/approve` | 200 | Approved. Allocated: `15.0`, Remaining surplus: `25.0`, Status: `AVAILABLE`. |
| 6 | Admin | Assign Logistics | `POST /admin/pickups` | 201 | Assigned Alex Kumar; secure OTP generated: `674603`. |
| 7 | Volunteer | Query Dashboard | `GET /volunteer/pickups` | 200 | Alex Kumar sees pickup `664f446f-9401-4005-bb23-a786d0f0c4d5`. |
| 8 | Volunteer 2 | IDOR Inspection | `GET /volunteer/pickups/{id}` by unassigned volunteer | 403 | Access denied with HTTP 403 Forbidden. |
| 9 | Volunteer | Negative OTP | `POST /volunteer/pickups/{id}/verify-otp` (`000000`) | 409 | Rejected: "Invalid or expired pickup OTP". |
| 10 | Volunteer | Valid OTP | `POST /volunteer/pickups/{id}/verify-otp` (`674603`) | 200 | Verified. Pickup status updated to `PICKED_UP`. |
| 11 | Volunteer | OTP Replay | `POST /volunteer/pickups/{id}/verify-otp` (`674603`) | 409 | Replay rejected with HTTP 409 Conflict. |
| 12 | Volunteer | Complete Delivery | `POST /volunteer/pickups/{id}/complete` | 200 | Delivery confirmed. Status updated to `COMPLETED`. |
| 13 | Security | Negative Auth | Unauthenticated & cross-role access | 401/403 | Unauthenticated -> 401; Donor->Admin -> 403; Receiver->Donor -> 403. |

---

## 7. Security & Database Integrity Verification

### 7.1 Security & Authorization Checks
- **Unauthenticated Access**: `GET /api/v1/donor/donations` without token returns HTTP 401 Unauthorized.
- **Cross-Role Access**: Donor token accessing `/api/v1/admin/pickups` returns HTTP 403 Forbidden.
- **Identity Spoofing**: Token for `hacker@mealshare.org` attempting to register `victim@mealshare.org` rejected with HTTP 403 ("Requested email does not match authenticated Firebase token email").
- **Admin Privilege Escalation**: Self-registration with role `ADMIN` rejected with HTTP 403 ("ADMIN role cannot be publicly registered").
- **IDOR Protection**: Volunteer attempting to inspect or verify another volunteer's assigned pickup rejected with HTTP 403 Forbidden.
- **OTP Verification Security**: Raw OTP is verified via `BCryptPasswordEncoder.matches()` against the salted hash. Brute-force threshold is enforced at maximum 5 attempts.

### 7.2 Database Table Schema Verification
All 10 tables are active in Docker PostgreSQL:
- `users`: 6 rows
- `donor_profiles`: 1 row
- `receiver_organizations`: 1 row
- `volunteer_profiles`: 2 rows
- `donations`: 6 rows (with PostGIS Point geometry and GiST index)
- `donation_requests`: 4 rows
- `pickups`: 3 rows (with BCrypt OTP hashes)
- `notifications`: 15 rows
- `user_device_tokens`: 0 rows
- `audit_logs`: 21 rows

---

## 8. Safe Demo Data Reset Mechanism

A dedicated Python script is provided for pre-presentation data restoration:

```powershell
python scripts/reset_demo_data.py
```

### Reset Operations:
1. Resets PostgreSQL dynamic transaction records in `mealshare_app` while preserving Flyway schema history.
2. Restores canonical demo accounts with active verification status:
   - **Admin Web**: `admin@mealshare.org` / `AdminPassword123!`
   - **Donor Mobile**: `donor@mealshare.org` / `DonorPassword123!`
   - **Receiver Mobile**: `receiver@mealshare.org` / `ReceiverPassword123!`
   - **Volunteer Mobile**: `volunteer@mealshare.org` / `VolunteerPassword123!`
3. Synchronizes Firebase Authentication display names and passwords.
4. Not exposed as an end-user UI action.

---

## 9. Full Regression Test Execution Results

| Subsystem | Command | Duration | Result | Status |
|---|---|:---:|---|:---:|
| **Backend REST API** | `.\mvnw.cmd test` | 1 min 03 s | **213 tests run, 0 failures, 0 errors, 0 skipped** | **BUILD SUCCESS** |
| **Admin Web Frontend** | `npm run build` | 10.92 s | **2518 modules transformed, dist/ generated** | **BUILD SUCCESS** |
| **Mobile Static Analysis** | `flutter analyze` | 57.7 s | **No issues found!** | **PASS** |
| **Mobile Widget Tests** | `flutter test` | 3.2 s | **6 tests passed, 0 failures** | **PASS** |
| **Live Registration Test** | `python scripts/test_live_registration.py` | 12.4 s | **6/6 live registration & onboarding tests passed** | **PASS** |
| **Live E2E Workflow** | `python execute_live_workflow.py` | 6.8 s | **13/13 cross-client workflow phases passed** | **PASS** |

---

## 10. Feature Status Classification (Academic Honesty)

### 10.1 IMPLEMENTED
- Firebase Authentication with cryptographic ID token validation.
- Complete registration and role onboarding for Donor, Receiver Org, and Volunteer.
- PostGIS spatial radius search (`ST_DWithin`, `ST_Distance`) with GiST indexing.
- Surplus donation creation with temporal validations and mandatory food safety declaration.
- FCFS claim submission and atomic quantity allocation logic.
- Admin volunteer assignment with BCrypt OTP generation.
- Volunteer numeric OTP verification and delivery completion.
- PostgreSQL in-app notifications and immutable audit log records.
- Cross-role authorization guards and IDOR protection.

### 10.2 PARTIALLY IMPLEMENTED
- **Push Notifications**: Backend FCM service structure and device token endpoints are implemented; real push delivery to physical mobile hardware requires production APNs/FCM credentials and active devices.
- **Geolocation**: Mobile discovery uses geographic coordinate queries (SRID 4326) centered on Bengaluru; hardware GPS integration on mobile devices uses coordinate fallback in desktop/emulator environments.

### 10.3 FUTURE (Not Implemented in Current Milestone)
- Machine Learning demand prediction and predictive redistribution.
- Operations Research (OR-Tools) multi-stop vehicle routing.
- Physical IoT temperature sensor logging during logistics handover.
- Public self-registration for administrator role.

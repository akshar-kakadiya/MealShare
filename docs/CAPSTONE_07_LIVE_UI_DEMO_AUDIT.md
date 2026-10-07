# MealShare CAPSTONE-07 — Real UI Live-Demo Audit Report

**Date**: October 4, 2026  
**Milestone**: CAPSTONE-07  
**Platform**: MealShare Surplus Food Redistribution Platform  
**Target Roles**: Administrator (React Web), Surplus Donor (Flutter Mobile), Receiver Organization (Flutter Mobile), Logistics Volunteer (Flutter Mobile)

---

## 1. Executive Summary

As part of the **CAPSTONE-07 Real UI Live-Demo Audit**, comprehensive end-to-end verification of all four operational client roles was conducted against the live, running MealShare stack:
- **Backend**: Spring Boot 3.4.3 (`http://localhost:8081`) with 47 REST endpoints across 18 controllers
- **Database**: PostgreSQL 16.8 + PostGIS 3.4.3 running in Docker on host port `5433`
- **Identity & Authentication**: Google Firebase Authentication (`mealshare-100`) providing cryptographic ID tokens verified via Firebase Admin SDK
- **Web Admin**: React 18.3 + TypeScript 5.8 + Tailwind CSS + Lucide Icons + Vite
- **Mobile Client**: Flutter 3.44.0 + Dart 3.7 with role-based routing (Donor, Receiver, Volunteer)

All four client roles, live UI workflows, error states, double-submission guards, offline resilience, and cross-client data integrity constraints were verified.

---

## 2. Infrastructure & Live Environment Verification

| Component | Target URL / Port | Technology | Live Verification Status | Output / Details |
|---|---|---|:---:|---|
| **PostgreSQL + PostGIS** | `localhost:5433` (Docker: `mealshare-postgres`) | PostgreSQL 16.8, PostGIS 3.4.3, SRID 4326 | **PASS** | Container healthy; tables `users`, `donations`, `donation_requests`, `pickups`, `notifications`, `audit_logs` online. |
| **Spring Boot REST API** | `http://localhost:8081` | Spring Boot 3.4.3, Java 17 | **PASS** | `GET /api/v1/health` returns `{"status":"UP","service":"mealshare-backend"}`. |
| **Firebase Admin SDK** | Backend service account | `firebase-admin` 9.2.0 | **PASS** | Initialized from `mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json`. |
| **Firebase Auth Token Exchange** | `identitytoolkit.googleapis.com` | Google Identity Toolkit v1 | **PASS** | Verified token acquisition and signature validation for all 5 test accounts. |

---

## 3. Client UI Audit & Code Hardening

### 3.1 Issues Identified and Fixed

1. **`mobile/lib/donor/donation_details_screen.dart`** (Dialog Double-Submission Vulnerability):
   - *Issue*: The Approve and Reject action dialogs in `donation_details_screen.dart` lacked processing guards (`isProcessing`). Repeated button taps during network latency could trigger duplicate state transitions or conflict responses.
   - *Fix*: Wrapped dialog builders with `StatefulBuilder` and introduced `isProcessing` flag. Action buttons disable immediately and display inline `CircularProgressIndicator` during in-flight requests. Cancel buttons are disabled during processing.

2. **`execute_live_workflow.py`** (Audit Script Field Alignments):
   - *Issue*: Script assumed `allocatedQuantity` existed directly on the initial donation creation response and `receiverName` on donor request list.
   - *Fix*: Aligned script to evaluate `allocatedQuantity` upon request adjudication response (`ApproveDonationRequestResponse`) and read `receiverOrganizationName` from `DonorDonationRequestResponse`.

---

## 4. Role-by-Role UI Audit Results

### 4.1 Administrator Role (React Admin Web)
- **Authentication & RBAC**: Real Firebase login verifies against PostgreSQL `users` table via `GET /users/me`. Non-admin or suspended users are rejected with explicit access-denied screens.
- **Dashboard (`/admin`)**: Real summary metrics load cleanly from `/admin/dashboard/stats` (Active Users, Active Donations, Pending Requests, Active Deliveries).
- **Users Management (`/admin/users`)**: Search, role filtering, pagination, and user status transitions (`ACTIVE`, `RESTRICTED`, `SUSPENDED`) guarded by `statusUpdating` flag.
- **Donations Page (`/admin/donations`)**: Displays real listings from `/admin/donations` with category chips, quantities, and food safety badges.
- **Requests Page (`/admin/requests`)**: Displays real donation requests with allocated vs requested quantities.
- **Pickups Page (`/admin/pickups`)**: Features live pickup status tracking and volunteer assignment modal with `assignLoading` double-submit guard. Raw OTP is strictly masked for operational security.
- **Organization Verification (`/admin/verification`)**: Adjudicates pending NGOs (`APPROVED` / `REJECTED`) with cascading activation to the underlying `users` record.
- **Settings & Security (`/admin/settings`)**: Real password update via Firebase `updatePassword`, connected database statistics, and safe logout.

### 4.2 Surplus Food Donor Role (Flutter Mobile)
- **Home Dashboard (`/donor/home`)**: Real-time impact cards (Meals Donated, People Helped) backed by live server queries. Pending request alert banner directs to approval screen.
- **Publish Surplus (`/donor/create`)**: Input fields for quantity, unit, categories, expiry timestamps, pickup window, and mandatory Food Safety Declaration. Protected against double-submission via `_isSubmitting` flag.
- **Manage Listings (`/donor/donations`)**: Filterable list (Available, Requested, Approved, Completed) with pull-to-refresh and empty states.
- **Incoming Requests (`/donor/requests` & `/donor/donation/:id`)**: Displays pending requests in strict First-Come-First-Served (FCFS) order. Approve and Reject dialogs feature `isProcessing` indicators preventing double submission.

### 4.3 Receiver Organization Role (Flutter Mobile)
- **Home Screen (`/receiver/home`)**: Real impact counters (Meals Received, Food Received, Pickups) and direct discover shortcut.
- **Spatial Discovery (`/receiver/discover`)**: GiST-indexed PostGIS spatial query (`GET /receiver/donations/nearby`) within configurable radius. Displays distance in kilometers, temporal expiry warnings, and empty states.
- **Donation Details & Request (`/receiver/donation/:id`)**: Food safety badges, storage conditions, and pickup address. Request modal enforces positive quantity, reasons, and double-submission protection (`isSubmitting`).
- **My Requests (`/receiver/requests`)**: Status chips (Pending, Approved, Rejected, Completed), withdrawal action with confirmation dialog, and error feedback.

### 4.4 Logistics Volunteer Role (Flutter Mobile)
- **Deliveries Dashboard (`/volunteer/home`)**: Live pickup listings filtered by tabs (`All`, `ASSIGNED`, `PICKED_UP`, `COMPLETED`).
- **Assigned Route Card**: Displays pickup location, donor contact, receiver destination, scheduled window, and quantity.
- **Secure OTP Verification**: 6-digit numeric input with live loading spinner (`verifying`), error banner for incorrect codes, and immediate transition to `PICKED_UP`.
- **Delivery Confirmation**: Confirmation modal before final handover completion, transitioning status to `COMPLETED`.

---

## 5. Fresh End-to-End Operational Lifecycle Verification

A completely fresh surplus food redistribution workflow was executed against live Spring Boot and Docker PostgreSQL:

```
[1] DONOR (Grand Gourmet Catering)
    ↳ Creates Donation: "Fresh Steamed Meals & Dal (CAPSTONE-07 Live)"
    ↳ Qty: 40 meals, Prepared: -1h, Window: now to +6h, Safe: True
    ↳ Status: AVAILABLE, ID: ab112e53-aa9b-4e98-af24-85ccaa9b9445

[2] RECEIVER (Hope Community Shelter Foundation)
    ↳ PostGIS Spatial Search (12.9700, 77.6000, 15 km radius)
    ↳ Discovered donation at distance 0.61 km
    ↳ Submits claim for 15 meals
    ↳ Status: PENDING, ID: cf885428-ab4b-4a69-93b4-e1df298f1d27

[3] DONOR (Grand Gourmet Catering)
    ↳ Views incoming request in FCFS queue
    ↳ Approves claim for 15 meals
    ↳ Request Status: APPROVED (Allocated: 15.0)
    ↳ Donation Status: AVAILABLE (Allocated: 15.0, Remaining Surplus: 25.0)

[4] ADMIN (Platform Operations)
    ↳ Assigns Volunteer: Alex Kumar
    ↳ Scheduled Window: +2 hours
    ↳ Generates Cryptographic Handover OTP: 373581
    ↳ Pickup Status: ASSIGNED, ID: 7bdb2a58-9815-4669-a21c-c9f5cb4460e4

[5] VOLUNTEER (Alex Kumar)
    ↳ Dashboard displays assigned pickup
    ↳ Submits WRONG OTP ('000000') ➔ Rejected with HTTP 409 (Invalid OTP)
    ↳ Submits CORRECT OTP ('373581') ➔ Verified! Status: PICKED_UP
    ↳ Re-submits OTP ('373581') ➔ Replay rejected with HTTP 409
    ↳ Completes handover ➔ Status: COMPLETED (CompletedAt recorded)

[6] SECURITY & IDOR CHECKS
    ↳ Unassigned Volunteer accesses pickup ➔ HTTP 403 Forbidden
    ↳ Unauthenticated request ➔ HTTP 401 Unauthorized
    ↳ Donor attempts Admin endpoint ➔ HTTP 403 Forbidden
    ↳ Receiver attempts Donor endpoint ➔ HTTP 403 Forbidden
```

### Database Persistence Evidence
- **Donation Record (`donations`)**:
  - `id`: `ab112e53-aa9b-4e98-af24-85ccaa9b9445`
  - `status`: `PICKED_UP`
  - `quantity`: `40.0`, `allocated_quantity`: `15.0`
- **Pickup Record (`pickups`)**:
  - `id`: `7bdb2a58-9815-4669-a21c-c9f5cb4460e4`
  - `status`: `COMPLETED`
  - `otp_attempts`: `1` (failed attempt recorded before success)
  - `otp_verified_at`: `2026-10-04 04:19:59.510971+00`
  - `picked_up_at`: `2026-10-04 04:19:59.510971+00`
  - `completed_at`: `2026-10-04 04:19:59.641361+00`
- **Audit Logs (`audit_logs`)**:
  - `8` discrete immutable audit records persisted across donation creation, request lifecycle, approval, pickup assignment, OTP verification, and completion.

---

## 6. Offline & Backend-Unavailable Behavior Verification

To guarantee that the application demonstrates clear error handling during live demonstration without silent prototype fallback:
1. **Spring Boot daemon temporarily halted**.
2. **Health check query verified offline**: Connection refused (HTTP code 1).
3. **Client behavior inspected**:
   - `ApiClient` throws `ApiException(statusCode: 0, message: "Cannot reach MealShare backend")`.
   - UI screens display error banners with "Backend unavailable: Cannot reach MealShare backend" and a visible "Retry" button.
   - No mock or simulated data is substituted in normal operating mode.
4. **Spring Boot daemon restarted**: Health check returns `UP`, and retry buttons cleanly restore live application state.

---

## 7. Full Regression Suite Results

All three sub-projects passed their respective verification commands:

| Project | Verification Command | Duration | Result | Status |
|---|---|:---:|:---:|:---:|
| **Spring Boot Backend** | `.\mvnw.cmd test` | 1 min 05 s | **213 tests run, 0 failures, 0 errors, 0 skipped** | **BUILD SUCCESS** |
| **React Admin Web** | `npm run build` | 11.51 s | **2518 modules transformed, clean production bundle** | **BUILD SUCCESS** |
| **Flutter Mobile** | `flutter analyze` | 58.1 s | **No issues found!** | **PASS** |

---

## 8. Remaining Known Limitations (Academic Honesty)

1. **Hardware / Device-Bound Features**:
   - Camera barcode/QR scanning and device-native GPS geolocation were verified using coordinate fallbacks and direct manual input rather than physical Android/iOS hardware sensors.
2. **Push Notifications**:
   - The FCM service layer is fully structured with token registration endpoints; live delivery to APNs/FCM gateways requires production cloud credentials and real mobile device tokens.
3. **Route Optimization & AI/ML**:
   - The platform relies on database-authoritative PostGIS Euclidean/spheroidal distance indexing and FCFS scheduling; OR-Tools VRP and predictive demand models remain proposed future extensions.

# MealShare Final Feature Inventory

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-01 (Engineering Audit & Presentation Readiness)  
> **Status**: FINAL & VERIFIED  

---

## 1. Status Classifications

Every significant feature across the MealShare platform is classified into one of four engineering categories based on actual source inspection and automated verification:

- **A. FULLY IMPLEMENTED**: Complete vertical slice. Production-grade backend REST API, PostgreSQL/PostGIS database persistence, security controls, and functional frontend UI (React Admin Web or Flutter Mobile).
- **B. PARTIALLY IMPLEMENTED**: Backend API and database logic are complete and functional, but frontend integration is partial, requires mock toggle, or handles a subset of edge cases.
- **C. PROTOTYPE / DEMO ONLY**: UI screens or client prototypes exist for presentation/mock walkthroughs without persistent backend REST integration or database backing.
- **D. NOT IMPLEMENTED / FUTURE**: Architecture-level proposals or planned features not currently present in source code.

---

## 2. Authentication & User Management

### 2.1 Firebase Authentication & Identity Mapping
- **User Role**: All Roles (`DONOR`, `RECEIVER_ORG`, `RECEIVER_VOL`, `ADMIN`)
- **Frontend**: 
  - React Admin Web: `admin-web/src/pages/LoginPage.tsx`, `admin-web/src/contexts/AuthContext.tsx`
  - Flutter Mobile: `mobile/lib/auth/screens/login_screen.dart`, `mobile/lib/auth/auth_service.dart`
- **Backend API**:
  - `POST /api/v1/users/register`
  - `GET /api/v1/users/me`
- **Database Support**: Table `users` (columns: `firebase_uid`, `email`, `role`, `status`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.security.FirebaseAuthenticationFilter`, `com.mealshare.user.UserController`, `com.mealshare.user.UserService`
  - React: `admin-web/src/config/firebase.ts`, `admin-web/src/api/client.ts`
  - Flutter: `mobile/lib/auth/auth_service.dart`, `mobile/lib/shared/api_client.dart`
- **Demonstration Availability**: **Available** (Live login for Admin, Donor, Receiver, Volunteer).

### 2.2 Donor Profile Management
- **User Role**: `DONOR`
- **Frontend**: Flutter Mobile (`mobile/lib/donor/profile_screen.dart`)
- **Backend API**:
  - `GET /api/v1/donor/profile`
  - `PUT /api/v1/donor/profile`
- **Database Support**: Table `donor_profiles` (1:1 with `users`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.donor.DonorProfileController`, `com.mealshare.donor.DonorProfileService`, `com.mealshare.donor.DonorProfile`
  - Flutter: `mobile/lib/donor/profile_screen.dart`
- **Demonstration Availability**: **Available**.

### 2.3 Receiver Organization Profile Management
- **User Role**: `RECEIVER_ORG`
- **Frontend**: Flutter Mobile (`mobile/lib/receiver/profile_screen.dart`)
- **Backend API**:
  - `GET /api/v1/receiver/profile`
  - `PUT /api/v1/receiver/profile`
- **Database Support**: Table `receiver_organizations` (1:1 with `users`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.receiver.ReceiverProfileController`, `com.mealshare.receiver.ReceiverOrganizationService`, `com.mealshare.receiver.ReceiverOrganization`
  - Flutter: `mobile/lib/receiver/profile_screen.dart`
- **Demonstration Availability**: **Available**.

### 2.4 Volunteer Profile & Availability Management
- **User Role**: `RECEIVER_VOL`, `ADMIN`
- **Frontend**: 
  - Flutter Mobile (`mobile/lib/home/volunteer_home_screen.dart`)
  - React Admin Web (`admin-web/src/pages/UsersPage.tsx`)
- **Backend API**: Integrated via `AdminUserController`, `PickupService` (availability status transitions)
- **Database Support**: Table `volunteer_profiles` (`availability_status`: `AVAILABLE`, `BUSY`, `INACTIVE`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.volunteer.VolunteerProfile`, `com.mealshare.volunteer.VolunteerAvailabilityStatus`, `com.mealshare.pickup.PickupService`
- **Demonstration Availability**: **Available**.

---

## 3. Donor Features

### 3.1 Surplus Food Listing Creation & Food Safety Declaration
- **User Role**: `DONOR`
- **Frontend**: Flutter Mobile (`mobile/lib/donor/create_donation_screen.dart`)
- **Backend API**: `POST /api/v1/donor/donations`
- **Database Support**: Table `donations` (PostGIS `location` `GEOMETRY(Point, 4326)`, `food_safety_declared`, `food_safety_declaration_text`, `preparation_datetime`, `consume_before`, `pickup_available_from`, `pickup_available_until`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.donation.DonorDonationController`, `com.mealshare.donation.DonationService`, `com.mealshare.donation.Donation`
  - Flutter: `mobile/lib/donor/create_donation_screen.dart`, `mobile/lib/mock/app_state.dart`
- **Demonstration Availability**: **Available**.

### 3.2 Donor Donations Listing & Detail Inspection
- **User Role**: `DONOR`
- **Frontend**: Flutter Mobile (`mobile/lib/donor/donations_list_screen.dart`, `mobile/lib/donor/donation_details_screen.dart`)
- **Backend API**:
  - `GET /api/v1/donor/donations`
  - `GET /api/v1/donor/donations/{id}`
- **Database Support**: Table `donations`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.donation.DonorDonationController`, `com.mealshare.donation.DonationService`
  - Flutter: `mobile/lib/donor/donations_list_screen.dart`, `mobile/lib/donor/donation_details_screen.dart`
- **Demonstration Availability**: **Available**.

### 3.3 Donation Cancellation
- **User Role**: `DONOR`
- **Frontend**: Flutter Mobile (`mobile/lib/donor/donation_details_screen.dart`)
- **Backend API**: `POST /api/v1/donor/donations/{id}/cancel`
- **Database Support**: Table `donations` (`status` $\rightarrow$ `CANCELLED`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.donation.DonorDonationController`, `com.mealshare.donation.DonationService`
  - Flutter: `mobile/lib/mock/app_state.dart`
- **Demonstration Availability**: **Available**.

### 3.4 Incoming Request Review & FCFS Approval
- **User Role**: `DONOR`
- **Frontend**: Flutter Mobile (`mobile/lib/donor/requests_screen.dart`, `mobile/lib/donor/donation_details_screen.dart`)
- **Backend API**:
  - `GET /api/v1/donor/donations/{donationId}/requests`
  - `POST /api/v1/donor/requests/{requestId}/approve`
  - `POST /api/v1/donor/requests/{requestId}/reject`
- **Database Support**: Tables `donation_requests`, `donations` (`allocated_quantity`, row-level pessimistic locking)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.donor.request.DonorRequestController`, `com.mealshare.donor.request.DonorRequestService`
  - Flutter: `mobile/lib/donor/requests_screen.dart`, `mobile/lib/mock/app_state.dart`
- **Demonstration Availability**: **Available**.

---

## 4. Receiver Organization Features

### 4.1 PostGIS Spatial Nearby Donation Discovery
- **User Role**: `RECEIVER_ORG`, `RECEIVER_VOL`
- **Frontend**: Flutter Mobile (`mobile/lib/receiver/discover_screen.dart`)
- **Backend API**: `GET /api/v1/receiver/donations/nearby` (`latitude`, `longitude`, `radiusKm`, `foodCategory`)
- **Database Support**: Table `donations` (`ST_DWithin`, `ST_Distance` spherical geography on GiST index)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.receiver.ReceiverDonationController`, `com.mealshare.receiver.ReceiverDiscoveryService`, `com.mealshare.donation.DonationRepository`
  - Flutter: `mobile/lib/receiver/discover_screen.dart`, `mobile/lib/mock/app_state.dart`
- **Demonstration Availability**: **Available**.

### 4.2 Donation Claim Request Submission
- **User Role**: `RECEIVER_ORG`
- **Frontend**: Flutter Mobile (`mobile/lib/receiver/donation_details_screen.dart`)
- **Backend API**: `POST /api/v1/receiver/donations/{donationId}/requests`
- **Database Support**: Table `donation_requests` (`status` = `PENDING`, unique active request index)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.receiver.ReceiverDonationController`, `com.mealshare.receiver.request.DonationRequestService`
  - Flutter: `mobile/lib/receiver/donation_details_screen.dart`, `mobile/lib/mock/app_state.dart`
- **Demonstration Availability**: **Available**.

### 4.3 My Requests Management & Withdrawal
- **User Role**: `RECEIVER_ORG`
- **Frontend**: Flutter Mobile (`mobile/lib/receiver/my_requests_screen.dart`)
- **Backend API**:
  - `GET /api/v1/receiver/requests`
  - `GET /api/v1/receiver/requests/{id}`
  - `POST /api/v1/receiver/requests/{id}/withdraw`
- **Database Support**: Table `donation_requests`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.receiver.request.ReceiverRequestController`, `com.mealshare.receiver.request.DonationRequestService`
  - Flutter: `mobile/lib/receiver/my_requests_screen.dart`, `mobile/lib/mock/app_state.dart`
- **Demonstration Availability**: **Available**.

### 4.4 Live Pickup GPS Tracking (Map Visualization)
- **User Role**: `RECEIVER_ORG`
- **Frontend**: Flutter Mobile (`mobile/lib/receiver/pickup_tracking_screen.dart`)
- **Backend API**: No live GPS streaming endpoint implemented (HTTP REST polling/status only)
- **Database Support**: Static coordinates in `donations` and `receiver_organizations`
- **Current Status**: **C. PROTOTYPE / DEMO ONLY**
- **Relevant Source Files**:
  - Flutter: `mobile/lib/receiver/pickup_tracking_screen.dart` (uses `AppState` timeline mockup)
- **Demonstration Availability**: **UI Prototype Only** (Visual timeline, not real-time GPS stream).

---

## 5. Volunteer Features

### 5.1 Assigned Pickups Inspection & IDOR Isolation
- **User Role**: `RECEIVER_VOL`
- **Frontend**: Flutter Mobile (`mobile/lib/home/volunteer_home_screen.dart`)
- **Backend API**:
  - `GET /api/v1/volunteer/pickups`
  - `GET /api/v1/volunteer/pickups/{pickupId}`
- **Database Support**: Table `pickups` joined with `volunteer_profiles`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.pickup.controller.VolunteerPickupController`, `com.mealshare.pickup.PickupService`
  - Flutter: `mobile/lib/home/volunteer_home_screen.dart`
- **Demonstration Availability**: **Available**.

### 5.2 Cryptographic OTP Verification (Pickup Handover)
- **User Role**: `RECEIVER_VOL`
- **Frontend**: Flutter Mobile (`mobile/lib/home/volunteer_home_screen.dart` OTP dialog)
- **Backend API**: `POST /api/v1/volunteer/pickups/{pickupId}/verify-otp`
- **Database Support**: Table `pickups` (`otp_hash` BCrypt, `otp_expires_at`, `otp_attempts`, `otp_verified_at`, `status` $\rightarrow$ `PICKED_UP`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.pickup.controller.VolunteerPickupController`, `com.mealshare.pickup.PickupService`, `com.mealshare.pickup.otp.PickupOtpService`
  - Flutter: `mobile/lib/home/volunteer_home_screen.dart`
- **Demonstration Availability**: **Available**.

### 5.3 Delivery Completion
- **User Role**: `RECEIVER_VOL`
- **Frontend**: Flutter Mobile (`mobile/lib/home/volunteer_home_screen.dart`)
- **Backend API**: `POST /api/v1/volunteer/pickups/{pickupId}/complete`
- **Database Support**: Table `pickups` (`status` $\rightarrow$ `COMPLETED`, `completed_at`), Table `volunteer_profiles` (`completed_deliveries_count` +1, `total_meals_delivered`, `availability_status` $\rightarrow$ `AVAILABLE`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.pickup.controller.VolunteerPickupController`, `com.mealshare.pickup.PickupService`
  - Flutter: `mobile/lib/home/volunteer_home_screen.dart`
- **Demonstration Availability**: **Available**.

---

## 6. Admin Web Features

### 6.1 Admin Operational KPI Dashboard
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/DashboardPage.tsx`)
- **Backend API**: `GET /api/v1/admin/dashboard`
- **Database Support**: Aggregates from `users`, `donations`, `donation_requests`, `pickups`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.admin.controller.AdminDashboardController`, `com.mealshare.admin.service.AdminDashboardService`
  - React: `admin-web/src/pages/DashboardPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.2 User Oversight & Account Status Adjudication
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/UsersPage.tsx`)
- **Backend API**:
  - `GET /api/v1/admin/users`
  - `GET /api/v1/admin/users/{id}`
  - `PATCH /api/v1/admin/users/{id}/status`
- **Database Support**: Table `users` (`status`: `ACTIVE`, `INACTIVE`, `RESTRICTED`, `SUSPENDED`)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.admin.controller.AdminUserController`, `com.mealshare.admin.service.AdminUserService`
  - React: `admin-web/src/pages/UsersPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.3 Receiver Organization Verification Queue
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/VerificationPage.tsx`)
- **Backend API**:
  - `GET /api/v1/admin/verification`
  - `GET /api/v1/admin/verification/{id}`
  - `PATCH /api/v1/admin/verification/{id}/status`
- **Database Support**: Table `receiver_organizations` (`verification_status`: `PENDING`, `APPROVED`, `REJECTED`), cascaded update to `users.status`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.admin.controller.AdminVerificationController`, `com.mealshare.admin.service.AdminVerificationService`
  - React: `admin-web/src/pages/VerificationPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.4 Volunteer Assignment to Approved Claims
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/PickupsPage.tsx`)
- **Backend API**:
  - `POST /api/v1/admin/pickups`
  - `GET /api/v1/admin/pickups`
  - `GET /api/v1/admin/pickups/{pickupId}`
- **Database Support**: Table `pickups`, `volunteer_profiles`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.pickup.controller.AdminPickupController`, `com.mealshare.pickup.PickupService`
  - React: `admin-web/src/pages/PickupsPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.5 Donations & Requests Operational Oversight (Read-Only)
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/DonationsPage.tsx`, `admin-web/src/pages/RequestsPage.tsx`)
- **Backend API**:
  - `GET /api/v1/admin/donations`
  - `GET /api/v1/admin/donations/{id}`
  - `GET /api/v1/admin/requests`
  - `GET /api/v1/admin/requests/{id}`
- **Database Support**: Tables `donations`, `donation_requests`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.admin.controller.AdminDonationController`, `com.mealshare.admin.controller.AdminRequestController`
  - React: `admin-web/src/pages/DonationsPage.tsx`, `admin-web/src/pages/RequestsPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.6 Analytics & Redistribution Reports
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/ReportsPage.tsx`)
- **Backend API**: `GET /api/v1/admin/reports/summary` (`timeRange`: `7d`, `30d`, `90d`, `1y`)
- **Database Support**: Aggregates from `donations`, `donation_requests`, `pickups`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.admin.controller.AdminReportController`, `com.mealshare.admin.service.AdminReportService`
  - React: `admin-web/src/pages/ReportsPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.7 Operational Audit Logs Inspection
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (Modal in Admin Dashboard / Activity views)
- **Backend API**: `GET /api/v1/admin/audit-logs`
- **Database Support**: Table `audit_logs` (`JSONB` delta state, IP, user-agent)
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.admin.controller.AdminAuditLogController`, `com.mealshare.admin.service.AdminAuditLogService`
  - React: `admin-web/src/pages/DashboardPage.tsx`
- **Demonstration Availability**: **Available**.

### 6.8 Admin Profile & Preference Settings
- **User Role**: `ADMIN`
- **Frontend**: React Admin Web (`admin-web/src/pages/SettingsPage.tsx`)
- **Backend API**: No dedicated backend settings endpoint.
- **Database Support**: None (Local component state only).
- **Current Status**: **C. PROTOTYPE / DEMO ONLY**
- **Relevant Source Files**:
  - React: `admin-web/src/pages/SettingsPage.tsx`
- **Demonstration Availability**: **UI Prototype Only**.

---

## 7. Cross-Cutting Systems

### 7.1 In-App Notifications
- **User Role**: All Roles
- **Frontend**:
  - React Admin: `admin-web/src/pages/NotificationsPage.tsx`
  - Flutter Mobile: `mobile/lib/donor/notifications_screen.dart`, `mobile/lib/receiver/notifications_screen.dart`
- **Backend API**:
  - `GET /api/v1/notifications`
  - `GET /api/v1/notifications/unread-count`
  - `PATCH /api/v1/notifications/{id}/read`
  - `POST /api/v1/notifications/read-all`
- **Database Support**: Table `notifications`
- **Current Status**: **A. FULLY IMPLEMENTED**
- **Relevant Source Files**:
  - Backend: `com.mealshare.notification.NotificationController`, `com.mealshare.notification.NotificationService`, `com.mealshare.notification.NotificationEventListener`
- **Demonstration Availability**: **Available**.

### 7.2 Push Notification Device Token Registration & FCM Dispatch
- **User Role**: All Roles
- **Frontend**: Token registration capability in mobile client
- **Backend API**:
  - `POST /api/v1/notifications/device-token`
  - `DELETE /api/v1/notifications/device-token`
- **Database Support**: Table `user_device_tokens`
- **Current Status**: **B. PARTIALLY IMPLEMENTED**
  - *Backend & DB Persistence*: Fully implemented and verified.
  - *Physical Device Delivery*: **NOT TESTED** in local/demo test suite (requires external APNS/FCM keys and physical hardware).
- **Relevant Source Files**:
  - Backend: `com.mealshare.notification.fcm.FcmService`, `com.mealshare.notification.UserDeviceToken`
- **Demonstration Availability**: **Database Verification Only** (In-app notifications deliver in real-time; physical push notifications marked as NOT TESTED).

### 7.3 Predictive AI/ML & Automated Optimization
- **User Role**: Platform Background Engine
- **Frontend**: None
- **Backend API**: None
- **Database Support**: None
- **Current Status**: **D. NOT IMPLEMENTED (FUTURE / PROPOSED)**
- **Relevant Source Files**:
  - Conceptual proposal only in `docs/MealShare_Architecture.md` ("Future Integrations").
- **Demonstration Availability**: **Not Available** (Proposed for future roadmap: demand prediction, allocation optimization, route optimization).

---

## 8. Summary Feature Matrix

| # | Feature Name | Primary Role | Frontend | Backend API | Database Table | Status | Demo Status |
|---|--------------|--------------|----------|-------------|----------------|--------|-------------|
| 1 | Firebase Authentication | All | React & Flutter | `/api/v1/users/**` | `users` | **A** | **Pass** |
| 2 | Donor Profile Setup | Donor | Flutter | `/api/v1/donor/profile` | `donor_profiles` | **A** | **Pass** |
| 3 | Receiver Profile Setup | Receiver | Flutter | `/api/v1/receiver/profile` | `receiver_organizations` | **A** | **Pass** |
| 4 | Volunteer Profile Setup | Volunteer / Admin | Flutter & React | Internal / Admin | `volunteer_profiles` | **A** | **Pass** |
| 5 | Donation Publishing & Safety Decl. | Donor | Flutter | `/api/v1/donor/donations` | `donations` | **A** | **Pass** |
| 6 | Donation Management & Cancel | Donor | Flutter | `/api/v1/donor/donations/**` | `donations` | **A** | **Pass** |
| 7 | Request Review & FCFS Approval | Donor | Flutter | `/api/v1/donor/requests/**` | `donation_requests`, `donations` | **A** | **Pass** |
| 8 | PostGIS Spatial Discovery | Receiver | Flutter | `/api/v1/receiver/donations/nearby` | `donations` (GiST) | **A** | **Pass** |
| 9 | Request Submission & Withdraw | Receiver | Flutter | `/api/v1/receiver/requests/**` | `donation_requests` | **A** | **Pass** |
| 10 | Volunteer Assigned Pickups View | Volunteer | Flutter | `/api/v1/volunteer/pickups/**` | `pickups` | **A** | **Pass** |
| 11 | OTP Pickup Handover Verification | Volunteer | Flutter | `/api/v1/volunteer/pickups/**/verify-otp` | `pickups` | **A** | **Pass** |
| 12 | Delivery Completion Tracking | Volunteer | Flutter | `/api/v1/volunteer/pickups/**/complete` | `pickups`, `volunteer_profiles` | **A** | **Pass** |
| 13 | Admin Dashboard & KPIs | Admin | React | `/api/v1/admin/dashboard` | Aggregated views | **A** | **Pass** |
| 14 | Admin User Status Control | Admin | React | `/api/v1/admin/users/**` | `users` | **A** | **Pass** |
| 15 | Receiver Org Verification Queue | Admin | React | `/api/v1/admin/verification/**` | `receiver_organizations` | **A** | **Pass** |
| 16 | Admin Volunteer Assignment | Admin | React | `/api/v1/admin/pickups` | `pickups`, `volunteer_profiles` | **A** | **Pass** |
| 17 | Admin Audit Logs Oversight | Admin | React | `/api/v1/admin/audit-logs` | `audit_logs` | **A** | **Pass** |
| 18 | Admin Aggregated Reports | Admin | React | `/api/v1/admin/reports/**` | Aggregated views | **A** | **Pass** |
| 19 | In-App Notifications Persistence | All | React & Flutter | `/api/v1/notifications/**` | `notifications` | **A** | **Pass** |
| 20 | Push Token Registration & FCM | All | Flutter (Client) | `/api/v1/notifications/device-token` | `user_device_tokens` | **B** | **Partial (DB Only)** |
| 21 | Live Map GPS Breadcrumbs | Receiver | Flutter | None | None | **C** | **UI Prototype** |
| 22 | Admin Settings Page | Admin | React | None | None | **C** | **UI Prototype** |
| 23 | Predictive Demand & Route ML | System | None | None | None | **D** | **Future** |

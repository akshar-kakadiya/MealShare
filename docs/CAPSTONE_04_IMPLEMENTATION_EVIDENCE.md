# MealShare Implementation Evidence Specification

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2 (>= 50% Implementation Requirement)  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Compliance with Presentation-2 Implementation Guidelines

According to the official **PES University MCA Presentation-2 Guidelines** (`Presentation-2 Deliverables.pdf`):
> - *Students must demonstrate at least 50% of the project functionalities implemented.*
> - *The implemented modules/features should be functional and demonstrable.*
> - *Login functionality will be considered as only 5% of the project implementation. Therefore, students are expected to demonstrate substantial project-specific functionality beyond the login module.*

This document provides concrete, verifiable evidence across **15 core engineering modules**, demonstrating that MealShare substantially exceeds the minimum 50% requirement through fully implemented backend controllers, database schemas, frontend interfaces, and automated test suites.

---

## 2. Implemented Modules Evidence Matrix

### Module 1: Authentication & Role-Based Access Control (RBAC)
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Auth Screen (`mobile/lib/screens/auth/`), React Login Page (`admin-web/src/pages/Login.tsx`).
- **Backend**: `FirebaseAuthenticationFilter.java`, `SecurityConfig.java`, `UserController.java`.
- **Database**: PostgreSQL `users` table (`V2__create_foundation_tables.sql`), indexes on `firebase_uid`, `email`, `role`.
- **REST Endpoints**: `POST /api/v1/users/register`, `GET /api/v1/users/me`.
- **Automated Test Evidence**: 9 tests in `SecurityTest.java` verifying token extraction, database role resolution, and suspended account isolation.

### Module 2: Donor Profile Management
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Donor Profile Screen (`mobile/lib/screens/donor/donor_profile_screen.dart`).
- **Backend**: `DonorProfileController.java`, `DonorProfileService.java`.
- **Database**: PostgreSQL `donor_profiles` table, FK `user_id` referencing `users(id) ON DELETE CASCADE`.
- **REST Endpoints**: `GET /api/v1/donor/profile`, `PUT /api/v1/donor/profile`.
- **Automated Test Evidence**: `DonorSecurityAndApiTest.java` verifying profile retrieval, coordinate updates, and IDOR protection.

### Module 3: Surplus Food Donation Publishing
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Create Donation Screen (`mobile/lib/screens/donor/create_donation_screen.dart`).
- **Backend**: `DonorDonationController.java`, `DonationService.java`.
- **Database**: PostgreSQL `donations` table (`V3__create_donations_table.sql`), food category enum check constraint.
- **REST Endpoints**: `POST /api/v1/donor/donations`, `GET /api/v1/donor/donations`, `GET /api/v1/donor/donations/{id}`.
- **Automated Test Evidence**: 18 tests in `DonationServiceTest.java` verifying listing creation, temporal validation, and category enforcement.

### Module 4: Embedded Food Safety Self-Declarations
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Donation Safety Form Checklist & Agreement Toggle.
- **Backend**: `DonationService.validateFoodSafety()`, `DonationService.validateTemporalConstraints()`.
- **Database**: `donations.food_safety_declared`, `donations.food_safety_declaration_text`, `donations.food_safety_declared_at`.
- **REST Endpoints**: Validated within `POST /api/v1/donor/donations`.
- **Automated Test Evidence**: Unit tests verifying rejection (`HTTP 400`) if `foodSafetyDeclared` is false or consume-before is in the past.

### Module 5: PostGIS Spatial Proximity Discovery
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Nearby Discovery Screen (`mobile/lib/screens/receiver/nearby_donations_screen.dart`).
- **Backend**: `ReceiverDonationController.java`, `ReceiverDiscoveryService.java`, `DonationRepository.java`.
- **Database**: PostGIS `GEOMETRY(Point, 4326)` column with GiST index (`idx_donations_location_gist`) enabled via `V1__enable_postgis.sql`.
- **REST Endpoints**: `GET /api/v1/receiver/donations/nearby?latitude=x&longitude=y&radiusKm=z`.
- **Automated Test Evidence**: 7 tests in `ReceiverDiscoveryServiceTest.java` and 3 tests in `SpatialRadiusCalculationTest.java` verifying `ST_DWithin` and geodesic distance sorting.

### Module 6: Receiver Organization Claim Request Workflow
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Claim Request Dialog & Request History (`mobile/lib/screens/receiver/my_requests_screen.dart`).
- **Backend**: `ReceiverRequestController.java`, `DonationRequestService.java`.
- **Database**: PostgreSQL `donation_requests` table (`V4__create_donation_requests_table.sql`), partial unique index `idx_unique_active_donation_request`.
- **REST Endpoints**: `POST /api/v1/receiver/donations/{id}/requests`, `GET /api/v1/receiver/requests`, `POST /api/v1/receiver/requests/{id}/withdraw`.
- **Automated Test Evidence**: 10 tests in `DonationRequestServiceTest.java` verifying claim submission, active request uniqueness, and withdrawal constraints.

### Module 7: Concurrency-Safe First-Come, First-Served (FCFS) Allocation
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Donor Incoming Claims Tab (`mobile/lib/screens/donor/donation_requests_screen.dart`).
- **Backend**: `DonorRequestController.java`, `DonationRequestService.approveRequest()`.
- **Database**: `donations.allocated_quantity` (`V5__add_donation_allocation.sql`) with check constraints `chk_donations_allocated_quantity_non_negative` and `chk_donations_allocated_le_quantity`.
- **REST Endpoints**: `GET /api/v1/donor/donations/{id}/requests`, `POST /api/v1/donor/requests/{id}/approve`, `POST /api/v1/donor/requests/{id}/reject`.
- **Automated Test Evidence**: Concurrency integration tests verifying that concurrent claims cannot over-allocate available donation quantity.

### Module 8: Volunteer Profile & Availability Management
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Volunteer Home Screen (`mobile/lib/screens/volunteer/volunteer_home_screen.dart`).
- **Backend**: `VolunteerProfileRepository.java`, `PickupService.java`.
- **Database**: `volunteer_profiles` table, dynamic status column `availability_status` (`AVAILABLE`, `BUSY`, `INACTIVE`) enabled via `V6`.
- **REST Endpoints**: Handled via user registration and admin assignment queries.
- **Automated Test Evidence**: Unit tests in `PickupServiceTest.java` confirming that only volunteers with `availability_status = 'AVAILABLE'` are eligible for dispatch.

### Module 9: Admin Volunteer Pickup Dispatch
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: React Admin Pickups Page & Assign Modal (`admin-web/src/pages/Pickups.tsx`).
- **Backend**: `AdminPickupController.java`, `PickupService.assignVolunteer()`.
- **Database**: PostgreSQL `pickups` table (`V6__create_pickups_and_volunteer_assignment.sql`).
- **REST Endpoints**: `POST /api/v1/admin/pickups`, `GET /api/v1/admin/pickups`, `GET /api/v1/admin/pickups/{pickupId}`.
- **Automated Test Evidence**: Tests in `PickupServiceTest.java` verifying volunteer status transition to `BUSY`, scheduling timestamp persistence, and OTP token generation.

### Module 10: Cryptographically Secure OTP Handover Verification
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Volunteer Verify OTP Screen (`mobile/lib/screens/volunteer/`).
- **Backend**: `PickupOtpService.java`, `VolunteerPickupController.java`, `PickupService.verifyOtp()`.
- **Database**: `pickups.otp_hash` (BCrypt string), `pickups.otp_expires_at`, `pickups.otp_attempts`.
- **REST Endpoints**: `POST /api/v1/volunteer/pickups/{pickupId}/verify-otp`.
- **Automated Test Evidence**: 12 tests in `PickupSecurityAndApiTest.java` and 23 tests in `PickupServiceTest.java` verifying BCrypt match, rate limiting (5 attempts lockout), and 10-minute expiry.

### Module 11: Pickup Delivery Completion & Lifecycle Cascading
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Complete Pickup Action, React Admin Pickup Detail View.
- **Backend**: `VolunteerPickupController.java`, `PickupService.completePickup()`.
- **Database**: `pickups.status = 'COMPLETED'`, `pickups.completed_at`.
- **REST Endpoints**: `POST /api/v1/volunteer/pickups/{pickupId}/complete`.
- **Automated Test Evidence**: Verified in `PickupServiceTest.java` confirming cascade updates across `pickups`, `donation_requests`, and `donations`.

### Module 12: Decoupled Operational Notification Pipeline
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: Flutter Notification Drawer (`mobile/lib/models/app_notification.dart`).
- **Backend**: `NotificationEventListener.java`, `NotificationController.java`, 9 Domain Event Records in `com.mealshare.notification.event.*`.
- **Database**: PostgreSQL `notifications` table (`V7__create_notifications.sql`), `user_device_tokens` table.
- **REST Endpoints**: 6 endpoints (`GET /notifications`, `GET /unread-count`, `PATCH /{id}/read`, `POST /read-all`, `POST /device-token`, `DELETE /device-token`).
- **Automated Test Evidence**: 6 tests in `NotificationEventListenerTest.java` and Flutter widget test `notification_test.dart` (3 tests passed).

### Module 13: Append-Only Audit Trail & Traceability
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: React Admin Audit Logs Page (`admin-web/src/pages/AuditLogs.tsx`).
- **Backend**: `AdminAuditLogController.java`, `AuditLogService.java`.
- **Database**: PostgreSQL `audit_logs` table (`V2`), capturing actor UID, role, action, target entity, client IP, user-agent, and JSONB `delta_state`.
- **REST Endpoints**: `GET /api/v1/admin/audit-logs`.
- **Automated Test Evidence**: `AdminAuditLogControllerTest.java` verifying log creation upon entity mutations and structured filtering.

### Module 14: Institutional Receiver Verification Queue
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: React Admin Verification Page (`admin-web/src/pages/Verification.tsx`).
- **Backend**: `AdminVerificationController.java`, `AdminVerificationService.java`.
- **Database**: `receiver_organizations.verification_status` (`PENDING`, `APPROVED`, `REJECTED`).
- **REST Endpoints**: `GET /api/v1/admin/verification`, `GET /api/v1/admin/verification/{id}`, `PATCH /api/v1/admin/verification/{id}/status`.
- **Automated Test Evidence**: Unit tests confirming that approving an organization cascades their account status to `ACTIVE`.

### Module 15: Operational Analytics & Dashboard Reporting
- **Implementation Status**: **COMPLETE & VERIFIED**
- **Frontend**: React Admin Dashboard (`admin-web/src/pages/Dashboard.tsx`), Reports Page (`admin-web/src/pages/Reports.tsx`).
- **Backend**: `AdminDashboardController.java`, `AdminReportController.java`.
- **Database**: Aggregated SQL queries over `donations`, `pickups`, `users`, and `donation_requests`.
- **REST Endpoints**: `GET /api/v1/admin/dashboard`, `GET /api/v1/admin/reports/summary?timeRange=7d`.
- **Automated Test Evidence**: Controller tests verifying aggregated counts, volume trends, and time-range metrics.

---

## 3. Summary of Concrete Implementation Metrics

| Metric Dimension | Implemented Scope | Verification Artifact |
|:---|:---:|:---|
| **Spring Boot REST Controllers** | 18 Controllers | `backend/src/main/java/com/mealshare/` |
| **Documented REST Endpoints** | 47 Endpoints | `docs/API_INVENTORY.md` |
| **Relational & Spatial Tables** | 10 Database Tables | PostgreSQL `mealshare_app` via Docker (Port 5433) |
| **Flyway Schema Migrations** | 7 Versioned Migrations (V1–V7) | `backend/src/main/resources/db/migration/` |
| **React 18 Admin Web Pages** | 10 Management & Analytics Pages | `admin-web/src/pages/` |
| **Flutter Mobile Screens** | 17 Role-Specific Screens | `mobile/lib/screens/` |
| **Automated Backend Tests** | 204 Tests Passing (0 Failures) | `.\mvnw.cmd test` (Build Success) |
| **Frontend Production Build** | 2,517 Modules Transformed (0 TS Errors) | `npm run build` in `admin-web/` |
| **Flutter Static Analysis & Tests** | 0 Lint/Type Errors, 6/6 Tests Passing | `flutter analyze` & `flutter test` |

**Conclusion**: MealShare delivers complete, verifiable software implementations across all 15 functional domains, easily satisfying and surpassing the university's 50% demonstration threshold.

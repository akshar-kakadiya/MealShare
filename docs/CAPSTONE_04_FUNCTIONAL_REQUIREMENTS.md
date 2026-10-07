# MealShare Functional Requirements Specification

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Scope & Role Classification

This document details the functional requirements implemented in the MealShare platform across its four distinct user roles and central system services. Every listed requirement corresponds to verified controllers, services, database tables, and user interfaces in the codebase.

```
       ┌────────────────────────────────────────────────────────┐
       │                   MEALSHARE ACTORS                     │
       └───────┬────────────┬──────────────┬────────────┬───────┘
               │            │              │            │
               ▼            ▼              ▼            ▼
           [ DONOR ]   [ RECEIVER ]   [ VOLUNTEER ]  [ ADMIN ]
               │            │              │            │
       ────────┴────────────┴──────────────┴────────────┴────────
                 SPRING BOOT REST API (47 ENDPOINTS)
```

---

## 2. Donor Role Functional Requirements

| Requirement ID | Functional Capability | Detailed Specification | Implemented API Route |
|:---:|:---|:---|:---|
| **FR-DON-01** | **User Registration & Profile Setup** | A verified Firebase user can register as a `DONOR` and establish their corporate or individual profile, including entity name, donor type, address text, and default geographic coordinates (latitude/longitude). | `POST /api/v1/users/register`<br>`PUT /api/v1/donor/profile`<br>`GET /api/v1/donor/profile` |
| **FR-DON-02** | **Surplus Food Donation Creation** | The donor can publish a surplus food listing capturing title, description, food category (from 8 standardized enums), quantity, quantity unit, preparation timestamp, consume-before deadline, pickup time window, and geolocated pickup address. | `POST /api/v1/donor/donations` |
| **FR-DON-03** | **Mandatory Food Safety Declaration** | The system enforces that donors must explicitly accept the food safety declaration (`foodSafetyDeclared = true`) with optional declaration text before listing creation is permitted. | Handled in `DonationService.createDonation()` |
| **FR-DON-04** | **Temporal Validity Constraints** | The system validates that `preparationDateTime <= NOW()`, `consumeBefore > NOW()`, and `pickupAvailableUntil <= consumeBefore`. Listings failing temporal rules are rejected with `HTTP 400 Bad Request`. | Handled in `DonationService.validateTemporalConstraints()` |
| **FR-DON-05** | **Donation Management & Updates** | The donor can view a paginated list of their active and historical donations, inspect details, and update mutable fields (title, description, pickup notes) of an `AVAILABLE` donation. | `GET /api/v1/donor/donations`<br>`GET /api/v1/donor/donations/{id}`<br>`PATCH /api/v1/donor/donations/{id}` |
| **FR-DON-06** | **Donation Cancellation** | The donor can cancel an active `AVAILABLE` donation if food quality deteriorates before pickup, triggering notification dispatch to affected stakeholders. | `POST /api/v1/donor/donations/{id}/cancel` |
| **FR-DON-07** | **Chronological Claim Request Review** | The donor can inspect incoming claim requests submitted by receiver organizations in strict First-Come, First-Served (FCFS) order (`requestedAt ASC`). | `GET /api/v1/donor/donations/{id}/requests` |
| **FR-DON-08** | **Quantity Allocation & Claim Adjudication** | The donor can approve a claim request (atomically incrementing `allocated_quantity` and locking the claim) or reject a request with an explicit rejection reason. | `POST /api/v1/donor/requests/{id}/approve`<br>`POST /api/v1/donor/requests/{id}/reject` |

---

## 3. Receiver Organization Role Functional Requirements

| Requirement ID | Functional Capability | Detailed Specification | Implemented API Route |
|:---:|:---|:---|:---|
| **FR-REC-01** | **Receiver Organization Profile Setup** | A verified Firebase user can register as a `RECEIVER_ORG` and configure organization name, registration number, address, service radius (default 10 km), and beneficiary capacity. | `POST /api/v1/users/register`<br>`PUT /api/v1/receiver/profile`<br>`GET /api/v1/receiver/profile` |
| **FR-REC-02** | **PostGIS Spatial Proximity Discovery** | The receiver can discover available, non-expired donations within their geographical service radius. Proximity is computed database-side using PostGIS `ST_DWithin` and sorted by geodesic distance (`ST_Distance`). | `GET /api/v1/receiver/donations/nearby` |
| **FR-REC-03** | **Donation Detail Inspection** | The receiver can inspect comprehensive details of an available donation, including food category, remaining unallocated quantity, donor entity name, pickup window, and food safety declaration metadata. | `GET /api/v1/receiver/donations/{id}` |
| **FR-REC-04** | **Donation Claim Submission** | The receiver can submit a formal claim request for an available donation, specifying requested quantity and organizational necessity reason. The system enforces active claim uniqueness via partial unique indexing. | `POST /api/v1/receiver/donations/{donationId}/requests` |
| **FR-REC-05** | **Request Tracking & History** | The receiver can view a paginated list of all active and historical claims submitted by their organization, tracking state transitions (`PENDING`, `APPROVED`, `REJECTED`, `WITHDRAWN`, `COMPLETED`). | `GET /api/v1/receiver/requests`<br>`GET /api/v1/receiver/requests/{id}` |
| **FR-REC-06** | **Claim Withdrawal** | The receiver can withdraw an active claim while it remains in `PENDING` status prior to donor adjudication. | `POST /api/v1/receiver/requests/{id}/withdraw` |

---

## 4. Volunteer Role Functional Requirements

| Requirement ID | Functional Capability | Detailed Specification | Implemented API Route |
|:---:|:---|:---|:---|
| **FR-VOL-01** | **Volunteer Profile & Availability** | A registered volunteer (`RECEIVER_VOL`) maintains profile details and availability status (`AVAILABLE`, `BUSY`, `INACTIVE`), which governs their eligibility for task assignment. | Configured via `volunteer_profiles` and user registration |
| **FR-VOL-02** | **Assigned Pickup Task List** | The volunteer can view a paginated dashboard of delivery tasks specifically assigned to them by administrators. Enforces IDOR ownership protection. | `GET /api/v1/volunteer/pickups` |
| **FR-VOL-03** | **Pickup Detail & Location Guidance** | The volunteer can inspect full details of an assigned pickup, including scheduled collection time, donor pickup address, geocoordinates, receiver destination, and food quantity. | `GET /api/v1/volunteer/pickups/{pickupId}` |
| **FR-VOL-04** | **Cryptographic OTP Handover Verification** | Upon arriving at the donor's facility, the volunteer verifies the physical collection by submitting the donor's 6-digit OTP code. The backend compares against the BCrypt hash under pessimistic locking, enforcing rate limiting and 10-minute expiry. | `POST /api/v1/volunteer/pickups/{pickupId}/verify-otp` |
| **FR-VOL-05** | **Delivery Completion Confirmation** | Upon delivering the food to the receiver organization's facility, the volunteer marks the pickup as complete, transitioning the pickup, request, and donation to `COMPLETED` status. | `POST /api/v1/volunteer/pickups/{pickupId}/complete` |

---

## 5. Administrator Role Functional Requirements

| Requirement ID | Functional Capability | Detailed Specification | Implemented API Route |
|:---:|:---|:---|:---|
| **FR-ADM-01** | **Operational KPI Dashboard** | The administrator can view real-time platform statistics, including active donation counters, pending claims, ongoing deliveries, completed meals redistributed, and 7-day activity feeds. | `GET /api/v1/admin/dashboard` |
| **FR-ADM-02** | **User Account Oversight & Adjudication** | The administrator can inspect all user accounts across roles, search by name/email, and adjudicate account status (`ACTIVE`, `RESTRICTED`, `SUSPENDED`). Suspended users are immediately blocked from API access. | `GET /api/v1/admin/users`<br>`GET /api/v1/admin/users/{id}`<br>`PATCH /api/v1/admin/users/{id}/status` |
| **FR-ADM-03** | **Institutional Receiver Verification Queue** | The administrator inspects receiver organization registration numbers, legal credentials, and physical addresses, approving or rejecting organizations before they can claim food. | `GET /api/v1/admin/verification`<br>`GET /api/v1/admin/verification/{id}`<br>`PATCH /api/v1/admin/verification/{id}/status` |
| **FR-ADM-04** | **Platform Donation & Request Oversight** | The administrator maintains global read oversight over all platform donations, claim requests, remaining allocation balances, and status histories. | `GET /api/v1/admin/donations`<br>`GET /api/v1/admin/donations/{id}`<br>`GET /api/v1/admin/requests`<br>`GET /api/v1/admin/requests/{id}` |
| **FR-ADM-05** | **Volunteer Pickup Assignment** | The administrator assigns an available volunteer (`availability_status = 'AVAILABLE'`) to an approved claim request, generating a 6-digit OTP code shown once to the administrator and issued to the donor. | `POST /api/v1/admin/pickups`<br>`GET /api/v1/admin/pickups`<br>`GET /api/v1/admin/pickups/{pickupId}` |
| **FR-ADM-06** | **Aggregated Impact Reports** | The administrator can generate operational volume reports aggregated by time range (`7d`, `30d`, `90d`, `1y`) showing food categories redistributed and volunteer delivery trends. | `GET /api/v1/admin/reports/summary` |
| **FR-ADM-07** | **Immutable Audit Log Inspection** | The administrator can query, filter, and inspect the system audit trail by actor ID, action type, entity name, and date range. | `GET /api/v1/admin/audit-logs` |

---

## 6. System-Level Functional Requirements

| Requirement ID | Functional Capability | Detailed Specification | Implementation Artifact |
|:---:|:---|:---|:---|
| **FR-SYS-01** | **Firebase Token Verification** | Every incoming HTTP request with `Authorization: Bearer <token>` is validated via the Firebase Admin SDK. | `FirebaseAuthenticationFilter.java` |
| **FR-SYS-02** | **Database-Authoritative RBAC** | User roles and granted authorities are resolved strictly from the PostgreSQL `users` table via `findByFirebaseUid()`; client-supplied token claims are ignored. | `FirebaseAuthenticationFilter.java` |
| **FR-SYS-03** | **PostGIS Spatial Engine** | All spatial entities utilize `GEOMETRY(Point, 4326)` columns backed by GiST indexing, supporting `ST_DWithin` and `ST_Distance` SQL queries. | Flyway `V1__enable_postgis.sql`, `V3__create_donations_table.sql` |
| **FR-SYS-04** | **Decoupled Event Notifications** | The system publishes 9 domain events via `ApplicationEventPublisher`. The listener processes events `AFTER_COMMIT` to generate in-app alerts and prepare FCM payload structures. | `NotificationEventListener.java`, `notifications` table |
| **FR-SYS-05** | **In-App Notification Management** | Authenticated users can retrieve notifications, query unread counts, mark single alerts as read, and bulk mark all alerts as read. | `NotificationController.java` (6 endpoints) |
| **FR-SYS-06** | **Immutable Audit Logging** | All critical business state mutations automatically trigger append-only audit logging capturing actor, role, action, target entity, client IP, user-agent, and JSONB `delta_state`. | `AuditLogService.java`, `audit_logs` table |

---

## 7. Traceability Matrix

Every listed functional requirement maps directly to implemented source code and passes automated regression tests:
- **Backend API**: 47 Endpoints across 18 REST Controllers.
- **Database**: 10 Tables across 7 Flyway Migrations.
- **Frontend**: 10 React 18 Admin Pages, 17 Flutter Mobile Screens.
- **Automated Tests**: 204 Spring Boot tests, 0 failures.

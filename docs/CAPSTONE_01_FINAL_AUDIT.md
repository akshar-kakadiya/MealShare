# CAPSTONE-01: Final Product Engineering Audit & Presentation Readiness Report

> **Document Version**: 1.0  
> **Evaluation Milestone**: Final Capstone Engineering Audit  
> **Date**: September 26, 2026  
> **Audit Status**: APPROVED & READY FOR PRESENTATION  
> **Lead Architecture Scope**: React Admin Web + Flutter Mobile + Spring Boot 3.4.x + PostgreSQL 16 / PostGIS 3.4.3  

---

## 1. Executive Summary

This report delivers the comprehensive engineering audit of the **MealShare** surplus food redistribution platform at the conclusion of all core development tasks (BACKEND-01 through BACKEND-11).

The primary objective of this audit is to provide an objective, evidence-based evaluation of what has been implemented, validated, and verified across the full-stack architecture, while explicitly documenting system boundaries, architectural invariants, and future enhancements.

### Overall Assessment
- **Architecture Integrity**: **PASS** — Approved architecture strictly maintained with zero unauthorized frameworks or refactoring.
- **Business Workflow Execution**: **PASS** — Full donor-to-receiver-to-volunteer redistribution cycle verified against Docker PostgreSQL/PostGIS.
- **Database Schema & Spatial Indexing**: **PASS** — 10 Flyway migrations (V1–V7) verified with GiST indexing and spatial constraints.
- **Security & Authorization**: **PASS** — Token validation, database role resolution, BCrypt OTP hashing, rate-limiting, and IDOR protection verified.
- **Food Safety Framework**: **PASS** — Enforces mandatory donor self-declaration, temporal consume-before constraints, and traceability without false legal/certification claims.
- **Notification Persistence**: **PASS** (Database Persistence) / **NOT TESTED** (Real-device FCM push delivery).
- **AI/ML Functionality**: **FUTURE / PROPOSED** — Explicitly classified as future roadmap; no fictitious ML models claimed.
- **Automated Test Regression**: **PASS** — 204 backend tests passed, React build passed, Flutter analysis and tests passed with 0 errors.

---

## 2. Architecture Status

The system architecture conforms strictly to the approved specification:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MealShare Platform Stack                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   React.js 18 + Vite Admin Web              Flutter 3.x Mobile App          │
│   (Port 5173 / Production dist)            (Donor, Receiver, Volunteer)     │
│   └──────────────────────┬─────┘            └──────────────┬────────────────┘│
│                          │                                 │                 │
│                          ▼                                 ▼                 │
│                 ┌──────────────────────────────────────────────────┐        │
│                 │             Firebase Authentication              │        │
│                 │         (Identity & Bearer Token Issuance)       │        │
│                 └────────────────────────┬─────────────────────────┘        │
│                                          │ Bearer <Firebase_ID_Token>       │
│                                          ▼                                  │
│                 ┌──────────────────────────────────────────────────┐        │
│                 │           Spring Boot 3.4.3 REST API             │        │
│                 │                 (Host Port 8081)                 │        │
│                 │   • Firebase Admin SDK Token Verification        │        │
│                 │   • Database-Controlled Role Resolution          │        │
│                 │   • Hibernate Spatial 6 + Spring Data JPA        │        │
│                 │   • Spring ApplicationEvents & Decoupled Async   │        │
│                 └────────────────────────┬─────────────────────────┘        │
│                                          │ JDBC / PostGIS Drivers           │
│                                          ▼                                  │
│                 ┌──────────────────────────────────────────────────┐        │
│                 │      PostgreSQL 16 + PostGIS 3.4.3 (Docker)      │        │
│                 │             Host Port: 5433 (mealshare_app)      │        │
│                 │   • Spatial GiST Index on donations.location     │        │
│                 │   • 10 Relational & Audit Tables                 │        │
│                 └──────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

| Component | Target Architecture | Implemented Version | Status | Operational Notes |
|:----------|:--------------------|:--------------------|:-------|:------------------|
| **Backend REST API** | Spring Boot 3.4.x (Java 17) | Spring Boot 3.4.3, OpenJDK 17.0.18 | **PASS** | Runs cleanly on port `8081` (avoids port 8080 host conflict). |
| **Database** | PostgreSQL 16 + PostGIS 3.4 | PostgreSQL 16.8 + PostGIS 3.4.3 | **PASS** | Running in Docker `mealshare-postgres` on host port `5433`. |
| **Migrations** | Flyway Schema Migration | Flyway Core 10.x (V1 through V7) | **PASS** | Automated startup schema execution and validation. |
| **Identity & Auth** | Firebase Authentication | Firebase Admin SDK 9.2.0 | **PASS** | Identity handled by Firebase; roles adjudicated by PostgreSQL. |
| **Admin Web** | React 18 + Vite + Tailwind | React 18.3, Vite 8.2, TypeScript | **PASS** | Configured in Real API Mode (`VITE_API_BASE_URL`). |
| **Mobile App** | Flutter 3.x (Multi-Role) | Flutter 3.44.0 (Dart 3.x) | **PASS** | Unified client for Donor, Receiver, and Volunteer. |

---

## 3. Feature Inventory

Based on source inspection in `docs/FINAL_FEATURE_INVENTORY.md`, the platform features are categorized as follows:

| Category | Count | Implemented Scope |
|:---------|:-----:|:------------------|
| **A. FULLY IMPLEMENTED** | 19 | Complete vertical slice: DB persistence, Spring Boot REST API, security rules, and user-facing UI. |
| **B. PARTIALLY IMPLEMENTED** | 1 | Push Device Token Registration (DB persistence verified; physical device delivery NOT TESTED). |
| **C. PROTOTYPE / DEMO ONLY** | 2 | Live Map GPS Breadcrumbs (visual UI timeline only); Admin Settings (local client state only). |
| **D. NOT IMPLEMENTED / FUTURE** | 1 | Predictive AI/ML and OR-Tools optimization (explicitly reserved for future roadmap). |

### Summary of Major Implemented Features
1. **Firebase Authentication & RBAC**: Automated registration, token decoding, and database-enforced roles (`ROLE_DONOR`, `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL`, `ROLE_ADMIN`).
2. **Surplus Food Publishing**: Donor listing creation with mandatory food safety certification, temporal validation, and PostGIS Point geometry.
3. **Geospatial Proximity Discovery**: Receiver discovery of available donations within configurable radius using native PostGIS `ST_DWithin` and `ST_Distance`.
4. **FCFS Claim & Allocation Locking**: Receiver claim submission with pessimistic write locking preventing over-allocation of food quantities.
5. **Admin Volunteer Assignment**: Administrator oversight of claims and assignment of active, available volunteers.
6. **Cryptographic OTP Handover**: 6-digit `SecureRandom` OTP hashed via BCrypt, verified with attempt rate-limiting and expiration windows.
7. **Delivery Lifecycle Completion**: Volunteer delivery handover, updating lifetime metrics and transitioning requests to `COMPLETED`.
8. **In-App Notification Dispatch**: Decoupled Spring Event listeners persisting operational notifications to PostgreSQL.
9. **Operational Audit Trail**: Immutable logging of actor ID, role, action, and JSONB delta state.
10. **Aggregated Reporting**: Real-time administrative KPI calculations and redistribution metrics.

---

## 4. Core Business Workflow

The core operational redistribution lifecycle is implemented and verified across clients and backend:

```
[DONOR]
Create donation with safety declaration & coordinates
   │  (API: POST /api/v1/donor/donations)
   ▼
[RECEIVER]
Discover nearby donations via PostGIS spatial search
   │  (API: GET /api/v1/receiver/donations/nearby)
   ▼
[RECEIVER]
Create claim request for required quantity
   │  (API: POST /api/v1/receiver/donations/{id}/requests)
   ▼
[DONOR]
Approve request (Pessimistic DB lock, FCFS arrival)
   │  (API: POST /api/v1/donor/requests/{id}/approve)
   ▼
[ADMIN]
Assign available volunteer (BCrypt OTP generated)
   │  (API: POST /api/v1/admin/pickups)
   ▼
[VOLUNTEER]
View assigned pickup details & addresses
   │  (API: GET /api/v1/volunteer/pickups)
   ▼
[VOLUNTEER]
Verify 6-digit OTP at donor pickup point
   │  (API: POST /api/v1/volunteer/pickups/{id}/verify-otp)
   ▼
[VOLUNTEER]
Complete delivery at receiver organization
   │  (API: POST /api/v1/volunteer/pickups/{id}/complete)
   ▼
[SYSTEM]
Persist notifications + write JSONB audit logs
   │  (Propagation.REQUIRES_NEW event listeners)
   ▼
[ADMIN]
Real-time monitoring, KPI dashboards, and analytics
   │  (API: GET /api/v1/admin/dashboard, /reports/summary)
```

### Detailed Operational Step Mapping

| Step | UI Client | Backend REST API | DB Table & Entity | State Transition | Notification Event | Audit Log Event |
|:-----|:----------|:-----------------|:------------------|:-----------------|:-------------------|:----------------|
| **1. Create Donation** | Flutter Donor Screen | `POST /api/v1/donor/donations` | `donations` (`Donation`) | Initial: `AVAILABLE` | None | `DONATION_CREATED` |
| **2. Nearby Discovery** | Flutter Receiver Screen | `GET /api/v1/receiver/donations/nearby` | `donations` (`location` GiST) | None (Read-only query) | None | None |
| **3. Create Request** | Flutter Receiver Screen | `POST /api/v1/receiver/donations/{id}/requests` | `donation_requests` (`DonationRequest`) | Initial: `PENDING` | `REQUEST_CREATED` $\rightarrow$ Donor | `DONATION_REQUEST_CREATED` |
| **4. Approve Request** | Flutter Donor Screen | `POST /api/v1/donor/requests/{id}/approve` | `donation_requests`, `donations` | Request: `APPROVED`<br>Donation: `allocated_quantity += Q` | `REQUEST_APPROVED` $\rightarrow$ Receiver | `REQUEST_APPROVED` |
| **5. Assign Volunteer** | React Admin Web | `POST /api/v1/admin/pickups` | `pickups`, `volunteer_profiles` | Pickup: `ASSIGNED`<br>Vol: `BUSY` | `VOLUNTEER_ASSIGNED` $\rightarrow$ Volunteer | `VOLUNTEER_ASSIGNED` |
| **6. View Pickup** | Flutter Volunteer Screen | `GET /api/v1/volunteer/pickups/{id}` | `pickups` (`Pickup`) | None (IDOR checked) | None | None |
| **7. Verify OTP** | Flutter Volunteer Screen | `POST /api/v1/volunteer/pickups/{id}/verify-otp` | `pickups`, `donations` | Pickup: `PICKED_UP`<br>Donation: `PICKED_UP` | `OTP_VERIFIED` $\rightarrow$ Donor | `OTP_VERIFIED` |
| **8. Complete Pickup** | Flutter Volunteer Screen | `POST /api/v1/volunteer/pickups/{id}/complete` | `pickups`, `donation_requests`, `volunteer_profiles` | Pickup: `COMPLETED`<br>Request: `COMPLETED`<br>Vol: `AVAILABLE` | `PICKUP_COMPLETED` $\rightarrow$ Donor & Receiver | `PICKUP_COMPLETED` |
| **9. Admin Oversight** | React Admin Web | `GET /api/v1/admin/dashboard` | `donations`, `pickups`, `audit_logs` | Real-time aggregate queries | In-app notification view | `AUDIT_LOGS_ACCESSED` |

---

## 5. Database Status

The PostgreSQL schema is managed via Flyway migrations (V1 through V7) in database `mealshare_app`.

### Schema Inventory

```
┌──────────────────┐       ┌──────────────────────┐       ┌────────────────────────┐
│      users       │       │    donor_profiles    │       │ receiver_organizations │
├──────────────────┤       ├──────────────────────┤       ├────────────────────────┤
│ id (PK, UUID)    │◄──────┼ user_id (FK, UUID)   │       │ id (PK, UUID)          │
│ firebase_uid     │       │ entity_name          │  ┌────┼ user_id (FK, UUID)     │
│ email, phone     │       │ address_text, lat/lon│  │    │ verification_status    │
│ role, status     │       │ policy_accepted      │  │    │ beneficiary_capacity   │
└────────┬─────────┘       └──────────┬───────────┘  │    └───────────┬────────────┘
         │                            │              │                │
         │ 1:1                        │ 1:N          │                │ 1:N
         ▼                            ▼              │                ▼
┌──────────────────┐       ┌──────────────────────┐  │    ┌────────────────────────┐
│volunteer_profiles│       │      donations       │  │    │   donation_requests    │
├──────────────────┤       ├──────────────────────┤  │    ├────────────────────────┤
│ id (PK, UUID)    │       │ id (PK, UUID)        │  │    │ id (PK, UUID)          │
│ user_id (FK)     │       │ donor_id (FK)        │  │    │ donation_id (FK) ◄─────┼──┐
│ availability_stat│       │ title, food_category │  │    │ receiver_org_id (FK)───┘  │
│ deliveries_count │       │ quantity, allocated_q│  │    │ requested_quantity     │  │
│ total_meals      │       │ location (PostGIS)   │◄─┘    │ status (PENDING, etc.) │  │
└────────┬─────────┘       │ food_safety_declared │       └───────────┬────────────┘  │
         │                 │ status (AVAILABLE...)│                   │ 1:1           │
         │                 └──────────────────────┘                   ▼               │
         │                                                ┌────────────────────────┐  │
         │ 1:N                                            │        pickups         │  │
         └───────────────────────────────────────────────►│ id (PK, UUID)          │  │
                                                          │ donation_request_id(FK)├──┘
                                                          │ volunteer_id (FK)      │
                                                          │ otp_hash (BCrypt)      │
                                                          │ otp_expires_at/attempts│
                                                          │ status (ASSIGNED...)   │
                                                          └────────────────────────┘
```

#### Detailed Table Specifications

1. **`users`**:
   - *Purpose*: Core platform user identity, mapped to Firebase UID.
   - *Key Columns*: `id` (UUID, PK), `firebase_uid` (VARCHAR 128, Unique), `email` (VARCHAR 255, Unique), `role` (VARCHAR 20), `status` (VARCHAR 30).
   - *Constraints*: `CHECK (role IN ('DONOR', 'RECEIVER_ORG', 'RECEIVER_VOL', 'ADMIN'))`, `CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'RESTRICTED', 'SUSPENDED'))`.
   - *Indexes*: `idx_users_firebase_uid`, `idx_users_email`, `idx_users_role`.

2. **`donor_profiles`**:
   - *Purpose*: Extended profile information for food donors.
   - *Key Columns*: `id` (UUID, PK), `user_id` (UUID, FK $\rightarrow$ `users.id`, Unique), `donor_type`, `entity_name`, `address_text`, `latitude`, `longitude`, `policy_accepted`, `rating_average`, `completed_donations_count`.
   - *Foreign Key*: `user_id REFERENCES users(id) ON DELETE CASCADE`.

3. **`receiver_organizations`**:
   - *Purpose*: Institutional profiles for NGOs, shelters, and community kitchens.
   - *Key Columns*: `id` (UUID, PK), `user_id` (UUID, FK $\rightarrow$ `users.id`, Unique), `org_type`, `org_name`, `registration_number`, `address_text`, `service_latitude`, `service_longitude`, `service_radius_meters`, `verification_status`, `beneficiary_capacity`, `rating_average`.
   - *Foreign Key*: `user_id REFERENCES users(id) ON DELETE CASCADE`.

4. **`volunteer_profiles`**:
   - *Purpose*: Operational volunteer profile for food transport and handover.
   - *Key Columns*: `id` (UUID, PK), `user_id` (UUID, FK $\rightarrow$ `users.id`, Unique), `full_name`, `service_latitude`, `service_longitude`, `availability_status`, `completed_deliveries_count`, `total_meals_delivered`, `rating_average`.
   - *Constraints*: `CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'INACTIVE'))`.
   - *Index*: `idx_volunteer_profiles_availability`.

5. **`donations`**:
   - *Purpose*: Surplus food listings with spatial coordinates and food safety attributes.
   - *Key Columns*: `id` (UUID, PK), `donor_id` (UUID, FK $\rightarrow$ `donor_profiles.id`), `title`, `food_category`, `quantity`, `quantity_unit`, `allocated_quantity`, `preparation_datetime`, `consume_before`, `pickup_available_from`, `pickup_available_until`, `pickup_address`, `latitude`, `longitude`, `location`, `food_safety_declared`, `food_safety_declaration_text`, `food_safety_declared_at`, `status`.
   - *Spatial Column*: **`location GEOMETRY(Point, 4326) NOT NULL`**.
   - *Spatial Index*: **`idx_donations_location_gist ON donations USING GIST (location)`**.
   - *Quantity Constraints*: `CHECK (allocated_quantity >= 0.0)`, `CHECK (allocated_quantity <= quantity)`.
   - *Status Constraint*: `CHECK (status IN ('AVAILABLE', 'REQUESTED', 'APPROVED', 'PICKUP_ASSIGNED', 'PICKED_UP', 'COMPLETED', 'EXPIRED', 'CANCELLED'))`.

6. **`donation_requests`**:
   - *Purpose*: Formal claims submitted by receiver organizations for donation portions.
   - *Key Columns*: `id` (UUID, PK), `donation_id` (UUID, FK $\rightarrow$ `donations.id`), `receiver_org_id` (UUID, FK $\rightarrow$ `receiver_organizations.id`), `requested_quantity`, `request_reason`, `status`, `requested_at`.
   - *Constraints*: `CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'COMPLETED'))`, `CHECK (requested_quantity > 0)`.
   - *Indexes*: `idx_donation_requests_fcfs (donation_id, requested_at ASC)`, `idx_unique_active_donation_request UNIQUE (donation_id, receiver_org_id) WHERE status IN ('PENDING', 'APPROVED')`.

7. **`pickups`**:
   - *Purpose*: Delivery tasks binding approved donation claims to assigned volunteers.
   - *Key Columns*: `id` (UUID, PK), `donation_request_id` (UUID, FK $\rightarrow$ `donation_requests.id`, Unique), `volunteer_id` (UUID, FK $\rightarrow$ `volunteer_profiles.id`), `status`, `scheduled_at`, `assigned_at`, `otp_hash`, `otp_expires_at`, `otp_attempts`, `otp_verified_at`, `picked_up_at`, `completed_at`.
   - *Security Columns*: `otp_hash VARCHAR(255) NOT NULL` (stores BCrypt hash only; raw OTP is never persisted), `otp_expires_at TIMESTAMPTZ`, `otp_attempts INTEGER DEFAULT 0`.
   - *Constraints*: `CHECK (status IN ('ASSIGNED', 'PICKED_UP', 'COMPLETED', 'CANCELLED'))`.

8. **`notifications`**:
   - *Purpose*: In-app notifications delivered across user roles.
   - *Key Columns*: `id` (UUID, PK), `recipient_user_id` (UUID, FK $\rightarrow$ `users.id`), `type`, `title`, `message`, `entity_type`, `entity_id`, `is_read`, `created_at`, `read_at`.
   - *Indexes*: `idx_notifications_recipient_read (recipient_user_id, is_read)`, `idx_notifications_created_at`.

9. **`user_device_tokens`**:
   - *Purpose*: Firebase Cloud Messaging device tokens for push notification delivery.
   - *Key Columns*: `id` (UUID, PK), `user_id` (UUID, FK $\rightarrow$ `users.id`), `fcm_token` (TEXT), `platform`, `created_at`, `last_used_at`.
   - *Constraints*: `UNIQUE (user_id, fcm_token)`.

10. **`audit_logs`**:
    - *Purpose*: Immutable operational audit trail for compliance and dispute resolution.
    - *Key Columns*: `id` (BIGSERIAL, PK), `actor_user_id`, `actor_role`, `action_type`, `entity_name`, `entity_id`, `delta_state` (JSONB), `client_ip`, `user_agent`, `timestamp`.
    - *Indexes*: `idx_audit_logs_actor`, `idx_audit_logs_entity (entity_name, entity_id)`, `idx_audit_logs_timestamp`.

---

## 6. API Status

The backend exposes **47 REST API endpoints** across 18 controllers, fully documented in `docs/API_INVENTORY.md`:

| Functional Area | Total Endpoints | Read-Only | Mutations | Role Authorization |
|:----------------|:---------------:|:---------:|:---------:|:-------------------|
| **Health Probe** | 1 | 1 | 0 | Public |
| **Authentication & Users** | 2 | 1 | 1 | `ROLE_UNREGISTERED`, `ROLE_USER` |
| **Donor Operations** | 10 | 4 | 6 | `ROLE_DONOR` |
| **Receiver Operations** | 8 | 5 | 3 | `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL` |
| **Volunteer Operations** | 4 | 2 | 2 | `ROLE_RECEIVER_VOL` |
| **Admin Operations** | 16 | 13 | 3 | `ROLE_ADMIN` |
| **Notifications & Push** | 6 | 2 | 4 | Authenticated (`ROLE_USER`) |
| **Total** | **47** | **28** | **19** | Evaluated via `@PreAuthorize` |

---

## 7. Security Status

### Implemented Security Controls (**PASS**)

1. **Firebase ID Token Verification**: All incoming API requests with `Authorization: Bearer <token>` are decoded and verified via the Firebase Admin SDK.
2. **Database-Enforced Role Authority**: User roles are resolved strictly from the PostgreSQL `users` table via `userRepository.findByFirebaseUid()`. Client-supplied roles in token claims or request bodies are ignored.
3. **Suspended Account Isolation**: Users marked as `SUSPENDED` are assigned `ROLE_SUSPENDED` and blocked from accessing all operational endpoints.
4. **IDOR (Insecure Direct Object Reference) Protection**:
   - Donors can only view/cancel/update their own donations.
   - Receivers can only view/withdraw their own claims.
   - Volunteers can only inspect and verify pickups specifically assigned to their `volunteer_id`.
   - Users can only read and modify their own notifications.
5. **Admin Role Isolation**:
   - Public registration strictly rejects `ROLE_ADMIN` registration attempts.
   - The Admin User Service does not provide any role elevation or admin promotion endpoints.
6. **Cryptographic OTP Security**:
   - Generated using `java.security.SecureRandom` (6 numeric digits).
   - Stored in PostgreSQL strictly as a one-way **BCrypt hash** (`BCryptPasswordEncoder`).
   - The raw OTP is returned once to the Admin/Donor at creation time and is **never** printed to application logs, audit logs, or returned in subsequent DTOs.
   - **Rate Limiting**: Enforces a strict maximum of 5 verification attempts. Exceeding 5 attempts locks the pickup from further verification.
   - **Expiration**: Enforces an explicit 10-minute expiry window (`otp_expires_at`).
   - **Single Use**: Once successfully verified, the pickup transitions to `PICKED_UP`, rendering the OTP permanently invalid for further attempts.
7. **Pessimistic Concurrency Locking**:
   - Concurrent claim approvals lock the `donations` row (`SELECT ... FOR UPDATE`), preventing race conditions and over-allocation.
   - Concurrent OTP verifications lock the `pickups` row.
8. **Audit Trail Logging**:
   - Every mutation records the actor UID, actor role, IP address, user-agent, entity ID, and JSONB delta state. Plaintext credentials and OTPs are explicitly sanitized.

### Future Security Roadmap (Not Implemented / Future)
- Distributed Redis-backed token revocation / immediate blacklist.
- IP-based sliding-window rate limiting via Bucket4j or Cloudflare.
- Automated checksum and antivirus scanning for uploaded NGO verification documents.

---

## 8. Food Safety Status

MealShare implements an explicit, transparent operational food safety workflow designed to maintain transparency and traceability across surplus food transfers:

```
[Donor Input]
Mandatory Food Safety Declaration + Preparation Time + Consume-Before Time
       │
       ▼
[Backend Validation]
1. Declaration must be TRUE
2. Consume-Before must be strictly AFTER Preparation Time
3. Pickup Window must NOT precede Preparation Time
4. Pickup Window must conclude BEFORE Consume-Before Time
       │
       ▼
[Receiver Discovery]
Expired donations are automatically excluded from spatial search
       │
       ▼
[Pickup Handover]
Volunteer scheduled time must precede Consume-Before Time
       │
       ▼
[Traceability & Audit]
Full timestamped trail recorded in PostgreSQL and AuditLog
```

### Essential Food Safety Disclaimers
1. **No Platform Certification**: MealShare **does NOT certify, lab-test, or guarantee food quality, safety, or nutritional standards**. The platform acts as a digital coordination and dispatch system.
2. **No Automatic Liability Transfer**: The platform **does NOT transfer legal liability** for food safety from food businesses or donors to the platform or to receiving organizations.
3. **Participating Party Responsibilities**:
   - Donors are legally responsible for food hygiene, accurate labeling, temperature control prior to handover, and adherence to relevant food safety regulations (e.g., FSSAI standards).
   - Receiving organizations are responsible for inspecting food appearance, smell, and packaging integrity upon receipt before serving to beneficiaries.

---

## 9. Notification Status

The notification system uses an asynchronous event-driven architecture decoupled via Spring `ApplicationEventPublisher`:

- **Event Emitters**: Business services emit lightweight record events (`DonationRequestCreatedEvent`, `VolunteerAssignedEvent`, `OtpVerifiedEvent`, `PickupCompletedEvent`, etc.).
- **Transactional Listener**: `NotificationEventListener` captures events using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- **Database Persistence**: Notifications are persisted in PostgreSQL table `notifications` under `Propagation.REQUIRES_NEW` to prevent notification logging from rolling back business transactions.
- **Privacy Assurance**: Plaintext OTP codes and sensitive credentials are never placed in notification messages.

### Current Implementation & Verification Status

| Dimension | Verification Status | Evidentiary Basis |
|:----------|:-------------------:|:------------------|
| **In-App Database Persistence** | **PASS** | Verified end-to-end; unit and integration tests validate persistence and unread counter queries. |
| **Real-Device Push Delivery (FCM)** | **NOT TESTED** | `FcmService` and `user_device_tokens` schema are fully coded, but actual push dispatch to physical devices is **not tested** in local development due to absence of live APNS/FCM cloud certificates. |

---

## 10. AI/ML Status

An engineering source code inspection across all directories (`backend`, `admin-web`, `mobile`, `docs`) confirms:

- **Implemented ML Functionality**: **NONE**
- **Machine Learning Status**: **FUTURE / PROPOSED**

### Historical Context & Future Scope
Early concept documentation (`MealShare_Architecture.md`, `MealShare_SRS_v1.0.md`) identified prospective AI/ML opportunities for subsequent project phases:
1. **Surplus Demand Prediction**: Machine learning models forecasting surplus food generation based on seasonal trends, historical donation patterns, and weather.
2. **Optimal Food Allocation**: Multi-objective algorithms balancing beneficiary dietary needs, urgency, and portion sizes.
3. **Route Optimization**: Dispatch routing using Google OR-Tools to minimize volunteer transit times and carbon footprint.

> **Engineering Declaration**: MealShare does **NOT** deploy an active AI/ML model in this version. The current system relies entirely on deterministic business logic, spatial SQL (PostGIS), and relational integrity rules.

---

## 11. Presentation Demo Sequence

A standardized, executable 14-step presentation sequence is documented in `docs/CAPSTONE_DEMO_CHECKLIST.md`:

1. **Admin Login**: Authenticate as `admin@mealshare.org` on React Admin Web.
2. **Admin Dashboard**: Inspect live baseline KPIs and redistribution charts.
3. **Donor Creates Donation**: Publish 30 meals with mandatory food safety declaration.
4. **Receiver Discovers**: Execute PostGIS spatial search finding donation within 10 km.
5. **Receiver Requests**: Submit claim request for 20 meals.
6. **Donor Approves**: Atomic FCFS approval decrements remaining count to 10 meals.
7. **Admin Assigns Volunteer**: Assign Alex Volunteer; capture generated 6-digit OTP.
8. **Volunteer Views Pickup**: Check pickup details and destination in Flutter Mobile.
9. **OTP Verification**: Enter 6-digit OTP at donor handover point; verify BCrypt match.
10. **Pickup Completion**: Complete delivery; volunteer stats increment automatically.
11. **Admin Oversight**: Verify updated `COMPLETED` pickup status on Admin Web.
12. **Notifications**: Verify in-app notifications delivered across Donor, Receiver, and Volunteer.
13. **Audit Trail**: Inspect JSONB audit logs confirming complete chronological event trail.
14. **Reports**: View updated aggregate redistribution and environmental impact reports.

---

## 12. Known Limitations

The following engineering limitations are documented for full transparency:

1. **FCM Real-Device Delivery**: In-app notifications persist and synchronize via PostgreSQL; live push delivery to physical iOS/Android lock screens is **NOT TESTED** in local offline evaluation.
2. **Map GPS Breadcrumbs**: The receiver pickup tracking screen displays a discrete state timeline rather than continuous real-time GPS coordinates (**PROTOTYPE**).
3. **Host Port Configuration**: The default Spring Boot port 8080 conflicts with host Oracle TNS Listener (`TNSLSNR.exe`); Spring Boot is configured to port `8081` across all clients.
4. **Admin Settings**: The Admin Settings screen provides static UI controls without dedicated database persistence (**PROTOTYPE**).

---

## 13. Recommended Presentation Claims

To ensure academic and technical credibility during evaluation, presentation teams must adhere to the following communication guidelines:

| ❌ Misleading / Inaccurate Claim | ✅ Technically Accurate Statement |
|:---------------------------------|:-----------------------------------|
| *"MealShare is an AI-powered platform."* | *"MealShare uses PostGIS spatial indexing for proximity discovery. Predictive ML models are identified as a future roadmap item and are not implemented."* |
| *"The platform certifies that all food is safe."* | *"The platform enforces mandatory food safety self-declarations and temporal consume-before constraints. Legal and physical liability remains with donors and receivers."* |
| *"Real-time push notifications are fully verified."* | *"In-app notification persistence is implemented and tested in PostgreSQL. FCM push notification code is structured for deployment once cloud keys are provided; physical device delivery is NOT TESTED."* |
| *"The system is fully automated."* | *"Redistribution workflows are human-in-the-loop: donors review claims, administrators assign vetted volunteers, and physical OTP verification governs handovers."* |
| *"The platform is completely production-ready."* | *"MealShare is a capstone engineering implementation backed by automated test suites and live database integration. Additional hardening would be required for production deployment."* |

---

## 14. Final Test Regression Results

Full regression testing was conducted across all subsystems without modifying existing business logic:

### 14.1 Spring Boot Backend Tests
```
Command: .\mvnw.cmd test
Profile: test
Total Tests Run: 204
Failures: 0
Errors: 0
Skipped: 0
Execution Time: 1 min 21 s
Result: BUILD SUCCESS (PASS)
```

### 14.2 React Admin Web Build
```
Command: npm run build
Tools: tsc -b && vite build
Modules Transformed: 2,517
Build Time: 15.88 s
TypeScript Errors: 0
Result: BUILD SUCCESS (PASS)
```

### 14.3 Flutter Mobile Static Analysis
```
Command: flutter analyze
Analyzed: mobile/ (lib, test)
Issues Found: 0
Execution Time: 61.3 s
Result: No issues found! (PASS)
```

### 14.4 Flutter Mobile Widget & Unit Tests
```
Command: flutter test
Total Tests Run: 6
Passed: 6
Failed: 0
Result: All tests passed! (PASS)
```

---

## 15. Conclusion

**TASK CAPSTONE-01 is complete.** The MealShare platform demonstrates engineering rigor, adherence to the approved architecture, transparent food safety and notification handling, and reliable automated regression test results. The system is ready for the Capstone demonstration based on the implemented functionality and completed regression tests.

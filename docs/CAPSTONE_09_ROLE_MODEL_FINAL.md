# MEALSHARE — CAPSTONE-09: ROLE MODEL FINAL & ARCHITECTURAL CONSISTENCY REPORT

**Document Version:** 1.0  
**Date:** October 6, 2026  
**Milestone:** CAPSTONE-09  
**Status:** COMPLETE & VERIFIED  

---

## 1. Executive Summary & Final Role Model

The MealShare redistribution platform has transitioned to a strictly **three-role operational model**. The concept of an independent volunteer intermediary has been eliminated. The operational model recognizes that physical food collection and delivery is carried out directly by the claiming recipient or their internal logistics.

### Final Operational Roles:
1. **DONOR**: Surplus food provider (Hotels, Restaurants, Messes, Caterers, Households, Individual Citizens).
2. **RECEIVER**: Surplus food claimant and recipient (Registered NGOs, Shelters, Community Centers, Individual Beneficiaries).
3. **ADMIN**: Platform governance, receiver verification, dispute adjudication, and pickup dispatching.

No fourth user role exists anywhere in the runtime codebase, database constraints, API route definitions, mobile interface, or web administrative console.

---

## 2. Authentication & Authorization Architecture

MealShare enforces a zero-trust, token-based identity architecture combining Firebase Authentication and Spring Security:

```
[Flutter / React Client]
       │
       ▼ (1) Firebase Auth ID Token (JWT)
[Spring Boot: FirebaseAuthenticationFilter]
       │
       ▼ (2) Verify Token Signature & Claims (Firebase Admin SDK)
       │
       ▼ (3) Database User Lookup by firebase_uid (PostgreSQL)
       │
       ▼ (4) Role Assignment:
             • ROLE_DONOR
             • ROLE_RECEIVER
             • ROLE_ADMIN
             • ROLE_UNREGISTERED (if UID not found in DB)
             • ROLE_SUSPENDED    (if DB status is SUSPENDED)
       │
       ▼ (5) Controller Security Enforcement:
             • @PreAuthorize("hasRole('DONOR')")
             • @PreAuthorize("hasRole('RECEIVER')")
             • @PreAuthorize("hasRole('ADMIN')")
```

### Key Security Invariants:
* **Anti-Spoofing Registration**: Public registration (`POST /api/v1/users/register`) strictly validates that `request.email` matches the verified email claim inside the Firebase ID token.
* **Role Privilege Escalation Prevention**: Public registration permits only `DONOR` and `RECEIVER`. Any attempt to register `ADMIN` is rejected with `IllegalArgumentException` ("ADMIN role cannot be publicly registered").
* **Suspension Enforcement**: Suspended users are demoted to `ROLE_SUSPENDED` in `FirebaseAuthenticationFilter`, denying access to all operational endpoints.
* **Dead Endpoint Elimination**: Legacy `/api/v1/volunteer/**` endpoints do not exist in controller mappings; unauthenticated requests return `401 Unauthorized`, and authenticated requests return `404 Not Found`.

---

## 3. Direct Pickup Ownership & Custody Handover Workflow

With the elimination of third-party volunteers, the pickup lifecycle is directly owned by the claiming Receiver Organization:

```mermaid
sequenceDiagram
    autonumber
    actor Donor as Donor (Flutter)
    actor Receiver as Receiver (Flutter)
    actor Admin as Admin (React Web)
    participant API as Spring Boot API
    participant DB as PostgreSQL 16.8 (PostGIS)

    Donor->>API: POST /api/v1/donor/donations (Publishes food, e.g. 40 meals)
    Receiver->>API: GET /api/v1/receiver/donations/nearby (PostGIS spatial discovery)
    Receiver->>API: POST /api/v1/receiver/donations/{id}/requests (Claims 15 meals)
    Donor->>API: POST /api/v1/donor/requests/{id}/approve (Allocates 15 meals)
    Note over Donor,API: Remaining surplus = 25 meals; Status = AVAILABLE
    Admin->>API: POST /api/v1/admin/pickups (Schedules pickup for claiming Receiver)
    API->>DB: Generates 6-digit BCrypt OTP, Status = ASSIGNED
    Receiver->>API: GET /api/v1/receiver/pickups (Views scheduled collection)
    Note over Receiver,Donor: Receiver arrives at Donor pickup address
    Donor-->>Receiver: Provides 6-digit OTP displayed on Donor screen
    Receiver->>API: POST /api/v1/receiver/pickups/{id}/verify-otp
    API->>DB: Validates BCrypt hash, Status -> PICKED_UP
    Receiver->>API: POST /api/v1/receiver/pickups/{id}/complete
    API->>DB: Status -> COMPLETED (Request & Donation updated)
```

### Handover Cryptographic Invariants:
* **Secure OTP Generation**: 6-digit random code generated via `SecureRandom`.
* **One-Way Hash Storage**: Only the BCrypt hash is persisted in the database; plaintext is never stored.
* **Replay Protection**: Verifying OTP on a pickup already in `PICKED_UP` or `COMPLETED` state fails immediately with `HTTP 409 Conflict`.
* **IDOR Protection**: Only the specific Receiver associated with `pickups.receiver_id` can query or verify the pickup. Requests from other receivers return `HTTP 403 Forbidden`.

---

## 4. Database Schema Reconciliation (Flyway V9)

The relational schema has been reconciled to enforce the three-role model at the database level:

### Active Database Migrations:
* **`V1__enable_postgis.sql`**: PostGIS spatial extension.
* **`V2__create_foundation_tables.sql`**: Base user, donor, and receiver tables (historical).
* **`V3__create_donations_table.sql`**: Surplus donations with `GEOMETRY(Point, 4326)`.
* **`V4__create_donation_requests_table.sql`**: First-come-first-served claim queue.
* **`V5__add_donation_allocation.sql`**: Fractional allocation tracking.
* **`V6__create_pickups_and_volunteer_assignment.sql`**: Historical pickup table.
* **`V7__create_notifications.sql`**: In-app notifications & device tokens.
* **`V8__add_request_allocated_quantity.sql`**: Request-level allocation tracking.
* **`V9__remove_volunteer_role_and_reconcile_receiver.sql`** (*Active Canonical State*):
  1. Updates `users.role` check constraint:
     ```sql
     ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
     ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('DONOR', 'RECEIVER', 'ADMIN'));
     ```
  2. Drops legacy `volunteer_profiles` table and associated foreign keys.
  3. Alters `pickups` table: replaces `volunteer_id` with `receiver_id UUID NOT NULL REFERENCES receiver_organizations(id) ON DELETE CASCADE`.
  4. Migrates legacy role identifiers: `RECEIVER_ORG` and `RECEIVER_VOL` mapped to `RECEIVER`.

---

## 5. API Endpoint Reconciliation

| Method | Endpoint | Authorized Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/users/register` | Authenticated (No Role) | Self-registration (DONOR or RECEIVER only) |
| `GET` | `/api/v1/users/me` | Registered User | Returns current authenticated profile and role |
| `POST` | `/api/v1/donor/donations` | `DONOR` | Publish surplus food donation |
| `GET` | `/api/v1/donor/donations` | `DONOR` | List donor's own donations |
| `POST` | `/api/v1/donor/donations/{id}/cancel` | `DONOR` | Cancel available donation |
| `GET` | `/api/v1/donor/requests` | `DONOR` | Review incoming claim requests |
| `POST` | `/api/v1/donor/requests/{id}/approve` | `DONOR` | Approve claim and allocate quantity |
| `POST` | `/api/v1/donor/requests/{id}/reject` | `DONOR` | Reject claim with reason |
| `GET` | `/api/v1/receiver/donations/nearby` | `RECEIVER` | PostGIS spatial discovery within radius |
| `POST` | `/api/v1/receiver/donations/{id}/requests` | `RECEIVER` | Submit claim request |
| `GET` | `/api/v1/receiver/requests` | `RECEIVER` | List receiver's own claim requests |
| `POST` | `/api/v1/receiver/requests/{id}/withdraw` | `RECEIVER` | Withdraw pending request |
| `GET` | `/api/v1/receiver/pickups` | `RECEIVER` | List scheduled pickups assigned to receiver |
| `GET` | `/api/v1/receiver/pickups/{id}` | `RECEIVER` | Get pickup details with IDOR check |
| `POST` | `/api/v1/receiver/pickups/{id}/verify-otp` | `RECEIVER` | Verify handover OTP (transitions to `PICKED_UP`) |
| `POST` | `/api/v1/receiver/pickups/{id}/complete` | `RECEIVER` | Complete collection (transitions to `COMPLETED`) |
| `POST` | `/api/v1/admin/pickups` | `ADMIN` | Schedule pickup for claiming receiver |
| `GET` | `/api/v1/admin/pickups` | `ADMIN` | List and monitor all pickups across platform |
| `GET` | `/api/v1/admin/dashboard` | `ADMIN` | Platform metrics (Donors, Receivers, Admins) |
| `GET` | `/api/v1/admin/reports/summary` | `ADMIN` | Redistribution audit summary |

---

## 6. Client Application Reconciliation

### Flutter Mobile Client:
* **Role Options**: Public registration dropdown contains strictly **"Food Donor"** and **"Food Receiver"**.
* **Role Routing**: `auth_service.dart` defines `enum UserRole { donor, receiver, admin }`. Login routes `receiver` to `/receiver/home`.
* **Direct Receiver Collection**: `receiver_pickups_screen.dart` provides full pickup tracking, OTP verification modal, and delivery confirmation dialog.
* **Purged Legacy Artifacts**: Completely deleted `volunteer_home_screen.dart` and removed `/volunteer` routes from `router.dart` and `app_router.dart`. Removed legacy volunteer endpoint aliases from `api_config.dart`.

### React Admin Web Client:
* **User Management (`UsersPage.tsx`)**: Tabs strictly display `All`, `DONOR`, `RECEIVER`, `ADMIN`.
* **Pickup Logistics (`PickupsPage.tsx`)**: "Schedule Food Pickup" modal targets the claiming Receiver directly. The table displays Donor $\rightarrow$ Receiver routes.
* **Analytics (`DashboardPage.tsx`, `ReportsPage.tsx`)**: Displays Receiver Organization counts; purged all volunteer counters.
* **Status Badges (`StatusBadge.tsx`)**: Role badges reflect only `DONOR`, `RECEIVER`, and `ADMIN`.

---

## 7. Security & IDOR Verification

The live E2E automated test suite (`execute_live_workflow.py`) verified complete security compliance:

1. **Unauthenticated Access**: `GET /api/v1/receiver/pickups` without a Bearer token returns `HTTP 401 Unauthorized`.
2. **Cross-Role Access**: Donor attempting to call Admin endpoint (`GET /api/v1/admin/dashboard`) returns `HTTP 403 Forbidden`.
3. **Cross-Role Access**: Receiver attempting to call Donor endpoint (`GET /api/v1/donor/donations`) returns `HTTP 403 Forbidden`.
4. **IDOR Prevention**: Receiver A attempting to inspect a pickup assigned to Receiver B returns `HTTP 403 Forbidden`.
5. **OTP Validation**: Submitting an incorrect OTP code (`000000`) returns `HTTP 409 Conflict`.
6. **Replay Prevention**: Re-submitting the valid OTP after food collection returns `HTTP 409 Conflict`.

---

## 8. Verification & Test Execution Results

All four layers of the test pyramid were executed and confirmed passing:

| Test Suite | Command | Result | Status |
|---|---|---|---|
| **Spring Boot Backend** | `.\mvnw.cmd test` | 211 tests run, 0 failures, 0 errors | **PASS** |
| **React Admin Web** | `npm run build` | 2,518 modules transformed, 0 errors | **PASS** |
| **Flutter Analysis** | `flutter analyze` | No issues found | **PASS** |
| **Flutter Unit Tests** | `flutter test` | 6 tests passed, 0 failures | **PASS** |
| **Live E2E Integration** | `python execute_live_workflow.py` | 13/13 phases passed against live Docker DB & Spring Boot | **PASS** |

---

## 9. Legacy Reference Audit & Transparency

A repository-wide search for "volunteer" was conducted:
1. **Flyway Historical Migrations (`V2`, `V4`, `V6`)**: Contain historical SQL definitions preserved strictly to maintain Flyway checksum integrity. Migration `V9` supersedes and eliminates the volunteer schema at runtime.
2. **Flutter Material Icons (`Icons.volunteer_activism`)**: Flutter SDK Material Design icon name used to visually represent charity donations and meal giving. It is not an application role or user type.
3. **Operational Runtime Code**: Zero operational code references exist in `backend/src/main`, `admin-web/src`, or `mobile/lib`.

---

## 10. Operational Demonstration Readiness

The MealShare platform is fully operational for live capstone demonstration:
* **Docker Services**: PostgreSQL 16.8 + PostGIS on port 5433, Redis on 6379, MinIO on 9000/9001.
* **Spring Boot API**: Running on port 8081 with 9 validated Flyway migrations.
* **Firebase Authentication**: Live ID token authentication active.
* **Client Interfaces**: React Admin web console and Flutter mobile apps operational without mock data dependencies.

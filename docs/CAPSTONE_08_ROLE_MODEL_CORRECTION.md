# MealShare — Capstone-08: Critical Role Model Correction Report

**Date:** October 6, 2026  
**Milestone:** CAPSTONE-08 / PRODUCT ARCHITECTURE CORRECTION  
**Status:** COMPLETED & VERIFIED ACROSS FULL STACK  

---

## 1. Executive Summary & Architectural Scope

A mandatory architecture and product model correction has been executed across the MealShare platform. The platform now implements **STRICTLY THREE APPLICATION ROLES**:

1. **`DONOR`** — Entity or individual contributing surplus food (Hotels, Hostels, Messes, Event Organizers, Caterers, Households, Individual Persons).
2. **`RECEIVER`** — Entity or individual receiving food assistance (NGOs, Community Kitchens, Shelters, Individual Beneficiaries).
3. **`ADMIN`** — Platform operational administrator managing verification, food redistribution safety, and platform oversight.

**Key Rule:** There is **NO separate VOLUNTEER user role**. Pickups are scheduled directly for the claiming **RECEIVER**, who visits the donor location, verifies the secure 6-digit OTP code with the donor upon physical collection, and confirms handover/completion.

---

## 2. Target Role Model & Domain Definitions

| Role | Domain Entities | Capabilities in MealShare | Client UI |
| :--- | :--- | :--- | :--- |
| **`DONOR`** | Hotels, Hostels, Messes, Event Organizers, Caterers, Households, Individuals | Post surplus food with food safety declarations, set pickup timeframes, review FCFS claims, approve/reject requests, provide handover OTP code upon pickup. | Flutter Mobile (`/donor/*`) |
| **`RECEIVER`** | NGOs, Community Shelters, Community Kitchens, Individuals | Search nearby surplus food via PostGIS spatial engine, request quantities, track requests, review scheduled pickups, verify OTP with donor, mark collection complete. | Flutter Mobile (`/receiver/*`) |
| **`ADMIN`** | Platform Administrators | Manage platform users, review receiver organization verification, schedule pickups for approved requests, monitor platform audit trails and reports. | React Web (`/users`, `/pickups`, `/verification`, etc.) |

---

## 3. Database Schema & Migration (Flyway V9)

Flyway migration `V9__remove_volunteer_role_and_reconcile_receiver.sql` reconciles the schema to the 3-role model:

1. **Role Constraint Update:**
   - Dropped legacy constraint `users_role_check`.
   - Migrated legacy records: `UPDATE users SET role = 'RECEIVER' WHERE role IN ('RECEIVER_ORG', 'RECEIVER_VOL')`.
   - Updated audit logs: `UPDATE audit_logs SET actor_role = 'RECEIVER' WHERE actor_role IN ('RECEIVER_ORG', 'RECEIVER_VOL')`.
   - Created new constraint: `ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('DONOR', 'RECEIVER', 'ADMIN'))`.

2. **Pickups Reconciled to Receiver:**
   - Added column: `receiver_id UUID NOT NULL REFERENCES receiver_organizations(id) ON DELETE CASCADE`.
   - Backfilled existing pickups: `receiver_id` populated directly from associated `donation_requests.receiver_org_id`.
   - Dropped column `volunteer_id` and foreign key constraint.
   - Dropped legacy table `volunteer_profiles`.

---

## 4. Backend Spring Boot Implementation

1. **New Controller:** [`ReceiverPickupController`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/backend/src/main/java/com/mealshare/pickup/controller/ReceiverPickupController.java)
   - `GET /api/v1/receiver/pickups` — Returns paginated pickups scheduled for the authenticated receiver.
   - `GET /api/v1/receiver/pickups/{pickupId}` — Detail view with strict IDOR ownership checks.
   - `POST /api/v1/receiver/pickups/{pickupId}/verify-otp` — Verifies 6-digit BCrypt OTP and transitions status to `PICKED_UP`.
   - `POST /api/v1/receiver/pickups/{pickupId}/complete` — Marks delivery/collection complete and transitions status to `COMPLETED`.
   - Enforces `@PreAuthorize("hasRole('RECEIVER')")`.

2. **Admin Pickup Scheduling:**
   - `POST /api/v1/admin/pickups` accepts `AssignPickupRequest` with `{ "donationRequestId": "...", "scheduledAt": "..." }`.
   - Reconciles receiver ID automatically from the approved donation request.

3. **Backend Test Suite:**
   - `.\mvnw.cmd test` passes with **209/209 tests successful (0 failures, 0 errors, 0 skipped)**.

---

## 5. React Admin Web Hardening

1. **User Management:**
   - Role filter tabs updated to strictly: `All`, `DONOR`, `RECEIVER`, `ADMIN`.
2. **Pickup Logistics Management:**
   - Replaced "Assign Volunteer" with "Schedule Food Pickup".
   - Pickups table displays Donor $\rightarrow$ Receiver directly.
   - Detail drawer displays Receiver details and delivery location.
3. **Analytics & Reports:**
   - Replaced "Active Volunteers" stat card with "Active Receivers".
4. **Build Status:**
   - `npm run build` completed with **0 TypeScript/lint errors** (`dist` output generated).

---

## 6. Flutter Mobile Hardening

1. **Authentication Service ([`auth_service.dart`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/mobile/lib/auth/auth_service.dart)):**
   - Reconciled `UserRole` enum: `enum UserRole { donor, receiver, admin }`.
   - Backend string mappings: `DONOR`, `RECEIVER`, `ADMIN`.
   - Automatic session role sync from `GET /api/v1/users/me`.
2. **Registration Screen ([`register_screen.dart`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/mobile/lib/auth/screens/register_screen.dart)):**
   - Cleaned registration role selector to "Food Donor" and "Food Receiver".
   - Subtitle: "Register as a Donor or Receiver".
3. **Login Screen ([`login_screen.dart`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/mobile/lib/auth/screens/login_screen.dart)):**
   - Removed Volunteer demo button; preserved Donor and Receiver demo logins.
4. **Receiver Pickups ([`receiver_pickups_screen.dart`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/mobile/lib/receiver/receiver_pickups_screen.dart)):**
   - Dedicated food collection screen for Receivers.
   - Real-time scheduled pickup inspection, OTP entry dialog, and collection completion.
   - Deprecated `VolunteerHomeScreen` cleanly delegating to `ReceiverPickupsScreen`.
5. **Quality Verification:**
   - `flutter analyze`: **No issues found!**
   - `flutter test`: **All 6 tests passed!**

---

## 7. Live Fresh E2E Workflow Verification (`execute_live_workflow.py`)

A full end-to-end operational workflow was executed against live Docker PostgreSQL (`port 5433`), live Spring Boot (`port 8081`), and genuine Firebase ID tokens:

| Phase | Description | Result | Details |
| :---: | :--- | :---: | :--- |
| **1** | Token Acquisition | **PASS** | Genuine Firebase ID tokens acquired for Donor, Receiver, Admin, and secondary Receiver. |
| **2** | Create Donation | **PASS** | Donor creates 40 meals (`ID: 4bc9fa6d-cff7-4d73-b7d5-1ffdef98deea`, Status: `AVAILABLE`). |
| **3** | Spatial Discovery | **PASS** | Receiver discovers donation via PostGIS spatial search (0.61 km away). |
| **4** | Submit Claim | **PASS** | Receiver claims 15 meals (`ID: c2cdd623-9077-4881-a58f-bf2131a32d6b`, Status: `PENDING`). |
| **5** | Approve Claim | **PASS** | Donor approves claim. Allocated: 15.0, Remaining: 25.0, Status: `AVAILABLE`. |
| **6** | Schedule Pickup | **PASS** | Admin schedules pickup directly for receiver (`ID: e800e612-f991-48b3-bbc7-aec42f327f96`). Secure OTP: `224914`. |
| **7** | View Scheduled Pickup | **PASS** | Receiver views pickup on `/api/v1/receiver/pickups` with status `ASSIGNED`. |
| **8** | IDOR Protection | **PASS** | Unassigned receiver attempting access receives **HTTP 403 Forbidden**. |
| **9** | Wrong OTP Handling | **PASS** | OTP `000000` rejected with **HTTP 409 Conflict** (`Invalid or expired pickup OTP`). |
| **10** | Valid OTP Verification | **PASS** | Receiver submits `224914`. Status transitions to `PICKED_UP`. Timestamp recorded. |
| **11** | Replay Prevention | **PASS** | Re-submitting valid OTP rejected with **HTTP 409 Conflict**. |
| **12** | Complete Pickup | **PASS** | Receiver completes collection. Status transitions to `COMPLETED`. |
| **13** | Negative Auth Checks | **PASS** | Unauthenticated $\rightarrow$ 401; Donor $\rightarrow$ Admin $\rightarrow$ 403; Receiver $\rightarrow$ Donor $\rightarrow$ 403. |

---

## 8. Database Persistence & Audit Verification

Direct SQL inspection of `mealshare_app` database confirms complete persistence:
- **`pickups`:** Status = `COMPLETED`, `receiver_id` = `22222222-2222-2222-2222-222222222222`, `otp_verified_at` and `completed_at` populated.
- **`donations`:** Allocated quantity = 15.0, status = `PICKED_UP`.
- **`donation_requests`:** Status = `COMPLETED`.
- **`notifications`:** Real persisted notifications generated for each event (`REQUEST_CREATED`, `REQUEST_APPROVED`, `PICKUP_SCHEDULED`, `OTP_VERIFIED`, `PICKUP_COMPLETED`).
- **`audit_logs`:** Actor roles strictly `DONOR`, `RECEIVER`, `ADMIN` with native JSONB state records.

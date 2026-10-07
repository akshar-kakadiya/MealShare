# MealShare Fresh End-to-End Business Workflow Report

**Document Version:** 1.0  
**Date:** September 27, 2026  
**Milestone:** Fresh End-to-End Lifecycle Execution & Integrity Audit  
**Target Environment:** Local Docker PostgreSQL (`localhost:5433`) + Spring Boot 3.4.3 (`http://localhost:8081`) + Live Firebase Authentication (`mealshare-100`)

---

## 1. Executive Summary

This report documents the live, automated execution of a complete, unbroken MealShare business lifecycle on the real application architecture without mock fallbacks or simulated data. 

A newly published donation of prepared meals was tracked from initial donor creation through PostGIS spatial discovery, partial allocation approval, volunteer assignment, IDOR security verification, two-phase OTP verification (including wrong attempt counting, correct OTP acceptance, replay prevention, and raw secret secrecy), and final delivery completion.

### High-Level Summary of Execution Results
| Step | Phase | Actor / Endpoint | HTTP Status | Verdict |
|---|---|---|:---:|:---:|
| 1 | Firebase Authentication | 5 Real Actors via Firebase Web SDK | 200 | **PASS** |
| 2 | Donation Creation | Donor &rarr; `POST /donor/donations` | 201 | **PASS** |
| 3 | Spatial Discovery | Receiver &rarr; `GET /receiver/donations/nearby` | 200 | **PASS** |
| 4 | Request Creation | Receiver &rarr; `POST /receiver/donations/{id}/requests` | 201 | **PASS** |
| 5 | Request Approval | Donor &rarr; `POST /donor/requests/{id}/approve` | 200 | **PASS** |
| 6 | Invariant Enforcement | $15.0 \le 15.0$, $15.0 \le 40.0$, $40.0 - 15.0 = 25.0$ | DB & App | **PASS** |
| 7 | Volunteer Assignment | Admin &rarr; `POST /admin/pickups` | 201 | **PASS** |
| 8 | IDOR Protection | Other Volunteer &rarr; `GET /volunteer/pickups/{id}` | 403 | **PASS** |
| 9.1 | Invalid OTP Check | Volunteer &rarr; `POST /volunteer/pickups/{id}/verify-otp` (`999999`) | 409 | **PASS** |
| 9.2 | Valid OTP Verification | Volunteer &rarr; `POST /volunteer/pickups/{id}/verify-otp` (Valid OTP) | 200 | **PASS** |
| 9.3 | OTP Replay Protection | Volunteer &rarr; `POST /volunteer/pickups/{id}/verify-otp` (Replay) | 409 | **PASS** |
| 9.4 | Raw Secret Secrecy | Volunteer &rarr; `GET /volunteer/pickups/{id}` | 200 | **PASS** |
| 10 | Delivery Completion | Volunteer &rarr; `POST /volunteer/pickups/{id}/complete` | 200 | **PASS** |
| 11 | Security Role Checks | Unauth (401), Donor &rarr; Admin (403), Receiver &rarr; Donor (403) | 401 / 403 | **PASS** |
| 12 | Monorepo Regression | Backend (213/213), Admin Web (Build), Mobile (Analyze) | Green | **PASS** |

---

## 2. Actors & Identity Synchronization

All actors were provisioned in Firebase Authentication project `mealshare-100` and synchronized with corresponding rows in PostgreSQL `users`, `donor_profiles`, `receiver_organizations`, and `volunteer_profiles`.

> [!NOTE]
> All credentials below are represented symbolically (`<PASSWORD_REDACTED>`) in compliance with security guidelines.

| Actor Role | Email | User ID (`users.id`) | Firebase UID | Initial Status |
|---|---|---|---|:---:|
| **Donor** | `donor@mealshare.org` | `11111111-1111-1111-1111-111111111111` | `drekxkp9uXV2g16JioDrgu7uXSk2` | `ACTIVE` |
| **Receiver Org** | `receiver@mealshare.org` | `22222222-2222-2222-2222-222222222221` | `pwvnvHgatoahwb9ndKfSM2TVFPG2` | `ACTIVE` |
| **Admin** | `admin@mealshare.org` | `8ff89794-edb1-48c0-94eb-c584815263cb` | `ObiO1gDTdBTZJkzDFEGgTVNTEir2` | `ACTIVE` |
| **Assigned Volunteer** | `volunteer@mealshare.org` | `33333333-3333-3333-3333-333333333331` | `mXSXYZ1nJTf4HfNTDNxRcfrDYVO2` | `ACTIVE` |
| **Other Volunteer** | `other.vol@mealshare.org` | `9dcc4e1b-a1b9-4cbf-9c8b-0cf7a4aaa572` | `X7cbD4mXYCOoC0TZkRjT3cbDSHv2` | `ACTIVE` |

---

## 3. Step-by-Step Workflow Execution Evidence

### Step 1: Authentication & Token Acquisition
All 5 actors authenticated against Firebase Authentication using standard client credentials. The acquired Firebase ID tokens were attached as `Authorization: Bearer <TOKEN>` on all subsequent REST API requests.
- **Verdict:** All 5 Firebase ID tokens successfully acquired.

### Step 2: Fresh Donation Creation (Donor)
- **Endpoint:** `POST /api/v1/donor/donations`
- **Request Payload:**
  - Title: `"Fresh Vegetable Biryani"`
  - Description: `"Authentic dum vegetable biryani prepared for banquet event, safely packed in thermo-insulated containers"`
  - Food Category: `PREPARED_MEALS`
  - Quantity: `40.0 meals`
  - Preparation Time: 1 hour prior to publication
  - Pickup Window: 15 minutes prior to publication until 6 hours ahead
  - Consume Before: 8 hours ahead
  - Pickup Address: `"Grand Gourmet Catering, MG Road, Bengaluru, Karnataka 560001"`
  - Coordinates: Latitude `12.9716`, Longitude `77.5946`
  - Food Safety Declared: `true` (Declaration: `"Hygiene standards verified; temperature held above 65C until insulated packing"`)
- **API Response:** `HTTP 201 Created`
  - Donation ID: `1aada97e-7900-4c11-aeb8-efd3a5e67a3f`
  - Status: `AVAILABLE`
  - Allocated Quantity: `0.0`
- **Database Confirmation:**
  - `donations.location` stored as PostGIS `POINT(77.5946 12.9716)` with SRID 4326.
  - Audit log action `DONATION_CREATED` recorded.

### Step 3: Spatial Discovery (Receiver Organization)
- **Endpoint:** `GET /api/v1/receiver/donations/nearby?latitude=12.9716&longitude=77.5946&radiusKm=15`
- **Query Mechanism:** Native PostgreSQL PostGIS query utilizing `ST_DWithin` on GiST index and `ST_Distance` calculation.
- **API Response:** `HTTP 200 OK`
  - Item Found: Donation `1aada97e-7900-4c11-aeb8-efd3a5e67a3f`
  - Distance: `0.0 km`
  - Available Quantity: `40.0 meals`
  - Temporal Filters: Verified `consume_before > NOW()` and `pickup_available_until > NOW()`.

### Step 4: Donation Request Submission (Receiver Organization)
- **Endpoint:** `POST /api/v1/receiver/donations/1aada97e-7900-4c11-aeb8-efd3a5e67a3f/requests`
- **Request Payload:**
  - Requested Quantity: `15.0 meals`
  - Request Reason: `"Evening hot meal distribution for shelter residents"`
- **API Response:** `HTTP 201 Created`
  - Request ID: `eb51b7e7-e78b-45f5-9dfc-d90f43af204d`
  - Status: `PENDING`
  - Allocated Quantity: `0.0`
- **Database Confirmation:**
  - Audit log action `REQUEST_CREATED` recorded.
  - In-app notification `REQUEST_CREATED` published to Donor user.

### Step 5: Request Review & Approval (Donor)
- **Review Endpoint:** `GET /api/v1/donor/donations/1aada97e-7900-4c11-aeb8-efd3a5e67a3f/requests` (`HTTP 200 OK`)
  - Request `eb51b7e7-e78b-45f5-9dfc-d90f43af204d` listed with status `PENDING`.
- **Approval Endpoint:** `POST /api/v1/donor/requests/eb51b7e7-e78b-45f5-9dfc-d90f43af204d/approve`
- **API Response:** `HTTP 200 OK`
  - Request Status: `APPROVED`
  - Request Allocated Quantity: `15.0`
  - Donation Total Allocated Quantity: `15.0`
  - Remaining Unallocated Quantity: `25.0`
  - Donation Status: `AVAILABLE` (remains available because unallocated surplus $25.0 > 0$)
- **Database Confirmation:**
  - Audit log action `REQUEST_APPROVED` recorded.
  - In-app notification `REQUEST_APPROVED` published to Receiver user.

### Step 6: Invariant & Constraint Verification
The following core invariants were verified both in-memory and at the database constraint level:
1. **Request Allocation Limit:** `request.allocated_quantity (15.0) <= request.requested_quantity (15.0)` &rarr; **PASS**
2. **Donation Allocation Cap:** `donation.allocated_quantity (15.0) <= donation.quantity (40.0)` &rarr; **PASS**
3. **Surplus Arithmetic:** `remaining = 40.0 - 15.0 = 25.0` &rarr; **PASS**
4. **Availability State:** Donation status remains `AVAILABLE` because `remaining > 0` &rarr; **PASS**

### Step 7: Admin Assigns Volunteer
- **Endpoint:** `POST /api/v1/admin/pickups`
- **Request Payload:**
  - `donationRequestId`: `eb51b7e7-e78b-45f5-9dfc-d90f43af204d`
  - `volunteerId`: `33333333-3333-3333-3333-333333333332` (Volunteer: Alex Kumar)
  - `scheduledAt`: 2 hours ahead (within pickup window)
- **API Response:** `HTTP 201 Created`
  - Pickup ID: `85c72dee-289b-4a5f-8cc1-00a2cd650955`
  - Status: `ASSIGNED`
  - Volunteer Availability Status: Automatically shifted from `AVAILABLE` &rarr; `BUSY`.
  - Secure OTP: 6-digit cryptographically secure code generated.
  - OTP Expiration: Scheduled time + 30-minute buffer window, capped before pickup window close.
- **Database Confirmation:**
  - Audit log actions `VOLUNTEER_ASSIGNED` and `OTP_GENERATED` recorded.
  - In-app notification `VOLUNTEER_ASSIGNED` published to Volunteer user.

### Step 8: Volunteer Verification & IDOR Security Check
- **Volunteer Pickup Listing:** `GET /api/v1/volunteer/pickups` (`HTTP 200 OK`)
  - Pickup `85c72dee-289b-4a5f-8cc1-00a2cd650955` successfully listed with status `ASSIGNED`.
- **IDOR Protection Check:**
  - Other Volunteer (`other.vol@mealshare.org`) attempts `GET /api/v1/volunteer/pickups/85c72dee-289b-4a5f-8cc1-00a2cd650955`.
  - Result: `HTTP 403 Forbidden` (`AccessDeniedException: You do not have permission to view this pickup.`).
  - **Verdict:** IDOR protection confirmed; volunteers cannot access pickups assigned to others.

### Step 9: Two-Phase OTP Verification Workflow
1. **Invalid OTP Submission:**
   - Volunteer submits `POST /api/v1/volunteer/pickups/85c72dee-289b-4a5f-8cc1-00a2cd650955/verify-otp` with `{ "otp": "999999" }`.
   - Result: `HTTP 409 Conflict` (`"Invalid or expired pickup OTP."`).
   - Database State: `pickups.otp_attempts` incremented to `1`.
   - Audit Log: `OTP_VERIFICATION_FAILED` logged.
2. **Valid OTP Submission:**
   - Volunteer submits `POST /api/v1/volunteer/pickups/85c72dee-289b-4a5f-8cc1-00a2cd650955/verify-otp` with `<VALID_OTP>`.
   - Result: `HTTP 200 OK`.
   - Pickup Status: Transitions to `PICKED_UP`.
   - Timestamp: `picked_up_at = 2026-09-27 17:28:55.736652+00`.
   - Donation Status: Transitions to `PICKED_UP`.
   - Notification: `OTP_VERIFIED` sent to Donor.
   - Audit Log: `OTP_VERIFIED` logged.
3. **Replay Protection Check:**
   - Volunteer resubmits the same OTP to `/verify-otp`.
   - Result: `HTTP 409 Conflict` (`"Pickup is not in ASSIGNED state. Current status: PICKED_UP"`).
   - **Verdict:** Replay attack prevented.
4. **Raw OTP Secrecy Check:**
   - Volunteer inspects pickup details via `GET /api/v1/volunteer/pickups/85c72dee-289b-4a5f-8cc1-00a2cd650955`.
   - Response inspected: Contains no `otp`, `otpHash`, or plaintext OTP field.
   - **Verdict:** Raw OTP secret is never exposed through client-facing read endpoints.

### Step 10: Pickup Delivery Completion
- **Endpoint:** `POST /api/v1/volunteer/pickups/85c72dee-289b-4a5f-8cc1-00a2cd650955/complete`
- **API Response:** `HTTP 200 OK`
  - Pickup Status: `COMPLETED`
  - Completed At: `2026-09-27 17:28:55.902995+00`
- **Cascading State Changes:**
  - `donation_requests.status`: Transitions to `COMPLETED`.
  - `volunteer_profiles.availability_status`: Automatically reset to `AVAILABLE`.
  - `volunteer_profiles.total_meals_delivered`: Incremented by 15.
  - `notifications`: `PICKUP_COMPLETED` sent to both Donor and Receiver.
  - `audit_logs`: `PICKUP_COMPLETED` recorded.

---

## 4. PostgreSQL Database Trace Evidence

The following records were extracted directly from the live Docker PostgreSQL database (`mealshare_app`) immediately following the workflow run:

### Table: `donations`
```sql
SELECT id, title, food_category, quantity, quantity_unit, allocated_quantity, status, 
       ST_AsText(location) as location_wkt, created_at, updated_at 
FROM donations WHERE id = '1aada97e-7900-4c11-aeb8-efd3a5e67a3f';
```
```
-[ RECORD 1 ]------+-------------------------------------
id                 | 1aada97e-7900-4c11-aeb8-efd3a5e67a3f
title              | Fresh Vegetable Biryani
food_category      | PREPARED_MEALS
quantity           | 40
quantity_unit      | meals
allocated_quantity | 15
status             | PICKED_UP
location_wkt       | POINT(77.5946 12.9716)
created_at         | 2026-09-27 17:28:54.845985+00
updated_at         | 2026-09-27 17:28:55.773107+00
```

### Table: `donation_requests`
```sql
SELECT id, donation_id, receiver_org_id, requested_quantity, allocated_quantity, 
       request_reason, status, requested_at, updated_at 
FROM donation_requests WHERE id = 'eb51b7e7-e78b-45f5-9dfc-d90f43af204d';
```
```
-[ RECORD 1 ]------+----------------------------------------------------
id                 | eb51b7e7-e78b-45f5-9dfc-d90f43af204d
donation_id        | 1aada97e-7900-4c11-aeb8-efd3a5e67a3f
receiver_org_id    | 22222222-2222-2222-2222-222222222222
requested_quantity | 15
allocated_quantity | 15
request_reason     | Evening hot meal distribution for shelter residents
status             | COMPLETED
requested_at       | 2026-09-27 17:28:54.96193+00
updated_at         | 2026-09-27 17:28:55.907218+00
```

### Table: `pickups`
```sql
SELECT id, status, scheduled_at, assigned_at, otp_hash, otp_expires_at, 
       otp_attempts, otp_verified_at, picked_up_at, completed_at 
FROM pickups WHERE id = '85c72dee-289b-4a5f-8cc1-00a2cd650955';
```
```
-[ RECORD 1 ]---+-------------------------------------------------------------
id              | 85c72dee-289b-4a5f-8cc1-00a2cd650955
status          | COMPLETED
scheduled_at    | 2026-09-27 19:28:54.799+00
assigned_at     | 2026-09-27 17:28:55.338392+00
otp_hash        | $2a$10$emueTBDwleLyqEcs1W.lqe/QU5jlZJ.3bPFG02yeRyhlK/h/9dP5G
otp_expires_at  | 2026-09-27 19:58:54.799+00
otp_attempts    | 1
otp_verified_at | 2026-09-27 17:28:55.736652+00
picked_up_at    | 2026-09-27 17:28:55.736652+00
completed_at    | 2026-09-27 17:28:55.902995+00
```

### Table: `notifications`
```sql
SELECT id, recipient_user_id, type, title, entity_type, entity_id, is_read, created_at 
FROM notifications 
WHERE entity_id IN ('1aada97e-7900-4c11-aeb8-efd3a5e67a3f', 'eb51b7e7-e78b-45f5-9dfc-d90f43af204d', '85c72dee-289b-4a5f-8cc1-00a2cd650955') 
ORDER BY created_at ASC;
```
```
                  id                  |          recipient_user_id           |        type        |           title           |   entity_type    |              entity_id               | is_read |          created_at           
--------------------------------------+--------------------------------------+--------------------+---------------------------+------------------+--------------------------------------+---------+-------------------------------
 3b5b30d5-3777-4e84-8300-c0cab326adbf | 11111111-1111-1111-1111-111111111111 | REQUEST_CREATED    | New Donation Request      | DONATION_REQUEST | eb51b7e7-e78b-45f5-9dfc-d90f43af204d | f       | 2026-09-27 17:28:54.985845+00
 8154687b-153a-43e3-b028-ce915cbb3189 | 22222222-2222-2222-2222-222222222221 | REQUEST_APPROVED   | Donation Request Approved | DONATION_REQUEST | eb51b7e7-e78b-45f5-9dfc-d90f43af204d | f       | 2026-09-27 17:28:55.165827+00
 6d28e625-94c9-4376-8734-b8a4623a7f63 | 33333333-3333-3333-3333-333333333331 | VOLUNTEER_ASSIGNED | New Pickup Assigned       | PICKUP           | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | f       | 2026-09-27 17:28:55.376364+00
 30cabce3-bf67-46d2-9f1e-8438b35b3936 | 11111111-1111-1111-1111-111111111111 | OTP_VERIFIED       | Donation Handed Over      | PICKUP           | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | f       | 2026-09-27 17:28:55.795007+00
 2cf9d3f8-6414-4115-bda2-9ebe2f2bc59d | 11111111-1111-1111-1111-111111111111 | PICKUP_COMPLETED   | Pickup Delivery Completed | PICKUP           | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | f       | 2026-09-27 17:28:55.93874+00
 0e3fb1b6-c20b-4eab-ba9b-eb9a8b06a290 | 22222222-2222-2222-2222-222222222221 | PICKUP_COMPLETED   | Food Delivery Completed   | PICKUP           | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | f       | 2026-09-27 17:28:55.955901+00
```

### Table: `audit_logs`
```sql
SELECT id, actor_user_id, actor_role, action_type, entity_name, entity_id, timestamp 
FROM audit_logs 
WHERE entity_id IN ('1aada97e-7900-4c11-aeb8-efd3a5e67a3f', 'eb51b7e7-e78b-45f5-9dfc-d90f43af204d', '85c72dee-289b-4a5f-8cc1-00a2cd650955') 
ORDER BY id ASC;
```
```
 id  |            actor_user_id             |  actor_role  |       action_type       |   entity_name   |              entity_id               |           timestamp           
-----+--------------------------------------+--------------+-------------------------+-----------------+--------------------------------------+-------------------------------
 117 | 11111111-1111-1111-1111-111111111111 | DONOR        | DONATION_CREATED        | Donation        | 1aada97e-7900-4c11-aeb8-efd3a5e67a3f | 2026-09-27 17:28:54.846989+00
 118 | 22222222-2222-2222-2222-222222222221 | RECEIVER_ORG | REQUEST_CREATED         | DonationRequest | eb51b7e7-e78b-45f5-9dfc-d90f43af204d | 2026-09-27 17:28:54.962989+00
 119 | 11111111-1111-1111-1111-111111111111 | DONOR        | REQUEST_APPROVED        | DonationRequest | eb51b7e7-e78b-45f5-9dfc-d90f43af204d | 2026-09-27 17:28:55.122577+00
 120 | 8ff89794-edb1-48c0-94eb-c584815263cb | ADMIN        | VOLUNTEER_ASSIGNED      | Pickup          | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | 2026-09-27 17:28:55.339392+00
 121 | 8ff89794-edb1-48c0-94eb-c584815263cb | ADMIN        | OTP_GENERATED           | Pickup          | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | 2026-09-27 17:28:55.353124+00
 122 | 33333333-3333-3333-3333-333333333331 | RECEIVER_VOL | OTP_VERIFICATION_FAILED | Pickup          | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | 2026-09-27 17:28:55.597345+00
 123 | 33333333-3333-3333-3333-333333333331 | RECEIVER_VOL | OTP_VERIFIED            | Pickup          | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | 2026-09-27 17:28:55.75708+00
 124 | 33333333-3333-3333-3333-333333333331 | RECEIVER_VOL | PICKUP_COMPLETED        | Pickup          | 85c72dee-289b-4a5f-8cc1-00a2cd650955 | 2026-09-27 17:28:55.921632+00
```

---

## 5. Security & Role Boundary Enforcement

Cross-role access boundaries were tested against live endpoints using genuine Firebase tokens:
1. **Unauthenticated Request:**
   - Request: `GET /api/v1/donor/donations` with no token.
   - Result: `HTTP 401 Unauthorized` &rarr; **PASS**
2. **Privilege Escalation Prevention:**
   - Request: Donor (`donor@mealshare.org`) attempts to call admin endpoint `GET /api/v1/admin/pickups`.
   - Result: `HTTP 403 Forbidden` &rarr; **PASS**
3. **Role Cross-Contamination Prevention:**
   - Request: Receiver (`receiver@mealshare.org`) attempts to create a donation via `POST /api/v1/donor/donations`.
   - Result: `HTTP 403 Forbidden` &rarr; **PASS**
4. **Horizontal Privilege Escalation (IDOR):**
   - Request: Other Volunteer (`other.vol@mealshare.org`) attempts `GET /api/v1/volunteer/pickups/{pickupId}`.
   - Result: `HTTP 403 Forbidden` &rarr; **PASS**

---

## 6. Monorepo Regression Verification

All three subsystems across the repository were executed through their native build/test commands to confirm full integrity:

```
1. Spring Boot Backend Test Suite:
   Command: .\mvnw.cmd test
   Result: Tests run: 213, Failures: 0, Errors: 0, Skipped: 0 [BUILD SUCCESS]

2. React Admin Web:
   Command: npm run build
   Result: tsc -b && vite build -> built in 1.09s, 0 errors [EXIT CODE 0]

3. Flutter Mobile Application:
   Command: flutter analyze
   Result: No issues found! (ran in 5.0s) [EXIT CODE 0]
```

---

## 7. Scope & Limitation Acknowledgments

In accordance with strict engineering audit standards:
1. **In-App vs. Push Notifications:** The automated workflow verified that domain events correctly produce, dispatch, and persist in-app notifications in PostgreSQL (`notifications` table). End-to-end delivery of physical push notifications to physical mobile device APNs/FCM tokens was not tested and remains dependent on active production device tokens.
2. **Food Safety Controls:** Food safety validation is enforced through mandatory self-declarations and temporal constraints (`consume_before`, `pickup_available_until`, and `preparation_datetime`). The system does not perform physical temperature logging or chemical food testing.
3. **AI/ML Implementations:** MealShare does not employ machine learning models, inference engines, or AI forecasting. All matching is performed deterministically using PostGIS spatial indexes and relational status constraints.
4. **Production Readiness:** While all functional requirements, security boundaries, and relational invariants are strictly verified, production deployment would require high-availability clustering, hardware security modules (HSM) for key management, and production-grade monitoring.

# MealShare Capstone Demonstration Checklist

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Target Audience**: Evaluation Committee, Examiners, and Capstone Presenters  
> **Status**: VERIFIED & EXECUTABLE  
> **Backend Base URL**: `http://localhost:8081`  
> **Admin Web URL**: `http://localhost:5173` (or production build preview)  
> **Mobile App**: Flutter Mobile (Android Emulator / Real Device)  

---

## Pre-Demo Setup Verification

Before starting the live demonstration, ensure the following services are active:

1. **Docker PostgreSQL + PostGIS Container**:
   - Status: Running on host port `5433` (database: `mealshare_app`).
   - Check: `docker ps` confirms container `mealshare-postgres` is healthy.
2. **Spring Boot Backend**:
   - Status: Active on port `8081` (`java -jar backend/target/mealshare-backend-1.0.0-SNAPSHOT.jar` or `./mvnw spring-boot:run`).
   - Check: `curl http://localhost:8081/api/v1/health` returns `{"status":"UP"}`.
3. **Admin Web**:
   - Status: Running on port `5173` (`npm run dev` in `admin-web`).
4. **Mobile Client**:
   - Status: Running on Flutter emulator or test device (`flutter run -d chrome` or mobile device).

### Pre-Seeded Test Credentials

| Role | Email | Password | Display Name / Organization |
|:-----|:------|:---------|:----------------------------|
| **Admin** | `admin@mealshare.org` | `admin123` | Platform Administrator |
| **Donor** | `donor@mealshare.org` | `donor123` | Grand Gourmet Catering Co. (Indiranagar) |
| **Receiver Org** | `receiver@mealshare.org` | `receiver123` | Hope Community Shelter Foundation |
| **Volunteer** | `volunteer@mealshare.org` | `volunteer123` | Alex Volunteer (Indiranagar) |

---

## 14-Step End-to-End Capstone Demonstration Sequence

```
[1. Admin Login] ──► [2. Admin Dashboard] ──► [3. Donor Creates Donation]
                                                         │
[6. Donor Approves Request] ◄── [5. Receiver Requests] ◄── [4. Receiver Discovers]
        │
        ▼
[7. Admin Assigns Volunteer] ──► [8. Volunteer Views Pickup] ──► [9. OTP Verification]
                                                                        │
[12. Notifications Review] ◄── [11. Admin Sees Updated State] ◄── [10. Pickup Completion]
        │
        ▼
[13. Audit Log Inspection] ──► [14. Redistribution Reports]
```

---

### Step 1: Admin Authentication
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — Login Screen (`/login`)
- **Action**: Enter admin email and password, click **"Sign In"**.
- **Expected Visible Result**: Successful authentication via Firebase SDK; redirected to `/admin` dashboard. No mock mode warnings.
- **Backend / API Involved**:
  - Firebase Authentication Web SDK
  - `GET /api/v1/users/me`

---

### Step 2: Admin Dashboard Baseline Review
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — Dashboard (`/admin`)
- **Action**: Inspect active platform KPIs (Total Users, Active Donations, Pending Requests, Active Pickups).
- **Expected Visible Result**: Live database metrics render cleanly; status breakdown donut charts and 7-day activity graphs load without mock indicators.
- **Backend / API Involved**:
  - `GET /api/v1/admin/dashboard`

---

### Step 3: Donor Creates Surplus Food Donation
- **Account / Role**: `donor@mealshare.org` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile — Create Donation Screen
- **Action**:
  1. Fill title: `"Fresh Paneer Butter Masala & Roti"`
  2. Category: `PREPARED_MEALS`
  3. Quantity: `30`, Unit: `meals`
  4. Preparation Time: Set to current time (e.g., 1 hour ago)
  5. Consume-Before Time: Set to 6 hours in the future
  6. Pickup Window: Current time to 4 hours in the future
  7. Address: `"100 Feet Road, Indiranagar, Bengaluru"` (Lat: `12.9716`, Lon: `77.6412`)
  8. Check mandatory **"I certify food safety & hygiene standards"** checkbox.
  9. Click **"Publish Donation"**.
- **Expected Visible Result**: Green success toast: *"Donation created successfully"*. Donation appears immediately under Donor's active listing with `AVAILABLE` badge and remaining quantity `30`.
- **Backend / API Involved**:
  - `POST /api/v1/donor/donations`
  - Database: Insert into `donations` with PostGIS `ST_SetSRID(ST_MakePoint(77.6412, 12.9716), 4326)`.

---

### Step 4: Receiver Discovers Nearby Donation
- **Account / Role**: `receiver@mealshare.org` (`ROLE_RECEIVER_ORG`)
- **Screen**: Flutter Mobile — Receiver Nearby Discovery Screen
- **Action**: Open Discover feed centered around Indiranagar (Radius: 10 km).
- **Expected Visible Result**: The newly created donation `"Fresh Paneer Butter Masala & Roti"` is displayed with calculated geodesic distance (e.g., `~1.2 km away`), food category badge, and countdown timer.
- **Backend / API Involved**:
  - `GET /api/v1/receiver/donations/nearby?latitude=12.978&longitude=77.640&radiusKm=10`
  - PostGIS: `ST_DWithin` and `ST_Distance` evaluation on GiST index.

---

### Step 5: Receiver Submits Claim Request
- **Account / Role**: `receiver@mealshare.org` (`ROLE_RECEIVER_ORG`)
- **Screen**: Flutter Mobile — Donation Details Screen
- **Action**:
  1. Tap on `"Fresh Paneer Butter Masala & Roti"`.
  2. Inspect food safety declaration text and donor details.
  3. Enter Requested Quantity: `20` meals.
  4. Enter Reason: `"Dinner distribution for night shelter residents"`.
  5. Tap **"Submit Claim Request"**.
- **Expected Visible Result**: Request submitted message displayed. Button disables with status `"Request Pending Approval"`. Request appears in **"My Requests"** tab with `PENDING` badge.
- **Backend / API Involved**:
  - `POST /api/v1/receiver/donations/{donationId}/requests`
  - Event: `DonationRequestCreatedEvent` published.
  - Notification: In-app notification created for Donor.

---

### Step 6: Donor Reviews and Approves Request
- **Account / Role**: `donor@mealshare.org` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile — Donor Incoming Requests Screen
- **Action**:
  1. Open Incoming Requests for the donation.
  2. View claim from `"Hope Community Shelter Foundation"` for `20 meals` in FCFS queue.
  3. Tap **"Approve"**.
- **Expected Visible Result**: Request status updates to `APPROVED`. Remaining available quantity dynamically decrements from `30` to `10 meals`.
- **Backend / API Involved**:
  - `POST /api/v1/donor/requests/{requestId}/approve`
  - Database: Pessimistic write lock on `donations`, atomic update `allocated_quantity = 20.0`.
  - Event: `DonationRequestApprovedEvent` published.

---

### Step 7: Admin Assigns Volunteer to Approved Claim
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — Pickups Management (`/admin/pickups`)
- **Action**:
  1. Locate the approved request for `"Fresh Paneer Butter Masala & Roti"`.
  2. Click **"Assign Volunteer"**.
  3. Select volunteer `"Alex Volunteer"`.
  4. Select scheduled pickup time: within valid pickup window.
  5. Click **"Confirm Assignment"**.
- **Expected Visible Result**:
  - Success banner with generated 6-digit numeric OTP (e.g., `839201`).
  - Examiner Note: *"This OTP is shown once to the Admin/Donor and stored exclusively as a BCrypt hash in the database."*
  - Pickup status becomes `ASSIGNED`.
- **Backend / API Involved**:
  - `POST /api/v1/admin/pickups`
  - BCrypt: SecureRandom OTP hashed and saved to `pickups.otp_hash`.
  - Event: `VolunteerAssignedEvent` published.

---

### Step 8: Volunteer Views Assigned Task
- **Account / Role**: `volunteer@mealshare.org` (`ROLE_RECEIVER_VOL`)
- **Screen**: Flutter Mobile — Volunteer Home Screen
- **Action**: Pull to refresh assigned deliveries list.
- **Expected Visible Result**:
  - New delivery card appears with status `ASSIGNED`.
  - Displays Pickup Address (`Indiranagar`), Delivery Address (`Hope Shelter`), and meal count (`20 meals`).
  - Button visible: **"Verify Pickup OTP"**.
- **Backend / API Involved**:
  - `GET /api/v1/volunteer/pickups`

---

### Step 9: Volunteer Verifies OTP at Food Handover
- **Account / Role**: `volunteer@mealshare.org` (`ROLE_RECEIVER_VOL`)
- **Screen**: Flutter Mobile — Volunteer Home Screen (OTP Dialog)
- **Action**:
  1. Tap **"Verify Pickup OTP"**.
  2. *(Optional demonstration of security)*: Enter incorrect code `"000000"` $\rightarrow$ Observe error message *"Invalid or expired pickup OTP"* and attempt counter incremented.
  3. Enter valid OTP generated in Step 7.
  4. Tap **"Verify"**.
- **Expected Visible Result**: OTP validated successfully. Pickup card transitions immediately from `ASSIGNED` to `PICKED_UP` with green badge. Action button changes to **"Complete Delivery"**.
- **Backend / API Involved**:
  - `POST /api/v1/volunteer/pickups/{pickupId}/verify-otp`
  - BCrypt: `passwordEncoder.matches(rawOtp, pickup.getOtpHash())`.
  - Event: `OtpVerifiedEvent` published.

---

### Step 10: Volunteer Completes Delivery Handover
- **Account / Role**: `volunteer@mealshare.org` (`ROLE_RECEIVER_VOL`)
- **Screen**: Flutter Mobile — Volunteer Home Screen
- **Action**: Tap **"Complete Delivery"** upon arrival at shelter.
- **Expected Visible Result**: Pickup card moves to `COMPLETED` tab. Volunteer's lifetime delivered count increments by `+20 meals`.
- **Backend / API Involved**:
  - `POST /api/v1/volunteer/pickups/{pickupId}/complete`
  - Database: Updates `pickups.status = COMPLETED`, `donation_requests.status = COMPLETED`, `volunteer_profiles.total_meals_delivered += 20`.
  - Event: `PickupCompletedEvent` published.

---

### Step 11: Admin Observes Synchronized System State
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — Pickups & Donations Overview (`/admin/pickups`, `/admin/donations`)
- **Action**: Refresh or navigate to Pickups and Donations tables.
- **Expected Visible Result**:
  - Pickups table shows status `COMPLETED` with verified and completion timestamps.
  - Donations table shows `20 / 30 meals` allocated and delivered.
- **Backend / API Involved**:
  - `GET /api/v1/admin/pickups`
  - `GET /api/v1/admin/donations/{id}`

---

### Step 12: Notification Flow Verification
- **Account / Role**: Admin, Donor, Receiver, Volunteer
- **Screen**:
  - React Admin: `/admin/notifications`
  - Flutter Mobile: Notifications Screen
- **Action**: Open notifications in each client.
- **Expected Visible Result**:
  - Donor received: *"New Donation Request"*, *"Donation Handed Over"*, and *"Pickup Delivery Completed"*.
  - Receiver received: *"Donation Request Approved"* and *"Food Delivery Completed"*.
  - Volunteer received: *"New Pickup Assigned"*.
  - Unread count badge decrements upon opening or clicking **"Mark all as read"**.
- **Backend / API Involved**:
  - `GET /api/v1/notifications`
  - `POST /api/v1/notifications/read-all`

---

### Step 13: Operational Audit Trail Inspection
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — Audit Logs
- **Action**: View audit log entries generated during the demo session.
- **Expected Visible Result**: Complete chronological trail of events:
  1. `DONATION_CREATED` (Actor: Donor)
  2. `DONATION_REQUEST_CREATED` (Actor: Receiver)
  3. `REQUEST_APPROVED` (Actor: Donor)
  4. `VOLUNTEER_ASSIGNED` (Actor: Admin)
  5. `OTP_VERIFIED` (Actor: Volunteer)
  6. `PICKUP_COMPLETED` (Actor: Volunteer)
  - Crucial Security Check: Audit JSON details do **NOT** expose plaintext OTP or raw hash.
- **Backend / API Involved**:
  - `GET /api/v1/admin/audit-logs`

---

### Step 14: Analytics & Redistribution Reports
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — Reports (`/admin/reports`)
- **Action**: Toggle time range (`30d`).
- **Expected Visible Result**: Live aggregation metrics reflect the 20 newly distributed meals:
  - Total Food Rescued volume updated.
  - Category breakdown chart includes `PREPARED_MEALS`.
  - Redistribution success rate reflected in visual bar/donut charts.
- **Backend / API Involved**:
  - `GET /api/v1/admin/reports/summary?timeRange=30d`

---

## Examiner Q&A Quick Reference

| Potential Examiner Question | Verified Technical Answer |
|:----------------------------|:--------------------------|
| *How does MealShare guarantee food safety?* | MealShare **does not certify food safety**. The platform enforces operational hygiene and temporal validation (mandatory donor self-declaration, strict consume-before timestamps, pickup windows). Legal liability and physical quality checks remain with participating donors and receivers. |
| *Where is the AI/ML model for demand prediction?* | Predictive demand forecasting and OR-Tools route optimization are **proposed future architectural enhancements**, documented in the project roadmap. No ML models are currently deployed in this build. |
| *Are push notifications delivering to physical phones?* | In-app notification persistence in PostgreSQL is **implemented and tested**. External Firebase Cloud Messaging (FCM) dispatch to physical APNS/FCM mobile devices requires live Apple Developer and Google cloud credentials and is classified as **NOT TESTED** in local offline evaluation. |
| *How does the system prevent volunteer OTP spoofing?* | OTPs are 6-digit cryptographically secure numbers generated with `java.security.SecureRandom`. Only a BCrypt hash is stored in PostgreSQL. OTP verification is rate-limited (max 5 attempts), expires after 10 minutes, and requires a pessimistic write lock. |

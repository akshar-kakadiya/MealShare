# MealShare Live Demonstration Script — Phase-1 Capstone

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2 Live Demonstration  
> **Target Execution Duration**: 8 to 12 Minutes  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Demonstration Environment Pre-Flight Checklist

Before initiating the presentation, ensure the local environment is verified and operational:
1. **PostgreSQL / PostGIS Database**: Running on host port `5433` (`docker ps` shows `mealshare-postgres` healthy).
2. **Spring Boot Backend**: Running on port `8081` (`curl http://localhost:8081/api/v1/health` returns `UP`).
3. **React Admin Web**: Running on port `5173` (`http://localhost:5173`).
4. **Flutter Mobile Application**: Running in Chrome / Emulator / Device.
5. **Seeded Test Accounts**:
   - Admin: `admin@mealshare.org` (`ROLE_ADMIN`)
   - Donor: `donor@grandhotel.com` (`ROLE_DONOR`, Grand Hotel Banquet)
   - Receiver: `shelter@hopehaven.org` (`ROLE_RECEIVER_ORG`, Hope Haven Shelter, Verified)
   - Volunteer: `volunteer@cityrescue.org` (`ROLE_RECEIVER_VOL`, Rahul Volunteer, Available)

---

## 2. Step-by-Step Demonstration Sequence (20 Steps)

```
┌────────────────────────────────────────────────────────────────────────┐
│                      20-STEP LIVE DEMO FLOW                            │
│  Admin Setup (Steps 1-2) ──► Donor Listing (Steps 3-5)                │
│       │                                  │                             │
│       ▼                                  ▼                             │
│  Receiver Discovery & Claim (Steps 6-9) ◄┘                             │
│       │                                                                │
│       ▼                                                                │
│  Donor Allocation Approval (Steps 10-12)                               │
│       │                                                                │
│       ▼                                                                │
│  Admin Volunteer Assignment (Step 13)                                  │
│       │                                                                │
│       ▼                                                                │
│  Volunteer OTP Verification & Delivery (Steps 14-16)                   │
│       │                                                                │
│       ▼                                                                │
│  System Verification: Statuses, Alerts, Audit & Reports (Steps 17-20)  │
└────────────────────────────────────────────────────────────────────────┘
```

---

### Phase I: Administrative Governance & Baseline Setup (Steps 1–2)

#### Step 1: Administrator Authentication
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — `/login`
- **Action**: Enter administrator credentials and click **Sign In**.
- **Expected Result**: Successfully authenticated; redirected to `/admin/dashboard`.
- **Technical Point to Mention**: *"Authentication is mediated by Firebase tokens, with the Spring Boot backend evaluating the user's role directly from PostgreSQL to enforce `ROLE_ADMIN` privileges."*

#### Step 2: System Health & KPI Dashboard Inspection
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — `/admin/dashboard`
- **Action**: Highlight the platform overview KPI cards (Total Users, Active Donations, Pending Claims, Ongoing Pickups).
- **Expected Result**: Real-time counter cards and 7-day activity metrics render dynamically.
- **Technical Point to Mention**: *"This dashboard aggregates live statistics across the 10 database tables via `AdminDashboardController`."*

---

### Phase II: Donor Publishing & Food Safety Governance (Steps 3–5)

#### Step 3: Donor Mobile Authentication
- **Account / Role**: `donor@grandhotel.com` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile App — Login Screen
- **Action**: Sign in as the Donor account; app loads the Donor Home viewport.
- **Expected Result**: Donor navigation loads showing "My Donations" and active listing counters.
- **Technical Point to Mention**: *"Flutter uses Provider state management and routes the user to the Donor shell based on the backend role response."*

#### Step 4: Surplus Food Listing Creation
- **Account / Role**: `donor@grandhotel.com` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile App — Create Donation Screen
- **Action**: Input listing details:
  - Title: *"Vegetable Biryani & Dal (50 Portions)"*
  - Category: `PREPARED_MEALS`
  - Quantity: `50.0` | Unit: `Portions`
  - Coordinates: Latitude `12.9352`, Longitude `77.5348` (PES University, Bengaluru)
- **Expected Result**: Form fields populate with real-time validation.
- **Technical Point to Mention**: *"The coordinates will be transformed server-side into a native PostGIS `GEOMETRY(Point, 4326)` entity with a GiST spatial index."*

#### Step 5: Food Safety Declaration & Pickup Window Validation
- **Account / Role**: `donor@grandhotel.com` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile App — Create Donation Screen (Safety Section)
- **Action**: Set preparation time (1 hour ago), consume-before deadline (5 hours in future), pickup window (next 2 hours), and **check the mandatory Food Safety Declaration checkbox**. Click **Publish Donation**.
- **Expected Result**: Donation created with status `AVAILABLE`; green success snackbar displayed.
- **Technical Point to Mention**: *"Backend enforces temporal validation: if consume-before is in the past, or if the food safety checkbox is unchecked, the backend rejects the transaction with `HTTP 400`."*

---

### Phase III: Receiver Geospatial Discovery & Claim Request (Steps 6–9)

#### Step 6: Receiver Mobile Authentication
- **Account / Role**: `shelter@hopehaven.org` (`ROLE_RECEIVER_ORG`)
- **Screen**: Flutter Mobile App — Login Screen / Account Switcher
- **Action**: Switch account to the verified Receiver Organization.
- **Expected Result**: Receiver Home loads with Nearby Discovery and Request History tabs.
- **Technical Point to Mention**: *"The organization has already been institutional-verified by the admin (`verification_status = 'APPROVED'`), granting permission to claim food."*

#### Step 7: PostGIS Spatial Proximity Discovery
- **Account / Role**: `shelter@hopehaven.org` (`ROLE_RECEIVER_ORG`)
- **Screen**: Flutter Mobile App — Nearby Donations Screen
- **Action**: Trigger nearby search with receiver coordinates (`12.9340, 77.5360`) and 10 km radius.
- **Expected Result**: The Grand Hotel biryani listing appears at the top of the list, displaying computed geodesic distance (e.g., *"0.24 km away"*).
- **Technical Point to Mention**: *"This query is executed directly in PostgreSQL using `ST_DWithin` and `ST_Distance` on the GiST-indexed `location` column, filtered by `consume_before > NOW()`."*

#### Step 8: Donation Details Inspection
- **Account / Role**: `shelter@hopehaven.org` (`ROLE_RECEIVER_ORG`)
- **Screen**: Flutter Mobile App — Donation Detail Screen
- **Action**: Tap on the Biryani card to inspect the full listing.
- **Expected Result**: Displays donor entity name, food category, available quantity (50 Portions), pickup deadline, and food safety declaration status.
- **Technical Point to Mention**: *"The receiver verifies the food safety declaration and expiry timeline before committing logistics."*

#### Step 9: Claim Request Submission
- **Account / Role**: `shelter@hopehaven.org` (`ROLE_RECEIVER_ORG`)
- **Screen**: Flutter Mobile App — Request Claim Dialog
- **Action**: Enter Requested Quantity: `30` Portions, Reason: *"Lunch distribution for 30 children"*, and submit.
- **Expected Result**: Claim created in `PENDING` status; donation request list displays the new active claim.
- **Technical Point to Mention**: *"The partial unique index `idx_unique_active_donation_request` prevents the organization from submitting duplicate pending claims for this donation."*

---

### Phase IV: Donor Concurrency-Safe Claim Allocation (Steps 10–12)

#### Step 10: Donor Reviews Incoming Claims
- **Account / Role**: `donor@grandhotel.com` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile App — Donor Donation Detail / Requests Tab
- **Action**: Re-open the Biryani listing and navigate to the "Incoming Requests" section.
- **Expected Result**: Hope Haven Shelter's request for 30 portions appears in chronological order (`requested_at ASC`).
- **Technical Point to Mention**: *"FCFS ordering guarantees transparent, equitable review for non-profit agencies."*

#### Step 11: Claim Adjudication & Allocation Approval
- **Account / Role**: `donor@grandhotel.com` (`ROLE_DONOR`)
- **Screen**: Flutter Mobile App — Incoming Requests Tab
- **Action**: Tap **Approve Request**.
- **Expected Result**: Request transitions to `APPROVED`. Remaining available donation balance updates to `20 Portions` (`allocated_quantity` updated to 30).
- **Technical Point to Mention**: *"The allocation is atomic. Database check constraint `chk_donations_allocated_le_quantity` guarantees that total allocations cannot exceed available quantity."*

#### Step 12: Event Publication Confirmation
- **Account / Role**: System / Donor
- **Screen**: Backend Console / Notification Drawer
- **Action**: Note that an internal `DonationRequestApprovedEvent` was fired.
- **Expected Result**: Notification record created in PostgreSQL for the receiver organization.
- **Technical Point to Mention**: *"Processed `AFTER_COMMIT` via Spring Application Events, isolating the notification pipeline from core transaction failure."*

---

### Phase V: Admin Volunteer Assignment & OTP Generation (Step 13)

#### Step 13: Administrator Dispatches Volunteer Courier
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — `/admin/pickups` → "Assign Pickup" Modal
- **Action**: Select the approved claim request, select available volunteer *"Rahul Volunteer"*, set scheduled pickup time, and click **Assign Pickup**.
- **Expected Result**: Pickup created with status `ASSIGNED`. The 6-digit OTP code (e.g., `482915`) is displayed **once** in the admin modal and issued to the donor.
- **Technical Point to Mention**: *"Only a salted BCrypt hash is stored in `pickups.otp_hash`. The plain code is never saved in the database."*

---

### Phase VI: Volunteer Handover Verification & Completion (Steps 14–16)

#### Step 14: Volunteer Inspects Assigned Delivery Task
- **Account / Role**: `volunteer@cityrescue.org` (`ROLE_RECEIVER_VOL`)
- **Screen**: Flutter Mobile App — Volunteer Home / Assigned Pickups
- **Action**: Sign in as Volunteer; open the newly assigned delivery task.
- **Expected Result**: Pickup details display donor address (Grand Hotel), destination (Hope Haven), scheduled time, and status `ASSIGNED`.
- **Technical Point to Mention**: *"Enforces IDOR security: a volunteer cannot view or manipulate pickups assigned to other volunteers."*

#### Step 15: Cryptographic OTP Handover Verification
- **Account / Role**: `volunteer@cityrescue.org` (`ROLE_RECEIVER_VOL`)
- **Screen**: Flutter Mobile App — Verify Handover Screen
- **Action**: Volunteer physically obtains the 6-digit OTP from the donor and inputs it into the mobile keypad. Tap **Verify Handover**.
- **Expected Result**: Backend verifies BCrypt hash under pessimistic write locking; pickup status immediately updates to `PICKED_UP`; timestamp recorded.
- **Technical Point to Mention**: *"Rate-limiting is enforced: maximum 5 attempts, with a 10-minute expiry window to prevent brute-force attacks."*

#### Step 16: Delivery Completion Confirmation
- **Account / Role**: `volunteer@cityrescue.org` (`ROLE_RECEIVER_VOL`)
- **Screen**: Flutter Mobile App — Active Pickup Screen
- **Action**: Volunteer arrives at Hope Haven Shelter, delivers meals, and taps **Complete Delivery**.
- **Expected Result**: Pickup status transitions to `COMPLETED`; donation request transitions to `COMPLETED`; completed delivery counters increment.
- **Technical Point to Mention**: *"Triggers `PickupCompletedEvent`, cascading completion state across the request and updating volunteer performance metrics."*

---

### Phase VII: Audit Trail, Notifications & Reports (Steps 17–20)

#### Step 17: Platform State Consistency Inspection
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — `/admin/donations` and `/admin/requests`
- **Action**: Refresh listings to inspect updated statuses.
- **Expected Result**: Donation reflects allocated and completed quantities; request reflects `COMPLETED`.
- **Technical Point to Mention**: *"Demonstrates end-to-end relational consistency across the entire redistribution lifecycle."*

#### Step 18: Notification Queue Verification
- **Account / Role**: `shelter@hopehaven.org` / `donor@grandhotel.com`
- **Screen**: Flutter Mobile App — Notifications Screen
- **Action**: Open notification drawer.
- **Expected Result**: In-app notifications displayed: *"Donation Request Approved"*, *"Pickup Assigned"*, and *"Delivery Completed"*.
- **Technical Point to Mention**: *"Notifications are stored in PostgreSQL (`notifications` table) with read/unread tracking via REST endpoints."*

#### Step 19: Immutable Audit Log Review
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — `/admin/audit-logs`
- **Action**: Filter audit logs by entity `PICKUP` and `DONATION_REQUEST`.
- **Expected Result**: Shows chronological entries: actor UIDs, actions (`CREATE`, `APPROVE`, `VERIFY_OTP`, `COMPLETE`), client IP addresses, and JSONB `delta_state` snapshots.
- **Technical Point to Mention**: *"Guarantees non-repudiation and post-incident regulatory auditability in accordance with food safety standards."*

#### Step 20: Operational Impact Analytics
- **Account / Role**: `admin@mealshare.org` (`ROLE_ADMIN`)
- **Screen**: React Admin Web — `/admin/reports`
- **Action**: Select Time Range: `7d`; view Category Distribution and Volume charts.
- **Expected Result**: Interactive Recharts bar/pie charts render showing 30 portions of `PREPARED_MEALS` successfully redistributed.
- **Technical Point to Mention**: *"Concludes the live demonstration, showing measurable food waste diversion and community impact."*

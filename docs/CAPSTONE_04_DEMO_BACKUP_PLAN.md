# MealShare Live Demonstration Contingency & Backup Plan

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Core Demonstration Principle

To maintain academic honesty during examination:
- **Never pretend a failed live feature worked**.
- If a live component encounters an infrastructure failure (e.g., network timeout, cloud provider outage), state the failure transparently, explain the underlying technical cause, and immediately pivot to the **Pre-Seeded Database State** or **Verified Execution Evidence**.
- Clearly articulate to the examination committee: *"This is a pre-seeded fallback demonstration to illustrate the verified workflow."*

---

## 2. Contingency Procedures Matrix

| Failure Scenario | Root Cause | Immediate Diagnostic Action | Pre-Planned Fallback Procedure | Verification Evidence to Cite |
|:---|:---|:---|:---|:---|
| **Scenario 1: Firebase Auth / Cloud Outage** | External internet loss or Firebase token verification timeout. | Check internet connectivity; inspect backend log for `FirebaseAuthException`. | Switch mobile app to **Mock Auth Mode** (`AppState.isMockMode = true`) or use local pre-authenticated bearer token test harness. In Admin Web, use local development credentials bypass if pre-configured. | Reference `SecurityTest.java` (9 passing tests proving token decoding logic offline). |
| **Scenario 2: Spring Boot Backend Stops** | Port conflict (e.g., Oracle on 8080) or process termination. | Run `netstat -ano \| findstr 8081` to verify PID. Check `backend/target/` for executable JAR. | Restart backend in terminal: `cd backend; java -jar target/mealshare-backend-1.0.0-SNAPSHOT.jar --server.port=8081`. Takes < 6 seconds to boot. | Show Swagger UI (`http://localhost:8081/swagger-ui.html`) or Postman collection. |
| **Scenario 3: Docker PostgreSQL Container Halts** | Docker daemon pause or host port conflict on 5433. | Run `docker ps -a` in PowerShell. | Execute recovery command: `docker start mealshare-postgres`. Verify database availability via `pg_isready -p 5433`. | Point to Flyway migrations V1–V7 in `src/main/resources/db/migration/`. |
| **Scenario 4: OTP Verification Code Lost / Expired** | 10-minute window elapsed or OTP not noted during admin modal. | Inspect database `pickups` table: `SELECT id, status, otp_attempts FROM pickups WHERE status = 'ASSIGNED';`. | **Do NOT attempt random codes** (5 failed attempts will lock the pickup). Instead, cancel and reassign pickup in Admin Web, or use pre-seeded pickup `pickup-test-01` with known test OTP `123456`. | Reference `PickupOtpServiceTest.java` and `PickupServiceTest.java` (23 passing tests). |
| **Scenario 5: Mobile UI Rendering Glitch** | Flutter web hot-reload state corruption or browser cache mismatch. | Hard refresh browser tab (`Ctrl + F5`) or restart Flutter development server (`flutter run -d chrome`). | Switch immediately to the **React Admin Web portal**, which provides full operational visibility over donations, claim requests, volunteer pickups, and audit logs. | Show passing `flutter analyze` and `flutter test` terminal outputs. |
| **Scenario 6: PostGIS Proximity Query Returns Empty** | Receiver coordinates set outside donation discovery radius (> 10 km). | Check coordinates of published donation vs. receiver search point. | Adjust receiver radius slider to `25 km` or set coordinates to known test point: Latitude `12.9352`, Longitude `77.5348`. | Show native SQL query in `DonationRepository.java` demonstrating `ST_DWithin` logic. |

---

## 3. Pre-Seeded Fallback Datasets in PostgreSQL

If live record creation is interrupted, the PostgreSQL database contains pre-seeded baseline entities initialized via database test scripts:

1. **Active Available Donation (`AVAILABLE`)**:
   - Title: *"Packaged Bakery & Bread Baskets"*
   - Quantity: `40.0 Kg` | Location: `12.9352, 77.5348`
   - Food Safety: Declared (`food_safety_declared = true`)
2. **Approved Claim Request (`APPROVED`)**:
   - Organization: Hope Haven Children Home (`shelter@hopehaven.org`)
   - Allocated Quantity: `25.0 Kg`
3. **Assigned Pickup Task (`ASSIGNED`)**:
   - Volunteer: Rahul Volunteer (`volunteer@cityrescue.org`)
   - Status: Ready for OTP verification
4. **Completed Lifecycle Records (`COMPLETED`)**:
   - Historical donations, pickups, notifications, and audit log entries readily inspectable in Admin Web.

---

## 4. Academic Presentation Integrity Checklist

- [x] Pre-flight health check executed 10 minutes prior to presentation.
- [x] Docker container running and healthy on port 5433.
- [x] Spring Boot backend running on port 8081.
- [x] Terminal windows open with automated test scripts ready for verification.
- [x] Fallback datasets confirmed present in database.
- [x] Clear verbal script prepared to distinguish LIVE DEMO from PRESEEDED FALLBACK.

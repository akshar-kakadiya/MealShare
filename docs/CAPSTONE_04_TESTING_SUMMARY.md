# MealShare Testing & Verification Summary

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Executive Test Summary

All subsystems comprising the MealShare platform have undergone rigorous automated testing and static analysis without modifying any underlying business logic. All tests execute successfully against the local development environment:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   MEALSHARE AUTOMATED TEST MATRIX                      │
├───────────────────────┬─────────────────────────┬──────────────────────┤
│ SUBSYSTEM             │ COMMAND                 │ RESULT               │
├───────────────────────┼─────────────────────────┼──────────────────────┤
│ Spring Boot Backend   │ .\mvnw.cmd test         │ 204/204 Passed (PASS)│
│ React Admin Web       │ npm run build           │ 0 TS Errors (PASS)   │
│ Flutter Mobile Lint   │ flutter analyze         │ 0 Issues (PASS)      │
│ Flutter Mobile Tests  │ flutter test            │ 6/6 Passed (PASS)    │
└───────────────────────┴─────────────────────────┴──────────────────────┘
```

---

## 2. Spring Boot Backend Test Suite Details

- **Test Execution Tool**: Apache Maven Wrapper (`mvnw.cmd test`), JUnit 5, Spring Boot Test, Mockito, MockMvc.
- **Active Spring Profile**: `test`
- **Total Tests Executed**: **204**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **0**
- **Build Status**: **BUILD SUCCESS**

### Test Categories & Coverage Breakdown

| Functional Category | Primary Test Classes | Verified Engineering Controls |
|:---|:---|:---|
| **Authentication & Token Extraction** | `SecurityTest.java`<br>`UserServiceTest.java` | Verifies Firebase Bearer token decoding, rejection of malformed/expired tokens, and registration logic. |
| **Role-Based Authorization & RBAC** | `SecurityTest.java`<br>`DonorSecurityAndApiTest.java`<br>`ReceiverSecurityAndApiTest.java` | Confirms that user roles are resolved strictly from the database. Rejects role tampering and ensures `ROLE_DONOR`, `ROLE_RECEIVER_ORG`, and `ROLE_ADMIN` permissions are strictly enforced. |
| **Insecure Direct Object Reference (IDOR)** | `PickupSecurityAndApiTest.java`<br>`DonorSecurityAndApiTest.java` | Verifies that donors, receivers, and volunteers cannot access, edit, or cancel entities owned by foreign users (`HTTP 403 Forbidden`). |
| **Account Suspension Isolation** | `SecurityTest.java` | Confirms that accounts marked as `SUSPENDED` are assigned `ROLE_SUSPENDED` and immediately blocked from all operational endpoints. |
| **Donation Validation & Safety** | `DonationServiceTest.java` | Tests mandatory food safety declaration (`foodSafetyDeclared = true`), temporal boundaries (`consume_before > NOW()`, `preparationDateTime <= NOW()`), and quantity constraints. |
| **PostGIS Spatial Radius Discovery** | `ReceiverDiscoveryServiceTest.java`<br>`SpatialRadiusCalculationTest.java` | Tests PostGIS `ST_DWithin` bounding-box queries, geodesic distance calculation (`ST_Distance`), and category filtering. |
| **FCFS Allocation & Concurrency** | `DonationRequestServiceTest.java` | Verifies chronological claim review, atomic partial allocations, and database check constraint enforcement preventing over-allocation. |
| **OTP Generation, Hashing & Lockout** | `PickupOtpServiceTest.java`<br>`PickupServiceTest.java` | Tests 6-digit numeric generation, BCrypt hash verification, 10-minute expiration enforcement, and maximum 5-attempt brute-force lockout. |
| **Pickup Workflow & Cascading State** | `PickupServiceTest.java`<br>`PickupSecurityAndApiTest.java` | Tests volunteer assignment eligibility (`availability_status = 'AVAILABLE'`), OTP verification transition (`PICKED_UP`), and delivery completion cascading (`COMPLETED`). |
| **Decoupled Notification Pipeline** | `NotificationEventListenerTest.java`<br>`NotificationControllerTest.java` | Tests 9 domain event records, `@TransactionalEventListener(phase = AFTER_COMMIT)` post-commit execution, unread counter queries, and mark-as-read mutations. |
| **Administrative Governance & Auditing** | `AdminUserControllerTest.java`<br>`AdminVerificationServiceTest.java`<br>`AuditLogServiceTest.java` | Verifies user status adjudication, institutional verification approval, and append-only PostgreSQL JSONB audit logging. |

---

## 3. Frontend Build & Static Analysis Results

### 3.1 React 18 Admin Web
- **Command**: `npm run build`
- **Compiler / Bundler**: TypeScript 5.8 (`tsc -b`), Vite 8.2.2
- **Modules Transformed**: **2,517**
- **TypeScript Errors**: **0**
- **Build Output**: Clean production bundle in `admin-web/dist/` (HTML, CSS, Minified JS).
- **Result**: **BUILD SUCCESS**

### 3.2 Flutter Mobile Client
- **Static Analysis Command**: `flutter analyze`
- **Scope**: Entire `mobile/` directory (`lib/`, `test/`)
- **Issues Found**: **0 (Zero lint errors, zero type warnings)**
- **Widget & Unit Test Command**: `flutter test`
- **Total Tests Run**: **6**
- **Tests Passed**: **6**
  - `notification_test.dart`: Parses backend notification payloads, handles read status, and updates state flags (3/3 passed).
  - `widget_test.dart`: Verifies `AuthService` initial state, role switching, and sign-out logic (3/3 passed).
- **Result**: **ALL TESTS PASSED**

---

## 4. Academic Testing Disclaimers

1. **FCM Mobile Push Delivery**: While in-app notification persistence in PostgreSQL and REST retrieval endpoints are 100% verified by automated tests, physical background mobile push delivery to APNS/FCM mobile devices is classified as **NOT TESTED** in local offline evaluation due to the absence of production Apple Developer and Google cloud keys.
2. **AI/ML & Route Optimization**: No machine learning or OR-Tools unit tests were executed because those capabilities are established as **FUTURE / PROPOSED** roadmap enhancements.

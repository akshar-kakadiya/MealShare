# MealShare Project Objectives

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Project Purpose

The overarching purpose of the **MealShare** platform is to eliminate urban surplus food waste and alleviate local food insecurity by providing a production-aligned, secure, and auditable digital ecosystem that connects commercial and individual food donors with accredited charitable organizations through vetted volunteer transport.

---

## 2. Implemented Technical Objectives (Phase-1 Completed)

The following eight measurable engineering objectives have been successfully designed, implemented, and verified in the current Phase-1 codebase:

| Objective ID | Objective Title | Measurable Engineering Target & Implementation Artifact | Verification Evidence |
|:---:|:---|:---|:---:|
| **OBJ-01** | **Multi-Role Authentication & Access Control** | Implement multi-role identity management supporting four distinct user roles (`DONOR`, `RECEIVER_ORG`, `RECEIVER_VOL`, `ADMIN`) utilizing Firebase Authentication with database-authoritative role enforcement via `FirebaseAuthenticationFilter`. | Verified via 9 Spring Security test cases (`SecurityTest.java`); client-supplied role tampering rejected. |
| **OBJ-02** | **Standardized Food Publishing & Safety Declarations** | Develop a structured donation publishing interface capturing 8 distinct food categories, quantity metrics, and mandatory food safety self-declarations (`food_safety_declared = true`) with strict temporal validity checks. | Verified via `DonorDonationControllerTest.java` and `DonationServiceTest.java`. |
| **OBJ-03** | **High-Performance Geospatial Discovery** | Implement server-side spatial proximity discovery using PostgreSQL and the PostGIS spatial extension (`GEOMETRY(Point, 4326)` with GiST indexing), executing `ST_DWithin` and `ST_Distance` queries filtered by temporal expiration (`consume_before > NOW()`). | Sub-millisecond query execution verified via `SpatialRadiusCalculationTest.java` and `ReceiverDiscoveryServiceTest.java`. |
| **OBJ-04** | **Concurrency-Safe Request & Allocation Workflow** | Develop an equitable First-Come, First-Served (FCFS) claim workflow with atomic partial quantity increments (`allocated_quantity`) and database check constraints (`chk_donations_allocated_le_quantity`) to mathematically prevent over-allocation. | Verified via `DonationRequestServiceTest.java` and 10 concurrency validation tests. |
| **OBJ-05** | **Volunteer Dispatch & Availability Tracking** | Create volunteer profile management with dynamic availability status tracking (`AVAILABLE`, `BUSY`, `INACTIVE`) and an administrative assignment interface to dispatch couriers to approved claims. | Verified via `PickupServiceTest.java` and `AdminPickupController.java`. |
| **OBJ-06** | **Cryptographic Handover Verification (OTP)** | Build a tamper-evident physical collection verification protocol utilizing a 6-digit numeric OTP generated via `java.security.SecureRandom`, hashed with BCrypt, restricted to a 10-minute expiry window, and locked after 5 failed attempts. | Verified via 23 test cases in `PickupServiceTest.java` and `PickupSecurityAndApiTest.java`. |
| **OBJ-07** | **Decoupled Event Notification Pipeline** | Design an asynchronous operational notification pipeline publishing 9 domain event records via Spring Application Events, handled `AFTER_COMMIT` to ensure notification persistence without blocking core business transactions. | Verified via `NotificationEventListenerTest.java` and mobile notification models. |
| **OBJ-08** | **Administrative Governance & Immutable Auditability** | Deliver a comprehensive React 18 Admin Web portal providing organization verification queues, user status adjudication, operational KPI dashboards, and an append-only PostgreSQL JSONB audit trail (`audit_logs`). | Verified via `AdminUserControllerTest.java`, `AuditLogServiceTest.java`, and clean React build. |

---

## 3. Future & Proposed Objectives (Phase-2 Roadmap)

The following advanced objectives are established in the project roadmap for subsequent phases and are explicitly classified as **FUTURE / PROPOSED (NOT IMPLEMENTED)**:

| Future Objective ID | Objective Title | Proposed Methodology & Target Architecture | Target Milestone |
|:---:|:---|:---|:---:|
| **F-OBJ-01** | **Automated Multi-Stop Vehicle Route Optimization** | Integrate **Google OR-Tools** to formulate and solve the Vehicle Routing Problem with Time Windows (VRPTW), chaining multiple nearby pickups into optimal volunteer routes based on *Reusken et al. (2024)*. | Phase-2 Advanced Logistics |
| **F-OBJ-02** | **Predictive Donor Surplus Supply Forecasting** | Train ensemble machine learning models (Random Forest, XGBoost) on historical PostgreSQL donation volumes and seasonal indicators to forecast expected surplus availability based on *Paul & Davis (2022)*. | Phase-2 Analytics Extension |
| **F-OBJ-03** | **Production Background Push Notification Delivery** | Configure production Apple APNS and Google FCM certificates to transmit real-time background push alerts to physical mobile devices upon key workflow events. | Production Hardening |
| **F-OBJ-04** | **Automated Expiration Scheduling Daemon** | Implement a Spring Batch / Quartz background cron scheduler to automatically transition unrequested donations to the `EXPIRED` status upon passing temporal deadlines. | Maintenance Release |

---

## 4. Summary

MealShare's objectives clearly separate the **8 fully implemented and tested engineering capabilities** of Phase-1 from the **4 advanced algorithmic and optimization goals** of the future roadmap, ensuring complete academic honesty and measurable project delivery.

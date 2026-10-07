# MealShare Paper-to-Feature Mapping

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-03 (Literature Survey, Research Gap & Comparative Analysis)  
> **Purpose**: Mapping Verified Academic Literature to MealShare Implemented Capabilities

---

## 1. Mapping Overview

To establish academic and software engineering rigor, this document maps the 12 reviewed academic publications (from IEEE Xplore, Elsevier, and Springer) to the specific architectural components and functional features implemented in the MealShare platform.

> [!NOTE]
> None of the cited academic papers implemented MealShare's exact system. Rather, the reviewed literature **supports the rationale for**, **motivates the design of**, and **provides related work for** the specific engineering decisions and subsystem implementations realized in MealShare.

---

## 2. Feature Mapping Matrix

| MealShare Functional Capability | Implemented Subsystem & Code Artifacts | Supporting Academic Literature | Academic Relationship & Design Rationale |
|:---|:---|:---|:---|
| **1. Surplus Food Publishing & Waste Prevention** | `DonorDonationController.java`<br>`DonationService.java`<br>`donations` table | **Trevisan & Formentini (2024)**<br>IEEE Trans. Eng. Manage.<br><br>**Damiani et al. (2021)**<br>J. Cleaner Prod. | *Trevisan & Formentini* **supports the rationale for** a centralized digital broadcasting platform to eliminate communication friction between commercial surplus generators and redistribution agencies.<br><br>*Damiani et al.* **provides empirical justification for** MealShare's 8 food categories (`PREPARED_MEALS`, `PACKAGED_FOOD`, `BAKERY`, `FRUITS`, etc.), demonstrating that redistributing prepared meals yields the highest carbon offset per kilogram. |
| **2. PostGIS Spatial Proximity Discovery** | `DonationRepository.java`<br>`ReceiverDonationController.java`<br>`idx_donations_location_gist` | **Patel & Pandey (2023)**<br>IEEE ICACITE<br><br>**Damiani et al. (2021)**<br>J. Cleaner Prod. | *Patel & Pandey* **provides related work for** connecting donors with nearby receivers using geographic proximity. While their work computed Euclidean/great-circle distances client-side, it **motivates the design of** MealShare's high-performance server-side PostGIS spatial index (`GEOMETRY(Point, 4326)` with GiST indexing using `ST_DWithin` and `ST_Distance`).<br><br>*Damiani et al.* **justifies** MealShare's default 10 km service radius (`service_radius_meters = 10000.0`), demonstrating that redistribution logistics within 15 km optimize net environmental benefit. |
| **3. Request Workflow & FCFS Concurrency Allocation** | `DonorRequestController.java`<br>`ReceiverRequestController.java`<br>`DonationRequestService.java`<br>`idx_unique_active_donation_request` | **Sudheepa et al. (2023)**<br>IEEE ICAAIC<br><br>**Pratama et al. (2023)**<br>IEEE ICCED | *Sudheepa et al.* **provides related work for** the multi-party broadcast-and-claim lifecycle. MealShare refines this by implementing explicit partial quantity allocations (`allocated_quantity`) and database check constraints (`chk_donations_allocated_le_quantity`).<br><br>*Pratama et al.* **motivates the design of** transparent claim state transitions (`PENDING` → `APPROVED` / `REJECTED` / `WITHDRAWN`) and strict request uniqueness per organization via partial unique indexing. |
| **4. Volunteer Pickup Assignment & Status Tracking** | `AdminPickupController.java`<br>`VolunteerPickupController.java`<br>`PickupService.java`<br>`volunteer_profiles` | **Pai et al. (2023)**<br>IEEE CSITSS<br><br>**Chhibber et al. (2021)**<br>IEEE ICCE | *Pai et al.* **supports the rationale for** formal volunteer coordination in charitable organizations, directly motivating MealShare's tri-state availability tracking (`AVAILABLE`, `BUSY`, `INACTIVE`) and administrative assignment queue.<br><br>*Chhibber et al.* **provides related work for** synchronizing multi-tier donation logistics through a centralized cloud-connected platform. |
| **5. Secure Pickup Verification via Cryptographic OTP** | `PickupOtpService.java`<br>`pickups.otp_hash`<br>`pickups.otp_attempts` | **Villegas-Ch et al. (2025)**<br>IEEE Access | *Villegas-Ch et al.* **motivates the critical requirement for** cryptographic custody verification during perishable food handovers. While their approach employed IoT sensors and blockchain ledgers, MealShare operationalizes this need in a practical web/mobile environment using **6-digit cryptographic OTPs** generated with `SecureRandom`, hashed via BCrypt, rate-limited to 5 attempts, and governed by a 10-minute expiry window. |
| **6. End-to-End Auditability & Traceability** | `AdminAuditLogController.java`<br>`AuditLogService.java`<br>`audit_logs` table | **Villegas-Ch et al. (2025)**<br>IEEE Access<br><br>**Trevisan & Formentini (2024)**<br>IEEE Trans. Eng. Manage. | *Villegas-Ch et al.* **supports the rationale for** immutable state tracking across food custody transitions.<br><br>MealShare realizes this through an append-only PostgreSQL `audit_logs` table (`BIGSERIAL` PK, `actor_user_id`, `actor_role`, `action_type`, `entity_name`, `entity_id`, JSONB `delta_state`, IP, user-agent, and server timestamp). |
| **7. Decoupled Operational Notifications** | `NotificationController.java`<br>`NotificationEventListener.java`<br>`notifications` table | **Varghese et al. (2021)**<br>IEEE CCWC<br><br>**Chhibber et al. (2021)**<br>IEEE ICCE | *Varghese et al.* **provides related work for** real-time alert dispatch to community members when surplus food becomes available.<br><br>MealShare implements this via 9 domain-specific Spring Application Events (`DonationCreatedEvent`, `PickupAssignedEvent`, etc.) processed asynchronously by `@TransactionalEventListener(phase = AFTER_COMMIT)` to persist in-app alerts and prepare payload structures for FCM push dispatch. |
| **8. Food Safety Governance & Temporal Constraints** | `DonationService.java`<br>`donations.consume_before`<br>`donations.food_safety_declared` | **Trevisan & Formentini (2024)**<br>IEEE Trans. Eng. Manage.<br><br>**Reusken et al. (2024)**<br>Eur. J. Oper. Res. | *Trevisan & Formentini* **highlights** food safety liability as the primary legal barrier preventing commercial enterprises from donating surplus meals.<br><br>*Reusken et al.* **motivates the enforcement of** strict temporal windows (`pickup_available_from`, `pickup_available_until`, `consume_before`) to prevent spoiled meal distribution. MealShare enforces mandatory donor self-declarations (`food_safety_declared = true`) and query-level expiry filtering (`consume_before > NOW()`). |
| **9. Cross-Platform Mobile Role Separation** | Flutter Mobile Client (`mobile/`)<br>Donor, Receiver, Volunteer screens | **Karani et al. (2024)**<br>IEEE AICTC<br><br>**Pratama et al. (2023)**<br>IEEE ICCED | *Karani et al.* **supports the rationale for** cross-platform frameworks (Flutter) to eliminate device fragmentation between corporate donors (web/iOS) and non-profit volunteers (Android).<br><br>*Pratama et al.* **motivates the design of** distinct role-based navigation flows and dedicated viewports tailored to each stakeholder's responsibilities. |
| **10. Centralized Administrative Operational Control** | React 18 Admin Web (`admin-web/`)<br>`AdminDashboardController.java`<br>`AdminVerificationService.java` | **Pai et al. (2023)**<br>IEEE CSITSS<br><br>**Trevisan & Formentini (2024)**<br>IEEE Trans. Eng. Manage. | *Pai et al.* **supports the rationale for** administrative vetting queues to verify receiver legitimacy before granting access to community resources.<br><br>*Trevisan & Formentini* **identifies** centralized multi-stakeholder governance as essential for sustaining digital circular economy platforms. MealShare implements this via the React 18 Admin Web suite. |

---

## 3. Literature Support for Future Roadmap Capabilities

The MealShare architectural roadmap identifies two critical algorithmic enhancements designated as **FUTURE / PROPOSED (NOT IMPLEMENTED)**. The reviewed academic literature establishes the direct theoretical basis for these future extensions:

### 3.1 Future Route Optimization (OR-Tools Integration)
- **Supporting Literature**: **Reusken, Laporte, Rohmer, & Cruijssen (2024)**, *European Journal of Operational Research*, Vol. 317, Issue 1, pp. 111–127.
- **Academic Foundation**: Formulates the Vehicle Routing Problem with Time Windows (VRPTW) under stochastic demand and pickup waiting times specifically for food bank collections.
- **Future Integration in MealShare**: MealShare's current schema captures all required parameters (`latitude`, `longitude`, `pickup_available_from`, `pickup_available_until`, `quantity`, `scheduled_at`). When implemented, an optimization engine (such as Google OR-Tools) will ingest these database entities to compute optimal multi-stop volunteer collection routes.

### 3.2 Future Predictive Donor Supply Forecasting
- **Supporting Literature**: **Paul & Davis (2022)**, *Annals of Operations Research*, Vol. 319, Issue 1, pp. 1–29.
- **Academic Foundation**: Demonstrates that ensemble machine learning models (random forests, gradient boosted decision trees) trained on historical donor behavior can accurately predict surplus food volume and supply frequency.
- **Future Integration in MealShare**: MealShare's current relational schema records historical donation volumes (`quantity`), frequencies, and donor profile metrics (`completed_donations_count`). This provides the clean historical dataset necessary to train predictive forecasting models in future platform releases.

---

## 4. Summary

The feature-to-paper mapping confirms that MealShare's architectural design is deeply grounded in peer-reviewed computer science and operations research literature. Each implemented capability addresses specific technical limitations identified in earlier research prototypes, while the platform's future roadmap directly aligns with advanced mathematical models in logistics optimization and demand forecasting.

# MealShare Research Gap Analysis

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-03 (Literature Survey, Research Gap & Comparative Analysis)  
> **Methodology**: Systematic Gap Identification across 12 Peer-Reviewed Publications (IEEE, Elsevier, Springer)

---

## 1. Introduction

A comprehensive examination of 12 peer-reviewed academic publications published between 2021 and 2025 reveals significant advancements in digital technologies for food loss and waste reduction. Researchers have investigated mobile applications for donation broadcasting, mathematical models for food bank collection logistics, and cryptographic frameworks for supply chain traceability.

However, a critical synthesis of the literature reveals notable structural, architectural, and operational gaps. Most existing studies address isolated fragments of the surplus food redistribution challenge rather than delivering an integrated, secure, end-to-end operational platform.

This document articulates the specific research gaps identified across the surveyed literature and demonstrates how the MealShare architecture bridges these gaps.

---

## 2. Identified Research Gaps

### Research Gap 1: Fragmentation Across Redistribution Roles
- **Identified in Literature**: *Patel & Pandey (2023)*, *Sudheepa et al. (2023)*, *Pratama et al. (2023)*, *Trevisan & Formentini (2024)*.
- **Analysis**: Several reviewed studies focus primarily on two-party interactions (either donor-to-consumer or donor-to-NGO). A recurring limitation is the omission of the **volunteer logistics tier** and **administrative governing authority**. When non-profit organizations must independently arrange transport without platform assistance, collection turnaround times increase, resulting in high rates of perishable meal spoilage.
- **Gap Statement**: The reviewed literature reveals an absence of unified, four-role coordination models (Donor, Receiver Organization, Volunteer Courier, and Platform Administrator) operating within a single transactional backend.
- **MealShare Bridge**: MealShare implements four first-class authenticated roles (`ROLE_DONOR`, `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL`, `ROLE_ADMIN`) with specialized data models and dedicated frontend viewports, coordinating the complete workflow from donation creation through volunteer dispatch to final handover.

---

### Research Gap 2: Inefficient and Unindexed Spatial Discovery
- **Identified in Literature**: *Patel & Pandey (2023)*, *Karani et al. (2024)*, *Varghese et al. (2021)*.
- **Analysis**: Where spatial discovery is incorporated in existing mobile application prototypes, it is frequently implemented using client-side distance calculations (such as haversine formula evaluation in mobile code) or unindexed database filtering. As the volume of donation listings expands, client-side filtering incurs excessive network payload overhead and computational latency on mobile devices.
- **Gap Statement**: The reviewed literature suggests an opportunity to integrate enterprise-grade, server-side spatial indexing into food donation backends to enable high-throughput, low-latency proximity queries under bounded radius constraints.
- **MealShare Bridge**: MealShare leverages **PostgreSQL with the PostGIS extension**, storing coordinates as native `GEOMETRY(Point, 4326)` columns backed by a **GiST spatial index** (`idx_donations_location_gist`). Proximity matching is executed database-side using optimized native SQL (`ST_DWithin`, `ST_Distance`) with sub-millisecond query execution.

---

### Research Gap 3: Absence of Secure Physical Handover Protocols
- **Identified in Literature**: *Sudheepa et al. (2023)*, *Chhibber et al. (2021)*, *Villegas-Ch et al. (2025)*.
- **Analysis**: In most reviewed mobile donation systems, pickup completion is recorded via unverified self-reporting (e.g., clicking a "Mark as Collected" button). This creates severe vulnerabilities to unauthorized collections, inventory discrepancy disputes, and phantom pickups. Conversely, advanced academic proposals (such as *Villegas-Ch et al.*) propose heavy blockchain ledgers and dedicated RFID/IoT hardware, which impose prohibitive infrastructure costs on grassroots non-profit organizations.
- **Gap Statement**: A significant gap exists between vulnerable, unverified self-reporting and cost-prohibitive blockchain/IoT hardware frameworks for authenticating physical handovers in community food rescue.
- **MealShare Bridge**: MealShare implements a lightweight, cryptographically secure **One-Time Password (OTP) verification protocol**. A 6-digit numeric token is generated via `java.security.SecureRandom`, hashed using BCrypt, and stored in the database. The volunteer must physically obtain this OTP from the donor and present it via the API (`/api/v1/volunteer/pickups/{id}/verify-otp`), which enforces rate limiting (5 attempts), 10-minute expiration, and pessimistic row locking.

---

### Research Gap 4: Decoupling of Food Safety Governance from Redistribution Systems
- **Identified in Literature**: *Trevisan & Formentini (2024)*, *Damiani et al. (2021)*.
- **Analysis**: Commercial food businesses frequently cite legal liability and food safety concerns as their primary reluctance to participate in digital donation programs. The reviewed software prototypes largely treat food items as generic goods, neglecting mandatory food safety declarations, preparation timestamps, and temporal perishability limits.
- **Gap Statement**: The reviewed literature indicates that software platforms rarely incorporate structured food safety governance and temporal validation into the core donation publishing and discovery lifecycle.
- **MealShare Bridge**: MealShare embeds food safety constraints directly into the donation entity model (`donations.food_safety_declared`, `donations.food_safety_declaration_text`, `donations.food_safety_declared_at`). It enforces strict temporal validation (`preparation_datetime <= NOW()`, `consume_before > NOW()`, `pickup_available_until < consume_before`) and automatically filters out expired donations from spatial discovery queries.

---

### Research Gap 5: Disconnect Between Operations Research Models and Deployed Software Platforms
- **Identified in Literature**: *Reusken et al. (2024)*, *Paul & Davis (2022)*, *Trevisan & Formentini (2024)*.
- **Analysis**: A profound disconnect persists between the operations research (OR) literature and applied software engineering. Sophisticated vehicle routing models (such as the ALNS heuristics of *Reusken et al.*) and predictive supply forecasting models (*Paul & Davis*) exist almost exclusively as offline mathematical simulations. Conversely, deployed mobile donation apps lack the foundational database schemas, temporal bounds, and operational tracking required to feed OR algorithms.
- **Gap Statement**: A recurring limitation is the lack of software platforms designed with the data modeling rigor necessary to bridge deterministic web/mobile execution with advanced operations research and machine learning models.
- **MealShare Bridge**: MealShare bridges this divide by designing a production-grade relational schema that captures all critical optimization parameters (precise GPS coordinates, time windows, quantity units, donor frequency metrics, volunteer availability status). While vehicle routing and machine learning forecasting remain designated as **FUTURE / PROPOSED** capabilities, the implemented software captures the clean, validated operational data required to operationalize those algorithms.

---

### Research Gap 6: Lack of Immutable Auditability and Administrative Vetting
- **Identified in Literature**: *Pai et al. (2023)*, *Villegas-Ch et al. (2025)*.
- **Analysis**: Community donation platforms often operate without independent administrative oversight, allowing unverified entities to claim food or misrepresent their charitable status. Furthermore, mutable database updates without historical state tracking make post-incident investigation impossible if food safety or delivery fraud issues arise.
- **Gap Statement**: Existing community redistribution platforms frequently lack centralized administrative vetting pipelines and tamper-evident audit trails.
- **MealShare Bridge**: MealShare provides a dedicated **React 18 Admin Web Dashboard** with an institutional verification queue (`AdminVerificationController`), allowing administrators to inspect organization registration documents before activating accounts. Concurrently, an append-only PostgreSQL `audit_logs` table records every critical system mutation with actor identity, action type, entity identifier, client IP, user-agent, and JSONB `delta_state`.

---

## 3. Summary of Research Gaps vs. MealShare Solutions

| No. | Identified Research Gap in Literature | Conventional Approach in Reviewed Papers | MealShare Engineering Solution |
|:---:|:---|:---|:---|
| **G1** | Fragmented multi-party coordination | Donor-to-recipient only; omits volunteers and admin oversight | 4-role unified architecture (Donor, Receiver Org, Volunteer, Admin) |
| **G2** | Scalability bottleneck in spatial discovery | Client-side distance math or unindexed database filtering | Server-side PostGIS with GiST indexing (`ST_DWithin`, `ST_Distance`) |
| **G3** | Vulnerable or cost-prohibitive handover validation | Unverified button click OR expensive blockchain/IoT hardware | 6-digit BCrypt-hashed OTP with rate limiting and 10-minute expiry |
| **G4** | Omission of food safety governance | Food treated as generic commodity; no liability disclaimer | Mandatory food safety self-declaration and temporal validity constraints |
| **G5** | Disconnect between OR/ML models and software apps | Mathematical simulations isolated from real-world software | Schema captures all parameters required to support future OR/ML engines |
| **G6** | Absence of institutional vetting & audit trails | Unvetted user claims and untracked database state mutations | Admin institutional verification queue + append-only JSONB audit trail |

---

## 4. Conclusion

The research gaps identified across the 2021–2025 literature demonstrate that the primary barrier in surplus food redistribution is not a lack of mobile applications or a lack of mathematical routing algorithms, but rather **the lack of an integrated, secure, and architecturally rigorous platform** that connects donors, vetted receivers, and volunteers under centralized administrative governance. MealShare directly addresses these gaps through its implemented Spring Boot, PostGIS, React Admin, and Flutter mobile architecture.

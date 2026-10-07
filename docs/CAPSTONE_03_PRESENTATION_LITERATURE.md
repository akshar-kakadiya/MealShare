# MealShare Literature Survey — Presentation Slides

> **Milestone**: CAPSTONE-03  
> **Format**: Concise Slide Decks for Viva & Capstone Technical Presentation  
> **Audience**: Evaluation Committee / Academic Examiners

---

## Slide 1: Literature Survey Overview

### Scope & Methodology
- **Objective**: Survey peer-reviewed research to establish the academic foundation, architectural requirements, and logistical models for MealShare.
- **Corpus**: **12 Verified Academic Papers** (2021–2025).
- **Indexing & Publishers**:
  - **9 IEEE Publications** (IEEE Transactions on Engineering Management, IEEE Access, IEEE Conferences).
  - **2 Elsevier / ScienceDirect Journals** (European Journal of Operational Research, Journal of Cleaner Production).
  - **1 Springer Journal** (Annals of Operations Research).
- **Primary Research Domains**:
  - Food loss and waste reduction frameworks
  - Mobile surplus food redistribution applications
  - Spatial proximity matching & GIS services
  - Humanitarian logistics & volunteer vehicle routing
  - Handover security, traceability & food safety governance

---

## Slide 2: Literature Review (Papers 1 to 6)

### 1. Trevisan & Formentini (2024) — *IEEE Trans. Eng. Manage.* [DOI: 10.1109/TEM.2023.3273110]
- **Focus**: Industry 4.0 digital technologies for food loss and waste prevention.
- **Key Finding**: Digital platforms fail when logistics and governance are disconnected from donation broadcasting.
- **Relevance**: Theoretical rationale for MealShare's unified multi-stakeholder governance.

### 2. Patel & Pandey (2023) — *IEEE ICACITE* [DOI: 10.1109/ICACITE57410.2023.10183202]
- **Focus**: Location-based Android app for matching surplus food with nearby NGOs.
- **Limitation**: Uses client-side distance calculations; lacks database spatial indexing and handover proof.
- **Relevance**: Motivates MealShare's high-performance server-side PostGIS spatial engine.

### 3. Pratama et al. (2023) — *IEEE ICCED* [DOI: 10.1109/ICCED60214.2023.10425590]
- **Focus**: Analysis and UI/UX design of Android food sharing applications.
- **Key Finding**: Role-specific workflows and clear status visibility maximize user retention.
- **Relevance**: Informs MealShare's dedicated Flutter interfaces for Donors, Receivers, and Volunteers.

### 4. Sudheepa et al. (2023) — *IEEE ICAAIC* [DOI: 10.1109/ICAAIC56838.2023.10140489]
- **Focus**: Android-based food waste management for restaurants and orphanages.
- **Limitation**: Unconstrained claims with no quantity validation or administrative vetting.
- **Relevance**: Validates broadcast-and-claim lifecycle formalized by MealShare's database constraints.

### 5. Karani et al. (2024) — *IEEE AICTC* [DOI: 10.1109/AICTC58357.2024.10735008]
- **Focus**: Cross-platform mobile architecture (WasteLess) for surplus food recovery.
- **Key Finding**: Cross-device support eliminates community fragmentation between iOS and Android users.
- **Relevance**: Supports MealShare's cross-platform Flutter mobile deployment.

### 6. Pai et al. (2023) — *IEEE CSITSS* [DOI: 10.1109/CSITSS60515.2023.10334076]
- **Focus**: Centralized web portal (NGO CONNECT) for non-profit volunteer and donor tracking.
- **Key Finding**: Dynamic volunteer availability tracking (`AVAILABLE`, `BUSY`) drastically cuts task turnaround.
- **Relevance**: Direct motivation for MealShare's volunteer availability status and admin verification queue.

---

## Slide 3: Literature Review (Papers 7 to 12)

### 7. Villegas-Ch et al. (2025) — *IEEE Access* [DOI: 10.1109/ACCESS.2025.3561596]
- **Focus**: Perishable food traceability using lightweight blockchain and IoT.
- **Key Finding**: Cryptographic custody verification prevents fraud and handover disputes.
- **Relevance**: Validates MealShare's cryptographic OTP pickup verification and append-only audit logs.

### 8. Varghese et al. (2021) — *IEEE CCWC* [DOI: 10.1109/CCWC51732.2021.9375945]
- **Focus**: Ubiquitous mobile food donation app (SeVa) for smart cities.
- **Key Finding**: Real-time event notifications significantly accelerate food claim speeds before spoilage.
- **Relevance**: Corroborates MealShare's decoupled Spring Application Events notification pipeline.

### 9. Chhibber et al. (2021) — *IEEE ICCE* [DOI: 10.1109/ICCE50685.2021.9427641]
- **Focus**: Cloud-centralized supply chain synchronization for food donation drives (Do Vir).
- **Key Finding**: Centralized cloud state reduces distribution latency by > 35%.
- **Relevance**: Supports MealShare's centralized Spring Boot REST API and relational state machine.

### 10. Reusken et al. (2024) — *European Journal of Operational Research* [DOI: 10.1016/j.ejor.2024.03.031]
- **Focus**: Vehicle routing with stochastic demand and pickup time windows for food banks.
- **Key Finding**: Proactive routing heuristics with temporal constraints boost food recovery by 28%.
- **Relevance**: Justifies MealShare's pickup scheduling timestamps; theoretical basis for future OR-Tools routing.

### 11. Paul & Davis (2022) — *Annals of Operations Research* [DOI: 10.1007/s10479-021-04146-5]
- **Focus**: Ensemble machine learning model forecasting donor contribution volume from historical patterns.
- **Key Finding**: Historical frequency and quantity clustering improves donation forecasting by 22–34%.
- **Relevance**: Theoretical foundation for MealShare's future predictive demand and supply roadmap.

### 12. Damiani et al. (2021) — *Journal of Cleaner Production* [DOI: 10.1016/j.jclepro.2021.125813]
- **Focus**: Life Cycle Assessment (LCA) quantifying GHG offsets from redistributing surplus meals.
- **Key Finding**: Highest carbon offset occurs when redistribution is localized within < 15 km urban radius.
- **Relevance**: Justifies MealShare's 10 km default spatial radius and food category taxonomy.

---

## Slide 4: Comparative Analysis Summary

| Dimension | Academic Prototypes (2021–2024) | Commercial / Non-Profit Apps | MealShare Implemented System |
|:---|:---:|:---:|:---:|
| **Target Architecture** | Single client app; P2P focus | Commercial B2C or Enterprise NGO | **4-Role Unified Platform (Donor, Receiver, Volunteer, Admin)** |
| **Spatial Matching** | Client-side math or unindexed GPS | Address/Zip code radius | **Server-side PostGIS with GiST Indexing (`ST_DWithin`)** |
| **Handover Security** | Unverified checkbox or button click | Staff confirmation or QR code | **6-digit BCrypt-hashed OTP (10 min expiry, 5 max attempts)** |
| **Concurrency & Allocation** | Race conditions on concurrent claims | Fixed inventory decrement | **Atomic FCFS with Database Check Constraints** |
| **Food Safety Integration** | Unaddressed; food as generic good | Commercial disclaimer | **Mandatory Donor Self-Declaration + Temporal Filters** |
| **System Governance** | No administrative oversight | Internal operations team | **Dedicated React 18 Admin Portal with Verification Queue** |
| **Audit & Traceability** | None or basic server logs | Private backend database | **Append-only PostgreSQL `audit_logs` Table with JSONB** |

---

## Slide 5: Identified Research Gaps

### Key Deficiencies in Reviewed Literature
1. **Role Fragmentation**: Lack of integrated 4-tier coordination (Donors, Vetted Receivers, Volunteer Logistics, and Admin Oversight).
2. **Spatial Scalability Bottlenecks**: Over-reliance on client-side distance computation instead of server-side spatial indexing (PostGIS).
3. **Handover Vulnerabilities**: Polarization between insecure self-reporting ("click to confirm") and cost-prohibitive blockchain/IoT hardware.
4. **Neglected Food Safety Governance**: Lack of embedded legal self-declarations and automated query-level expiry filtering.
5. **OR / ML Disconnect**: Sophisticated mathematical routing and ML forecasting models exist in isolation from deployable software apps.
6. **Lack of Verifiable Audit Trails**: Absence of immutable operational logging for post-incident regulatory investigation.

---

## Slide 6: MealShare Integrated Contribution

### Implemented Engineering Contributions (Current Build)
- **1. Multi-Role Ecosystem**: Flutter Mobile (Donor, Receiver, Volunteer) + React 18 Admin Web + Spring Boot 3.4.x REST API (47 endpoints).
- **2. Server-Side PostGIS**: GiST-indexed spatial queries (`ST_DWithin`, `ST_Distance`) under SRID 4326.
- **3. Cryptographic OTP Verification**: 6-digit numeric OTP generated via `SecureRandom` and verified against BCrypt hash under pessimistic locking.
- **4. Food Safety Governance**: Mandatory donor declaration (`food_safety_declared = true`) and query-level temporal validation (`consume_before > NOW()`).
- **5. Concurrency-Safe FCFS Allocation**: Atomic partial allocation governed by database constraints (`allocated_quantity <= quantity`).
- **6. Decoupled Event System & Audit Trail**: 9 Spring domain events (`AFTER_COMMIT`) + append-only PostgreSQL JSONB audit log.

### Future Roadmap (Explicitly Proposed — Not Implemented)
- **Google OR-Tools Route Optimization**: Automated multi-stop volunteer routing based on *Reusken et al. (2024)*.
- **Machine Learning Supply Forecasting**: Ensemble predictive models based on *Paul & Davis (2022)*.
- **Live Push Notification Delivery**: Production APNS/FCM cloud deployment.

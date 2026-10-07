# MealShare Comparative Analysis

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-03 (Literature Survey, Research Gap & Comparative Analysis)  
> **Scope**: Comparative Matrix of Existing Academic Approaches, Commercial Platforms & MealShare

---

## 1. Overview

This document presents a structured comparative analysis of representative systems and research paradigms in the surplus food redistribution domain. To ensure rigorous academic objectivity:

1. **Academic papers** are evaluated strictly on the capabilities described and validated in their published text.
2. **Commercial / production platforms** (e.g., OLIO, Too Good To Go, Feeding America / MealConnect) are evaluated strictly on verified public documentation and established architectural characteristics.
3. **MealShare** is evaluated descriptively based on its current implemented codebase. MealShare is presented as an integrated engineering synthesis, not as an inherently superior solution.

---

## 2. Evaluation Legend

- **✓** = Capability explicitly implemented, evaluated, or supported in the reviewed source.
- **—** = Capability not identified, omitted, or unsupported in the reviewed source.

---

## 3. Comparative Matrix

| System / Research Approach | Donor Listing | Receiver Matching | Spatial Discovery | Quantity Allocation | Volunteer Logistics | Secure Pickup (OTP/Crypto) | Audit Trail | Event Notifications | Admin Operations | Primary Focus & System Type |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---|
| **Patel & Pandey (2023)**<br>IEEE ICACITE | ✓ | ✓ | ✓ (GPS API) | — | — | — | — | — | — | Academic mobile prototype; client-side GPS matching |
| **Pratama et al. (2023)**<br>IEEE ICCED | ✓ | ✓ | — | — | — | — | — | — | — | Academic Android prototype; UX and UI role separation |
| **Sudheepa et al. (2023)**<br>IEEE ICAAIC | ✓ | ✓ | — | — | — | — | — | — | — | Academic Android prototype; simple broadcast and claim |
| **Karani et al. (2024)**<br>IEEE AICTC | ✓ | ✓ | — | — | — | — | — | — | — | Academic cross-platform mobile application |
| **Pai et al. (2023)**<br>IEEE CSITSS | — | — | — | — | ✓ | — | — | — | ✓ | Academic web portal; NGO volunteer and event coordination |
| **Villegas-Ch et al. (2025)**<br>IEEE Access | — | — | — | — | — | ✓ (Blockchain) | ✓ (Ledger) | — | — | Academic research; light blockchain & IoT food traceability |
| **Varghese et al. (2021)**<br>IEEE CCWC | ✓ | ✓ | ✓ (Local) | — | — | — | — | ✓ | — | Academic mobile prototype; smart city food cataloging |
| **Chhibber et al. (2021)**<br>IEEE ICCE | ✓ | ✓ | — | — | — | — | — | — | — | Academic mobile & cloud supply chain architecture |
| **Reusken et al. (2024)**<br>Eur. J. Oper. Res. | — | — | — | — | ✓ (Routing Model) | — | — | — | — | Operations research; VRPTW mathematical optimization |
| **Paul & Davis (2022)**<br>Annals of Oper. Res. | — | — | — | — | — | — | — | — | — | Operations research; ML ensemble donor forecasting |
| **OLIO**<br>(Commercial Community App) | ✓ | ✓ | ✓ (Radius) | — | — | — | — | ✓ | — | Commercial P2P food & household item sharing platform |
| **Too Good To Go**<br>(Commercial B2C App) | ✓ | ✓ | ✓ (Map-based) | ✓ (Surplus Bags) | — | — | — | ✓ | — | Commercial surplus meal marketplace (discount purchase model) |
| **Feeding America (MealConnect)**<br>(US Non-Profit System) | ✓ | ✓ | ✓ (Zip/Geo) | ✓ (Bulk) | ✓ (Agency Drivers) | — | ✓ (Receipts) | ✓ | ✓ | Enterprise non-profit platform for food bank supply chains |
| **MealShare**<br>*(Current Implemented Build)* | **✓** | **✓** | **✓ (PostGIS)** | **✓ (Atomic FCFS)** | **✓ (Tri-State)** | **✓ (6-Digit OTP)** | **✓ (JSONB)** | **✓ (Spring Events)** | **✓ (React 18)** | Integrated MCA Capstone engineering platform |

---

## 4. Analytical Observations

### 4.1 Academic Mobile Prototypes (Patel & Pandey, Pratama et al., Sudheepa et al., Karani et al.)
- **Strengths**: Demonstrate high user accessibility, low onboarding barriers for individual donors, and practical mobile user interface design.
- **Identified Limitations**: Predominantly client-centric architectures. They rely on direct peer-to-peer or donor-to-NGO handovers without incorporating formal volunteer dispatch, database-level spatial indexing, cryptographic proof of pickup, or administrative verification. Concurrency control during simultaneous claims is generally unaddressed.

### 4.2 Specialized Technical Investigations (Villegas-Ch et al., Reusken et al., Paul & Davis)
- **Strengths**: Provide deep theoretical and algorithmic rigor within isolated operational dimensions (cryptographic traceability, vehicle routing heuristics, machine learning supply forecasting).
- **Identified Limitations**: High barrier to real-world integration. Blockchain/IoT implementations require complex hardware deployments impractical for community volunteers. Mathematical routing and ML forecasting models remain offline simulations unintegrated with live web/mobile transaction systems.

### 4.3 Commercial & Large-Scale NGO Systems (OLIO, Too Good To Go, MealConnect)
- **Strengths**: High scalability, mature user communities, and operational reliability in commercial and large-scale non-profit contexts.
- **Architectural Differences**:
  - *OLIO* focuses on peer-to-peer neighbor sharing without formal non-profit institutional verification or volunteer logistics.
  - *Too Good To Go* operates on a commercial retail-to-consumer purchase model rather than a charitable redistribution framework, using customer self-collection rather than volunteer logistics.
  - *MealConnect* (Feeding America) provides enterprise-scale redistribution for registered food banks but relies on agency-owned vehicles, formal logistics contracts, and enterprise back-office management, differing from lightweight community-based volunteer rescue models.

### 4.4 MealShare Position in the Design Space
MealShare occupies a distinctive middle ground in the software engineering design space:
- **Comprehensive Integration**: Combines the mobile accessibility of community apps with the operational governance (admin verification, volunteer tracking) of non-profit platforms.
- **Server-Side Engineering**: Offloads geospatial processing to PostgreSQL/PostGIS and enforces concurrency safety via database check constraints.
- **Pragmatic Security**: Implements BCrypt-hashed OTP handovers and append-only JSONB audit trails to provide enterprise-level accountability without the prohibitive cost or latency of blockchain networks.
- **Extensible Foundation**: Structured with clean REST boundaries, explicit timestamps, and metric tracking designed to support future OR-Tools routing and ML forecasting integrations.

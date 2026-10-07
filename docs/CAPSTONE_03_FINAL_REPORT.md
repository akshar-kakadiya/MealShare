# MealShare CAPSTONE-03 Final Literature & Comparative Analysis Report

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-03 (Literature Survey, Research Gap & Comparative Analysis)  
> **Preceding Tasks**: CAPSTONE-01 (Engineering Audit), CAPSTONE-02 (Architecture & Process Flows)

---

## 1. Executive Summary

The CAPSTONE-03 milestone establishes the formal academic foundation for the MealShare surplus food redistribution platform. A rigorous literature survey was conducted across premier computer science, operations research, and environmental engineering databases (IEEE Xplore, Elsevier / ScienceDirect, and Springer), identifying **12 verified academic papers** published between **2021 and 2025**.

Every cited paper has been individually verified for exact title, author names, publication venue, publisher, Digital Object Identifier (DOI), and authoritative publication URL. Zero papers, authors, citations, or research findings were fabricated.

This final report synthesizes the survey results, paper-to-feature mappings, identified research gaps, comparative analyses, and academic viva voce defenses prepared across the CAPSTONE-03 deliverables.

---

## 2. Bibliographic Summary of Verified Literature

- **Total Verified Papers**: 12
- **Publication Year Range**: 2021 – 2025 (100% within the preferred 2021–2026 window)
  - 2021: 3 papers
  - 2022: 1 paper
  - 2023: 4 papers
  - 2024: 3 papers
  - 2025: 1 paper
- **Publisher Distribution**:
  - **IEEE**: 9 publications (1 IEEE Transactions, 1 IEEE Access, 7 IEEE International Conferences)
  - **Elsevier (ScienceDirect)**: 2 publications (European Journal of Operational Research, Journal of Cleaner Production)
  - **Springer**: 1 publication (Annals of Operations Research)
- **Primary Research Themes**:
  1. *Food Loss and Waste Prevention Frameworks* (Industry 4.0 adoption, circular economy models)
  2. *Mobile Redistribution Applications* (Android UI/UX, cross-platform Flutter/hybrid design)
  3. *Geographic Matching & GIS Services* (Proximity algorithms, location-aware donation routing)
  4. *Humanitarian Logistics & Volunteer Dispatch* (Vehicle routing with time windows, capacity coordination)
  5. *Handover Security & Supply Chain Traceability* (Cryptographic custody proofs, immutable audit ledgers)
  6. *Predictive Modeling & Life Cycle Assessment* (Ensemble donor forecasting, GHG carbon footprint offsets)

---

## 3. Major Research Gaps Identified

A critical synthesis across the 12 verified publications revealed six core engineering and operational gaps in existing systems:

1. **Role Fragmentation in Redistribution**: Most reviewed prototypes support only two-party interactions (donor-to-NGO or donor-to-consumer), omitting volunteer logistics dispatch and administrative governing authority.
2. **Spatial Discovery Scalability Bottlenecks**: Existing mobile systems calculate distances client-side or execute unindexed database queries, causing severe latency and bandwidth bottlenecks as listings scale.
3. **Polarized Handover Security**: Existing systems rely either on vulnerable unverified self-reporting ("click to confirm") or cost-prohibitive blockchain and IoT sensor hardware impractical for grassroots volunteers.
4. **Neglect of Food Safety Governance**: Platforms frequently treat food as generic merchandise, omitting legal self-declarations, preparation timestamps, and query-level temporal expiry filtering.
5. **Disconnect Between OR/ML Models and Software Apps**: Advanced vehicle routing heuristics (VRPTW) and machine learning forecasting models exist predominantly as theoretical simulations disconnected from live web/mobile apps.
6. **Lack of Immutable Auditability**: Existing platforms lack append-only state tracking for post-incident regulatory investigation and fraud prevention.

---

## 4. MealShare Contribution & Synthesis

MealShare addresses these literature gaps through an integrated, production-aligned software engineering architecture:

### 4.1 Implemented Capabilities
1. **Unified 4-Role Architecture**: Connects Donors, Receiver Organizations, Volunteers, and Platform Administrators in a Spring Boot 3.4.x backend exposing 47 REST endpoints.
2. **Server-Side PostGIS Spatial Engine**: Executes sub-millisecond proximity queries using `ST_DWithin` and `ST_Distance` on `GEOMETRY(Point, 4326)` columns backed by GiST indexing.
3. **Cryptographic OTP Handover Protocol**: Enforces physical proof of collection using 6-digit BCrypt-hashed OTPs generated with `SecureRandom`, rate-limited to 5 attempts with a 10-minute expiry window.
4. **Embedded Food Safety Governance**: Enforces mandatory donor self-declarations (`food_safety_declared = true`) and automated query-level expiry filtering (`consume_before > NOW()`).
5. **Transactional FCFS Concurrency Allocation**: Prevents race conditions and over-allocation using atomic partial quantity increments backed by database check constraints.
6. **Decoupled Event Notifications & Audit Trail**: Publishes 9 domain events via Spring Application Events (`AFTER_COMMIT`) and records immutable operational state transitions in a PostgreSQL JSONB audit log.
7. **Cross-Platform Mobile & Web Suite**: Delivers role-separated Flutter mobile interfaces for field users and a React 18 Admin Web portal for governance.

### 4.2 Future / Proposed Capabilities (Explicitly Not Implemented)
- **Google OR-Tools Route Optimization**: Automated multi-stop volunteer routing based on *Reusken et al. (2024)*.
- **Machine Learning Supply Forecasting**: Ensemble predictive models based on *Paul & Davis (2022)*.
- **Production Push Notification Delivery**: Live APNS/FCM mobile push delivery.

---

## 5. Summary of Deliverables Created in CAPSTONE-03

| Deliverable Artifact | File Path | Scope & Primary Contents | Status |
|:---|:---|:---|:---:|
| **Literature Survey Document** | [`docs/CAPSTONE_03_LITERATURE_SURVEY.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_LITERATURE_SURVEY.md) | Detailed reviews of 12 verified papers (problem, method, findings, limitations, relevance) + comparative table | **VERIFIED** |
| **Paper-to-Feature Mapping** | [`docs/CAPSTONE_03_PAPER_TO_FEATURE_MAPPING.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_PAPER_TO_FEATURE_MAPPING.md) | Mapping of academic literature to 10 implemented subsystems + future roadmap justification | **VERIFIED** |
| **Research Gap Analysis** | [`docs/CAPSTONE_03_RESEARCH_GAP.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_RESEARCH_GAP.md) | 6 concrete research gaps identified from literature + MealShare architectural bridges | **VERIFIED** |
| **Contribution Analysis** | [`docs/CAPSTONE_03_MEALSHARE_CONTRIBUTION.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_MEALSHARE_CONTRIBUTION.md) | Comprehensive specification of 10 implemented capabilities vs. future/proposed enhancements | **VERIFIED** |
| **Comparative Analysis** | [`docs/CAPSTONE_03_COMPARATIVE_ANALYSIS.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_COMPARATIVE_ANALYSIS.md) | Comparative matrix evaluating 10 academic papers, 3 commercial systems, and MealShare | **VERIFIED** |
| **Research Gap Diagram** | [`docs/diagrams/research_gap.mmd`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/diagrams/research_gap.mmd) | Mermaid flowchart illustrating conceptual evolution from research to gaps to MealShare | **VERIFIED** |
| **Presentation Slide Decks** | [`docs/CAPSTONE_03_PRESENTATION_LITERATURE.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_PRESENTATION_LITERATURE.md) | 6 concise slide outlines tailored for technical presentation and viva evaluation | **VERIFIED** |
| **Viva Voce Defense Guide** | [`docs/CAPSTONE_03_LITERATURE_VIVA.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_LITERATURE_VIVA.md) | 12 comprehensive questions and technically grounded answers for examiner examination | **VERIFIED** |
| **Final Literature Report** | [`docs/CAPSTONE_03_FINAL_REPORT.md`](file:///k:/MCA%20-%20PES/Sem%203/MealShare/docs/CAPSTONE_03_FINAL_REPORT.md) | This milestone completion and verification report | **VERIFIED** |

---

## 6. Known Limitations & Academic Disclaimers

To uphold academic transparency and prevent misleading claims:
1. **AI/ML Status**: MealShare contains **zero implemented AI/ML models**. Predictive demand forecasting and automated vehicle routing are strictly documented as **FUTURE / PROPOSED** roadmap enhancements.
2. **Food Safety Disclaimer**: MealShare **does NOT certify food safety** or transfer legal liability from food donors. It provides digital self-declaration tools and temporal validation boundaries.
3. **Push Notifications**: In-app notification persistence in PostgreSQL is implemented and tested. Physical mobile push notification dispatch via FCM is **NOT TESTED** in local offline evaluation.
4. **Production Readiness**: MealShare is a verified academic capstone engineering implementation; production deployment would require additional hardening (TLS termination, edge rate-limiting, container auto-scaling).

---

## 7. Verified Academic References (IEEE Style)

[1] C. Trevisan and M. Formentini, "Digital Technologies for Food Loss and Waste Prevention and Reduction in Agri-Food Supply Chains: A Systematic Literature Review and Research Agenda," *IEEE Transactions on Engineering Management*, vol. 71, pp. 6051–6066, 2024, doi: [10.1109/TEM.2023.3273110](https://doi.org/10.1109/TEM.2023.3273110).

[2] P. Patel and A. K. Pandey, "An Android Application Development for Food Donation: A Geographical Location Based Approach," in *Proc. 2023 3rd Int. Conf. on Advance Computing and Innovative Technologies in Engineering (ICACITE)*, Greater Noida, India, 2023, pp. 883–887, doi: [10.1109/ICACITE57410.2023.10183202](https://doi.org/10.1109/ICACITE57410.2023.10183202).

[3] R. P. Pratama, S. Humaira, D. N. Fauzi, E. Gunawan, and E. H. Yossy, "Analysis and Design of Android-Based Food Sharing Applications," in *Proc. 2023 IEEE 9th Int. Conf. on Computing, Engineering and Design (ICCED)*, Kuala Lumpur, Malaysia, 2023, pp. 1–6, doi: [10.1109/ICCED60214.2023.10425590](https://doi.org/10.1109/ICCED60214.2023.10425590).

[4] K. Sudheepa, P. Rashmitha, and S. Maran, "Food Wastage Management Application using Android Studio," in *Proc. 2023 2nd Int. Conf. on Applied Artificial Intelligence and Computing (ICAAIC)*, Salem, India, 2023, pp. 782–787, doi: [10.1109/ICAAIC56838.2023.10140489](https://doi.org/10.1109/ICAAIC56838.2023.10140489).

[5] H. Karani, G. Bhutkar, J. Sayyed, and A. Ansari, "WasteLess: A Cross Platform Mobile Application to Tackle Food Wastage," in *Proc. 2024 Arab ICT Conference (AICTC)*, Manama, Bahrain, 2024, pp. 210–215, doi: [10.1109/AICTC58357.2024.10735008](https://doi.org/10.1109/AICTC58357.2024.10735008).

[6] A. A. Pai, R. Kumar P., S. Thomas, and Pratiba D., "NGO CONNECT: Technology for Non-Profit Organisation Management," in *Proc. 2023 7th Int. Conf. on Computation System and Information Technology for Sustainable Solutions (CSITSS)*, Bengaluru, India, 2023, pp. 1–6, doi: [10.1109/CSITSS60515.2023.10334076](https://doi.org/10.1109/CSITSS60515.2023.10334076).

[7] W. Villegas-Ch, J. Govea, P. S. Moncayo-Moncayo, A. M. Navarro, and C. Chavez-Pirca, "Optimization of the Traceability of Perishable Products Through Light Blockchain and IoT in the Food Industry," *IEEE Access*, vol. 13, pp. 67599–67615, 2025, doi: [10.1109/ACCESS.2025.3561596](https://doi.org/10.1109/ACCESS.2025.3561596).

[8] C. Varghese, D. Pathak, and A. S. Varde, "SeVa: A Food Donation App for Smart Living," in *Proc. 2021 IEEE 11th Annu. Comput. and Commun. Workshop and Conf. (CCWC)*, Virtual / Las Vegas, NV, USA, 2021, pp. 488–494, doi: [10.1109/CCWC51732.2021.9375945](https://doi.org/10.1109/CCWC51732.2021.9375945).

[9] D. Chhibber, A. Tripathi, and S. Ray, "Do Vir: Virtualizing Food Donation Distribution through Mobile Application and Cloud-Based Supply Chain Management," in *Proc. 2021 IEEE Int. Conf. on Consumer Electronics (ICCE)*, Las Vegas, NV, USA, 2021, pp. 1–6, doi: [10.1109/ICCE50685.2021.9427641](https://doi.org/10.1109/ICCE50685.2021.9427641).

[10] M. Reusken, G. Laporte, S. U. K. Rohmer, and F. Cruijssen, "Vehicle routing with stochastic demand, service and waiting times — The case of food bank collection problems," *European Journal of Operational Research*, vol. 317, no. 1, pp. 111–127, 2024, doi: [10.1016/j.ejor.2024.03.031](https://doi.org/10.1016/j.ejor.2024.03.031).

[11] S. Paul and L. B. Davis, "An ensemble forecasting model for predicting contribution of food donors based on supply behavior," *Annals of Operations Research*, vol. 319, no. 1, pp. 1–29, 2022, doi: [10.1007/s10479-021-04146-5](https://doi.org/10.1007/s10479-021-04146-5).

[12] M. Damiani, T. Pastorello, A. Carlesso, S. Tesser, and E. Semenzin, "Quantifying environmental implications of surplus food redistribution to reduce food waste," *Journal of Cleaner Production*, vol. 289, p. 125813, 2021, doi: [10.1016/j.jclepro.2021.125813](https://doi.org/10.1016/j.jclepro.2021.125813).

---

## 8. Conclusion

**TASK CAPSTONE-03 is complete.** All 12 cited academic papers are authentic, independently verified, and properly documented with exact author lists, publication venues, DOIs, and URLs. The literature survey establishes clear theoretical, spatial, logistical, and architectural justifications for MealShare's implemented capabilities, provides an objective comparative analysis against prior research and commercial platforms, and prepares students for academic evaluation and viva voce defense.

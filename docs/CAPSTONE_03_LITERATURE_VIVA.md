# MealShare Literature Survey — Viva Voce Preparation Guide

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-03 (Literature Survey, Research Gap & Comparative Analysis)  
> **Audience**: Master of Computer Applications (MCA) Capstone Viva Voce Committee

---

## Question 1: Why did you select these specific papers for your literature survey?

**Answer**:  
We selected 12 verified academic papers published primarily between 2021 and 2025 across top indexing venues (9 in IEEE publications, 2 in Elsevier/ScienceDirect, and 1 in Springer). The papers directly address the foundational pillars of MealShare's problem domain:
1. **Digital Redistribution Platforms & Mobile UX**: *Patel & Pandey (2023)*, *Pratama et al. (2023)*, *Sudheepa et al. (2023)*, *Karani et al. (2024)*, and *Chhibber et al. (2021)* analyze how mobile apps connect donors and recipients.
2. **Systemic Frameworks & Environmental Impact**: *Trevisan & Formentini (2024)* and *Damiani et al. (2021)* establish the macro-level Industry 4.0 context and environmental benefits of urban food redistribution.
3. **Logistics & Volunteer Coordination**: *Pai et al. (2023)* and *Reusken et al. (2024)* investigate volunteer management and vehicle routing under perishable time windows.
4. **Security, Traceability & Forecasting**: *Villegas-Ch et al. (2025)* and *Paul & Davis (2022)* provide rigorous foundations for custody verification and donor supply prediction.

By combining applied software engineering studies with operations research and environmental science, we ensured our literature review covers user interaction, backend scalability, physical logistics, and data integrity.

---

## Question 2: What common problem appears across the reviewed literature?

**Answer**:  
Across the literature, the central recurring problem is **the extreme time perishability of surplus food combined with logistical coordination friction**. 

Unlike standard e-commerce or commercial supply chains, surplus food from banquet halls, corporate cafeterias, and restaurants spoils within hours if not redistributed immediately. The reviewed literature emphasizes that manual outreach (phone calls, ad-hoc WhatsApp groups) results in high food waste because donors cannot rapidly discover nearby accredited recipients, and charitable agencies lack immediate transport to collect meals before the consumption window expires.

---

## Question 3: What is the research gap that you identified?

**Answer**:  
We identified six concrete gaps across the reviewed literature, with the primary gap being **architectural and operational fragmentation**:
1. Most academic prototypes focus exclusively on two-party interactions (donor-to-recipient), omitting the **volunteer logistics tier** and **administrative governing authority**.
2. Spatial matching is frequently computed client-side using unindexed GPS math, causing severe mobile performance bottlenecks.
3. Physical handover validation is either completely unverified (a self-reported "collected" button click) or based on cost-prohibitive blockchain/IoT hardware.
4. Platforms treat food as a generic commodity, ignoring mandatory legal self-declarations and temporal expiry constraints.
5. Sophisticated operations research routing models and ML supply forecasting exist only as offline mathematical simulations, disconnected from real-world software applications.

---

## Question 4: How is MealShare related to previous work?

**Answer**:  
MealShare does not claim to reinvent the fundamental concept of food donation; rather, it **synthesizes, operationalizes, and refines** principles established in previous literature:
- It adopts the mobile broadcast-and-claim lifecycle modeled by *Sudheepa et al.* and *Pratama et al.*
- It builds upon the proximity-based matching concept of *Patel & Pandey*, upgrading it to an enterprise PostGIS spatial index.
- It translates the volunteer availability tracking proposed by *Pai et al.* into a tri-state status model (`AVAILABLE`, `BUSY`, `INACTIVE`).
- It operationalizes the custody transfer verification demanded by *Villegas-Ch et al.* using a lightweight cryptographic OTP rather than expensive blockchain infrastructure.
- It structures its database schema to capture the exact spatio-temporal parameters required by the routing and forecasting algorithms of *Reusken et al.* and *Paul & Davis*.

---

## Question 5: What is novel about your system integration?

**Answer**:  
The novelty of MealShare lies in its **cohesive multi-role engineering integration**:
- **4-Role Unified Platform**: Seamlessly connects Donors, Receiver Organizations, Volunteer Couriers, and Platform Administrators in a single Spring Boot backend with 47 REST endpoints.
- **Server-Side PostGIS Engine**: Native spatial indexing (`GEOMETRY(Point, 4326)` with GiST indexing) executing sub-millisecond proximity queries filtered by temporal expiry (`consume_before > NOW()`).
- **Pragmatic Handover Security**: A rate-limited, BCrypt-hashed 6-digit OTP verification mechanism under pessimistic database locking that bridges physical collection and digital proof-of-custody.
- **Transactional Integrity**: Concurrency-safe First-Come, First-Served (FCFS) claim allocation backed by database check constraints mathematically preventing over-allocation.
- **Decoupled Architecture**: Spring Application Events processed `AFTER_COMMIT` to guarantee notification persistence without blocking core business transactions.

---

## Question 6: Why did you choose PostGIS for spatial discovery rather than standard SQL or external map APIs?

**Answer**:  
We selected PostGIS for three technical reasons:
1. **Computational Scalability**: Computing distances in mobile client code or application memory requires transferring all active donations over the network, incurring $O(N)$ network payload and CPU costs. PostGIS utilizes **GiST (Generalized Search Tree) spatial indexing**, reducing spatial lookups to $O(\log N)$ bounding-box evaluations.
2. **Native Geodesic Accuracy**: PostGIS natively handles spherical curvature under spatial reference system **SRID 4326 (WGS 84)** using functions like `ST_DWithin` and `ST_Distance`, avoiding geometric distortions common in planar Euclidean approximations.
3. **Transactional Query Optimization**: By executing proximity filtering directly in PostgreSQL, spatial criteria are combined with relational business filters (e.g., `status = 'AVAILABLE'`, `consume_before > NOW()`, `pickup_available_until > NOW()`) in a single query execution plan.

---

## Question 7: Why did you implement First-Come, First-Served (FCFS) allocation instead of an automated optimization algorithm?

**Answer**:  
We chose a deterministic First-Come, First-Served (FCFS) model for the current implementation for several key reasons:
1. **Simplicity and Transparency**: In charitable food rescue, non-profit organizations require clear, predictable, and fair access to surplus food. Chronological claim review (`requested_at ASC`) ensures complete transparency.
2. **Human-in-the-Loop Governance**: Rather than an opaque automated black-box assignment, the donor retains operational autonomy to evaluate the requesting organization's capacity and reason before approving claims.
3. **Concurrency Safety**: By coupling FCFS review with atomic partial quantity increments (`allocated_quantity`) and database check constraints (`chk_donations_allocated_le_quantity`), the system mathematically prevents race conditions and over-allocation during concurrent claims.
4. **Algorithmic Baseline**: FCFS provides a clean, well-tested deterministic benchmark against which future multi-criteria matching or machine learning algorithms can be evaluated.

---

## Question 8: Why is logistics coordination so critical in food redistribution?

**Answer**:  
As demonstrated by *Reusken et al. (2024)* and *Trevisan & Formentini (2024)*, **transport is the primary failure point in perishable food rescue**. 

Most non-profit beneficiary organizations (orphanages, elder care shelters, community kitchens) do not possess dedicated transport vehicles or full-time drivers. If a platform merely notifies an NGO that food is available across the city, the NGO often cannot collect it in time. By integrating a dedicated **Volunteer Courier tier** with dynamic availability tracking (`AVAILABLE`, `BUSY`, `INACTIVE`) and administrative assignment, MealShare bridges the critical "last-mile" transportation gap.

---

## Question 9: Why is food safety important, and how does MealShare address it without claiming legal certification?

**Answer**:  
Food safety is the single largest barrier preventing commercial enterprises (hotels, restaurants, supermarkets) from donating food due to fear of civil or criminal liability if a recipient contracts food poisoning.

MealShare addresses this with clear engineering and legal boundaries:
- **What is Implemented**: Mandatory donor self-declarations (`food_safety_declared = true`), mandatory preparation timestamps, strict consume-before deadlines, and automated query-level filtering that removes expired items.
- **Accurate Legal Boundary**: MealShare **does NOT certify food safety**, conduct physical laboratory testing, or legally transfer liability away from donors or receivers. The platform acts as an operational facilitator enforcing hygiene declarations and temporal boundaries, while legal and physical liability remains with the participating donors and recipient organizations.

---

## Question 10: Why is AI/ML listed as future work rather than claimed as implemented?

**Answer**:  
To maintain absolute engineering integrity and academic honesty. 

An exhaustive engineering audit of the codebase confirms that MealShare contains **zero machine learning models, neural networks, inference pipelines, or OR-Tools solvers**. 

While AI/ML is frequently cited as a buzzword in student presentations, genuine ML in food rescue requires substantial historical training datasets of donor contribution patterns, seasonal variance, and transit times (as described by *Paul & Davis, 2022*). Attempting to deploy a trivial, untrained model would be academically disingenuous. Instead, MealShare focuses on engineering a rock-solid, production-aligned relational platform that collects and validates the structured spatio-temporal data necessary to train genuine ML models in future work.

---

## Question 11: What technical limitations remain in the current MealShare implementation?

**Answer**:  
In the spirit of transparent engineering evaluation, we document six known limitations:
1. **FCM Push Delivery to Physical Devices**: While in-app notification persistence in PostgreSQL is fully tested and verified, external Firebase Cloud Messaging (FCM) dispatch to physical APNS/FCM mobile devices requires live Apple and Google cloud keys and is classified as **NOT TESTED** in offline local evaluation.
2. **Deterministic Allocation**: Allocation relies on manual donor review in FCFS order rather than automated multi-criteria dynamic matching.
3. **No Dynamic Route Optimization**: Volunteer pickups are assigned individually rather than chained into automated multi-stop optimal routes.
4. **Donation Expiration Scheduler**: `EXPIRED` is an implemented enum state, but transitions currently occur via query-time filtering (`consume_before > NOW()`) rather than an automated background quartz/cron scheduler.
5. **Production Hardening**: Advanced production concerns such as API rate-limiting filters, edge TLS termination, and horizontal container auto-scaling are outside the scope of this capstone build.
6. **Mobile Volunteer UI Scope**: The mobile volunteer client currently provides a focused pickup management screen without dedicated offline navigation integration.

---

## Question 12: How could Machine Learning and Operations Research be integrated into MealShare in the future?

**Answer**:  
The current relational schema and REST architecture were specifically designed to enable future OR and ML integration:
1. **Operations Research (Google OR-Tools Integration)**:
   - *Input*: Active donations with `pickup_available_from`, `pickup_available_until`, PostGIS coordinates, and available volunteers (`availability_status = 'AVAILABLE'`).
   - *Engine*: A background Spring service invokes Google OR-Tools to solve the Vehicle Routing Problem with Time Windows (VRPTW), as formulated by *Reusken et al. (2024)*.
   - *Output*: Chained multi-stop pickup routes assigned automatically to volunteers based on shortest transit time and vehicle capacity.
2. **Machine Learning (Predictive Donor Forecasting)**:
   - *Input*: Clean historical data from PostgreSQL `donations` and `donor_profiles` (historical quantities, food categories, day-of-week timestamps, completed donation frequencies).
   - *Engine*: An ensemble regression model (Random Forest / XGBoost), as formulated by *Paul & Davis (2022)*, trained to forecast expected surplus volumes from specific donor clusters.
   - *Output*: Proactive dashboard alerts to nearby receiver organizations to prepare storage capacity hours before commercial surplus meals are physically published.

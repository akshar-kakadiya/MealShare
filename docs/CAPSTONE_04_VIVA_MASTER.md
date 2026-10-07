# MealShare Capstone Master Viva Voce Guide (35 Questions & Answers)

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2 Comprehensive Viva Voce Defense  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## Category 1: Project Overview & Core Mission (Q1–Q4)

### Q1: What is MealShare, and what core problem does it solve?
**Answer**: MealShare is an integrated surplus food redistribution platform engineered to minimize urban food waste and alleviate local food insecurity. It bridges commercial and individual food donors with accredited charitable organizations through vetted volunteer transport. It solves the critical coordination, perishability, and verification bottlenecks that currently cause edible surplus food to be discarded into landfills.

### Q2: Why did you choose surplus food redistribution for your Capstone project?
**Answer**: Urban food waste is fundamentally a computer science and logistics challenge rather than a supply deficit. Millions of meals are discarded daily while nearby shelters struggle with hunger due to information asymmetry, lack of transport, and lack of trust. Developing an integrated platform combining geospatial queries, concurrency control, and cryptographic handover verification provided a challenging, real-world software engineering problem with high societal impact.

### Q3: What are the primary user roles in MealShare, and why are all four necessary?
**Answer**: MealShare integrates four distinct roles:
1. **Donors**: Food businesses publishing surplus listings and food safety declarations.
2. **Receivers**: Non-profit shelters claiming food based on beneficiary capacity.
3. **Volunteers**: Couriers who solve the critical "last-mile" transportation deficit.
4. **Administrators**: Governing authorities who verify non-profit credentials, adjudicate users, and monitor system-wide audit logs.
Prior academic prototypes omitted volunteers or administrators, resulting in uncollected food or unvetted claims.

### Q4: Explain the complete end-to-end redistribution workflow in MealShare.
**Answer**: 
1. Donor publishes a surplus meal listing with a mandatory food safety declaration (`AVAILABLE`).
2. Receiver discovers the listing via PostGIS spatial radius queries and submits a claim request (`PENDING`).
3. Donor reviews claims chronologically and approves an allocation (`APPROVED`, locking quantity).
4. Administrator assigns an available volunteer courier (`ASSIGNED`, generating a 6-digit OTP).
5. Volunteer arrives at the donor facility and inputs the OTP, authenticating custody (`PICKED_UP`).
6. Volunteer delivers food to the receiver and marks the task complete (`COMPLETED`).

---

## Category 2: System Architecture & Technology Stack (Q5–Q10)

### Q5: Can you explain the high-level architecture of MealShare?
**Answer**: MealShare employs a decoupled multi-tier architecture:
- **Client Tier**: Cross-platform Flutter mobile applications for field users (Donor, Receiver, Volunteer) and a React 18 TypeScript web portal for administrators.
- **Identity Tier**: Firebase Authentication issuing signed Bearer JWTs.
- **Backend API Tier**: Spring Boot 3.4.x REST backend exposing 47 endpoints across 18 controllers.
- **Data Tier**: PostgreSQL 16.8 with PostGIS 3.4.3 spatial extension, versioned via 7 Flyway migrations.

### Q6: Why did you choose Spring Boot 3.4.x for the backend?
**Answer**: Spring Boot provides enterprise-grade transactional management (`@Transactional`), robust declarative security (Spring Security 6), native Hibernate PostGIS spatial integration, and seamless decoupled event handling (`@TransactionalEventListener`), ensuring strict data consistency and high API throughput.

### Q7: Why did you choose Flutter for the mobile client?
**Answer**: Flutter allows compiling a single Dart codebase to native performance across both iOS and Android. Food redistribution involves heterogeneous field devices; as proven by Karani et al. (2024), single-platform apps fragment community networks. Flutter's Provider architecture enables role-specific UI rendering while reducing maintenance overhead.

### Q8: Why did you choose React 18 with Vite for the Admin Web portal?
**Answer**: Administrative oversight requires complex desktop data manipulation: dense tabular views, search filters, verification queues, and analytics charts. React 18 with TypeScript eliminates runtime type errors, while Vite provides rapid development cycles. Material-UI and Recharts deliver responsive dashboards without unnecessary framework overhead.

### Q9: Why did you choose PostgreSQL over MongoDB or other NoSQL alternatives?
**Answer**: Surplus food redistribution is a zero-sum, multi-party transaction. If a donor lists 50 meals, concurrent allocations must never exceed 50. PostgreSQL provides ACID transactional guarantees, partial unique indexes, and storage-level check constraints (`allocated_quantity <= quantity`). NoSQL document stores lack native multi-table transactional invariants, creating high risks of race conditions during concurrent claims.

### Q10: How does your architecture decouple event notifications from database transactions?
**Answer**: When an operational event occurs (e.g., claim approval), the service publishes a domain event using `ApplicationEventPublisher`. The listener processes the event using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. If notification creation or push dispatch encounters an error, the core business transaction remains successfully committed.

---

## Category 3: Database & Spatial Design (Q11–Q15)

### Q11: Explain your database schema and entity relationships.
**Answer**: The database consists of 10 tables across 7 Flyway migrations:
- `users` is the central identity table, with 1-to-1 extension profiles: `donor_profiles`, `receiver_organizations`, and `volunteer_profiles`.
- A donor profile has a 1-to-many relationship with `donations`.
- A donation has a 1-to-many relationship with `donation_requests`.
- An approved donation request has a 1-to-1 relationship with `pickups`.
- A pickup links 1 volunteer to 1 donation request.
- Central auxiliary tables include `notifications`, `user_device_tokens`, and append-only `audit_logs`.

### Q12: Why did you choose PostGIS for spatial discovery?
**Answer**: PostGIS stores coordinates natively as `GEOMETRY(Point, 4326)` adhering to WGS 84. Using a Generalized Search Tree (GiST) index (`idx_donations_location_gist`), proximity queries execute in $O(\log N)$ time rather than $O(N)$ sequential table scans. This avoids transferring all active records over the network for client-side distance math.

### Q13: What is SRID 4326, and why is it important?
**Answer**: Spatial Reference Identifier (SRID) 4326 refers to the WGS 84 coordinate reference system, which uses latitude and longitude coordinates on the Earth's ellipsoidal surface. It is the global standard used by GPS and modern mapping systems, allowing PostGIS to calculate true geodesic distances without projection distortions.

### Q14: What is a GiST index, and how does it optimize proximity queries?
**Answer**: A Generalized Search Tree (GiST) index organizes geometric data into a hierarchical tree of bounding boxes (R-tree structure). When a query executes `ST_DWithin(location, search_point, radius)`, the database evaluates bounding-box intersections to rapidly discard non-matching records, querying thousands of geographic points in sub-millisecond execution times.

### Q15: How does the database prevent duplicate active claims by the same organization?
**Answer**: Flyway migration V4 creates a partial unique index:
`CREATE UNIQUE INDEX idx_unique_active_donation_request ON donation_requests (donation_id, receiver_org_id) WHERE status IN ('PENDING', 'APPROVED');`.
This allows historical records (`REJECTED`, `WITHDRAWN`, `COMPLETED`), but mathematically prevents an organization from submitting multiple concurrent active claims for the same donation.

---

## Category 4: Backend Engineering & API Design (Q16–Q20)

### Q16: How many REST controllers and endpoints are exposed, and how are they organized?
**Answer**: The backend exposes **47 REST API endpoints** across **18 controllers**, grouped into:
- Public Health Probe: 1 endpoint (`/api/v1/health`)
- Authentication & User Management: 2 endpoints (`/api/v1/users`)
- Donor Operations: 10 endpoints (profiles, listings, claim review, approvals)
- Receiver Operations: 8 endpoints (profiles, PostGIS discovery, requests)
- Volunteer Operations: 4 endpoints (pickup lists, OTP verification, completion)
- Admin Governance: 16 endpoints (dashboard, verification, users, donations, pickups, reports, audit logs)
- Notifications: 6 endpoints (queries, unread count, read mutations, tokens)

### Q17: How is Role-Based Access Control (RBAC) enforced across endpoints?
**Answer**: RBAC is enforced through a dual layer:
1. URL-pattern rules in `SecurityConfig.java` enforcing public, authenticated, or role-restricted URL prefixes.
2. Method-level `@PreAuthorize("hasRole('DONOR')")` annotations on controller methods.
Roles are resolved strictly from the PostgreSQL `users` table via `findByFirebaseUid()`, preventing client token tampering.

### Q18: How does your backend prevent Insecure Direct Object References (IDOR)?
**Answer**: Every service method validates ownership before executing state changes. In `PickupService`, the authenticated volunteer's ID is compared against `pickup.getVolunteer().getId()`. If a mismatched ID is passed in the URL, the service throws an `AccessDeniedException`, returning `HTTP 403 Forbidden`.

### Q19: Explain how First-Come, First-Served (FCFS) allocation works.
**Answer**: When a donor views incoming requests via `/api/v1/donor/donations/{id}/requests`, the repository orders records by `requested_at ASC`. Donors review claims in the order received. Approving a claim locks the requested quantity into `allocated_quantity` and updates the request to `APPROVED`.

### Q20: How is allocation made concurrency-safe during simultaneous claim approvals?
**Answer**: Concurrency safety is guaranteed by Spring `@Transactional` boundaries combined with database check constraints:
`chk_donations_allocated_le_quantity CHECK (allocated_quantity <= quantity)`.
If two requests are concurrently processed, PostgreSQL will atomically reject any commit that causes `allocated_quantity` to exceed available `quantity`, preventing race conditions.

---

## Category 5: Security & Cryptography (Q21–Q25)

### Q21: How does Firebase Authentication integrate with your Spring Boot backend?
**Answer**: Clients authenticate with Firebase on mobile or web and receive a cryptographically signed JWT ID token. Every API request passes this token in the `Authorization: Bearer <token>` header. Our `FirebaseAuthenticationFilter` decodes the token using the Firebase Admin SDK, extracts the `firebase_uid`, and resolves the user entity from PostgreSQL.

### Q22: Where are user roles stored, and why aren't they stored in Firebase custom claims?
**Answer**: User roles are stored **authoritatively in the PostgreSQL `users` table**. While Firebase supports custom claims, storing roles in PostgreSQL ensures our database remains the single source of truth, allows instant account suspensions without waiting for token refresh cycles, and keeps our architecture cloud-vendor-independent.

### Q23: How is the physical collection OTP generated and verified?
**Answer**: When an administrator assigns a volunteer pickup, `PickupOtpService` generates a cryptographically secure 6-digit numeric token using `java.security.SecureRandom`. The plain code is displayed once to the admin and issued to the donor. Only a salted BCrypt hash is stored in `pickups.otp_hash`. When the volunteer arrives, they enter the code; the backend compares the input using `BCryptPasswordEncoder.matches()`.

### Q24: Why use BCrypt for hashing OTPs?
**Answer**: BCrypt incorporates a random salt and a configurable work factor. Storing a BCrypt hash ensures that even if the database is dumped or read via SQL injection, active OTP codes cannot be reversed or pre-computed via rainbow tables.

### Q25: How does the system prevent OTP brute-force attacks?
**Answer**: Three layers of defense:
1. Short validity: OTP expires after 10 minutes (`pickups.otp_expires_at`).
2. Attempt limiter: `pickups.otp_attempts` increments upon each failed attempt; reaching 5 failed attempts permanently locks the pickup.
3. Concurrency control: Verification executes under pessimistic write locking (`PESSIMISTIC_WRITE`).

---

## Category 6: Food Safety & Governance (Q26–Q28)

### Q26: What food safety controls are implemented in MealShare?
**Answer**: MealShare enforces three operational controls:
1. Mandatory self-declaration: Donors must agree to a food safety declaration (`food_safety_declared = true`).
2. Temporal validation: Preparation time must be in the past (`preparationDateTime <= NOW()`), consume-before deadline must be in the future (`consumeBefore > NOW()`), and pickup window must precede the consumption deadline.
3. Automatic query filtering: PostGIS discovery queries exclude donations where `consume_before <= NOW()`.

### Q27: Does MealShare certify food safety?
**Answer**: **No. MealShare does NOT certify food safety**, conduct chemical/bacteriological testing, or provide regulatory guarantees. It acts as an operational facilitator enforcing digital self-declarations and temporal boundaries.

### Q28: Who bears legal and physical liability for donated food?
**Answer**: Legal and physical liability remains strictly with the participating food donors and receiving organizations under applicable national food donation laws (e.g., Good Samaritan food donation regulations). MealShare explicitly records this legal boundary.

---

## Category 7: Notifications & Push Reality (Q29–Q31)

### Q29: How are notifications generated and stored?
**Answer**: When state mutations occur, domain events (e.g., `DonationRequestApprovedEvent`) are published. `NotificationEventListener` captures these events `AFTER_COMMIT`, persists an in-app `Notification` entity into PostgreSQL, and exposes them via 6 REST endpoints.

### Q30: What is the current implementation status of Firebase Cloud Messaging (FCM)?
**Answer**: The backend codebase contains fully implemented FCM dispatch logic (`FcmService.java`) and database tables for device tokens (`user_device_tokens`). However, in our offline local development environment, external push delivery to physical mobile handsets is classified as **NOT TESTED** because it requires paid Apple Developer and Google Cloud production certificates.

### Q31: What happens if an external notification service fails?
**Answer**: Because listeners execute `AFTER_COMMIT`, any FCM network timeout or external API failure is logged but isolated; it will never roll back an already-committed donation claim or verified pickup.

---

## Category 8: AI, Machine Learning & Operations Research (Q32–Q33)

### Q32: Is there any AI or Machine Learning currently implemented in MealShare?
**Answer**: **No. MealShare contains zero implemented AI/ML models, neural networks, or inference pipelines.** We maintain strict academic honesty: predictive demand forecasting and automated vehicle route optimization are established as **FUTURE / PROPOSED** roadmap enhancements based on published literature (*Paul & Davis, 2022*; *Reusken et al., 2024*).

### Q33: How could Machine Learning and OR-Tools be integrated in the future?
**Answer**: Our database schema was engineered to collect clean training data:
- *OR-Tools Route Optimization*: Ingests active donations, PostGIS coordinates, and pickup time windows to solve the Vehicle Routing Problem with Time Windows (VRPTW).
- *Machine Learning Forecasting*: An ensemble regression model trained on historical donation volume, categories, and day-of-week timestamps to forecast expected surplus volumes.

---

## Category 9: Research Foundation & Academic Gaps (Q34–Q35)

### Q34: What research gap in the literature does MealShare address?
**Answer**: As synthesized across 12 verified papers (2021–2025), existing research is heavily fragmented: mathematical routing models exist without software implementations, while mobile prototypes connect only 2 parties, compute coordinates client-side, and rely on unverified "click-to-confirm" handovers. MealShare bridges this divide by delivering a 4-role platform combining server-side PostGIS indexing, BCrypt OTP custody proofs, and administrative auditability.

### Q35: What are the main limitations of your Phase-1 implementation?
**Answer**: We acknowledge four clear scope boundaries:
1. FCM push delivery to physical devices is not tested in local offline mode.
2. Allocation relies on deterministic FCFS rather than dynamic multi-criteria optimization.
3. Pickups are assigned individually rather than chained into automated multi-stop routes.
4. Expiration is enforced via query-time filtering rather than a background cron daemon.
These limitations define our clear roadmap for Phase-2 development.

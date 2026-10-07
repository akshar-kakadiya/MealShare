# MealShare Technical Architecture Rationale

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Format**: Concise 30–60 Second Viva Voce Defense Responses  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Frontend & Client Technologies

### Q1: Why did you choose Flutter for the mobile client?
> **Viva Defense (45s)**:  
> *"We chose Flutter because food redistribution involves diverse field users: donors operating on iOS or Android, and volunteers and non-profit staff often using low-cost Android devices. As demonstrated by Karani et al. (2024), single-platform native apps create community fragmentation. Flutter provides a single compiled Dart codebase delivering native 60fps performance across both iOS and Android. Its Provider state management cleanly separates UI views from business logic and enables role-based dynamic view rendering across Donor, Receiver, and Volunteer modes."*

### Q2: Why did you choose React with TypeScript for the Admin Web?
> **Viva Defense (45s)**:  
> *"Administrative governance in MealShare requires desktop-optimized data management: dense tabular listings, search filters, document inspection queues, and real-time analytical charts. React 18 with Vite provides instant hot-module replacement and component-level modularity. TypeScript eliminates an entire class of runtime type errors in complex data structures like audit logs and verification payloads. Leveraging Material-UI and Recharts allows us to deliver responsive administrative KPI dashboards without heavy frontend framework bloat."*

---

## 2. Backend & Database Architecture

### Q3: Why did you choose Spring Boot 3.4.x (Java 17)?
> **Viva Defense (45s)**:  
> *"Surplus food redistribution demands strict transactional guarantees, concurrency safety, and enterprise-grade security. Spring Boot 3.4 on Java 17 LTS provides robust declarative transaction management (`@Transactional`), native integration with Hibernate and PostGIS spatial dialects, and a mature security ecosystem (Spring Security 6). The framework allows us to cleanly decouple operational notifications using Spring Application Events without stalling database transactions, all while maintaining high throughput across 47 REST endpoints."*

### Q4: Why did you choose PostgreSQL over MongoDB or other NoSQL databases?
> **Viva Defense (45s)**:  
> *"Surplus food allocation is strictly zero-sum: if 50 meals are available, allocating 30 leaves exactly 20. Relational databases like PostgreSQL provide full ACID transaction guarantees and enforce mathematical check constraints at the storage engine level. NoSQL document stores lack native multi-table transactional invariants and partial unique indexing, which would introduce severe race conditions and inventory discrepancies during concurrent claims."*

### Q5: Why did you choose PostGIS for spatial queries rather than computing distances in application code?
> **Viva Defense (50s)**:  
> *"Computing Euclidean or Haversine distances in application code requires fetching all active records over the network, incurring an $O(N)$ memory and bandwidth penalty on every search. PostGIS is an industry-standard spatial database extension that provides native `GEOMETRY(Point, 4326)` storage and Generalized Search Tree (GiST) indexing. This reduces spatial proximity search to an $O(\log N)$ bounding-box index scan using native functions like `ST_DWithin` and `ST_Distance`, seamlessly combining geospatial boundaries with temporal filters (`consume_before > NOW()`) in a single query plan."*

### Q6: Why did you use Flyway for database migrations?
> **Viva Defense (35s)**:  
> *"Flyway guarantees repeatable, version-controlled schema evolution across development, testing, and production environments. By maintaining migrations as immutable SQL scripts (V1 through V7), schema changes—such as PostGIS extension enablement, table creation, check constraints, and spatial indexes—are applied deterministically. Setting Hibernate's `ddl-auto` to `validate` ensures the application code can never mutate database structure unpredictably."*

---

## 3. Security, Allocation & Handover Protocols

### Q7: Why did you choose Firebase Authentication combined with database-authoritative RBAC?
> **Viva Defense (50s)**:  
> *"We adopt a defense-in-depth architecture. Firebase Authentication offloads sensitive identity concerns—such as cryptographic token signing, password hashing, and OAuth provider management—to Google's secure infrastructure. However, to prevent client-side privilege escalation, user roles are never trusted from token claims. Our `FirebaseAuthenticationFilter` decodes the token, extracts the UID, and queries the PostgreSQL `users` table directly. The application's granted authority is derived strictly from the database, ensuring suspended or unauthorized users are locked out immediately."*

### Q8: Why did you implement First-Come, First-Served (FCFS) allocation?
> **Viva Defense (45s)**:  
> *"FCFS provides a transparent, predictable, and equitable allocation mechanism essential for charitable organizations. Claims are reviewed chronologically (`requestedAt ASC`), eliminating accusations of favoritism. Furthermore, donors retain human-in-the-loop oversight to evaluate the requesting organization's capacity before approving. When coupled with database check constraints, FCFS provides a deterministic, concurrency-safe baseline that avoids the opacity and complexity of automated black-box assignment."*

### Q9: Why did you implement an OTP handover protocol rather than a simple button click or QR code?
> **Viva Defense (45s)**:  
> *"A simple 'Mark as Picked Up' button is vulnerable to fraud, accidental clicks, and phantom pickups. Conversely, proprietary QR scanners or hardware RFID readers impose equipment costs on grassroots charities. A 6-digit numeric OTP bridges the digital-physical divide effortlessly: the donor verbally communicates the code to the arriving volunteer. The volunteer inputs the code, and the backend authenticates the physical handover, creating an irrefutable proof-of-custody transfer."*

### Q10: Why did you use BCrypt for OTP hashing instead of storing plain text?
> **Viva Defense (40s)**:  
> *"Storing plain text OTP codes in the database creates an internal security risk: any database administrator, compromised read-replica, or SQL injection vulnerability could expose valid tokens. BCrypt is a cryptographically salted, adaptive one-way hash function. By storing only the hash (`pickups.otp_hash`) and validating via `BCryptPasswordEncoder.matches()`, even an attacker with direct database read access cannot deduce the active OTP."*

---

## 4. API Design & Architectural Governance

### Q11: Why did you choose REST APIs over GraphQL or gRPC?
> **Viva Defense (40s)**:  
> *"REST over HTTP/JSON is the universal standard for multi-platform integration across web (React) and mobile (Flutter) clients. It provides standardized HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `403 Forbidden`, `404 Not Found`), allows transparent reverse-proxy caching, and maps cleanly to Spring Security URL-pattern authorization rules. Our 47 endpoints are fully documented and testable via OpenAPI / Swagger UI."*

### Q12: How does your architecture prevent Insecure Direct Object References (IDOR)?
> **Viva Defense (45s)**:  
> *"IDOR occurs when an application exposes a direct reference to an internal entity without verifying user ownership. In MealShare, every controller method verifies the authenticated actor against the entity's owner ID. For example, a volunteer can only inspect pickups where `pickup.volunteer_id == authenticated_user_id`, and a donor can only cancel donations matching their `donor_id`. If an unauthorized ID is supplied in the path, the service immediately throws an `AccessDeniedException`, returning `HTTP 403 Forbidden`."*

# MealShare Non-Functional Requirements Specification

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Overview

This document specifies the non-functional requirements (NFRs) implemented across the MealShare platform. To maintain strict academic and engineering integrity, all requirements describe verifiable architectural properties, security controls, and design patterns implemented in the codebase without fabricated benchmark metrics.

```
       ┌────────────────────────────────────────────────────────┐
       │             NON-FUNCTIONAL ARCHITECTURAL PILLARS       │
       ├──────────────┬──────────────┬──────────────┬───────────┤
       │   SECURITY   │ PERFORMANCE  │ RELIABILITY  │USABILITY &│
       │ & GOVERNANCE │& SCALABILITY │& DATA HEALTH │MAINTAIN.  │
       └──────────────┴──────────────┴──────────────┴───────────┘
```

---

## 2. Security & Governance Requirements

### 2.1 Identity & Authentication (NFR-SEC-01)
- **Token Verification**: Every incoming API request to protected endpoints must present an `Authorization: Bearer <Firebase_ID_Token>` header. Tokens are cryptographically decoded and validated server-side using the Firebase Admin SDK.
- **Stateless Session Management**: The Spring Security context is configured as `SessionCreationPolicy.STATELESS`. The backend maintains zero server-side HTTP session state, eliminating session fixation and CSRF vulnerabilities on REST endpoints.

### 2.2 Database-Authoritative Authorization & RBAC (NFR-SEC-02)
- **Authority Resolution**: User roles are resolved strictly from the PostgreSQL `users` table via `userRepository.findByFirebaseUid()`. Client-supplied role claims within JWT tokens or request bodies are discarded.
- **Granular Role Isolation**: Granted authorities (`ROLE_DONOR`, `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL`, `ROLE_ADMIN`) are enforced using method-level security (`@PreAuthorize`) and URL pattern matching in `SecurityConfig.java`.
- **Suspended Account Isolation**: Accounts marked as `SUSPENDED` in the database are assigned `ROLE_SUSPENDED` by the security filter, completely isolating the user from accessing operational endpoints.

### 2.3 Insecure Direct Object Reference (IDOR) Protection (NFR-SEC-03)
- **Ownership Verification**: All entity modification and retrieval endpoints enforce strict entity-to-actor ownership verification. 
- **Enforcement**:
  - Donors can only inspect, edit, or cancel donations associated with their authenticated `donor_id`.
  - Receivers can only view or withdraw claim requests created by their authenticated `receiver_org_id`.
  - Volunteers can only view, verify OTPs for, or complete pickups assigned specifically to their `volunteer_id`.
  - Unauthenticated access attempts to foreign resources are rejected with `HTTP 403 Forbidden`.

### 2.4 Cryptographic Handover Security (NFR-SEC-04)
- **Secure Random Generation**: Handover OTPs are 6-digit numeric tokens generated using `java.security.SecureRandom`.
- **One-Way Hashing**: Plaintext OTP codes are never persisted in the database; only a salted BCrypt hash (`pickups.otp_hash`) is stored.
- **Brute-Force & Replay Mitigation**:
  - The OTP is subject to a strict 10-minute expiry window (`pickups.otp_expires_at`).
  - The verification service enforces a maximum of 5 verification attempts (`pickups.otp_attempts >= 5` locks the pickup).
  - Verification is executed under pessimistic database locking (`PESSIMISTIC_WRITE`) to eliminate race conditions during concurrent OTP submissions.

---

## 3. Performance & Scalability Requirements

### 3.1 Server-Side Spatial Indexing (NFR-PERF-01)
- **Spatial Data Types**: Geographic coordinates are stored in the `donations` table as native PostGIS `GEOMETRY(Point, 4326)` columns adhering to the WGS 84 coordinate reference system.
- **GiST Spatial Index**: A Generalized Search Tree (GiST) index (`idx_donations_location_gist`) is established on the geometry column. This enables bounding-box spatial queries (`ST_DWithin`) and geodesic distance calculations (`ST_Distance`) to execute in $O(\log N)$ logarithmic time rather than $O(N)$ sequential table scans.

### 3.2 Bounded Data Pagination (NFR-PERF-02)
- **Resource Protection**: All collection endpoints (e.g., `/donations`, `/requests`, `/pickups`, `/audit-logs`, `/notifications`) enforce Spring Data pagination parameters (`page`, `size`, `sort`).
- **Memory Safety**: Default page sizes are constrained to prevent unbounded memory allocation and excessive network payload transmission.

### 3.3 Relational Indexing Strategy (NFR-PERF-03)
- **B-Tree Indexing**: Dedicated B-Tree indexes are established on frequently filtered columns across all migrations:
  - `users(firebase_uid)`, `users(email)`, `users(role)`.
  - `donations(donor_id)`, `donations(status)`, `donations(created_at DESC)`.
  - `donation_requests(donation_id)`, `donation_requests(receiver_org_id)`, `donation_requests(donation_id, requested_at ASC)`.
  - `pickups(volunteer_id)`, `pickups(donation_request_id)`, `pickups(status)`.
  - `notifications(recipient_user_id, is_read)`, `notifications(created_at)`.
  - `audit_logs(actor_user_id)`, `audit_logs(entity_name, entity_id)`, `audit_logs(timestamp)`.

---

## 4. Reliability & Data Integrity Requirements

### 4.1 Transactional Atomicity & Concurrency Safety (NFR-REL-01)
- **ACID Boundaries**: All state-modifying business operations in `DonationService`, `DonationRequestService`, and `PickupService` are annotated with Spring `@Transactional`.
- **Allocation Invariant Enforcement**: Multi-user claim concurrency is governed by database check constraints:
  - `chk_donations_allocated_quantity_non_negative`: Enforces `allocated_quantity >= 0.0`.
  - `chk_donations_allocated_le_quantity`: Mathematically enforces `allocated_quantity <= quantity`.
- **Duplicate Claim Prevention**: A partial unique index (`idx_unique_active_donation_request`) prevents an organization from submitting multiple concurrent active claims (`PENDING`, `APPROVED`) for the same donation.

### 4.2 Decoupled Notification Fault Isolation (NFR-REL-02)
- **Asynchronous Event Processing**: The notification subsystem is decoupled from core transactional workflows using Spring Application Events.
- **Post-Commit Execution**: Event listeners are annotated with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. If notification generation, socket communication, or push dispatch encounters an error, the primary business transaction (such as donation approval or OTP verification) remains committed and unaffected.

---

## 5. Maintainability & Architecture Requirements

### 5.1 Layered Separation of Concerns (NFR-MAINT-01)
- **Controller-Service-Repository Pattern**: The backend adheres strictly to a 3-tier enterprise architecture:
  - *Controller Layer*: HTTP protocol handling, parameter binding, and `@PreAuthorize` authorization.
  - *Service Layer*: Transactional business logic, temporal validations, and event publication.
  - *Repository Layer*: Spring Data JPA interfaces and native PostGIS SQL query execution.
- **Global Error Handling**: Centralized exception resolution using `@RestControllerAdvice` guarantees uniform, structured error payloads (`ErrorResponse` with timestamp, status, and message) across all 47 endpoints.

### 5.2 Version-Controlled Database Migrations (NFR-MAINT-02)
- **Flyway Versioning**: All schema definitions, indexes, PostGIS extensions, and constraints are maintained as immutable Flyway SQL migration scripts (V1 through V7) in `src/main/resources/db/migration/`.
- **Environment Parity**: Hibernate `ddl-auto` is set to `validate` in production configuration, ensuring application code cannot alter database structure without formal migration scripts.

---

## 6. Usability Requirements

### 6.1 Role-Specific User Experience (NFR-USE-01)
- **Tailored Interfaces**: The frontend provides distinct viewports tailored to each stakeholder:
  - *Donors*: Quick meal listing creation, mandatory safety declaration checklist, and chronological claim review.
  - *Receivers*: Map/radius-based discovery, meal detail inspection, and one-click claim requests.
  - *Volunteers*: Pickup queue, donor location guidance, and numeric keypad OTP input.
  - *Administrators*: Tabular data management, search/filter controls, and analytical charts.
- **State Feedback**: Applications provide explicit visual indicators for loading spinners, form validation errors, network failure alerts, and empty dataset states.

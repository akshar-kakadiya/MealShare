# MealShare Project Scope, Limitations & Future Work

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## 1. Context & Academic Transparency

Engineering projects developed within academic capstone constraints must demonstrate clear technical boundaries. Rather than presenting theoretical assumptions or unverified features as complete, this document formally records the **known operational limitations** of the Phase-1 MealShare implementation and outlines the structured **future research and engineering roadmap**.

---

## 2. Documented Project Limitations

| Subsystem / Area | Documented Operational Limitation | Engineering Rationale & Current Mitigation | Academic Status |
|:---|:---|:---|:---:|
| **Push Notification Delivery** | External Firebase Cloud Messaging (FCM) dispatch to physical APNS/FCM mobile devices is **NOT TESTED**. | Physical push delivery requires active Apple Developer Program memberships, Google Cloud FCM production server keys, and live provisioned mobile handsets. In offline local evaluation, in-app notification persistence in PostgreSQL and REST retrieval are 100% verified. | **SCOPE BOUNDARY**<br>(Offline Dev Mode) |
| **Artificial Intelligence & Machine Learning** | The platform contains **zero implemented AI/ML models**, training pipelines, or neural inference engines. | Genuine predictive modeling requires longitudinal datasets of urban surplus generation across seasons. Deploying an untrained model would violate academic integrity. The database schema captures all requisite features to train such models in future work. | **FUTURE ROADMAP**<br>(Explicitly Not Implemented) |
| **Logistics Route Optimization** | Automated multi-stop vehicle route optimization via Google OR-Tools is **NOT IMPLEMENTED**. | Current logistics dispatch assigns individual volunteer pickups to approved claims. Multi-stop TSP/VRP route chaining is designed as an algorithmic extension based on *Reusken et al. (2024)*. | **FUTURE ROADMAP**<br>(Explicitly Not Implemented) |
| **Receiver Live Map Tracking** | Real-time GPS location streaming of volunteers during transit operates as prototype/demonstration functionality. | High-frequency WebSocket or SSE streaming requires continuous GPS device polling. Phase-1 focuses on deterministic REST state transitions (`ASSIGNED` → `PICKED_UP` → `COMPLETED`). | **PROTOTYPE SCOPE** |
| **Admin Settings & Configuration** | The Admin Settings page functions as local administrative UI configuration. | Platform parameters (e.g., OTP expiry minutes, maximum attempts) are configured via Spring Boot `application.yml` rather than dynamic database-driven admin overrides. | **PROTOTYPE SCOPE** |
| **Automated Expiry Scheduling** | Donations transition to `EXPIRED` status via query-level temporal filtering (`consume_before > NOW()`) rather than a background cron daemon. | Expired listings are completely filtered out of receiver discovery and claim creation, ensuring safe operational behavior without requiring an active Quartz scheduler. | **PLANNED EXTENSION** |
| **Production Infrastructure Hardening** | Enterprise production infrastructure concerns are not implemented in the local development environment. | Edge TLS/HTTPS termination, reverse proxy rate-limiting (e.g., NGINX / Cloudflare), distributed Redis caching, and Kubernetes container orchestration remain operational deployment tasks. | **PRODUCTION HARDENING** |

---

## 3. Future Research & Engineering Roadmap

The following future capabilities represent natural technical extensions enabled by MealShare's existing relational schema and REST architectural foundation:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   MEALSHARE FUTURE ROADMAP (PHASE-2)                   │
├────────────────────────┬───────────────────────┬───────────────────────┤
│  ALGORITHMIC LOGISTICS │ PREDICTIVE ANALYTICS  │ PRODUCTION HARDENING  │
│  Google OR-Tools VRPTW │ Ensemble ML Models    │ Live FCM Push Alerts  │
│  Multi-Stop Chaining   │ Donor Supply Forecast │ Distributed Redis     │
│  (Reusken et al. 2024) │ (Paul & Davis 2022)   │ Kubernetes Scaling    │
└────────────────────────┴───────────────────────┴───────────────────────┘
```

### 3.1 Automated Multi-Stop Vehicle Route Optimization (Google OR-Tools)
- **Academic Foundation**: *Reusken, Laporte, Rohmer, & Cruijssen (2024)*, *European Journal of Operational Research*.
- **Proposed Architecture**: Integrate Google OR-Tools in a dedicated Spring service to solve the Capacitated Vehicle Routing Problem with Time Windows (CVRPTW).
- **Execution Flow**: When multiple approved claims are active in a neighborhood, the solver will ingest volunteer vehicle capacity, pickup time windows (`pickup_available_from`, `pickup_available_until`), and PostGIS coordinates to chain multiple collections into an optimal single volunteer route.

### 3.2 Predictive Donor Supply & Demand Forecasting (Ensemble ML)
- **Academic Foundation**: *Paul & Davis (2022)*, *Annals of Operations Research*.
- **Proposed Architecture**: Deploy an offline Python / Scikit-learn microservice running an ensemble regression model (Random Forest + XGBoost).
- **Execution Flow**: Model trains on historical PostgreSQL data (`completed_donations_count`, food categories, day-of-week timestamps, volume history) to forecast expected food surplus patterns, enabling recipient organizations to prepare refrigerated storage capacity in advance.

### 3.3 Multi-Criteria Dynamic Claim Matching
- **Concept**: Enhance the current First-Come, First-Served (FCFS) approval workflow with a multi-criteria scoring recommendation engine.
- **Scoring Dimensions**: Evaluate recipient nutritional urgency, beneficiary population size, storage holding capacity, and transit proximity to recommend optimal allocations to donors.

### 3.4 Production Push Notification Delivery & Live Telemetry
- **Concept**: Deploy live Apple Push Notification Service (APNS) and Google Firebase Cloud Messaging (FCM) production keys.
- **Execution Flow**: Replace polling with instant background push alerts when donations are published nearby or pickups are assigned. Integrate live background GPS breadcrumbs via WebSocket for active volunteer transit.

### 3.5 Automated Quartz Background Expiration Daemon
- **Concept**: Implement a Spring `@Scheduled` or Quartz daemon executing every 60 seconds to evaluate `donations.consume_before <= NOW()` and automatically transition orphaned listings to `EXPIRED`.

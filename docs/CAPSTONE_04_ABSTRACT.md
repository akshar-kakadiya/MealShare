# MealShare Capstone Abstract

> **Course**: UQ25CA741A_Capstone Project Phase-1  
> **Milestone**: Presentation-2  
> **Document Version**: 1.0  
> **Date**: September 26, 2026

---

## Abstract

Urban food waste and hunger represent a critical paradox in modern food supply chains. While commercial food enterprises, event banquet halls, and institutional dining facilities discard substantial quantities of wholesome, edible surplus food daily, community shelters, orphanages, and underprivileged populations suffer from acute nutritional insecurity. The primary impediment to timely redistribution is not an absence of surplus food, but rather the severe coordination, logistical, and verification friction associated with managing highly perishable goods. Traditional redistribution efforts rely on informal communication channels that lack geographic visibility, transport coordination, physical handover verification, and administrative auditability.

To address these challenges, this capstone project presents **MealShare**, an integrated, multi-stakeholder surplus food redistribution platform. MealShare connects four primary user roles—Donors, Accredited Receiver Organizations, Volunteer Couriers, and Platform Administrators—within a unified, secure, and production-aligned software engineering architecture. Donors publish surplus food listings accompanied by mandatory food safety self-declarations and strict pickup time windows. Non-profit receiver organizations discover active, unexpired donations using a high-performance spatial proximity discovery engine powered by PostgreSQL and the PostGIS spatial extension. Claim requests are processed through an equitable, concurrency-safe First-Come, First-Served (FCFS) allocation workflow governed by database-level check constraints to mathematically prevent over-allocation.

To resolve the critical last-mile logistics gap, platform administrators assign vetted volunteer couriers with dynamic availability tracking to approved claims. Physical collection handovers are secured via a cryptographically generated, rate-limited 6-digit One-Time Password (OTP) verification protocol, ensuring authenticated custody transfers without requiring expensive proprietary hardware. Decoupled Spring Application Events manage operational notification persistence, while an append-only PostgreSQL JSONB audit trail records every critical system mutation for regulatory traceability.

The system is implemented using an enterprise technology stack comprising **Flutter** for cross-platform mobile clients, **React 18** and **TypeScript** for the administrative web portal, **Spring Boot 3.4.x** (Java 17) for the REST backend exposing 47 endpoints, **PostgreSQL 16.8 with PostGIS 3.4.3** for relational and spatial persistence, and **Firebase Authentication** for identity management. The platform implementation is verified by a comprehensive automated test suite consisting of 204 passing backend unit and integration tests, successful React production builds, and clean Flutter static analysis. Advanced algorithmic capabilities—including machine learning demand forecasting and automated vehicle route optimization—are established as foundational extensions in the project's future development roadmap.

**Keywords**: *Surplus Food Redistribution, Humanitarian Logistics, PostGIS Spatial Indexing, Concurrency-Safe Allocation, Handover OTP Security, Spring Boot, Flutter, React 18, Regulatory Traceability.*

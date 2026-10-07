# CAPSTONE-12 — DEPLOYMENT AUDIT & ARCHITECTURE SPECIFICATION

**Document Version:** 1.0  
**Date:** October 7, 2026  
**Milestone:** CAPSTONE-12  
**Project:** MealShare (Redistribution Platform)  
**Roles:** DONOR, RECEIVER, ADMIN (Strict 3-Role Model — Zero Volunteers)  

---

## 1. Executive Summary

Milestone **CAPSTONE-12** initiates the live production deployment of the MealShare ecosystem. In accordance with the project rule:
> **From this milestone onward, every feature that is built must be deployed to the live MealShare system.**

This audit examines every tier of the project—Spring Boot backend, PostgreSQL with PostGIS spatial storage, React 18 Admin Web, and Flutter mobile application—to assess deployment readiness, identify environment coupling, and define the live architecture.

---

## 2. Live Architecture Specification

The live production deployment topology connects all real clients through secure HTTPS channels:

```
                                    INTERNET
                                       │
                      ┌────────────────┴────────────────┐
                      │                                 │
              Flutter Mobile                      React Admin Web
        (Android Release / Web)             (Firebase Hosting / CDN)
                      │                                 │
                      └────────────────┬────────────────┘
                                       │ HTTPS (TLS 1.3)
                                       ▼
                              Spring Boot API (v3.4.3)
                         [Reverse Proxy / Container Cloud]
                                       │
                      ┌────────────────┴────────────────┐
                      │                                 │
              Firebase Auth (v9.4.3)         PostgreSQL 16 + PostGIS 3.4
             (mealshare-100 Project)          (Spatial Relational Core)
```

---

## 3. Comprehensive Project Audit

### 3.1. Backend Tier (Spring Boot)

| Component | Audit Finding | Deployment Assessment |
| :--- | :--- | :--- |
| **Framework Version** | Spring Boot `3.4.3` | Current, stable release on Java 17 baseline |
| **Java Baseline** | Java `17` (configured in `pom.xml` properties) | Compatible with lightweight container base images (`eclipse-temurin:17-jre-alpine`) |
| **Build Tool** | Apache Maven with wrapper (`.\mvnw.cmd`) | Hermetic builds available via multi-stage Dockerfile |
| **Configuration File** | `backend/src/main/resources/application.yml` | Property resolution with environment variable fallbacks |
| **Server Port** | `${SERVER_PORT:8081}` | Cloud container platforms commonly inject `${PORT}`. Needs `${PORT:${SERVER_PORT:8081}}` support |
| **Datasource Config** | `jdbc:postgresql://localhost:5433/mealshare_app` | Fully externalized via `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| **JPA / Hibernate** | `ddl-auto: validate`, Hibernate Spatial `6.6.8` | Schema managed strictly by Flyway; Hibernate performs validation only |
| **Flyway Migrations** | `spring.flyway.enabled: true`, `locations: classpath:db/migration` | Runs migrations automatically on boot (`baseline-on-migrate: true`) |
| **Firebase Admin SDK** | `com.google.firebase:firebase-admin:9.4.3` | `FirebaseConfig.java` reads `firebase.credentials.path` with fallback to `backend/mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json` |
| **CORS Configuration** | `CorsConfig.java` reading `cors.allowed-origins` | Defaults to `http://localhost:5173,http://localhost:3000`. Must be extended with live hosting domains |
| **Actuator & Health** | `spring-boot-starter-actuator` + `HealthController.java` | Public `/actuator/health` and `/api/v1/health` permit load-balancer liveness/readiness probes |
| **Swagger / OpenAPI** | `springdoc-openapi-starter-webmvc-ui:2.8.5` | Exposed at `/swagger-ui.html` and `/api-docs` (PermitAll) |

### 3.2. Database Tier (PostgreSQL + PostGIS)

| Component | Audit Finding | Deployment Assessment |
| :--- | :--- | :--- |
| **Engine Version** | PostgreSQL `16.4` | High compatibility across container and cloud managed databases |
| **Spatial Extension** | PostGIS `3.4` (`postgis/postgis:16-3.4`) | Enabled in migration `V1__enable_postgis.sql`. Mandatory for spatial queries (`ST_DWithin`, `ST_Distance`) |
| **Crypto Extension** | `pgcrypto` / `uuid-ossp` | Required for `gen_random_uuid()` PK default generation |
| **Migration Sequence** | 10 Flyway scripts (`V1` through `V10`) | Validated cleanly against clean and initialized PostgreSQL instances |
| **Table Inventory** | 11 tables | `users`, `donor_profiles`, `receiver_organizations`, `audit_logs`, `donations`, `donation_requests`, `pickups`, `notifications`, `user_device_tokens`, `ratings`, `flyway_schema_history` |
| **Seed & Demo Data** | `backend/src/main/resources/demo_dataset.sql` | Canonical database reset script ready via `scripts/reset_demo_data.py` |

### 3.3. Flutter Mobile Tier

| Component | Audit Finding | Deployment Assessment |
| :--- | :--- | :--- |
| **API Base URL** | `mobile/lib/config/api_config.dart` | Configured with `String.fromEnvironment('BACKEND_URL')` override. Supports runtime override via `ApiConfig.setBaseUrl(...)` |
| **Localhost Check** | `10.0.2.2`, `localhost`, `127.0.0.1` | Isolated strictly to `api_config.dart` fallback block. Zero hardcoded local URLs across feature screens |
| **Firebase Auth** | `firebase_auth: ^5.7.0` | `AuthService.dart` handles token acquisition and auth state transitions |
| **Android Application ID** | `com.mealshare.mealshare_mobile` | Defined in `mobile/android/app/build.gradle.kts` |
| **Build Configuration** | Release build uses debug signing key | `flutter build apk --release` succeeds without external keystore setup |
| **Build Command for Live** | Parameterized dart-define | `flutter build apk --release --dart-define=BACKEND_URL=https://<live-api-host>` |

### 3.4. Admin Web Tier (React 18)

| Component | Audit Finding | Deployment Assessment |
| :--- | :--- | :--- |
| **Build Tooling** | Vite `8.2.2`, TypeScript `5.8`, React `19.2` | Fast incremental bundling; generates static assets in `admin-web/dist` |
| **API Client** | `admin-web/src/api/client.ts` | Configured with `import.meta.env.VITE_API_BASE_URL` with local fallback |
| **Firebase Configuration** | `admin-web/src/config/firebase.ts` | Configured with `import.meta.env.VITE_FIREBASE_*` variables for `mealshare-100` |
| **Local Environment** | `admin-web/.env` | Contains active Firebase credentials for `mealshare-100` project |
| **Production Build Status** | `npm run build` | Verified: compiles cleanly with 0 TypeScript/lint errors |
| **Hosting Target** | Static SPA | Can be hosted on Firebase Hosting, Vercel, Netlify, or Cloudflare Pages |

### 3.5. Existing Deployment Infrastructure

| Item | Status | Notes |
| :--- | :--- | :--- |
| **Dockerfile** | Not present | A production multi-stage Dockerfile is required for backend containerization |
| **docker-compose.yml** | Present (Local Dev) | Configures `postgres` (PostGIS), `redis`, and `minio` for local development |
| **Deployment Scripts** | None | Deployment pipeline scripts need to be created |
| **Cloud CLI Availability** | `firebase-tools@15.22.4`, `vercel@50.1.2` | Firebase CLI is logged in and connected to project `mealshare-100` |
| **Cloud Project** | `mealshare-100` (GCP / Firebase) | Active project with Firebase Auth, Cloud Storage, and Hosting capabilities |

---

## 4. Key Deployment Coupling & Pre-Requisites

1. **Backend Port Binding:**
   `application.yml` should accept `${PORT}` in addition to `${SERVER_PORT}` so that platforms assigning dynamic ports (e.g. Render, Railway, Cloud Run, Heroku) function smoothly without port collisions.
2. **CORS Allowed Origins:**
   `application.yml` must allow expanding `cors.allowed-origins` via environment variable `${CORS_ALLOWED_ORIGINS}` so live frontend domains can interact with the API with credentials.
3. **Database PostGIS Requirement:**
   The cloud database provider **must** support the PostGIS extension (`CREATE EXTENSION IF NOT EXISTS postgis`).
4. **Firebase Service Account:**
   The service account JSON (`mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json`) must be accessible to the deployed backend container via path mount or environment variable.

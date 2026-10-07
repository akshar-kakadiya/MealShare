# MealShare — Final Architecture Documentation

> **Version**: 1.0  
> **Last Updated**: 2026-09-06  
> **Status**: APPROVED & IMPLEMENTED

---

## 1. System Overview

MealShare is a surplus-food redistribution platform connecting food donors with receiver organizations and volunteers for last-mile delivery.

```
┌───────────────────────────────────────────────────────────────────┐
│                        MealShare Platform                         │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│   React.js (Admin Web)     Flutter Mobile App                     │
│   ┌──────────────────┐     ┌────────────────────────────────┐    │
│   │  Admin Dashboard  │     │  Donor  │ Receiver │ Volunteer │    │
│   │  (ERP / Mgmt)     │     │  Role   │  Role    │   Role    │    │
│   └────────┬─────────┘     └──────────┬─────────────────────┘    │
│            │                          │                           │
│            ▼                          ▼                           │
│   ┌────────────────────────────────────────────────┐              │
│   │           Firebase Authentication               │              │
│   │     (Email/Password, Token Issuance)            │              │
│   └────────────────────┬───────────────────────────┘              │
│                        │ Firebase ID Token                        │
│                        ▼                                          │
│   ┌────────────────────────────────────────────────┐              │
│   │         Spring Boot REST API (Java 17)          │              │
│   │   • Firebase Admin SDK (token verification)     │              │
│   │   • Spring Security (authorization, RBAC)       │              │
│   │   • Spring Data JPA + Hibernate Spatial         │              │
│   │   • Flyway (migrations)                         │              │
│   │   • Swagger / OpenAPI                           │              │
│   └────────────────────┬───────────────────────────┘              │
│                        │                                          │
│                        ▼                                          │
│   ┌────────────────────────────────────────────────┐              │
│   │       PostgreSQL 18 + PostGIS                   │              │
│   │       Database: mealshare_app                   │              │
│   └────────────────────────────────────────────────┘              │
│                                                                   │
│   Future Integrations (not yet implemented):                      │
│   ┌──────────┐ ┌─────┐ ┌───────────┐ ┌────────────┐             │
│   │ WebSocket│ │ FCM │ │ OSM/OSRM  │ │ Python ML  │             │
│   │          │ │     │ │           │ │ + OR-Tools  │             │
│   └──────────┘ └─────┘ └───────────┘ └────────────┘             │
└───────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

### Frontend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Admin Web** | React.js + TypeScript + Vite | Admin-only dashboard (ERP, management) |
| **Mobile App** | Flutter (single app) | Donor + Receiver + Volunteer roles |

> ⚠️ **React.js = ADMIN WEB ONLY**  
> ⚠️ **Flutter = DONOR / RECEIVER / VOLUNTEER MOBILE APPLICATION**  
> There are NO web dashboards for Donor, Receiver, or Volunteer.

### Backend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **API** | Java 17 + Spring Boot 3.4 | REST API server |
| **Build** | Maven (via Maven Wrapper) | Build automation |
| **Security** | Spring Security + Firebase Admin SDK | Authentication & authorization |
| **ORM** | Spring Data JPA + Hibernate 6 | Database access |
| **Spatial** | Hibernate Spatial + PostGIS | Geolocation queries |
| **Migrations** | Flyway | Database schema management |
| **API Docs** | SpringDoc OpenAPI / Swagger UI | API documentation |
| **Monitoring** | Spring Actuator | Health checks, metrics |

### Authentication

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Identity Provider** | Firebase Authentication | User identity, login, registration, token issuance |
| **Token Verification** | Firebase Admin SDK (server) | Server-side ID token verification |
| **Web Client Auth** | Firebase Web SDK | Admin web authentication |
| **Mobile Client Auth** | Firebase Flutter SDK | Mobile app authentication |

> ⚠️ **NO custom JWT** — Spring Boot does NOT issue its own access/refresh tokens.  
> Firebase handles all token lifecycle (issuance, refresh, revocation).

### Database

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **RDBMS** | PostgreSQL 18 | Primary data store |
| **Spatial** | PostGIS | Geolocation support |
| **Database Name** | `mealshare_app` | Application database |
| **Migrations** | Flyway | Schema versioning |

### Infrastructure (Docker)

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| PostgreSQL + PostGIS | postgis/postgis:16-3.4 | 5432 | Relational & spatial database |
| Redis | redis:7.2-alpine | 6379 | Cache, session revocation (future) |
| MinIO | minio/minio | 9000/9001 | Object storage for evidence (future) |

### Future Integrations

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Real-Time** | WebSocket + Firebase Cloud Messaging (FCM) | Push notifications, live updates |
| **Maps & Routing** | OpenStreetMap + OSRM | Map display, route optimization |
| **AI/ML** | Python + Machine Learning | Predictive analytics |
| **Optimization** | Google OR-Tools | Delivery route optimization |
| **Cloud Storage** | Firebase Storage + AWS/Cloud | File storage |

---

## 3. Authentication Flow

```
┌──────────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│  Flutter / React  │     │  Firebase Auth        │     │  Spring Boot API  │
│  Client           │     │  (Google Cloud)       │     │  Backend          │
└────────┬─────────┘     └──────────┬───────────┘     └────────┬─────────┘
         │                          │                           │
         │  1. signIn(email, pwd)   │                           │
         │ ─────────────────────► │                           │
         │                          │                           │
         │  2. Firebase ID Token    │                           │
         │ ◄───────────────────── │                           │
         │                          │                           │
         │  3. API Request          │                           │
         │  Authorization: Bearer <token>                       │
         │ ──────────────────────────────────────────────────► │
         │                          │                           │
         │                          │  4. Verify ID Token       │
         │                          │ ◄──────────────────────── │
         │                          │                           │
         │                          │  5. Token Claims          │
         │                          │ ────────────────────────► │
         │                          │                           │
         │                          │     6. Map UID → User     │
         │                          │     7. Check Role/Auth    │
         │                          │     8. Process Request    │
         │                          │                           │
         │  9. API Response         │                           │
         │ ◄────────────────────────────────────────────────── │
```

### Responsibilities

| Layer | Responsibility |
|-------|---------------|
| **Firebase** | Identity, email/password auth, login, registration, token issuance, token refresh |
| **Spring Boot** | Verify Firebase ID token, map Firebase UID → MealShare User, authorization, role checks, business rules |
| **React** | Firebase Web SDK for admin login |
| **Flutter** | Firebase Flutter SDK for mobile login/registration |

---

## 4. Project Directory Structure

```
MealShare/
├── backend/                    ← Java + Spring Boot REST API
│   ├── .mvn/wrapper/           ← Maven Wrapper
│   ├── src/main/java/com/mealshare/
│   │   ├── MealShareApplication.java
│   │   ├── config/             ← Firebase, Security, CORS, OpenAPI configs
│   │   ├── security/           ← Firebase auth filter, principal, token
│   │   ├── user/               ← User entity, repository, service, controller
│   │   ├── donor/              ← DonorProfile entity (foundation)
│   │   ├── receiver/           ← ReceiverOrganization entity (foundation)
│   │   ├── volunteer/          ← VolunteerProfile entity (foundation)
│   │   ├── common/             ← Error handling, health controller, DTOs
│   │   └── audit/              ← AuditLog entity (foundation)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/       ← Flyway SQL migrations
│   ├── src/test/               ← Unit & integration tests
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── .env.example
│
├── admin-web/                  ← React.js Admin Dashboard (ADMIN ONLY)
│   ├── src/
│   │   ├── config/firebase.ts
│   │   ├── contexts/AuthContext.tsx
│   │   ├── api/client.ts
│   │   ├── pages/LoginPage.tsx
│   │   ├── pages/DashboardPage.tsx
│   │   ├── components/ProtectedRoute.tsx
│   │   └── layouts/AdminLayout.tsx
│   ├── .env.example
│   └── package.json
│
├── mobile/                     ← Flutter App (DONOR + RECEIVER + VOLUNTEER)
│   ├── lib/
│   │   ├── auth/               ← Auth service, login, register screens
│   │   ├── routing/            ← Role-based routing (GoRouter)
│   │   ├── home/               ← Role-specific home screens
│   │   ├── config/             ← API config
│   │   └── shared/             ← API client
│   ├── pubspec.yaml
│   └── FIREBASE_SETUP.md
│
├── docs/                       ← Documentation
│   └── MealShare_Architecture.md  (this file)
│
├── legacy/                     ← Archived reference implementations
│   ├── nestjs-backend/         ← NestJS + Prisma + JWT (REFERENCE ONLY)
│   └── react-frontend/        ← React multi-role web app (REFERENCE ONLY)
│
├── docker-compose.yml          ← PostgreSQL + PostGIS, Redis, MinIO
├── setup_database.sql          ← Database creation script
└── .gitignore
```

---

## 5. User Roles

| Role | Code | Platform | Description |
|------|------|----------|-------------|
| Donor | `DONOR` | Flutter Mobile | Food donors (individuals, hotels, restaurants, etc.) |
| Receiver Organization | `RECEIVER_ORG` | Flutter Mobile | NGOs, shelters, community kitchens |
| Receiver Volunteer | `RECEIVER_VOL` | Flutter Mobile | Individual volunteers receiving/distributing food |
| Admin | `ADMIN` | React Web | Platform administrators |

> Admin accounts CANNOT be created through public registration.

---

## 6. API Foundation

| Endpoint | Auth | Description |
|----------|------|-------------|
| `GET /api/v1/health` | Public | Health check |
| `GET /api/v1/users/me` | Firebase Token | Current user profile |
| `GET /swagger-ui.html` | Public | API documentation |
| `GET /actuator/health` | Public | Spring Actuator health |

---

## 7. Database: mealshare_app

### Foundation Tables

| Table | Description |
|-------|-------------|
| `users` | Core user accounts (Firebase UID, email, role, status) |
| `donor_profiles` | Donor-specific profile data |
| `receiver_organizations` | Receiver organization profiles |
| `volunteer_profiles` | Volunteer profiles |
| `audit_logs` | System audit trail |

### Legacy Database

The original `mealshare` database (created by NestJS/Prisma) remains **untouched** as a reference. The new Spring Boot application uses `mealshare_app` exclusively.

---

## 8. Setup Instructions

### Prerequisites
1. Java 17+ (verified: OpenJDK 17.0.18)
2. Node.js 24+ (verified: v24.13.1)
3. Flutter 3.44+ (verified: Flutter 3.44.0 stable)
4. PostgreSQL 18+ with PostGIS
5. Firebase project with Authentication enabled

### Database Setup
```bash
# Using psql:
psql -U postgres -h localhost -c "CREATE DATABASE mealshare_app;"

# Or run the provided script:
psql -U postgres -h localhost -f setup_database.sql
```

### Backend (Spring Boot)
```bash
cd backend

# Configure Firebase (copy and edit):
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# Edit application-local.yml with your Firebase service account path

# Build and run:
./mvnw.cmd clean compile      # Windows
./mvnw clean compile           # Linux/Mac
./mvnw.cmd spring-boot:run     # Run the application
```

### Admin Web (React)
```bash
cd admin-web
npm install

# Configure Firebase (copy and edit):
cp .env.example .env
# Edit .env with your Firebase project credentials

npm run dev    # Development server on http://localhost:5173
```

### Mobile (Flutter)
```bash
cd mobile
flutter pub get

# See FIREBASE_SETUP.md for Firebase configuration
# After configuring Firebase:
flutter run
```

### Firebase Configuration Required
1. **Backend**: Firebase service account JSON → set `FIREBASE_CREDENTIALS_PATH`
2. **Admin Web**: Firebase web app config → set `VITE_FIREBASE_*` variables in `.env`
3. **Mobile**: `google-services.json` (Android) and/or `GoogleService-Info.plist` (iOS)

See `mobile/FIREBASE_SETUP.md` for detailed mobile Firebase setup.

---

## 9. Architecture Decisions

### ❌ NOT Part of Final Architecture
- NestJS
- Prisma ORM
- Passport.js
- Custom JWT (access/refresh tokens generated by backend)
- Web dashboards for Donor, Receiver, or Volunteer

### ✅ Final Architecture Choices
- Java + Spring Boot (backend)
- Firebase Authentication (identity & tokens)
- Spring Security + Firebase Admin SDK (server-side auth)
- PostgreSQL + PostGIS (database)
- Flyway (migrations)
- React.js (admin web only)
- Flutter (mobile app for all non-admin roles)

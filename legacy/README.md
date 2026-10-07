# MealShare — Legacy / Reference Implementations

> **Status**: ARCHIVED — Reference Only  
> **Archived**: 2026-09-06

This directory contains the **earlier implementation** of MealShare that used a different technology stack. It is preserved as a **business-rules reference** for the new final architecture.

## Contents

### `nestjs-backend/`
- **Stack**: NestJS + Prisma + PostgreSQL + Passport JWT
- **Modules**: Auth, Users, Donors, Receivers, Volunteers, Admin, Donations, Requests
- **Key Reference Material**:
  - `prisma/schema.prisma` — Comprehensive domain model (461 lines, all enums, relations)
  - `src/modules/donations/` — Donation lifecycle, food-safety validation, FCFS allocation
  - `src/modules/requests/` — Request lifecycle, concurrency, donor approval
  - `src/modules/auth/` — Registration, login, RBAC concepts
  - `src/common/guards/` — JWT auth guard, roles guard
  - `test/` — E2E tests with business rule validation
  - `.env.example` — Configuration reference

### `react-frontend/`
- **Stack**: React 19 + Vite + TypeScript + Tailwind CSS + Axios
- **Pages**: Login, Register, Admin/Donor/Receiver/Volunteer Dashboards
- **Key Reference Material**:
  - `src/contexts/AuthContext.tsx` — Auth state management patterns
  - `src/routes/` — Role-based routing, protected routes
  - `src/services/` — API service patterns
  - `src/types/` — TypeScript type definitions

## ⚠️ Important Notes

1. **Do NOT modify** these files — they are reference-only
2. **Do NOT run** `npm install`, `prisma migrate`, or `nest start` from this directory
3. **Do NOT delete** until the new Spring Boot implementation has reached equivalent business functionality
4. The **final architecture** is documented in `docs/MealShare_Architecture.md`

## Why This Exists

The legacy implementation contains valuable **business rules and domain knowledge**:
- Donation model with food-safety fields
- FCFS (First-Come-First-Served) request allocation logic
- Donor approval workflow
- OTP handover concept
- PostGIS nearby discovery queries
- Request lifecycle state machine
- Audit logging patterns
- RBAC authorization patterns
- API contract designs
- Validation rules

These rules should be **translated** (not copied) into the new Java + Spring Boot architecture.

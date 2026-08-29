# MealShare Backend API

Surplus-Food Redistribution Platform — NestJS Modular Monolith API.

Built strictly according to:
- `MealShare PRD v1.0`
- `MealShare SRS v1.0`
- `MealShare SADD v1.0`
- `MealShare API Contracts v1.0`

---

## 🛠 Technology Stack

- **Framework**: NestJS (Node.js 20 LTS / TypeScript)
- **Database**: PostgreSQL 16 + PostGIS 3.4 (Prisma ORM)
- **Cache & Real-Time**: Redis 7.2
- **Authentication**: Stateless JWT + Refresh Token Rotation + Argon2/Bcrypt + Role Guards
- **Documentation**: Swagger OpenAPI 3.0 at `/api/docs`

---

## 🚀 Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Environment Configuration
Copy the `.env.example` file:
```bash
cp .env.example .env
```

### 3. Generate Database Client
```bash
npx prisma generate
```

### 4. Run Development Server
```bash
npm run start:dev
```
API runs at `http://localhost:3000/api/v1`  
Swagger Documentation is available at `http://localhost:3000/api/docs`

---

## 🧪 Testing

```bash
# Run Unit Tests
npm run test

# Run End-to-End (E2E) Integration Tests
npm run test:e2e
```

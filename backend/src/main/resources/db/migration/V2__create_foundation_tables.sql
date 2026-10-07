-- =============================================================================
-- MealShare Foundation Schema
-- Database: PostgreSQL + PostGIS
-- =============================================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid    VARCHAR(128) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(20),
    name            VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('DONOR', 'RECEIVER_ORG', 'RECEIVER_VOL', 'ADMIN')),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION'
                    CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'RESTRICTED', 'SUSPENDED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Donor profiles
CREATE TABLE IF NOT EXISTS donor_profiles (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    donor_type                  VARCHAR(30) NOT NULL,
    entity_name                 VARCHAR(255) NOT NULL,
    address_text                TEXT NOT NULL,
    latitude                    DOUBLE PRECISION NOT NULL,
    longitude                   DOUBLE PRECISION NOT NULL,
    policy_accepted             BOOLEAN NOT NULL DEFAULT FALSE,
    rating_average              DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    completed_donations_count   INTEGER NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Receiver organizations
CREATE TABLE IF NOT EXISTS receiver_organizations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    org_type                VARCHAR(30) NOT NULL,
    org_name                VARCHAR(255) NOT NULL,
    registration_number     VARCHAR(100) NOT NULL,
    address_text            TEXT NOT NULL,
    service_latitude        DOUBLE PRECISION NOT NULL,
    service_longitude       DOUBLE PRECISION NOT NULL,
    service_radius_meters   DOUBLE PRECISION NOT NULL DEFAULT 10000.0,
    verification_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    beneficiary_capacity    INTEGER NOT NULL DEFAULT 50,
    rating_average          DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Volunteer profiles
CREATE TABLE IF NOT EXISTS volunteer_profiles (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name                   VARCHAR(255) NOT NULL,
    profession                  VARCHAR(100),
    about_bio                   TEXT,
    service_latitude            DOUBLE PRECISION NOT NULL,
    service_longitude           DOUBLE PRECISION NOT NULL,
    service_radius_meters       DOUBLE PRECISION NOT NULL DEFAULT 10000.0,
    completed_deliveries_count  INTEGER NOT NULL DEFAULT 0,
    total_meals_delivered        INTEGER NOT NULL DEFAULT 0,
    rating_average              DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   VARCHAR(255),
    actor_role      VARCHAR(20) NOT NULL,
    action_type     VARCHAR(50) NOT NULL,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(255) NOT NULL,
    delta_state     JSONB,
    client_ip       VARCHAR(45),
    user_agent      TEXT,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

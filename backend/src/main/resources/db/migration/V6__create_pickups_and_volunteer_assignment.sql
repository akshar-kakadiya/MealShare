-- =============================================================================
-- MealShare Pickups and Volunteer Assignment Schema
-- Database: PostgreSQL + PostGIS
-- =============================================================================

-- Add availability_status to volunteer_profiles
ALTER TABLE volunteer_profiles
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
    CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'INACTIVE'));

CREATE INDEX IF NOT EXISTS idx_volunteer_profiles_availability
    ON volunteer_profiles(availability_status);

-- Pickups table
CREATE TABLE IF NOT EXISTS pickups (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_request_id     UUID NOT NULL UNIQUE REFERENCES donation_requests(id) ON DELETE CASCADE,
    volunteer_id            UUID NOT NULL REFERENCES volunteer_profiles(id) ON DELETE CASCADE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED'
                            CHECK (status IN ('ASSIGNED', 'PICKED_UP', 'COMPLETED', 'CANCELLED')),
    scheduled_at            TIMESTAMPTZ NOT NULL,
    assigned_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    otp_hash                VARCHAR(255) NOT NULL,
    otp_expires_at          TIMESTAMPTZ NOT NULL,
    otp_attempts            INTEGER NOT NULL DEFAULT 0,
    otp_verified_at         TIMESTAMPTZ,
    picked_up_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pickups_volunteer_id ON pickups(volunteer_id);
CREATE INDEX IF NOT EXISTS idx_pickups_donation_request_id ON pickups(donation_request_id);
CREATE INDEX IF NOT EXISTS idx_pickups_status ON pickups(status);

-- =============================================================================
-- MealShare Schema Migration V11
-- Reconcile Operational Pickup Workflow & Real-Time Logistics Tracking
-- Database: PostgreSQL + PostGIS
-- =============================================================================

-- 1. Update status check constraint on pickups table
ALTER TABLE pickups DROP CONSTRAINT IF EXISTS pickups_status_check;
ALTER TABLE pickups ADD CONSTRAINT pickups_status_check
    CHECK (status IN ('ASSIGNED', 'PICKUP_PENDING', 'EN_ROUTE_TO_PICKUP', 'AT_PICKUP', 'PICKED_UP', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED'));

-- 2. Add operational journey, evidence, and tracking columns to pickups table
ALTER TABLE pickups
    ADD COLUMN IF NOT EXISTS started_pickup_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS arrived_at_pickup_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS food_inspected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS inspection_notes TEXT,
    ADD COLUMN IF NOT EXISTS inspection_rejected BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pickup_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS pickup_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS pickup_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS started_delivery_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS arrived_at_delivery_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delivery_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS delivery_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS delivery_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS delivered_quantity DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS quantity_discrepancy_reason TEXT,
    ADD COLUMN IF NOT EXISTS current_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS current_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS tracking_active BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Create real-time pickup location updates tracking table
CREATE TABLE IF NOT EXISTS pickup_location_updates (
    id              BIGSERIAL PRIMARY KEY,
    pickup_id       UUID NOT NULL REFERENCES pickups(id) ON DELETE CASCADE,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    speed           DOUBLE PRECISION,
    heading         DOUBLE PRECISION,
    phase           VARCHAR(30) NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_location_updates_pickup_id ON pickup_location_updates(pickup_id, recorded_at DESC);

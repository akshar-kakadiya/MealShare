-- =============================================================================
-- MealShare Donations Schema & PostGIS Spatial Table
-- =============================================================================

CREATE TABLE IF NOT EXISTS donations (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donor_id                        UUID NOT NULL REFERENCES donor_profiles(id) ON DELETE CASCADE,
    title                           VARCHAR(255) NOT NULL,
    description                     TEXT,
    food_category                   VARCHAR(50) NOT NULL
                                    CHECK (food_category IN ('PREPARED_MEALS', 'PACKAGED_FOOD', 'BAKERY', 'FRUITS', 'VEGETABLES', 'GRAINS', 'DAIRY', 'OTHER')),
    quantity                        DOUBLE PRECISION NOT NULL CHECK (quantity > 0),
    quantity_unit                   VARCHAR(50) NOT NULL,
    preparation_datetime            TIMESTAMPTZ NOT NULL,
    consume_before                  TIMESTAMPTZ NOT NULL,
    pickup_available_from           TIMESTAMPTZ NOT NULL,
    pickup_available_until          TIMESTAMPTZ NOT NULL,
    pickup_address                  TEXT NOT NULL,
    latitude                        DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90.0 AND 90.0),
    longitude                       DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180.0 AND 180.0),
    location                        GEOMETRY(Point, 4326) NOT NULL,
    food_safety_declared            BOOLEAN NOT NULL DEFAULT FALSE,
    food_safety_declaration_text    TEXT,
    food_safety_declared_at         TIMESTAMPTZ,
    status                          VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE'
                                    CHECK (status IN ('AVAILABLE', 'REQUESTED', 'APPROVED', 'PICKUP_ASSIGNED', 'PICKED_UP', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donations_donor_id ON donations(donor_id);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_created_at ON donations(created_at DESC);
CREATE INDEX idx_donations_location_gist ON donations USING GIST (location);

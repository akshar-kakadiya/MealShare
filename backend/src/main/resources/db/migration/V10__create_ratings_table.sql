-- =============================================================================
-- MealShare Schema Migration V10
-- Create Ratings & Feedback Table
-- =============================================================================

CREATE TABLE IF NOT EXISTS ratings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pickup_id       UUID NOT NULL REFERENCES pickups(id) ON DELETE CASCADE,
    rater_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rated_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating          INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    feedback        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ratings_pickup_rater UNIQUE (pickup_id, rater_user_id),
    CONSTRAINT chk_ratings_different_users CHECK (rater_user_id <> rated_user_id)
);

CREATE INDEX IF NOT EXISTS idx_ratings_pickup_id ON ratings(pickup_id);
CREATE INDEX IF NOT EXISTS idx_ratings_rater_user_id ON ratings(rater_user_id);
CREATE INDEX IF NOT EXISTS idx_ratings_rated_user_id ON ratings(rated_user_id);
CREATE INDEX IF NOT EXISTS idx_ratings_created_at ON ratings(created_at DESC);

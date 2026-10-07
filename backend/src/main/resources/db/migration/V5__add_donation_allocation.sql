-- =============================================================================
-- MealShare Add Donation Allocated Quantity
-- =============================================================================

ALTER TABLE donations
ADD COLUMN IF NOT EXISTS allocated_quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE donations
DROP CONSTRAINT IF EXISTS chk_donations_allocated_quantity_non_negative,
ADD CONSTRAINT chk_donations_allocated_quantity_non_negative
CHECK (allocated_quantity >= 0.0);

ALTER TABLE donations
DROP CONSTRAINT IF EXISTS chk_donations_allocated_le_quantity,
ADD CONSTRAINT chk_donations_allocated_le_quantity
CHECK (allocated_quantity <= quantity);

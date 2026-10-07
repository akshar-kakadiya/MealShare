-- =============================================================================
-- MealShare Add Donation Request Allocated Quantity & Constraints
-- =============================================================================

ALTER TABLE donation_requests
ADD COLUMN IF NOT EXISTS allocated_quantity DOUBLE PRECISION NOT NULL DEFAULT 0.0;

-- Backfill existing APPROVED and COMPLETED requests with their requested quantity
UPDATE donation_requests
SET allocated_quantity = requested_quantity
WHERE status IN ('APPROVED', 'COMPLETED');

-- Ensure non-negative allocated quantity constraint
ALTER TABLE donation_requests
DROP CONSTRAINT IF EXISTS chk_donation_requests_allocated_quantity_non_negative,
ADD CONSTRAINT chk_donation_requests_allocated_quantity_non_negative
CHECK (allocated_quantity >= 0.0);

-- Ensure allocated quantity cannot exceed requested quantity
ALTER TABLE donation_requests
DROP CONSTRAINT IF EXISTS chk_donation_requests_allocated_le_requested,
ADD CONSTRAINT chk_donation_requests_allocated_le_requested
CHECK (allocated_quantity <= requested_quantity);

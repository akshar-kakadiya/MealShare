-- =============================================================================
-- Migration V14: Enforce that food consume-before deadline is strictly after pickup window end
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_donations_consume_after_pickup'
    ) THEN
        ALTER TABLE donations
            ADD CONSTRAINT chk_donations_consume_after_pickup
            CHECK (consume_before > pickup_available_until);
    END IF;
END $$;

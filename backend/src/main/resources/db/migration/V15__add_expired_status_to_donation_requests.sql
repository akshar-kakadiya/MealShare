-- =============================================================================
-- Migration V15: Add 'EXPIRED' status to donation_requests check constraint
-- =============================================================================

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT con.conname
        FROM pg_constraint con
        INNER JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'donation_requests'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%status%IN%'
    ) LOOP
        EXECUTE 'ALTER TABLE donation_requests DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;

    ALTER TABLE donation_requests
        ADD CONSTRAINT chk_donation_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'COMPLETED', 'EXPIRED'));
END $$;

-- =============================================================================
-- MealShare Schema Migration V9
-- Reconcile to 3-Role Model: DONOR, RECEIVER, ADMIN
-- Remove separate Volunteer role, volunteer_profiles table, and volunteer pickup references
-- =============================================================================

-- 1. Drop existing role CHECK constraint on users table
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- 2. Reconcile existing user roles to RECEIVER
UPDATE users SET role = 'RECEIVER' WHERE role IN ('RECEIVER_ORG', 'RECEIVER_VOL');
UPDATE audit_logs SET actor_role = 'RECEIVER' WHERE actor_role IN ('RECEIVER_ORG', 'RECEIVER_VOL');

-- 3. Add new role CHECK constraint on users table
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('DONOR', 'RECEIVER', 'ADMIN'));

-- 3. Transition pickups table from volunteer_id to receiver_id
ALTER TABLE pickups ADD COLUMN IF NOT EXISTS receiver_id UUID REFERENCES receiver_organizations(id) ON DELETE CASCADE;

-- Backfill receiver_id from associated donation_requests
UPDATE pickups p
SET receiver_id = dr.receiver_org_id
FROM donation_requests dr
WHERE p.donation_request_id = dr.id AND p.receiver_id IS NULL;

-- Drop foreign key and index on volunteer_id
ALTER TABLE pickups DROP CONSTRAINT IF EXISTS pickups_volunteer_id_fkey;
DROP INDEX IF EXISTS idx_pickups_volunteer_id;
ALTER TABLE pickups DROP COLUMN IF EXISTS volunteer_id;

-- Set NOT NULL and add index on receiver_id
ALTER TABLE pickups ALTER COLUMN receiver_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pickups_receiver_id ON pickups(receiver_id);

-- 4. Drop volunteer_profiles table
DROP TABLE IF EXISTS volunteer_profiles CASCADE;

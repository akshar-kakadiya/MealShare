-- =============================================================================
-- MealShare - Safe Clean Local Development Reset
-- Removes prototype/demo operational records and test accounts while
-- preserving the real platform administrator account and database schema.
-- =============================================================================

BEGIN;

-- 1. Remove operational records
DELETE FROM ratings;
DELETE FROM pickup_location_updates;
DELETE FROM pickups;
DELETE FROM donation_requests;
DELETE FROM donations;
DELETE FROM notifications;
DELETE FROM user_device_tokens;

-- 2. Remove role profiles
DELETE FROM donor_profiles;
DELETE FROM receiver_organizations;

-- 3. Remove non-admin users (preserve the admin account)
DELETE FROM users WHERE role != 'ADMIN' AND email != 'admin@mealshare.org';

-- 4. Clean non-admin audit logs
DELETE FROM audit_logs WHERE actor_role != 'ADMIN';

-- 5. Ensure the administrator account is present, active, and complete
INSERT INTO users (id, firebase_uid, email, phone, name, role, status, category, profile_status, verification_status, created_at, updated_at)
VALUES (
    '8ff89794-edb1-48c0-94eb-c584815263cb',
    'uSkzPzgPUVQBRES9AaDsF1ONqEG2',
    'admin@mealshare.org',
    '+919876543210',
    'Platform Administrator',
    'ADMIN',
    'ACTIVE',
    NULL,
    'COMPLETE',
    'APPROVED',
    NOW() - INTERVAL '30 days',
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    firebase_uid = 'uSkzPzgPUVQBRES9AaDsF1ONqEG2',
    email = 'admin@mealshare.org',
    role = 'ADMIN',
    status = 'ACTIVE',
    profile_status = 'COMPLETE',
    verification_status = 'APPROVED',
    updated_at = NOW();

COMMIT;

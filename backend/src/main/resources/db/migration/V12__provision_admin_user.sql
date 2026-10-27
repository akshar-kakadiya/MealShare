-- =============================================================================
-- MealShare Schema Migration V12
-- Provision Canonical Platform Administrator User (Idempotent)
-- =============================================================================

-- 1. Insert canonical platform administrator only if neither ID, email, nor default Firebase UID exists
INSERT INTO users (id, firebase_uid, email, phone, name, role, status, created_at, updated_at)
SELECT
    '8ff89794-edb1-48c0-94eb-c584815263cb',
    'V9VdFS2zWydjDyRMocw4smfyMFq2',
    'admin@mealshare.org',
    '+919876543210',
    'Platform Administrator',
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users
    WHERE id = '8ff89794-edb1-48c0-94eb-c584815263cb'
       OR email = 'admin@mealshare.org'
       OR firebase_uid = 'V9VdFS2zWydjDyRMocw4smfyMFq2'
);

-- 2. If the administrator account already exists, safely ensure ADMIN role without
-- overwriting custom Firebase UID, user identity, or active/suspended status
UPDATE users
SET role = 'ADMIN',
    status = CASE WHEN status = 'PENDING_VERIFICATION' THEN 'ACTIVE' ELSE status END,
    updated_at = NOW()
WHERE (id = '8ff89794-edb1-48c0-94eb-c584815263cb'
    OR email = 'admin@mealshare.org'
    OR firebase_uid = 'V9VdFS2zWydjDyRMocw4smfyMFq2')
  AND (role != 'ADMIN' OR status = 'PENDING_VERIFICATION');

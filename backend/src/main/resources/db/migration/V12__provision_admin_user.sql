-- =============================================================================
-- MealShare Schema Migration V12
-- Provision Canonical Platform Administrator User
-- =============================================================================

INSERT INTO users (id, firebase_uid, email, phone, name, role, status, created_at, updated_at)
VALUES (
    '8ff89794-edb1-48c0-94eb-c584815263cb',
    'V9VdFS2zWydjDyRMocw4smfyMFq2',
    'admin@mealshare.org',
    '+919876543210',
    'Platform Administrator',
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
)
ON CONFLICT (firebase_uid) DO UPDATE
SET email = EXCLUDED.email, role = 'ADMIN', status = 'ACTIVE', updated_at = NOW();

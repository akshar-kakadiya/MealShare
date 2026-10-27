-- =============================================================================
-- MealShare Schema Migration V13
-- Role-Based Registration, Category Attributes, Profile Completion & Unified Verification
-- =============================================================================

-- 1. Extend users table with category, profile completion, and verification state
ALTER TABLE users ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_status VARCHAR(30) NOT NULL DEFAULT 'INCOMPLETE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE users ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS action_required_notes TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified_by VARCHAR(128);

-- 2. Extend donor_profiles table with category fields and verification state
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255);
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS business_type VARCHAR(100);
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS event_info TEXT;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS photos TEXT;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS profile_status VARCHAR(30) NOT NULL DEFAULT 'INCOMPLETE';
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS action_required_notes TEXT;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE donor_profiles ADD COLUMN IF NOT EXISTS verified_by VARCHAR(128);

-- 3. Extend receiver_organizations table with category fields and action required state
-- Drop NOT NULL from registration_number so Individuals and Community Groups are not forced to provide one
ALTER TABLE receiver_organizations ALTER COLUMN registration_number DROP NOT NULL;

ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255);
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS photos TEXT;
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS profile_status VARCHAR(30) NOT NULL DEFAULT 'INCOMPLETE';
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS action_required_notes TEXT;
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE receiver_organizations ADD COLUMN IF NOT EXISTS verified_by VARCHAR(128);

-- 4. Normalize and backfill safe defaults for existing users and profiles
-- Existing active accounts are normalized from legacy 'VERIFIED' to 'APPROVED'. Genuinely PENDING accounts are preserved.
UPDATE users
SET profile_status = CASE
        WHEN status = 'ACTIVE' THEN 'COMPLETE'
        ELSE profile_status
    END,
    verification_status = CASE
        WHEN verification_status = 'VERIFIED' THEN 'APPROVED'
        WHEN verification_status IN ('APPROVED', 'REJECTED', 'ACTION_REQUIRED') THEN verification_status
        WHEN status = 'ACTIVE' THEN 'APPROVED'
        ELSE 'PENDING'
    END,
    category = COALESCE(category, CASE
        WHEN role = 'DONOR' THEN 'HOTEL_RESTAURANT'
        WHEN role = 'RECEIVER' THEN 'NGO'
        ELSE 'ADMIN'
    END),
    verified_at = CASE
        WHEN status = 'ACTIVE' OR verification_status = 'VERIFIED' THEN COALESCE(verified_at, NOW())
        ELSE NULL
    END
WHERE verification_status = 'VERIFIED'
   OR (status = 'ACTIVE' AND (verification_status IS NULL OR verification_status = 'PENDING'))
   OR (category IS NULL);

UPDATE donor_profiles
SET profile_status = CASE
        WHEN address_text IS NOT NULL AND address_text != 'Not Specified' AND latitude != 0.0 THEN 'COMPLETE'
        ELSE profile_status
    END,
    verification_status = CASE
        WHEN verification_status = 'VERIFIED' THEN 'APPROVED'
        WHEN verification_status IN ('APPROVED', 'REJECTED', 'ACTION_REQUIRED') THEN verification_status
        WHEN user_id IN (SELECT id FROM users WHERE status = 'ACTIVE') THEN 'APPROVED'
        ELSE 'PENDING'
    END,
    verified_at = CASE
        WHEN user_id IN (SELECT id FROM users WHERE status = 'ACTIVE') THEN COALESCE(verified_at, NOW())
        ELSE NULL
    END
WHERE verification_status = 'VERIFIED' OR verification_status IS NULL;

UPDATE receiver_organizations
SET profile_status = CASE
        WHEN address_text IS NOT NULL AND address_text != 'Not Specified' THEN 'COMPLETE'
        ELSE profile_status
    END,
    verification_status = CASE
        WHEN verification_status = 'VERIFIED' THEN 'APPROVED'
        WHEN verification_status IN ('APPROVED', 'REJECTED', 'ACTION_REQUIRED') THEN verification_status
        WHEN user_id IN (SELECT id FROM users WHERE status = 'ACTIVE') THEN 'APPROVED'
        ELSE 'PENDING'
    END,
    verified_at = CASE
        WHEN user_id IN (SELECT id FROM users WHERE status = 'ACTIVE') THEN COALESCE(verified_at, NOW())
        ELSE NULL
    END
WHERE verification_status = 'VERIFIED' OR verification_status IS NULL;

-- 5. Add / Update check constraints (safely after data normalization)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_profile_status_check'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_profile_status_check
            CHECK (profile_status IN ('INCOMPLETE', 'COMPLETE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_verification_status_check'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_verification_status_check
            CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACTION_REQUIRED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'donor_profiles_profile_status_check'
    ) THEN
        ALTER TABLE donor_profiles ADD CONSTRAINT donor_profiles_profile_status_check
            CHECK (profile_status IN ('INCOMPLETE', 'COMPLETE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'donor_profiles_verification_status_check'
    ) THEN
        ALTER TABLE donor_profiles ADD CONSTRAINT donor_profiles_verification_status_check
            CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACTION_REQUIRED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'receiver_organizations_profile_status_check'
    ) THEN
        ALTER TABLE receiver_organizations ADD CONSTRAINT receiver_organizations_profile_status_check
            CHECK (profile_status IN ('INCOMPLETE', 'COMPLETE'));
    END IF;
END $$;

ALTER TABLE receiver_organizations DROP CONSTRAINT IF EXISTS receiver_organizations_verification_status_check;
ALTER TABLE receiver_organizations ADD CONSTRAINT receiver_organizations_verification_status_check
    CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACTION_REQUIRED'));

-- 6. Indexes for fast filtering in Admin Portal and APIs
CREATE INDEX IF NOT EXISTS idx_users_category ON users(category);
CREATE INDEX IF NOT EXISTS idx_users_verification_status ON users(verification_status);
CREATE INDEX IF NOT EXISTS idx_users_profile_status ON users(profile_status);
CREATE INDEX IF NOT EXISTS idx_donor_profiles_verification_status ON donor_profiles(verification_status);
CREATE INDEX IF NOT EXISTS idx_receiver_orgs_verification_status ON receiver_organizations(verification_status);

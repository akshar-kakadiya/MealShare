-- =============================================================================
-- MealShare Database Setup Script
-- Run this ONCE to create the mealshare_app database
-- =============================================================================
--
-- INSTRUCTIONS:
-- 1. Open a PostgreSQL client (psql, pgAdmin, DBeaver, etc.)
-- 2. Connect to the PostgreSQL server as the postgres superuser
-- 3. Execute this script
--
-- Using psql from command line:
--   psql -U postgres -h localhost -f setup_database.sql
--
-- Using pgAdmin:
--   1. Connect to your PostgreSQL server
--   2. Open Query Tool
--   3. Paste and run this script
--
-- IMPORTANT:
-- - This script does NOT modify or delete the existing 'mealshare' database
-- - It only creates a new 'mealshare_app' database if it doesn't exist
-- - PostGIS extension will be enabled by Flyway migrations automatically
-- =============================================================================

-- Check and create mealshare_app database
-- Note: CREATE DATABASE cannot be in a transaction block,
-- so we use a DO block to check existence first

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mealshare_app') THEN
        PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE mealshare_app');
        RAISE NOTICE 'Database mealshare_app created successfully.';
    ELSE
        RAISE NOTICE 'Database mealshare_app already exists. Skipping creation.';
    END IF;
END
$$;

-- Alternative: If the above doesn't work (dblink not available),
-- run this command directly in psql:
--
--   CREATE DATABASE mealshare_app;
--
-- Or using createdb utility:
--
--   createdb -U postgres -h localhost mealshare_app

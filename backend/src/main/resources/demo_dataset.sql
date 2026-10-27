-- =============================================================================
-- MealShare Capstone Demo Dataset
-- Suitable for real frontend-to-backend demonstrations across:
-- React Admin Web + Flutter Donor, Receiver Org, and Volunteer Mobile apps.
-- Database: PostgreSQL 16 + PostGIS 3.4 (mealshare_app)
-- =============================================================================

-- Clean up any transient demo records while preserving schema
DELETE FROM ratings;
DELETE FROM audit_logs;
DELETE FROM notifications;
DELETE FROM pickups;
DELETE FROM donation_requests;
DELETE FROM donations;
DELETE FROM receiver_organizations;
DELETE FROM donor_profiles;
DELETE FROM users;

-- 1. Users (1 Admin, 1 Donor, 1 Receiver Organization, 1 Suspended Donor)
INSERT INTO users (id, firebase_uid, email, phone, name, role, status, category, profile_status, verification_status, created_at, updated_at) VALUES
('8ff89794-edb1-48c0-94eb-c584815263cb', 'uSkzPzgPUVQBRES9AaDsF1ONqEG2', 'admin@mealshare.org', '+919876543210', 'Platform Administrator', 'ADMIN', 'ACTIVE', NULL, 'COMPLETE', 'APPROVED', NOW() - INTERVAL '30 days', NOW()),
('11111111-1111-1111-1111-111111111111', 'drekxkp9uXV2g16JioDrgu7uXSk2', 'donor@mealshare.org', '+919876543211', 'Grand Gourmet Catering', 'DONOR', 'ACTIVE', 'HOTEL_RESTAURANT', 'COMPLETE', 'APPROVED', NOW() - INTERVAL '20 days', NOW()),
('22222222-2222-2222-2222-222222222221', 'pwvnvHgatoahwb9ndKfSM2TVFPG2', 'receiver@mealshare.org', '+919876543212', 'Hope Community Shelter', 'RECEIVER', 'ACTIVE', 'NGO', 'COMPLETE', 'APPROVED', NOW() - INTERVAL '15 days', NOW()),
('44444444-4444-4444-4444-444444444441', 'suspended_donor_uid', 'suspended@mealshare.org', '+919876543215', 'Suspended Account', 'DONOR', 'SUSPENDED', 'HOTEL_RESTAURANT', 'INCOMPLETE', 'REJECTED', NOW() - INTERVAL '2 days', NOW());

-- 2. Profiles
INSERT INTO donor_profiles (id, user_id, entity_name, donor_type, address_text, latitude, longitude, policy_accepted, rating_average, completed_donations_count, profile_status, verification_status, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111112', '11111111-1111-1111-1111-111111111111', 'Grand Gourmet Catering Co.', 'HOTEL_RESTAURANT', '100 Feet Road, Indiranagar, Bengaluru, Karnataka 560038', 12.9716, 77.5946, true, 4.9, 12, 'COMPLETE', 'APPROVED', NOW() - INTERVAL '20 days', NOW());

INSERT INTO receiver_organizations (id, user_id, org_name, org_type, registration_number, address_text, service_latitude, service_longitude, service_radius_meters, verification_status, beneficiary_capacity, rating_average, profile_status, created_at, updated_at) VALUES
('22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222221', 'Hope Community Shelter Foundation', 'NGO', 'REG-KA-2024-8891', 'CMH Road, Indiranagar, Bengaluru, Karnataka 560038', 12.9780, 77.6400, 15000.0, 'APPROVED', 150, 4.8, 'COMPLETE', NOW() - INTERVAL '15 days', NOW());

-- 3. Realistic Demo Donations (3 records)
-- Donation 1: AVAILABLE (Active surplus available for discovery and requesting)
INSERT INTO donations (id, donor_id, title, description, food_category, quantity, allocated_quantity, quantity_unit, preparation_datetime, consume_before, pickup_available_from, pickup_available_until, pickup_address, latitude, longitude, location, food_safety_declared, food_safety_declaration_text, food_safety_declared_at, status, created_at, updated_at) VALUES
('a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111112', 'Fresh Vegetable Biryani & Mint Raita', 'Catering surplus prepared 2 hours ago. Kept hot in commercial thermal containers. Ready for immediate pickup.', 'PREPARED_MEALS', 40.0, 0.0, 'MEALS', NOW() - INTERVAL '2 hours', NOW() + INTERVAL '6 hours', NOW(), NOW() + INTERVAL '4 hours', '100 Feet Road, Indiranagar, Bengaluru', 12.9716, 77.5946, ST_SetSRID(ST_MakePoint(77.5946, 12.9716), 4326), true, 'Cooked in sanitized kitchen following hygiene and temperature protocols.', NOW() - INTERVAL '2 hours', 'AVAILABLE', NOW() - INTERVAL '2 hours', NOW());

-- Donation 2: AVAILABLE with Partial Allocation (Active surplus partially approved)
INSERT INTO donations (id, donor_id, title, description, food_category, quantity, allocated_quantity, quantity_unit, preparation_datetime, consume_before, pickup_available_from, pickup_available_until, pickup_address, latitude, longitude, location, food_safety_declared, food_safety_declaration_text, food_safety_declared_at, status, created_at, updated_at) VALUES
('a2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111112', 'Artisan Multigrain Bread & Baked Rolls', 'Fresh morning bakery batch surplus, sealed in food-grade kraft bags.', 'BAKERY', 25.0, 15.0, 'ITEMS', NOW() - INTERVAL '4 hours', NOW() + INTERVAL '24 hours', NOW(), NOW() + INTERVAL '12 hours', '12th Main, HAL 2nd Stage, Bengaluru', 12.9680, 77.6490, ST_SetSRID(ST_MakePoint(77.6490, 12.9680), 4326), true, 'Packaged in clean dry conditions without allergens.', NOW() - INTERVAL '4 hours', 'AVAILABLE', NOW() - INTERVAL '4 hours', NOW());

-- Donation 3: COMPLETED (Full lifecycle demonstrated)
INSERT INTO donations (id, donor_id, title, description, food_category, quantity, allocated_quantity, quantity_unit, preparation_datetime, consume_before, pickup_available_from, pickup_available_until, pickup_address, latitude, longitude, location, food_safety_declared, food_safety_declaration_text, food_safety_declared_at, status, created_at, updated_at) VALUES
('a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111112', 'Corporate Luncheon Buffet Food', 'Packaged healthy meals with dal, rice, steamed veggies and chapati.', 'PREPARED_MEALS', 30.0, 30.0, 'MEALS', NOW() - INTERVAL '1 day', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '24 hours', NOW() - INTERVAL '20 hours', 'MG Road, Bengaluru', 12.9750, 77.6050, ST_SetSRID(ST_MakePoint(77.6050, 12.9750), 4326), true, 'Hygiene verified and inspected prior to dispatch.', NOW() - INTERVAL '1 day', 'COMPLETED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '19 hours');

-- 4. Donation Requests (2 records)
-- Request 1: APPROVED for Donation 2 (Ready for Admin pickup scheduling)
INSERT INTO donation_requests (id, donation_id, receiver_org_id, requested_quantity, request_reason, status, requested_at, updated_at) VALUES
('b1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 15.0, 'Afternoon snack distribution for 15 children at the community shelter.', 'APPROVED', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '2 hours');

-- Request 2: COMPLETED for Donation 3 (Fully picked up and delivered)
INSERT INTO donation_requests (id, donation_id, receiver_org_id, requested_quantity, request_reason, status, requested_at, updated_at) VALUES
('b2222222-2222-2222-2222-222222222222', 'a3333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 30.0, 'Dinner service for homeless residents at Indiranagar shelter facility.', 'COMPLETED', NOW() - INTERVAL '23 hours', NOW() - INTERVAL '19 hours');

-- 5. Pickup (1 completed lifecycle record with BCrypt OTP hash and receiver_id)
INSERT INTO pickups (id, donation_request_id, receiver_id, status, scheduled_at, assigned_at, otp_hash, otp_expires_at, otp_attempts, otp_verified_at, picked_up_at, completed_at, created_at, updated_at) VALUES
('c1111111-1111-1111-1111-111111111111', 'b2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'COMPLETED', NOW() - INTERVAL '21 hours', NOW() - INTERVAL '21 hours', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', NOW() - INTERVAL '20 hours', 0, NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '19 hours', NOW() - INTERVAL '21 hours', NOW() - INTERVAL '19 hours');

-- 6. Notifications (Realistic operational notification flow)
INSERT INTO notifications (id, recipient_user_id, type, title, message, entity_type, entity_id, is_read, created_at, read_at) VALUES
('d1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'REQUEST_CREATED', 'New Donation Request', 'Hope Community Shelter Foundation requested 15.0 ITEMS of Artisan Multigrain Bread.', 'DONATION_REQUEST', 'b1111111-1111-1111-1111-111111111111', true, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '2 hours 30 minutes'),
('d2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222221', 'REQUEST_APPROVED', 'Donation Request Approved', 'Your request for 15.0 ITEMS of Artisan Multigrain Bread has been approved!', 'DONATION_REQUEST', 'b1111111-1111-1111-1111-111111111111', false, NOW() - INTERVAL '2 hours', null),
('d3333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222221', 'PICKUP_COMPLETED', 'Food Collection Completed', 'Delivery of 30 meals for Corporate Luncheon Buffet Food is complete.', 'PICKUP', 'c1111111-1111-1111-1111-111111111111', true, NOW() - INTERVAL '19 hours', NOW() - INTERVAL '18 hours');

-- 7. Audit Logs (Native PostgreSQL JSONB delta_state)
INSERT INTO audit_logs (actor_user_id, actor_role, action_type, entity_name, entity_id, delta_state, client_ip, user_agent, timestamp) VALUES
('11111111-1111-1111-1111-111111111111', 'DONOR', 'DONATION_CREATED', 'Donation', 'a1111111-1111-1111-1111-111111111111', '{"title": "Fresh Vegetable Biryani", "quantity": 40.0}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', NOW() - INTERVAL '2 hours'),
('22222222-2222-2222-2222-222222222221', 'RECEIVER', 'REQUEST_CREATED', 'DonationRequest', 'b1111111-1111-1111-1111-111111111111', '{"donationId": "a2222222-2222-2222-2222-222222222222", "quantity": 15.0}', '127.0.0.1', 'Dart/3.0 (dart:io)', NOW() - INTERVAL '3 hours'),
('11111111-1111-1111-1111-111111111111', 'DONOR', 'REQUEST_APPROVED', 'DonationRequest', 'b1111111-1111-1111-1111-111111111111', '{"allocatedQuantity": 15.0, "status": "APPROVED"}', '127.0.0.1', 'Dart/3.0 (dart:io)', NOW() - INTERVAL '2 hours'),
('8ff89794-edb1-48c0-94eb-c584815263cb', 'ADMIN', 'PICKUP_SCHEDULED', 'Pickup', 'c1111111-1111-1111-1111-111111111111', '{"receiverId": "22222222-2222-2222-2222-222222222222"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', NOW() - INTERVAL '21 hours'),
('22222222-2222-2222-2222-222222222221', 'RECEIVER', 'PICKUP_COMPLETED', 'Pickup', 'c1111111-1111-1111-1111-111111111111', '{"status": "COMPLETED"}', '127.0.0.1', 'Dart/3.0 (dart:io)', NOW() - INTERVAL '19 hours');

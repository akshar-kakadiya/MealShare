-- CreateEnum
CREATE TYPE "UserRole" AS ENUM ('Donor', 'Receiver_Org', 'Receiver_Vol', 'Admin');

-- CreateEnum
CREATE TYPE "UserStatus" AS ENUM ('Pending_Verification', 'Active', 'Restricted', 'Suspended');

-- CreateEnum
CREATE TYPE "DonorType" AS ENUM ('Individual', 'Hotel', 'Restaurant', 'Caterer', 'Hostel', 'Event_Organizer');

-- CreateEnum
CREATE TYPE "OrgType" AS ENUM ('NGO', 'Shelter', 'Old_Age_Home', 'Community_Kitchen', 'Other');

-- CreateEnum
CREATE TYPE "VerificationStatus" AS ENUM ('Unverified', 'Pending', 'Verified', 'Rejected');

-- CreateEnum
CREATE TYPE "DonationStatus" AS ENUM ('Draft', 'Posted', 'Requested', 'Approved', 'Pickup_Pending', 'Picked_Up', 'In_Transit', 'Delivered', 'Completed', 'Rejected', 'Cancelled', 'Expired', 'Disputed');

-- CreateEnum
CREATE TYPE "RequestStatus" AS ENUM ('Pending', 'Accepted', 'Rejected', 'Withdrawn', 'Voided_Expired');

-- CreateEnum
CREATE TYPE "StorageCondition" AS ENUM ('Refrigerated', 'Room_Temperature', 'Frozen', 'Heated_Container');

-- CreateEnum
CREATE TYPE "InspectionResult" AS ENUM ('Accepted', 'Rejected');

-- CreateEnum
CREATE TYPE "TrackingStatus" AS ENUM ('Active', 'Terminated', 'Interrupted');

-- CreateEnum
CREATE TYPE "ComplaintCategory" AS ENUM ('Food_Safety_Spoilage', 'Quantity_Discrepancy', 'Unprofessional_Conduct', 'Unjustified_Cancellation', 'Fraudulent_Documentation', 'Other');

-- CreateEnum
CREATE TYPE "ComplaintStatus" AS ENUM ('Open', 'Under_Investigation', 'Resolved', 'Dismissed');

-- CreateTable
CREATE TABLE "users" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "phone" TEXT NOT NULL,
    "password_hash" TEXT NOT NULL,
    "role" "UserRole" NOT NULL,
    "status" "UserStatus" NOT NULL DEFAULT 'Pending_Verification',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "refresh_tokens" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "token_hash" TEXT NOT NULL,
    "expires_at" TIMESTAMP(3) NOT NULL,
    "revoked" BOOLEAN NOT NULL DEFAULT false,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "refresh_tokens_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "donor_profiles" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "type" "DonorType" NOT NULL,
    "entity_name" TEXT NOT NULL,
    "address_text" TEXT NOT NULL,
    "latitude" DOUBLE PRECISION NOT NULL,
    "longitude" DOUBLE PRECISION NOT NULL,
    "policy_accepted" BOOLEAN NOT NULL DEFAULT false,
    "policy_accepted_at" TIMESTAMP(3),
    "policy_version" TEXT,
    "rating_average" DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    "completed_donations_count" INTEGER NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "donor_profiles_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "receiver_organizations" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "type" "OrgType" NOT NULL,
    "org_name" TEXT NOT NULL,
    "registration_number" TEXT NOT NULL,
    "address_text" TEXT NOT NULL,
    "service_latitude" DOUBLE PRECISION NOT NULL,
    "service_longitude" DOUBLE PRECISION NOT NULL,
    "service_radius_meters" DOUBLE PRECISION NOT NULL DEFAULT 10000.0,
    "verification_status" "VerificationStatus" NOT NULL DEFAULT 'Pending',
    "beneficiary_capacity" INTEGER NOT NULL DEFAULT 50,
    "rating_average" DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "receiver_organizations_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "volunteer_profiles" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "full_name" TEXT NOT NULL,
    "profession" TEXT NOT NULL,
    "about_bio" TEXT NOT NULL,
    "service_latitude" DOUBLE PRECISION NOT NULL,
    "service_longitude" DOUBLE PRECISION NOT NULL,
    "service_radius_meters" DOUBLE PRECISION NOT NULL DEFAULT 10000.0,
    "completed_deliveries_count" INTEGER NOT NULL DEFAULT 0,
    "total_meals_delivered" INTEGER NOT NULL DEFAULT 0,
    "rating_average" DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "volunteer_profiles_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "organization_verifications" (
    "id" TEXT NOT NULL,
    "org_id" TEXT NOT NULL,
    "document_type" TEXT NOT NULL,
    "document_url" TEXT NOT NULL,
    "status" "VerificationStatus" NOT NULL DEFAULT 'Pending',
    "reviewed_by_id" TEXT,
    "admin_notes" TEXT,
    "reviewed_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "organization_verifications_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "donations" (
    "id" TEXT NOT NULL,
    "donor_id" TEXT NOT NULL,
    "accepted_receiver_id" TEXT,
    "status" "DonationStatus" NOT NULL DEFAULT 'Draft',
    "food_type" TEXT NOT NULL,
    "quantity_servings" INTEGER NOT NULL,
    "prep_date_time" TIMESTAMP(3) NOT NULL,
    "storage_condition" "StorageCondition" NOT NULL,
    "consumption_deadline" TIMESTAMP(3) NOT NULL,
    "is_vegetarian" BOOLEAN NOT NULL DEFAULT true,
    "allergens" JSONB NOT NULL DEFAULT '[]',
    "pickup_address" TEXT NOT NULL,
    "pickup_latitude" DOUBLE PRECISION NOT NULL,
    "pickup_longitude" DOUBLE PRECISION NOT NULL,
    "food_photo_url" TEXT NOT NULL,
    "notes" TEXT,
    "handover_otp_hash" TEXT,
    "otp_expires_at" TIMESTAMP(3),
    "failed_otp_attempts" INTEGER NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "donations_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "donation_requests" (
    "id" TEXT NOT NULL,
    "donation_id" TEXT NOT NULL,
    "receiver_id" TEXT NOT NULL,
    "status" "RequestStatus" NOT NULL DEFAULT 'Pending',
    "queue_timestamp" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "notes" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "donation_requests_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "pickups" (
    "id" TEXT NOT NULL,
    "donation_id" TEXT NOT NULL,
    "receiver_id" TEXT NOT NULL,
    "inspection_result" "InspectionResult" NOT NULL,
    "rejection_reason_code" TEXT,
    "verified_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "pickup_latitude" DOUBLE PRECISION NOT NULL,
    "pickup_longitude" DOUBLE PRECISION NOT NULL,
    "photo_evidence_url" TEXT NOT NULL,

    CONSTRAINT "pickups_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "tracking_sessions" (
    "id" TEXT NOT NULL,
    "donation_id" TEXT NOT NULL,
    "receiver_id" TEXT NOT NULL,
    "status" "TrackingStatus" NOT NULL DEFAULT 'Active',
    "started_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "ended_at" TIMESTAMP(3),

    CONSTRAINT "tracking_sessions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "tracking_points" (
    "id" BIGSERIAL NOT NULL,
    "session_id" TEXT NOT NULL,
    "latitude" DOUBLE PRECISION NOT NULL,
    "longitude" DOUBLE PRECISION NOT NULL,
    "heading" DOUBLE PRECISION,
    "speed" DOUBLE PRECISION,
    "recorded_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "tracking_points_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "deliveries" (
    "id" TEXT NOT NULL,
    "donation_id" TEXT NOT NULL,
    "receiver_id" TEXT NOT NULL,
    "confirmed_servings" INTEGER NOT NULL,
    "discrepancy_reason_code" TEXT,
    "completed_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "destination_latitude" DOUBLE PRECISION NOT NULL,
    "destination_longitude" DOUBLE PRECISION NOT NULL,
    "photo_evidence_url" TEXT NOT NULL,

    CONSTRAINT "deliveries_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "reviews" (
    "id" TEXT NOT NULL,
    "donation_id" TEXT NOT NULL,
    "reviewer_id" TEXT NOT NULL,
    "target_id" TEXT NOT NULL,
    "rating" INTEGER NOT NULL,
    "feedback_tags" JSONB NOT NULL DEFAULT '[]',
    "comments" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "reviews_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "complaints" (
    "id" TEXT NOT NULL,
    "donation_id" TEXT NOT NULL,
    "complainant_id" TEXT NOT NULL,
    "target_id" TEXT NOT NULL,
    "category" "ComplaintCategory" NOT NULL,
    "narrative" TEXT NOT NULL,
    "evidence_urls" JSONB NOT NULL DEFAULT '[]',
    "status" "ComplaintStatus" NOT NULL DEFAULT 'Open',
    "admin_finding" TEXT,
    "sanction_applied" TEXT,
    "resolved_by_id" TEXT,
    "resolved_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "complaints_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "certificates" (
    "id" TEXT NOT NULL,
    "volunteer_id" TEXT NOT NULL,
    "milestone_tier" TEXT NOT NULL,
    "completed_deliveries_count" INTEGER NOT NULL,
    "certificate_pdf_url" TEXT NOT NULL,
    "verification_token" TEXT NOT NULL,
    "issued_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "certificates_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "notifications" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "message" TEXT NOT NULL,
    "event_type" TEXT NOT NULL,
    "reference_id" TEXT,
    "is_read" BOOLEAN NOT NULL DEFAULT false,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "notifications_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "audit_logs" (
    "id" BIGSERIAL NOT NULL,
    "actor_user_id" TEXT,
    "actor_role" TEXT NOT NULL,
    "action_type" TEXT NOT NULL,
    "entity_name" TEXT NOT NULL,
    "entity_id" TEXT NOT NULL,
    "delta_state" JSONB,
    "client_ip" TEXT,
    "user_agent" TEXT,
    "timestamp" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "audit_logs_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "users_email_key" ON "users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "users_phone_key" ON "users"("phone");

-- CreateIndex
CREATE UNIQUE INDEX "donor_profiles_user_id_key" ON "donor_profiles"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "receiver_organizations_user_id_key" ON "receiver_organizations"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "volunteer_profiles_user_id_key" ON "volunteer_profiles"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "donation_requests_donation_id_receiver_id_key" ON "donation_requests"("donation_id", "receiver_id");

-- CreateIndex
CREATE UNIQUE INDEX "pickups_donation_id_key" ON "pickups"("donation_id");

-- CreateIndex
CREATE UNIQUE INDEX "tracking_sessions_donation_id_key" ON "tracking_sessions"("donation_id");

-- CreateIndex
CREATE UNIQUE INDEX "deliveries_donation_id_key" ON "deliveries"("donation_id");

-- CreateIndex
CREATE UNIQUE INDEX "reviews_donation_id_reviewer_id_key" ON "reviews"("donation_id", "reviewer_id");

-- CreateIndex
CREATE UNIQUE INDEX "certificates_verification_token_key" ON "certificates"("verification_token");

-- AddForeignKey
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "refresh_tokens_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "donor_profiles" ADD CONSTRAINT "donor_profiles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "receiver_organizations" ADD CONSTRAINT "receiver_organizations_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "volunteer_profiles" ADD CONSTRAINT "volunteer_profiles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "organization_verifications" ADD CONSTRAINT "organization_verifications_org_id_fkey" FOREIGN KEY ("org_id") REFERENCES "receiver_organizations"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "organization_verifications" ADD CONSTRAINT "organization_verifications_reviewed_by_id_fkey" FOREIGN KEY ("reviewed_by_id") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "donations" ADD CONSTRAINT "donations_donor_id_fkey" FOREIGN KEY ("donor_id") REFERENCES "donor_profiles"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "donations" ADD CONSTRAINT "donations_accepted_receiver_id_fkey" FOREIGN KEY ("accepted_receiver_id") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "donation_requests" ADD CONSTRAINT "donation_requests_donation_id_fkey" FOREIGN KEY ("donation_id") REFERENCES "donations"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "donation_requests" ADD CONSTRAINT "donation_requests_receiver_id_fkey" FOREIGN KEY ("receiver_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "pickups" ADD CONSTRAINT "pickups_donation_id_fkey" FOREIGN KEY ("donation_id") REFERENCES "donations"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "pickups" ADD CONSTRAINT "pickups_receiver_id_fkey" FOREIGN KEY ("receiver_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "tracking_sessions" ADD CONSTRAINT "tracking_sessions_donation_id_fkey" FOREIGN KEY ("donation_id") REFERENCES "donations"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "tracking_sessions" ADD CONSTRAINT "tracking_sessions_receiver_id_fkey" FOREIGN KEY ("receiver_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "tracking_points" ADD CONSTRAINT "tracking_points_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "tracking_sessions"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "deliveries" ADD CONSTRAINT "deliveries_donation_id_fkey" FOREIGN KEY ("donation_id") REFERENCES "donations"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "deliveries" ADD CONSTRAINT "deliveries_receiver_id_fkey" FOREIGN KEY ("receiver_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "reviews" ADD CONSTRAINT "reviews_donation_id_fkey" FOREIGN KEY ("donation_id") REFERENCES "donations"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "reviews" ADD CONSTRAINT "reviews_reviewer_id_fkey" FOREIGN KEY ("reviewer_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "reviews" ADD CONSTRAINT "reviews_target_id_fkey" FOREIGN KEY ("target_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "complaints" ADD CONSTRAINT "complaints_donation_id_fkey" FOREIGN KEY ("donation_id") REFERENCES "donations"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "complaints" ADD CONSTRAINT "complaints_complainant_id_fkey" FOREIGN KEY ("complainant_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "complaints" ADD CONSTRAINT "complaints_target_id_fkey" FOREIGN KEY ("target_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "complaints" ADD CONSTRAINT "complaints_resolved_by_id_fkey" FOREIGN KEY ("resolved_by_id") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "certificates" ADD CONSTRAINT "certificates_volunteer_id_fkey" FOREIGN KEY ("volunteer_id") REFERENCES "volunteer_profiles"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "notifications" ADD CONSTRAINT "notifications_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "audit_logs" ADD CONSTRAINT "audit_logs_actor_user_id_fkey" FOREIGN KEY ("actor_user_id") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;

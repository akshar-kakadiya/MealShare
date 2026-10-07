-- =============================================================================
-- MealShare Donation Requests Schema
-- =============================================================================

CREATE TABLE IF NOT EXISTS donation_requests (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_id             UUID NOT NULL REFERENCES donations(id) ON DELETE CASCADE,
    receiver_org_id         UUID NOT NULL REFERENCES receiver_organizations(id) ON DELETE CASCADE,
    requested_quantity      DOUBLE PRECISION NOT NULL CHECK (requested_quantity > 0),
    request_reason          TEXT,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'COMPLETED')),
    requested_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donation_requests_donation_id ON donation_requests(donation_id);
CREATE INDEX idx_donation_requests_receiver_org_id ON donation_requests(receiver_org_id);
CREATE INDEX idx_donation_requests_fcfs ON donation_requests(donation_id, requested_at ASC);

-- Prevent duplicate active requests concurrently for the same donation by the same receiver organization
CREATE UNIQUE INDEX idx_unique_active_donation_request 
ON donation_requests (donation_id, receiver_org_id) 
WHERE status IN ('PENDING', 'APPROVED');

-- ==============================================================================
-- Flyway Migration V6: Add Resolution Notes, Root Cause, and Service Visits
-- ==============================================================================

-- 1. Add resolution_notes and root_cause to incident table
ALTER TABLE incident ADD COLUMN IF NOT EXISTS resolution_notes TEXT;
ALTER TABLE incident ADD COLUMN IF NOT EXISTS root_cause VARCHAR(50);

-- 2. Create service_visits table
CREATE TABLE IF NOT EXISTS service_visits (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    engineer_id BIGINT NOT NULL,
    visit_date DATE NOT NULL DEFAULT CURRENT_DATE,
    check_in_at TIMESTAMP,
    check_out_at TIMESTAMP,
    work_notes TEXT NOT NULL,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_visit_incident FOREIGN KEY (incident_id) REFERENCES incident(id) ON DELETE CASCADE,
    CONSTRAINT fk_service_visit_engineer FOREIGN KEY (engineer_id) REFERENCES engineers(id)
);

CREATE INDEX IF NOT EXISTS idx_service_visits_incident_id ON service_visits(incident_id);
CREATE INDEX IF NOT EXISTS idx_service_visits_engineer_id ON service_visits(engineer_id);

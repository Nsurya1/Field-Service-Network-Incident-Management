-- ==============================================================================
-- Flyway Migration V2: Add resolved_at and closed_at timestamps to incident table
-- ==============================================================================

ALTER TABLE incident
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_incident_resolved_at ON incident(resolved_at);

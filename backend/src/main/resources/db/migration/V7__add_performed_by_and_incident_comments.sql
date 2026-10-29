-- V7: Add performed_by to incident_history and create incident_comments table

-- 1. Add performed_by column to incident_history
ALTER TABLE incident_history ADD COLUMN performed_by VARCHAR(50);

-- Backfill existing rows with 'system'
UPDATE incident_history SET performed_by = 'system' WHERE performed_by IS NULL;

-- Set default for future rows if not explicitly provided
ALTER TABLE incident_history ALTER COLUMN performed_by SET DEFAULT 'system';

-- 2. Create incident_comments table
CREATE TABLE incident_comments (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    author VARCHAR(50) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_incident_comments_incident FOREIGN KEY (incident_id) REFERENCES incident (id) ON DELETE CASCADE
);

CREATE INDEX idx_incident_comments_incident_id ON incident_comments (incident_id);
CREATE INDEX idx_incident_comments_created_at ON incident_comments (created_at);

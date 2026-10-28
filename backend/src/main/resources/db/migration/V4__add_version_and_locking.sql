-- ==============================================================================
-- Flyway Migration V4: Add Optimistic Locking Version to Incident
-- ==============================================================================

-- 1. Add version column for optimistic locking to incident table
ALTER TABLE incident ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

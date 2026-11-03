-- ==============================================================================
-- Flyway Migration V10: Add password reset fields to users table
-- ==============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP WITHOUT TIME ZONE;

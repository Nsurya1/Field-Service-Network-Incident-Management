-- ==============================================================================
-- Flyway Migration V9: Update users_role_check to include ROLE_VIEWER
-- ==============================================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ENGINEER', 'ROLE_VIEWER'));

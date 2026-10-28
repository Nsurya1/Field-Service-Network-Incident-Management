-- ==============================================================================
-- Flyway Migration V5: Add Phone, Active, and Availability Status to Engineers
-- ==============================================================================

-- 1. Add phone column
ALTER TABLE engineers ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

-- 2. Add active boolean column (default true)
ALTER TABLE engineers ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

-- 3. Add availability_status column (default AVAILABLE)
ALTER TABLE engineers ADD COLUMN IF NOT EXISTS availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

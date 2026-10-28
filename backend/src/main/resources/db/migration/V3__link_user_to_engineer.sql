-- ==============================================================================
-- Flyway Migration V3: Link User to Engineer (Ownership-Based Access)
-- ==============================================================================

-- 1. Add engineer_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS engineer_id BIGINT;

-- 2. Add foreign key constraint referencing engineers(id)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_engineer'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_engineer
            FOREIGN KEY (engineer_id) REFERENCES engineers (id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Create unique index ensuring one-to-one relationship
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_engineer_id ON users(engineer_id) WHERE engineer_id IS NOT NULL;

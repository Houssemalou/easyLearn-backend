-- Add created_by to students and professors, and used_by/used_at to access_tokens

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE professors
    ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE access_tokens
    ADD COLUMN IF NOT EXISTS used_by UUID,
    ADD COLUMN IF NOT EXISTS used_at TIMESTAMP;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_by UUID;

-- Add foreign key constraints (nullable)
ALTER TABLE IF EXISTS students
    ADD CONSTRAINT IF NOT EXISTS fk_students_created_by_users FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE IF EXISTS professors
    ADD CONSTRAINT IF NOT EXISTS fk_professors_created_by_users FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE IF EXISTS access_tokens
    ADD CONSTRAINT IF NOT EXISTS fk_access_tokens_used_by_users FOREIGN KEY (used_by) REFERENCES users(id);

ALTER TABLE IF EXISTS users
    ADD CONSTRAINT IF NOT EXISTS fk_users_created_by_users FOREIGN KEY (created_by) REFERENCES users(id);

-- Add unique constraint so one user can be set as usedBy of at most one token (token consumed by one user)
ALTER TABLE IF EXISTS access_tokens
    ADD CONSTRAINT IF NOT EXISTS uq_access_tokens_used_by UNIQUE (used_by);

-- Create index for faster admin lookups
CREATE INDEX IF NOT EXISTS idx_students_created_by ON students(created_by);
CREATE INDEX IF NOT EXISTS idx_professors_created_by ON professors(created_by);
CREATE INDEX IF NOT EXISTS idx_access_tokens_used_by ON access_tokens(used_by);
CREATE INDEX IF NOT EXISTS idx_users_created_by ON users(created_by);

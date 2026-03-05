-- Create premium_tokens table for chatbot premium access
CREATE TABLE IF NOT EXISTS premium_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    is_used BOOLEAN DEFAULT FALSE,
    duration_days INTEGER DEFAULT 30,
    expires_at TIMESTAMP,
    created_by UUID REFERENCES users(id),
    used_by UUID REFERENCES users(id),
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_premium_tokens_token ON premium_tokens(token);
CREATE INDEX IF NOT EXISTS idx_premium_tokens_used_by ON premium_tokens(used_by);

-- Add premium_expires_at column to students table
ALTER TABLE students ADD COLUMN IF NOT EXISTS premium_expires_at TIMESTAMP;

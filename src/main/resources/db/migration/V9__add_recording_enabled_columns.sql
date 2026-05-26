-- Add recording_enabled column to access_tokens, professors, and rooms tables
ALTER TABLE access_tokens ADD COLUMN IF NOT EXISTS recording_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE professors ADD COLUMN IF NOT EXISTS recording_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS recording_enabled BOOLEAN DEFAULT TRUE;

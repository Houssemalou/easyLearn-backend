-- Fix image_url column that is too short for base64 data URLs (varchar(500) -> TEXT)
ALTER TABLE challenges ALTER COLUMN image_url TYPE TEXT;

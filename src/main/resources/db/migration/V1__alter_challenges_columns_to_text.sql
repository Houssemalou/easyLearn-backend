-- Fix challenges table columns that are too short (varchar(500) -> TEXT)
ALTER TABLE challenges ALTER COLUMN options TYPE TEXT;
ALTER TABLE challenges ALTER COLUMN title TYPE TEXT;

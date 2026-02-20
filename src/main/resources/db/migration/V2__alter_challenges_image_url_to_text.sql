-- Fix image_url column that is too short for base64 data URLs (varchar(500) -> TEXT)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'challenges'
  ) THEN
    EXECUTE 'ALTER TABLE public.challenges ALTER COLUMN image_url TYPE TEXT';
  END IF;
END
$$;

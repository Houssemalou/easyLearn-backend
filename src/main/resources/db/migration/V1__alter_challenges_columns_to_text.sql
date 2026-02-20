-- Fix challenges table columns that are too short (varchar(500) -> TEXT)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'challenges'
  ) THEN
    EXECUTE 'ALTER TABLE public.challenges ALTER COLUMN options TYPE TEXT';
    EXECUTE 'ALTER TABLE public.challenges ALTER COLUMN title TYPE TEXT';
  END IF;
END
$$;

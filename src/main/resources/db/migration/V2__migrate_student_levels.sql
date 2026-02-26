-- Flyway migration: map old CEFR levels to primary YEAR1..YEAR9
-- Adjusts existing student level values and increases column length if necessary.

-- For PostgreSQL: make sure column can hold new enum names
ALTER TABLE students ALTER COLUMN level TYPE VARCHAR(10);

-- Map existing CEFR values to YEAR1..YEAR6 (remaining YEAR7-9 reserved)
UPDATE students SET level =
  CASE
    WHEN level = 'A1' THEN 'YEAR1'
    WHEN level = 'A2' THEN 'YEAR2'
    WHEN level = 'B1' THEN 'YEAR3'
    WHEN level = 'B2' THEN 'YEAR4'
    WHEN level = 'C1' THEN 'YEAR5'
    WHEN level = 'C2' THEN 'YEAR6'
    ELSE 'YEAR1'
  END
WHERE level IS NOT NULL;

-- Note: After this migration, restart the application so JPA/Hibernate
-- picks up the updated enum values in the entity.

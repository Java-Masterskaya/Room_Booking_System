ALTER TABLE equipment DROP CONSTRAINT IF EXISTS equipment_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS equipment_name_lower_key
    ON equipment (LOWER(name));

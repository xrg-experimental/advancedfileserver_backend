-- First add columns as nullable
ALTER TABLE shared_folder_validations
    ADD COLUMN can_read BOOLEAN,
    ADD COLUMN can_write BOOLEAN,
    ADD COLUMN can_execute BOOLEAN,
    ADD COLUMN permission_check_error TEXT;

-- Set default values for existing rows
UPDATE shared_folder_validations 
SET can_read = false,
    can_write = false,
    can_execute = false
WHERE can_read IS NULL
   OR can_write IS NULL
   OR can_execute IS NULL;

-- Add NOT NULL constraints
ALTER TABLE shared_folder_validations
    ALTER COLUMN can_read SET NOT NULL,
    ALTER COLUMN can_write SET NOT NULL,
    ALTER COLUMN can_execute SET NOT NULL;

-- Set defaults for future rows
ALTER TABLE shared_folder_validations
    ALTER COLUMN can_read SET DEFAULT false,
    ALTER COLUMN can_write SET DEFAULT false,
    ALTER COLUMN can_execute SET DEFAULT false;

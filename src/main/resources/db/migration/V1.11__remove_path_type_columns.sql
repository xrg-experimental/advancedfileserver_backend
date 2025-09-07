-- Remove is_base_path and is_temp_path columns from shared_folder_configs table
-- Since we only have base paths now, these columns are no longer needed

-- Drop the constraint that referenced these columns
ALTER TABLE shared_folder_configs DROP CONSTRAINT IF EXISTS check_path_type;

-- Drop the columns
ALTER TABLE shared_folder_configs DROP COLUMN IF EXISTS is_base_path;
ALTER TABLE shared_folder_configs DROP COLUMN IF EXISTS is_temp_path;
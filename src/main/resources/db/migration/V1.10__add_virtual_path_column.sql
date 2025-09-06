-- Add virtual_path_id column
ALTER TABLE files ADD COLUMN virtual_path_id BIGINT;

-- Add foreign key constraint
ALTER TABLE files 
    ADD CONSTRAINT fk_files_virtual_path 
    FOREIGN KEY (virtual_path_id) 
    REFERENCES virtual_paths(id);

-- Add index for the foreign key
CREATE INDEX idx_files_virtual_path ON files(virtual_path_id);

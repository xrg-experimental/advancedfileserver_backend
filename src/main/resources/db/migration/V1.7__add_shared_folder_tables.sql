-- Shared folder configuration table
CREATE TABLE shared_folder_configs (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(1024) NOT NULL,
    is_base_path BOOLEAN NOT NULL DEFAULT false,
    is_temp_path BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    modified_by BIGINT REFERENCES users(id),
    CONSTRAINT unique_path UNIQUE (path),
    CONSTRAINT check_path_type CHECK (
        (is_base_path AND NOT is_temp_path) OR 
        (is_temp_path AND NOT is_base_path) OR 
        (NOT is_base_path AND NOT is_temp_path)
    )
);

-- Shared folder validation status table
CREATE TABLE shared_folder_validations (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES shared_folder_configs(id),
    is_valid BOOLEAN NOT NULL,
    last_checked_at TIMESTAMP NOT NULL,
    error_message TEXT,
    checked_by BIGINT REFERENCES users(id),
    CONSTRAINT unique_validation UNIQUE (config_id)
);

-- Indexes
CREATE INDEX idx_shared_folder_configs_paths ON shared_folder_configs(path);
CREATE INDEX idx_shared_folder_validations_status ON shared_folder_validations(is_valid);

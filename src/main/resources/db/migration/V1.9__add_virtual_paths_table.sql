CREATE SEQUENCE virtual_paths_seq INCREMENT BY 50;

CREATE TABLE virtual_paths (
    id BIGINT PRIMARY KEY DEFAULT nextval('virtual_paths_seq'),
    virtual_path VARCHAR(1024) NOT NULL,
    physical_path VARCHAR(1024) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT REFERENCES virtual_paths(id),
    is_directory BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    modified_at TIMESTAMP NOT NULL,
    modified_by BIGINT REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES users(id),
    CONSTRAINT uq_virtual_path UNIQUE (virtual_path)
);

CREATE INDEX idx_virtual_path ON virtual_paths(virtual_path);
CREATE INDEX idx_physical_path ON virtual_paths(physical_path);

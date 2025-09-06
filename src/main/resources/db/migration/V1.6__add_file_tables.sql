-- File metadata table
CREATE SEQUENCE files_seq INCREMENT BY 50;

CREATE TABLE files (
    id BIGINT PRIMARY KEY DEFAULT nextval('files_seq'),
    name VARCHAR(255) NOT NULL,
    path TEXT NOT NULL,
    physical_path TEXT NOT NULL,
    size BIGINT NOT NULL,
    mime_type VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    modified_by BIGINT REFERENCES users(id),
    group_id BIGINT REFERENCES groups(id),
    checksum VARCHAR(64),
    is_directory BOOLEAN NOT NULL DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_files_path ON files(path);
CREATE INDEX idx_files_group ON files(group_id);
CREATE INDEX idx_files_created_by ON files(created_by);
CREATE INDEX idx_files_deleted ON files(is_deleted, deleted_at);

-- File versions table
CREATE SEQUENCE file_versions_seq INCREMENT BY 50;

CREATE TABLE file_versions (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_versions_seq'),
    file_id BIGINT NOT NULL REFERENCES files(id),
    version_number INTEGER NOT NULL,
    size BIGINT NOT NULL,
    physical_path TEXT NOT NULL,
    checksum VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    comment TEXT
);

CREATE INDEX idx_file_versions_file ON file_versions(file_id);

-- File tags table
CREATE SEQUENCE file_tags_seq INCREMENT BY 50;

CREATE TABLE file_tags (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_tags_seq'),
    name VARCHAR(50) NOT NULL,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE file_tag_mappings (
    file_id BIGINT NOT NULL REFERENCES files(id),
    tag_id BIGINT NOT NULL REFERENCES file_tags(id),
    added_at TIMESTAMP NOT NULL,
    added_by BIGINT REFERENCES users(id),
    PRIMARY KEY (file_id, tag_id)
);

CREATE INDEX idx_file_tags_name ON file_tags(name);

-- File shares table
CREATE SEQUENCE file_shares_seq INCREMENT BY 50;

CREATE TABLE file_shares (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_shares_seq'),
    file_id BIGINT NOT NULL REFERENCES files(id),
    shared_by BIGINT NOT NULL REFERENCES users(id),
    shared_with BIGINT REFERENCES users(id),
    shared_with_group BIGINT REFERENCES groups(id),
    access_token VARCHAR(255),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    last_accessed TIMESTAMP,
    access_count INTEGER DEFAULT 0,
    can_write BOOLEAN DEFAULT false,
    can_share BOOLEAN DEFAULT false,
    CONSTRAINT share_target_check CHECK (
        (shared_with IS NOT NULL AND shared_with_group IS NULL) OR
        (shared_with IS NULL AND shared_with_group IS NOT NULL) OR
        (shared_with IS NULL AND shared_with_group IS NULL AND access_token IS NOT NULL)
    )
);

CREATE INDEX idx_file_shares_file ON file_shares(file_id);
CREATE INDEX idx_file_shares_token ON file_shares(access_token);
CREATE INDEX idx_file_shares_expiry ON file_shares(expires_at);

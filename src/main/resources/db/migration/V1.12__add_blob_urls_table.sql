-- Create blob_urls table for temporary download URLs using hard links
CREATE TABLE blob_urls (
    token VARCHAR(64) PRIMARY KEY,
    original_path VARCHAR(1000) NOT NULL,
    hard_link_path VARCHAR(1000) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL
);

-- Index for efficient cleanup of expired URLs
CREATE INDEX idx_blob_urls_expires_at ON blob_urls(expires_at);

-- Index for user-specific queries
CREATE INDEX idx_blob_urls_created_by ON blob_urls(created_by);

-- Index for cleanup queries combining expiration and creation time
CREATE INDEX idx_blob_urls_cleanup ON blob_urls(expires_at, created_at);

-- Performance indexes matching repository queries
CREATE INDEX idx_blob_urls_original_path_active ON blob_urls(original_path, expires_at);
CREATE INDEX idx_blob_urls_created_by_active ON blob_urls(created_by, expires_at);

-- Ensure a hard-link path maps to exactly one token
CREATE UNIQUE INDEX uq_blob_urls_hard_link_path ON blob_urls(hard_link_path);

-- Data integrity checks
ALTER TABLE blob_urls
    ADD CONSTRAINT chk_blob_urls_nonneg_size CHECK (file_size >= 0);

ALTER TABLE blob_urls
    ADD CONSTRAINT chk_blob_urls_expiry_after_create CHECK (expires_at > created_at);
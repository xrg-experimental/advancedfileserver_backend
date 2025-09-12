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
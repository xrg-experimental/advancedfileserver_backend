CREATE TABLE user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    token TEXT,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_sessions_username ON user_sessions(username);
CREATE INDEX idx_sessions_expires ON user_sessions(expires_at) WHERE active = true;

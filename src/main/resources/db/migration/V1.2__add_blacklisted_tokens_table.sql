CREATE SEQUENCE blacklisted_tokens_seq INCREMENT BY 50;

CREATE TABLE blacklisted_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('blacklisted_tokens_seq'),
    token TEXT NOT NULL UNIQUE,
    token_hash VARCHAR(64),
    expires_at TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL,
    username VARCHAR(255),
    token_type VARCHAR(50) DEFAULT 'JWT',
    revocation_reason VARCHAR(255)
);

CREATE UNIQUE INDEX ux_blacklisted_tokens_token ON blacklisted_tokens(token);
CREATE INDEX idx_blacklisted_tokens_expires ON blacklisted_tokens(expires_at);
CREATE INDEX idx_blacklisted_tokens_username ON blacklisted_tokens(username);
CREATE INDEX idx_blacklisted_tokens_token_type ON blacklisted_tokens(token_type);

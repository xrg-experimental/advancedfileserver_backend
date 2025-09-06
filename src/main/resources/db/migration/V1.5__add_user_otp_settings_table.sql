CREATE SEQUENCE user_otp_settings_seq INCREMENT BY 50;

CREATE TABLE user_otp_settings (
    id BIGINT PRIMARY KEY DEFAULT nextval('user_otp_settings_seq'),
    user_id BIGINT NOT NULL UNIQUE,
    otp_enabled BOOLEAN NOT NULL DEFAULT false,
    otp_secret VARCHAR(32),
    required BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_otp_settings_user ON user_otp_settings(user_id);

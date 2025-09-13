-- Demo Users (password is 'password123' bcrypted)
INSERT INTO users (id, username, password, email, enabled, user_type, created_at) VALUES
(1, 'admin', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'admin@example.com', true, 'ADMIN', CURRENT_TIMESTAMP),
(2, 'internal1', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'internal1@example.com', true, 'INTERNAL', CURRENT_TIMESTAMP),
(3, 'internal2', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'internal2@example.com', true, 'INTERNAL', CURRENT_TIMESTAMP),
(4, 'external1', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'external1@example.com', true, 'EXTERNAL', CURRENT_TIMESTAMP),
(5, 'external2', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'external2@example.com', true, 'EXTERNAL', CURRENT_TIMESTAMP),
(6, 'admin1', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'admin1@example.com', true, 'ADMIN', CURRENT_TIMESTAMP),
(7, 'admin2', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'admin2@example.com', true, 'ADMIN', CURRENT_TIMESTAMP),
(8, 'admin3', '$2a$10$Y35SUbLOYVV3qeDRKRJGCewKqglptsOZb0HDoDqCz36tj547qA6HC', 'admin3@example.com', true, 'ADMIN', CURRENT_TIMESTAMP);

-- OTP Settings for admin users (required for all admins)
INSERT INTO user_otp_settings (id, user_id, otp_enabled, otp_secret, required) VALUES
(1, 1, true, 'JBSWY3DPEHPK3PXP', true),  -- admin
(2, 6, true, 'JBSWY3DPEHPK3PXQ', true),  -- admin1
(3, 7, true, 'JBSWY3DPEHPK3PXR', true),  -- admin2
(4, 8, true, 'JBSWY3DPEHPK3PXS', true);  -- admin3

-- User Roles
INSERT INTO user_roles (user_id, roles) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_INTERNAL'),
(3, 'ROLE_INTERNAL'),
(4, 'ROLE_EXTERNAL'),
(5, 'ROLE_EXTERNAL'),
(6, 'ROLE_ADMIN'),
(7, 'ROLE_ADMIN'),
(8, 'ROLE_ADMIN');

-- Demo Groups
INSERT INTO groups (id, name, description, base_path, created_at) VALUES
(1, 'Marketing', 'Marketing team workspace', '/volume1/shared/marketing', CURRENT_TIMESTAMP),
(2, 'Development', 'Development team workspace', '/volume1/shared/development', CURRENT_TIMESTAMP),
(3, 'External-Projects', 'External collaborators workspace', '/volume1/shared/external', CURRENT_TIMESTAMP);

-- User-Group Assignments
INSERT INTO user_groups (user_id, group_id) VALUES
(1, 1), -- admin in Marketing
(1, 2), -- admin in Development
(1, 3), -- admin in External-Projects
(2, 1), -- internal1 in Marketing
(3, 2), -- internal2 in Development
(4, 3), -- external1 in External-Projects
(5, 3); -- external2 in External-Projects

-- Group Permissions
INSERT INTO group_permissions (id, group_id, can_read, can_write, can_delete, can_share, can_upload)
VALUES
    (1, 1, true, true, true, true, true),
    (2, 2, true, true, false, true, true);

-- Demo Blacklisted Tokens
INSERT INTO blacklisted_tokens (id, token, token_hash, expires_at, blacklisted_at, username, token_type, revocation_reason)
VALUES
    (1, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.demo.token', 
       'hash_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.demo.token',
       NOW(),
       NOW(), 
       'user1', 
       'JWT', 
       'Demo revoked token');

-- Reset sequences
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('groups_id_seq', (SELECT MAX(id) FROM groups));
SELECT setval('group_permissions_seq', (SELECT MAX(id) FROM group_permissions));
SELECT setval('user_otp_settings_seq', (SELECT MAX(id) FROM user_otp_settings));
SELECT setval('blacklisted_tokens_seq', (SELECT MAX(id) FROM blacklisted_tokens));

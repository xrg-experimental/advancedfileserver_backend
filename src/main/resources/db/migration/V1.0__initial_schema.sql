CREATE SEQUENCE users_id_seq INCREMENT BY 50;
CREATE SEQUENCE groups_id_seq INCREMENT BY 50;
CREATE SEQUENCE groups_seq INCREMENT BY 50;
CREATE SEQUENCE users_seq INCREMENT BY 50;

CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_id_seq'),
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    user_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_login TIMESTAMP
);

CREATE TABLE groups (
    id BIGINT PRIMARY KEY DEFAULT nextval('groups_id_seq'),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_groups (
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (group_id) REFERENCES groups(id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, roles),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_groups_name ON groups(name);

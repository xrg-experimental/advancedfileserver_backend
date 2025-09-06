CREATE SEQUENCE group_permissions_seq;

CREATE TABLE group_permissions (
    id BIGINT PRIMARY KEY DEFAULT nextval('group_permissions_seq'),
    group_id BIGINT NOT NULL UNIQUE,
    can_read BOOLEAN NOT NULL DEFAULT true,
    can_write BOOLEAN NOT NULL DEFAULT false,
    can_delete BOOLEAN NOT NULL DEFAULT false,
    can_share BOOLEAN NOT NULL DEFAULT false,
    can_upload BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (group_id) REFERENCES groups(id)
);

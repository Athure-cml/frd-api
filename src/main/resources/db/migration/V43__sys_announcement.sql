CREATE TABLE sys_announcement (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(128) NOT NULL,
    content      TEXT         NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    published_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP,
    created_by   BIGINT       NOT NULL REFERENCES sys_user(id),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE sys_announcement_read (
    announcement_id BIGINT    NOT NULL REFERENCES sys_announcement(id) ON DELETE CASCADE,
    user_id         BIGINT    NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    read_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (announcement_id, user_id)
);

CREATE INDEX idx_sys_announcement_active ON sys_announcement (enabled, published_at DESC);

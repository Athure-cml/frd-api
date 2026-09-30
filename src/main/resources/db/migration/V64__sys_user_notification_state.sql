CREATE TABLE IF NOT EXISTS sys_user_notification_state (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    notice_id   VARCHAR(128) NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    dismissed   BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_notification_state_user_notice
    ON sys_user_notification_state (user_id, notice_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_notification_state_user
    ON sys_user_notification_state (user_id);

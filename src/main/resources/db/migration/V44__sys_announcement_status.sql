ALTER TABLE sys_announcement
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED';

ALTER TABLE sys_announcement
    ADD COLUMN valid_days INTEGER;

ALTER TABLE sys_announcement
    ALTER COLUMN published_at DROP NOT NULL;

UPDATE sys_announcement
SET status = 'DISABLED'
WHERE enabled = false;

UPDATE sys_announcement
SET valid_days = GREATEST(1, EXTRACT(DAY FROM (expires_at - published_at))::INTEGER)
WHERE expires_at IS NOT NULL
  AND published_at IS NOT NULL;

CREATE INDEX idx_sys_announcement_status ON sys_announcement (status, published_at DESC NULLS LAST);

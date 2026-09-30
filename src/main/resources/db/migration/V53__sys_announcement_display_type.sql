ALTER TABLE sys_announcement
    ADD COLUMN display_type VARCHAR(16) NOT NULL DEFAULT 'MODAL';

UPDATE sys_announcement
SET display_type = 'MODAL'
WHERE display_type IS NULL;

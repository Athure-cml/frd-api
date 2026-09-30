CREATE TABLE approval_config (
  id BIGSERIAL PRIMARY KEY,
  config_no VARCHAR(32) NOT NULL UNIQUE,
  config_object VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_approval_config_object ON approval_config(config_object);

CREATE TABLE approval_config_step (
  id BIGSERIAL PRIMARY KEY,
  config_id BIGINT NOT NULL REFERENCES approval_config(id) ON DELETE CASCADE,
  sort_order INTEGER NOT NULL,
  approver_id BIGINT NOT NULL REFERENCES sys_user(id),
  approver_name VARCHAR(64) NOT NULL
);

CREATE INDEX idx_approval_config_step_config ON approval_config_step(config_id, sort_order);

CREATE TABLE quote_approval_log (
  id BIGSERIAL PRIMARY KEY,
  quote_order_id BIGINT NOT NULL REFERENCES quote_order(id) ON DELETE CASCADE,
  action VARCHAR(32) NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32),
  comment TEXT,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quote_approval_log_order ON quote_approval_log(quote_order_id);
CREATE INDEX idx_quote_approval_log_created ON quote_approval_log(created_at DESC);

ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS approved_by_name VARCHAR(64);

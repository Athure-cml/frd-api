-- 已报价自动发信：发送流水 + 报价单冗余状态
CREATE TABLE IF NOT EXISTS quote_mail_log (
  id BIGSERIAL PRIMARY KEY,
  quote_id BIGINT NOT NULL,
  quote_no VARCHAR(64),
  inquiry_id BIGINT,
  to_email VARCHAR(255),
  cc VARCHAR(500),
  subject VARCHAR(500),
  attachment_name VARCHAR(255),
  status VARCHAR(20) NOT NULL,
  stage VARCHAR(20),
  skip_reason VARCHAR(40),
  error_code VARCHAR(20),
  error_message VARCHAR(1000),
  trigger_type VARCHAR(20) NOT NULL,
  operator_id BIGINT,
  operator_name VARCHAR(64),
  started_at TIMESTAMP,
  finished_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_quote_mail_log_quote ON quote_mail_log (quote_id);
CREATE INDEX IF NOT EXISTS idx_quote_mail_log_status ON quote_mail_log (status);

ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS last_mail_status VARCHAR(20);
ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS last_mail_at TIMESTAMP;
ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS last_mail_error VARCHAR(500);

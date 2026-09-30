ALTER TABLE quote_approval_log
  ADD COLUMN IF NOT EXISTS node_title VARCHAR(64);

ALTER TABLE quote_approval_log
  DROP CONSTRAINT IF EXISTS quote_approval_log_quote_order_id_fkey;

ALTER TABLE quote_approval_log
  ADD CONSTRAINT quote_approval_log_quote_order_id_fkey
  FOREIGN KEY (quote_order_id) REFERENCES quote_order(id) ON DELETE RESTRICT;

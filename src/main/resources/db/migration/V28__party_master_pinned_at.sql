-- 客商主数据置顶：客户 / 供应商 / 船公司 / 代理商
ALTER TABLE customer
  ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP NULL;

ALTER TABLE supplier
  ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP NULL;

ALTER TABLE shipping_line
  ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP NULL;

ALTER TABLE agent
  ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_customer_pinned_at
  ON customer (pinned_at DESC NULLS LAST, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_supplier_pinned_at
  ON supplier (category, pinned_at DESC NULLS LAST, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_shipping_line_pinned_at
  ON shipping_line (pinned_at DESC NULLS LAST, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_pinned_at
  ON agent (pinned_at DESC NULLS LAST, updated_at DESC);

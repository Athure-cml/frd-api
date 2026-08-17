-- 客商主数据自定义拖拽排序
ALTER TABLE customer
  ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE supplier
  ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE shipping_line
  ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE agent
  ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

UPDATE customer SET sort_order = id::integer WHERE sort_order = 0;
UPDATE supplier SET sort_order = id::integer WHERE sort_order = 0;
UPDATE shipping_line SET sort_order = id::integer WHERE sort_order = 0;
UPDATE agent SET sort_order = id::integer WHERE sort_order = 0;

CREATE INDEX IF NOT EXISTS idx_customer_sort_order
  ON customer ((pinned_at IS NULL), sort_order, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_supplier_sort_order
  ON supplier (category, (pinned_at IS NULL), sort_order, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_shipping_line_sort_order
  ON shipping_line ((pinned_at IS NULL), sort_order, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_sort_order
  ON agent ((pinned_at IS NULL), sort_order, updated_at DESC);

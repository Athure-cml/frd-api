-- 报价变更单：父子版本链路
ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS parent_quote_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS root_quote_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS revision_no INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS change_reason VARCHAR(512) NULL,
  ADD COLUMN IF NOT EXISTS current_version BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS superseded_by_quote_id BIGINT NULL;

COMMENT ON COLUMN quote_order.parent_quote_id IS '变更来源报价单 ID';
COMMENT ON COLUMN quote_order.root_quote_id IS '版本族根报价单 ID（原单）';
COMMENT ON COLUMN quote_order.revision_no IS '修订号：0=原版，1=R1…';
COMMENT ON COLUMN quote_order.change_reason IS '发起变更原因';
COMMENT ON COLUMN quote_order.current_version IS '是否当前对外生效版本';
COMMENT ON COLUMN quote_order.superseded_by_quote_id IS '被哪张变更单替代';

CREATE INDEX IF NOT EXISTS idx_quote_order_root_quote_id ON quote_order (root_quote_id);
CREATE INDEX IF NOT EXISTS idx_quote_order_parent_quote_id ON quote_order (parent_quote_id);
CREATE INDEX IF NOT EXISTS idx_quote_order_root_current
  ON quote_order (root_quote_id, current_version);

-- 历史数据：根单即自身
UPDATE quote_order
SET root_quote_id = id
WHERE root_quote_id IS NULL;

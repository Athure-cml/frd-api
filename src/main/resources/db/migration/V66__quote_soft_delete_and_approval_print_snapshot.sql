-- 草稿软删：有审批记录时可删，报价列表不可见，审批仍可查看
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_quote_order_deleted_at ON quote_order (deleted_at);

-- 提交审批时保存报价单打印模板快照（sheet + 成本快照等）
ALTER TABLE quote_approval_log ADD COLUMN IF NOT EXISTS print_snapshot TEXT;

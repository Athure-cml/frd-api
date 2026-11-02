-- 已归档原因：区分变更替换 / 已报价过期 / 待报价过期，供月年统计拆分
ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS archive_reason VARCHAR(32) NULL;

COMMENT ON COLUMN quote_order.archive_reason IS '归档原因：SUPERSEDED|QUOTED_EXPIRED|PENDING_EXPIRED';

-- 仍残留的变更中 → 已归档（变更）
UPDATE quote_order
SET status = 'ARCHIVED',
    archive_reason = 'SUPERSEDED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status = 'REVISING';

-- 历史已归档：有 superseded_by / 原变更中 视为变更替换；其余暂标已报价过期
UPDATE quote_order
SET archive_reason = CASE
  WHEN superseded_by_quote_id IS NOT NULL THEN 'SUPERSEDED'
  WHEN status IN ('SUPERSEDED', 'REVISING') THEN 'SUPERSEDED'
  ELSE 'QUOTED_EXPIRED'
END
WHERE status IN ('ARCHIVED', 'SUPERSEDED', 'EXPIRED', 'REVISING')
  AND archive_reason IS NULL;

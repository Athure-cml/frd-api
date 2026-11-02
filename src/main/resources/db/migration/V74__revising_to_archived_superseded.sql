-- 纠偏：变更中应归为已归档（变更）
-- 兼容旧版 V71 把 REVISING 误迁成 PENDING_QUOTE 的环境

ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS archive_reason VARCHAR(32) NULL;

-- 仍为变更中的单据
UPDATE quote_order
SET status = 'ARCHIVED',
    archive_reason = 'SUPERSEDED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status = 'REVISING';

-- 旧迁移误把变更中打成待报价，但已有被替代痕迹
UPDATE quote_order
SET status = 'ARCHIVED',
    archive_reason = 'SUPERSEDED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status = 'PENDING_QUOTE'
  AND superseded_by_quote_id IS NOT NULL;

-- 已归档但缺变更原因、且能确认为被替代版本
UPDATE quote_order
SET archive_reason = 'SUPERSEDED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status IN ('ARCHIVED', 'SUPERSEDED')
  AND archive_reason IS NULL
  AND superseded_by_quote_id IS NOT NULL;

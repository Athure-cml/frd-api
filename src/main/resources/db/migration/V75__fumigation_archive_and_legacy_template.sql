-- 熏蒸成本库 / 报价库：归档旧数据；视图模板锁定为 legacy（历史快照仍用旧表头）

ALTER TABLE cost_fumigation
  ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE quote_library_entry
  ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE cost_table_template
  ADD COLUMN IF NOT EXISTS legacy BOOLEAN NOT NULL DEFAULT FALSE;

-- 现有熏蒸成本全部归档（新数据 archived=false）
UPDATE cost_fumigation
SET archived = TRUE
WHERE archived = FALSE;

-- 现有熏蒸报价库条目全部归档
UPDATE quote_library_entry
SET archived = TRUE
WHERE cost_mode = 'fumigation'
  AND archived = FALSE;

-- 现有熏蒸视图模板锁定隐藏，取消默认
UPDATE cost_table_template
SET legacy = TRUE,
    is_default = FALSE,
    updated_at = NOW()
WHERE mode = 'fumigation'
  AND legacy = FALSE;

CREATE INDEX IF NOT EXISTS idx_cost_fumigation_archived ON cost_fumigation (archived);
CREATE INDEX IF NOT EXISTS idx_quote_library_entry_archived
  ON quote_library_entry (cost_mode, archived);
CREATE INDEX IF NOT EXISTS idx_cost_table_template_legacy
  ON cost_table_template (library, mode, legacy);

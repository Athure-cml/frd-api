-- 报价库确认状态：pendingConfirm=待确认，confirmed=已确认
-- 历史数据默认已确认，避免上线后整库待确认
ALTER TABLE quote_library_entry
  ADD COLUMN confirm_status VARCHAR(32) NOT NULL DEFAULT 'confirmed';

CREATE INDEX idx_quote_library_entry_confirm
  ON quote_library_entry (cost_mode, confirm_status);

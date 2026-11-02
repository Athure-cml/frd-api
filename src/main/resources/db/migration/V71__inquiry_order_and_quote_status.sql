-- 询价单 + 报价单可空关联；报价展示四态兼容历史数据

CREATE TABLE IF NOT EXISTS inquiry_order (
  id BIGSERIAL PRIMARY KEY,
  inquiry_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_SUBMIT',
  service_types JSONB NOT NULL DEFAULT '[]'::jsonb,
  customer_id BIGINT NULL,
  customer_name VARCHAR(128) NOT NULL,
  transport_mode VARCHAR(16) NOT NULL DEFAULT 'SEA',
  route_summary VARCHAR(256) NULL,
  quote_date DATE NULL,
  valid_until DATE NULL,
  zip_code VARCHAR(16) NULL,
  city VARCHAR(128) NULL,
  state VARCHAR(32) NULL,
  por VARCHAR(64) NULL,
  pol VARCHAR(64) NULL,
  pod VARCHAR(64) NULL,
  fumigation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  fumigation_point VARCHAR(64) NULL,
  oak_type VARCHAR(16) NULL,
  pick_up_address VARCHAR(512) NULL,
  remark VARCHAR(512) NULL,
  quote_id BIGINT NULL,
  submitted_at TIMESTAMP NULL,
  created_by BIGINT NOT NULL,
  created_by_name VARCHAR(64) NULL,
  dept_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT uk_inquiry_no UNIQUE (inquiry_no)
);

CREATE INDEX IF NOT EXISTS idx_inquiry_status ON inquiry_order (status);
CREATE INDEX IF NOT EXISTS idx_inquiry_customer ON inquiry_order (customer_id);
CREATE INDEX IF NOT EXISTS idx_inquiry_quote ON inquiry_order (quote_id);
CREATE INDEX IF NOT EXISTS idx_inquiry_created_by ON inquiry_order (created_by);
CREATE INDEX IF NOT EXISTS idx_inquiry_updated ON inquiry_order (updated_at);

ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS inquiry_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_quote_order_inquiry ON quote_order (inquiry_id);

-- 线上数据归一为四态：待报价 / 已报价 / 已作废 / 已归档
UPDATE quote_order
SET status = 'PENDING_QUOTE',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status IN ('DRAFT', 'PENDING', 'PENDING_APPROVAL');

UPDATE quote_order
SET status = 'QUOTED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status IN ('SENT', 'EFFECTIVE', 'FOLLOWING', 'WON');

UPDATE quote_order
SET status = 'VOIDED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status IN ('REJECTED', 'LOST');

-- 已被替代 / 已过期历史态 → 已归档
UPDATE quote_order
SET status = 'ARCHIVED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status IN ('SUPERSEDED', 'EXPIRED');

-- 变更中 → 已归档（变更）；archive_reason 列若尚未存在则先补齐
ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS archive_reason VARCHAR(32) NULL;

UPDATE quote_order
SET status = 'ARCHIVED',
    archive_reason = 'SUPERSEDED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND status = 'REVISING';

-- 已超过有效期且仍为待报价/已报价 → 已归档（含变更前旧单据）
UPDATE quote_order
SET status = 'ARCHIVED',
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND valid_until IS NOT NULL
  AND valid_until < CURRENT_DATE
  AND status IN ('PENDING_QUOTE', 'QUOTED', 'DRAFT', 'PENDING', 'PENDING_APPROVAL', 'SENT', 'EFFECTIVE', 'FOLLOWING');

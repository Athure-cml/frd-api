-- 报价单业务表结构（字段名与 Excel 模板英文一致，snake_case 存库）
-- Hibernate ddl-auto=update 会自动同步；生产环境建议用 Flyway 执行本脚本

-- quote_order 扩展列（在既有 quote_order 表上 ALTER）
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS zip_code VARCHAR(16);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS city VARCHAR(128);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS state VARCHAR(32);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS por VARCHAR(64);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS pol VARCHAR(64);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS pod VARCHAR(64);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS o_f_usd VARCHAR(128);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS ssl VARCHAR(128);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS trucking_non_oak_usd DECIMAL(14,2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS trucking_oak_usd DECIMAL(14,2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS fm_non_oak DECIMAL(14,2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS fm_oak DECIMAL(14,2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS doc_usd VARCHAR(64);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS cargo_max_weight_ton VARCHAR(128);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS sheet_remark VARCHAR(1024);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS follow_up_by BIGINT;
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS follow_up_by_name VARCHAR(64);

-- 状态枚举：DRAFT / EFFECTIVE / FOLLOWING / WON / EXPIRED / VOIDED
-- 兼容旧值：PENDING,SENT,LOST 由应用层映射

-- 报价单成本匹配快照（生成报价时冻结成本库版本与明细）
CREATE TABLE IF NOT EXISTS quote_cost_snapshot (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  quote_id        BIGINT       NOT NULL,
  cost_type       VARCHAR(16)  NOT NULL,  -- ROAD | SEA | FUMIGATION
  cost_ref_id     BIGINT       NOT NULL,
  cost_version    VARCHAR(64),            -- valid_date 或 updated_at 编码
  match_keys_json CLOB,
  snapshot_json   CLOB         NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_quote_cost_snapshot_quote FOREIGN KEY (quote_id) REFERENCES quote_order(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_quote_cost_snapshot_quote ON quote_cost_snapshot(quote_id);
CREATE INDEX IF NOT EXISTS idx_quote_cost_snapshot_type ON quote_cost_snapshot(quote_id, cost_type);

-- 业务跟进记录
CREATE TABLE IF NOT EXISTS quote_follow_up (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  quote_id        BIGINT       NOT NULL,
  follow_status   VARCHAR(32)  NOT NULL,  -- 跟进状态（与单据状态可不同）
  content         VARCHAR(2000) NOT NULL,
  follow_up_by    BIGINT       NOT NULL,
  follow_up_by_name VARCHAR(64) NOT NULL,
  follow_up_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_quote_follow_up_quote FOREIGN KEY (quote_id) REFERENCES quote_order(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_quote_follow_up_quote ON quote_follow_up(quote_id);

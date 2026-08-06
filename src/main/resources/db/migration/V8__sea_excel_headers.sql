-- 海运成本库对齐最新业务 Excel 表头：
-- POR | POL | POD | 中文简称 | 英文品名 | 箱型 | 运费 | 有效期
-- | 附加费(BUC/EBS/GRI/OTHERS + 各自有效期) | ALL IN | SSL | AGENT | REMARK

ALTER TABLE cost_sea RENAME COLUMN origin TO pol;
ALTER TABLE cost_sea RENAME COLUMN destination TO pod;
ALTER TABLE cost_sea RENAME COLUMN carrier TO ssl;
ALTER TABLE cost_sea RENAME COLUMN unit_price TO freight;
ALTER TABLE cost_sea RENAME COLUMN valid_date TO freight_valid_date;
ALTER TABLE cost_sea RENAME COLUMN spec TO container_type;

ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS por VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS cn_short_name VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS en_product_name VARCHAR(256);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS buc_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS ebs NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS ebs_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS gri NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS gri_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS others NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS others_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS agent VARCHAR(128);

-- 旧附加费有效期 DATE → BUC 有效期字符串
UPDATE cost_sea
SET buc_valid_date = to_char(surcharge_valid_date, 'YYYY-MM-DD')
WHERE surcharge_valid_date IS NOT NULL
  AND (buc_valid_date IS NULL OR buc_valid_date = '');

ALTER TABLE cost_sea DROP COLUMN IF EXISTS surcharge_valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS unit;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS currency;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS valid_from;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS valid_to;

-- 同步内置默认模板
UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "surcharge",
      "labelKey": "page.costLibrary.seaGroups.surcharge",
      "headerClassName": "sea-header-surcharge",
      "fields": ["buc", "bucValidDate", "ebs", "ebsValidDate", "gri", "griValidDate", "others", "othersValidDate"]
    }
  ],
  "fields": ["por", "pol", "pod", "cnShortName", "enProductName", "containerType", "freight", "freightValidDate", "buc", "bucValidDate", "ebs", "ebsValidDate", "gri", "griValidDate", "others", "othersValidDate", "allIn", "ssl", "agent", "remark"],
  "fieldOrder": ["por", "pol", "pod", "cnShortName", "enProductName", "containerType", "freight", "freightValidDate", "buc", "bucValidDate", "ebs", "ebsValidDate", "gri", "griValidDate", "others", "othersValidDate", "allIn", "ssl", "agent", "remark"],
  "customFields": null,
  "fieldOverrides": null
}'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'sea'
  AND code = 'sea_default';

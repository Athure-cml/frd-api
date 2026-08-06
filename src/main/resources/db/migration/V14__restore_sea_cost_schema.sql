-- V12 误把海运表改成卡车列；此处还原为海运成本库标准结构。
-- 旧卡车列数据无意义，清空后重建。

TRUNCATE TABLE cost_sea RESTART IDENTITY;

ALTER TABLE cost_sea DROP COLUMN IF EXISTS supplier;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS base_freight;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS psc;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS chassis;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS overweight;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS isps;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS stops_ff;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS all_in_non_oak;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS all_in_oak;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS waiting_fee;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS drop_pick;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS reposition;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS us_lift;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS otrw_fee;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS log_yard_name_address;

ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS por VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS pol VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS pod VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS cn_short_name VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS en_product_name VARCHAR(256);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS container_type VARCHAR(64);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS freight NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS freight_valid_date VARCHAR(64);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS buc NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS buc_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS ebs NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS ebs_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS gri NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS gri_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS others NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS others_valid_date VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS all_in NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS ssl VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS agent VARCHAR(128);

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
WHERE mode = 'sea';

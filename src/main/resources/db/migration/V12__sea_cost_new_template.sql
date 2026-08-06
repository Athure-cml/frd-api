-- 海运成本库对齐最新业务 Excel 表头（旧列无法无损映射，清空后重建）：
-- POR* | POD | POL* | SUPPLIER* | BASE FREIGHT* | PSC | CHASSIS | OVERWEIGHT | ISPS | STOPS FF
-- | ALL IN - FF* | ALL IN - FF (NON OAK)* | ALL IN - FF (OAK)*
-- | WAITING FEE | DROP/PICK | REPOSITION | US LIFT | OTR/W FEE | REMARK | VALID DATE | LOG YARD NAME / ADDRESS

TRUNCATE TABLE cost_sea RESTART IDENTITY;

ALTER TABLE cost_sea DROP COLUMN IF EXISTS cn_short_name;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS en_product_name;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS container_type;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS freight;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS freight_valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS buc;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS buc_valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS ebs;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS ebs_valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS gri;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS gri_valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS others;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS others_valid_date;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS ssl;
ALTER TABLE cost_sea DROP COLUMN IF EXISTS agent;

ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS supplier VARCHAR(128);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS base_freight NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS psc NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS chassis NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS overweight NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS isps NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS stops_ff NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS all_in_non_oak NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS all_in_oak NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS waiting_fee NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS drop_pick NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS reposition NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS us_lift NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS otrw_fee NUMERIC(14, 2);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS valid_date VARCHAR(64);
ALTER TABLE cost_sea ADD COLUMN IF NOT EXISTS log_yard_name_address VARCHAR(512);

-- 同步全部海运模板布局（含必填）
UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "route",
      "labelKey": "page.costLibrary.seaGroups.route",
      "headerClassName": "sea-header-route",
      "fields": ["por", "pod", "pol", "supplier"]
    },
    {
      "key": "freight",
      "labelKey": "page.costLibrary.seaGroups.freight",
      "headerClassName": "sea-header-freight",
      "fields": ["baseFreight", "psc", "chassis", "overweight", "isps", "stopsFf", "allIn", "allInNonOak", "allInOak"]
    },
    {
      "key": "extra",
      "labelKey": "page.costLibrary.seaGroups.extra",
      "headerClassName": "sea-header-extra",
      "fields": ["waitingFee", "dropPick", "reposition", "usLift", "otrwFee", "remark", "validDate", "logYardNameAddress"]
    }
  ],
  "fields": ["por", "pod", "pol", "supplier", "baseFreight", "psc", "chassis", "overweight", "isps", "stopsFf", "allIn", "allInNonOak", "allInOak", "waitingFee", "dropPick", "reposition", "usLift", "otrwFee", "remark", "validDate", "logYardNameAddress"],
  "fieldOrder": ["por", "pod", "pol", "supplier", "baseFreight", "psc", "chassis", "overweight", "isps", "stopsFf", "allIn", "allInNonOak", "allInOak", "waitingFee", "dropPick", "reposition", "usLift", "otrwFee", "remark", "validDate", "logYardNameAddress"],
  "customFields": null,
  "fieldOverrides": {
    "por": { "required": true },
    "pol": { "required": true },
    "supplier": { "required": true },
    "baseFreight": { "required": true },
    "allIn": { "required": true },
    "allInNonOak": { "required": true },
    "allInOak": { "required": true }
  }
}'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'sea';

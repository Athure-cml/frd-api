-- 卡车成本库对齐最新业务 Excel 表头（旧列无法无损映射，清空后重建）：
-- POR* | POD | POL* | SUPPLIER* | BASE FREIGHT* | PSC | CHASSIS | OVERWEIGHT | ISPS | STOPS FF
-- | ALL IN - FF* | ALL IN - FF (NON OAK)* | ALL IN - FF (OAK)*
-- | WAITING FEE | DROP/PICK | REPOSITION | US LIFT | OTR/W FEE | REMARK | VALID DATE | LOG YARD NAME / ADDRESS

TRUNCATE TABLE cost_road RESTART IDENTITY;

ALTER TABLE cost_road DROP COLUMN IF EXISTS zip_code;
ALTER TABLE cost_road DROP COLUMN IF EXISTS city;
ALTER TABLE cost_road DROP COLUMN IF EXISTS state;
ALTER TABLE cost_road DROP COLUMN IF EXISTS fsc;
ALTER TABLE cost_road DROP COLUMN IF EXISTS ow_tri_axle;
ALTER TABLE cost_road DROP COLUMN IF EXISTS split;
ALTER TABLE cost_road DROP COLUMN IF EXISTS stop_off;
ALTER TABLE cost_road DROP COLUMN IF EXISTS redelivery;
ALTER TABLE cost_road DROP COLUMN IF EXISTS prepull;
ALTER TABLE cost_road DROP COLUMN IF EXISTS ns_lift;

ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS pod VARCHAR(128);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS psc NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS overweight NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS isps NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS stops_ff NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS drop_pick NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS reposition NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS us_lift NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS otrw_fee NUMERIC(14, 2);

UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "route",
      "labelKey": "page.costLibrary.roadGroups.route",
      "headerClassName": "road-header-route",
      "fields": ["por", "pod", "pol", "supplier"]
    },
    {
      "key": "freight",
      "labelKey": "page.costLibrary.roadGroups.freight",
      "headerClassName": "road-header-freight",
      "fields": ["baseFreight", "psc", "chassis", "overweight", "isps", "stopsFf", "allIn", "allInNonOak", "allInOak"]
    },
    {
      "key": "extra",
      "labelKey": "page.costLibrary.roadGroups.extra",
      "headerClassName": "road-header-extra",
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
WHERE mode = 'road';

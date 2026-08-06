-- 卡车成本库对齐正确业务 Excel 表头（旧列无法无损映射，清空后重建）：
-- ZIP CODE* | CITY* | STATE* | POD* | POL* | SUPPLIER | BASE FREIGHT | FSC | CHASSIS | TRI/TANDEM AXLE | SPLIT | STOP OFF
-- | ALL IN - NO FM* | ALL IN - FM ONE WAY* | ALL IN - FM ROUND*
-- | WAITING FEE | REDELIVERY | PREPULL | TO LIFT | OTHER FEE | REMARK | VALID DATE | LOG YARD NAME & ADDRESS

TRUNCATE TABLE cost_road RESTART IDENTITY;

ALTER TABLE cost_road DROP COLUMN IF EXISTS por;
ALTER TABLE cost_road DROP COLUMN IF EXISTS psc;
ALTER TABLE cost_road DROP COLUMN IF EXISTS overweight;
ALTER TABLE cost_road DROP COLUMN IF EXISTS isps;
ALTER TABLE cost_road DROP COLUMN IF EXISTS stops_ff;
ALTER TABLE cost_road DROP COLUMN IF EXISTS all_in;
ALTER TABLE cost_road DROP COLUMN IF EXISTS all_in_non_oak;
ALTER TABLE cost_road DROP COLUMN IF EXISTS all_in_oak;
ALTER TABLE cost_road DROP COLUMN IF EXISTS drop_pick;
ALTER TABLE cost_road DROP COLUMN IF EXISTS reposition;
ALTER TABLE cost_road DROP COLUMN IF EXISTS us_lift;
ALTER TABLE cost_road DROP COLUMN IF EXISTS otrw_fee;
ALTER TABLE cost_road DROP COLUMN IF EXISTS ow_tri_axle;
ALTER TABLE cost_road DROP COLUMN IF EXISTS ns_lift;

ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS zip_code VARCHAR(32);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS city VARCHAR(64);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS state VARCHAR(32);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS pod VARCHAR(128);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS pol VARCHAR(128);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS fsc NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS tri_tandem_axle NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS split NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS stop_off NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS all_in_no_fm NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS all_in_fm_one_way NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS all_in_fm_round NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS redelivery NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS prepull NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS to_lift NUMERIC(14, 2);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS other_fee NUMERIC(14, 2);

UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "route",
      "labelKey": "page.costLibrary.roadGroups.route",
      "headerClassName": "road-header-route",
      "fields": ["zipCode", "city", "state", "pod", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff"]
    },
    {
      "key": "freight",
      "labelKey": "page.costLibrary.roadGroups.freight",
      "headerClassName": "road-header-freight",
      "fields": ["allInNoFm", "allInFmOneWay", "allInFmRound"]
    },
    {
      "key": "extra",
      "labelKey": "page.costLibrary.roadGroups.extra",
      "headerClassName": "road-header-extra",
      "fields": ["waitingFee", "redelivery", "prepull", "toLift", "otherFee", "remark"]
    },
    {
      "key": "meta",
      "labelKey": "page.costLibrary.roadGroups.meta",
      "headerClassName": "road-header-meta",
      "fields": ["validDate", "logYardNameAddress"]
    }
  ],
  "fields": ["zipCode", "city", "state", "pod", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff", "allInNoFm", "allInFmOneWay", "allInFmRound", "waitingFee", "redelivery", "prepull", "toLift", "otherFee", "remark", "validDate", "logYardNameAddress"],
  "fieldOrder": ["zipCode", "city", "state", "pod", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff", "allInNoFm", "allInFmOneWay", "allInFmRound", "waitingFee", "redelivery", "prepull", "toLift", "otherFee", "remark", "validDate", "logYardNameAddress"],
  "customFields": null,
  "fieldOverrides": {
    "zipCode": { "required": true },
    "city": { "required": true },
    "state": { "required": true },
    "pod": { "required": true },
    "pol": { "required": true },
    "allInNoFm": { "required": true },
    "allInFmOneWay": { "required": true },
    "allInFmRound": { "required": true }
  }
}'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'road';

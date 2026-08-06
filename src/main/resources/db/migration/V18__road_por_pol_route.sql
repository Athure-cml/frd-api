-- 卡车成本库路由字段更正为 POR（美国城市接货地）+ POL（港口）
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS por VARCHAR(128);

UPDATE cost_road
SET por = COALESCE(NULLIF(TRIM(por), ''), city)
WHERE por IS NULL OR TRIM(por) = '';

ALTER TABLE cost_road DROP COLUMN IF EXISTS zip_code;
ALTER TABLE cost_road DROP COLUMN IF EXISTS city;
ALTER TABLE cost_road DROP COLUMN IF EXISTS state;
ALTER TABLE cost_road DROP COLUMN IF EXISTS pod;

UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "route",
      "labelKey": "page.costLibrary.roadGroups.route",
      "headerClassName": "road-header-route",
      "fields": ["por", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff"]
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
      "fields": ["waitingFee", "redelivery", "prepull", "nsLift", "otherFee", "remark"]
    },
    {
      "key": "meta",
      "labelKey": "page.costLibrary.roadGroups.meta",
      "headerClassName": "road-header-meta",
      "fields": ["validDate", "logYardNameAddress"]
    }
  ],
  "fields": ["por", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff", "allInNoFm", "allInFmOneWay", "allInFmRound", "waitingFee", "redelivery", "prepull", "nsLift", "otherFee", "remark", "validDate", "logYardNameAddress"],
  "fieldOrder": ["por", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff", "allInNoFm", "allInFmOneWay", "allInFmRound", "waitingFee", "redelivery", "prepull", "nsLift", "otherFee", "remark", "validDate", "logYardNameAddress"],
  "customFields": null,
  "fieldOverrides": {
    "por": { "required": true },
    "pol": { "required": true },
    "supplier": { "required": true },
    "allInNoFm": { "required": true },
    "allInFmOneWay": { "required": true },
    "allInFmRound": { "required": true }
  }
}'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'road';

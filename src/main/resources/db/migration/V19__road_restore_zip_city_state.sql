-- 恢复卡车成本库 ZIP / CITY / STATE（与 POR/POL 并存；不恢复 POD）
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS zip_code VARCHAR(32);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS city VARCHAR(128);
ALTER TABLE cost_road ADD COLUMN IF NOT EXISTS state VARCHAR(32);

UPDATE cost_road
SET city = COALESCE(NULLIF(TRIM(city), ''), por)
WHERE city IS NULL OR TRIM(city) = '';

UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "route",
      "labelKey": "page.costLibrary.roadGroups.route",
      "headerClassName": "road-header-route",
      "fields": ["zipCode", "city", "state", "por", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff"]
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
  "fields": ["zipCode", "city", "state", "por", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff", "allInNoFm", "allInFmOneWay", "allInFmRound", "waitingFee", "redelivery", "prepull", "nsLift", "otherFee", "remark", "validDate", "logYardNameAddress"],
  "fieldOrder": ["zipCode", "city", "state", "por", "pol", "supplier", "baseFreight", "fsc", "chassis", "triTandemAxle", "split", "stopOff", "allInNoFm", "allInFmOneWay", "allInFmRound", "waitingFee", "redelivery", "prepull", "nsLift", "otherFee", "remark", "validDate", "logYardNameAddress"],
  "customFields": null,
  "fieldOverrides": {
    "zipCode": { "required": true },
    "city": { "required": true },
    "state": { "required": true },
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

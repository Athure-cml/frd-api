-- 熏蒸成本库对齐最新业务 Excel 表头：
-- REGION | STATION | FM-OUTDOOR(NON OAK/OAK/VALIDITY) | FM-INDOOR(NON OAK/OAK/VALIDITY) | ADDRESS

ALTER TABLE cost_fumigation RENAME COLUMN port TO region;
ALTER TABLE cost_fumigation RENAME COLUMN non_oak_outdoor TO outdoor_non_oak;
ALTER TABLE cost_fumigation RENAME COLUMN oak_outdoor TO outdoor_oak;
ALTER TABLE cost_fumigation RENAME COLUMN non_oak_indoor TO indoor_non_oak;
ALTER TABLE cost_fumigation RENAME COLUMN oak_indoor TO indoor_oak;
ALTER TABLE cost_fumigation RENAME COLUMN remark TO address;

ALTER TABLE cost_fumigation ADD COLUMN outdoor_validity VARCHAR(128);
ALTER TABLE cost_fumigation ADD COLUMN indoor_validity VARCHAR(128);

ALTER TABLE cost_fumigation DROP COLUMN IF EXISTS non_oak_quote_summer;
ALTER TABLE cost_fumigation DROP COLUMN IF EXISTS non_oak_quote_winter;
ALTER TABLE cost_fumigation DROP COLUMN IF EXISTS oak_quote_summer;
ALTER TABLE cost_fumigation DROP COLUMN IF EXISTS oak_quote_winter;

-- 同步内置默认模板布局（应用启动时 migrator 也会再次对齐）
UPDATE cost_table_template
SET layout = '{
  "groups": [
    {
      "key": "outdoor",
      "labelKey": "page.costLibrary.fumigationGroups.outdoor",
      "headerClassName": "fumigation-header-primary",
      "fields": ["outdoorNonOak", "outdoorOak", "outdoorValidity"]
    },
    {
      "key": "indoor",
      "labelKey": "page.costLibrary.fumigationGroups.indoor",
      "headerClassName": "fumigation-header-primary",
      "fields": ["indoorNonOak", "indoorOak", "indoorValidity"]
    }
  ],
  "fields": ["region", "station", "outdoorNonOak", "outdoorOak", "outdoorValidity", "indoorNonOak", "indoorOak", "indoorValidity", "address"],
  "fieldOrder": ["region", "station", "outdoorNonOak", "outdoorOak", "outdoorValidity", "indoorNonOak", "indoorOak", "indoorValidity", "address"],
  "customFields": null,
  "fieldOverrides": null
}'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'fumigation'
  AND code = 'fumigation_default';

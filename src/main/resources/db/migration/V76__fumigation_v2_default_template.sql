-- 熏蒸新版单级表头默认模板（旧版已在 V75 标为 legacy 隐藏）

INSERT INTO cost_table_template (
  library, mode, code, name, is_default, legacy, layout, created_at, updated_at
)
SELECT
  'cost',
  'fumigation',
  'fumigation_v2',
  'page.costLibrary.template.defaultName',
  TRUE,
  FALSE,
  '{
    "groups": [],
    "fields": ["region", "station", "outdoorNonOak", "outdoorOak", "cf_fum_eff", "outdoorValidity", "address", "remark"],
    "fieldOrder": ["region", "station", "outdoorNonOak", "outdoorOak", "cf_fum_eff", "outdoorValidity", "address", "remark"],
    "customFields": [
      { "field": "cf_fum_eff", "title": "生效期", "required": true, "dataType": "date" }
    ],
    "fieldOverrides": {
      "region": { "title": "REGION", "required": true },
      "station": { "title": "STATION", "required": true },
      "outdoorNonOak": { "title": "FM NON OAK", "required": true },
      "outdoorOak": { "title": "FM OAK", "required": true },
      "cf_fum_eff": { "title": "生效期", "required": true },
      "outdoorValidity": { "title": "有效期", "required": true },
      "address": { "title": "ADDRESS" },
      "remark": { "title": "备注" }
    }
  }'::jsonb,
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM cost_table_template
  WHERE library = 'cost' AND mode = 'fumigation' AND legacy = FALSE
);

INSERT INTO cost_table_template (
  library, mode, code, name, is_default, legacy, layout, created_at, updated_at
)
SELECT
  'quote',
  'fumigation',
  'q_fumigation_v2',
  'page.quote.library.template.defaultName',
  TRUE,
  FALSE,
  '{
    "groups": [],
    "fields": ["region", "station", "outdoorNonOak", "outdoorOak", "cf_fum_eff", "outdoorValidity", "address", "remark"],
    "fieldOrder": ["region", "station", "outdoorNonOak", "outdoorOak", "cf_fum_eff", "outdoorValidity", "address", "remark"],
    "customFields": [
      { "field": "cf_fum_eff", "title": "生效期", "required": true, "dataType": "date" }
    ],
    "fieldOverrides": {
      "region": { "title": "REGION", "required": true },
      "station": { "title": "STATION", "required": true },
      "outdoorNonOak": { "title": "FM NON OAK", "required": true },
      "outdoorOak": { "title": "FM OAK", "required": true },
      "cf_fum_eff": { "title": "生效期", "required": true },
      "outdoorValidity": { "title": "有效期", "required": true },
      "address": { "title": "ADDRESS" },
      "remark": { "title": "备注" }
    }
  }'::jsonb,
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM cost_table_template
  WHERE library = 'quote' AND mode = 'fumigation' AND legacy = FALSE
);

-- 熏蒸成本库模板：全部字段必填
UPDATE cost_table_template
SET layout = jsonb_set(
    COALESCE(layout, '{}'::jsonb),
    '{fieldOverrides}',
    '{
      "region": { "required": true },
      "station": { "required": true },
      "outdoorNonOak": { "required": true },
      "outdoorOak": { "required": true },
      "outdoorValidity": { "required": true },
      "indoorNonOak": { "required": true },
      "indoorOak": { "required": true },
      "indoorValidity": { "required": true },
      "address": { "required": true }
    }'::jsonb,
    true
),
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'fumigation';

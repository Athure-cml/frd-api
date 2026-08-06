-- 海运成本库模板：POR / POL / POD / 英文品名 / 箱型 / 运费 / 运费有效期 必填
UPDATE cost_table_template
SET layout = jsonb_set(
    COALESCE(layout, '{}'::jsonb),
    '{fieldOverrides}',
    COALESCE(layout->'fieldOverrides', '{}'::jsonb) || '{
      "por": { "required": true },
      "pol": { "required": true },
      "pod": { "required": true },
      "enProductName": { "required": true },
      "containerType": { "required": true },
      "freight": { "required": true },
      "freightValidDate": { "required": true }
    }'::jsonb,
    true
),
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'sea';

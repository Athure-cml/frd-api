-- 修复 V21：fieldOverrides 为 JSON null 时与对象做 || 会变成数组，导致启动反序列化失败
UPDATE cost_table_template
SET layout = jsonb_set(
    COALESCE(layout, '{}'::jsonb),
    '{fieldOverrides}',
    '{
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

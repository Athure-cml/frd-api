-- 卡车成本库：SUPPLIER 也是必填
UPDATE cost_table_template
SET layout = jsonb_set(
      COALESCE(layout, '{}'::jsonb),
      '{fieldOverrides}',
      COALESCE(layout->'fieldOverrides', '{}'::jsonb) || '{
        "zipCode": {"required": true},
        "city": {"required": true},
        "state": {"required": true},
        "pod": {"required": true},
        "pol": {"required": true},
        "supplier": {"required": true},
        "allInNoFm": {"required": true},
        "allInFmOneWay": {"required": true},
        "allInFmRound": {"required": true}
      }'::jsonb
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'road';

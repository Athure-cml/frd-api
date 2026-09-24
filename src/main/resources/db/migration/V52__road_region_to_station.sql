-- 卡车成本库：REGION 列重命名为 STATION（关联熏蒸供应商）
ALTER TABLE cost_road RENAME COLUMN region TO station;

-- 已有 road 模板 layout 中字段键 region → station
UPDATE cost_table_template
SET layout = replace(layout::text, '"region"', '"station"')::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'road'
  AND layout::text LIKE '%"region"%';

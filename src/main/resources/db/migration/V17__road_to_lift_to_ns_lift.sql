-- 卡车成本库：TO LIFT 更正为 NS LIFT
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'cost_road'
      AND column_name = 'to_lift'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'cost_road'
      AND column_name = 'ns_lift'
  ) THEN
    ALTER TABLE cost_road RENAME COLUMN to_lift TO ns_lift;
  ELSIF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'cost_road'
      AND column_name = 'to_lift'
  ) AND EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'cost_road'
      AND column_name = 'ns_lift'
  ) THEN
    UPDATE cost_road SET ns_lift = COALESCE(ns_lift, to_lift);
    ALTER TABLE cost_road DROP COLUMN to_lift;
  ELSIF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'cost_road'
      AND column_name = 'ns_lift'
  ) THEN
    ALTER TABLE cost_road ADD COLUMN ns_lift NUMERIC(14, 2);
  END IF;
END $$;

UPDATE cost_table_template
SET layout = replace(replace(layout::text, '"toLift"', '"nsLift"'), 'TO LIFT', 'NS LIFT')::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE mode = 'road';

-- 供应商公式里若仍写 TO LIFT，兼容别名由求值器识别；此处顺带改成标准名
UPDATE supplier
SET non_fumigation_package_formula = replace(non_fumigation_package_formula, 'TO LIFT', 'NS LIFT'),
    fumigation_non_oak_package_formula = replace(fumigation_non_oak_package_formula, 'TO LIFT', 'NS LIFT'),
    fumigation_oak_package_formula = replace(fumigation_oak_package_formula, 'TO LIFT', 'NS LIFT'),
    updated_at = CURRENT_TIMESTAMP
WHERE COALESCE(non_fumigation_package_formula, '') LIKE '%TO LIFT%'
   OR COALESCE(fumigation_non_oak_package_formula, '') LIKE '%TO LIFT%'
   OR COALESCE(fumigation_oak_package_formula, '') LIKE '%TO LIFT%';

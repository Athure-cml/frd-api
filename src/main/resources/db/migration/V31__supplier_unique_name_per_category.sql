-- 供应商名称唯一改为「同分类内」唯一（卡车 / 熏蒸 / 堆场 / 其他互不影响）
DROP INDEX IF EXISTS uk_supplier_name_ci;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM supplier
    GROUP BY category, LOWER(TRIM(name))
    HAVING COUNT(*) > 1
  ) THEN
    CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_category_name_ci
      ON supplier (category, LOWER(TRIM(name)));
  END IF;
END $$;

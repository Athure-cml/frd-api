-- 客商名称唯一（忽略大小写与首尾空格）；已有重复时跳过建索引，由应用层拦截新重复
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM customer GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1
  ) THEN
    CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_name_ci
      ON customer (LOWER(TRIM(name)));
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM supplier GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1
  ) THEN
    CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_name_ci
      ON supplier (LOWER(TRIM(name)));
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM shipping_line GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1
  ) THEN
    CREATE UNIQUE INDEX IF NOT EXISTS uk_shipping_line_name_ci
      ON shipping_line (LOWER(TRIM(name)));
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM agent GROUP BY LOWER(TRIM(name)) HAVING COUNT(*) > 1
  ) THEN
    CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_name_ci
      ON agent (LOWER(TRIM(name)));
  END IF;
END $$;

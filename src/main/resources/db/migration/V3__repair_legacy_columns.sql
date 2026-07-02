-- 修复 Hibernate ddl-auto 时代遗留的列名/缺列（表已存在时 V2 不会重建）

-- currency
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'currency' AND column_name = 'base'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'currency' AND column_name = 'is_base'
  ) THEN
    ALTER TABLE currency RENAME COLUMN base TO is_base;
  END IF;
END $$;

ALTER TABLE currency ADD COLUMN IF NOT EXISTS symbol VARCHAR(8);
ALTER TABLE currency ADD COLUMN IF NOT EXISTS decimal_places INTEGER;
ALTER TABLE currency ADD COLUMN IF NOT EXISTS is_base BOOLEAN;
ALTER TABLE currency ADD COLUMN IF NOT EXISTS sort INTEGER;
ALTER TABLE currency ADD COLUMN IF NOT EXISTS status INTEGER;
ALTER TABLE currency ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE currency ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE currency SET decimal_places = 2 WHERE decimal_places IS NULL;
UPDATE currency SET is_base = FALSE WHERE is_base IS NULL;
UPDATE currency SET sort = 0 WHERE sort IS NULL;
UPDATE currency SET status = 1 WHERE status IS NULL;
UPDATE currency SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE currency SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE currency ALTER COLUMN decimal_places SET DEFAULT 2;
ALTER TABLE currency ALTER COLUMN is_base SET DEFAULT FALSE;
ALTER TABLE currency ALTER COLUMN sort SET DEFAULT 0;
ALTER TABLE currency ALTER COLUMN status SET DEFAULT 1;
ALTER TABLE currency ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE currency ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- md_us_state
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'md_us_state' AND column_name = 'name'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'md_us_state' AND column_name = 'name_zh'
  ) THEN
    ALTER TABLE md_us_state RENAME COLUMN name TO name_zh;
  END IF;
END $$;

ALTER TABLE md_us_state ADD COLUMN IF NOT EXISTS name_zh VARCHAR(64);

-- md_global_port
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS name_en VARCHAR(128);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS name_zh VARCHAR(128);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS route VARCHAR(64);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS country_region VARCHAR(128);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS port_type VARCHAR(16);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS function_code VARCHAR(16);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS locode_status VARCHAR(8);
ALTER TABLE md_global_port ADD COLUMN IF NOT EXISTS data_version VARCHAR(32);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'md_global_port' AND column_name = 'name'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'md_global_port' AND column_name = 'name_en'
  ) THEN
    ALTER TABLE md_global_port RENAME COLUMN name TO name_en;
  END IF;
END $$;

-- md_dest_zip
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'md_dest_zip' AND column_name = 'zip'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'md_dest_zip' AND column_name = 'zip_code'
  ) THEN
    ALTER TABLE md_dest_zip RENAME COLUMN zip TO zip_code;
  END IF;
END $$;

ALTER TABLE md_dest_zip ADD COLUMN IF NOT EXISTS zip_code VARCHAR(32);

-- md_dest_city
ALTER TABLE md_dest_city ADD COLUMN IF NOT EXISTS state_id BIGINT;
ALTER TABLE md_dest_city ADD COLUMN IF NOT EXISTS name VARCHAR(128);

-- md_inland_por
ALTER TABLE md_inland_por ADD COLUMN IF NOT EXISTS pol_id BIGINT;
ALTER TABLE md_inland_por ADD COLUMN IF NOT EXISTS region VARCHAR(64);

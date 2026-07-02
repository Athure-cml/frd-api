-- Hibernate 旧库可能把字符串列建成 bytea，导致 LOWER/TRIM 报错
-- ERROR: function lower(bytea) does not exist

CREATE OR REPLACE FUNCTION quote_fix_bytea_column(
    p_table text, p_column text, p_type text)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = p_table
      AND column_name = p_column
      AND udt_name = 'bytea'
  ) THEN
    EXECUTE format(
      'ALTER TABLE %I ALTER COLUMN %I TYPE %s USING convert_from(%I, ''UTF8'')',
      p_table, p_column, p_type, p_column);
  END IF;
END $$;

-- currency
SELECT quote_fix_bytea_column('currency', 'code', 'VARCHAR(8)');
SELECT quote_fix_bytea_column('currency', 'name', 'VARCHAR(64)');
SELECT quote_fix_bytea_column('currency', 'symbol', 'VARCHAR(8)');

-- md_us_state
SELECT quote_fix_bytea_column('md_us_state', 'code', 'VARCHAR(8)');
SELECT quote_fix_bytea_column('md_us_state', 'name_zh', 'VARCHAR(64)');

-- md_dest_city
SELECT quote_fix_bytea_column('md_dest_city', 'name', 'VARCHAR(128)');

-- md_dest_zip
SELECT quote_fix_bytea_column('md_dest_zip', 'zip_code', 'VARCHAR(32)');

-- md_global_port
SELECT quote_fix_bytea_column('md_global_port', 'code', 'VARCHAR(8)');
SELECT quote_fix_bytea_column('md_global_port', 'name_en', 'VARCHAR(128)');
SELECT quote_fix_bytea_column('md_global_port', 'name_zh', 'VARCHAR(128)');
SELECT quote_fix_bytea_column('md_global_port', 'route', 'VARCHAR(64)');
SELECT quote_fix_bytea_column('md_global_port', 'country_region', 'VARCHAR(128)');
SELECT quote_fix_bytea_column('md_global_port', 'port_type', 'VARCHAR(16)');
SELECT quote_fix_bytea_column('md_global_port', 'function_code', 'VARCHAR(16)');
SELECT quote_fix_bytea_column('md_global_port', 'locode_status', 'VARCHAR(8)');
SELECT quote_fix_bytea_column('md_global_port', 'data_version', 'VARCHAR(32)');

-- md_inland_por
SELECT quote_fix_bytea_column('md_inland_por', 'name', 'VARCHAR(128)');
SELECT quote_fix_bytea_column('md_inland_por', 'region', 'VARCHAR(64)');

-- md_data_sync_meta
SELECT quote_fix_bytea_column('md_data_sync_meta', 'sync_key', 'VARCHAR(64)');
SELECT quote_fix_bytea_column('md_data_sync_meta', 'data_version', 'VARCHAR(32)');
SELECT quote_fix_bytea_column('md_data_sync_meta', 'remark', 'VARCHAR(512)');

-- exchange_rate
SELECT quote_fix_bytea_column('exchange_rate', 'from_currency', 'VARCHAR(8)');
SELECT quote_fix_bytea_column('exchange_rate', 'to_currency', 'VARCHAR(8)');

DROP FUNCTION quote_fix_bytea_column(text, text, text);

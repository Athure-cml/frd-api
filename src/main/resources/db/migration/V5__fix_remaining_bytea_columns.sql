-- V5：补齐 V4 未覆盖的 Hibernate bytea 列（H2/ddl-auto 时代遗留库）
-- 全新 Flyway 建库可安全跳过（列类型已正确）

CREATE OR REPLACE FUNCTION fix_bytea_column(
    p_table  text,
    p_column text,
    p_type   text
)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name  = p_table
      AND column_name = p_column
      AND udt_name   = 'bytea'
  ) THEN
    EXECUTE format(
      'ALTER TABLE %I ALTER COLUMN %I TYPE %s USING convert_from(%I, ''UTF8'')',
      p_table, p_column, p_type, p_column
    );
  END IF;
END $$;

-- ========== sys ==========
SELECT fix_bytea_column('sys_department', 'code', 'VARCHAR(32)');
SELECT fix_bytea_column('sys_department', 'name', 'VARCHAR(64)');
SELECT fix_bytea_column('sys_permission', 'code', 'VARCHAR(128)');
SELECT fix_bytea_column('sys_permission', 'name', 'VARCHAR(128)');
SELECT fix_bytea_column('sys_permission', 'type', 'VARCHAR(16)');
SELECT fix_bytea_column('sys_role', 'code', 'VARCHAR(64)');
SELECT fix_bytea_column('sys_role', 'name', 'VARCHAR(64)');
SELECT fix_bytea_column('sys_role', 'data_scope', 'VARCHAR(16)');
SELECT fix_bytea_column('sys_role', 'remark', 'VARCHAR(255)');
SELECT fix_bytea_column('sys_operation_log', 'request_body', 'TEXT');

-- ========== cost ==========
SELECT fix_bytea_column('cost_road', 'valid_date', 'VARCHAR(32)');
SELECT fix_bytea_column('cost_road', 'supplier', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_road', 'log_yard_name_address', 'VARCHAR(512)');
SELECT fix_bytea_column('cost_road', 'city', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_road', 'state', 'VARCHAR(32)');
SELECT fix_bytea_column('cost_road', 'por', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_road', 'pol', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_road', 'remark', 'VARCHAR(512)');

SELECT fix_bytea_column('cost_sea', 'origin', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_sea', 'destination', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_sea', 'carrier', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_sea', 'spec', 'VARCHAR(32)');
SELECT fix_bytea_column('cost_sea', 'unit', 'VARCHAR(16)');
SELECT fix_bytea_column('cost_sea', 'surcharge_valid_date', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_sea', 'valid_date', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_sea', 'currency', 'VARCHAR(8)');
SELECT fix_bytea_column('cost_sea', 'status', 'VARCHAR(16)');
SELECT fix_bytea_column('cost_sea', 'remark', 'VARCHAR(512)');

SELECT fix_bytea_column('cost_fumigation', 'port', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_fumigation', 'station', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_fumigation', 'non_oak_quote_summer', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_fumigation', 'non_oak_quote_winter', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_fumigation', 'oak_quote_summer', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_fumigation', 'oak_quote_winter', 'VARCHAR(128)');
SELECT fix_bytea_column('cost_fumigation', 'remark', 'VARCHAR(512)');

SELECT fix_bytea_column('cost_table_template', 'mode', 'VARCHAR(16)');
SELECT fix_bytea_column('cost_table_template', 'code', 'VARCHAR(64)');
SELECT fix_bytea_column('cost_table_template', 'name', 'VARCHAR(128)');

-- ========== quote ==========
SELECT fix_bytea_column('quote_order', 'route_summary', 'VARCHAR(256)');
SELECT fix_bytea_column('quote_order', 'transport_mode', 'VARCHAR(16)');
SELECT fix_bytea_column('quote_order', 'status', 'VARCHAR(16)');
SELECT fix_bytea_column('quote_order', 'currency', 'VARCHAR(8)');
SELECT fix_bytea_column('quote_order', 'base_currency', 'VARCHAR(8)');
SELECT fix_bytea_column('quote_order', 'o_f_usd', 'VARCHAR(128)');
SELECT fix_bytea_column('quote_order', 'doc_usd', 'VARCHAR(64)');
SELECT fix_bytea_column('quote_order', 'cargo_max_weight_ton', 'VARCHAR(128)');
SELECT fix_bytea_column('quote_order', 'created_by_name', 'VARCHAR(64)');

SELECT fix_bytea_column('quote_order_line', 'item_name', 'VARCHAR(128)');
SELECT fix_bytea_column('quote_order_line', 'spec', 'VARCHAR(256)');
SELECT fix_bytea_column('quote_order_line', 'cost_mode', 'VARCHAR(16)');
SELECT fix_bytea_column('quote_order_line', 'unit', 'VARCHAR(16)');

SELECT fix_bytea_column('quote_follow_up', 'follow_status', 'VARCHAR(32)');
SELECT fix_bytea_column('quote_follow_up', 'content', 'VARCHAR(2000)');
SELECT fix_bytea_column('quote_follow_up', 'follow_up_by_name', 'VARCHAR(64)');

SELECT fix_bytea_column('quote_cost_snapshot', 'cost_type', 'VARCHAR(16)');
SELECT fix_bytea_column('quote_cost_snapshot', 'cost_version', 'VARCHAR(64)');

DROP FUNCTION IF EXISTS fix_bytea_column(text, text, text);

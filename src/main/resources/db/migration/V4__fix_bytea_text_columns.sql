-- ============================================================
-- V4__fix_bytea_text_columns.sql
-- 修复 Hibernate 旧库将 VARCHAR 列建成 bytea 的问题
-- ERROR: function lower(bytea) does not exist
--
-- 安全说明：
--   1. 仅当列实际为 bytea 时才执行转换（通过 udt_name 判断）
--   2. 使用 convert_from(col, 'UTF8') 将 bytea → text
--   3. 幂等：重复执行不会报错
--   4. 函数执行完毕后自动清理
-- ============================================================

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

-- ========== currency ==========
SELECT fix_bytea_column('currency',        'code',           'VARCHAR(8)');
SELECT fix_bytea_column('currency',        'name',           'VARCHAR(64)');
SELECT fix_bytea_column('currency',        'symbol',         'VARCHAR(8)');

-- ========== md_us_state ==========
SELECT fix_bytea_column('md_us_state',    'code',           'VARCHAR(8)');
SELECT fix_bytea_column('md_us_state',    'name_zh',        'VARCHAR(64)');

-- ========== md_dest_city ==========
SELECT fix_bytea_column('md_dest_city',   'name',           'VARCHAR(128)');

-- ========== md_dest_zip ==========
SELECT fix_bytea_column('md_dest_zip',   'zip_code',       'VARCHAR(32)');

-- ========== md_global_port ==========
SELECT fix_bytea_column('md_global_port', 'code',           'VARCHAR(8)');
SELECT fix_bytea_column('md_global_port', 'name_en',        'VARCHAR(128)');
SELECT fix_bytea_column('md_global_port', 'name_zh',        'VARCHAR(128)');
SELECT fix_bytea_column('md_global_port', 'route',          'VARCHAR(64)');
SELECT fix_bytea_column('md_global_port', 'country_region', 'VARCHAR(128)');
SELECT fix_bytea_column('md_global_port', 'port_type',      'VARCHAR(16)');
SELECT fix_bytea_column('md_global_port', 'function_code',   'VARCHAR(16)');
SELECT fix_bytea_column('md_global_port', 'locode_status',  'VARCHAR(8)');
SELECT fix_bytea_column('md_global_port', 'data_version',   'VARCHAR(32)');

-- ========== md_inland_por ==========
SELECT fix_bytea_column('md_inland_por', 'name',           'VARCHAR(128)');
SELECT fix_bytea_column('md_inland_por', 'region',         'VARCHAR(64)');

-- ========== md_data_sync_meta ==========
SELECT fix_bytea_column('md_data_sync_meta', 'sync_key',    'VARCHAR(64)');
SELECT fix_bytea_column('md_data_sync_meta', 'data_version','VARCHAR(32)');
SELECT fix_bytea_column('md_data_sync_meta', 'remark',      'VARCHAR(512)');

-- ========== exchange_rate ==========
SELECT fix_bytea_column('exchange_rate',   'from_currency', 'VARCHAR(8)');
SELECT fix_bytea_column('exchange_rate',   'to_currency',   'VARCHAR(8)');

-- ========== customer ==========
SELECT fix_bytea_column('customer',        'code',           'VARCHAR(32)');
SELECT fix_bytea_column('customer',        'name',           'VARCHAR(128)');
SELECT fix_bytea_column('customer',        'contact_name',   'VARCHAR(64)');
SELECT fix_bytea_column('customer',        'phone',          'VARCHAR(32)');
SELECT fix_bytea_column('customer',        'email',          'VARCHAR(128)');
SELECT fix_bytea_column('customer',        'address',        'VARCHAR(256)');
SELECT fix_bytea_column('customer',        'remark',         'VARCHAR(512)');

-- ========== quote_order ==========
SELECT fix_bytea_column('quote_order',     'quote_no',       'VARCHAR(32)');
SELECT fix_bytea_column('quote_order',     'customer_name',  'VARCHAR(128)');
SELECT fix_bytea_column('quote_order',     'zip_code',       'VARCHAR(16)');
SELECT fix_bytea_column('quote_order',     'city',           'VARCHAR(128)');
SELECT fix_bytea_column('quote_order',     'state',          'VARCHAR(32)');
SELECT fix_bytea_column('quote_order',     'por',            'VARCHAR(64)');
SELECT fix_bytea_column('quote_order',     'pol',            'VARCHAR(64)');
SELECT fix_bytea_column('quote_order',     'pod',            'VARCHAR(64)');
SELECT fix_bytea_column('quote_order',     'ssl',            'VARCHAR(128)');
SELECT fix_bytea_column('quote_order',     'follow_up_by_name', 'VARCHAR(64)');
SELECT fix_bytea_column('quote_order',     'sheet_remark',   'VARCHAR(1024)');
SELECT fix_bytea_column('quote_order',     'remark',         'VARCHAR(512)');

-- ========== sys_user ==========
SELECT fix_bytea_column('sys_user',        'username',       'VARCHAR(64)');
SELECT fix_bytea_column('sys_user',        'real_name',      'VARCHAR(64)');
SELECT fix_bytea_column('sys_user',        'avatar',         'VARCHAR(512)');
SELECT fix_bytea_column('sys_user',        'phone',          'VARCHAR(20)');
SELECT fix_bytea_column('sys_user',        'email',          'VARCHAR(128)');
SELECT fix_bytea_column('sys_user',        'home_path',      'VARCHAR(128)');

-- ========== sys_operation_log ==========
SELECT fix_bytea_column('sys_operation_log','username',       'VARCHAR(64)');
SELECT fix_bytea_column('sys_operation_log','real_name',      'VARCHAR(64)');
SELECT fix_bytea_column('sys_operation_log','module',         'VARCHAR(64)');
SELECT fix_bytea_column('sys_operation_log','action',         'VARCHAR(16)');
SELECT fix_bytea_column('sys_operation_log','resource_type',  'VARCHAR(64)');
SELECT fix_bytea_column('sys_operation_log','resource_id',    'VARCHAR(64)');
SELECT fix_bytea_column('sys_operation_log','summary',        'VARCHAR(256)');
SELECT fix_bytea_column('sys_operation_log','request_method', 'VARCHAR(8)');
SELECT fix_bytea_column('sys_operation_log','request_uri',    'VARCHAR(256)');
SELECT fix_bytea_column('sys_operation_log','ip_address',     'VARCHAR(64)');
SELECT fix_bytea_column('sys_operation_log','error_message',  'VARCHAR(512)');

-- 清理临时函数
DROP FUNCTION IF EXISTS fix_bytea_column(text, text, text);

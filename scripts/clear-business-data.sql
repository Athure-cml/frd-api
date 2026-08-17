-- =============================================================================
-- 上线前业务数据清理：只保留「系统管理」+「主数据」
-- 用法（本地 / 线上 PostgreSQL 均可）：
--   psql -h <host> -U <user> -d <db> -f scripts/clear-business-data.sql
-- =============================================================================
-- 保留：
--   sys_department / sys_permission / sys_role / sys_role_permission /
--   sys_user / sys_user_role
--   currency / exchange_rate
--   md_us_state / md_dest_city / md_dest_zip / md_global_port / md_inland_por /
--   md_container_type / md_data_sync_meta
--   supplier_type（其他供应商类型字典）
--   flyway_schema_history
-- 清空：
--   报价、成本库、成本表模板、客户/供应商/船司/代理、操作日志
-- 说明：
--   cost_table_template 清空后，重启 quote-api 会自动回填 road/sea/fumigation 默认模板
-- =============================================================================

BEGIN;

TRUNCATE TABLE
  quote_cost_snapshot,
  quote_follow_up,
  quote_order_line,
  quote_order,
  cost_road,
  cost_sea,
  cost_fumigation,
  cost_table_template,
  customer,
  supplier,
  shipping_line,
  agent,
  sys_operation_log
RESTART IDENTITY;

COMMIT;

-- 校验：业务表应为 0；主数据 / 系统表应 > 0
SELECT 'customer' AS tbl, COUNT(*) AS cnt FROM customer
UNION ALL SELECT 'supplier', COUNT(*) FROM supplier
UNION ALL SELECT 'shipping_line', COUNT(*) FROM shipping_line
UNION ALL SELECT 'agent', COUNT(*) FROM agent
UNION ALL SELECT 'quote_order', COUNT(*) FROM quote_order
UNION ALL SELECT 'cost_road', COUNT(*) FROM cost_road
UNION ALL SELECT 'cost_sea', COUNT(*) FROM cost_sea
UNION ALL SELECT 'cost_fumigation', COUNT(*) FROM cost_fumigation
UNION ALL SELECT 'sys_operation_log', COUNT(*) FROM sys_operation_log
UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user
UNION ALL SELECT 'currency', COUNT(*) FROM currency
UNION ALL SELECT 'md_global_port', COUNT(*) FROM md_global_port
UNION ALL SELECT 'md_container_type', COUNT(*) FROM md_container_type
UNION ALL SELECT 'supplier_type', COUNT(*) FROM supplier_type
ORDER BY tbl;

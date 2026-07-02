-- Render PostgreSQL 全量重建：清空 public schema 后重启 frd-api，Flyway 会重新执行 V1 建表
-- 无 SQL 控制台时：在 frd-api 设置 QUOTE_DB_RESET=true 部署一次（见 DEPLOY.md）

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO PUBLIC;

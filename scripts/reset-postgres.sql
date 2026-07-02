-- Render PostgreSQL 全量重建：清空 public schema 后重启 frd-api，Flyway 会重新执行 V1 建表
-- 在 Render Dashboard → PostgreSQL → Connect → 用 psql 或 SQL 控制台执行

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO PUBLIC;

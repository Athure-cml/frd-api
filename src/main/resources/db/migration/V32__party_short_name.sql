-- 客商主数据：客户 / 供应商 / 船公司 / 代理 增加简称
ALTER TABLE customer ADD COLUMN IF NOT EXISTS short_name VARCHAR(64);
ALTER TABLE supplier ADD COLUMN IF NOT EXISTS short_name VARCHAR(64);
ALTER TABLE shipping_line ADD COLUMN IF NOT EXISTS short_name VARCHAR(64);
ALTER TABLE agent ADD COLUMN IF NOT EXISTS short_name VARCHAR(64);

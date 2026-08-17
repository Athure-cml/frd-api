-- 供应商分类：卡车 / 熏蒸 / 仓库堆场 / 其他
ALTER TABLE supplier
  ADD COLUMN IF NOT EXISTS category VARCHAR(32) NOT NULL DEFAULT 'TRUCK';

ALTER TABLE supplier
  ADD COLUMN IF NOT EXISTS contact_name VARCHAR(64);

ALTER TABLE supplier
  ADD COLUMN IF NOT EXISTS phone VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_supplier_category ON supplier (category);

-- 历史数据归为卡车供应商，并清空旧硬编码类型
UPDATE supplier SET category = 'TRUCK', types = '[]'::jsonb;

-- 其他供应商可维护类型字典
CREATE TABLE IF NOT EXISTS supplier_type (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  status INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_type_name_ci
  ON supplier_type (LOWER(TRIM(name)));

INSERT INTO supplier_type (name, sort_order, status)
SELECT v.name, v.sort_order, 1
FROM (
  VALUES
    ('订舱代理', 1),
    ('车队', 2),
    ('报关行', 3),
    ('专线', 4),
    ('租箱公司', 5),
    ('其他', 6)
) AS v(name, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM supplier_type st WHERE LOWER(TRIM(st.name)) = LOWER(TRIM(v.name))
);

-- 船公司 / 代理商补充联系人、电话
ALTER TABLE shipping_line
  ADD COLUMN IF NOT EXISTS contact_name VARCHAR(64);

ALTER TABLE shipping_line
  ADD COLUMN IF NOT EXISTS phone VARCHAR(64);

ALTER TABLE agent
  ADD COLUMN IF NOT EXISTS contact_name VARCHAR(64);

ALTER TABLE agent
  ADD COLUMN IF NOT EXISTS phone VARCHAR(64);

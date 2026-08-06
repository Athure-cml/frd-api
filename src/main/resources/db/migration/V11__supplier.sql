-- 供应商主数据
CREATE TABLE IF NOT EXISTS supplier (
    id                                  BIGSERIAL PRIMARY KEY,
    code                                VARCHAR(32)  NOT NULL UNIQUE,
    name                                VARCHAR(128) NOT NULL,
    email                               VARCHAR(128),
    remark                              VARCHAR(512),
    non_fumigation_package_formula      TEXT,
    fumigation_non_oak_package_formula  TEXT,
    fumigation_oak_package_formula      TEXT,
    status                              INTEGER      NOT NULL DEFAULT 1,
    created_by                          BIGINT,
    created_by_name                     VARCHAR(64),
    dept_id                             BIGINT,
    created_at                          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_supplier_name ON supplier (name);
CREATE INDEX IF NOT EXISTS idx_supplier_status ON supplier (status);

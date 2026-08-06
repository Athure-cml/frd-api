-- 船公司 / 代理商主数据
CREATE TABLE IF NOT EXISTS shipping_line (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    name             VARCHAR(128) NOT NULL,
    email            VARCHAR(128),
    remark           VARCHAR(512),
    status           INTEGER      NOT NULL DEFAULT 1,
    created_by       BIGINT,
    created_by_name  VARCHAR(64),
    dept_id          BIGINT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipping_line_name ON shipping_line (name);
CREATE INDEX IF NOT EXISTS idx_shipping_line_status ON shipping_line (status);

CREATE TABLE IF NOT EXISTS agent (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    name             VARCHAR(128) NOT NULL,
    email            VARCHAR(128),
    remark           VARCHAR(512),
    status           INTEGER      NOT NULL DEFAULT 1,
    created_by       BIGINT,
    created_by_name  VARCHAR(64),
    dept_id          BIGINT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_name ON agent (name);
CREATE INDEX IF NOT EXISTS idx_agent_status ON agent (status);

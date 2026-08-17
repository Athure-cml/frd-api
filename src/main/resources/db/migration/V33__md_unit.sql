-- 费用单位主数据（如 day / hours，展示时与金额拼成 20/hours）
CREATE TABLE IF NOT EXISTS md_unit (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    remark      VARCHAR(256),
    sort        INTEGER      NOT NULL DEFAULT 0,
    status      INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_md_unit_code_ci
  ON md_unit (LOWER(TRIM(code)));

CREATE INDEX IF NOT EXISTS idx_md_unit_status_sort
  ON md_unit (status, sort, code);

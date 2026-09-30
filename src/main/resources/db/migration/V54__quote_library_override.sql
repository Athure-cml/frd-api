-- 报价库：费用字段人工覆盖（在成本+规则之上）
CREATE TABLE quote_library_override (
    id               BIGSERIAL PRIMARY KEY,
    cost_mode        VARCHAR(16)  NOT NULL,
    cost_id          BIGINT       NOT NULL,
    field_overrides  JSONB        NOT NULL DEFAULT '{}',
    updated_by       BIGINT       REFERENCES sys_user(id),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_quote_library_override UNIQUE (cost_mode, cost_id)
);

CREATE INDEX idx_quote_library_override_mode_cost ON quote_library_override (cost_mode, cost_id);

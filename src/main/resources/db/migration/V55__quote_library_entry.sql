-- 报价库成员：仅已「入报价库」的成本行才在报价库展示
CREATE TABLE quote_library_entry (
    id           BIGSERIAL PRIMARY KEY,
    cost_mode    VARCHAR(16)  NOT NULL,
    cost_id      BIGINT       NOT NULL,
    promoted_by  BIGINT       REFERENCES sys_user(id),
    promoted_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_quote_library_entry UNIQUE (cost_mode, cost_id)
);

CREATE INDEX idx_quote_library_entry_mode_cost ON quote_library_entry (cost_mode, cost_id);

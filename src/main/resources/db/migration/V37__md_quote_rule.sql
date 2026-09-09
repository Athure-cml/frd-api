CREATE TABLE IF NOT EXISTS md_quote_rule (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(128)   NOT NULL,
    target_field      VARCHAR(32)    NOT NULL,
    condition_type    VARCHAR(32)    NOT NULL DEFAULT 'ALWAYS',
    condition_amount  NUMERIC(18, 4),
    calc_type         VARCHAR(32)    NOT NULL,
    add_amount        NUMERIC(18, 4),
    fixed_amount      NUMERIC(18, 4),
    cif_factor        NUMERIC(18, 6),
    cif_rate          NUMERIC(18, 6),
    sort_order        INT            NOT NULL DEFAULT 0,
    status            INT            NOT NULL DEFAULT 1,
    remark            TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_md_quote_rule_target_sort
    ON md_quote_rule (target_field, sort_order);

-- 默认规则（与业务表一致；可在主数据-报价单规则中增删改）
INSERT INTO md_quote_rule (name, target_field, condition_type, condition_amount, calc_type, add_amount, fixed_amount, cif_factor, cif_rate, sort_order, status)
VALUES
    ('海运费-成本加价', 'OCEAN_FREIGHT', 'ALWAYS', NULL, 'COST_PLUS', 50, NULL, NULL, NULL, 10, 1),
    ('卡车费-无熏蒸', 'TRUCKING_FEE', 'FUMIGATION_DISABLED', NULL, 'COST_PLUS', 100, NULL, NULL, NULL, 20, 1),
    ('卡车费-有熏蒸', 'TRUCKING_FEE', 'FUMIGATION_ENABLED', NULL, 'COST_PLUS', 150, NULL, NULL, NULL, 21, 1),
    ('熏蒸 NON-OAK-费用>800', 'FM_NON_OAK', 'BASE_GT', 800, 'COST_PLUS', 100, NULL, NULL, NULL, 30, 1),
    ('熏蒸 NON-OAK-费用≤800', 'FM_NON_OAK', 'BASE_LTE', 800, 'COST_PLUS', 50, NULL, NULL, NULL, 31, 1),
    ('熏蒸 OAK-费用>800', 'FM_OAK', 'BASE_GT', 800, 'COST_PLUS', 100, NULL, NULL, NULL, 32, 1),
    ('熏蒸 OAK-费用≤800', 'FM_OAK', 'BASE_LTE', 800, 'COST_PLUS', 50, NULL, NULL, NULL, 33, 1),
    ('单证费-中国港口', 'DOC_FEE', 'POD_CHINA', NULL, 'FIXED', NULL, 300, NULL, NULL, 40, 1),
    ('单证费-非中国港口', 'DOC_FEE', 'POD_NOT_CHINA', NULL, 'FIXED', NULL, 350, NULL, NULL, 41, 1),
    ('保险费-CIF公式', 'CARGO_INSURANCE', 'ALWAYS', NULL, 'CIF_MULTIPLY', NULL, NULL, 1.1, 0.001, 50, 1),
    ('代理费-CIF比例', 'CARGO_AGENT', 'ALWAYS', NULL, 'CIF_PERCENT', NULL, NULL, NULL, 0.005, 60, 1);

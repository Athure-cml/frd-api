-- 保险费 / 代理费：多条可选规则；remark 存打印/下拉用的公式文案

UPDATE md_quote_rule
SET name        = '保险费-0.1%',
    remark      = 'CIF *1.1*0.1% MIN 15',
    cif_factor  = 1.1,
    cif_rate    = 0.001,
    sort_order  = 50,
    status      = 1
WHERE target_field = 'CARGO_INSURANCE'
  AND name = '保险费-CIF公式';

INSERT INTO md_quote_rule (
    name, target_field, condition_type, condition_amount, calc_type,
    add_amount, fixed_amount, cif_factor, cif_rate, sort_order, status, remark
)
SELECT '保险费-0.1%', 'CARGO_INSURANCE', 'ALWAYS', NULL, 'CIF_MULTIPLY',
       NULL, NULL, 1.1, 0.001, 50, 1, 'CIF *1.1*0.1% MIN 15'
WHERE NOT EXISTS (
    SELECT 1 FROM md_quote_rule
    WHERE target_field = 'CARGO_INSURANCE'
      AND remark = 'CIF *1.1*0.1% MIN 15'
);

INSERT INTO md_quote_rule (
    name, target_field, condition_type, condition_amount, calc_type,
    add_amount, fixed_amount, cif_factor, cif_rate, sort_order, status, remark
)
SELECT '保险费-0.15%', 'CARGO_INSURANCE', 'ALWAYS', NULL, 'CIF_MULTIPLY',
       NULL, NULL, 1.1, 0.0015, 51, 1, 'CIF*1.1*0.15% MIN 15'
WHERE NOT EXISTS (
    SELECT 1 FROM md_quote_rule
    WHERE target_field = 'CARGO_INSURANCE'
      AND remark = 'CIF*1.1*0.15% MIN 15'
);

INSERT INTO md_quote_rule (
    name, target_field, condition_type, condition_amount, calc_type,
    add_amount, fixed_amount, cif_factor, cif_rate, sort_order, status, remark
)
SELECT '保险费-0.35%', 'CARGO_INSURANCE', 'ALWAYS', NULL, 'CIF_MULTIPLY',
       NULL, NULL, 1.1, 0.0035, 52, 1, 'CIF *1.1*0.35% MIN 70'
WHERE NOT EXISTS (
    SELECT 1 FROM md_quote_rule
    WHERE target_field = 'CARGO_INSURANCE'
      AND remark = 'CIF *1.1*0.35% MIN 70'
);

UPDATE md_quote_rule
SET remark     = 'CIF*0.5% MIN 100',
    cif_rate   = 0.005,
    sort_order = 60,
    status     = 1
WHERE target_field = 'CARGO_AGENT'
  AND name = '代理费-CIF比例';

INSERT INTO md_quote_rule (
    name, target_field, condition_type, condition_amount, calc_type,
    add_amount, fixed_amount, cif_factor, cif_rate, sort_order, status, remark
)
SELECT '代理费-0.5%', 'CARGO_AGENT', 'ALWAYS', NULL, 'CIF_PERCENT',
       NULL, NULL, NULL, 0.005, 60, 1, 'CIF*0.5% MIN 100'
WHERE NOT EXISTS (
    SELECT 1 FROM md_quote_rule
    WHERE target_field = 'CARGO_AGENT'
      AND remark = 'CIF*0.5% MIN 100'
);

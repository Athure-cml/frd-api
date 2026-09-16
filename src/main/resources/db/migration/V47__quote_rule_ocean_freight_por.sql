-- 海运费：指定起运港（POR）额外加价 +100（与「海运费-成本加价 +50」叠加）

INSERT INTO md_quote_rule (
    name, target_field, condition_type, calc_type, add_amount, sort_order, status, remark
)
SELECT
    '海运费-东海岸起运港加价',
    'OCEAN_FREIGHT',
    'POR_IN',
    'COST_PLUS',
    100,
    11,
    1,
    'NEW YORK,NY,NORFOLK,VA,SAVANNAH,GA,CHARLESTON,SC'
WHERE NOT EXISTS (
    SELECT 1 FROM md_quote_rule
    WHERE target_field = 'OCEAN_FREIGHT'
      AND condition_type = 'POR_IN'
      AND remark = 'NEW YORK,NY,NORFOLK,VA,SAVANNAH,GA,CHARLESTON,SC'
);

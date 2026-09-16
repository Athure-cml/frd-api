-- 东海岸 POR 海运费独立 +100（不与 ALWAYS +50 叠加）

UPDATE md_quote_rule
SET add_amount = 100
WHERE target_field = 'OCEAN_FREIGHT'
  AND condition_type = 'POR_IN'
  AND remark = 'NEW YORK,NY,NORFOLK,VA,SAVANNAH,GA,CHARLESTON,SC'
  AND add_amount = 50;

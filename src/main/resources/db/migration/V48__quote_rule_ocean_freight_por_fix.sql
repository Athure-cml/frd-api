-- 东海岸 POR 海运费合计 +100（基础 ALWAYS +50，POR_IN 再 +50），非叠加 +150

UPDATE md_quote_rule
SET add_amount = 50
WHERE target_field = 'OCEAN_FREIGHT'
  AND condition_type = 'POR_IN'
  AND remark = 'NEW YORK,NY,NORFOLK,VA,SAVANNAH,GA,CHARLESTON,SC'
  AND add_amount = 100;

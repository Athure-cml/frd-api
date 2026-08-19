-- 卡车成本库：ZIP 占位符 CITY、PA有误 → CITY、STATE有误
UPDATE cost_road
SET zip_code = 'CITY、STATE有误'
WHERE TRIM(zip_code) = 'CITY、PA有误';

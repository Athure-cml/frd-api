ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS pick_up_address VARCHAR(512);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS trucking_fee NUMERIC(14, 2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS ns_lift NUMERIC(14, 2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS chassis NUMERIC(14, 2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS waiting NUMERIC(14, 2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS redelivery_fee NUMERIC(14, 2);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS truck_remark VARCHAR(512);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS cargo_insurance_premium VARCHAR(128);
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS cargo_agent_fee VARCHAR(128);

UPDATE quote_order
SET pick_up_address = TRIM(BOTH FROM CONCAT_WS(', ', NULLIF(TRIM(zip_code), ''), NULLIF(TRIM(city), ''), NULLIF(TRIM(state), '')))
WHERE (pick_up_address IS NULL OR TRIM(pick_up_address) = '')
  AND (zip_code IS NOT NULL OR city IS NOT NULL OR state IS NOT NULL);

UPDATE quote_order
SET trucking_fee = trucking_non_oak_usd
WHERE trucking_fee IS NULL AND trucking_non_oak_usd IS NOT NULL;

UPDATE quote_order
SET cargo_insurance_premium = cargo_max_weight_ton
WHERE cargo_insurance_premium IS NULL AND cargo_max_weight_ton IS NOT NULL;

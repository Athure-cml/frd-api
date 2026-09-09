ALTER TABLE shipping_line
    ADD COLUMN IF NOT EXISTS contract_no VARCHAR(128);

ALTER TABLE shipping_line
    ADD COLUMN IF NOT EXISTS valid_until DATE;

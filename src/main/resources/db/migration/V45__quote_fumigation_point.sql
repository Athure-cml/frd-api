ALTER TABLE quote_order
    ADD COLUMN IF NOT EXISTS fumigation_point VARCHAR(64);

COMMENT ON COLUMN quote_order.fumigation_point IS '熏蒸点（港口名称，与 POR 同源主数据）';

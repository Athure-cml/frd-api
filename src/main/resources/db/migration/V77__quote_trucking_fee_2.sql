-- 报价单支持第二条卡车费（引入最多 2 条卡车报价库）
ALTER TABLE quote_order
  ADD COLUMN IF NOT EXISTS trucking_fee_2 NUMERIC(14, 2);

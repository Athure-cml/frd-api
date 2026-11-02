-- 询价递交生成的报价单：操作人暂空，首次保存时再写入
ALTER TABLE quote_order
  ALTER COLUMN created_by DROP NOT NULL;

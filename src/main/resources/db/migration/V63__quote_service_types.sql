-- 服务类型改为多选：VARCHAR -> JSONB 数组
ALTER TABLE quote_order ADD COLUMN IF NOT EXISTS service_types JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE quote_order
SET service_types = jsonb_build_array(service_type)
WHERE service_type IS NOT NULL
  AND btrim(service_type) <> ''
  AND (service_types IS NULL OR service_types = '[]'::jsonb);

ALTER TABLE quote_order DROP COLUMN IF EXISTS service_type;

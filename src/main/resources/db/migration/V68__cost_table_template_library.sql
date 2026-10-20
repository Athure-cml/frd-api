-- 成本库 / 报价库模板分库。历史模板一律归入成本库。
ALTER TABLE cost_table_template
    ADD COLUMN IF NOT EXISTS library VARCHAR(16);

UPDATE cost_table_template
SET library = 'cost'
WHERE library IS NULL OR BTRIM(library) = '';

ALTER TABLE cost_table_template
    ALTER COLUMN library SET DEFAULT 'cost';

ALTER TABLE cost_table_template
    ALTER COLUMN library SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cost_table_template_library_mode
    ON cost_table_template (library, mode);

-- 卡车 / 熏蒸成本库补充状态字段（海运已有）
ALTER TABLE cost_road
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'active';

ALTER TABLE cost_fumigation
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'active';

CREATE INDEX IF NOT EXISTS idx_cost_road_status ON cost_road (status);
CREATE INDEX IF NOT EXISTS idx_cost_fumigation_status ON cost_fumigation (status);
CREATE INDEX IF NOT EXISTS idx_cost_sea_status ON cost_sea (status);

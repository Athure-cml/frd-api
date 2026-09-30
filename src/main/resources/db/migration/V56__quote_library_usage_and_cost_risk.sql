CREATE TABLE quote_library_usage (
  id         BIGSERIAL PRIMARY KEY,
  quote_id   BIGINT       NOT NULL REFERENCES quote_order (id) ON DELETE CASCADE,
  cost_mode  VARCHAR(16)  NOT NULL,
  cost_id    BIGINT       NOT NULL,
  locked_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_quote_library_usage_quote_mode UNIQUE (quote_id, cost_mode)
);

CREATE INDEX idx_quote_library_usage_cost ON quote_library_usage (cost_mode, cost_id);

ALTER TABLE quote_order
  ADD COLUMN cost_risk_active BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN cost_risk_reason VARCHAR(512),
  ADD COLUMN cost_risk_at TIMESTAMP,
  ADD COLUMN cost_risk_dismissed_at TIMESTAMP,
  ADD COLUMN cost_risk_dismissed_by BIGINT;

CREATE INDEX idx_quote_order_cost_risk ON quote_order (cost_risk_active, updated_at);

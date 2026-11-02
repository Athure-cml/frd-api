-- 旧版曾「一次性清空全部成本异常」。现改为：
-- 1) 真实费用变更打的异常必须保留；
-- 2) 仅有效期等非价格变更造成的误标，由应用启动时
--    QuoteCostRiskService.reconcileFalsePositiveRisks() 对照快照费用签名清除。
-- 本脚本不再改写 quote_order.cost_risk_*。
SELECT 1;

-- 报价单状态流转：草稿 → 待审批 → 已发送 → 已成交 / 已放弃(拒绝/过期/作废)

UPDATE quote_order SET status = 'SENT' WHERE status IN ('EFFECTIVE', 'FOLLOWING');
UPDATE quote_order SET status = 'PENDING_APPROVAL' WHERE status = 'PENDING';
UPDATE quote_order SET status = 'REJECTED' WHERE status = 'LOST';

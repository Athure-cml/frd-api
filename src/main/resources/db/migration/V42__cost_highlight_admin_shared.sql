-- 超级管理员/系统管理员标记：对业务部门可见
ALTER TABLE cost_dept_highlight
    ADD COLUMN admin_shared BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_cost_dept_highlight_admin_shared
    ON cost_dept_highlight (cost_mode, admin_shared)
    WHERE admin_shared = TRUE;

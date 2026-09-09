-- 部门共享：成本库常用行标记（多部门可分别标记同一 cost_id）
CREATE TABLE cost_dept_highlight (
    id              BIGSERIAL PRIMARY KEY,
    dept_id         BIGINT       NOT NULL REFERENCES sys_department(id) ON DELETE CASCADE,
    cost_mode       VARCHAR(16)  NOT NULL,
    cost_id         BIGINT       NOT NULL,
    color           VARCHAR(16)  NOT NULL,
    remark          VARCHAR(128),
    marked_by       BIGINT       NOT NULL REFERENCES sys_user(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cost_dept_highlight UNIQUE (dept_id, cost_mode, cost_id)
);

CREATE INDEX idx_cost_dept_highlight_mode_cost ON cost_dept_highlight (cost_mode, cost_id);
CREATE INDEX idx_cost_dept_highlight_dept_mode ON cost_dept_highlight (dept_id, cost_mode);

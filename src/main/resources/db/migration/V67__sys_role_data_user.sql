-- 角色「指定数据」可见用户关联表
CREATE TABLE IF NOT EXISTS sys_role_data_user (
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_role_data_user_user_id ON sys_role_data_user(user_id);

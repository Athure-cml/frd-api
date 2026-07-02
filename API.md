# 福瑞多报价系统 API

## 接口文档（Swagger UI）

| 地址 | 说明 |
|------|------|
| http://localhost:8080/swagger-ui/index.html | 可视化接口文档 |
| http://localhost:8080/api-docs | OpenAPI JSON |
| http://localhost:8080/h2-console | 本地 H2 数据库控制台（仅 `local` 配置） |

权限设计详见：[docs/AUTHZ.md](docs/AUTHZ.md)

## 测试账号（密码均为 `123456`）

| 工号 | 部门 | 角色 | 数据范围 |
|------|------|------|----------|
| `vben` | 客服部 | 超级管理员 | ALL |
| `cs001` | 客服部 | 销售/客服 | SELF（仅本人数据） |
| `doc001` | 单证部 | 单证员 | DEPT |
| `ops001` | 海外操作部 | 海外操作 | DEPT |
| `bkg001` | 订舱部 | 订舱员 | DEPT |
| `fin001` | 财务部 | 财务 | ALL |

## 数据库

- **默认**：嵌入式 H2，数据文件 `./data/furuiduo`（无需安装 MySQL）
- **切换 MySQL**：`--spring.profiles.active=mysql`，先建库 `furuiduo_quote`

## 当前接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/hello` | 健康检查 |
| POST | `/auth/login` | 登录 |
| GET | `/auth/codes` | 权限码列表 |
| GET | `/menu/all` | 当前用户侧边栏菜单树（按权限/角色过滤） |
| POST | `/auth/logout` | 退出 |
| GET | `/user/info` | 用户信息（含部门、角色、dataScope、passwordSecurity） |
| PUT | `/user/profile` | 更新当前用户资料（姓名、手机号、邮箱） |
| POST | `/user/avatar` | 上传当前用户头像（JPG/PNG/GIF/WEBP，最大 2MB） |
| PUT | `/user/password` | 修改当前用户密码 |
| GET | `/sys/departments` | 部门列表 |
| GET | `/sys/departments/{id}` | 部门详情 |
| POST | `/sys/departments` | 新建部门（`sys:dept:manage`） |
| PUT | `/sys/departments/{id}` | 更新部门 |
| DELETE | `/sys/departments/{id}` | 删除部门 |
| GET | `/sys/users` | 用户分页列表（`page`、`pageSize`、`username`、`deptId`、`status`） |
| GET | `/sys/users/{id}` | 用户详情 |
| POST | `/sys/users` | 新建用户（`sys:user:manage`） |
| PUT | `/sys/users/{id}` | 更新用户 |
| DELETE | `/sys/users/{id}` | 删除用户 |
| GET | `/sys/roles` | 角色列表（含权限码） |
| GET | `/sys/roles/{id}` | 角色详情 |
| POST | `/sys/roles` | 新建角色（`sys:role:manage`） |
| PUT | `/sys/roles/{id}` | 更新角色 |
| DELETE | `/sys/roles/{id}` | 删除角色 |
| GET | `/sys/permissions` | 权限码目录（角色表单用） |
| GET | `/cost-library/road` | 卡车成本分页（`cost:road:view`） |
| POST | `/cost-library/road` | 新建卡车成本（`cost:road:edit`） |
| PUT | `/cost-library/road/{id}` | 更新卡车成本 |
| DELETE | `/cost-library/road/{id}` | 删除卡车成本 |
| POST | `/cost-library/road/batch-delete` | 批量删除 |
| PATCH | `/cost-library/road/batch` | 批量修改 `{ ids, fields }` |
| POST | `/cost-library/road/import` | Excel 导入（multipart `file`） |
| GET | `/cost-library/road/export` | Excel 导出（支持 `templateId`，按模板列顺序与自定义字段） |
| GET | `/cost-library/sea` | 海运成本分页（`cost:sea:view`） |
| POST/PUT/DELETE | `/cost-library/sea/...` | 海运 CRUD（`cost:sea:edit`） |
| POST | `/cost-library/sea/batch-delete` | 海运批量删除 |
| PATCH | `/cost-library/sea/batch` | 海运批量修改 |
| POST | `/cost-library/sea/import` | 海运 Excel 导入 |
| GET | `/cost-library/sea/export` | 海运 Excel 导出 |
| GET | `/cost-library/rail` | 铁路成本分页（`cost:rail:view`） |
| POST/PUT/DELETE | `/cost-library/rail/...` | 铁路 CRUD（`cost:rail:edit`） |
| POST | `/cost-library/rail/batch-delete` | 铁路批量删除 |
| PATCH | `/cost-library/rail/batch` | 铁路批量修改 |
| POST | `/cost-library/rail/import` | 铁路 Excel 导入 |
| GET | `/cost-library/rail/export` | 铁路 Excel 导出 |
| GET | `/cost-library/templates?mode=road\|sea\|rail` | 表格视图模板列表（对应模式 `cost:*:view`） |
| GET | `/cost-library/templates/{id}` | 表格视图模板详情 |
| POST | `/cost-library/templates` | 新建表格模板（`cost:{mode}:edit`） |
| PUT | `/cost-library/templates/{id}` | 更新表格模板 |
| DELETE | `/cost-library/templates/{id}` | 删除表格模板（不可删当前默认） |
| POST | `/cost-library/templates/{id}/set-default` | 设为默认模板 |
| GET | `/cost-library/templates/{id}/export` | 导出模板 Excel |
| POST | `/cost-library/templates/export` | 按当前布局预览导出 Excel |

## 统一响应

```json
{
  "code": 0,
  "data": {},
  "error": null,
  "message": "ok"
}
```

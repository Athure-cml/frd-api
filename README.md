# 福瑞多报价系统 — 后端 API

Spring Boot 3 + JPA + Flyway，为 [vue-vben-admin](../后台管理系统/vue-vben-admin) 提供 REST 接口。

## 本地运行

**前置条件**：本地 PostgreSQL，并创建数据库 `frd_postgres`。

```bash
mvn spring-boot:run
```

默认 profile 为 `postgres`，连接 `jdbc:postgresql://127.0.0.1:5432/frd_postgres`。  
可通过环境变量覆盖：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`。

服务地址 `http://localhost:8080`，健康检查 `GET /hello`。  
首次启动会自动执行 Flyway 迁移并初始化演示账号（工号 `vben`，密码 `123456`）。

### 其他 profile

| Profile | 说明 |
|---------|------|
| `postgres` | 默认，本地 PostgreSQL + Flyway |
| `render` | Render 生产部署 |
| `local` | [Legacy] 嵌入式 H2，与生产 schema 不兼容 |
| `mysql` | [Legacy] MySQL，不启用 Flyway |

## 部署

见 [DEPLOY.md](./DEPLOY.md) 与上级 [../DEPLOY.md](../DEPLOY.md)（Render + PostgreSQL）。

## 文档

- `docs/AUTHZ.md` — 权限
- `docs/quote-sheet-schema.sql` — 报价单表结构参考（历史 MySQL 草稿，以 Flyway V1 为准）

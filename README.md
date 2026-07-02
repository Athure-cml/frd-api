# 福瑞多报价系统 — 后端 API

Spring Boot 3 + JPA，为 [vue-vben-admin](../后台管理系统/vue-vben-admin) 提供 REST 接口。

## 本地运行

```bash
mvn spring-boot:run
```

默认 `http://localhost:8080`，健康检查 `GET /hello`。

## 部署

见 [DEPLOY.md](./DEPLOY.md) 与上级 [../DEPLOY.md](../DEPLOY.md)（Render + PostgreSQL）。

## 文档

- `docs/AUTHZ.md` — 权限
- `docs/quote-sheet-schema.sql` — 报价单表结构参考

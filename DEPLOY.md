# quote-api Render 部署速查

完整说明见上级目录 [`../DEPLOY.md`](../DEPLOY.md)。

## 快速命令

```bash
# 本地
mvn spring-boot:run

# Render Build
mvn clean package -DskipTests

# Render Start
java -jar target/quote-api-0.0.1-SNAPSHOT.jar
```

## 必配环境变量

```
SPRING_PROFILES_ACTIVE=render
JAVA_VERSION=21
CORS_ALLOWED_ORIGIN_PATTERNS=https://your-app.vercel.app,https://*.vercel.app
```

关联 Postgres 后 `DATABASE_URL` 或 `PGHOST` 等由 Render 自动注入。

## 数据库表结构（Git 管理）

| 文件 | 说明 |
|------|------|
| `src/main/resources/db/migration/V1__init_schema.sql` | Flyway 全量建表（24 张业务表 + 关联表） |
| `scripts/reset-postgres.sql` | 清空库并重建 schema |

Render 启动时 `spring.flyway.enabled=true`，自动执行迁移。`ddl-auto=none`，不再依赖 Hibernate 自动建表。

## 生产库重建步骤（表缺失 / 500 报错时）

1. Render Dashboard → PostgreSQL → **Connect** → 打开 SQL 控制台
2. 执行 `scripts/reset-postgres.sql`：

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO PUBLIC;
```

3. Render → frd-api → **Manual Deploy** 重新部署
4. 启动后 Flyway 执行 V1 建表，DataSeeder 写入初始用户（`vben` / `123456`）
5. 验证：`GET https://frd-api.onrender.com/hello` → 200

## 健康检查

`GET /hello`

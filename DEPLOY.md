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
| `scripts/reset-postgres.sql` | 清库 SQL（供 psql / CLI 使用） |

Render 启动时 `spring.flyway.enabled=true`，自动执行迁移。`ddl-auto=none`，不再依赖 Hibernate 自动建表。

## 生产库重建（Render 已无网页 SQL 控制台）

### 方式 A：环境变量一键清库（推荐，无需 SQL 控制台）

1. Render → **frd-api** 服务 → **Environment** → 新增：

   ```
   QUOTE_DB_RESET=true
   ```

2. **Manual Deploy** 重新部署，等日志出现「数据库重建完成」
3. **立刻删除** `QUOTE_DB_RESET` 环境变量，再 **Manual Deploy** 一次（否则每次启动都会清库）
4. 验证：`GET https://frd-api.onrender.com/hello` → 200，用 `vben` / `123456` 登录

## 仅重置三个成本库测试数据（不影响用户/权限/报价单）

1. Render → **frd-api** → **Environment** → 新增：

   ```
   QUOTE_COST_RESET=true
   ```

2. **Manual Deploy**，等日志出现「成本库测试数据已重建」
3. **立刻删除** `QUOTE_COST_RESET`，再 **Manual Deploy** 一次
4. 三个成本库各为 20 条全新测试数据（含 zipCode 等最新字段）

### 方式 B：Render CLI + psql

```bash
# 安装 CLI 后登录：https://render.com/docs/cli
render login
render psql <你的数据库名或ID> -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO PUBLIC;"
```

然后 Manual Deploy frd-api。

### 方式 C：本机 psql（External Connection）

1. Render → PostgreSQL → 右上角 **Connect** → **External Connection**
2. 复制 **PSQL Command** 或 **External Database URL**
3. 在本机 PowerShell 执行（把命令换成你复制的）：

```powershell
# 示例：Render 提供的 PSQL Command 粘贴后加 -c
$env:PGPASSWORD="你的密码"
psql -h dpg-xxxxx-a.oregon-postgres.render.com -U frd_api_user frd_api -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO PUBLIC;"
```

未安装 psql：可安装 [PostgreSQL 客户端](https://www.postgresql.org/download/windows/)，或用 DBeaver / pgAdmin 用 External URL 连接后执行 `scripts/reset-postgres.sql`。

### 方式 D：新建 Postgres 再关联（最省事但会换库）

1. Render 新建一个 PostgreSQL 实例
2. frd-api → Environment → 断开旧库，**Connect** 新库
3. Manual Deploy（Flyway 会在空库上自动建表）

## 健康检查

`GET /hello`

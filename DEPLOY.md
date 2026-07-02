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

关联 Postgres 后 `PGHOST` 等由 Render 自动注入（见 `render.yaml`）。

## 健康检查

`GET /hello`

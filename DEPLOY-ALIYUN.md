# 阿里云 Nginx + PostgreSQL 部署

## 架构

```
浏览器 → Nginx (80/443)
           ├─ /          → 前端静态文件 (apps/web-antd/dist)
           └─ /api/      → 反代 quote-api (127.0.0.1:8080，去掉 /api 前缀)
                quote-api → PostgreSQL
```

同域反代可避免 CORS；**不要**再让前端请求 `https://frd-api.onrender.com`。

## 1. 后端

```bash
cd quote-api
mvn clean package -DskipTests
```

产物：`target/quote-api-0.0.1-SNAPSHOT.jar`

环境变量示例（systemd 或启动脚本）：

```bash
export SPRING_PROFILES_ACTIVE=postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/frd_postgres
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=你的密码
export PORT=8080
# 若前端与 API 不同域才需要；同域 /api 反代可省略
# export CORS_ALLOWED_ORIGIN_PATTERNS=http://121.41.48.28,http://121.41.48.28:*

java -jar target/quote-api-0.0.1-SNAPSHOT.jar
```

邮编数据：生产无 US.txt 自动导入，登录后在 **主数据 → 美国州邮政编码 → 导入** 上传 GeoNames `US.txt`。

## 2. 前端

```bash
cd vue-vben-admin/apps/web-antd
cp .env.production.aliyun.example .env.production
# 编辑 .env.production，确认 VITE_GLOB_API_URL=/api
cd ../..
pnpm build:antd
```

将 `apps/web-antd/dist/` 上传到服务器，例如 `/var/www/frd-admin/`。

## 3. Nginx 配置示例

```nginx
server {
    listen 80;
    server_name 121.41.48.28;   # 或你的域名

    root /var/www/frd-admin;
    index index.html;

    # 前端 SPA（hash 路由）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反代：/api/auth/login → http://127.0.0.1:8080/auth/login
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 50m;
        proxy_read_timeout 600s;
    }

    # 头像等上传文件（若后端返回 /uploads/...）
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/uploads/;
    }
}
```

```bash
nginx -t && systemctl reload nginx
```

配置文件模板：`deploy/nginx/frd-admin.conf`（复制到 `/etc/nginx/conf.d/` 并修改 `server_name`、`root`）。

## 4. 验证

```bash
curl http://127.0.0.1:8080/hello
curl http://121.41.48.28/api/hello
```

浏览器访问 `http://121.41.48.28/#/auth/login`，Network 中登录请求应为：

`http://121.41.48.28/api/auth/login`（**不是** onrender.com）

## 5. 常见错误

| 现象 | 原因 | 处理 |
|------|------|------|
| CORS blocked，请求 onrender.com | 用了旧 dist，API 仍指向 Render | 按上文改 `.env.production` 并 **重新 build** |
| 502 Bad Gateway | quote-api 未启动或端口不对 | 检查 `java -jar` 与 `proxy_pass` 端口 |
| 登录 401 / 数据库错误 | PG 连接失败 | 检查 `SPRING_DATASOURCE_*` 与库是否已建 |

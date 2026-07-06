# Nginx 配置教程（阿里云 · 零基础）

按顺序在**服务器上**执行即可。假设：

- 服务器 IP：`121.41.48.28`（改成你的）
- 前端目录：`/var/www/frd-admin`
- 后端 Java：`127.0.0.1:8080`（quote-api 已启动）

---

## 第一步：安装 Nginx

**Alibaba Cloud Linux / CentOS：**

```bash
sudo yum install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

**Ubuntu / Debian：**

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

浏览器访问 `http://你的IP`，能看到 Nginx 默认页说明安装成功。

---

## 第二步：上传前端文件

在你**本机**已打好包，目录是：

`vue-vben-admin/apps/web-antd/dist/`

把整个 `dist` **里面的所有文件**（不是 dist 文件夹本身）上传到服务器：

```bash
# 本机 PowerShell 示例（需已安装 scp）
scp -r dist/* root@121.41.48.28:/var/www/frd-admin/
```

**或在服务器上创建目录后，用 WinSCP / FileZilla 上传：**

```bash
sudo mkdir -p /var/www/frd-admin
sudo chown -R $USER:$USER /var/www/frd-admin
```

上传后服务器上应有：`/var/www/frd-admin/index.html`

---

## 第三步：上传 Nginx 配置文件

把项目里的 `deploy/nginx/frd-admin.conf` 拷到服务器，例如：

```bash
# 本机
scp deploy/nginx/frd-admin.conf root@121.41.48.28:/tmp/frd-admin.conf
```

**在服务器上：**

```bash
# 改 IP（如需要）
sudo sed -i 's/121.41.48.28/你的IP或域名/g' /tmp/frd-admin.conf

# 安装配置
sudo cp /tmp/frd-admin.conf /etc/nginx/conf.d/frd-admin.conf

# 若 default 配置冲突，可关掉默认站点（可选）
sudo mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak 2>/dev/null || true
```

**Ubuntu** 有时用 `sites-available`：

```bash
sudo cp /tmp/frd-admin.conf /etc/nginx/sites-available/frd-admin.conf
sudo ln -sf /etc/nginx/sites-available/frd-admin.conf /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
```

---

## 第四步：检查并重启 Nginx

```bash
sudo nginx -t
```

看到 `syntax is ok` 和 `test is successful` 后：

```bash
sudo systemctl reload nginx
```

---

## 第五步：确认后端在跑

```bash
curl http://127.0.0.1:8080/hello
```

应返回类似：`{"code":0,"data":"Hello"...}`

再测 Nginx 反代：

```bash
curl http://127.0.0.1/api/hello
```

也应返回同样内容。

---

## 第六步：浏览器登录

打开：`http://121.41.48.28/#/auth/login`

按 F12 → Network，点登录，请求地址应为：

`http://121.41.48.28/api/auth/login`

**不能**再出现 `onrender.com`。

默认账号：`vben` / `123456`

---

## 一键脚本（可选）

在服务器上，进入 quote-api 目录后：

```bash
chmod +x deploy/setup-nginx.sh
sudo ./deploy/setup-nginx.sh 121.41.48.28 /var/www/frd-admin
```

---

## 常见问题

| 现象 | 处理 |
|------|------|
| 403 Forbidden | `sudo chmod -R 755 /var/www/frd-admin` |
| 502 Bad Gateway | 后端没启动：`java -jar ...` 或 `systemctl start frd-api` |
| 404 刷新页面白屏 | 确认配置里有 `try_files $uri $uri/ /index.html;` |
| 仍请求 onrender | 重新上传**新打的** dist（`VITE_GLOB_API_URL=/api`） |
| 阿里云安全组 | 控制台放行 **80** 端口（HTTPS 再开 443） |

---

## 配置文件做了什么（看懂即可）

```nginx
location / {
    try_files $uri $uri/ /index.html;   # 前端 Vue 路由
}

location /api/ {
    proxy_pass http://127.0.0.1:8080/;  # /api/xxx → 后端 /xxx
}
```

浏览器访问 `/api/auth/login` → Nginx 转发到 `http://127.0.0.1:8080/auth/login`  
前后端**同域**，所以没有跨域问题。

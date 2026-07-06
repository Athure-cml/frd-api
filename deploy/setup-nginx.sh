#!/usr/bin/env bash
# 阿里云 Nginx 一键配置（在服务器上以 root 或 sudo 运行）
# 用法：sudo ./deploy/setup-nginx.sh [服务器IP或域名] [前端目录]
# 示例：sudo ./deploy/setup-nginx.sh 121.41.48.28 /var/www/frd-admin

set -euo pipefail

SERVER_NAME="${1:-121.41.48.28}"
WEB_ROOT="${2:-/var/www/frd-admin}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_SRC="$SCRIPT_DIR/nginx/frd-admin.conf"
CONF_DST="/etc/nginx/conf.d/frd-admin.conf"

if [[ ! -f "$CONF_SRC" ]]; then
  echo "找不到 $CONF_SRC"
  exit 1
fi

echo "==> 安装 Nginx（如已安装会跳过）"
if command -v apt-get &>/dev/null; then
  apt-get update -qq
  apt-get install -y nginx
elif command -v yum &>/dev/null; then
  yum install -y nginx
else
  echo "请手动安装 nginx 后重试"
  exit 1
fi

echo "==> 创建前端目录 $WEB_ROOT"
mkdir -p "$WEB_ROOT"
if [[ ! -f "$WEB_ROOT/index.html" ]]; then
  echo "警告：$WEB_ROOT/index.html 不存在，请先把 dist 文件上传到此目录"
fi

echo "==> 写入 Nginx 配置 server_name=$SERVER_NAME root=$WEB_ROOT"
sed -e "s|server_name 121.41.48.28;|server_name $SERVER_NAME;|" \
    -e "s|root /var/www/frd-admin;|root $WEB_ROOT;|" \
    "$CONF_SRC" > /tmp/frd-admin.conf
cp /tmp/frd-admin.conf "$CONF_DST"

if [[ -f /etc/nginx/conf.d/default.conf ]]; then
  mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak.$(date +%s) || true
  echo "已备份默认站点 default.conf"
fi

echo "==> 检查配置"
nginx -t

echo "==> 启动 / 重载 Nginx"
systemctl enable nginx
systemctl restart nginx

echo ""
echo "完成。请确认："
echo "  1. 后端：curl http://127.0.0.1:8080/hello"
echo "  2. 反代：curl http://127.0.0.1/api/hello"
echo "  3. 浏览器：http://$SERVER_NAME/#/auth/login"
echo ""
echo "若 /api/hello 失败，请先启动 quote-api："
echo "  java -jar /opt/frd-api/quote-api-0.0.1-SNAPSHOT.jar"

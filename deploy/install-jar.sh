#!/usr/bin/env bash
# quote-api systemd 服务示例（阿里云）
# 用法：
#   1. 复制 jar 到 /opt/frd-api/
#   2. sudo cp deploy/systemd/frd-api.service /etc/systemd/system/
#   3. 编辑 Environment 中的数据库密码
#   4. sudo systemctl daemon-reload && sudo systemctl enable --now frd-api

set -euo pipefail

INSTALL_DIR="/opt/frd-api"
JAR_NAME="quote-api-0.0.1-SNAPSHOT.jar"

mkdir -p "$INSTALL_DIR"
cp "target/$JAR_NAME" "$INSTALL_DIR/"
echo "已复制 $INSTALL_DIR/$JAR_NAME"
echo "请配置 systemd 并启动：systemctl start frd-api"

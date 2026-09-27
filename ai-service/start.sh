#!/usr/bin/env bash
# ai-service 启动脚本 (开发模式)
# 用法: ./start.sh

set -e

cd "$(dirname "$0")"

# 检查 .env
if [ ! -f .env ]; then
  echo "⚠️  .env 不存在，复制 .env.example 并填写 LLM_API_KEY"
  cp .env.example .env
  echo "已创建 .env，请编辑后重新运行"
  exit 1
fi

# 检查依赖
if ! python -c "import fastapi" 2>/dev/null; then
  echo "📦 安装依赖..."
  pip install -r requirements.txt
fi

echo "🚀 启动 AI 服务 (port 8000)..."
exec python run.py

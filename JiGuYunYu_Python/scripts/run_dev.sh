#!/usr/bin/env bash
# run_dev.sh —— Linux/macOS 开发环境一键启动
set -euo pipefail

# 1. 检查 .env
if [ ! -f .env ]; then
    echo "[WARN] .env not found, copying from .env.example ..."
    if [ -f .env.example ]; then cp .env.example .env; else echo "[ERROR] .env.example not found"; exit 1; fi
fi

# 2. 加载 .env
set -a
source .env
set +a

# 3. 设置 PYTHONPATH
export PYTHONPATH="$(pwd)"

# 4. 启动 uvicorn
echo "Starting dev server on http://0.0.0.0:8000 ..."
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

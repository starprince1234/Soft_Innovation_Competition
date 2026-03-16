# syntax=docker/dockerfile:1.7

# ---- build stage ----
ARG PYTHON_BASE_IMAGE=python:3.11
FROM ${PYTHON_BASE_IMAGE} AS builder
WORKDIR /app

# 安装构建依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl gcc \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml ./
# 安装项目基础依赖
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --upgrade pip setuptools wheel \
    && pip install .

COPY . .

# ---- runtime stage ----
FROM ${PYTHON_BASE_IMAGE}
WORKDIR /app

# 安装运行时必需的工具（curl 用于 healthcheck）
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app

# 从 builder 拷贝已安装的包
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin
COPY --from=builder /app /app

# 切换到非特权用户
USER app

# 生产 workers 由环境变量控制
ENV UVICORN_WORKERS=2
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
# sentence-transformers 模型缓存目录（持久化挂载可复用）
ENV SENTENCE_TRANSFORMERS_HOME=/app/.cache/sentence_transformers

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8000/health || exit 1

CMD uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers ${UVICORN_WORKERS}

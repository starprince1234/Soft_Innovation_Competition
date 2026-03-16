# JiGuYunYu — Python 内部 AI 服务

> FastAPI 服务，面向 Java 提供 `/internal/*` 检测/对话/RAG/健康检查能力。

---

## 快速开始

### 1. 配置

```bash
cp .env.example .env
# 编辑 .env，至少填写 INTERNAL_TOKEN
```

### 2. 安装依赖

```bash
pip install -e ".[dev]"
```

### 3. 本地启动

**Windows PowerShell**
```powershell
.\scripts\run_dev.ps1
```

**Linux / macOS**
```bash
bash scripts/run_dev.sh
```

或手动：
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. 运行测试

```bash
pytest -q
```

### 5. 冒烟测试（需先启动服务）

```bash
python scripts/smoke_test.py
```

---

## 接口契约

### 公开接口

| 方法 | 路径      | 说明          | 鉴权 |
|------|-----------|---------------|------|
| GET  | `/health` | 进程级健康检查 | 无   |

### 内部接口（`/internal/*`）

> **必须携带** `X-Internal-Token` 请求头，值与 `.env` 中 `INTERNAL_TOKEN` 一致。

| 方法 | 路径                            | 说明             |
|------|---------------------------------|------------------|
| GET  | `/internal/health`              | 聚合依赖健康探测 |
| POST | `/internal/detect/process`      | 图片检测         |
| POST | `/internal/dialog/responses`    | RAG + LLM 对话   |

### 统一响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... }
}
```

| code  | 含义                |
|-------|---------------------|
| 0     | 成功                |
| 1001  | 鉴权失败（token）   |
| 1002  | IP 不在白名单       |
| 2001  | 下游依赖不可用      |
| 4000  | 业务错误            |
| 9000  | 未知错误            |

---

## 鉴权规则

1. **Header**：`X-Internal-Token`
2. **IP 白名单**（可选）：`INTERNAL_IP_WHITELIST` 逗号分隔，空则不启用
3. 真实 IP 优先从 `X-Real-IP` / `X-Forwarded-For` 获取

---

## Docker

```bash
DOCKER_BUILDKIT=1 docker build -t jiguyunyu .
docker run -p 8000:8000 --env-file .env jiguyunyu
```

若希望跨多次构建持续复用依赖下载缓存（依赖不变则几乎不重新下载），推荐：

```bash
docker buildx build \
  --load \
  --cache-from=type=local,src=.docker-buildx-cache \
  --cache-to=type=local,dest=.docker-buildx-cache,mode=max \
  -t jiguyunyu .
```

生产环境可通过 `UVICORN_WORKERS` 环境变量控制 worker 数量。

---

## 项目结构

```
app/
├── main.py              # FastAPI 入口
├── bootstrap.py         # 依赖装配（Composition Root）
├── settings.py          # 统一配置
├── exceptions.py        # 项目级异常
├── logging_config.py    # 结构化日志
├── api/                 # 路由 + 契约
├── domain/              # 纯领域模型 + ports 抽象
├── infra/               # 外部依赖实现
├── middlewares/          # 鉴权 / request_id
├── rag/                 # 检索策略
└── usecases/            # 业务编排
```

---

## 常见排障

| 问题                     | 排查方向                        |
|--------------------------|---------------------------------|
| 401 unauthorized         | 检查 `X-Internal-Token` 是否正确 |
| 403 ip forbidden         | 检查 `INTERNAL_IP_WHITELIST`    |
| 2001 dependency error    | 检查 LLM/向量/缓存连接配置     |
| 服务启动时缺 INTERNAL_TOKEN | 确保 `.env` 已配置             |

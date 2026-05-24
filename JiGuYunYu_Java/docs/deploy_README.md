```md
# JiGuYunYu 部署指南（Docker / Docker Compose）

本项目通过 **Docker Compose** 一键启动以下 5 个服务：
- **MySQL 8.0** — 数据库
- **Redis 7** — 缓存 / JWT 黑名单 / API 限流
- **jigu-java** — Java 后端（Spring Boot 3，对外 API）
- **jigu-python** — Python AI 服务（FastAPI，RAG / 检测 / 对话）
- **Nginx 1.27** — 反向代理入口

> **目标**
> 1. 本地开发：`docker compose up -d` 即可运行全栈
> 2. 上云部署：推送镜像到 DockerHub → 云服务器 pull + compose up
> 3. 统一入口：所有外部请求 → Nginx → Java；Python 仅内部通信，不暴露

---

## 目录结构（deploy）

```
deploy/
  docker-compose.yml        # 统一 Compose 文件（dev/prod 共用）
  .env.example              # 环境变量模板（必须复制为 .env）
  nginx/nginx.conf          # Nginx 路由配置
  mysql/init/01_schema.sql  # MySQL 初始化 DDL
```

---

## 0. 前置要求

- **Docker**（Windows 推荐 Docker Desktop + WSL2）
- **Docker Compose**（新版自带：`docker compose`）

---

## 1. 配置 `.env`

`.env` 是 **唯一需要修改的文件**。

```bash
cd deploy/
cp .env.example .env
# 编辑 .env，按下方说明填入真实值
```

### 必须修改的变量

| 变量 | 用途 | 说明 |
|------|------|------|
| `DH_USER` | DockerHub 用户名 | 推镜像的账号 |
| `VER` | 镜像版本号 | 如 `0.1.0`，不要只用 latest |
| `MYSQL_PASSWORD` | MySQL 业务用户密码 | 替换默认值 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 替换默认值 |
| `JWT_SECRET` | JWT 签名密钥 | ≥32 字符随机字符串 |
| `INTERNAL_TOKEN` | Java ↔ Python 内部鉴权 | 随机字符串，两端必须一致 |
| `QIANFAN_API_KEY` | 百度千帆 API Key | 千帆控制台获取 |
| `QIANFAN_SECRET_KEY` | 百度千帆 Secret Key | 千帆控制台获取 |
| `VLM_BASE_URL` | VLM 模型（Qwen3-VL-LoRA）地址 | OpenAI-compatible API 地址，通常以 `/v1` 结尾 |
| `VLM_API_KEY` | VLM API Key | 从模型服务或部署平台获取，生产环境放入 Secrets |
| `BOS_ENDPOINT` | 百度 BOS 端点 | 如 `https://bj.bcebos.com` |
| `BOS_ACCESS_KEY` | BOS Access Key | BOS 控制台获取 |
| `BOS_SECRET_KEY` | BOS Secret Key | BOS 控制台获取 |
| `BOS_BUCKET` | BOS Bucket 名称 | 如 `jigu-bucket` |

### 向量库配置（已预设）

```env
VECTOR_BACKEND=chroma
CHROMA_PERSIST_DIR=/app/chroma_data
CHROMA_COLLECTION=jigu_artifacts
EMBEDDING_DIM=384
EMBEDDING_MODEL=paraphrase-multilingual-MiniLM-L12-v2
```

> 向量库使用 **ChromaDB + Sentence-BERT (384d)**，Docker Compose 已配置持久化卷。
> 首次启动时 Python 容器会自动下载模型（约 500MB），此后从缓存加载。

---

## 2. 本地开发启动

```bash
cd deploy/

# 1) 准备 .env（首次）
cp .env.example .env
# 编辑 .env

# 2) 启动
docker compose up -d

# 3) 查看状态
docker compose ps

# 4) 查看日志
docker compose logs -f jigu-java
docker compose logs -f jigu-python
```

### 验证

```bash
# Nginx 入口 → Java
curl http://localhost/api/v1/health

# Nginx 自检
curl http://localhost/nginx-health

# Swagger 文档（开发环境）
# 浏览器访问 http://localhost/swagger-ui/index.html
```

---

## 3. 构建并推送镜像

### 3.1 本地构建镜像

在项目根目录（非 deploy）执行：

```bash
# 构建 Java 镜像
docker build -t jigu-java:0.1.0 -f docker/java/Dockerfile .

# 构建 Python 镜像（在 JiGuYunYu_Python 目录）
cd ../JiGuYunYu_Python
DOCKER_BUILDKIT=1 docker build -t jigu-python:0.1.0 .

# 如需最大化复用 pip 下载缓存（推荐）
docker buildx build \
  --load \
  --cache-from=type=local,src=.docker-buildx-cache \
  --cache-to=type=local,dest=.docker-buildx-cache,mode=max \
  -t jigu-python:0.1.0 .
```

### 3.2 打 Tag 并推送到 DockerHub

```bash
# 登录 DockerHub
docker login

# Tag（替换 your_dockerhub_user 为你的用户名）
docker tag jigu-java:0.1.0 your_dockerhub_user/jigu-java:0.1.0
docker tag jigu-python:0.1.0 your_dockerhub_user/jigu-python:0.1.0

# Push
docker push your_dockerhub_user/jigu-java:0.1.0
docker push your_dockerhub_user/jigu-python:0.1.0
```

---

## 4. 云服务器部署

### 4.1 上传 deploy 目录

将 `deploy/` 目录上传到云服务器：

```bash
scp -r deploy/ user@your_server:/opt/jigu/deploy
```

### 4.2 首次启动

```bash
ssh user@your_server
cd /opt/jigu/deploy

cp .env.example .env
# 编辑 .env（填入生产环境的真实密钥、密码）

docker compose up -d
docker compose ps
```

### 4.3 安全组配置

云服务器安全组/防火墙仅放行：
- **80**（HTTP，Nginx 入口）
- **443**（HTTPS，未来启用 TLS 时）
- **22**（SSH 管理）

> **不要**对外暴露 3306 (MySQL)、6379 (Redis)、8080 (Java)、8000 (Python) 端口。
> Docker Compose 中这些端口已仅映射于容器内网络。

---

## 5. 版本更新

当发布新版本后：

```bash
# 1) 修改 .env 中的 VER
VER=0.2.0

# 2) 拉取新镜像
docker compose pull

# 3) 滚动更新
docker compose up -d
```

---

## 6. 常见问题

### 6.1 拉不到镜像
- 检查 `.env` 的 `DH_USER` 和 `VER` 是否正确
- 镜像仓库是否 public，或已 `docker login`

### 6.2 端口冲突
修改 `.env` 的端口变量后重启：
```bash
docker compose up -d
```

### 6.3 Java 连不上 MySQL / Redis
```bash
docker compose ps        # 检查服务健康状态
docker compose logs -f jigu-java  # 查看 Java 启动日志
```
MySQL 和 Redis 设有 healthcheck，Java 会等待它们 healthy 后再启动。

### 6.4 Python AI 模型首次加载慢
首次启动 Python 容器时，Sentence-BERT 模型（约 500MB）会自动下载。
Docker Compose 已配置模型缓存卷 `model_cache`，后续启动直接从缓存加载。

### 6.5 清空并重建（⚠ 会清数据库）
```bash
docker compose down -v   # 删除容器和卷
docker compose up -d     # 重新初始化
```

---

## 7. Nginx 路由说明

| 路径 | 目标 | 说明 |
|------|------|------|
| `/` | Java:8080 | 所有外部 API |
| `/nginx-health` | Nginx 200 | 健康检查 |
| `/swagger-ui/**` | Java:8080 | API 文档 |
| `/internal/**` | 403 | 禁止外部访问内部 API |

> Python 服务通过 Docker 内部网络 (`jigu-python:8000`) 与 Java 通信，
> 不经过 Nginx，不暴露给外部。
```

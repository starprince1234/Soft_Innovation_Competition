# 稽古云语

稽古云语是一个面向文物识别、文物知识问答和文物管理的全栈项目。仓库包含 Java API 后端、Python AI 内部服务、Android 客户端、静态 Web 联调页，以及本地/云端 Docker Compose 部署配置。

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [环境变量](#环境变量)
- [常用命令](#常用命令)
- [测试](#测试)
- [构建](#构建)
- [部署](#部署)
- [Docker 运行方式](#docker-运行方式)
- [API 文档](#api-文档)
- [数据库说明](#数据库说明)
- [Android 客户端](#android-客户端)
- [复现检查清单](#复现检查清单)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)
- [License](#license)

## 功能特性

- 用户注册、登录、登出、JWT 鉴权和角色权限区分。
- 文物列表、文物详情、收藏、反馈和团队协作接口。
- 文物图片检测任务：Java 对外提供任务接口，Python 内部服务调用 VLM/占位检测器处理。
- AI 对话与 RAG：Python 服务提供内部对话、知识检索和健康检查能力。
- 管理端接口：用户管理、反馈管理、文物审核、知识库重建和仪表盘概览。
- Android 客户端：Kotlin + Jetpack Compose 实现核心页面和接口封装。
- Docker Compose 一键启动 MySQL、Redis、Java、Python、Nginx 和本地 mock VLM。

## 技术栈

- 后端：Java 17、Spring Boot 3.1.6、Spring Security、Spring Data JPA、Maven
- AI 服务：Python >= 3.10、FastAPI、Uvicorn、Pydantic、ChromaDB、Sentence Transformers、Redis
- Android：Kotlin、Jetpack Compose、Gradle、Retrofit、OkHttp、Coil
- 数据库/缓存：MySQL 8.0、Redis 7、ChromaDB
- AI / LLM：百度千帆、OpenAI-compatible VLM 服务、Qwen 系列模型配置
- 部署：Docker、Docker Compose、Nginx

## 项目结构

```text
.
├── JiGuYunYu_Java/                 # Spring Boot 多模块 Java 后端
│   ├── jigu-bootstrap/             # Java 服务入口
│   ├── jigu-api/                   # Controller 与 API DTO
│   ├── jigu-application/           # 应用服务
│   ├── jigu-domain/                # 领域模型与仓储接口
│   ├── jigu-infrastructure/        # JPA、Redis、BOS、Python Client 等实现
│   └── deploy/                     # Java 侧 Docker Compose、Nginx、MySQL 初始化脚本
├── JiGuYunYu_Python/               # FastAPI AI 内部服务
│   ├── app/                        # Python 服务源码
│   ├── tests/                      # pytest 测试
│   ├── scripts/                    # 启动与冒烟测试脚本
│   ├── .env.example                # Python 服务环境变量模板
│   └── pyproject.toml              # Python 依赖与工具配置
├── JiGuYunYu_Frontend/             # 前端入口
│   ├── index.html                  # 简单 Web 联调页
│   └── Android/jiguyunyu_2.0/      # Android 客户端
├── release/cloud/                  # 云端部署 compose、Nginx、迁移脚本
├── scripts/                        # 本地构建、联调、远程运维辅助脚本
├── assets/                         # 示例资源
├── .env.backend.example            # 根目录 Docker Compose 环境变量模板
├── docker-compose.backend.yml      # 本地全栈后端 Compose
└── README.md                       # 项目复现说明
```

不会提交的本地目录包括 `.venv/`、`backend-data/`、`JiGuYunYu_Python/chroma_data/`、Maven `target/`、Android `build/`、`.gradle/` 等。

## 环境要求

- Git
- Docker Desktop 或 Docker Engine + Docker Compose v2
- Java 17
- Maven 3.8+（也可以使用 IDE 内置 Maven）
- Python 3.10+，建议 3.10 或 3.11
- Android Studio / Android SDK（仅运行 Android 客户端时需要）
- 可选：PowerShell 7+（Windows 下运行部分脚本更方便）

## 快速开始

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd <项目目录>
```

如果使用当前已配置远端：

```bash
git clone https://github.com/starprince1234/Soft_Innovation_Competition.git
cd Soft_Innovation_Competition
```

### 2. 配置环境变量

本地 Docker 联调推荐使用根目录模板：

```bash
cp .env.backend.example .env.backend
```

然后编辑 `.env.backend`，至少修改：

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_TOKEN`

如果只启动 Python 服务：

```bash
cd JiGuYunYu_Python
cp .env.example .env
```

### 3. 构建镜像

在项目根目录执行：

```bash
docker build -t jigu-java:0.1.0 -f JiGuYunYu_Java/docker/java/Dockerfile JiGuYunYu_Java
docker build -t jigu-python:0.1.0 JiGuYunYu_Python
```

也可以使用已有脚本构建 Python 镜像：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build_python_image_cached.ps1 -ImageTag "jigu-python:0.1.0"
```

### 4. 启动本地后端

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d
docker compose --env-file .env.backend -f docker-compose.backend.yml ps
```

首次启动会初始化 MySQL，并为 ChromaDB 和模型缓存创建本地数据目录。

### 5. 验证服务

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8000/health
curl http://localhost/nginx-health
```

Web 联调页：

```bash
cd JiGuYunYu_Frontend
python -m http.server 5173
```

浏览器访问：

```text
http://localhost:5173/
```

## 环境变量

根目录 `.env.backend.example` 用于 `docker-compose.backend.yml`：

| 变量名 | 必填 | 默认值/示例 | 说明 |
| ------ | ---- | ----------- | ---- |
| `JIGU_JAVA_IMAGE` | 否 | `jigu-java:0.1.0` | Java 镜像名 |
| `JIGU_PYTHON_IMAGE` | 否 | `jigu-python:0.1.0` | Python 镜像名 |
| `MYSQL_PORT` | 否 | `3306` | 本地 MySQL 映射端口 |
| `REDIS_PORT` | 否 | `6379` | 本地 Redis 映射端口 |
| `JAVA_PORT` | 否 | `8080` | Java API 端口 |
| `PYTHON_PORT` | 否 | `8000` | Python 服务端口 |
| `NGINX_HTTP_PORT` | 否 | `80` | Nginx HTTP 入口端口 |
| `MYSQL_DATABASE` | 否 | `jigu` | MySQL 数据库名 |
| `MYSQL_USER` | 否 | `jigu` | MySQL 业务用户 |
| `MYSQL_PASSWORD` | 是 | `change_me_mysql_password` | MySQL 业务用户密码，生产放入 Secrets |
| `MYSQL_ROOT_PASSWORD` | 是 | `change_me_mysql_root_password` | MySQL root 密码，生产放入 Secrets |
| `SPRING_PROFILES_ACTIVE` | 否 | `docker` | Spring Profile |
| `JWT_SECRET` | 是 | `change_me_to_a_long_random_secret_at_least_32_chars` | JWT 签名密钥，生产放入 Secrets |
| `JWT_EXPIRES_SECONDS` | 否 | `3600` | JWT 有效期秒数 |
| `INTERNAL_TOKEN` | 是 | `change_me_internal_token_very_secret` | Java 与 Python 内部鉴权 token，生产放入 Secrets |
| `INTERNAL_IP_WHITELIST` | 否 | 空 | Python 内部接口 IP 白名单，空表示不启用 |
| `PYTHON_ENV` | 否 | `production` | Python 运行环境 |
| `PYTHON_LOG_LEVEL` | 否 | `INFO` | Python 日志级别 |
| `UVICORN_WORKERS` | 否 | `2` | Uvicorn worker 数 |
| `VLM_BASE_URL` | 否 | `http://mock-vlm:18080/v1` | VLM OpenAI-compatible 地址；生产放入环境变量 |
| `VLM_API_KEY` | 否 | `your_vlm_api_key_here` | VLM API Key，生产放入 Secrets |
| `VLM_MODEL_NAME` | 否 | `qwen3-vl-lora` | VLM 模型名 |
| `VLM_TIMEOUT` | 否 | `60.0` | VLM 超时秒数 |
| `VLM_ENABLE_THINKING` | 否 | 空 | 部分 Qwen 模型是否开启 thinking |
| `MOCK_VLM_PORT` | 否 | `18080` | 本地 mock VLM 端口 |
| `QIANFAN_API_KEY` | 否 | `your_qianfan_api_key_here` | 百度千帆 API Key，生产放入 Secrets |
| `QIANFAN_SECRET_KEY` | 否 | `your_qianfan_secret_key_here` | 百度千帆 Secret Key，生产放入 Secrets |
| `QIANFAN_BASE_URL` | 否 | 空 | 千帆接口地址 |
| `LLM_TIMEOUT` | 否 | `180.0` | LLM 超时秒数 |
| `PYTHON_CONNECT_TIMEOUT_MS` | 否 | `5000` | Java 调 Python 连接超时 |
| `PYTHON_READ_TIMEOUT_MS` | 否 | `180000` | Java 调 Python 读取超时 |
| `VECTOR_BACKEND` | 否 | `chroma` | 向量库后端 |
| `VECTOR_DB_URL` | 否 | 空 | 外部向量库地址，Chroma 本地模式可为空 |
| `CHROMA_COLLECTION` | 否 | `jigu_artifacts` | Chroma collection 名称 |
| `EMBEDDING_MODEL` | 否 | `paraphrase-multilingual-MiniLM-L12-v2` | Embedding 模型 |
| `EMBEDDING_DIM` | 否 | `384` | Embedding 维度 |
| `BOS_ENDPOINT` | 否 | 空 | 对象存储 endpoint，生产放入环境变量 |
| `BOS_ACCESS_KEY` | 否 | `your_bos_access_key_here` | BOS/OSS Access Key，生产放入 Secrets |
| `BOS_SECRET_KEY` | 否 | `your_bos_secret_key_here` | BOS/OSS Secret Key，生产放入 Secrets |
| `BOS_BUCKET` | 否 | 空 | BOS/OSS Bucket |
| `BOS_HEALTH_CHECK_BUCKET_EXISTS` | 否 | `false` | 是否在健康检查中探测 bucket |

生产部署还可能需要：

| 变量名 | 必填 | 说明 |
| ------ | ---- | ---- |
| `VLM_SSH_HOST` | 视部署方式而定 | VLM SSH 隧道主机，放入 Secrets |
| `VLM_SSH_PORT` | 视部署方式而定 | VLM SSH 端口 |
| `VLM_SSH_USER` | 视部署方式而定 | VLM SSH 用户 |
| `VLM_SSH_PASSWORD` | 视部署方式而定 | VLM SSH 密码，放入 Secrets |
| `VLM_REMOTE_API_PORT` | 视部署方式而定 | 远端 VLM API 端口 |
| `VLM_LOCAL_TUNNEL_PORT` | 视部署方式而定 | 本地隧道端口 |

## 常用命令

```bash
# 启动本地 Docker 后端
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d

# 查看本地 Docker 后端状态
docker compose --env-file .env.backend -f docker-compose.backend.yml ps

# 停止本地 Docker 后端
docker compose --env-file .env.backend -f docker-compose.backend.yml down

# Python 服务本地开发
cd JiGuYunYu_Python
pip install -e ".[dev]"
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Java 后端本地开发
cd JiGuYunYu_Java
mvn -pl jigu-bootstrap -am spring-boot:run

# 静态 Web 联调页
cd JiGuYunYu_Frontend
python -m http.server 5173

# Android Debug 包
cd JiGuYunYu_Frontend/Android/jiguyunyu_2.0
./gradlew assembleDebug
```

Windows 下运行 Gradle 可使用 `gradlew.bat`。

## 测试

Python：

```bash
cd JiGuYunYu_Python
pytest -q
```

Java：

```bash
cd JiGuYunYu_Java
mvn test
```

Android：

```bash
cd JiGuYunYu_Frontend/Android/jiguyunyu_2.0
./gradlew test
```

后端链路冒烟：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\e2e_java_detect_mock_vlm.ps1
```

## 构建

Java JAR：

```bash
cd JiGuYunYu_Java
mvn -pl jigu-bootstrap -am package
```

构建产物在 `JiGuYunYu_Java/**/target/`，不应提交。

Python 镜像：

```bash
docker build -t jigu-python:0.1.0 JiGuYunYu_Python
```

Java 镜像：

```bash
docker build -t jigu-java:0.1.0 -f JiGuYunYu_Java/docker/java/Dockerfile JiGuYunYu_Java
```

Android APK：

```bash
cd JiGuYunYu_Frontend/Android/jiguyunyu_2.0
./gradlew assembleDebug
```

构建产物在 Android `build/` 目录，不应提交。

## 部署

当前项目提供 Docker Compose 部署配置，但真实生产环境变量不应进入仓库。

本地后端：

```bash
cp .env.backend.example .env.backend
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d
```

云端部署参考：

- `release/cloud/docker-compose.prod.yml`
- `release/cloud/nginx.conf`
- `JiGuYunYu_Java/docs/deploy_README.md`

部署前请在 GitHub Secrets 或部署平台环境变量中配置所有密码、token、API Key、SSH 信息和对象存储密钥。

## Docker 运行方式

启动：

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build
```

停止：

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml down
```

查看日志：

```bash
docker compose --env-file .env.backend -f docker-compose.backend.yml logs -f jigu-java
docker compose --env-file .env.backend -f docker-compose.backend.yml logs -f jigu-python
```

注意：`backend-data/` 是本地 MySQL、Redis、Chroma 等运行数据，不要提交。

## API 文档

Base URL：

```text
http://localhost:8080
```

健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

认证：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}'
```

主要接口分组：

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me/password`
- `GET /api/v1/artifacts`
- `GET /api/v1/artifact/{id}`
- `POST /api/v1/detect/tasks`
- `GET /api/v1/detect/tasks/{taskId}/status`
- `GET /api/v1/detect/results/{taskId}`
- `POST /api/v1/dialog/requests`
- `GET /api/v1/dialog/results/{id}`
- `GET /api/v1/dialog/histories`
- `DELETE /api/v1/dialog/histories`
- `GET /api/v1/favorites`
- `POST /api/v1/feedback`
- `GET /api/v1/admin/dashboard/overview`

Swagger UI 默认路径：

```text
http://localhost:8080/swagger-ui/index.html
```

Python 内部服务：

```bash
curl http://localhost:8000/health
```

内部接口需要 `X-Internal-Token`，只供 Java 后端或受信任服务调用。

## 数据库说明

- 主数据库：MySQL 8.0
- 初始化脚本：`JiGuYunYu_Java/deploy/mysql/init/01_schema.sql`
- 追加迁移：`JiGuYunYu_Java/deploy/mysql/init/02_dialog_conversation_id.sql`、`release/cloud/*.sql`
- 缓存：Redis 7
- 向量库：ChromaDB，本地持久化目录为 `backend-data/chroma/` 或 `JiGuYunYu_Python/chroma_data/`

本地 Docker 首次启动 MySQL 容器时会执行 `/docker-entrypoint-initdb.d` 下的 SQL。已有数据卷不会重复初始化；如需重建数据库，请先备份数据并手动确认操作，不要把数据库文件或 dump 提交到 GitHub。

## Android 客户端

Android 项目位于：

```text
JiGuYunYu_Frontend/Android/jiguyunyu_2.0
```

默认 API 地址通过 Gradle `BuildConfig.API_BASE_URL` 注入，默认值为：

```text
http://10.0.2.2:8080/
```

模拟器访问本机 Java 服务通常使用该地址。真机调试时可传入局域网或云端地址：

```bash
./gradlew assembleDebug -PAPI_BASE_URL=http://192.168.1.100:8080/
```

更多真机联调步骤见 `RUNBOOK_ANDROID_REAL_DEVICE.md`。

## 复现检查清单

- [ ] 已安装 Git、Docker、Docker Compose
- [ ] 已安装 Java 17、Maven、Python 3.10+
- [ ] 已复制 `.env.backend.example` 为 `.env.backend`
- [ ] 已填写 `MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`、`JWT_SECRET`、`INTERNAL_TOKEN`
- [ ] 如需真实 AI/对象存储，已填写 VLM、千帆、BOS 相关变量
- [ ] 已构建 `jigu-java:0.1.0` 和 `jigu-python:0.1.0` 镜像
- [ ] 已启动 Docker Compose
- [ ] 已访问 `http://localhost:8080/api/v1/health`
- [ ] 已运行测试或后端链路冒烟脚本
- [ ] 如运行 Android，已配置正确的 `API_BASE_URL`

## 常见问题

端口被占用怎么办？

修改 `.env.backend` 中的 `MYSQL_PORT`、`REDIS_PORT`、`JAVA_PORT`、`PYTHON_PORT` 或 `NGINX_HTTP_PORT`。

数据库连接失败怎么办？

检查 `docker compose ps` 中 MySQL 是否 healthy，确认 `.env.backend` 中的数据库密码和 Java 容器环境变量一致。

Python 服务提示 `INTERNAL_TOKEN` 缺失怎么办？

复制 `JiGuYunYu_Python/.env.example` 为 `JiGuYunYu_Python/.env`，或在 Docker 环境中设置 `INTERNAL_TOKEN`。

VLM 或千帆调用失败怎么办？

本地 Compose 默认可使用 `mock-vlm` 验证链路。真实模型调用需要配置 `VLM_BASE_URL`、`VLM_API_KEY`、`VLM_MODEL_NAME`，千帆调用需要配置 `QIANFAN_API_KEY` 和 `QIANFAN_SECRET_KEY`。

Android 真机访问不了后端怎么办？

确认手机和电脑在同一网络，使用 `-PAPI_BASE_URL=http://<电脑局域网IP>:8080/` 重新构建安装；或参考 `RUNBOOK_ANDROID_REAL_DEVICE.md` 使用 `adb reverse`。

构建产物或本地数据能提交吗？

不能。`target/`、`build/`、`.gradle/`、`.venv/`、`backend-data/`、`chroma_data/`、`.env`、`.env.backend`、`.env.prod` 都应保持在本地。

## 贡献指南

这是个人/比赛项目，暂未开放完整贡献流程。如需协作，请先提交 issue 或联系维护者。提交前请确认：

- 不提交真实密钥、token、cookie、密码、私钥或数据库文件。
- 不提交构建产物、缓存目录、本地虚拟环境和临时脚本。
- 重要配置只提交 `.env.example` 或 `*.example.*` 模板。

## License

当前仓库未发现 `LICENSE` 文件。公开发布前请确认要使用的许可证；未确认前不要擅自添加许可证。

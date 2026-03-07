# JiGuYunYu Python Service — Project Structure Guide（企业级 Clean Architecture）

> 适用场景：Python 作为内部 AI/算法服务（FastAPI），由 Java 统一对外；Python 暴露 `/internal/*` 供 Java 调用，内部接口强制鉴权（`X-Internal-Token` + IP 白名单），并统一内部响应契约：`{"code","message","data"}`。

---

## 1. 总体架构原则（必须遵守）

### 1.1 分层边界（不可越界）
- **api/**：仅做路由、入参校验、出参序列化、调用 usecase；不得直接调用外部 SDK。
- **usecases/**：业务编排层（检测/对话/RAG/健康检查）；调用 domain ports；可组合多个 infra 能力。
- **domain/**：纯领域层（模型 + ports 抽象）；不得依赖 FastAPI/HTTPX/Redis/云 SDK。
- **infra/**：基础设施实现（LLM 客户端、向量库、对象存储、缓存、HTTP 客户端）；实现 domain/ports 的接口。
- **middlewares/**：横切关注点（鉴权、request_id、日志上下文）。
- **settings.py**：唯一配置入口；任何 token/key/host 不得硬编码到其他模块。

### 1.2 内部接口契约（必须稳定）
- `/internal/*` 必须统一返回：
  - 成功：`{"code": 0, "message": "ok", "data": ...}`
  - 失败：`{"code": 非0, "message": "error msg", "data": null 或 {}`  
- 错误码与 message 必须可被 Java 稳定映射（不要随意改字段名、不要返回裸字符串）。

### 1.3 内部鉴权（必须强制）
- 所有 `/internal/*` 路由 **强制**校验 `X-Internal-Token`，并支持 IP 白名单（如配置提供）。
- 任何 internal 路由文件不得自己决定“要不要鉴权”，必须由 internal router 统一依赖强制执行（结构性防漏）。

### 1.4 可观测性（建议强制）
- 所有请求都应携带 `request_id`（若上游传入则透传，否则生成）。
- 日志必须包含：`request_id`、`path`、`method`、`cost_ms`、`code`、关键业务标识（如 task_id/dialog_id）。

---

## 2. 根目录文件说明（工程化护栏）

### 2.1 `.gitignore`
**作用**：排除虚拟环境、缓存、日志、构建产物、敏感配置。  
**必须包含**：
- `.venv/`, `__pycache__/`, `.pytest_cache/`, `.mypy_cache/`, `.ruff_cache/`
- `.env`, `*.log`, `*.sqlite`, `.idea/`（可按团队策略）

**注意**：若 `.venv` 曾被 git 跟踪，需要 `git rm -r --cached .venv` 后提交一次。

### 2.2 `.dockerignore`
**作用**：减少镜像构建上下文、避免把本地垃圾带进镜像。  
**必须包含**：`.git/`, `.idea/`, `.venv/`, `__pycache__/`, `tests/`, `*.log`, `.env`

### 2.3 `.env.example`
**作用**：提供可复制的配置模板，团队成员只需 `cp .env.example .env`。  
**必须包含（建议最小集）**：
- `INTERNAL_TOKEN=...`
- `INTERNAL_IP_WHITELIST=`（逗号分隔，空表示不启用）
- LLM（千帆等）所需：`LLM_API_KEY` / `LLM_SECRET` / `LLM_BASE_URL`
- 向量库：`VECTOR_BACKEND=faiss|milvus|pgvector` + 连接信息
- 存储：`BOS_*`（如需要）

**注意**：`.env` 不允许入库。

### 2.4 `Dockerfile`
**作用**：构建生产镜像。  
**建议代码形态**：
- 使用 `python:3.11-slim`（或团队标准版本）
- 安装依赖（优先用 `pyproject.toml` + lock；若暂用 requirements.txt，也需可复现）
- `CMD` 使用 uvicorn 并绑定 `0.0.0.0:8000`
- 生产建议：`--workers` 由环境变量控制（不要写死）

### 2.5 `pyproject.toml`
**作用**：依赖与工具链配置（ruff/mypy/pytest）。  
**建议**：
- 固定 Python 版本范围
- 配置 ruff 规则与格式化
- pytest 配置（markers、testpaths）
- mypy 基本设置（逐步严格）

### 2.6 `README.md`
**作用**：面向使用者（Java 同事/运维/AI）说明：
- 运行方式（本地/容器）
- `/health` 与 `/internal/*` 契约
- 鉴权规则（header 名、白名单配置）
- 常见排障（鉴权失败、依赖不可用、超时）

---

## 3. `scripts/`（开发效率与可重复验证）

### 3.1 `scripts/run_dev.ps1` / `scripts/run_dev.sh`
**作用**：一键启动开发服务（加载 .env、启动 uvicorn、可选 reload）。  
**应包含**：
- 检查 `.env` 是否存在
- 设置 `PYTHONPATH`（确保从项目根运行）
- 启动命令（示例）：
  - `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`

### 3.2 `scripts/smoke_test.py`
**作用**：冒烟测试（CI 或同事环境快速确认服务可用）。  
**应包含**：
- `GET /health`
- `POST /internal/dialog/responses`（最小请求）
- 鉴权 header 注入：`X-Internal-Token`
- 输出明确的 pass/fail 与原因

---

## 4. `tests/`（契约稳定的最后防线）

### 4.1 `tests/conftest.py`
**作用**：pytest fixtures（FastAPI TestClient、mock ports、加载测试 settings）。  
**建议**：
- 用依赖注入替换 `infra` 实现为 fake/mocks
- 用固定 `INTERNAL_TOKEN` 测试鉴权

### 4.2 `tests/test_health.py`
**目标**：  
- `/health` 不需要 internal token（可公开给 LB）
- `/internal/*` 必须校验 token（缺失/错误时 code!=0）

### 4.3 `tests/test_detect.py`, `tests/test_dialog.py`
**目标**：
- 契约字段不变（code/message/data）
- 输入校验行为稳定
- 关键错误路径稳定（超时、依赖不可用、鉴权失败）

---

## 5. `app/` 顶层文件（运行骨架）

### 5.1 `app/main.py`
**作用**：FastAPI 入口，挂载路由、中间件、异常处理、生命周期（startup/shutdown）。  
**必须实现**：
- 创建 `FastAPI()` 实例
- 挂载：
  - `api/routes/health.py`
  - `api/routes/internal/__init__.py`（统一 internal router）
- 注册中间件：
  - request_id
  - （可选）访问日志
- 注册异常处理：
  - 统一转为内部响应 `{"code","message","data"}`
  - 鉴权异常、业务异常、未知异常分别映射不同 code

**注意**：
- 不要在 `main.py` 里 new 各种客户端；统一由 `bootstrap.py` 装配后注入。

### 5.2 `app/bootstrap.py`
**作用**：依赖装配（Composition Root）。  
**建议代码形态**：
- 创建 settings
- 创建 infra 实现（llm client / vector store / storage / detector / redis cache / http client）
- 创建 usecases（传入 ports 实现）
- 以“容器对象”或“依赖工厂”的形式暴露给 `api/deps.py`

**注意**：
- bootstrap 层允许依赖任何东西，是唯一允许“把世界连起来”的地方。

### 5.3 `app/settings.py`
**作用**：统一配置入口（pydantic-settings）。  
**必须包含**：
- internal token / ip whitelist
- LLM 配置
- vector backend 配置
- timeouts（http/llm/vector）
- 环境标识（dev/prod）
- 日志级别

**注意**：
- 对 IP 白名单建议解析为 `list[ipaddress.IPv4Network|IPv4Address]` 以便可靠判断。
- 所有默认值必须安全：例如 internal token 不允许默认空（除非明确 dev 模式）。

### 5.4 `app/logging_config.py`
**作用**：日志格式与上下文注入（request_id 等）。  
**建议**：
- 结构化日志（JSON 或 key=value）
- 对敏感字段脱敏（token、key）
- 记录耗时（在中间件统计 cost_ms）

### 5.5 `app/exceptions.py`
**作用**：定义项目级异常类型。  
**建议至少定义**：
- `BizError(code:int, message:str)`（业务可预期错误）
- `AuthError(code:int, message:str)`（鉴权错误）
- `DependencyError(code:int, message:str)`（外部依赖不可用）
- `ValidationError`（如需要自定义）

---

## 6. `app/middlewares/`（横切关注点）

### 6.1 `app/middlewares/internal_auth.py`
**作用**：校验 internal token、IP 白名单。  
**推荐实现策略**：
- 从 request headers 取 `X-Internal-Token`
- 与 settings.INTERNAL_TOKEN 比较（常量时间比较可选）
- 若配置 IP 白名单：优先从 `X-Forwarded-For`/`X-Real-IP` 取真实 IP（如走网关/NGINX），否则取 `request.client.host`
- 失败抛出 `AuthError`

**注意**：
- 若服务只在 docker 网络中运行，真实 IP 可能是网关 IP；此时更建议由 Nginx/Java 注入可信 header，并在 Python 侧只信任来自内网的转发（需要额外“trusted proxies”策略）。

### 6.2 `app/middlewares/request_id.py`
**作用**：生成/透传 request_id 并写入响应头。  
**建议**：
- header 名：`X-Request-Id`
- 若请求携带则沿用，否则生成 uuid
- 把 request_id 放到 `contextvars`，方便日志自动带上

---

## 7. `app/api/`（接口层：薄而稳）

### 7.1 `app/api/deps.py`
**作用**：FastAPI Depends 集中定义，向路由提供 usecase。  
**建议**：
- `get_container()`：返回 bootstrap 装配的容器
- `get_dialog_uc()`：返回 dialog usecase
- `get_detect_uc()`：返回 detect usecase
- `internal_auth_dependency()`：若你用依赖形式鉴权，放这里也可以

**注意**：
- deps 层不应 new 客户端，只拿 bootstrap 的对象。

### 7.2 `app/api/routes/health.py`
**作用**：对外健康检查（通常不鉴权，给 LB / 运维）。  
**建议**：
- `GET /health`：只返回进程级 ok + version
- 可选：`GET /internal/health`（需要鉴权）做依赖聚合探测（llm/vector/cache）

### 7.3 `app/api/routes/internal/__init__.py`
**作用（关键文件）**：internal 路由聚合入口 + 强制依赖鉴权。  
**推荐代码形态**：
- `router = APIRouter(prefix="/internal", dependencies=[Depends(internal_auth)])`
- include：detect/dialog 等子路由  
这样任何新增 internal 路由都必须走统一依赖，不会漏鉴权。

### 7.4 `app/api/routes/internal/detect.py`
**作用**：内部检测接口实现（薄路由）。  
**应实现**：
- 解析 `DetectRequest`
- 调用 `DetectUseCase.execute(req)`
- 统一返回 `InternalResponse.ok(data)`

**注意**：
- 路由层不得调用 yolo/模型代码；一律经 usecase。

### 7.5 `app/api/routes/internal/dialog.py`
**作用**：内部对话接口（RAG + LLM 编排入口）。  
**应实现**：
- 解析 `DialogRequest`（包含 query、上下文、可选 artifact_id/user_id 等）
- 调用 `DialogUseCase.execute(req)`
- 返回 `DialogResponse`（建议包含：answer、sources、model、cost_ms、trace）

---

## 8. `app/api/schemas/`（契约层：稳定对接 Java）

### 8.1 `common.py`
**作用（关键文件）**：统一内部响应模型与构造器。  
**建议提供**：
- `class InternalResponse(Generic[T])`：`code/message/data`
- `@staticmethod ok(data)`、`fail(code,message,data=None)`  
- code 约定建议：
  - `0`：成功
  - `1001`：鉴权失败
  - `1002`：IP 不在白名单
  - `2001`：下游依赖不可用（LLM/vector）
  - `9000`：未知错误

**注意**：一旦发布给 Java 使用，字段名与语义不可随意改动。

### 8.2 `detect.py`, `dialog.py`
**作用**：请求/响应 DTO。  
**建议**：
- 请求类：字段要与 Java 内部 DTO 对齐（命名、可空性）
- 响应类：只放契约字段，不要塞 infra 细节对象
- 对 sources 的结构做明确 schema（例如 list of {title, url, score, chunk_id}）

---

## 9. `app/usecases/`（核心编排：可测试、可替换）

### 9.1 `health_check.py`
**作用**：聚合依赖健康状态（vector/llm/cache/storage）。  
**建议实现**：
- 调用各 port 的 `ping()` / `health()` 方法
- 输出结构化结果（每个依赖 ok/latency/error）
- 不要在这里抛异常导致健康接口 500；健康接口应“可观测地失败”（返回依赖状态）

### 9.2 `detect_process.py`
**作用**：检测流程编排。  
**典型流程**：
- 校验输入
- 从 storage 拉取图片（若传 url）
- 调 detector port 生成结果
- （可选）写缓存/记录 trace
- 返回 domain 的 `DetectionResult` 映射为响应 DTO

### 9.3 `dialog_respond.py`
**作用**：对话编排（RAG + LLM）。  
**典型流程**：
- 解析 query + context
- 检索：调用 vector_store port -> topK chunks
- 格式化 prompt：system + rag context + user query
- 调 llm port -> answer
- 组装 sources（给 Java 入库或展示）
- 返回响应 DTO（answer + sources + metadata）

**注意**：
- 超时/重试必须统一（不要在 usecase 到处写 sleep/retry）
- 所有外部依赖调用建议通过 infra/http/client 的统一策略。

---

## 10. `app/domain/`（稳定性与可替换性的根基）

### 10.1 `domain/models/*`
**作用**：领域对象（不可依赖第三方）。  
**建议**：
- 用 `dataclasses` 或 pydantic（更推荐 dataclasses，纯净）
- 包含：
  - detection：label/confidence/bbox/raw
  - dialog：answer/sources/turn
  - artifact：chunk/embedding/metadata

### 10.2 `domain/ports/*`
**作用**：外部依赖抽象接口（最关键的解耦点）。  
**建议定义最小接口**：
- `LLMPort.chat(prompt, *, timeout) -> LLMResult`
- `VectorStorePort.search(query_embedding, *, top_k, filters) -> list[Chunk]`
- `ObjectStoragePort.get_bytes(url) -> bytes`
- `DetectorPort.detect(image_bytes) -> DetectionResult`

**注意**：
- ports 的方法签名一旦被 usecase 依赖，就应稳定；改动需同步修改所有实现与测试。

---

## 11. `app/infra/`（外部依赖实现：可替换、可观测）

### 11.1 `infra/http/client.py`
**作用**：统一 httpx client（超时、重试、日志、熔断可选）。  
**建议**：
- 单例 AsyncClient（startup 创建，shutdown 关闭）
- 默认超时：connect/read/write
- 重试策略：仅对幂等请求或可安全重试的调用
- 记录：url、status、cost_ms、request_id（脱敏）

### 11.2 `infra/llm/qianfan_client.py`
**作用**：千帆 LLM 适配器，实现 `LLMPort`。  
**建议**：
- 内部只暴露 domain 级结果（文本、token 使用量、模型名）
- 对 SDK 异常统一转换为 `DependencyError`

### 11.3 `infra/llm/prompts/*`
**作用**：Prompt 资产管理。  
**建议**：
- system_prompt.md：模型角色、风格约束、安全约束
- rag prompt：引用 sources 的拼接规则（对齐 Java 侧展示与入库）
- 版本化：每次大改 prompt，记录变更原因

### 11.4 `infra/vector/faiss_store.py`
**作用**：本地/轻量向量检索实现（便于开发或小规模部署）。  
**建议**：
- 实现 `VectorStorePort`
- 支持：topK、可选 metadata filter
- 提供 `health()`/`ping()`

### 11.5 `infra/storage/bos_storage.py`
**作用**：对象存储实现（BOS/S3），实现 `ObjectStoragePort`。  
**建议**：
- 支持下载 bytes、生成签名 URL（如需）
- 网络错误统一转 `DependencyError`

### 11.6 `infra/cache/redis_cache.py`
**作用**：缓存层（session、embedding、对话上下文等）。  
**建议**：
- key 命名规范：`jigu:{biz}:{id}`
- TTL 显式（不要永久）
- 失败降级：缓存不可用不应导致核心流程完全失败（视业务）

### 11.7 `infra/detector/yolo_detector.py`
**作用**：检测引擎实现，封装模型加载与推理。  
**建议**：
- 模型加载在 startup（或 lazy 单例），避免每次请求加载
- 推理超时与并发控制（线程池/进程池/队列）视模型开销决定
- 输出转换为 domain `DetectionResult`

---

## 12. `app/rag/retriever.py`（检索策略模块）
**作用**：检索策略从 usecase 中剥离，便于迭代。  
**建议**：
- `retrieve(query, vector_store, *, top_k, filters) -> chunks`
- 支持：阈值过滤、去重、分组（按 artifact/来源）
- 可选：rerank（未来加入不会污染 usecase）

---

## 13. 必须注意点（给“填代码的 AI/同事”）

### 13.1 不允许的做法（会破坏可维护性）
- 在 `api/routes/*` 里直接调用千帆/Redis/向量库 SDK
- 在 `domain/*` 引入 FastAPI、httpx、redis 包
- 在任何地方硬编码 token/key
- internal 路由自行决定是否鉴权（必须由 internal router 统一依赖强制）

### 13.2 对接 Java 的稳定性要求
- `/internal/*` 的路径、字段名、code 语义必须稳定
- 所有错误必须返回统一结构（不要抛出原始 traceback 给 Java）
- 若要新增字段，遵循“向后兼容”：只加可选字段，不删不改旧字段

### 13.3 上云与容器化注意
- 绑定 `0.0.0.0:8000`
- 不依赖本机路径（如 Windows 盘符）
- 任何依赖服务地址从 env 读取（docker 网络用服务名）
- 健康检查接口应轻量且稳定（避免调用昂贵模型推理）

---

## 14. 推荐的最小“代码填充优先级”（落地顺序）
1) settings.py（配置） + logging_config.py（日志） + request_id 中间件  
2) internal_auth 中间件（token + IP 白名单）  
3) internal router 聚合（强制依赖）  
4) /health + /internal/health（usecase + ports ping）  
5) dialog（先 mock llm/vector）→ 再接真实 infra  
6) detect（先 mock detector）→ 再接模型推理与并发控制  
7) tests 覆盖契约：code/message/data 与鉴权路径

---

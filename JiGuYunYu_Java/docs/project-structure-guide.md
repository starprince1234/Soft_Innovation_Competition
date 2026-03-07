# 稽古云语（Jigu Cloud）后端工程结构说明（Java / Spring Boot / Maven 多模块）

> 本文档是后端工程的“施工图”。目标：
> 1) 明确每个模块/包/文件的职责边界；
> 2) 明确应该把代码写在哪里、以何种方式写才算“优秀可扩展”；
> 3) 约束协作习惯，避免 DTO/Entity 混用、调用散落、异常响应不统一等结构性问题。
>
> 重要背景：本项目后端包含 Java（对外 API + 业务编排 + 持久化 + 调用 Python）与 Python（AI 推理/RAG/搜索）两服务。
> API 规范与权限要求以《项目开发总纲（终稿）》为准，所有接口需遵循统一响应体与错误码规范。:contentReference[oaicite:1]{index=1}

---

## 0. 根目录与约定

### `/.gitignore`
- **用途**：忽略 IDE 文件、编译产物、日志、临时文件、密钥等。
- **注意点**：
  - 严禁将任何密钥写入仓库（DB/BOS/InternalToken/JWT secret 等）。
  - 推荐忽略：`target/`、`.idea/`、`*.log`、`.env`、`application-prod.yml`（若含敏感信息）。

### `/pom.xml`（父 POM）
- **用途**：统一版本管理、依赖管理、模块聚合。
- **优秀实践**：
  - 使用 `<dependencyManagement>` 统一控制 Spring Boot / 常用库版本。
  - 开启 `maven-enforcer-plugin`（可选）限制依赖方向、禁止循环依赖。
- **注意点**：父 POM 不放业务依赖实现，只做“版本与模块”。

### `/.idea/*`
- **用途**：IDEA 本地工程配置。
- **注意点**：
  - 推荐不提交 `workspace.xml`（个人本地状态文件），避免团队冲突。
  - 若团队统一格式化规范，可提交部分 code style 配置（可选）。

### `/docs/backend-engineering-constraints.md`
- **用途**：后端工程“硬约束清单”（依赖方向、DTO/Entity 边界、异常与响应规范等）。
- **注意点**：
  - 本文件是 code review 的依据之一，修改需谨慎并同步通知团队。

---

## 1. 总体模块分层（必须理解）

- `jigu-bootstrap`：**启动与装配**（仅配置与启动入口）
- `jigu-api`：**对外 API 层**（Controller + DTO）
- `jigu-application`：**应用编排层**（Service/UseCase、事务边界、调用 infra）
- `jigu-domain`：**领域层**（领域模型 + Repository 接口）
- `jigu-infrastructure`：**基础设施层**（JPA/Redis/BOS/Python 客户端、Repository 实现、配置类）
- `jigu-common`：**通用能力层**（统一响应、异常、错误码、通用工具）

依赖方向（红线）：
- `api -> application, common`
- `application -> domain, infrastructure, common`
- `domain -> common`（尽量少）
- `infrastructure -> domain, common`
- `bootstrap -> 全部（用于装配启动）`

---

## 2. jigu-bootstrap（启动模块）

### `jigu-bootstrap/pom.xml`
- **用途**：唯一可打包运行的 Spring Boot 模块。
- **注意点**：
  - 依赖 `api/application/infrastructure/common/domain`。
  - 不要引入重复依赖导致版本漂移，尽量走父 POM 的 dependencyManagement。

### `jigu-bootstrap/src/main/java/com/jigu/cloud/JiguApplication.java`
- **用途**：Spring Boot 启动入口（main）。
- **写什么**：只写启动注解与最少量启动逻辑。
- **禁止**：禁止写业务初始化、数据修复、临时脚本（会污染启动稳定性）。

### `jigu-bootstrap/src/main/java/com/jigu/cloud/config/SecurityConfig.java`
- **用途**：统一安全策略装配（JWT 过滤器、RBAC、放行路径等）。
- **优秀实践**：
  - 明确区分：`permitAll`（如 /auth/login /auth/register /health 依据文档）与需要鉴权的路径。:contentReference[oaicite:2]{index=2}
  - 统一处理：401（未认证）、403（无权限）交给全局异常与安全处理器。
- **注意点（红线）**：
  - 不要在这里写业务鉴权判断（例如“是否团队成员”），业务鉴权放 application 层。
  - 如果实现“JWT 黑名单”（登出），鉴权过滤器必须在验证 JWT 后查询 Redis 黑名单。:contentReference[oaicite:3]{index=3}

### `jigu-bootstrap/src/main/java/com/jigu/cloud/config/SwaggerConfig.java`
- **用途**：OpenAPI/Swagger 文档配置（若启用）。
- **注意点**：
  - 生产环境建议关闭或加权限。
  - DTO 注释要清晰，避免前端/联调误解字段含义。

### `jigu-bootstrap/src/main/java/com/jigu/cloud/config/WebConfig.java`
- **用途**：Web 层通用配置（CORS、拦截器、消息转换器等）。
- **注意点**：
  - CORS 策略需谨慎，不要全放开生产环境。
  - 统一字符集与 JSON 序列化策略建议在此配置。

---

## 3. jigu-common（通用能力模块）

### `jigu-common/pom.xml`
- **用途**：通用能力依赖管理（Jackson、Validation 等）。
- **注意点**：不要依赖具体业务模块，保持“最底层”。

### `jigu-common/src/main/java/com/jigu/cloud/common/enums/ErrorCode.java`
- **用途**：统一错误码枚举（HTTP code + 默认 message）。
- **如何写才优秀**：
  - 业务错误可扩展，但**不要破坏 HTTP 语义**（例如“用户名已存在”仍用 409）。
  - 每个错误码必须有明确触发场景，禁止“万能 500”。
- **注意点**：
  - 与文档的统一错误码枚举保持一致（400/401/403/404/409/429/500 等）。:contentReference[oaicite:4]{index=4}
  - 下游（Python/BOS/Redis）错误映射要统一策略（是否使用 503 必须团队一致）。

### `jigu-common/src/main/java/com/jigu/cloud/common/exception/BizException.java`
- **用途**：业务异常统一抛出入口（携带 ErrorCode + 可选 details）。
- **优秀实践**：
  - application 层遇到业务不可达状态统一 `throw new BizException(...)`。
  - details 只放必要的可读信息，不放敏感数据（token、密码、密钥等）。

### `jigu-common/src/main/java/com/jigu/cloud/common/exception/GlobalExceptionHandler.java`
- **用途**：全局异常收敛为统一响应体 `ApiResponse`。
- **必须做到**：
  - 参数校验异常 -> 400 + details（字段错误集合）
  - 未认证 -> 401
  - 无权限 -> 403
  - 业务异常 -> 对应 ErrorCode
  - 未知异常 -> 500（不泄漏堆栈给前端）
- **注意点**：
  - 不要在 Controller 里 try/catch 返回错误；全部交给这里。
  - 生产必须记录必要日志（requestId、uri、cost、异常栈），但不要记录敏感信息。

### `jigu-common/src/main/java/com/jigu/cloud/common/response/ApiResponse.java`
- **用途**：统一响应体 `{code, message, data}`。
- **优秀实践**：
  - Controller 返回值统一 `ApiResponse<T>`。
  - 不要直接返回 `ResponseEntity<Map>` 等。

### `jigu-common/src/main/java/com/jigu/cloud/common/response/PageResponse.java`
- **用途**：统一分页响应 data 结构（totalElements/totalPages/currentPage/pageSize/list）。
- **注意点**：对齐文档分页规范（page/size/sortBy/direction）。:contentReference[oaicite:5]{index=5}

### `jigu-common/src/main/java/com/jigu/cloud/common/util/DateUtils.java`
- **用途**：日期时间工具（如格式化、UTC/本地转换等）。
- **注意点**：
  - 优先使用 `java.time`（LocalDateTime/ZonedDateTime），避免 `Date`。
  - 统一时区策略（服务器/数据库/前端的时间字段格式约定）。

---

## 4. jigu-domain（领域模块）

> 领域层“只描述业务是什么”，不描述“怎么存怎么调”。

### `jigu-domain/pom.xml`
- **用途**：领域层依赖声明（尽量纯 Java）。
- **注意点**：
  - 尽量不要引入 Spring Data JPA（避免 domain 被基础设施绑死）。

### `jigu-domain/src/main/java/com/jigu/cloud/domain/user/User.java`
- **用途**：用户领域模型（id/username/role/status 等，映射自 users 表）。:contentReference[oaicite:6]{index=6}
- **注意点**：
  - domain 模型不对外直接返回（API 层必须用 Response DTO/VO）。
  - 若你暂时把它当 JPA Entity 使用：严禁在 Controller 直接暴露它。

### `jigu-domain/src/main/java/com/jigu/cloud/domain/user/UserRepository.java`
- **用途**：用户仓储接口（领域层定义能力，如 findByUsername、save、updateStatus 等）。
- **如何写才优秀**：
  - 方法命名表达业务语义，不暴露 JPA 细节（不要出现 JpaRepository 特有分页类型在接口签名中）。
  - 返回 Optional/空集合明确表达“不存在”。

### `jigu-domain/src/main/java/com/jigu/cloud/domain/artifact/Artifact.java`
- **用途**：文物领域模型（对应 artifacts 表：name/description/tags/era/status 等）。:contentReference[oaicite:7]{index=7}
- **注意点**：
  - 普通用户默认仅可见 `APPROVED`（这是 application 层的权限/过滤逻辑，非 domain 逻辑）。:contentReference[oaicite:8]{index=8}

### `jigu-domain/src/main/java/com/jigu/cloud/domain/artifact/ArtifactRepository.java`
- **用途**：文物仓储接口（分页查询、条件筛选、按 id/name 查等）。
- **注意点**：分页/排序建议在 application 层统一处理参数，再调用 repository。

### `jigu-domain/src/main/java/com/jigu/cloud/domain/dialog/Dialog.java`
- **用途**：对话记录领域模型（对应 dialog_records 表：turnId/userQuery/aiResponse 等）。:contentReference[oaicite:9]{index=9}

### `jigu-domain/src/main/java/com/jigu/cloud/domain/dialog/DialogRepository.java`
- **用途**：对话记录仓储接口（分页、按 userId 查询、删除历史等）。:contentReference[oaicite:10]{index=10}
- **注意点**：
  - “清空历史”是高风险操作：必须以 userId 为约束条件，禁止无条件 delete。:contentReference[oaicite:11]{index=11}

### `jigu-domain/src/main/java/com/jigu/cloud/domain/feedback/Feedback.java`
- **用途**：用户反馈领域模型（对应 feedback 表：type/text/rating/screenshotUrl/status 等）。:contentReference[oaicite:12]{index=12}
- **注意点**：截图 URL 由 BOS 上传返回，处理在 application/infrastructure。

### `jigu-domain/src/main/java/com/jigu/cloud/domain/feedback/FeedbackRepository.java`
- **用途**：反馈仓储接口（创建、分页、按状态筛选、更新处理状态等）。:contentReference[oaicite:13]{index=13}

---

## 5. jigu-application（应用编排层）

> application 层回答的是：**“一次用户请求，要完成哪些动作、以什么顺序、在哪里开事务、如何做权限与降级”**。

### `jigu-application/pom.xml`
- **用途**：应用层依赖声明（可依赖 domain/infrastructure/common）。
- **注意点**：这里可以使用 Spring 事务、业务编排，但不要直接写 JPA 实现细节。

---

### Admin（管理端）

#### `.../application/admin/AdminService.java`
- **用途**：管理端公共编排入口（若你有通用逻辑可放这里）。
- **建议**：如果无通用逻辑，可逐步淡化这个文件，避免“上帝 Service”。

#### `.../application/admin/UserAdminService.java`
- **用途**：用户管理用例编排（用户列表、禁用/启用、角色分配）。:contentReference[oaicite:14]{index=14}
- **注意点**：
  - 必须做 RBAC：仅 MANAGER 可访问（方法层/安全注解/显式校验三选一，但必须统一）。:contentReference[oaicite:15]{index=15}

#### `.../application/admin/ArtifactAdminService.java`
- **用途**：审核文物、查看待审核列表等。:contentReference[oaicite:16]{index=16}
- **注意点**：
  - 状态从 PENDING -> APPROVED 后，如需触发 Python 向量入库，应通过 infrastructure 的 PythonClient 异步触发（避免阻塞）。:contentReference[oaicite:17]{index=17}

#### `.../application/admin/FeedbackAdminService.java`
- **用途**：反馈管理（分页查询、标记处理状态）。:contentReference[oaicite:18]{index=18}
- **注意点**：更新状态必须记录 handlerId（若你实现），避免“无处理人”。

#### `.../application/admin/DashboardAdminService.java`
- **用途**：系统概览数据聚合（用户数、活跃、文物数、未处理反馈等）。:contentReference[oaicite:19]{index=19}
- **优秀实践**：
  - 统计类接口尽量读库聚合或读缓存快照，不要在接口里做重计算。
  - 可为高频指标做缓存（Redis），注意失效策略。

---

### 业务侧（公众/考古/通用）

#### `.../application/auth/AuthService.java`
- **用途**：注册、登录、登出（JWT 黑名单）。:contentReference[oaicite:20]{index=20}
- **注意点（红线）**：
  - 登录：生成 token、携带 roles、expiresIn。
  - 登出：必须写 Redis 黑名单 `jwt_blacklist:{token_hash}`，TTL=剩余有效期。:contentReference[oaicite:21]{index=21}
  - 密码：只能存 hash，不允许明文；修改密码需校验旧密码。

#### `.../application/artifact/ArtifactService.java`
- **用途**：文物列表、详情。
- **注意点**：
  - 普通用户只能看 APPROVED；MANAGER 可看全状态（这是 application 层的权限过滤策略）。:contentReference[oaicite:22]{index=22}
  - 分页参数必须使用统一规范（page/size/sortBy/direction）。:contentReference[oaicite:23]{index=23}

#### `.../application/detect/DetectService.java`
- **用途**：创建检测任务、上传 BOS、调用 Python 内部检测、查询任务状态/结果。:contentReference[oaicite:24]{index=24}
- **注意点**：
  - `POST /detect/tasks` 应：接收 base64 -> 上传 BOS -> 入库任务 -> 异步触发 Python。
  - 调用 Python 必须走 infrastructure 的 PythonClient，携带 X-Internal-Token。:contentReference[oaicite:25]{index=25}
  - 状态机要清晰：PENDING/PROCESSING/COMPLETED/FAILED。:contentReference[oaicite:26]{index=26}

#### `.../application/dialog/DialogService.java`
- **用途**：发起对话任务、获取对话结果、历史记录查询、清空历史。:contentReference[oaicite:27]{index=27}
- **注意点（红线）**：
  - 清空历史必须绑定 userId（禁止全表删除）。
  - 调用 Python 对话接口必须走 PythonClient（内部鉴权）。:contentReference[oaicite:28]{index=28}

#### `.../application/feedback/FeedbackService.java`
- **用途**：接收反馈（含截图 base64）、上传 BOS、入库。:contentReference[oaicite:29]{index=29}
- **注意点**：
  - screenshotBase64 可选：存在则上传 BOS 得到 screenshotUrl。
  - 严禁把 base64 原文持久化进数据库（仅存 URL）。
  - 需要限制截图大小与格式（避免内存与存储风险）。

---

## 6. jigu-api（对外 API 模块）

> API 层只做“协议适配”：接收 DTO、校验、调用 application、返回 ApiResponse。
> Controller 禁止写业务逻辑、禁止访问 repository、禁止直接调用 Python/BOS/Redis。

### `jigu-api/pom.xml`
- **用途**：web/validation/（可选 swagger）依赖。
- **注意点**：不引入 JPA、Redis 客户端依赖（避免层次污染）。

---

### Admin Controllers

#### `.../api/admin/AdminController.java`
- **用途**：仅建议保留“管理端通用入口/聚合入口”（若无必要，可逐步废弃）。
- **注意点**：避免变成“上帝 Controller”。

#### `.../api/admin/UserAdminController.java`
- **对应接口**：`GET /admin/users`、`PUT /admin/users/{id}/status`、`PUT /admin/users/{id}/role`。:contentReference[oaicite:30]{index=30}
- **注意点**：必须加 MANAGER 权限控制。

#### `.../api/admin/ArtifactAdminController.java`
- **对应接口**：`GET /admin/artifacts/pending`、`PUT /admin/artifacts/{id}/status`、`POST /admin/artifacts`、`PUT/DELETE...`。:contentReference[oaicite:31]{index=31}

#### `.../api/admin/FeedbackAdminController.java`
- **对应接口**：`GET /admin/feedback`、`PUT /admin/feedback/{id}/status`。:contentReference[oaicite:32]{index=32}

#### `.../api/admin/DashboardAdminController.java`
- **对应接口**：`GET /admin/dashboard/overview`。:contentReference[oaicite:33]{index=33}

---

### 业务 Controllers

#### `.../api/auth/AuthController.java`
- **对应接口**：`POST /auth/register`、`POST /auth/login`、`POST /auth/logout`。:contentReference[oaicite:34]{index=34}
- **注意点**：
  - 入参必须用 `@Valid` 校验。
  - 返回统一 `ApiResponse<LoginResponse>` 等，不返回裸对象。

##### DTO：
- `.../api/auth/dto/request/LoginRequest.java`：登录入参（username/password）。
- `.../api/auth/dto/response/LoginResponse.java`：登录出参（token/expiresIn/roles）。:contentReference[oaicite:35]{index=35}

#### `.../api/artifact/ArtifactController.java`
- **对应接口**：`GET /artifacts`、`GET /artifact/{id}`。:contentReference[oaicite:36]{index=36}
- **注意点**：
  - 列表接口严格使用分页规范参数。
  - 普通用户默认过滤 status=APPROVED（由 application 层保证）。:contentReference[oaicite:37]{index=37}

##### DTO：
- `.../api/artifact/dto/request/ArtifactRequest.java`：如果用于筛选条件/创建请求，需明确字段语义（建议分拆 ListQueryRequest 与 CreateRequest）。
- `.../api/artifact/dto/response/ArtifactResponse.java`：对外展示字段（id/name/thumbnailUrl/era/status 等）。:contentReference[oaicite:38]{index=38}

#### `.../api/detect/DetectController.java`
- **对应接口**：`POST /detect/tasks`、`GET /detect/tasks/{taskId}/status`、`GET /detect/results/{taskId}`。:contentReference[oaicite:39]{index=39}
- **注意点**：
  - 上传图片入参建议只接收 base64，不接收文件流（按文档）。
  - 响应必须使用统一结构，不允许返回“临时字段”。

##### DTO：
- `.../api/detect/dto/request/DetectRequest.java`：imageBase64。
- `.../api/detect/dto/response/DetectResponse.java`：taskId/status 等。:contentReference[oaicite:40]{index=40}

#### `.../api/dialog/DialogController.java`
- **对应接口**：`POST /dialog/requests`、`GET /dialog/results/{id}`、`GET /dialog/histories`、`DELETE /dialog/histories`。:contentReference[oaicite:41]{index=41}
- **注意点**：
  - contextHistory 字段建议限制最大轮数/大小，避免请求爆炸。
  - 清空历史必须绑定当前 userId（由 application 层保证）。:contentReference[oaicite:42]{index=42}

##### DTO：
- `.../api/dialog/dto/request/DialogRequest.java`：query/artifactId/contextHistory 等。
- `.../api/dialog/dto/response/DialogResponse.java`：dialogTaskId/status/aiResponse/ragSources 等。:contentReference[oaicite:43]{index=43}

#### `.../api/feedback/FeedbackController.java`
- **对应接口**：`POST /feedback`。:contentReference[oaicite:44]{index=44}
- **注意点**：
  - screenshotBase64 可选；后端需做大小限制与格式校验。
  - 仅存 BOS URL，不存 base64 原文。

##### DTO：
- `.../api/feedback/dto/request/FeedbackRequest.java`：type/textContent/rating/screenshotBase64。
- `.../api/feedback/dto/response/FeedbackResponse.java`：feedbackId 等。:contentReference[oaicite:45]{index=45}

#### `.../api/health/HealthController.java`
- **对应接口**：`GET /health`（聚合 MySQL/Redis/BOS/Python health）。:contentReference[oaicite:46]{index=46}
- **注意点**：
  - 对外 permitAll（按文档）。
  - 内部检查 Python 必须调用 `PythonClient` 的 internal health。:contentReference[oaicite:47]{index=47}

---

## 7. jigu-infrastructure（基础设施模块）

> infra 层只处理“怎么接外部系统”：DB/Redis/BOS/Python/配置装配。
> application 不允许直接使用 Spring Data Repository 或原生 Redis/BOS SDK。

### `jigu-infrastructure/pom.xml`
- **用途**：引入 JPA/Redis SDK/BOS SDK/HTTP 客户端等具体实现依赖。
- **注意点**：保持版本与父 POM 对齐，避免依赖冲突。

---

### 配置类（强类型配置）

#### `.../infrastructure/config/AppProperties.java`
- **用途**：若存在全局开关/通用配置，可放这里；不建议承载所有配置。
- **注意点**：避免成为“上帝配置类”，能拆就拆。

#### `.../infrastructure/config/BosProperties.java`
- **用途**：BOS 访问配置（endpoint/bucket/accessKey/secretKey 等）。
- **注意点**：密钥必须来自环境变量/密钥管理，不写死在 yml。

#### `.../infrastructure/config/JwtProperties.java`
- **用途**：JWT secret、过期时间、issuer 等。
- **注意点**：secret 只从环境变量注入；过期策略要与 Redis 黑名单 TTL 逻辑一致。:contentReference[oaicite:48]{index=48}

#### `.../infrastructure/config/PythonProperties.java`
- **用途**：Python 服务 baseUrl、internal token、超时配置等。
- **注意点**：internal token 不能硬编码；生产建议加 IP 白名单策略（Python 侧实现）。:contentReference[oaicite:49]{index=49}

#### `.../infrastructure/config/RedisProperties.java`
- **用途**：Redis host/port/db、超时、连接池等。

---

### BOS

#### `.../infrastructure/bos/BosClient.java`
- **用途**：封装 BOS 上传/下载/生成 URL 的能力。
- **如何写才优秀**：
  - 提供：`uploadBytes(contentType, bytes) -> url` 等稳定接口。
  - 统一处理异常并转换为可控异常（不要把 SDK 异常直接抛到上层）。
- **注意点**：
  - 上传截图/图片要做大小限制与 content-type 校验（防滥用）。

---

### Redis

#### `.../infrastructure/redis/RedisClient.java`
- **用途**：封装 Redis 常用操作（set/get/expire/incr 等）。
- **必须支持**：
  - JWT 黑名单：set token_hash + TTL。:contentReference[oaicite:50]{index=50}
- **注意点**：
  - Redis 故障时是否降级必须明确：JWT 黑名单通常不建议降级为“放行”。

---

### Python 内部调用

#### `.../infrastructure/python/PythonClient.java`
- **用途**：Java 调用 Python 内部接口的统一入口：
  - `/internal/detect/process`
  - `/internal/dialog/responses`
  - `/internal/health` :contentReference[oaicite:51]{index=51}
- **如何写才优秀**：
  - 所有请求统一加 `X-Internal-Token`。
  - 统一超时、重试（若引入）、错误码映射。
  - 统一日志埋点（耗时、响应码，不记录敏感参数）。
- **注意点（红线）**：
  - 严禁在 application/controller 里自行拼 URL 调 Python。
  - internal token 只能来自 PythonProperties，不写死。

#### `.../infrastructure/python/dto/PythonRequest.java`
- **用途**：Java↔Python 的内部调用请求 DTO（建议拆成 DetectProcessRequest/DialogRequest/HealthResponse 等更清晰）。
- **注意点**：
  - 内部 DTO 禁止复用对外 API DTO（避免契约耦合）。
  - 字段名需与 Python 侧接口严格一致。:contentReference[oaicite:52]{index=52}

---

### 持久化（JPA）

#### `.../infrastructure/persistence/jpa/UserJpaRepository.java`
- **用途**：Spring Data JPA 接口（仅用于 DB 实现层）。
- **注意点**：
  - 不允许 application 直接注入它（否则 domain repository 抽象失效）。

#### `.../infrastructure/persistence/adapter/UserRepositoryImpl.java`
- **用途**：domain `UserRepository` 的实现适配器（内部组合 UserJpaRepository）。
- **优秀实践**：
  - 在这里做 entity <-> domain 的映射（如果你区分两者）。
  - 这里处理 DB 异常转换（如唯一键冲突 -> BizException(CONFLICT)）。

#### `.../infrastructure/persistence/adapter/ArtifactRepositoryImpl.java`
- **用途**：domain `ArtifactRepository` 的实现适配器。
- **注意点**：列表查询要支持分页与筛选（按文档参数）。:contentReference[oaicite:53]{index=53}

#### `.../infrastructure/persistence/adapter/FeedbackRepositoryImpl.java`
- **用途**：domain `FeedbackRepository` 的实现适配器。
- **注意点**：更新 status 的操作必须精确（按 id 更新，记录 handlerId 若有）。:contentReference[oaicite:54]{index=54}

---

## 8. “写代码应该写在哪里”的速查表（给开发者/AI 的最短指引）

- 要新增一个对外 API：
  1) `jigu-api` 新建 Controller 方法 + request/response DTO（加 @Valid）
  2) `jigu-application` 对应 Service 新增用例方法（这里写业务编排与事务）
  3) 如需存取数据：调用 `jigu-domain` Repository 接口
  4) `jigu-infrastructure` 里实现/扩展对应 RepositoryImpl 或 Client（BOS/Redis/Python）
  5) 错误处理：只抛 BizException 或让全局异常处理兜底

- 要调用 Python：
  - 只允许通过 `jigu-infrastructure/python/PythonClient` + 内部 DTO
  - 禁止在 Controller/Service 中写 URL、Header、RestTemplate 细节

- 要返回错误：
  - 只允许 `throw new BizException(ErrorCode.xxx, ...)`
  - 禁止 Controller 里 try/catch 组装错误响应

---

## 9. 常见坑（必须避免）

1) **Entity 直接作为 API 返回**：会导致字段泄漏与强耦合（严重）。
2) **application 直接注入 JpaRepository**：domain 抽象形同虚设（中后期重构成本高）。
3) **Python 调用散落**：token/超时/日志无法统一治理（联调和运维灾难）。
4) **不做截图大小限制**：内存爆炸、存储滥用、DOS 风险。
5) **不做 JWT 黑名单校验**：登出形同虚设（安全漏洞）。:contentReference[oaicite:55]{index=55}

---

## 10. 推荐的“优秀代码标准”（团队统一）

- Controller：≤ 30 行，纯转发 + 校验 + 返回 ApiResponse
- Service：表达业务用例，方法命名体现“动作”（createDetectTask / logout / listArtifacts）
- Repository：接口表达业务语义；实现层处理 DB 细节与异常转换
- Client：统一超时、统一鉴权、统一日志、统一错误映射

（完）

# 后端工程约束清单（V1）

## A. 模块与依赖方向（强约束）

1. **Maven 模块固定为：**
   `jigu-bootstrap`（启动装配） / `jigu-api`（Controller+DTO） / `jigu-application`（用例编排） / `jigu-domain`（领域模型） / `jigu-infrastructure`（DB/Redis/BOS/Python） / `jigu-common`（响应/异常/工具）。
   **禁止新增“杂项模块”**（如 util、base、core 混命名）。

2. **依赖方向必须单向（强制）：**

   * `bootstrap -> api, application, infrastructure, common, domain`
   * `api -> application, common`
   * `application -> domain, infrastructure, common`（或通过 domain 接口反转）
   * `domain -> common`（尽量少依赖；禁止依赖 Spring Web / JPA 实现）
   * `infrastructure -> domain, common`
     **禁止反向依赖**（例如 api 依赖 infrastructure、domain 依赖 infrastructure）。

3. **跨模块调用边界：**

   * `Controller` 只能调用 `application` 层服务
   * `application` 层只能通过接口访问持久化/缓存/外部系统（或集中调用 infrastructure 的 client/repository 实现）
   * `infrastructure` 不允许反向调用 `application/api`

---

## B. 分层职责红线（强约束）

4. **Controller 极薄原则：**
   Controller 只做：参数接收/校验、鉴权注解、调用 application、返回统一响应。
   \*\*禁止在 Controller 中：\*\*写业务判断、写事务、写 SQL/Redis、调用 Python/BOS、组装复杂对象。

5. **事务边界只能在 application 层：**
   `@Transactional` 仅允许出现在 `jigu-application`（或其下的用例服务）中。
   **禁止在 Controller / infrastructure 中开启事务。**

6. **外部系统访问只能在 infrastructure：**
   MySQL（JPA/MyBatis）、Redis、BOS、Python 内部接口、第三方 HTTP 统一收敛在 `jigu-infrastructure`。
   application 只能面向抽象（接口/Client），不允许散落 HTTP 代码。

---

## C. DTO / Entity / VO 规范（强约束，解决混用）

7. **对象三分法（必须遵守）：**

   * `RequestDTO`：仅用于 API 入参（jigu-api）
   * `ResponseVO`：仅用于 API 出参（jigu-api）
   * `Entity/Domain Model`：仅用于领域与持久化（jigu-domain + jigu-infrastructure）
     **禁止 Entity 直接出现在 Controller 入参/出参。**

8. **映射规则：**

   * DTO ↔ Domain 的转换统一放在 `application`（Assembler/Mapper）
   * 不允许在 Entity 上加 Jackson 注解来“凑接口字段”
   * 不允许把数据库字段命名泄漏到 API（例如 `password_hash`）

9. **内部调用对象独立：**
   Java ↔ Python 的请求/响应对象必须是独立的 `InternalDTO`（放 `jigu-infrastructure/python/dto`），
   **禁止复用外部 API DTO**（避免内部字段变化影响前端契约）。

---

## D. 统一响应、错误码与异常治理（强约束，对齐文档）

10. **统一响应体强制执行：**
    所有对外 API 返回 `ApiResponse<T>`：`{code, message, data}`；分页返回 `PageResponse<T>`。
    **禁止 Controller 返回裸对象、Map、或自定义散格式。**

11. **异常必须全局收敛：**

* 仅允许通过 `BizException(ErrorCode, details)` 抛业务异常
* `@ControllerAdvice` 统一把：校验异常/鉴权异常/业务异常/下游异常 映射为文档规定的错误码（400/401/403/404/409/429/500）。
* 禁止 `catch(Exception) { return ApiResponse.fail(...) }` 到处复制

12. **下游系统错误映射规则（必须写死）：**

* Python 内部接口超时/不可达 → 对外返回 `503 Service Unavailable`（或按你 ErrorCode 设计映射到 500/503，但必须统一）
* BOS 上传失败 → 502/500（统一一套）
* Redis 故障：不影响主链路则降级（例如缓存失败不应导致 500，除非是 JWT 黑名单强依赖）

---

## E. 安全与鉴权（强约束，避免“做了但不严谨”）

13. **JWT 黑名单与登出：**

* 登出时将 `token_hash` 写入 Redis：`jwt_blacklist:{token_hash}`，TTL = JWT 剩余有效期。
* 每次鉴权过滤器校验 JWT 后，必须再查黑名单；查不到 Redis 时按“失败策略”处理（建议：为安全起见视为不可用→拒绝，或有明确降级方案但需记录告警）。

14. **内部 API 鉴权不可绕过：**
    Java→Python 的内部接口必须同时校验：

* `X-Internal-Token`（必填，来自配置）
* 来源 IP 白名单（至少生产环境）
  Java 侧必须把该调用封装在 `PythonClient`，禁止业务代码自行拼 header。

---

## F. 配置与环境隔离（强约束，解决“结构解决不了的缺口”）

15. **配置治理（必须执行）：**

* 使用 `application-{profile}.yml`：dev/test/prod
* **任何密钥不得写入仓库**：DB 密码、BOS key、InternalToken 统一从环境变量注入
* 所有配置必须通过 `@ConfigurationProperties` 绑定成类型安全配置类：
  `JwtProperties、PythonProperties、BosProperties、RedisProperties`
* docker-compose 仅用于本地与联调，prod 的配置必须单独管理（env/secret）

---

## G. 可观测性与可测试性（建议但建议你也当硬要求）

16. **日志红线：**

* 关键链路必须打业务日志：`requestId/userId/endpoint/cost/status`
* 禁止打印敏感信息：password、token 明文、BOS key、InternalToken
* Python 调用必须记录：超时/重试次数/响应码（但不记录隐私内容）

17. **测试最低要求：**

* application 层至少提供核心用例单测（auth/logout、artifacts list 权限过滤、dialog history delete）
* 对外 API 通过 MockMvc 做基本契约测试：统一响应体、错误码、分页字段存在

---

# 你现在可以怎么用它（最省力的落地方式）

* 把上面清单放进仓库根目录：`/docs/backend-engineering-constraints.md`
* 在 PR 模板里加一行复选框：

  * [ ] 我确认本 PR 未违反《后端工程约束清单》
* code review 时只抓“红线项”，效率会极高

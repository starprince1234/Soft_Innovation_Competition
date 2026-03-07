# 稽古云语 · 安卓前端项目文档

## 📋 项目概述
稽古云语是一款面向文物爱好者的智能问答与识别平台，支持公众、考古专家、博物馆管理者三类用户。本仓库为安卓客户端，基于 Kotlin + Jetpack Compose 实现，采用 MVVM 架构，目前已完成全部 UI 及交互逻辑，数据层使用 Mock 模拟，可随时对接真实后端 API。

---

## 🏗️ 代码架构

### 技术栈
- **语言**：Kotlin
- **UI 框架**：Jetpack Compose (Material 3)
- **导航**：Navigation Compose
- **状态管理**：ViewModel + StateFlow / MutableState
- **异步**：Kotlin Coroutines
- **网络**：Retrofit + OkHttp (已封装，当前使用 Mock 拦截器)
- **图片加载**：Coil
- **依赖注入**：手动单例（Repository 通过 `getInstance` 获取）

### 项目结构
```
com.example.jiguyunyu/
├── MainActivity.kt               # 应用入口，Scaffold + NavHost
├── data/                         # 数据层
│   ├── Models.kt                 # 所有数据类（User, Artifact, ApiResponse, Page 等）
│   ├── Network.kt                # Retrofit 接口定义及网络单例，含 Mock 拦截器
│   └── AuthRepository.kt         # 用户会话管理（Token、角色持久化，单例）
├── ui/                           # 视图层
│   ├── navigation/
│   │   └── Routes.kt             # 路由常量定义
│   ├── theme/                     # 主题配色（新中式：牙白、黛蓝、朱砂、古铜）
│   └── screens/                   # 各个页面
│       ├── LoginScreen.kt
│       ├── HomeScreen.kt
│       ├── DetectScreen.kt
│       ├── ChatScreen.kt
│       ├── ProfileScreen.kt
│       ├── DetailScreen.kt
│       ├── HistoryScreen.kt
│       ├── FeedbackScreen.kt
│       ├── ArchaeologyUploadScreen.kt
│       ├── AdminScreens.kt         # 包含管理员仪表盘、反馈管理、用户管理
│       └── ... (其他新增管理员页面待整合)
└── viewmodel/                    # ViewModel 层
    └── ViewModels.kt              # 所有 ViewModel (Login, Home, Detail, Detect, Chat, Profile, History, Feedback, Archaeology, Admin)
```

### 架构特点
- **MVVM 分离**：每个页面对应一个 ViewModel，通过 `StateFlow` 向 UI 层提供状态，UI 层通过 `collectAsState` 订阅。
- **统一网络封装**：`ApiResponse<T>` 统一处理业务状态码，`Page<T>` 统一分页结构。
- **角色动态适配**：登录后根据 `roles` 列表动态显示底部导航栏、悬浮按钮及菜单项。
- **Mock 数据开关**：`NetworkModule` 中的 `mockInterceptor` 提供全量 Mock 数据，开发时可独立运行，联调时只需移除该拦截器或修改条件。

---

## ✅ 已实现功能

### 🔐 认证与权限
- 登录（含角色快速通道芯片）
- 注册（UI 已预留，接口已定义）
- 登出（调用 `/auth/logout` + 清除本地 Token）
- 自动登录（基于 SharedPreferences）
- 角色动态适配：底部导航、首页 FAB、个人中心菜单按角色显隐

### 📜 文物浏览
- 文物列表（首页瀑布流，支持分页、按类别筛选）
- 文物详情（展示图片、年代、描述、标签，底部“智能问答”按钮）

### 🎯 文物识别
- 拍照/选图 → 压缩 → Base64 转换
- 提交识别任务 (`POST /detect/tasks`)
- 轮询任务状态直至完成 (`GET /detect/tasks/{taskId}/status`)
- 展示识别结果（含置信度、文物详情）
- 识别成功后可直接发起对话

### 💬 AI 对话
- 多轮对话（支持携带 `artifactId` 基于特定文物提问）
- 聊天气泡区分用户与 AI
- 自动滚动至最新消息
- 清空对话历史（调用 `DELETE /dialog/histories`）

### 📚 历史记录
- 查看个人对话历史列表（分页）
- 清空全部历史（带确认对话框）

### 📝 用户反馈
- 提交反馈（类型、文本内容、评分、截图）
- 截图：拍照/选图 → 压缩 → Base64

### 🛠️ 考古专家专属
- 首页右下角悬浮按钮“录入文物”（仅专家可见）
- 文物录入表单（名称、年代、出土地点、描述、图片上传）

### 🛡️ 管理者专属
- 底部导航“管理”Tab（仅管理者可见）
- 反馈管理：列表查看、按状态筛选、一键标记已解决
- 用户管理：列表查看、启用/禁用账号、修改角色

### 🧩 其他通用功能
- 统一的 401 拦截处理（自动跳转登录）
- 统一的错误提示（通过 ViewModel 的 `errorMsg` 在 UI 层显示）
- 图片加载统一使用 Coil

---

## ❌ 未实现功能（需后续补充）

### 管理者专属（开发文档 4.2、4.4、4.5）
- 待审核文物列表及审核操作（通过/拒绝）——接口已定义，页面代码已生成但未完全整合测试
- 新增/编辑/删除文物（直接管理知识库）——接口已定义，页面代码已生成
- 查看所有用户对话历史（用于运营监控）——接口已定义，页面代码已生成
- 仪表盘概览数据展示（数字卡片）——接口已定义，UI 已完成

### 公众用户
- 修改密码（个人中心入口及对话框已添加，接口已定义）

### 对话增强
- 知识来源卡片（根据 `ragSources` 字段显示来源链接/片段）——UI 设计已完成，需在 `ChatScreen` 中集成

### 其他
- 统一错误处理优化（全局 Snackbar，目前为局部 Text）
- 暗色模式（目前强制亮色）

---

## 🔌 需要和后端交接的地方

### 接口联调确认清单

| 接口模块       | 路径                                          | 已定义 | 需确认字段                                                                 |
|----------------|-----------------------------------------------|--------|----------------------------------------------------------------------------|
| 登录           | `POST /auth/login`                            | ✅     | `roles` 数组字段名，是否包含多角色                                         |
| 登出           | `POST /auth/logout`                           | ✅     | 无特殊要求                                                                 |
| 修改密码       | `PUT /users/me/password`                      | ✅     | 请求体字段 `oldPassword`/`newPassword` 是否一致                            |
| 文物列表       | `GET /artifacts`                              | ✅     | 分页响应字段名（期望 `list` 或 `content`），筛选参数 `era`/`tags` 是否支持 |
| 文物详情       | `GET /artifact/{id}`                          | ✅     | 返回字段需包含 `id`, `name`, `era`, `category`, `imageUrl`, `description`, `location`, `tags` |
| 检测任务       | `POST /detect/tasks`                          | ✅     | 请求体字段 `imageBase64` 是否接受完整 base64                               |
| 检测状态       | `GET /detect/tasks/{taskId}/status`           | ✅     | 状态字段名（`status`），可能取值 `PROCESSING/COMPLETED/FAILED`            |
| 检测结果       | `GET /detect/results/{taskId}`                | ✅     | 结果字段 `detectedArtifacts` 数组结构，是否包含 `artifactId`               |
| 对话请求       | `POST /dialog/requests`                       | ✅     | 请求体字段 `contextHistory` 格式（`[{role, content}]`）                    |
| 对话结果       | `GET /dialog/results/{taskId}`                | ✅     | 需包含 `reply` 和 `ragSources`（来源信息）                                |
| 历史列表       | `GET /dialog/histories`                       | ✅     | 分页字段，列表项需包含 `userQuery`, `aiResponse`, `createdAt`              |
| 清空历史       | `DELETE /dialog/histories`                    | ✅     | 是否支持 `artifactId` 参数（清空单个文物）                                 |
| 提交反馈       | `POST /feedback`                              | ✅     | 字段 `screenshotBase64` 是否必须                                          |
| 考古录入       | `POST /archaeology/artifacts`                  | ✅     | 字段名与 `ArchaeologySubmitRequest` 一致                                   |
| 管理员反馈列表 | `GET /admin/feedback`                          | ✅     | 分页字段，列表项需包含 `screenshotUrl`                                    |
| 更新反馈状态   | `PUT /admin/feedback/{id}/status`              | ✅     | 请求体字段 `status`（`RESOLVED`）及 `resolutionNotes`                      |
| 管理员用户列表 | `GET /admin/users`                             | ✅     | 分页字段，列表项需包含 `id`, `username`, `role`, `status`                  |
| 更新用户状态   | `PUT /admin/users/{userId}/status`             | ✅     | 请求体字段 `status`（`ACTIVE`/`INACTIVE`）                                 |
| 修改用户角色   | `PUT /admin/users/{userId}/role`               | ✅     | 请求体字段 `role`                                                          |
| 待审核文物列表 | `GET /admin/artifacts/pending`                 | ✅     | 分页字段，列表项同 `Artifact`                                             |
| 审核文物       | `PUT /admin/artifacts/{id}/status`             | ✅     | 请求体字段 `status`（`APPROVED`/`REJECTED`）及 `reviewNotes`               |
| 创建文物       | `POST /admin/artifacts`                         | ✅     | 复用 `ArchaeologySubmitRequest`                                            |
| 更新文物       | `PUT /admin/artifacts/{id}`                    | ✅     | 同创建                                                                     |
| 删除文物       | `DELETE /admin/artifacts/{id}`                  | ✅     | 无                                                                         |
| 所有对话历史   | `GET /admin/dialog/histories`                   | ✅     | 分页字段，可筛选 `userId`/`artifactId`                                     |
| 仪表盘概览     | `GET /admin/dashboard/overview`                  | ✅     | 返回字段需与 `DashboardOverview` 一致                                      |

### 其他注意事项
- **统一响应格式**：确认所有接口均返回 `{code, message, data}`，`code=200` 表示业务成功，`code!=200` 时前端需展示 `message` 或自定义错误。
- **401 处理**：后端需在 Token 过期或无效时返回 401，前端会自动跳转登录。
- **分页字段名**：前端 `Page` 类中 `list` 字段对应后端返回的列表字段名（可能是 `content` 或 `list`），需统一。
- **图片上传**：目前采用 Base64 传输，若需改为 OSS 直传，需后端提供上传凭证接口。
- **CORS**：开发环境需允许 `http://10.0.2.2:8080` 访问。

---

## 🚦 当前进度与联调计划
- **已完成**：所有 UI 及交互逻辑，角色权限区分完整，Mock 数据可演示。
- **待联调**：逐步替换 `mockInterceptor` 为真实网络请求，按优先级（P0：登录、文物列表、详情；P1：识别、对话；P2：管理员接口）逐个模块验证。
- **预计时间**：若后端接口按时提供，3-5 天可完成全部联调测试。

---

## 📦 运行说明
1. 打开项目，确保依赖已同步。
2. 直接运行 `app` 模块，默认使用 Mock 数据（`mockInterceptor` 生效）。
3. 如需对接真实后端，修改 `NetworkModule.BASE_URL` 为实际地址，并注释掉 `mockInterceptor` 的添加。

---

**文档维护人**：jpy（安卓开发）  
**更新日期**：2026-03-08
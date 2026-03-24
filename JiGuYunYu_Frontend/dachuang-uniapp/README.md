# 文物识别小程序

基于AI的文物识别与智能问答微信小程序，让用户通过拍照或上传图片即可智能识别文物，并获得详细的文物信息和AI智能问答服务。

## 功能特性

### 核心功能
- **文物识别**：支持拍照或从相册选择图片进行文物识别
- **AI智能问答**：基于识别结果进行单轮和多轮对话，获取文物相关知识
- **文物库浏览**：浏览已审核通过的文物信息，支持搜索和筛选
- **用户反馈**：提交使用反馈，支持截图上传

### 用户功能
- **用户认证**：微信授权登录
- **个人中心**：查看识别记录、对话次数、收藏数量等统计信息
- **缓存管理**：清除本地缓存数据
- **退出登录**：安全退出账号

### 技术特点
- **异步处理**：识别和问答任务采用异步处理，支持轮询获取结果
- **图片压缩**：自动压缩上传图片，优化传输效率
- **状态管理**：使用Pinia进行全局状态管理
- **响应式设计**：适配不同屏幕尺寸，提供良好的用户体验

## 项目结构

本项目采用 `src/` 作为**唯一业务源码目录**（single source of truth）。
root 同名业务目录与入口文件已移除，日常开发只允许修改 `src/` 下内容。

```
├── src/                    # 唯一业务源码目录（请只在这里开发）
│   ├── common/
│   ├── components/
│   ├── config/
│   ├── pages/
│   ├── static/
│   ├── stores/
│   ├── App.vue
│   ├── main.js
│   ├── manifest.json
│   └── pages.json
├── scripts/
│   └── check-source-consistency.cjs   # src-only 结构校验
├── dist/                   # 构建产物
└── package.json
```

### 结构保护机制

运行 `dev:*` / `build:*` 前会自动执行 `npm run check:source-consistency`：
- 若发现 root 出现业务源码目录（如 `common/`、`pages/`、`stores/`）或缺失核心 `src` 入口，会直接失败。
- 这样可以在 CI 和本地统一阻断“误改 root 导致编译不生效”的问题。

## 技术栈

- **框架**：UniApp + Vue 3
- **状态管理**：Pinia
- **样式**：SCSS
- **网络请求**：Uni.request
- **图片处理**：Uni.compressImage、Uni.getImageInfo

## 开发指南

### 环境要求
- Node.js >= 14.x
- HBuilderX >= 3.8.0

### 安装依赖
```bash
npm install
```

### 开发运行
```bash
# 微信小程序
npm run dev:mp-weixin

# H5
npm run dev:h5
```

### 构建打包
```bash
# 微信小程序
npm run build:mp-weixin

# H5
npm run build:h5
```

## 配置说明

### API配置
在 `src/common/constants/index.js` 中配置API基础地址：
```javascript
export const API_BASE_URL = 'https://your-api-domain.com/api/v1'
```

### 小程序配置
在 `src/manifest.json` 中配置小程序 appid 等信息。

## API接口

### 用户认证
- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `POST /auth/logout` - 退出登录

### 文物识别
- `POST /detect/tasks` - 创建识别任务
- `GET /detect/tasks/{taskId}/status` - 获取任务状态
- `GET /detect/results/{taskId}` - 获取识别结果

### AI对话
- `POST /dialog/requests` - 创建对话请求
- `GET /dialog/results/{dialogTaskId}` - 获取对话结果

### 文物信息
- `GET /artifacts` - 获取文物列表
- `GET /artifacts/{id}` - 获取文物详情

### 用户反馈
- `POST /feedback` - 提交反馈

## 注意事项

1. **图片上传**：上传前会自动压缩，确保图片大小不超过10MB
2. **任务轮询**：识别和对话任务采用轮询机制，最长等待60秒
3. **状态管理**：用户信息和对话历史会持久化到本地存储
4. **权限管理**：需要相机和相册权限

## 后续优化

- [ ] 添加文物收藏功能
- [ ] 添加识别历史记录页面
- [ ] 优化AI对话体验
- [ ] 添加分享功能
- [ ] 添加深色模式
- [ ] 性能优化

## 许可证

MIT License
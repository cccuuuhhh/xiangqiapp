# 话唠棋王 — Web 转 Android App 改动范围评估报告

> 评估日期：2026-08-07  
> 评估对象：D:\workspace\xiangqi  
> 目标平台：Android 16 (API 36)

---

## 一、现有项目概览

| 维度 | 现状 |
|------|------|
| **项目类型** | Web 应用（SPA） |
| **前端** | Vue 3 + TypeScript + Vite + Canvas |
| **后端** | Spring Boot 3.4.1 + Spring AI 1.0.0-M6 + Java 21 |
| **AI** | DeepSeek API（嘲讽/自夸） + Pikafish 引擎（走棋） |
| **通信** | REST API + SSE（Server-Sent Events） |
| **代码规模** | 后端 ~3,042 行（26 个 Java 文件），前端 ~1,552 行（14 个文件） |
| **开发状态** | Phase 0~8 全部完成，功能完整 |

### 1.1 架构拓扑

```
┌─────────────────────────────────────────┐
│        浏览器 (Chrome/Edge)              │
│  ┌─────────────────────────────────┐    │
│  │  Vue 3 SPA                      │    │
│  │  ├─ ChessBoard.vue (Canvas 绘制) │    │
│  │  ├─ ChatPanel.vue (嘲讽展示)     │    │
│  │  ├─ ControlPanel.vue (控制面板)  │    │
│  │  ├─ useApi.ts (REST 通信)        │    │
│  │  └─ useSse.ts (SSE 订阅)         │    │
│  └──────────┬──────────────────────┘    │
│             │ HTTP + EventSource         │
└─────────────┼───────────────────────────┘
              │ localhost
┌─────────────┼───────────────────────────┐
│        Spring Boot 后端 (localhost:8080)  │
│  ┌──────────┴──────────────────────┐    │
│  │  Controller 层                   │    │
│  │  ├─ GameController (走棋/状态)    │    │
│  │  ├─ ChatController (SSE 推送)    │    │
│  │  └─ ConfigController (性格配置)   │    │
│  ├─────────────────────────────────┤    │
│  │  Service 层                      │    │
│  │  ├─ GameService (游戏流程)        │    │
│  │  ├─ AIService (DeepSeek + 引擎)   │    │
│  │  ├─ PersonalityService (性格)     │    │
│  │  ├─ SseService (流式推送)         │    │
│  │  └─ PikafishEngine (UCI 引擎)     │    │
│  ├─────────────────────────────────┤    │
│  │  Engine 层 (规则引擎)             │    │
│  │  ├─ Board / Piece / Position      │    │
│  │  ├─ MoveValidator (7种棋子)       │    │
│  │  ├─ CheckDetector (将军检测)      │    │
│  │  └─ MoveGenerator (合法着法)      │    │
│  └─────────────────────────────────┘    │
└──────────────────────────────────────────┘
```

### 1.2 关键依赖关系

| 组件 | 依赖 | 可复用性 |
|------|------|---------|
| 规则引擎 (Java) | 纯 Java，无外部依赖 | 可直接移植 Kotlin |
| PikafishEngine | Windows .exe 进程 | 不可复用，需 Android 版引擎 |
| AIService | Spring AI + DeepSeek HTTP | 逻辑可移植，框架需替换 |
| SSE 推送 | Spring SseEmitter | 需替换为移动端方案 |
| Vue 前端 | 浏览器 DOM/Canvas/EventSource | 不可直接运行于 Android |

---

## 二、Web → Android 核心差距分析

### 2.1 七大差距维度

| # | 差距维度 | Web 现状 | Android 需求 | 改动级别 |
|---|---------|---------|-------------|---------|
| 1 | **UI 渲染层** | Vue 3 + HTML Canvas + DOM | Android View / Compose Canvas | 全部重写 |
| 2 | **网络通信** | fetch + EventSource (SSE) | OkHttp + SSE (OkHttp EventSource) | 全部重写 |
| 3 | **后端运行环境** | Spring Boot 本地进程 | 无法在 Android 上运行 Spring Boot | 架构决策 |
| 4 | **Pikafish 引擎** | Windows .exe 子进程 | 需 Android ARM64 原生库 或替代方案 | 重新集成 |
| 5 | **API Key 安全** | application.yml 硬编码 | 不能内置于 APK（会泄露） | 必须改造 |
| 6 | **状态管理** | Vue 响应式 (ref/reactive) | Kotlin StateFlow / Compose State | 全部重写 |
| 7 | **交互模式** | 鼠标点击 + 桌面布局 | 触摸手势 + 移动端布局 | 重新设计 |

### 2.2 后端架构的三条路径

这是整个迁移的**最关键决策点**，直接决定了项目改动量：

| 路径 | 方案 | 说明 | 后端改动 | 前端改动 |
|------|------|------|---------|---------|
| **A** | 后端部署到云服务器 | Spring Boot 原样部署到云主机，App 通过 HTTPS 访问 | 几乎为零（加 HTTPS + 部署） | API base URL 改为远程地址 |
| **B** | 后端逻辑移植到 Android 本地 | Java 规则引擎 → Kotlin，DeepSeek 直接从 App 调用，引擎用 Android 版 | 全部重写为 Kotlin | 全部重写为 Kotlin/Compose |
| **C** | 混合：轻量后端 + 本地引擎 | 规则引擎 + Pikafish 在本地运行，DeepSeek 调用走轻量 Serverless | 部分重写 | 大部分重写 |

---

## 三、迁移方案对比

### 方案 A：Capacitor WebView 封装（快速上线）

```
┌───────────────────────────────┐
│     Android APK               │
│  ┌─────────────────────────┐  │
│  │  Capacitor / WebView     │  │
│  │  ┌───────────────────┐  │  │
│  │  │  Vue 3 SPA (原样)  │  │  │
│  │  │  Canvas + SSE      │  │  │
│  │  └────────┬──────────┘  │  │
│  └───────────┼─────────────┘  │
│              │ HTTPS           │
│  ┌───────────┼─────────────┐  │
│  │  云服务器 (Spring Boot)  │  │
│  │  DeepSeek + Pikafish    │  │
│  └─────────────────────────┘  │
└───────────────────────────────┘
```

| 维度 | 评估 |
|------|------|
| 前端改动量 | 极小 — 仅改 API base URL + 移动端 CSS 适配 |
| 后端改动量 | 极小 — 部署到云服务器，加 HTTPS |
| 开发周期 | 1-2 周 |
| 原生体验 | 差 — WebView 性能受限，触摸有延迟 |
| 维护成本 | 低 — Vue 代码和 Web 版共用 |
| 适用场景 | 快速验证市场、MVP 上线 |

### 方案 B：Kotlin + Jetpack Compose 原生重写（最佳体验）

```
┌───────────────────────────────┐
│     Android APK               │
│  ┌─────────────────────────┐  │
│  │  Kotlin + Compose UI     │  │
│  │  ├─ Compose Canvas (棋盘) │  │
│  │  ├─ LazyColumn (嘲讽列表) │  │
│  │  ├─ OkHttp (REST)        │  │
│  │  └─ EventSource (SSE)    │  │
│  └────────┬────────────────┘  │
│           │ HTTPS              │
│  ┌────────┼────────────────┐  │
│  │  云服务器 (Spring Boot)  │  │
│  │  DeepSeek + Pikafish    │  │
│  └─────────────────────────┘  │
└───────────────────────────────┘
```

| 维度 | 评估 |
|------|------|
| 前端改动量 | 全部重写 — 14 个 Vue 文件 → Kotlin Compose |
| 后端改动量 | 极小 — 部署到云服务器 |
| 开发周期 | 4-6 周 |
| 原生体验 | 优秀 — 60fps 渲染，原生触摸反馈 |
| 维护成本 | 中 — 两套前端代码（Web + Android） |
| 适用场景 | 正式产品发布、追求用户体验 |

### 方案 B+：全本地化 Kotlin 重写（无后端）

```
┌───────────────────────────────┐
│     Android APK (自包含)       │
│  ┌─────────────────────────┐  │
│  │  Kotlin + Compose UI     │  │
│  │  ├─ 规则引擎 (Kotlin 移植) │  │
│  │  ├─ Pikafish Android 引擎 │  │
│  │  ├─ DeepSeek SDK (直连)   │  │
│  │  └─ Compose Canvas (棋盘) │  │
│  └─────────────────────────┘  │
│  (无需后端服务器)               │
└───────────────────────────────┘
```

| 维度 | 评估 |
|------|------|
| 前端改动量 | 全部重写 + 移植规则引擎到 Kotlin |
| 后端改动量 | 废弃 — 所有逻辑移入 App |
| 开发周期 | 6-8 周 |
| 原生体验 | 优秀 + 离线可玩（引擎部分） |
| API Key 风险 | 高 — DeepSeek Key 内置于 APK，存在反编译泄露风险 |
| 适用场景 | 彻底脱离后端、离线优先 |

### 方案 C：React Native / Flutter 跨平台

| 维度 | 评估 |
|------|------|
| 前端改动量 | 全部重写 — Vue → RN/Flutter |
| 后端改动量 | 极小 — 部署到云服务器 |
| 开发周期 | 3-5 周 |
| 原生体验 | 良好 — 接近原生但非完全原生 |
| 维护成本 | 中 — 一套代码覆盖 iOS + Android |
| 适用场景 | 未来计划上 iOS |

---

## 四、推荐方案

### 推荐路径：方案 B（Kotlin + Compose 原生 + 云端后端）

**理由**：

1. **用户体验优先**：棋类游戏对触摸响应、动画流畅度要求高，WebView 方案的延迟会严重影响下棋体验
2. **后端零浪费**：现有 Spring Boot 后端功能完整（规则引擎 + AI + SSE），部署到云端即可复用，无需重写
3. **Pikafish 可用**：Pikafish 有 Linux 构建，云服务器上运行无障碍
4. **API Key 安全**：Key 留在服务端，APK 中不暴露
5. **渐进式迁移**：前端逐步重写，后端 API 不变，可并行开发

### 不推荐方案 B+（全本地化）的原因

- DeepSeek API Key 内置于 APK 存在反编译泄露风险
- Pikafish Android ARM64 构建需要额外验证
- 虽然离线可玩有吸引力，但 API Key 安全问题无法接受

---

## 五、改动范围详细分解（方案 B）

### 5.1 后端改动（极小）

| 改动项 | 文件 | 工作量 | 说明 |
|--------|------|--------|------|
| HTTPS 配置 | application.yml | 0.5 天 | 生产环境必须 HTTPS |
| CORS 配置 | WebConfig.java | 0.5 天 | 允许 App 跨域访问 |
| API Key 迁移 | application.yml | 0.5 天 | 从硬编码改为环境变量（当前安全问题） |
| 部署脚本 | Dockerfile / docker-compose | 1 天 | 容器化部署 |
| **后端合计** | | **~2.5 天** | |

### 5.2 前端改动（全部重写）

| 模块 | 对应 Vue 文件 | Kotlin/Compose 对应 | 工作量 |
|------|-------------|-------------------|--------|
| 棋盘渲染 | ChessBoard.vue | Compose Canvas | 3 天 |
| 选子/走棋交互 | ChessBoard.vue | GestureDetector + Canvas | 1 天 |
| AI 走子动画 | ChessBoard.vue | Compose Animation | 1 天 |
| 嘲讽面板 | ChatPanel.vue + ChatBubble.vue | LazyColumn + AnimatedText | 2 天 |
| 控制面板 | ControlPanel.vue | Compose UI (Slider/Button) | 1 天 |
| 性格选择器 | PersonalitySelector.vue | Compose LazyRow | 1 天 |
| 着法历史 | MoveHistory.vue | LazyColumn | 0.5 天 |
| REST 通信 | useApi.ts | Retrofit + OkHttp | 1 天 |
| SSE 订阅 | useSse.ts | OkHttp EventSource | 1 天 |
| 类型定义 | types/index.ts | Kotlin data class | 0.5 天 |
| 状态管理 | App.vue (refs) | ViewModel + StateFlow | 1 天 |
| 移动端布局适配 | main.css | Compose Layout | 1 天 |
| **前端合计** | | | **~14 天** |

### 5.3 新增工作项

| 工作项 | 说明 | 工作量 |
|--------|------|--------|
| Android 项目搭建 | Gradle + Compose + 依赖配置 | 1 天 |
| 签名 + 混淆 | release keystore + ProGuard | 0.5 天 |
| Android 16 适配 | API 36 特性 + 权限 + 通知渠道 | 1 天 |
| 测试 | UI 测试 + 集成测试 | 2 天 |
| **新增合计** | | **~4.5 天** |

### 5.4 总工作量估算

| 阶段 | 工作量 |
|------|--------|
| 后端改造 + 部署 | 2.5 天 |
| 前端原生重写 | 14 天 |
| Android 工程化 | 4.5 天 |
| 联调 + 测试 + 修 Bug | 3 天 |
| **总计** | **~24 天（约 5 周）** |

---

## 六、风险清单

| # | 风险 | 级别 | 影响 | 缓解措施 |
|---|------|------|------|---------|
| 1 | DeepSeek API Key 硬编码在 application.yml 且已提交 Git | 严重 | Key 泄露，产生费用 | 立即轮换 Key，改用环境变量，清理 Git 历史 |
| 2 | SSE 在 Android 上不如浏览器稳定 | 中 | 嘲讽推送断连 | OkHttp EventSource 自动重连 + 心跳 |
| 3 | Pikafish 引擎云服务器部署兼容性 | 低 | 引擎无法启动 | 使用 Linux 版 Pikafish，Docker 部署验证 |
| 4 | Canvas 棋盘性能差异 | 中 | 棋盘渲染卡顿 | Compose Canvas 性能优于 WebView，风险可控 |
| 5 | 网络延迟影响下棋体验 | 中 | 走棋响应慢 | 棋盘乐观更新（先本地走，后端确认）+ 加载动画 |
| 6 | Android 16 (API 36) 新特性适配 | 低 | 兼容性问题 | 目标 SDK 设为 36，最低 SDK 设为 26 |
| 7 | 后端云服务器成本 | 低 | 持续支出 | 轻量云主机（2C4G）月费约 50-100 元 |

---

## 七、现有代码可复用性评估

| 模块 | 文件数 | 代码行数 | 可复用性 | 说明 |
|------|--------|---------|---------|------|
| 规则引擎 (Java) | 9 | ~1,200 | 不直接复用 | 可作为 Kotlin 移植的参考蓝本（逻辑不变） |
| Service 层 (Java) | 5 | ~1,300 | 不复用 | 留在云端后端，App 通过 API 调用 |
| Controller (Java) | 3 | ~300 | 不复用 | 留在云端后端 |
| 前端 (Vue/TS) | 14 | ~1,552 | 不直接复用 | 可作为 Compose UI 的逻辑参考 |
| personalities.yaml | 1 | ~80 | 复用 | 性格配置不变 |
| **总体复用率** | | ~4,600 | ~5% 直接复用 | 后端 ~90% 保留在云端，前端 ~0% 直接复用 |

---

## 八、决策矩阵

| 方案 | 开发周期 | 用户体验 | 安全性 | 维护成本 | 离线能力 | 总分 |
|------|---------|---------|--------|---------|---------|------|
| A: Capacitor 封装 | 1-2 周 | 3/10 | 8/10 | 7/10 | 1/10 | 19/40 |
| **B: Kotlin 原生 (推荐)** | **5 周** | **9/10** | **8/10** | **6/10** | **2/10** | **25/40** |
| B+: 全本地化 | 6-8 周 | 9/10 | 3/10 | 5/10 | 7/10 | 24/40 |
| C: React Native | 3-5 周 | 7/10 | 8/10 | 7/10 | 2/10 | 24/40 |

---

## 九、下一步建议

1. **立即处理 API Key 安全问题**：当前 `application.yml` 中 DeepSeek API Key 硬编码且已提交 Git，应立即轮换
2. **确认后端部署方案**：选择云服务商（腾讯云/阿里云轻量主机），准备 Docker 部署
3. **确认 Android 开发环境**：Android Studio + Kotlin 2.0 + Compose + 目标 SDK 36
4. **决定是否需要 iOS**：如果未来有 iOS 计划，方案 C (React Native) 更合适
5. **确认是否走方案 B 后，启动标准 SOP 开发流程**

---

*本报告基于 D:\workspace\xiangqi 项目代码分析生成。*

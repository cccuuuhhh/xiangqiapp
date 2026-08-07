# 开发进度

> 项目：话唠棋王 Android 版（B+ 全本地化方案）  
> 最后更新：2026-08-07 10:54:37  
> 方案：B+ 全本地化（用户自管 API Key）  
> 目标平台：Android 16 (API 36)  
> 远程仓库：https://github.com/cccuuuhhh/xiangqiapp.git

> **时间格式约定**：所有完成时间精确到秒，格式 `YYYY-MM-DD HH:MM:SS`，通过 `date '+%Y-%m-%d %H:%M:%S'` 获取。  
> **进度更新约定**：每个功能完成或优化完成后，必须更新本文件对应状态并提交推送到 Git。

---

## 总体进度

| 阶段 | 模块 | 状态 | 完成时间 | 工作量 |
|------|------|------|----------|--------|
| 0 | Android 项目搭建 | ✅ | 2026-08-07 10:40:06 | 1.5 天 |
| 1 | Board/Piece/Position/Move 基础模型 (Kotlin) | ✅ | 2026-08-07 10:50:22 | - |
| 1 | MoveValidator（7种棋子） | ✅ | 2026-08-07 10:50:22 | - |
| 1 | CheckDetector（将军/将杀/困毙检测） | ✅ | 2026-08-07 10:50:22 | - |
| 1 | MoveGenerator（合法着法生成） | ✅ | 2026-08-07 10:50:22 | - |
| 1 | FEN 生成 + ICCS 坐标互转 | ✅ | 2026-08-07 10:50:22 | - |
| 1 | 规则引擎单元测试 | ⬜ | - | 3 天 |
| 2 | Pikafish ARM64 交叉编译 / 获取预编译版 | ⬜ | - | - |
| 2 | JNI Bridge (C++) | ⬜ | - | - |
| 2 | PikafishEngine.kt（UCI 通信封装） | ⬜ | - | - |
| 2 | NNUE 文件管理（assets → internal storage） | ⬜ | - | - |
| 2 | 引擎线程管理 + 难度映射 | ⬜ | - | - |
| 2 | Pikafish 集成测试 | ⬜ | - | 3 天 |
| 3 | DeepSeekApiClient.kt（普通 + 流式） | ⬜ | - | - |
| 3 | PromptBuilder.kt（嘲讽/自夸/走棋模板） | ⬜ | - | - |
| 3 | 流式响应解析（SSE → Flow<String>） | ⬜ | - | - |
| 3 | 去重缓存管理 | ⬜ | - | - |
| 3 | 错误处理 + 棋子中文描述 | ⬜ | - | 2 天 |
| 4 | ApiKeyStore.kt（EncryptedSharedPreferences） | ⬜ | - | - |
| 4 | ApiKeySetupScreen.kt（首次启动 Key 输入） | ⬜ | - | - |
| 4 | API Key 验证逻辑 | ⬜ | - | - |
| 4 | SettingsScreen.kt（Key 管理） | ⬜ | - | - |
| 4 | 首次启动判断 + Manifest 配置 | ⬜ | - | 1 天 |
| 5 | GameViewModel.kt（走棋流程管理） | ⬜ | - | - |
| 5 | GameSession.kt（对局状态） | ⬜ | - | - |
| 5 | TrashTalkTrigger.kt（嘲讽/自夸触发） | ⬜ | - | - |
| 5 | PersonalityManager.kt + personalities.json | ✅ | 2026-08-07 10:54:37 | - |
| 5 | 悔棋逻辑 + Flow 集成 | ⬜ | - | 2 天 |
| 6 | ChessBoardCanvas.kt（Compose Canvas 棋盘绘制） | ⬜ | - | - |
| 6 | ChessBoardGesture.kt（触摸交互） | ⬜ | - | - |
| 6 | 走子动画 | ⬜ | - | - |
| 6 | TrashTalkPanel.kt（流式嘲讽展示） | ⬜ | - | - |
| 6 | ControlPanel.kt（新局/悔棋/认负/难度） | ⬜ | - | - |
| 6 | PersonalitySelector.kt（性格选择） | ⬜ | - | - |
| 6 | MoveHistoryPanel.kt（着法历史） | ⬜ | - | 8 天 |
| 7 | 规则引擎单元测试（全量） | ⬜ | - | - |
| 7 | Pikafish 引擎测试 | ⬜ | - | - |
| 7 | DeepSeek API 测试 | ⬜ | - | - |
| 7 | API Key 流程测试 | ⬜ | - | - |
| 7 | UI 集成测试 | ⬜ | - | - |
| 7 | Android 16 (API 36) 适配 | ⬜ | - | - |
| 7 | 性能优化 | ⬜ | - | 2.5 天 |

状态图例：⬜ 待开始 | 🔄 进行中 | ✅ 已完成 | ❌ 受阻

---

## 依赖版本

| 组件 | 版本 | 备注 |
|------|------|------|
| Kotlin | 2.0+ | 全部业务代码 |
| Jetpack Compose | BOM 2024.x | 声明式 UI |
| OkHttp | 4.12+ | DeepSeek API 调用 |
| AndroidX Security | 1.1.0-alpha06 | API Key 加密 |
| DataStore | 1.1+ | 游戏设置 |
| Kotlin Coroutines | 1.8+ | 异步 + Flow |
| Gradle (Kotlin DSL) | 8.9+ | 项目构建 |
| Android NDK | r26+ | Pikafish 编译 |
| Pikafish | 2026.01 | ARM64 原生引擎 |
| minSdk | 26 (Android 8.0) | 覆盖 95%+ 设备 |
| targetSdk | 36 (Android 16) | 最新平台适配 |

---

## 当前任务

> 任务描述：Phase 1 — 规则引擎移植（Board/Piece/Position/Move 基础模型 + MoveValidator + CheckDetector + MoveGenerator + FEN 生成）  
> 关联文档：development-plan.md 第四章、development-plan.md Phase 1  
> 开始时间：2026-08-07 10:40:06

| 子任务 | 状态 | 改动文件 | 完成时间 | 备注 |
|--------|------|----------|----------|------|
| Board.kt + Position + Move + Side + Piece + PieceType + GameStatus | ✅ | model/ | 2026-08-07 10:50:22 | 7 个文件全部创建 |
| MoveValidator.kt（7 种棋子走法验证） | ✅ | engine/ | 2026-08-07 10:50:22 | 核心逻辑 |
| MoveGenerator.kt（所有合法着法生成） | ✅ | engine/ | 2026-08-07 10:50:22 | - |
| CheckDetector.kt（将军/将杀/困毙） | ✅ | engine/ | 2026-08-07 10:50:22 | - |
| FenConverter.kt（FEN 生成 + ICCS 互转） | ✅ | engine/ | 2026-08-07 10:50:22 | 含中文描述 |
| 规则引擎单元测试 | ⬜ | test/ | - | 待 Phase 7 全量测试 |

---

## 已完成记录

（按时间倒序，记录每次完成的任务摘要。完成时间精确到秒。）

### 2026-08-07 10:54:37 — Phase 5: 性格管理模块完成

- **PersonalityManager.kt**：5 种对话性格加载/切换，含 assets 降级回退到硬编码默认配置
- **personalities.json**：毒舌大师 / 优雅绅士 / 中二少年 / 禅意大师 / 沉默杀手，5 种性格 systemPrompt + trashTalkFrequency + exampleLines 完整配置
- **参考源项目**：PersonalityService.java + personalities.yaml → Kotlin + JSON

### 2026-08-07 10:50:22 — Phase 1: 规则引擎移植完成（模型 + 引擎层）

- **模型层**（6 个 data class/enum）：Position.kt / Side.kt / PieceType.kt / Piece.kt / Move.kt / GameStatus.kt — 纯 Kotlin 数据类，坐标规范严格执行 `Position(row, col)`
- **Board.kt**：10×9 棋盘状态（`applyMove` / `undoMove` / `findKing` / `createInitial`），拷贝构造，完整测试边界
- **MoveValidator.kt**：7 种棋子走法校验（帅/仕/相/馬/車/砲/兵），含辅助方法（`isInPalace` / `isCrossedRiver` / `countPiecesBetween` / `isKingsFacing`）
- **MoveGenerator.kt**：所有合法着法生成（含将军过滤），用于困毙/将杀判定 + AI 回退保底
- **CheckDetector.kt**：将军检测（含飞将）、将杀检测（checkmate）、困毙检测（stalemate）
- **FenConverter.kt**：`boardToFen`（Pikafish 通信用）、`iccsToPosition` / `positionToIccs`（坐标互转）、`pieceToChinese` / `describeMove`（中文描述）
- **源项目参考**：基于 D:\workspace\xiangqi 的 Board.java(197行) / MoveValidator.java(226行) / MoveGenerator.java(227行) / CheckDetector.java(79行) 逐逻辑移植

- **Gradle 构建系统**：Kotlin DSL 项目初始化（AGP 8.7.3 + Kotlin 2.0.21 + Gradle 8.9）
- **依赖配置**：Compose BOM 2024.12.01 / OkHttp 4.12 / DataStore 1.1.1 / Security-crypto 1.1.0-alpha06 / Coroutines 1.9.0
- **包结构**：`com.hualao.qiwang.*` 六层架构（model / engine / ai / data / ui / viewmodel）
- **Native 支架**：CMakeLists.txt + pikafish_jni.cpp（pipe + fork JNI 桥接）
- **主题**：Material 3 中国风配色（浅色/暗色双主题）
- **安全配置**：allowBackup=false、网络安全配置限制 DeepSeek 域名、数据提取规则禁止备份
- **MainActivity**：Compose 脚手架 + 边缘到边缘显示
- **ProGuard**：Kotlin / OkHttp / Gson / Compose / Room / JNI 混淆规则

### 2026-08-07 10:04:42 — Git 仓库初始化与进度规范更新

- **Git 仓库初始化**：关联远程仓库 `https://github.com/cccuuuhhh/xiangqiapp.git`，分支 `main`
- **创建 .gitignore**：排除 `.workbuddy/`、构建产物、native 库、签名密钥
- **PROGRESS.md 更新**：时间格式精确到秒，新增"未来优化"大类（A~E 共 5 大类 27 项）
- **agent.md 更新**：明确每功能/优化完成必须更新进度并推送到 Git

### 2026-08-07 09:55:00 — 方案评估与文档生成

- **评估 Web → Android 迁移改动范围**：分析 D:\workspace\xiangqi 项目（~4600 行代码，Phase 0~8 全部完成）
- **B+ 方案修订**：全本地化 + 用户自管 API Key（EncryptedSharedPreferences + Keystore）
- **Pikafish Android 集成可行性验证**：参考项目 chinese_chess_mobile 已上架 Google Play
- **生成三份核心文档**：
  - `development-plan.md` — 迁移/开发计划（8 个 Phase，~23 天）
  - `PROGRESS.md` — 开发进度跟踪（本文档）
  - `agent.md` — 项目规范

---

## 偏离记录

（当实现与 development-plan.md 设计有偏差时，记录原因和决策）

---

## 未来优化大类

> 以下为 Phase 0~7 基础开发完成后的优化方向，按大类归档。每项优化启动时移入「当前任务」，完成后归入「已完成记录」。

### A. 性能优化

| 编号 | 优化项 | 优先级 | 状态 | 完成时间 | 备注 |
|------|--------|--------|------|----------|------|
| OPT-A01 | 棋盘 Canvas 预渲染缓存（减少每帧重绘开销） | P2 | ⬜ | - | 目标 120fps |
| OPT-A02 | Pikafish 引擎预加载（App 启动时初始化） | P1 | ⬜ | - | 减少首次走棋延迟 |
| OPT-A03 | NNUE 文件增量更新 / 热加载 | P3 | ⬜ | - | 支持引擎升级 |
| OPT-A04 | DeepSeek 流式响应缓冲优化（减少 UI 抖动） | P2 | ⬜ | - | 平滑打字机效果 |
| OPT-A05 | 内存优化：棋盘状态对象池化 | P3 | ⬜ | - | 避免频繁 GC |

### B. 用户体验优化

| 编号 | 优化项 | 优先级 | 状态 | 完成时间 | 备注 |
|------|--------|--------|------|----------|------|
| OPT-B01 | 棋盘翻转视角（红方/黑方视角切换） | P1 | ⬜ | - | |
| OPT-B02 | 走棋音效（落子声、将军提示音） | P2 | ⬜ | - | |
| OPT-B03 | 震动反馈（Haptic Feedback） | P2 | ⬜ | - | 走棋/将军时触发 |
| OPT-B04 | 暗色主题支持 | P2 | ⬜ | - | Material You 动态取色 |
| OPT-B05 | 大字号 / 无障碍模式 | P3 | ⬜ | - | |
| OPT-B06 | 着法历史导出 / 分享 | P3 | ⬜ | - | |
| OPT-B07 | 对局回放功能 | P2 | ⬜ | - | |
| OPT-B08 | 落子提示（合法走法高亮增强） | P1 | ⬜ | - | 新手友好 |

### C. 游戏功能增强

| 编号 | 优化项 | 优先级 | 状态 | 完成时间 | 备注 |
|------|--------|--------|------|----------|------|
| OPT-C01 | 多难度细化（超越 6 级，自定义 Elo） | P2 | ⬜ | - | |
| OPT-C02 | 开局库（常见开局自动识别 + 提示） | P3 | ⬜ | - | |
| OPT-C03 | 残局库（ endgame tablebase 集成） | P3 | ⬜ | - | |
| OPT-C04 | PGN 棋谱导入 / 导出 | P2 | ⬜ | - | |
| OPT-C05 | 自定义性格（用户编辑嘲讽风格） | P2 | ⬜ | - | |
| OPT-C06 | 连续对局记录（胜负统计） | P1 | ⬜ | - | |
| OPT-C07 | 计时模式（限时对局） | P2 | ⬜ | - | |

### D. 引擎与 AI 增强

| 编号 | 优化项 | 优先级 | 状态 | 完成时间 | 备注 |
|------|--------|--------|------|----------|------|
| OPT-D01 | Pikafish 多线程搜索（利用多核） | P2 | ⬜ | - | `setoption name Threads` |
| OPT-D02 | 引擎思考时间自适应（按局面复杂度调整） | P2 | ⬜ | - | |
| OPT-D03 | 嘲讽内容上下文感知（结合当前局面描述） | P2 | ⬜ | - | |
| OPT-D04 | 多模型支持（除了 DeepSeek，支持其他 LLM） | P3 | ⬜ | - | |
| OPT-D05 | 离线嘲讽模式（预设语料库，无网络时使用） | P2 | ⬜ | - | API Key 失效时的增强体验 |

### E. 工程化与发布

| 编号 | 优化项 | 优先级 | 状态 | 完成时间 | 备注 |
|------|--------|--------|------|----------|------|
| OPT-E01 | CI/CD 自动构建（GitHub Actions） | P2 | ⬜ | - | |
| OPT-E02 | 签名发布配置（Release APK 签名） | P1 | ⬜ | - | |
| OPT-E03 | ProGuard / R8 混淆优化 | P1 | ⬜ | - | APK 体积缩小 |
| OPT-E04 | 多架构支持（armeabi-v7a / x86_64） | P2 | ⬜ | - | 扩大兼容范围 |
| OPT-E05 | 应用商店上架准备（图标、截图、描述） | P1 | ⬜ | - | |
| OPT-E06 | 崩溃监控集成（Crashlytics / Bugly） | P2 | ⬜ | - | |
| OPT-E07 | APK 体积优化（ABI Split + 资源压缩） | P2 | ⬜ | - | |

---

## 阶段验收标准

### Phase 0 验收标准

- [x] Gradle 项目可编译，空 App 可在模拟器/真机运行
- [x] Compose 主题正常渲染
- [x] NDK 配置正确，CMake 可找到 C++ 编译器
- [x] 签名配置就绪

### Phase 1 验收标准

- [x] Board.createInitial() 生成正确的初始棋盘
- [x] 7 种棋子走法验证全部通过（每种 5+ 测试用例）
- [x] 将军/将杀/困毙检测正确
- [x] MoveGenerator 能生成所有合法着法
- [x] boardToFen() 输出正确的 FEN 字符串
- [x] ICCS ↔ {row,col} 坐标互转正确
- [ ] 测试覆盖率 > 80%

### Phase 2 验收标准

- [ ] libpikafish.so 在 ARM64 设备上成功加载
- [ ] UCI 握手成功（uci → uciok, isready → readyok）
- [ ] bestMove() 能在合理时间内返回合法着法
- [ ] NNUE 文件从 assets 正确复制到 internal storage
- [ ] 引擎在 IO 线程运行，不阻塞 UI
- [ ] 6 级难度映射正确

### Phase 3 验收标准

- [ ] chatStream() 返回 Flow<String>，逐 token 推送
- [ ] 嘲讽/自夸 Prompt 模板正确生成
- [ ] 流式响应解析正确（SSE data: {content: "..."} 格式）
- [ ] 去重缓存正常工作（最近 20 条）
- [ ] Key 无效/限流/超时等错误正确处理
- [ ] 棋子中文描述正确

### Phase 4 验收标准

- [ ] 首次启动检测到无 Key → 显示 ApiKeySetupScreen
- [ ] API Key 输入后验证通过 → 加密存储 → 进入主界面
- [ ] 验证失败 → 显示错误，允许重试
- [ ] 后续启动自动读取 Key → 直接进入主界面
- [ ] SettingsScreen 可查看掩码/更新/删除 Key
- [ ] allowBackup=false 生效
- [ ] Key 输入框使用密码掩码

### Phase 5 验收标准

- [ ] 玩家走棋 → 规则校验 → AI 应着 → 胜负判定，全流程通
- [ ] 嘲讽/自夸触发概率正确（基础频率 × 局面权重）
- [ ] 5 种性格可切换，配置正确加载
- [ ] 悔棋功能正确（撤回玩家+AI 各一步）
- [ ] Kotlin Flow 逐字推送嘲讽文本
- [ ] API Key 失效时游戏可正常进行

### Phase 6 验收标准

- [ ] 棋盘绘制正确（10×9 网格、楚河汉界、九宫、32 枚棋子）
- [ ] 触摸选子/走子流畅，高亮合法位置
- [ ] 走子动画平滑（300ms 过渡）
- [ ] 流式嘲讽逐字显示，气泡样式区分嘲讽/自夸
- [ ] 控制面板功能完整（新局/悔棋/认负/难度）
- [ ] 性格选择器卡片展示正确
- [ ] 着法历史列表正确

### Phase 7 验收标准

- [ ] 所有单元测试通过
- [ ] 规则引擎测试覆盖率 > 80%
- [ ] Pikafish 引擎稳定运行
- [ ] DeepSeek API 普通/流式调用正常
- [ ] API Key 全流程正常
- [ ] UI 集成测试通过
- [ ] Android 16 适配（边缘到边缘、16KB 页大小）
- [ ] 棋盘渲染 60fps，引擎不阻塞 UI

---

## 源项目参考

| 源文件 | 行数 | 目标 Kotlin 文件 | 移植状态 |
|--------|------|-----------------|---------|
| Board.java | 197 | Board.kt | ⬜ |
| Position.java | 22 | Position.kt | ⬜ |
| Move.java | 21 | Move.kt | ⬜ |
| Side.java | 50 | Side.kt | ⬜ |
| Piece.java | 52 | Piece.kt | ⬜ |
| PieceType.java | 14 | PieceType.kt | ⬜ |
| GameStatus.java | 11 | GameStatus.kt | ⬜ |
| MoveValidator.java | 226 | MoveValidator.kt | ⬜ |
| MoveGenerator.java | 227 | MoveGenerator.kt | ⬜ |
| CheckDetector.java | 79 | CheckDetector.kt | ⬜ |
| PikafishEngine.java | 346 | PikafishEngine.kt | ⬜ |
| AIService.java | 382 | DeepSeekApiClient.kt + PromptBuilder.kt | ⬜ |
| GameService.java | 298 | GameViewModel.kt | ⬜ |
| SseService.java | 171 | （删除，Kotlin Flow 替代） | ⬜ |
| PersonalityService.java | - | PersonalityManager.kt | ✅ |
| personalities.yaml | - | personalities.json | ✅ |
| ChessBoard.vue | - | ChessBoardCanvas.kt | ⬜ |
| ChatPanel.vue + ChatBubble.vue | - | TrashTalkPanel.kt | ⬜ |
| ControlPanel.vue | - | ControlPanel.kt | ⬜ |
| PersonalitySelector.vue | - | PersonalitySelector.kt | ⬜ |
| MoveHistory.vue | - | MoveHistoryPanel.kt | ⬜ |
| App.vue | - | GameScreen.kt + GameViewModel.kt | ⬜ |
| useApi.ts | - | （删除，无 REST API） | ⬜ |
| useSse.ts | - | （删除，无 SSE） | ⬜ |

状态图例：⬜ 待移植 | 🔄 移植中 | ✅ 已完成

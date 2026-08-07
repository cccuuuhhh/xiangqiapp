# 话唠棋王 — 方案 B+ 修订版（全本地化 + 用户自管 API Key）

> 修订日期：2026-08-07  
> 目标平台：Android 16 (API 36)  
> 核心变更：取消后端服务器，全部逻辑本地化；DeepSeek API Key 由用户首次启动时手动输入，加密存储于本地

---

## 一、修订概要

### 1.1 相比上一版评估报告的核心变化

| 维度 | 上一版 B+ 评估 | 本次修订 |
|------|---------------|---------|
| **API Key 安全** | 内置于 APK，存在反编译泄露风险（评分 3/10） | 用户首次启动手动输入，EncryptedSharedPreferences 加密存储（评分 8/10） |
| **后端依赖** | 无后端 | 无后端（确认） |
| **Pikafish 可行性** | "需要额外验证" | **已验证可行**——`chinese_chess_mobile` 项目已上架 Google Play，Pikafish 原生 ARM64 + UCI 协议成熟 |
| **推荐度** | 不推荐（安全风险） | **推荐**——安全风险已解决，且无服务器运维成本 |
| **开发周期** | 6-8 周 | **4.5-5 周**（有成熟参考项目，降低不确定性） |

### 1.2 为什么 B+ 现在可行了

1. **API Key 安全问题已解决**：用户自行输入 Key，存储在 Android Keystore 加密的 SharedPreferences 中，APK 内不含任何密钥
2. **Pikafish Android 集成已验证**：开源项目 `chinese_chess_mobile`（已上架 Google Play）已成功将 Pikafish 作为原生 ARM64 库运行，通过 UCI 协议通信
3. **零服务器成本**：无需云主机、无需 HTTPS 证书、无需 Docker 部署
4. **离线可玩**：棋盘规则 + Pikafish 引擎完全本地运行，仅嘲讽/自夸需要联网调用 DeepSeek

---

## 二、目标架构

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────┐
│                  Android APK (自包含)                     │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              UI 层 (Jetpack Compose)              │   │
│  │  ├─ ChessBoardCanvas (Compose Canvas 棋盘绘制)    │   │
│  │  ├─ TrashTalkPanel (流式嘲讽展示)                 │   │
│  │  ├─ ControlPanel (新局/悔棋/认负/难度)            │   │
│  │  ├─ PersonalitySelector (性格选择)               │   │
│  │  ├─ ApiKeySetupScreen (首次启动 API Key 输入)     │   │
│  │  └─ SettingsScreen (设置/Key管理)                │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      │ StateFlow / Flow                  │
│  ┌───────────────────┴─────────────────────────────┐   │
│  │            领域层 (Kotlin)                        │   │
│  │  ├─ GameViewModel (游戏流程状态管理)              │   │
│  │  ├─ GameSession (对局状态/走棋历史)               │   │
│  │  ├─ TrashTalkTrigger (嘲讽/自夸触发决策)          │   │
│  │  └─ PersonalityManager (性格配置管理)             │   │
│  └──────┬────────────────┬──────────────┬──────────┘   │
│         │                │              │               │
│  ┌──────┴──────┐ ┌──────┴───────┐ ┌───┴──────────┐    │
│  │  规则引擎    │ │ Pikafish 引擎 │ │ DeepSeek 客户端│    │
│  │  (Kotlin)   │ │  (NDK/JNI)   │ │  (OkHttp)    │    │
│  │             │ │              │ │              │    │
│  │ Board       │ │ libpikafish  │ │ 流式 API 调用  │    │
│  │ MoveValidator│ │  .so (ARM64)│ │ SSE Stream   │    │
│  │ MoveGenerator│ │ UCI 协议通信  │ │ 嘲讽/自夸生成  │    │
│  │ CheckDetector│ │ NNUE 评估    │ │              │    │
│  └─────────────┘ └──────────────┘ └──────┬───────┘    │
│                                          │              │
│  ┌───────────────────────────────────────┴──────────┐  │
│  │              本地存储层                            │  │
│  │  ├─ EncryptedSharedPreferences (API Key 加密存储)  │  │
│  │  ├─ DataStore (游戏设置/难度/性格偏好)             │  │
│  │  └─ Room (可选: 对局历史/棋谱保存)                 │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              原生层 (NDK)                         │   │
│  │  ├─ libpikafish.so (ARM64 引擎二进制)             │   │
│  │  ├─ pikafish.nnue (神经网络评估文件)              │   │
│  │  └─ JNI Bridge (Kotlin ↔ C++ UCI 通信)           │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS (仅嘲讽/自夸时)
                       ▼
              ┌────────────────┐
              │  DeepSeek API  │
              │  (用户自己的 Key) │
              └────────────────┘
```

### 2.2 与原 Web 架构的对比

| 层 | 原 Web 架构 | B+ Android 架构 |
|----|------------|----------------|
| **UI** | Vue 3 + HTML Canvas + DOM | Jetpack Compose + Compose Canvas |
| **状态管理** | Vue ref/reactive | Kotlin StateFlow + Compose State |
| **规则引擎** | Java (后端进程) | Kotlin (App 内) |
| **AI 走棋** | Spring Boot → Pikafish.exe (子进程) | Kotlin → libpikafish.so (JNI) |
| **嘲讽/自夸** | Spring AI → DeepSeek API | OkHttp → DeepSeek API (直连) |
| **流式推送** | Spring SseEmitter → EventSource | Kotlin Flow (进程内流式) |
| **API Key** | application.yml 硬编码 | 用户输入 → EncryptedSharedPreferences |
| **网络通信** | fetch + EventSource | OkHttp (仅 DeepSeek API) |
| **后端服务器** | Spring Boot (必需) | **无** (全部本地) |

---

## 三、组件迁移方案

### 3.1 规则引擎移植（Java → Kotlin）

| 原始文件 | 行数 | Kotlin 目标 | 移植难度 | 说明 |
|---------|------|------------|---------|------|
| Board.java | 197 | Board.kt | 低 | 纯数据结构，直接转 Kotlin |
| Position.java | 22 | Position.kt | 低 | data class，一行搞定 |
| Move.java | 21 | Move.kt | 低 | data class |
| Side.java | 50 | Side.kt | 低 | enum + 扩展函数 |
| Piece.java | 52 | Piece.kt | 低 | 常量映射 |
| PieceType.java | 14 | PieceType.kt | 低 | enum |
| GameStatus.java | 11 | GameStatus.kt | 低 | enum |
| MoveValidator.java | 226 | MoveValidator.kt | 中 | 7 种棋子走法验证，逻辑核心 |
| MoveGenerator.java | 227 | MoveGenerator.kt | 中 | 合法着法生成 |
| CheckDetector.java | 79 | CheckDetector.kt | 中 | 将军/将杀/困毙检测 |
| **合计** | **899** | | | **参考蓝本，逻辑不变，语法转换** |

**关键**：这些是纯逻辑代码，无外部依赖。参考项目 `chinese_chess_mobile` 已有 Kotlin 版本的 Board/Piece/Move/Position 实现，可交叉验证。

### 3.2 Pikafish 引擎集成（NDK/JNI）

#### 现状
- 原项目通过 `ProcessBuilder` 启动 `pikafish.exe` 子进程，通过 stdin/stdout 通信 UCI 协议
- 引擎文件：`pikafish-xxx.exe` + `pikafish.nnue`（Windows 平台）

#### Android 方案

```
┌─────────────────────────────────────┐
│         Kotlin 层                    │
│  PikafishEngine.kt                  │
│  ├─ fun init()          // 加载 .so  │
│  ├─ fun sendCommand(cmd) // UCI 命令  │
│  ├─ fun readLine(): String // 读响应  │
│  ├─ fun bestMove(board, side): Move  │
│  └─ fun destroy()       // 关闭引擎   │
│         │ JNI                        │
│  ┌──────┴──────────────────┐        │
│  │   C++ JNI Bridge         │        │
│  │  ├─ Java_com_..._init()  │        │
│  │  ├─ Java_com_..._send()  │        │
│  │  └─ Java_com_..._read()  │        │
│  │  (管道通信：pipe + fork)  │        │
│  └──────┬──────────────────┘        │
│         │                           │
│  ┌──────┴──────────────────┐        │
│  │  libpikafish.so          │        │
│  │  (ARM64 原生引擎)         │        │
│  │  + pikafish.nnue          │        │
│  └──────────────────────────┘        │
└─────────────────────────────────────┘
```

**实现步骤**：

1. **交叉编译 Pikafish**（或下载预编译版）
   ```bash
   # 使用 NDK 交叉编译
   cd pikafish/src
   make -j build ARCH=armv8 COMP=ndk
   # 产物：pikafish (ARM64 可执行文件)
   ```

2. **JNI 桥接层**：创建 C++ wrapper，用 POSIX pipe 实现 stdin/stdout 重定向
   - `engine_init()`: 加载 .so，创建管道，fork 引擎线程
   - `engine_send(cmd)`: 写入管道（等价于 stdin）
   - `engine_read()`: 读取管道（等价于 stdout）
   - `engine_destroy()`: 发送 "quit"，清理资源

3. **NNUE 文件管理**
   - `pikafish.nnue` 打包在 APK `assets/` 中
   - 首次启动时从 assets 复制到 `app/data/files/`
   - 引擎初始化时指定 NNUE 文件路径

4. **UCI 通信逻辑**：与原 `PikafishEngine.java` 完全一致
   - `uci` → 等待 `uciok`
   - `setoption name Threads/Hash/Skill Level`
   - `isready` → 等待 `readyok`
   - `position fen <fen>` + `go depth <n>` → 等待 `bestmove`

**参考验证**：`chinese_chess_mobile` 项目的 `PikafishEngine.kt` 已实现相同方案，已上架 Google Play，Pikafish 在 Android ARM64 设备上运行稳定。

#### 坐标转换体系（保持不变）

原项目的 {row, col} ↔ ICCS 坐标互转逻辑直接移植到 Kotlin：

```
项目坐标: row 0 = 黑方底线, row 9 = 红方底线, col 0-8
ICCS 坐标: 列 a-i (0-8), 行 0 (红方底线) - 9 (黑方底线)

iccsToPosition("h2") → Position(row=7, col=7)
positionToIccs(Position(7, 7)) → "h2"
```

### 3.3 DeepSeek API 客户端（直连）

#### 现状
- Spring AI 的 `ChatClient` 封装了 DeepSeek API 调用
- 嘲讽/自夸是同步调用，SseService 负责逐字符推送（模拟打字机）

#### Android 方案

```kotlin
// DeepSeekApiClient.kt — 直接 HTTP 调用 DeepSeek API
class DeepSeekApiClient(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String  // 从 EncryptedSharedPreferences 读取
) {
    // 普通调用（走棋回退用）
    suspend fun chat(prompt: String): String

    // 流式调用（嘲讽/自夸用）— 返回 Flow 实现真打字机效果
    suspend fun chatStream(prompt: String): Flow<String>
}
```

**关键改进**：原 Web 版的 SseService 用 `Thread.sleep(30)` 模拟逐字推送。Android 版直接使用 DeepSeek 的 **streaming API**（SSE），实现真正的逐 token 流式输出，体验更好。

#### API 调用映射

| 原 Java 方法 | Android Kotlin 对应 | 说明 |
|-------------|-------------------|------|
| `AIService.generateTrashTalk()` | `DeepSeekApiClient.chatStream(prompt)` | 返回 Flow<String>，逐 token 推送 |
| `AIService.generateSelfPraise()` | `DeepSeekApiClient.chatStream(prompt)` | 同上 |
| `AIService.proposeMove()` (LLM回退) | `DeepSeekApiClient.chat(prompt)` | 同步调用，Pikafish 不可用时回退 |
| `AIService.buildTrashTalkPrompt()` | `PromptBuilder.buildTrashTalk()` | Prompt 模板直接移植 |
| `AIService.buildSelfPraisePrompt()` | `PromptBuilder.buildSelfPraise()` | 同上 |
| `AIService.buildMovePrompt()` | `PromptBuilder.buildMove()` | 同上 |

### 3.4 游戏流程管理移植

| 原 Java 组件 | Android Kotlin 对应 | 说明 |
|-------------|-------------------|------|
| `GameService.java` (298行) | `GameViewModel.kt` | 游戏流程、状态管理、走棋校验 |
| `GameSession` (内部类) | `GameSession.kt` | data class，对局状态 |
| `SseService.java` (171行) | **删除** | 不需要 SSE，Kotlin Flow 替代 |
| `PersonalityService.java` | `PersonalityManager.kt` | 性格配置管理 |
| `personalities.yaml` | `personalities.json` 或 Kotlin object | 5 种性格配置 |

#### 流程对比

**原 Web 流程**：
```
玩家走棋 → POST /api/move → GameService.playerMove()
  → MoveValidator 校验
  → Board.applyMove()
  → CheckDetector 检查胜负
  → AIService.proposeMove() (Pikafish/LLM)
  → Board.applyMove(aiMove)
  → SseService.pushTrashTalk() (异步 SSE)
  → SseService.pushSelfPraise() (异步 SSE)
  → 返回 MoveResult
```

**Android B+ 流程**：
```
玩家走棋 → GameViewModel.playerMove()
  → MoveValidator 校验 (本地 Kotlin)
  → Board.applyMove() (本地)
  → CheckDetector 检查胜负 (本地)
  → PikafishEngine.bestMove() (本地 JNI)
  → Board.applyMove(aiMove) (本地)
  → TrashTalkTrigger 决策 (本地)
  → DeepSeekApiClient.chatStream() (网络，流式)
  → UI 通过 Flow 逐字显示
```

**核心区别**：全部在 App 内完成，无需 HTTP 往返。只有嘲讽/自夸文本生成需要联网。

### 3.5 前端 UI 重写

| 原 Vue 组件 | Compose 对应 | 工作量 | 说明 |
|------------|-------------|--------|------|
| ChessBoard.vue | ChessBoardCanvas.kt | 3 天 | Compose Canvas 绘制棋盘/棋子 |
| ChessBoard.vue (交互) | ChessBoardGesture.kt | 1 天 | 触摸选子/走子/拖拽 |
| ChatPanel.vue + ChatBubble.vue | TrashTalkPanel.kt | 2 天 | LazyColumn + 流式文字动画 |
| ControlPanel.vue | ControlPanel.kt | 1 天 | 新局/悔棋/认负/难度 |
| PersonalitySelector.vue | PersonalitySelector.kt | 1 天 | LazyRow 卡片选择 |
| MoveHistory.vue | MoveHistoryPanel.kt | 0.5 天 | LazyColumn 着法列表 |
| App.vue (状态) | GameViewModel.kt | 1.5 天 | ViewModel + StateFlow |
| types/index.ts | Model.kt | 0.5 天 | data class 定义 |
| useApi.ts | **删除** | - | 无 REST API |
| useSse.ts | **删除** | - | 无 SSE |
| **新增** ApiKeySetupScreen | ApiKeySetupScreen.kt | 1 天 | 首次启动 Key 输入页 |
| **新增** SettingsScreen | SettingsScreen.kt | 0.5 天 | 设置/Key 管理 |
| **合计** | | **~12 天** | |

---

## 四、API Key 安全方案

### 4.1 安全架构

```
┌──────────────────────────────────────────┐
│              首次启动流程                   │
│                                        │
│  [启动 App]                             │
│      ↓                                 │
│  检查 EncryptedSharedPreferences 是否有 Key │
│      ↓                         ↓        │
│   有 Key                      无 Key     │
│      ↓                         ↓        │
│  进入主界面              ApiKeySetupScreen │
│                           ├─ 输入 DeepSeek API Key │
│                           ├─ 点击"验证"       │
│                           │   → 调用 DeepSeek 测试接口 │
│                           │   → 成功：加密存储     │
│                           │   → 失败：提示错误     │
│                           └─ 验证通过 → 进入主界面  │
└──────────────────────────────────────────┘
```

### 4.2 加密存储技术方案

| 维度 | 方案 |
|------|------|
| **存储方式** | AndroidX Security `EncryptedSharedPreferences` |
| **加密算法** | AES-256-GCM（值）+ AES-256-SIV（键） |
| **密钥管理** | Android Keystore 系统级密钥库（硬件隔离） |
| **密钥绑定** | 绑定到设备，不可导出（root 设备也难以提取） |
| **API** | `MasterKey.Builder().setKeyScheme(AES256_GCM).build()` |

```kotlin
// ApiKeyStore.kt
class ApiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "secure_prefs", masterKey,
        AES256_SIV, AES256_GCM
    )

    fun saveApiKey(key: String) {
        prefs.edit().putString("deepseek_api_key", key).apply()
    }

    fun getApiKey(): String? = prefs.getString("deepseek_api_key", null)

    fun clearApiKey() {
        prefs.edit().remove("deepseek_api_key").apply()
    }
}
```

### 4.3 安全性评估

| 威胁场景 | 风险等级 | 缓解措施 |
|---------|---------|---------|
| APK 反编译提取 Key | **无风险** | APK 内不含 Key |
| 中间人攻击窃取 Key | 低 | DeepSeek API 强制 HTTPS |
| Root 设备读取 SharedPreferences | 中低 | EncryptedSharedPreferences + Keystore 硬件隔离 |
| 设备备份泄露 | 低 | `allowBackup="false"` 禁止备份 |
| 屏幕共享/截图泄露 | 低 | API Key 输入框使用 `TextField(passwordVisualTransformation)` |
| Key 过期/失效 | 低 | 设置页可随时更新 Key，输入时自动验证 |

### 4.4 用户体验流程

1. **首次启动**：显示 API Key 输入页面，附带获取 Key 的链接（DeepSeek 开放平台）
2. **输入验证**：点击"验证"按钮，发送一个轻量测试请求，验证 Key 有效性
3. **验证成功**：加密存储，进入主界面
4. **验证失败**：显示错误信息（如"Key 无效"或"网络错误"），允许重试
5. **后续启动**：自动读取存储的 Key，直接进入主界面
6. **设置页面**：可随时查看（掩码显示）、更新、删除 Key
7. **Key 失效时**：游戏可正常进行（Pikafish 引擎本地运行），嘲讽功能显示"API Key 无效，请在设置中更新"

---

## 五、工作量分解

### 5.1 总览

| 阶段 | 工作项 | 工作量 |
|------|--------|--------|
| 1 | Android 项目搭建 | 1.5 天 |
| 2 | 规则引擎移植 (Java → Kotlin) | 3 天 |
| 3 | Pikafish NDK 集成 | 3 天 |
| 4 | DeepSeek API 客户端 | 2 天 |
| 5 | API Key 安全管理 | 1 天 |
| 6 | 游戏逻辑移植 | 2 天 |
| 7 | UI 开发 (Compose) | 8 天 |
| 8 | 测试 & 联调 | 2.5 天 |
| **总计** | | **~23 天（约 4.5-5 周）** |

### 5.2 详细分解

#### 阶段 1：Android 项目搭建（1.5 天）

| 任务 | 说明 |
|------|------|
| Gradle 项目初始化 | Kotlin DSL + Compose + NDK 配置 |
| 依赖配置 | Compose BOM / OkHttp / DataStore / Security-crypto / Coroutines |
| 项目包结构 | `com.hualao.qiwang.*` 分层（model/engine/ai/ui/data） |
| 最低/目标 SDK | minSdk=26, targetSdk=36 (Android 16) |
| 签名配置 | debug + release keystore |
| ProGuard 规则 | OkHttp / Kotlin 反射规则 |

#### 阶段 2：规则引擎移植（3 天）

| 任务 | 原始行数 | 说明 |
|------|---------|------|
| Board.kt | 197 | 10×9 棋盘状态、applyMove、createInitial |
| Position.kt + Move.kt + Side.kt + Piece.kt + PieceType.kt + GameStatus.kt | 170 | 数据类和枚举，快速转换 |
| MoveValidator.kt | 226 | 7 种棋子走法验证（将/士/象/马/车/炮/兵） |
| MoveGenerator.kt | 227 | 全部合法着法生成 |
| CheckDetector.kt | 79 | 将军/将杀/困毙检测 |
| 单元测试 | - | 移植后立即测试，确保规则一致性 |

#### 阶段 3：Pikafish NDK 集成（3 天）

| 任务 | 说明 |
|------|------|
| 交叉编译 Pikafish ARM64 | `make build ARCH=armv8 COMP=ndk` 或下载预编译版 |
| JNI Bridge (C++) | pipe + fork 方式，stdin/stdout 重定向 |
| PikafishEngine.kt | UCI 通信封装（init/send/read/bestMove/destroy） |
| NNUE 文件管理 | assets → internal storage 复制 |
| FEN 生成 | boardToFen() 移植 |
| 坐标互转 | ICCS ↔ {row,col} 移植 |
| 引擎线程管理 | 后台线程运行，避免阻塞 UI |
| 集成测试 | 验证引擎能正确返回 bestmove |

#### 阶段 4：DeepSeek API 客户端（2 天）

| 任务 | 说明 |
|------|------|
| DeepSeekApiClient.kt | OkHttp 封装，普通 + 流式两种调用 |
| PromptBuilder.kt | 移植 buildTrashTalkPrompt / buildSelfPraisePrompt / buildMovePrompt |
| 流式响应解析 | 解析 DeepSeek SSE 流（data: {content: "..."} 格式） |
| 去重缓存管理 | 移植 recentTrashTalks / recentSelfPraises (Deque) |
| 错误处理 | Key 无效 / 限流 / 网络超时 等 |
| 棋子中文描述 | 移植 pieceToChinese() / describeMove() |

#### 阶段 5：API Key 安全管理（1 天）

| 任务 | 说明 |
|------|------|
| ApiKeyStore.kt | EncryptedSharedPreferences 封装 |
| ApiKeySetupScreen.kt | 首次启动 Key 输入页面（含验证功能） |
| ApiKey验证逻辑 | 调用 DeepSeek 轻量接口验证 Key |
| SettingsScreen.kt | Key 管理（查看掩码/更新/删除） |
| 首次启动判断逻辑 | 无 Key → SetupScreen，有 Key → 主界面 |
| AndroidManifest 配置 | allowBackup=false,网络安全配置 |

#### 阶段 6：游戏逻辑移植（2 天）

| 任务 | 原始文件 | 说明 |
|------|---------|------|
| GameViewModel.kt | GameService.java (298行) | 走棋流程、AI 应着、胜负判定 |
| GameSession.kt | GameSession (内部类) | 对局状态 data class |
| TrashTalkTrigger.kt | shouldTrashTalk / shouldSelfPraise | 嘲讽/自夸触发概率逻辑 |
| PersonalityManager.kt | PersonalityService.java | 性格配置加载和切换 |
| personalities.json | personalities.yaml | 5 种性格配置（JSON 格式） |
| 悔棋逻辑 | undo() | 撤回玩家+AI 各一步 |
| Flow 集成 | SseService 替代 | Kotlin Flow 逐字推送嘲讽文本 |

#### 阶段 7：UI 开发（8 天）

| 任务 | 工作量 | 说明 |
|------|--------|------|
| ChessBoardCanvas.kt | 3 天 | Compose Canvas 绘制棋盘/棋子/坐标/河界/九宫 |
| ChessBoardGesture.kt | 1 天 | 触摸选子/走子，高亮合法位置 |
| 走子动画 | 0.5 天 | Compose Animation 平滑移动 |
| TrashTalkPanel.kt | 2 天 | 流式文字展示 + 气泡 UI + 自动滚动 |
| ControlPanel.kt | 0.5 天 | 新局/悔棋/认负/难度选择 |
| PersonalitySelector.kt | 0.5 天 | 卡片选择 + 头像/描述 |
| MoveHistoryPanel.kt | 0.5 天 | 着法列表 |

#### 阶段 8：测试 & 联调（2.5 天）

| 任务 | 说明 |
|------|------|
| 规则引擎单元测试 | 各种棋子走法、将军/将杀/困毙 |
| Pikafish 引擎测试 | 引擎初始化、bestmove 返回、坐标转换 |
| DeepSeek API 测试 | 普通/流式调用、错误处理 |
| UI 集成测试 | 走棋流程、嘲讽显示、API Key 流程 |
| Android 16 适配 | API 36 特性、权限、边缘到边缘显示 |
| 性能优化 | 棋盘渲染帧率、引擎线程优先级 |

---

## 六、风险清单（更新版）

| # | 风险 | 级别 | 影响 | 缓解措施 |
|---|------|------|------|---------|
| 1 | Pikafish 交叉编译失败 | 中 | 引擎不可用 | 下载 `chinese_chess_mobile` 项目的预编译版；或使用其 JNI 方案 |
| 2 | NNUE 文件体积过大 | 低 | APK 体积大 | pikafish.nnue 约 40MB，可接受；或首次启动时下载 |
| 3 | DeepSeek API 流式响应不稳定 | 低 | 嘲讽中断 | OkHttp 重试 + 超时处理 + 降级为非流式 |
| 4 | 用户输入无效 API Key | 低 | 嘲讽不可用 | 输入时验证 + 游戏可正常进行（引擎本地运行） |
| 5 | Android Keystore 不可用（旧设备） | 极低 | Key 存储降级 | minSdk=26，Keystore 在 API 23+ 可用 |
| 6 | Compose Canvas 棋盘性能 | 低 | 渲染卡顿 | Compose Canvas 性能优秀，参考项目用 View Canvas 已够用 |
| 7 | 引擎线程阻塞 UI | 低 | 界面卡顿 | 引擎在 IO 线程运行，UI 线程仅接收结果 |
| 8 | Android 16 (API 36) 适配 | 低 | 兼容性 | targetSdk=36，关注边缘到边缘、16KB 页大小 |

### 6.1 与上一版风险对比

| 风险 | 上一版 | 本版 | 变化原因 |
|------|-------|------|---------|
| API Key 泄露 | **严重** | **已解决** | 用户自管 Key + 加密存储 |
| Pikafish Android 可行性 | **中（需验证）** | **低（已验证）** | 参考项目已上架 Google Play |
| 后端服务器成本 | 低 | **消除** | 无后端 |
| SSE 稳定性 | 中 | **消除** | 无 SSE，Kotlin Flow 替代 |

---

## 七、参考项目对比

| 维度 | `chinese_chess_mobile` | 本项目「话唠棋王」 |
|------|----------------------|------------------|
| 引擎 | Pikafish (ARM64 + UCI) | **相同** |
| 语言 | Kotlin | **相同** |
| UI 框架 | Android View Canvas | Jetpack Compose Canvas（更现代） |
| AI 对话 | 无 | **DeepSeek 流式嘲讽/自夸（核心差异点）** |
| API Key | 无需（纯离线） | 用户输入 + 加密存储 |
| 性格系统 | 无 | 5 种性格（毒舌/优雅/中二/温柔/冷血） |
| 难度系统 | 6 级 | 可复用，映射到 Pikafish depth |
| 目标 SDK | 35 (Android 15) | 36 (Android 16) |
| 开源协议 | GPL-3.0 | 独立开发 |

**可借鉴内容**：
- PikafishEngine.kt 的 JNI 集成方案
- NNUE 文件管理方式
- Board/Piece/Move 的 Kotlin 数据结构设计
- UCI 通信的线程安全处理

**不可直接复用**：
- UI 代码（他们用 View Canvas，我们用 Compose）
- 游戏逻辑（他们没有嘲讽/自夸/性格系统）
- 架构设计（他们用 MVC，我们用 MVVM + ViewModel）

---

## 八、文件结构规划

```
app/src/main/
├── java/com/hualao/qiwang/
│   ├── model/                          # 数据模型
│   │   ├── Board.kt                    # 棋盘状态
│   │   ├── Piece.kt                    # 棋子定义
│   │   ├── Move.kt                     # 着法
│   │   ├── Position.kt                 # 坐标
│   │   ├── Side.kt                     # 红黑方
│   │   ├── GameStatus.kt               # 游戏状态枚举
│   │   └── GameSession.kt              # 对局会话
│   ├── engine/                         # 规则引擎
│   │   ├── MoveValidator.kt            # 走法验证
│   │   ├── MoveGenerator.kt            # 合法着法生成
│   │   └── CheckDetector.kt            # 将军/将杀检测
│   ├── ai/                             # AI 引擎
│   │   ├── PikafishEngine.kt           # Pikafish JNI 封装
│   │   ├── DeepSeekApiClient.kt        # DeepSeek API 客户端
│   │   ├── PromptBuilder.kt            # Prompt 模板
│   │   └── TrashTalkTrigger.kt         # 嘲讽/自夸触发逻辑
│   ├── data/                           # 本地存储
│   │   ├── ApiKeyStore.kt              # API Key 加密存储
│   │   ├── PersonalityManager.kt       # 性格配置管理
│   │   └── personalities.json          # 5种性格配置
│   ├── ui/                             # Compose UI
│   │   ├── screen/
│   │   │   ├── ApiKeySetupScreen.kt    # 首次启动 Key 输入
│   │   │   ├── GameScreen.kt           # 主游戏界面
│   │   │   └── SettingsScreen.kt       # 设置页
│   │   ├── component/
│   │   │   ├── ChessBoardCanvas.kt     # 棋盘绘制
│   │   │   ├── ChessBoardGesture.kt    # 触摸交互
│   │   │   ├── TrashTalkPanel.kt       # 嘲讽面板
│   │   │   ├── ControlPanel.kt         # 控制面板
│   │   │   ├── PersonalitySelector.kt  # 性格选择
│   │   │   └── MoveHistoryPanel.kt     # 着法历史
│   │   └── theme/
│   │       └── Theme.kt                # Material 3 主题
│   ├── viewmodel/
│   │   └── GameViewModel.kt            # 游戏状态管理
│   └── MainActivity.kt                 # 入口
├── cpp/                                # NDK 原生代码
│   ├── pikafish_jni.cpp                # JNI 桥接
│   └── CMakeLists.txt                  # CMake 构建配置
├── jniLibs/
│   └── arm64-v8a/
│       └── libpikafish.so              # Pikafish ARM64 引擎
├── assets/
│   └── pikafish.nnue                   # NNUE 神经网络文件
└── res/
    └── ...
```

---

## 九、技术栈清单

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Kotlin | 2.0+ | 全部业务代码 |
| UI 框架 | Jetpack Compose | BOM 2024.x | UI 渲染 |
| 最低 SDK | Android 8.0 (API 26) | - | 覆盖 95%+ 设备 |
| 目标 SDK | Android 16 (API 36) | - | 最新平台适配 |
| AI 引擎 | Pikafish | 2026.01 | 中国象棋走棋引擎 |
| HTTP 客户端 | OkHttp | 4.12+ | DeepSeek API 调用 |
| 加密存储 | AndroidX Security | 1.1.0-alpha06 | API Key 加密 |
| 偏好存储 | DataStore | 1.1+ | 游戏设置 |
| 异步 | Kotlin Coroutines | 1.8+ | 异步任务 + Flow |
| 构建工具 | Gradle (Kotlin DSL) | 8.9+ | 项目构建 |
| NDK | Android NDK | r26+ | Pikafish 原生编译 |

---

## 十、决策矩阵（更新版）

| 方案 | 开发周期 | 用户体验 | 安全性 | 维护成本 | 离线能力 | 服务器成本 | 总分 |
|------|---------|---------|--------|---------|---------|-----------|------|
| A: Capacitor 封装 | 1-2 周 | 3/10 | 8/10 | 7/10 | 1/10 | 需服务器 | 19/50 |
| B: Kotlin 原生 + 云端 | 5 周 | 9/10 | 8/10 | 5/10 | 2/10 | 需服务器 | 24/50 |
| **B+: 全本地化 (修订)** | **4.5-5 周** | **9/10** | **8/10** | **8/10** | **8/10** | **无** | **33/50** |
| C: React Native | 3-5 周 | 7/10 | 8/10 | 7/10 | 2/10 | 需服务器 | 24/50 |

> **B+ 方案修订后评分大幅提升**：安全性从 3/10 → 8/10（API Key 问题解决），维护成本从 5/10 → 8/10（无服务器），离线能力 7/10 → 8/10（仅嘲讽需联网），且无服务器成本。

---

## 十一、下一步建议

1. **确认方案**：确认走 B+ 修订版方案，启动标准 SOP 开发流程
2. **准备 Pikafish Android 二进制**：交叉编译或从参考项目获取预编译版
3. **DeepSeek API Key**：用户自行注册 DeepSeek 开放平台账号获取 API Key
4. **开发环境**：Android Studio + Kotlin 2.0 + Compose + NDK r26+
5. **启动 SOP 流程**：产品经理(PRD) → 架构师(系统设计+任务分解) → 工程师(代码实现) → QA(测试验证)

---

*本方案基于 D:\workspace\xiangqi 项目代码分析 + Pikafish Android 集成可行性验证生成。*

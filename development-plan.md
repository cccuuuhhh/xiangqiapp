# 话唠棋王 Android 版 — 迁移/开发计划文档

> 版本：v1.0.0 | 最后更新：2026-08-07  
> 方案：B+ 全本地化（用户自管 API Key）  
> 目标平台：Android 16 (API 36)  
> 源项目：`D:\workspace\xiangqi`（Vue 3 + Spring Boot + DeepSeek + Pikafish Web 版）

---

## 一、项目概述

### 1.1 项目定位

将现有 Web 版「话唠棋王」迁移为 **Android 原生 App**。核心变更：取消后端服务器，全部逻辑本地化；DeepSeek API Key 由用户首次启动时手动输入，加密存储于本地。

### 1.2 核心体验

- **打开即玩**：首次启动输入 API Key 后，离线可下棋
- **Pikafish 本地引擎**：ARM64 原生库，零延迟出棋，棋力碾压
- **DeepSeek 流式嘲讽/自夸**：真打字机效果（非模拟），需联网
- **五种性格**：毒舌大师、优雅绅士、中二少年、禅意大师、沉默杀手
- **零服务器成本**：无云主机、无 HTTPS 证书、无 Docker

### 1.3 技术选型

| 层 | 技术 | 版本 | 说明 |
|----|------|------|------|
| 语言 | Kotlin | 2.0+ | 全部业务代码 |
| UI 框架 | Jetpack Compose | BOM 2024.x | 声明式 UI + Canvas |
| 最低 SDK | Android 8.0 | API 26 | 覆盖 95%+ 设备 |
| 目标 SDK | Android 16 | API 36 | 最新平台适配 |
| AI 引擎 | Pikafish | 2026.01 | ARM64 原生库 + UCI 协议 |
| HTTP 客户端 | OkHttp | 4.12+ | DeepSeek API 调用 |
| 加密存储 | AndroidX Security | 1.1.0-alpha06 | API Key 加密 (AES-256-GCM) |
| 偏好存储 | DataStore | 1.1+ | 游戏设置 |
| 异步 | Kotlin Coroutines | 1.8+ | 异步任务 + Flow |
| 构建工具 | Gradle (Kotlin DSL) | 8.9+ | 项目构建 |
| NDK | Android NDK | r26+ | Pikafish 原生编译 |

### 1.4 与 Web 版的核心差异

| 维度 | Web 版 | Android B+ 版 |
|------|--------|---------------|
| 后端服务器 | Spring Boot（必需） | **无** |
| 规则引擎 | Java（后端进程） | Kotlin（App 内） |
| AI 走棋 | Pikafish.exe 子进程 | libpikafish.so JNI 调用 |
| 嘲讽/自夸 | Spring AI + SSE 模拟打字机 | OkHttp 直连 DeepSeek 流式 API（真打字机） |
| API Key | application.yml 硬编码 | 用户输入 + Keystore 加密 |
| 流式推送 | SseEmitter → EventSource | Kotlin Flow（进程内流式） |
| 网络通信 | fetch + EventSource | OkHttp（仅 DeepSeek API） |
| 服务器成本 | 月 50-100 元 | **零** |

---

## 二、架构设计

### 2.1 整体架构

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
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS (仅嘲讽/自夸时)
                       ▼
              ┌────────────────┐
              │  DeepSeek API  │
              │  (用户自己的 Key) │
              └────────────────┘
```

### 2.2 核心流程

#### 玩家走棋流程（全部本地）

```
玩家点击棋子 → ChessBoardGesture 检测触摸
  → MoveValidator 校验走法 (本地 Kotlin)
  → Board.applyMove() (本地)
  → CheckDetector 检查胜负 (本地)
  → 棋盘 UI 立即更新 (Compose State)
  → PikafishEngine.bestMove() (本地 JNI, IO 线程)
  → Board.applyMove(aiMove) (本地)
  → 棋盘 UI 更新 AI 走棋 (走子动画)
  → TrashTalkTrigger 决策 (本地)
  → DeepSeekApiClient.chatStream() (网络, IO 线程)
  → UI 通过 Flow 逐字显示嘲讽/自夸
```

**核心区别**：全部在 App 内完成，无需 HTTP 往返。只有嘲讽/自夸文本生成需要联网。

#### API Key 首次输入流程

```
[启动 App]
    ↓
检查 EncryptedSharedPreferences 是否有 Key
    ↓                    ↓
  有 Key               无 Key
    ↓                    ↓
进入主界面         ApiKeySetupScreen
                    ├─ 输入 DeepSeek API Key
                    ├─ 点击"验证"
                    │   → 调用 DeepSeek 测试接口
                    │   → 成功：加密存储
                    │   → 失败：提示错误
                    └─ 验证通过 → 进入主界面
```

#### 嘲讽/自夸流式推送（Kotlin Flow 替代 SSE）

```
TrashTalkTrigger.shouldTrashTalk() → true
    ↓
DeepSeekApiClient.chatStream(prompt) : Flow<String>
    ↓
GameViewModel.collect { token ->
    TrashTalkPanel 逐 token 追加显示 (真打字机效果)
}
    ↓
Flow 完成 → 嘲讽气泡定型
```

---

## 三、坐标系统规范（继承自 Web 版，不变）

### 3.1 唯一坐标标准

```
棋盘坐标系：

    row=0  [  ][  ][  ][  ][  ][  ][  ][  ][  ]  黑方底线
    row=1  [  ][  ][  ][  ][  ][  ][  ][  ][  ]
    row=2  [  ][  ][  ][  ][  ][  ][  ][  ][  ]
    row=3  [  ][  ][  ][  ][  ][  ][  ][  ][  ]
    row=4  [  ][  ][  ][  ][  ][  ][  ][  ][  ]  ← 楚河汉界上沿
          ———— 楚 河 —————— 汉 界 ——————
    row=5  [  ][  ][  ][  ][  ][  ][  ][  ][  ]  ← 楚河汉界下沿
    row=6  [  ][  ][  ][  ][  ][  ][  ][  ][  ]
    row=7  [  ][  ][  ][  ][  ][  ][  ][  ][  ]
    row=8  [  ][  ][  ][  ][  ][  ][  ][  ][  ]
    row=9  [  ][  ][  ][  ][  ][  ][  ][  ][  ]  红方底线

          col=0 1  2  3  4  5  6  7  8
```

| 概念 | 表示 | 示例 |
|------|------|------|
| 棋盘 | `Array<Array<String>>` 二维数组 | `board[0][0]` = 黑方左上角 |
| 坐标 | `Position(row: Int, col: Int)` | `Position(0, 0)` = 黑车初始位 |
| 走法 | `Move(from: Position, to: Position, piece: String)` | — |
| row 方向 | 0=黑方底线，9=红方底线 | row 增大 = 向红方前进 |
| col 方向 | 0=最左列，8=最右列 | — |

**关键约定**：
- 红方视角：己方底线 row=9，向前（向黑方）row 减小
- 黑方视角：己方底线 row=0，向前（向红方）row 增大
- Compose Canvas 渲染时根据视角做 row 翻转，但**存储和传输始终使用此坐标系不变**
- **禁止任何字母+数字混排**（如 `e0`、`a4`），**禁止中文列号**（如 "九"、"五"）

### 3.2 棋盘编码

- 红方：`rK`(帅)、`rA`(仕)、`rB`(相)、`rN`(馬)、`rR`(車)、`rC`(砲)、`rP`(兵)
- 黑方：`bK`(将)、`bA`(士)、`bB`(象)、`bN`(马)、`bR`(车)、`bC`(炮)、`bP`(卒)
- 空格：空字符串 `""`

### 3.3 ICCS 坐标互转（Pikafish 通信用）

```
项目坐标: row 0 = 黑方底线, row 9 = 红方底线, col 0-8
ICCS 坐标: 列 a-i (0-8), 行 0 (红方底线) - 9 (黑方底线)

iccsToPosition("h2") → Position(row=7, col=7)
positionToIccs(Position(7, 7)) → "h2"
```

### 3.4 初始棋盘布局

```
row=0: bR bN bB bA bK bA bB bN bR
row=1: -- -- -- -- -- -- -- -- --
row=2: -- bC -- -- -- -- -- bC --   (col=1, col=7 = 黑炮)
row=3: bP -- bP -- bP -- bP -- bP   (col=0,2,4,6,8 = 黑卒)
row=4: -- -- -- -- -- -- -- -- --
row=5: -- -- -- -- -- -- -- -- --
row=6: rP -- rP -- rP -- rP -- rP   (col=0,2,4,6,8 = 红兵)
row=7: -- rC -- -- -- -- -- rC --   (col=1, col=7 = 红炮)
row=8: -- -- -- -- -- -- -- -- --
row=9: rR rN rB rA rK rA rB rN rR
```

---

## 四、规则引擎设计（Java → Kotlin 移植）

### 4.1 核心数据类

```kotlin
// Position.kt
data class Position(val row: Int, val col: Int)

// Move.kt
data class Move(val from: Position, val to: Position, val piece: String)

// Side.kt
enum class Side(val value: String) {
    RED("red"), BLACK("black");
    fun opposite(): Side = if (this == RED) BLACK else RED
}

// PieceType.kt
enum class PieceType(val code: String) {
    KING("K"), ADVISOR("A"), BISHOP("B"), KNIGHT("N"),
    ROOK("R"), CANNON("C"), PAWN("P")
}

// GameStatus.kt
enum class GameStatus {
    PLAYING, RED_WIN, BLACK_WIN, DRAW
}

// Board.kt
class Board {
    val grid: Array<Array<String>> = Array(10) { Array(9) { "" } }
    
    fun pieceAt(pos: Position): String
    fun applyMove(move: Move): String  // 返回被吃的棋子
    fun undoMove(move: Move, captured: String)
    fun clone(): Board
    companion object {
        fun createInitial(): Board
    }
}
```

### 4.2 MoveValidator（7 种棋子走法校验）

| 棋子 | 走法规则 | 特殊约束 |
|------|---------|---------|
| 帅/将 | 九宫内一步（上下左右） | 不能与对方将/帅对面（飞将） |
| 仕/士 | 九宫内斜走一步 | — |
| 相/象 | 田字对角（2×2） | 不能过河，注意塞眼 |
| 馬/马 | 日字（1×2或2×1拐弯） | 注意蹩脚 |
| 車/车 | 直线任意距离 | 路径不能有子阻挡 |
| 砲/炮 | 直线任意距离 | **吃子必须翻山**（炮架），走子不翻山 |
| 兵/卒 | 未过河：只能向前一步 | 过河后：可向前/左/右一步，不能后退 |

### 4.3 CheckDetector

- 将军检测：走棋后不能让自己的将被将军
- 将杀（Checkmate）：将军且无合法应着
- 困毙（Stalemate）：无子可动

### 4.4 FEN 生成（Pikafish 通信）

```kotlin
fun boardToFen(board: Board, side: Side, moveCount: Int): String {
    // 将 Board 转换为 FEN 字符串供 Pikafish 使用
    // 格式示例: rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1
}
```

---

## 五、Pikafish NDK 集成设计

### 5.1 架构

```
┌─────────────────────────────────────┐
│         Kotlin 层                    │
│  PikafishEngine.kt                  │
│  ├─ fun init()          // 加载 .so  │
│  ├─ fun sendCommand(cmd) // UCI 命令  │
│  ├─ fun readLine(): String // 读响应  │
│  ├─ suspend fun bestMove(fen, depth): Move  │
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

### 5.2 UCI 通信协议

```
1. 发送 "uci" → 等待 "uciok"
2. 发送 "setoption name Threads value 2"
3. 发送 "setoption name Hash value 128"
4. 发送 "setoption name Skill Level value 10"  (难度)
5. 发送 "isready" → 等待 "readyok"
6. 发送 "position fen <fen>"
7. 发送 "go depth <depth>" → 等待 "bestmove <move>"
```

### 5.3 NNUE 文件管理

- `pikafish.nnue` 打包在 APK `assets/` 中
- 首次启动时从 assets 复制到 `app/data/files/`
- 引擎初始化时指定 NNUE 文件路径

### 5.4 难度映射

| 难度等级 | Pikafish Skill Level | Search Depth | 说明 |
|---------|---------------------|-------------|------|
| 入门 | 1 | 4 | 适合初学者 |
| 初级 | 3 | 6 | — |
| 中级 | 6 | 8 | — |
| 高级 | 10 | 12 | — |
| 大师 | 15 | 15 | — |
| 特级大师 | 20 | 20 | 棋力最强 |

### 5.5 参考项目

- `chinese_chess_mobile`（已上架 Google Play）：Pikafish ARM64 + UCI 协议 + Kotlin JNI 集成
- Pikafish 官方支持 NDK 交叉编译：`make build ARCH=armv8 COMP=ndk`

---

## 六、DeepSeek API 客户端设计

### 6.1 API 客户端接口

```kotlin
class DeepSeekApiClient(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String  // 从 EncryptedSharedPreferences 读取
) {
    // 普通调用（LLM 回退走棋用）
    suspend fun chat(prompt: String, temperature: Double = 0.15): String

    // 流式调用（嘲讽/自夸用）— 返回 Flow 实现真打字机效果
    suspend fun chatStream(
        prompt: String,
        temperature: Double = 0.85,
        systemPrompt: String? = null
    ): Flow<String>
}
```

### 6.2 Prompt 模板（移植自 Web 版）

#### 嘲讽 Prompt

```
你是一个中国象棋对手，你的性格设定如下：
{personality_config}

【当前局面分析】
{board_context_summary}
- 玩家刚走了：{last_move_desc}
- AI 回应了：{ai_move_desc}
- 局面状态：{situation_tags}

【上下文】
- 当前是第 {move_count} 回合
- AI 子力优势/劣势：{material_balance}

【去重约束】
你最近已经说过：
{recent_lines}
请确保这次的嘲讽和上面的完全不同。

请根据你的性格，对当前局面说一句嘲讽/点评的话。
要求：
1. 不超过50个字
2. 必须结合当前具体棋局内容
3. 关键局面（将军/吃子/绝杀）语气更强烈
4. 禁止人身攻击和脏话
```

#### 自夸 Prompt

```
你是一个中国象棋对手，你的性格设定如下：
{personality_config}

【当前局面分析】
- 你刚才走了一步妙手：{ai_move_desc}
- 这步棋的效果：{situation_tags}

请根据你的性格，自夸一下这步棋。
要求：
1. 不超过40个字
2. 符合你的性格设定
3. 可以得意，但不要太过分
```

#### LLM 走棋 Prompt（回退用）

```
你是一个中国象棋AI。当前棋盘状态如下，棋盘为10行(row 0-9)×9列(col 0-8)：

棋盘二维数组（row 0 = 黑方底线，row 9 = 红方底线）：
{board_array}

历史着法（row,col->row,col 格式）：
{move_history}

请分析局面，给出最优的下一步棋。

【重要】必须严格按以下格式回复，不要任何额外文字：
MOVE: <fromRow>,<fromCol>-<toRow>,<toCol>

示例：红方炮从(7,1)平到(7,4) → MOVE: 7,1-7,4
注意：你执{color}方。只输出一行 MOVE: 指令。
```

### 6.3 嘲讽/自夸触发机制

| 局面类型 | 触发权重 | 说明 |
|---------|---------|------|
| 丢大子（车/炮/马被吃） | ×2.0 | AI 吃掉玩家大子，嘲讽拉满 |
| 将军 | ×2.0 | 将军时刻，必须补刀 |
| 绝杀/将杀 | ×3.0 | 赢了必须嘲讽，优先级最高 |
| 妙手（AI 走出高质量着法） | ×1.5 | AI 自夸一下 |
| 玩家将军 AI | ×1.5 | AI 被将也要嘴硬 |
| 兑子 | ×1.2 | 交换棋子时点评 |
| 日常走棋 | ×1.0 | 正常权重 |

最终触发概率 = min(baseFrequency × 局面权重, 1.0)

### 6.4 嘲讽去重

- 维护最近 20 条已用嘲讽的原文
- 每次生成时将已用记录注入 Prompt
- 如生成内容与已用记录相似度过高（编辑距离 < 30%），重新生成一次
- 嘲讽和自夸各自独立去重缓存

### 6.5 关键参数（代码硬编码，不可通过配置修改）

| 参数 | 值 | 说明 |
|------|-----|------|
| 棋步 AI 温度 | 0.15 | LLM 回退走棋时 |
| 嘲讽 AI 温度 | 0.85 | 嘲讽/自夸生成 |
| LLM 走棋最大重试 | 3 次 | 非法着法重试 |
| LLM 重试后保底 | MoveGenerator 随机 | 3 次失败后 |
| 嘲讽去重 | 最近 20 条 | 原文存储 |
| 自夸基础概率 | 40% | 吃大子 ×2, 绝杀必发 |

---

## 七、API Key 安全方案

### 7.1 加密存储

| 维度 | 方案 |
|------|------|
| 存储方式 | AndroidX Security `EncryptedSharedPreferences` |
| 加密算法 | AES-256-GCM（值）+ AES-256-SIV（键） |
| 密钥管理 | Android Keystore 系统级密钥库（硬件隔离） |
| 密钥绑定 | 绑定到设备，不可导出 |

### 7.2 ApiKeyStore 接口

```kotlin
class ApiKeyStore(context: Context) {
    fun saveApiKey(key: String)
    fun getApiKey(): String?
    fun clearApiKey()
    fun hasApiKey(): Boolean
}
```

### 7.3 安全性评估

| 威胁场景 | 风险等级 | 缓解措施 |
|---------|---------|---------|
| APK 反编译提取 Key | **无风险** | APK 内不含 Key |
| 中间人攻击 | 低 | DeepSeek API 强制 HTTPS |
| Root 设备读取 | 中低 | EncryptedSharedPreferences + Keystore 硬件隔离 |
| 设备备份泄露 | 低 | `allowBackup="false"` |
| 屏幕共享/截图 | 低 | API Key 输入框使用 passwordVisualTransformation |
| Key 过期/失效 | 低 | 设置页可随时更新 |

### 7.4 用户体验流程

1. **首次启动**：显示 API Key 输入页面，附带获取 Key 的链接
2. **输入验证**：点击"验证"按钮，发送轻量测试请求
3. **验证成功**：加密存储，进入主界面
4. **验证失败**：显示错误信息，允许重试
5. **后续启动**：自动读取存储的 Key，直接进入主界面
6. **设置页面**：可随时查看（掩码）、更新、删除 Key
7. **Key 失效时**：游戏正常进行（Pikafish 本地），嘲讽显示"API Key 无效"

---

## 八、UI 设计

### 8.1 项目结构

```
app/src/main/
├── java/com/hualao/qiwang/
│   ├── model/                          # 数据模型
│   │   ├── Board.kt
│   │   ├── Piece.kt
│   │   ├── Move.kt
│   │   ├── Position.kt
│   │   ├── Side.kt
│   │   ├── GameStatus.kt
│   │   └── GameSession.kt
│   ├── engine/                         # 规则引擎
│   │   ├── MoveValidator.kt
│   │   ├── MoveGenerator.kt
│   │   └── CheckDetector.kt
│   ├── ai/                             # AI 引擎
│   │   ├── PikafishEngine.kt
│   │   ├── DeepSeekApiClient.kt
│   │   ├── PromptBuilder.kt
│   │   └── TrashTalkTrigger.kt
│   ├── data/                           # 本地存储
│   │   ├── ApiKeyStore.kt
│   │   ├── PersonalityManager.kt
│   │   └── personalities.json
│   ├── ui/                             # Compose UI
│   │   ├── screen/
│   │   │   ├── ApiKeySetupScreen.kt
│   │   │   ├── GameScreen.kt
│   │   │   └── SettingsScreen.kt
│   │   ├── component/
│   │   │   ├── ChessBoardCanvas.kt
│   │   │   ├── ChessBoardGesture.kt
│   │   │   ├── TrashTalkPanel.kt
│   │   │   ├── ControlPanel.kt
│   │   │   ├── PersonalitySelector.kt
│   │   │   └── MoveHistoryPanel.kt
│   │   └── theme/
│   │       └── Theme.kt
│   ├── viewmodel/
│   │   └── GameViewModel.kt
│   └── MainActivity.kt
├── cpp/                                # NDK 原生代码
│   ├── pikafish_jni.cpp
│   └── CMakeLists.txt
├── jniLibs/
│   └── arm64-v8a/
│       └── libpikafish.so
├── assets/
│   └── pikafish.nnue
└── res/
    └── ...
```

### 8.2 组件设计

#### ChessBoardCanvas.kt

- Compose Canvas 绘制 10×9 棋盘
- 棋盘线、楚河汉界、九宫斜线
- 32 枚棋子（圆形 + 汉字，红方楷体红色，黑方楷体黑色）
- 选中棋子高亮，合法目标位半透明圆点
- 最后一步走棋高亮标记
- 走子动画（Compose Animation，300ms 过渡）

#### ChessBoardGesture.kt

- 触摸选子：点击己方棋子，高亮 + 显示合法走法
- 触摸走子：点击合法目标位，执行走棋
- 触摸坐标 → 棋盘坐标转换

#### TrashTalkPanel.kt

- LazyColumn 展示嘲讽/自夸历史
- 流式文字逐 token 追加（真打字机效果）
- 嘲讽气泡（紫色）vs 自夸气泡（金色）
- 自动滚动到底部

#### ControlPanel.kt

- 新局按钮：重置棋盘
- 悔棋按钮：撤回玩家+AI 各一步
- 认负按钮：AI 胜利
- 难度选择：6 级（映射到 Pikafish Skill Level）

#### PersonalitySelector.kt

- LazyRow 横向卡片选择
- 5 种性格：头像 + 名称 + 描述
- 点击切换性格

#### ApiKeySetupScreen.kt

- API Key 输入框（密码模式，掩码显示）
- 验证按钮
- 获取 Key 的链接（DeepSeek 开放平台）
- 验证状态提示

#### SettingsScreen.kt

- API Key 管理（查看掩码/更新/删除）
- 难度设置
- 嘲讽频率调节
- 关于页面

### 8.3 布局方案

移动端竖屏布局：

```
┌──────────────────────────────┐
│    PersonalitySelector        │ ← 顶部：性格卡片横排
├──────────────────────────────┤
│                              │
│      ChessBoardCanvas         │ ← 中央：棋盘
│      (Compose Canvas)         │
│                              │
├──────────────────────────────┤
│    ControlPanel               │ ← 控制栏：新局/悔棋/认负/难度
├──────────────────────────────┤
│    TrashTalkPanel             │ ← 底部：嘲讽/自夸面板
│    (流式文字 + 气泡)           │   (可展开/收起)
└──────────────────────────────┘
```

---

## 九、性格配置设计

### 9.1 配置文件（personalities.json）

```json
[
  {
    "id": "toxic-master",
    "name": "毒舌大师",
    "avatar": "😈",
    "description": "嘴比刀子还狠，你每步棋都能挑出毛病",
    "systemPrompt": "你是一个棋艺高超但嘴巴极毒的中国象棋高手...",
    "trashTalkFrequency": 0.8,
    "speakingStyle": "尖酸刻薄，喜欢反问",
    "exampleLines": ["就这水平还敢跟我下？", "你这步棋是闭着眼下的吧？"]
  },
  {
    "id": "elegant-gentleman",
    "name": "优雅绅士",
    "avatar": "🎩",
    "description": "表面彬彬有礼，实则阴阳怪气大师",
    "systemPrompt": "你是一位举止优雅但喜欢阴阳怪气的中国象棋对手...",
    "trashTalkFrequency": 0.5,
    "speakingStyle": "表面客气，实则阴阳怪气",
    "exampleLines": ["有趣的走法……如果是初学者的话。", "您的勇气远远超过了您的棋艺。"]
  },
  {
    "id": "chuunibyou",
    "name": "中二少年",
    "avatar": "⚔️",
    "description": "每下一步棋都要喊出招式名",
    "systemPrompt": "你是一个沉醉于自己世界中二病晚期的中国象棋少年...",
    "trashTalkFrequency": 0.9,
    "speakingStyle": "中二、热血、喜欢给棋步起炫酷的名字",
    "exampleLines": ["见识一下我的终极奥义——「暗黑車輪斬」！", "凡人，你的棋力连我封印前的十分之一都不如。"]
  },
  {
    "id": "zen-master",
    "name": "禅意大师",
    "avatar": "🧘",
    "description": "下棋五分钟，讲哲理两小时",
    "systemPrompt": "你是一位看破红尘的禅意象棋大师...",
    "trashTalkFrequency": 0.4,
    "speakingStyle": "禅意、说教、喜欢引用不存在的名言",
    "exampleLines": ["车行直线，人生却从无直路可走。", "棋盘如人生，你的每一步都在暴露你的焦虑。"]
  },
  {
    "id": "silent-killer",
    "name": "沉默杀手",
    "avatar": "🗿",
    "description": "几乎不说话，但开口就是暴击",
    "systemPrompt": "你是一个沉默寡言但棋力极强的象棋AI...",
    "trashTalkFrequency": 0.15,
    "speakingStyle": "极度简洁，不超过10个字，一击必杀",
    "exampleLines": ["你输了。", "三步之内。", "还要继续吗？"]
  }
]
```

---

## 十、开发计划（里程碑）

### Phase 0：Android 项目搭建（1.5 天）

| 任务 | 产出 |
|------|------|
| Gradle 项目初始化（Kotlin DSL + Compose + NDK 配置） | build.gradle.kts |
| 依赖配置（Compose BOM / OkHttp / DataStore / Security-crypto / Coroutines） | dependencies |
| 项目包结构 `com.hualao.qiwang.*` 分层 | 目录结构 |
| 最低/目标 SDK 配置（minSdk=26, targetSdk=36） | 配置文件 |
| 签名配置（debug + release keystore） | signingConfigs |
| ProGuard 规则（OkHttp / Kotlin 反射） | proguard-rules.pro |
| Material 3 主题 | Theme.kt |
| MainActivity + Compose 脚手架 | 可运行的空 App |

### Phase 1：规则引擎移植（3 天）

| 任务 | 原始行数 | 产出 |
|------|---------|------|
| Board.kt（棋盘状态、applyMove、createInitial） | 197 | Board.kt |
| Position.kt + Move.kt + Side.kt + Piece.kt + PieceType.kt + GameStatus.kt | 170 | 6 个 data class / enum |
| MoveValidator.kt（7 种棋子走法验证） | 226 | MoveValidator.kt |
| MoveGenerator.kt（全部合法着法生成） | 227 | MoveGenerator.kt |
| CheckDetector.kt（将军/将杀/困毙检测） | 79 | CheckDetector.kt |
| 单元测试（每种棋子 5+ 用例） | - | 测试覆盖率 > 80% |
| FEN 生成 + ICCS 坐标互转 | - | FenConverter.kt |

### Phase 2：Pikafish NDK 集成（3 天）

| 任务 | 产出 |
|------|------|
| 交叉编译 Pikafish ARM64（或下载预编译版） | libpikafish.so |
| JNI Bridge C++（pipe + fork, stdin/stdout 重定向） | pikafish_jni.cpp + CMakeLists.txt |
| PikafishEngine.kt（UCI 通信封装） | PikafishEngine.kt |
| NNUE 文件管理（assets → internal storage 复制） | NnueManager.kt |
| 引擎线程管理（IO 线程，避免阻塞 UI） | CoroutineScope |
| 难度映射（6 级 → Skill Level + Depth） | 难度配置 |
| 集成测试（验证引擎返回 bestmove） | 测试用例 |

### Phase 3：DeepSeek API 客户端（2 天）

| 任务 | 产出 |
|------|------|
| DeepSeekApiClient.kt（OkHttp 封装，普通 + 流式） | DeepSeekApiClient.kt |
| PromptBuilder.kt（移植嘲讽/自夸/走棋 Prompt 模板） | PromptBuilder.kt |
| 流式响应解析（DeepSeek SSE 流 → Flow<String>） | 流式解析 |
| 去重缓存管理（recentTrashTalks / recentSelfPraises） | DedupManager.kt |
| 错误处理（Key 无效/限流/网络超时） | 错误处理 |
| 棋子中文描述（pieceToChinese / describeMove） | PieceDescriptor.kt |

### Phase 4：API Key 安全管理（1 天）

| 任务 | 产出 |
|------|------|
| ApiKeyStore.kt（EncryptedSharedPreferences 封装） | ApiKeyStore.kt |
| ApiKeySetupScreen.kt（首次启动 Key 输入页面） | ApiKeySetupScreen.kt |
| API Key 验证逻辑（调用 DeepSeek 轻量接口） | 验证逻辑 |
| SettingsScreen.kt（Key 管理：查看掩码/更新/删除） | SettingsScreen.kt |
| 首次启动判断逻辑（无 Key → SetupScreen） | 路由逻辑 |
| AndroidManifest 配置（allowBackup=false） | Manifest 配置 |

### Phase 5：游戏逻辑移植（2 天）

| 任务 | 原始文件 | 产出 |
|------|---------|------|
| GameViewModel.kt（走棋流程、AI 应着、胜负判定） | GameService.java (298行) | GameViewModel.kt |
| GameSession.kt（对局状态 data class） | GameSession (内部类) | GameSession.kt |
| TrashTalkTrigger.kt（嘲讽/自夸触发概率逻辑） | shouldTrashTalk / shouldSelfPraise | TrashTalkTrigger.kt |
| PersonalityManager.kt（性格配置加载和切换） | PersonalityService.java | PersonalityManager.kt |
| personalities.json（5 种性格配置） | personalities.yaml | personalities.json |
| 悔棋逻辑（undo: 撤回玩家+AI 各一步） | undo() | 悔棋逻辑 |
| Flow 集成（Kotlin Flow 逐字推送嘲讽） | SseService 替代 | Flow 集成 |

### Phase 6：UI 开发（8 天）

| 任务 | 工作量 | 产出 |
|------|--------|------|
| ChessBoardCanvas.kt（Compose Canvas 棋盘/棋子/坐标/河界/九宫） | 3 天 | ChessBoardCanvas.kt |
| ChessBoardGesture.kt（触摸选子/走子，高亮合法位置） | 1 天 | ChessBoardGesture.kt |
| 走子动画（Compose Animation 平滑移动） | 0.5 天 | 动画 |
| TrashTalkPanel.kt（流式文字 + 气泡 UI + 自动滚动） | 2 天 | TrashTalkPanel.kt |
| ControlPanel.kt（新局/悔棋/认负/难度） | 0.5 天 | ControlPanel.kt |
| PersonalitySelector.kt（卡片选择 + 头像/描述） | 0.5 天 | PersonalitySelector.kt |
| MoveHistoryPanel.kt（着法列表） | 0.5 天 | MoveHistoryPanel.kt |

### Phase 7：测试 & 联调（2.5 天）

| 任务 | 产出 |
|------|------|
| 规则引擎单元测试 | 各种棋子走法、将军/将杀/困毙 |
| Pikafish 引擎测试 | 引擎初始化、bestmove 返回、坐标转换 |
| DeepSeek API 测试 | 普通/流式调用、错误处理 |
| API Key 流程测试 | 首次输入、验证、更新、删除 |
| UI 集成测试 | 走棋流程、嘲讽显示、设置流程 |
| Android 16 (API 36) 适配 | 边缘到边缘、16KB 页大小 |
| 性能优化 | 棋盘渲染帧率、引擎线程优先级 |

### 总计

| 阶段 | 工作量 |
|------|--------|
| Phase 0 | 1.5 天 |
| Phase 1 | 3 天 |
| Phase 2 | 3 天 |
| Phase 3 | 2 天 |
| Phase 4 | 1 天 |
| Phase 5 | 2 天 |
| Phase 6 | 8 天 |
| Phase 7 | 2.5 天 |
| **总计** | **~23 天（约 4.5-5 周）** |

---

## 十一、SOP 开发流程

本项目遵循标准 SOP 开发流程，由软件开发团队协作完成：

```
用户需求 → 产品经理(PRD) → 架构师(系统设计+任务分解) → 工程师(代码实现) → QA工程师(测试验证)
```

### 11.1 各阶段对应的 SOP 角色

| Phase | 主要角色 | 产出 |
|-------|---------|------|
| Phase 0 | 工程师 | 项目脚手架 |
| Phase 1 | 工程师 → QA | 规则引擎 + 单元测试 |
| Phase 2 | 工程师 → QA | Pikafish 集成 + 测试 |
| Phase 3 | 工程师 → QA | DeepSeek 客户端 + 测试 |
| Phase 4 | 工程师 → QA | API Key 管理 + 测试 |
| Phase 5 | 工程师 → QA | 游戏逻辑 + 测试 |
| Phase 6 | 工程师 → QA | UI 开发 + 集成测试 |
| Phase 7 | QA | 全量测试 + Android 16 适配 |

### 11.2 增量开发支持

当用户在已有项目基础上提出变更需求时：

1. **产品经理**：基于旧 PRD + 新需求生成增量 PRD
2. **架构师**：基于旧设计 + 增量 PRD 生成增量设计 + 增量任务列表
3. **工程师**：修改已有代码 + 新增代码，最小变更原则
4. **QA**：全量回归测试 + 新功能测试

---

## 十二、风险清单

| # | 风险 | 级别 | 影响 | 缓解措施 |
|---|------|------|------|---------|
| 1 | Pikafish 交叉编译失败 | 中 | 引擎不可用 | 下载 `chinese_chess_mobile` 预编译版；或使用其 JNI 方案 |
| 2 | NNUE 文件体积过大 | 低 | APK 体积大 | pikafish.nnue 约 40MB，可接受；或首次启动时下载 |
| 3 | DeepSeek API 流式响应不稳定 | 低 | 嘲讽中断 | OkHttp 重试 + 超时处理 + 降级为非流式 |
| 4 | 用户输入无效 API Key | 低 | 嘲讽不可用 | 输入时验证 + 游戏正常进行（引擎本地运行） |
| 5 | Android Keystore 不可用 | 极低 | Key 存储降级 | minSdk=26，Keystore 在 API 23+ 可用 |
| 6 | Compose Canvas 棋盘性能 | 低 | 渲染卡顿 | Compose Canvas 性能优秀，参考项目用 View Canvas 已够用 |
| 7 | 引擎线程阻塞 UI | 低 | 界面卡顿 | 引擎在 IO 线程运行，UI 线程仅接收结果 |
| 8 | Android 16 (API 36) 适配 | 低 | 兼容性 | targetSdk=36，关注边缘到边缘、16KB 页大小 |

---

## 十三、扩展想法（未来迭代）

- **对局回放**：保存对局记录，支持回放
- **残局挑战**：预设经典残局，解残局模式
- **语音嘲讽**：TTS 把嘲讽文本读出来
- **自定义性格**：用户自定义 System Prompt
- **在线对战**：WebSocket 实现多人对战（当前范围外）
- **每日挑战**：每天一个固定残局

---

*本文档基于 D:\workspace\xiangqi 项目代码分析 + B+ 评估方案 + 标准 SOP 开发流程生成。*

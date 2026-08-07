# 话唠棋王 Android 版 — Agent 规范

> 适用项目：中国象棋「话唠棋王」Android 原生 App（B+ 全本地化方案）  
> 更新日期：2026-08-07  
> 源项目：`D:\workspace\xiangqi`（Web 版，已完成 Phase 0~8）

---

## 一、项目背景

本项目目标是开发一款**单机人机对战**中国象棋 **Android App**。AI 由 Pikafish 引擎驱动走棋，DeepSeek 大模型驱动嘲讽/自夸。全部逻辑本地化运行，无后端服务器。DeepSeek API Key 由用户首次启动时手动输入，加密存储于本地。

### 1.1 文档体系

| 文件 | 用途 | 读取时机 |
| --- | --- | --- |
| `agent.md` | **开发执行规范（本文）** — 约束和红线 | 每次任务开始时 |
| `development-plan.md` | **功能设计文档** — 架构、API、数据模型、Prompt 模板、文件结构、开发计划 | 需要了解具体设计细节时 |
| `PROGRESS.md` | **开发进度（唯一真相源）** | 每次任务开始必读 |

### 1.2 阅读优先级

```
agent.md → PROGRESS.md → development-plan.md（按需）
```

**原则**：agent.md 管"做不做"和"怎么做对"，development-plan.md 管"怎么做"的细节。

### 1.3 与源项目的关系

本项目基于 `D:\workspace\xiangqi`（Web 版）迁移而来。源项目的**业务逻辑**（规则引擎、Prompt 模板、性格配置、嘲讽触发机制）作为蓝本参考，但**全部代码使用 Kotlin 重写**，不直接复用 Java/Vue 代码。

| 可参考 | 不可直接复用 |
|-------|------------|
| 规则引擎逻辑（7 种棋子走法） | Java 语法 → Kotlin 语法 |
| Prompt 模板内容 | Spring AI 调用 → OkHttp 直连 |
| 性格配置（5 种） | YAML → JSON |
| 嘲讽/自夸触发机制 | SseEmitter → Kotlin Flow |
| UCI 通信协议 | ProcessBuilder → JNI |
| 坐标体系 {row, col} | — |

---

## 二、核心工作流

每次收到开发任务时，按以下三步执行：

### 第一步：项目对齐（理解现状）

1. **必读 PROGRESS.md**：确认当前开发阶段、已完成模块、进行中的任务
2. **按需读 development-plan.md**：根据本次任务涉及的模块，读取对应章节
3. **输出「现状对齐」**（限 200 字）：说明当前完成情况、本次任务与现有进度的关系

### 第二步：需求落地（拆分与实施）

1. **拆分**：将任务拆为原子化子任务，每个子任务改动范围控制在 1-3 个文件
2. **对齐文档**：确认每个子任务对应 development-plan.md 中的哪个章节，以文档中的设计为准
3. **实施**：按子任务依次开发，每个完成后验证并更新 PROGRESS.md

### 第三步：收尾

1. 更新 PROGRESS.md 整体状态
2. 如果任务影响了用户可感知的行为，更新 README.md
3. 按照「六、Git 提交机制」执行最终提交和推送

---

## 三、PROGRESS.md 规范

### 3.1 PROGRESS.md 结构

PROGRESS.md 是项目进度的**唯一真相源**，任何关于"做到哪了"的问题，答案都在 PROGRESS.md 中。

结构包含：
- **总体进度**：8 个 Phase 的模块状态表（⬜/🔄/✅/❌）
- **依赖版本**：所有技术组件的版本信息
- **当前任务**：正在执行的子任务详情
- **已完成记录**：按时间倒序记录每次完成的任务摘要
- **偏离记录**：实现与 development-plan.md 有偏差时的记录
- **阶段验收标准**：每个 Phase 的验收 checklist
- **源项目参考**：Web 版文件 → Kotlin 文件的移植状态追踪

### 3.2 子 PROGRESS.md 拆分规则

当单个阶段子任务超过 5 个时，拆分为独立文件：

```
PROGRESS.md                           ← 总进度
PROGRESS/
├── PROGRESS-phase0-setup.md          ← Phase 0：项目搭建
├── PROGRESS-phase1-engine.md         ← Phase 1：规则引擎
├── PROGRESS-phase2-pikafish.md       ← Phase 2：Pikafish NDK
├── PROGRESS-phase3-deepseek.md       ← Phase 3：DeepSeek 客户端
├── PROGRESS-phase4-apikey.md         ← Phase 4：API Key 管理
├── PROGRESS-phase5-game-logic.md     ← Phase 5：游戏逻辑
├── PROGRESS-phase6-ui.md             ← Phase 6：UI 开发
└── PROGRESS-phase7-test.md           ← Phase 7：测试联调
```

**规则**：
1. 子 PROGRESS.md 格式与主 PROGRESS.md 的「当前任务」部分一致
2. 主 PROGRESS.md「当前任务」指向对应的子 PROGRESS.md
3. 子文件完成后，在主 PROGRESS.md 中更新对应模块状态并归档

---

## 四、场景化调整

### 场景 A：按阶段开发（推荐，默认模式）

按 development-plan.md 第十章的 Phase 0~7 计划推进。

**额外约束**：
- Phase 有依赖时，先完成被依赖 Phase
- 每完成一个 Phase，验收标准全部通过才能进入下一 Phase
- Phase 完成后在 PROGRESS.md 标记状态

**Phase 依赖关系**：
```
Phase 0 (项目搭建)
  ├→ Phase 1 (规则引擎) ──┐
  │                       ├→ Phase 5 (游戏逻辑) ──┐
  ├→ Phase 2 (Pikafish) ──┘                       │
  │                                               ├→ Phase 6 (UI) ──→ Phase 7 (测试)
  ├→ Phase 3 (DeepSeek) ──┐                       │
  │                       ├→ Phase 5 (游戏逻辑) ──┘
  └→ Phase 4 (API Key) ───┘
```

**可并行**：Phase 1 / 2 / 3 / 4 之间无强依赖，可并行开发。Phase 5 依赖 1+2+3+4。

### 场景 B：Bug 修复 / 小范围调整

**额外约束**：
1. 最小化代码改动，优先修复而非重构
2. 修复后在 PROGRESS.md「偏离记录」记录「现象」「根因」「修复方案」
3. 补充或更新对应的单元测试

### 场景 C：设计偏离（实现与 development-plan.md 不一致）

**额外约束**：
1. 先在 PROGRESS.md「偏离记录」中记录：原设计、偏离原因、新方案
2. 评估偏离对其他模块的影响
3. 如果偏离是永久性的，更新 development-plan.md

### 场景 D：新增功能（超出 development-plan.md 范围）

**额外约束**：
1. 先写功能设计概要，补充到 development-plan.md 的「扩展想法」章节
2. 在 PROGRESS.md 增加该功能的状态行
3. 评估对已有模块的影响后再实施

---

## 五、关键注意事项

### 5.1 四条核心原则（每次改代码前自检）

1. **本地化吗？** — 不需要后端服务器、不需要多用户、不需要登录鉴权
2. **棋盘先更新了吗？** — 玩家走棋后棋盘立即更新，AI 走棋后立即更新，嘲讽/自夸异步 Flow 推送
3. **坐标是 `(row, col)` 吗？** — 禁止任何其他表示（字母混排、中文着法）
4. **够简单吗？** — MVP 阶段拒绝过度设计

### 5.2 坐标红线不可触碰

全项目唯一坐标规范为 `Position(row: Int, col: Int)`，禁止字母混排、中文着法。偏离即 bug。

```
棋盘坐标系：
    row=0 = 黑方底线
    row=9 = 红方底线
    col=0 = 最左列
    col=8 = 最右列

存储和传输始终使用此坐标系不变。
Compose Canvas 渲染时根据视角做 row 翻转，但坐标值不变。
```

### 5.3 关键架构决策不可推翻（已定论，不讨论）

| 决策 | 内容 | 说明 |
|------|------|------|
| 无后端 | 全部逻辑在 App 内 | B+ 方案核心 |
| API Key 用户自管 | 首次输入 → EncryptedSharedPreferences 加密存储 | 安全评分 8/10 |
| 走棋引擎 | Pikafish ARM64 原生库 + JNI | 参考 chinese_chess_mobile |
| 嘲讽推送 | Kotlin Flow（替代 SSE） | 真打字机效果 |
| AI 温度 | 棋步 0.15，嘲讽 0.85 | 代码硬编码 |
| AI 非法着法 | 重试 3 次 → MoveGenerator 随机保底 | 仅 LLM 回退时 |
| 嘲讽去重 | 最近 20 条原文存储 | 与自夸独立 |
| API Key 失效降级 | 游戏照常下，嘲讽不说话 | Pikafish 本地运行 |

### 5.4 API Key 安全红线

1. **APK 内不含任何 API Key** — 反编译也拿不到
2. **存储必须用 EncryptedSharedPreferences** — 禁止普通 SharedPreferences
3. **输入框必须用密码掩码** — passwordVisualTransformation
4. **allowBackup 必须为 false** — 禁止备份泄露
5. **Key 失效不崩溃** — 降级为无嘲讽模式

### 5.5 文档同步

代码改动影响接口、数据格式时，同步更新 development-plan.md。

### 5.6 测试先行

单元测试与实现代码同期完成，不要留到"以后补测试"。

### 5.7 交互确认

涉及接口变更、架构调整的任务，先输出方案等确认后再实施。

### 5.8 引擎线程安全

Pikafish 引擎必须在 IO 线程（`Dispatchers.IO`）运行，禁止在主线程（`Dispatchers.Main`）调用。UI 线程仅接收结果。

### 5.9 Compose 性能

- 棋盘 Canvas 绘制避免在 `draw` 回调中创建对象
- 状态更新使用 `StateFlow`，避免不必要的重组
- 走子动画使用 Compose Animation API，不用 `Handler.postDelayed`

---

## 六、Git 提交机制

> **核心原则**：完成一个任务或一组相关子任务后，立即更新 PROGRESS.md 并提交推送。不积攒、不拖延。  
> **远程仓库**：`https://github.com/cccuuuhhh/xiangqiapp.git`  
> **铁律**：每个功能完成或优化完成 → 更新 PROGRESS.md（时间精确到秒）→ git commit → git push。三步缺一不可。

### 6.0 进度更新与提交流程（强制执行）

每次功能完成或优化完成后，**必须**按以下顺序执行：

```bash
# 1. 获取当前时间（精确到秒）
date '+%Y-%m-%d %H:%M:%S'

# 2. 更新 PROGRESS.md：
#    - 总体进度表中对应项状态改为 ✅，完成时间填入精确到秒的时间戳
#    - 「已完成记录」新增一条，标题含时间戳
#    - 如果是优化项，同时更新「未来优化大类」对应行的状态和完成时间
#    - 更新文件顶部的「最后更新」时间戳

# 3. 提交并推送
git add -A
git commit -m "[phaseX-module] 简短描述"
git push origin main
```

**时间戳格式**：`YYYY-MM-DD HH:MM:SS`（如 `2026-08-07 14:23:05`），通过 `date '+%Y-%m-%d %H:%M:%S'` 获取，**禁止手动估算**。

**禁止的行为**：
- ❌ 完成功能后不更新 PROGRESS.md 就提交
- ❌ 更新了 PROGRESS.md 但不提交推送
- ❌ 提交了但不推送（本地积攒多个 commit）
- ❌ 时间戳手动填写（必须用 `date` 命令获取）

### 6.1 提交时机判断

#### 规则 A：单子任务提交

单个子任务完成后**立即提交**。子任务定义：一个能独立验证、独立回滚的最小改动单元。

#### 规则 B：相关性合并提交

当连续完成的多个子任务满足以下**任意一条**时，可以合并为一次提交：

| 条件 | 判断标准 | 示例 |
| --- | --- | --- |
| **同一文件** | 多个子任务改动了相同的文件 | 子任务 A 和 B 都改了 `MoveValidator.kt` |
| **同一模块** | 多个子任务属于同一个模块，改动文件不超过 3 个 | Phase 1 的 Board/Piece/Position/Move |
| **因果关系** | 子任务 B 是子任务 A 的直接延续 | "定义接口" → "实现接口" |
| **子任务过小** | 单个子任务改动不足 10 行 | 修复一个 import、调整一个常量 |

**不满足以上任何条件时，不要合并提交。**

#### 规则 C：必须立即提交

以下情况**无论是否满足合并条件，必须立即提交**：

1. **PROGRESS.md 更新后**：进度变更必须落盘
2. **跨模块修改**：如同时改了引擎和 UI
3. **对话即将结束**：当前会话的最后一个子任务完成后
4. **破坏性改动**：修改了接口签名、数据模型、公共 API
5. **关键里程碑**：模块状态从 🔄 变为 ✅ 时

### 6.2 提交信息格式

```
[Phase X] 简短描述（一句话概括做了什么）

- 具体改动点 1
- 具体改动点 2
```

**Phase 标签使用**：`phase0`、`phase1-engine`、`phase2-pikafish`、`phase3-deepseek`、`phase4-apikey`、`phase5-logic`、`phase6-ui`、`phase7-test`、`docs`

**示例**：

```
[phase1-engine] 实现 MoveValidator 七种棋子走法校验

- 实现 validateKing/Advisor/Bishop/Knight/Rook/Cannon/Pawn
- 添加飞将检测、蹩脚判断、炮架识别
- 每种棋子 5+ 单元测试覆盖
```

```
[phase2-pikafish] 完成 Pikafish JNI 桥接和 UCI 通信

- 交叉编译 libpikafish.so (ARM64)
- 实现 JNI Bridge (pipe + fork 方案)
- PikafishEngine.kt UCI 通信封装
- NNUE 文件从 assets 复制到 internal storage
- 集成测试验证 bestmove 返回
```

```
[phase6-ui] 实现 Compose Canvas 棋盘绘制和触摸交互

- 10×9 棋盘 + 32 枚棋子 + 楚河汉界
- 选子高亮 + 合法走法指示器
- 走子动画 300ms 过渡
```

### 6.3 分支策略

```bash
main          ← 主分支，始终保持可运行状态
```

**规则**：
- 直接在 main 上开发、提交
- **每个 commit 必须保证代码可编译**（不允许多个 commit 中有 broken commit）
- **每个 commit 必须保证 App 可构建**（`./gradlew assembleDebug` 成功）

### 6.4 推送时机

| 时机 | 操作 | 强制等级 |
| --- | --- | --- |
| **每次 commit 后** | `git push origin main` | 🔴 强制 |
| **每个功能/优化完成后** | 更新 PROGRESS.md → commit → push | 🔴 强制 |
| **每次对话结束前** | 检查是否有未推送的 commit，有则 push | 🔴 强制 |
| **每日收工前** | `git push` + 确认远程已更新 | 🟡 建议 |

**原则**：本地不积攒超过 1 个未推送的 commit。提交和推送是一组不可分割的操作。

### 6.5 提交前检查清单

每次 `git commit` 前确认：

- [ ] 代码可编译（`./gradlew assembleDebug` 成功）
- [ ] 相关测试通过（`./gradlew test`）
- [ ] PROGRESS.md 已更新（子任务状态 + 改动文件）
- [ ] 不包含调试代码（`Log.d("DEBUG", ...)`、`println()` 等）
- [ ] 不包含敏感信息（API Key 等，确认无硬编码密钥）
- [ ] 不包含大文件（libpikafish.so / pikafish.nnue 使用 Git LFS 或不提交）

---

## 七、SOP 开发流程

本项目遵循标准 SOP（标准作业程序）开发流程，由软件开发团队协作完成：

```
用户需求 → 产品经理(PRD) → 架构师(系统设计+任务分解) → 工程师(代码实现) → QA工程师(测试验证)
```

### 7.1 团队成员

| 成员 | 职责 |
|------|------|
| 产品经理（许清楚） | 创建 PRD / 市场调研 |
| 架构师（高见远） | 系统架构设计 + 任务分解 |
| 工程师（寇豆码） | 批量编写代码 + 全局一致性审查 |
| QA 工程师（严过关） | 编写测试 + 智能路由判定 |

### 7.2 工作流路由

| 场景 | 判定条件 | 使用工作流 |
|------|---------|-----------|
| 小型需求 | 单页面/小工具/≤ 10 文件 | ⚡ 快速模式 |
| Bug 修复 | 明确 Bug，非新功能 | 🔧 BugFix 快捷路径 |
| 中大型需求 | 多模块/复杂交互/> 10 文件 | 🏗️ 标准 SOP |

### 7.3 质量关卡

- 工程师完成所有文件后必须通过**全局一致性审查**（IS_PASS: YES）
- QA 每轮测试后必须做出**智能路由判定**（Engineer/QA/NoOne）
- 最多 2 轮测试，2 轮仍不过则输出报告标注遗留问题

### 7.4 反馈回路

- QA 发现源码 Bug → 反馈给工程师修复
- 架构师发现 PRD 歧义 → 反馈给产品经理澄清
- 工程师发现设计问题 → 反馈给架构师修订

---

## 八、执行流程示例（以"开发 MoveValidator"为例）

1. AI 读取 PROGRESS.md → 确认当前在 Phase 1，MoveValidator 待开始
2. AI 读取 development-plan.md 第四章（规则引擎） → 对齐设计
3. AI 输出「现状对齐」：概述已完成模块、MoveValidator 的接口定义、与其他模块的依赖
4. AI 拆分子任务：
   - 子任务 1：实现 validateKing + validateAdvisor
   - 子任务 2：实现 validateBishop + validateKnight
   - 子任务 3：实现 validateRook + validateCannon + validatePawn
   - 子任务 4：编写单元测试（每种棋子 5+ 用例）
5. 按子任务依次开发，每个完成后更新 PROGRESS.md 并按规则提交
6. 全部完成后标记 MoveValidator 为 ✅

每个子任务完成后输出：`✅ 子任务X 已完成。PROGRESS.md 已更新。下一步：子任务Y。`

---

## 九、快速速查

| 问题 | 答案 |
|------|------|
| 从哪里开始？ | 先读 PROGRESS.md |
| 坐标格式 | `Position(row: Int, col: Int)`，row 0=黑方底线，row 9=红方底线 |
| 棋盘编码 | `rK`/`rA`/`rB`/`rN`/`rR`/`rC`/`rP`（红），`bK`/`bA`/`bB`/`bN`/`bR`/`bC`/`bP`（黑），`""`（空） |
| Pikafish 通信 | UCI 协议，通过 JNI pipe + fork |
| 嘲讽推送方式 | Kotlin Flow（替代 SSE） |
| 棋步 AI 温度 | 0.15（代码硬编码） |
| 嘲讽 AI 温度 | 0.85（代码硬编码） |
| AI 3 次非法后 | MoveGenerator 随机保底 |
| 嘲讽去重 | 最近 20 条原文存储 |
| API Key 存储 | EncryptedSharedPreferences (AES-256-GCM) |
| 需要后端吗？ | 不需要（B+ 全本地化） |
| 需要登录吗？ | 不需要 |
| API Key 从哪来？ | 用户首次启动手动输入 |
| API Key 失效怎么办？ | 游戏照常下，嘲讽不说话 |
| 引擎在哪个线程？ | IO 线程（Dispatchers.IO） |
| 提交信息格式 | `[phaseX-module] 简短描述` |
| minSdk / targetSdk | 26 / 36 (Android 8.0 / Android 16) |
| 开发语言 | Kotlin 2.0+ |
| UI 框架 | Jetpack Compose (BOM 2024.x) |

---

## 十、文档优先级声明

当 agent.md 与其他文档冲突时，以 agent.md 为准：

```
agent.md > development-plan.md > PROGRESS.md（进度事实除外）
```

- **agent.md**：管"做不做"和"怎么做对"（规范、红线、约束）
- **development-plan.md**：管"怎么做"的细节（架构、接口、数据模型）
- **PROGRESS.md**：管"做到哪了"（进度事实，唯一真相源）

PROGRESS.md 中的进度事实不可被其他文档覆盖。当进度与计划冲突时，以进度为准并记录偏离。

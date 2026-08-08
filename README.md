# 话唠棋王 (Trash-Talking Chess King)

> 一个不仅能赢你棋，还会用嘴炮嘲讽你的 AI 中国象棋 Android App

[![Release](https://img.shields.io/badge/Release-v1.0.0-brightgreen)](https://github.com/cccuuuhhh/xiangqiapp/releases/tag/v1.0.0)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-BOM%202024.12-purple)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ 功能亮点

- ♟️ **标准中国象棋规则** — 9 竖线 × 10 横线，7 种棋子（帅仕相馬車砲兵）走法校验，将军/将杀/困毙检测
- 🧠 **Pikafish AI 引擎** — 基于 Stockfish 象棋分支，ARM64 NDK 本地编译，零网络延迟对弈
- 🤖 **DeepSeek AI 嘲讽** — 5 种对战性格，实时流式输出嘲讽/自夸/点评
- 🔐 **API Key 安全** — EncryptedSharedPreferences AES-256-GCM 加密存储，Key 仅存本地
- 🎨 **Material 3 中国风** — 浅色/暗色双主题，Canvas 自定义棋盘渲染
- 📱 **Android 16 适配** — Edge-to-Edge, Predictive Back, 16KB 页对齐

## 🤖 AI 性格一览

| 性格 | 说话风格 | 嘲讽频率 | 代表语录 |
|------|---------|---------|---------|
| 😈 **毒舌大师** | 尖酸刻薄，喜欢反问 | 高 | "你这步棋，我奶奶都下不出来" |
| 🎩 **优雅绅士** | 表面客气，阴阳怪气 | 中 | "阁下这一步，颇有初学者之风范" |
| ⚔️ **中二少年** | 热血，给棋步起名 | 极高 | "看我的——黑暗降龙伏虎破阵式！" |
| 🧘 **禅意大师** | 说教，引用名言 | 低 | "棋如人生，你这一步，如同人生走错了路" |
| 🗿 **沉默杀手** | 不超过 10 个字 | 极低 | "菜。" |

## 🏗️ 技术架构

```
┌──────────────────────────────────────────┐
│                 UI 层                     │
│  Compose Canvas / Material 3 / 主题切换  │
├──────────────────────────────────────────┤
│             ViewModel 层                  │
│   对局流程 / 状态管理 / Flow 集成         │
├────────────────────┬─────────────────────┤
│     AI 对话层      │   游戏规则引擎       │
│  DeepSeek SSE 流式 │  MoveValidator      │
│  PromptBuilder     │  CheckDetector      │
│  DedupManager      │  MoveGenerator      │
│  TrashTalkTrigger  │  FenConverter       │
├────────────────────┼─────────────────────┤
│    Pikafish 引擎   │   数据安全层         │
│  JNI Bridge + UCI  │  EncryptedSP        │
│  C++17 NEON 优化   │  AES-256-GCM        │
└────────────────────┴─────────────────────┘
```

## 📂 项目结构

```
xiangqiapp/
├── app/
│   ├── src/main/
│   │   ├── cpp/                          # JNI 桥接层
│   │   │   ├── CMakeLists.txt
│   │   │   ├── pikafish_jni.cpp          # 4 个 native 方法
│   │   │   └── pikafish_wrapper.h/cpp    # 进程内 UCI 管道封装
│   │   ├── java/com/hualao/qiwang/
│   │   │   ├── model/                    # 模型层 (8 文件)
│   │   │   │   ├── Board.kt              # 10×9 棋盘 + applyMove/undoMove
│   │   │   │   ├── Piece.kt, PieceType.kt, Side.kt
│   │   │   │   ├── Position.kt, Move.kt
│   │   │   │   ├── GameStatus.kt, GameSession.kt
│   │   │   ├── engine/                   # 规则引擎 (4 文件)
│   │   │   │   ├── MoveValidator.kt      # 7 种棋子走法校验
│   │   │   │   ├── CheckDetector.kt      # 将军/将杀/困毙
│   │   │   │   ├── MoveGenerator.kt      # 合法着法生成
│   │   │   │   └── FenConverter.kt       # FEN + ICCS 坐标
│   │   │   ├── ai/                       # AI 层 (6 文件)
│   │   │   │   ├── PikafishEngine.kt     # UCI 引擎通信
│   │   │   │   ├── NnueManager.kt        # NNUE 权重管理
│   │   │   │   ├── DeepSeekApiClient.kt  # DeepSeek API
│   │   │   │   ├── PromptBuilder.kt      # 嘲讽 Prompt
│   │   │   │   ├── TrashTalkTrigger.kt   # 嘲讽触发策略
│   │   │   │   └── DedupManager.kt       # 去重缓存
│   │   │   ├── data/                     # 数据层 (2 文件)
│   │   │   │   ├── ApiKeyStore.kt        # 加密 Key 存储
│   │   │   │   └── PersonalityManager.kt # 性格配置
│   │   │   ├── viewmodel/                # ViewModel (1 文件)
│   │   │   │   └── GameViewModel.kt      # 走棋全流程
│   │   │   └── ui/                       # UI 层 (10 文件)
│   │   │       ├── theme/Theme.kt
│   │   │       ├── screen/
│   │   │       │   ├── GameScreen.kt
│   │   │       │   ├── ApiKeySetupScreen.kt
│   │   │       │   └── SettingsScreen.kt
│   │   │       └── component/
│   │   │           ├── ChessBoardCanvas.kt
│   │   │           ├── TrashTalkPanel.kt
│   │   │           ├── ControlPanel.kt
│   │   │           ├── PersonalitySelector.kt
│   │   │           └── MoveHistoryPanel.kt
│   │   ├── assets/
│   │   │   └── personalities.json        # 5 种性格配置
│   │   └── test/                          # 120+ 单元测试
│   └── build.gradle.kts
├── vendor/pikafish/                       # Pikafish C++ 源码 (~100 文件)
├── scripts/download_nnue.py               # NNUE 权重下载脚本
├── release/                               # 发布 APK
└── .github/
```

## 🚀 快速开始

### 环境要求

| 工具 | 版本 |
|------|------|
| Android Studio | Ladybug (2024.2+) 或更高 |
| Android SDK | API 36 |
| Android NDK | 27.0+ |
| JDK | 17+ |
| Gradle | 8.9+ |
| Kotlin | 2.0+ |

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/cccuuuhhh/xiangqiapp.git --recursive
cd xiangqiapp

# 2. 设置 Android SDK 路径（创建 local.properties）
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# 3. （可选）下载 NNUE 神经网络权重文件
python scripts/download_nnue.py

# 4. 构建 Debug APK
./gradlew assembleDebug

# 5. 构建 Release APK（需设置签名环境变量）
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=xiangqi
export KEY_PASSWORD=your_password
./gradlew assembleRelease
```

### 生成签名密钥（仅首次）

```bash
keytool -genkeypair -v -keystore app/xiangqi.keystore \
  -alias xiangqi -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=XiangqiApp, OU=Hualao, O=Hualao, L=Shenzhen, ST=Guangdong, C=CN"
```

## 📥 下载安装

前往 [GitHub Releases](https://github.com/cccuuuhhh/xiangqiapp/releases) 下载最新 APK 直接安装。

> **系统要求**：Android 8.0+ (API 26) / ARM64 架构 (arm64-v8a)

## 🔑 API Key 配置

首次启动 App 时输入你的 DeepSeek API Key。Key 会通过 EncryptedSharedPreferences (AES-256-GCM) 加密存储在本地，不会上传到任何服务器。

- 没有 Key？可以**跳过**，仅使用 Pikafish AI 对弈（无 AI 嘲讽对话）
- 可在「设置」页面随时添加/修改/删除 Key

## 🧪 测试

```bash
# 运行单元测试
./gradlew test

# 规则引擎测试 (85+)：Board / MoveValidator / CheckDetector / MoveGenerator / FenConverter
# AI 层测试 (18)：DedupManager / PromptBuilder
```

## 📋 开发进度

| Phase | 模块 | 状态 |
|-------|------|------|
| Phase 0 | Android 项目搭建 | ✅ |
| Phase 1 | 规则引擎 + 测试 | ✅ |
| Phase 2 | Pikafish JNI 集成 | ✅ |
| Phase 3 | DeepSeek AI 客户端 | ✅ |
| Phase 4 | API Key 安全存储 | ✅ |
| Phase 5 | 游戏逻辑 + ViewModel | ✅ |
| Phase 6 | UI 全部组件 | ✅ |
| Phase 7 | 测试 + Android 16 适配 | ✅ |

详见 [PROGRESS.md](PROGRESS.md)

## 📜 License

MIT License — 详见 [LICENSE](LICENSE)

### 第三方组件

- [Pikafish](https://github.com/pikafish/Pikafish) — GPLv3，中国象棋 AI 引擎
- [DeepSeek](https://deepseek.com/) — AI 对话 API
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Apache 2.0

---

<sub>Made with ❤️ for Chinese Chess lovers</sub>

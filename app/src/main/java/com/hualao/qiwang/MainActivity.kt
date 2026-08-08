package com.hualao.qiwang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hualao.qiwang.data.ApiKeyStore
import com.hualao.qiwang.ui.screen.ApiKeySetupScreen
import com.hualao.qiwang.ui.screen.GameScreen
import com.hualao.qiwang.ui.screen.SettingsScreen
import com.hualao.qiwang.ui.theme.XiangqiTheme
import com.hualao.qiwang.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 话唠棋王 — Android 入口 Activity
 *
 * B+ 方案：全本地化，无后端服务器
 * - DeepSeek API Key 由用户首次启动时手动输入
 * - Pikafish 引擎本地运行（NDK/JNI）
 * - Jetpack Compose 全声明式 UI
 * - Android 16 (API 36) 完整适配：edge-to-edge + Predictive Back + 16KB page size
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15+ 强制 edge-to-edge，支持 Predictive Back Gesture
        enableEdgeToEdge()
        setContent {
            XiangqiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

/**
 * 页面枚举：API Key 设置 → 游戏主界面，设置页作为一个状态。
 */
private enum class AppScreen { SETUP, GAME, SETTINGS }

@Composable
fun App() {
    val context = LocalContext.current
    val keyStore = remember { ApiKeyStore(context) }

    // API Key 状态
    var hasApiKey by remember { mutableStateOf<Boolean?>(null) }
    var maskedKey by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf(AppScreen.SETUP) }

    // 初始化
    LaunchedEffect(Unit) {
        hasApiKey = keyStore.hasApiKey()
        if (hasApiKey == true) {
            maskedKey = keyStore.getMaskedApiKey()
            currentScreen = AppScreen.GAME
        } else {
            currentScreen = AppScreen.SETUP
        }
    }

    // 加载中
    if (hasApiKey == null) return

    // ViewModel（懒初始化，仅在进入 GAME 页面时创建）
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(context.applicationContext) as T
            }
        }
    }
    val gameViewModel: GameViewModel = viewModel(factory = factory)

    // 路由
    when (currentScreen) {
        AppScreen.SETUP -> {
            ApiKeySetupScreen(
                onKeyConfirmed = { key ->
                    if (key.isNotBlank()) {
                        keyStore.saveApiKey(key)
                    }
                    hasApiKey = true
                    maskedKey = keyStore.getMaskedApiKey()
                    currentScreen = AppScreen.GAME
                }
            )
        }

        AppScreen.GAME -> {
            GameScreen(
                viewModel = gameViewModel,
                onOpenSettings = {
                    maskedKey = keyStore.getMaskedApiKey()
                    currentScreen = AppScreen.SETTINGS
                }
            )
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                hasApiKey = hasApiKey == true,
                maskedKey = maskedKey,
                onSaveKey = { key ->
                    keyStore.saveApiKey(key)
                    hasApiKey = true
                    maskedKey = keyStore.getMaskedApiKey()
                },
                onDeleteKey = {
                    keyStore.clearApiKey()
                    hasApiKey = false
                    maskedKey = ""
                    currentScreen = AppScreen.SETUP
                },
                onValidateKey = { key ->
                    withContext(Dispatchers.IO) {
                        // 简单格式验证 + 异步调用 DeepSeek
                        if (!key.startsWith("sk-")) {
                            return@withContext false
                        }
                        val client = com.hualao.qiwang.ai.DeepSeekApiClient(key)
                        client.validateApiKey()
                    }
                },
                onBack = {
                    currentScreen = AppScreen.GAME
                }
            )
        }
    }
}

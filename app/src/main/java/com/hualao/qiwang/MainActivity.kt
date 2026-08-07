package com.hualao.qiwang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hualao.qiwang.data.ApiKeyStore
import com.hualao.qiwang.ui.screen.ApiKeySetupScreen
import com.hualao.qiwang.ui.screen.GameScreen
import com.hualao.qiwang.ui.theme.XiangqiTheme
import com.hualao.qiwang.viewmodel.GameViewModel

/**
 * 话唠棋王 — Android 入口 Activity
 *
 * B+ 方案：全本地化，无后端服务器
 * - DeepSeek API Key 由用户首次启动时手动输入
 * - Pikafish 引擎本地运行（NDK/JNI）
 * - Jetpack Compose 全声明式 UI
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

@Composable
fun App() {
    val context = LocalContext.current

    // 检查 API Key 状态
    var hasApiKey by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val keyStore = ApiKeyStore(context)
        hasApiKey = keyStore.hasApiKey()
    }

    // 检查中，显示空白
    if (hasApiKey == null) return

    if (hasApiKey == false) {
        // 首次启动 — 显示 API Key 设置页
        ApiKeySetupScreen(
            onKeyConfirmed = { key ->
                if (key.isNotBlank()) {
                    val keyStore = ApiKeyStore(context)
                    keyStore.saveApiKey(key)
                }
                hasApiKey = true
            }
        )
        return
    }

    // 已配置 Key — 创建 ViewModel 并进入游戏主界面
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(context.applicationContext) as T
            }
        }
    }
    val viewModel: GameViewModel = viewModel(factory = factory)

    GameScreen(viewModel = viewModel)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AppPreview() {
    XiangqiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Text("话唠棋王")
        }
    }
}

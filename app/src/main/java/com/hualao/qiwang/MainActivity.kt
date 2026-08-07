package com.hualao.qiwang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hualao.qiwang.ui.theme.XiangqiTheme

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
    Text(
        text = "话唠棋王",
        style = MaterialTheme.typography.headlineLarge
    )
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    XiangqiTheme {
        App()
    }
}

package com.hualao.qiwang.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hualao.qiwang.ai.DeepSeekApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * API Key 设置页面 — 首次启动或重置 Key 时使用。
 *
 * 功能：
 * - API Key 输入框（密码掩码）
 * - 显示/隐藏 Key 切换
 * - 验证按钮（调用 DeepSeek 轻量接口）
 * - DeepSeek 开放平台链接
 * - 验证状态展示
 */
@Composable
fun ApiKeySetupScreen(
    onKeyConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var validationSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Logo
        Text(
            text = "🏯",
            fontSize = 56.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "话唠棋王",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "AI 对弈 · 嘴强王者",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔑 需要 DeepSeek API Key",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "本应用使用 DeepSeek AI 生成嘲讽对话，需要您提供自己的 API Key。Key 将加密存储在您的设备上，不会上传到任何服务器。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // API Key 输入框
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                validationError = null
                validationSuccess = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            placeholder = { Text("sk-xxxxxxxxxxxxxxxx") },
            visualTransformation = if (showKey)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (apiKey.isNotBlank()) validateKey(
                        apiKey, { isValidating = it },
                        { validationError = it; validationSuccess = it == null },
                        scope,
                        { onKeyConfirmed(apiKey) }
                    )
                }
            ),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "隐藏" else "显示", fontSize = 12.sp)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 验证按钮
        Button(
            onClick = {
                validateKey(
                    apiKey,
                    { isValidating = it },
                    { validationError = it; validationSuccess = it == null },
                    scope,
                    { onKeyConfirmed(apiKey) }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = apiKey.isNotBlank() && !isValidating,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("验证中...")
            } else {
                Text("验证并开始", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 验证结果
        AnimatedVisibility(visible = validationError != null || validationSuccess) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (validationError != null) {
                    Text(
                        text = "❌ $validationError",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (validationSuccess) {
                    Text(
                        text = "✅ 验证成功！点击「验证并开始」进入游戏",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // DeepSeek 平台链接
        Text(
            text = "还没有 API Key？前往 DeepSeek 开放平台获取 →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "platform.deepseek.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // 跳过（离线模式）
        TextButton(
            onClick = { onKeyConfirmed("") }
        ) {
            Text(
                text = "跳过，使用离线模式（无 AI 嘲讽）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 验证 API Key 有效性。
 */
private fun validateKey(
    key: String,
    setValidating: (Boolean) -> Unit,
    setResult: (String?) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    onSuccess: () -> Unit
) {
    if (key.isBlank()) {
        setResult("请输入 API Key")
        return
    }

    setValidating(true)
    setResult(null)

    scope.launch {
        val result: String? = withContext(Dispatchers.IO) {
            try {
                val client = DeepSeekApiClient(key)
                if (client.validateApiKey()) null
                else "API Key 无效，请检查后重试"
            } catch (e: Exception) {
                e.message ?: "验证失败"
            }
        }

        setValidating(false)

        if (result == null) {
            setResult(null) // 成功
            onSuccess()
        } else {
            setResult(result)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ApiKeySetupScreenPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        ApiKeySetupScreen(onKeyConfirmed = {})
    }
}

package com.hualao.qiwang.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * API Key 管理设置页 — 查看、更新、删除 API Key。
 *
 * Phase 4 补充：提供已配置 Key 的管理入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hasApiKey: Boolean,
    maskedKey: String,
    onSaveKey: (String) -> Unit,
    onDeleteKey: () -> Unit,
    onValidateKey: suspend (String) -> Boolean,
    onBack: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---- API Key 管理 ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "DeepSeek API Key",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (hasApiKey && !isEditing) {
                        // 显示已配置 Key（掩码）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "当前 Key：",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                maskedKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("更换 Key")
                            }
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("删除 Key")
                            }
                        }
                    } else {
                        // 首次输入 或 编辑模式
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it; validationError = null },
                            label = { Text("输入 API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (showKey) "隐藏" else "显示"
                                    )
                                }
                            },
                            isError = validationError != null
                        )

                        if (validationError != null) {
                            Text(
                                validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (keyInput.isBlank()) {
                                    validationError = "请输入有效的 API Key"
                                    return@Button
                                }
                                scope.launch {
                                    isValidating = true
                                    validationError = null
                                    val valid = onValidateKey(keyInput.trim())
                                    isValidating = false
                                    if (valid) {
                                        onSaveKey(keyInput.trim())
                                        keyInput = ""
                                        isEditing = false
                                        snackbarHostState.showSnackbar("API Key 验证成功，已保存")
                                    } else {
                                        validationError = "Key 验证失败，请检查后重试"
                                    }
                                }
                            },
                            enabled = keyInput.isNotBlank() && !isValidating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("验证中...")
                            } else {
                                Text("验证并保存")
                            }
                        }

                        if (isEditing) {
                            TextButton(
                                onClick = {
                                    isEditing = false
                                    keyInput = ""
                                    validationError = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("取消")
                            }
                        }
                    }
                }
            }

            // ---- 使用说明 ----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "使用说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• 话唠棋王使用 DeepSeek API 生成嘲讽/自夸对话\n" +
                        "• 从 platform.deepseek.com 获取你的 API Key\n" +
                        "• Key 使用 AES-256-GCM 加密存储在本地\n" +
                        "• 不上传至任何第三方服务器\n" +
                        "• 未配置 Key 时将使用离线模式（预设语料库）\n" +
                        "• 离线模式下嘲讽对话体验受限",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ---- 删除确认对话框 ----
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("删除 API Key 后，嘲讽/自夸将使用离线模式。你可以随时重新配置。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteKey()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

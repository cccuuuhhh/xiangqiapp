package com.hualao.qiwang.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hualao.qiwang.model.GameStatus

/**
 * 难度标签映射。
 */
private val DIFFICULTY_LABELS = listOf("入门", "初级", "中级", "高级", "大师", "特级")

/**
 * 难度简短描述。
 */
private val DIFFICULTY_DESCS = listOf(
    "新手友好",
    "稍有挑战",
    "旗鼓相当",
    "需要认真",
    "高手过招",
    "极限挑战"
)

/**
 * 游戏控制面板。
 *
 * 功能：
 * - 新局 / 悔棋 / 认负 按钮
 * - 难度选择器（下拉菜单或横向选择）
 * - 游戏状态显示
 */
@Composable
fun ControlPanel(
    difficulty: Int,
    gameStatus: GameStatus,
    canUndo: Boolean,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    onDifficultyChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDifficultyMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // 游戏状态横幅
        if (gameStatus != GameStatus.PLAYING) {
            GameStatusBanner(gameStatus = gameStatus)
            Spacer(Modifier.height(8.dp))
        }

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 新局
            ControlButton(
                label = "新局",
                icon = "🔄",
                enabled = true,
                onClick = onNewGame
            )

            // 悔棋
            ControlButton(
                label = "悔棋",
                icon = "↩️",
                enabled = canUndo,
                onClick = onUndo
            )

            // 认负
            ControlButton(
                label = "认负",
                icon = "🏳️",
                enabled = gameStatus == GameStatus.PLAYING,
                onClick = onResign
            )

            // 难度
            Box {
                ControlButton(
                    label = DIFFICULTY_LABELS[difficulty],
                    icon = "⚙️",
                    enabled = true,
                    onClick = { showDifficultyMenu = true }
                )

                DropdownMenu(
                    expanded = showDifficultyMenu,
                    onDismissRequest = { showDifficultyMenu = false }
                ) {
                    DIFFICULTY_LABELS.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = label,
                                        fontWeight = if (index == difficulty) FontWeight.Bold else FontWeight.Normal,
                                        color = if (index == difficulty)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = DIFFICULTY_DESCS[index],
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onDifficultyChange(index)
                                showDifficultyMenu = false
                            },
                            leadingIcon = {
                                if (index == difficulty) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个控制按钮。
 */
@Composable
private fun ControlButton(
    label: String,
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "btnBg"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 游戏状态横幅。
 */
@Composable
private fun GameStatusBanner(gameStatus: GameStatus) {
    val (text, bgColor) = when (gameStatus) {
        GameStatus.RED_WIN -> "🎉 恭喜！红方获胜！" to Color(0xFF4CAF50).copy(alpha = 0.15f)
        GameStatus.BLACK_WIN -> "😈 黑方获胜！再练练吧～" to Color(0xFFF44336).copy(alpha = 0.15f)
        GameStatus.DRAW -> "🤝 平局！旗鼓相当" to Color(0xFFFF9800).copy(alpha = 0.15f)
        else -> return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ControlPanelPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        ControlPanel(
            difficulty = 3,
            gameStatus = GameStatus.PLAYING,
            canUndo = true,
            onNewGame = {},
            onUndo = {},
            onResign = {},
            onDifficultyChange = {}
        )
    }
}

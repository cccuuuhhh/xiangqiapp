package com.hualao.qiwang.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hualao.qiwang.engine.FenConverter

/**
 * 着法历史面板 — 以棋谱风格展示走棋历史。
 *
 * 功能：
 * - 回合编号（中文数字）
 * - 红方着法 + 黑方着法交替显示
 * - 滚动到最新着法
 * - 空状态提示
 */
@Composable
fun MoveHistoryPanel(
    playerMoves: List<String>,
    aiMoves: List<String>,
    modifier: Modifier = Modifier
) {
    // 合并为回合列表
    @Suppress("NAME_SHADOWING")
    val rounds = remember(playerMoves, aiMoves) {
        val maxLen = maxOf(playerMoves.size, aiMoves.size)
        (1..maxLen).map { round ->
            RoundEntry(
                round = round,
                playerMove = playerMoves.getOrNull(round - 1),
                aiMove = aiMoves.getOrNull(round - 1)
            )
        }
    }

    Column(modifier = modifier) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "着法历史",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (rounds.isNotEmpty()) {
                Text(
                    text = "${rounds.size} 回合",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (rounds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无走棋记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                itemsIndexed(rounds) { _, round ->
                    MoveHistoryRow(round)
                }
            }
        }
    }
}

/**
 * 单行者法历史行。
 */
@Composable
private fun MoveHistoryRow(round: RoundEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 回合号
            Text(
                text = roundNoText(round.round),
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 红方着法
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = round.playerMove ?: "...",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // 黑方着法
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = round.aiMove ?: "...",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 回合记录（内部使用）。
 */
private data class RoundEntry(
    val round: Int,
    val playerMove: String?,
    val aiMove: String?
)

/**
 * 数字转中文数字回合号。
 */
private fun roundNoText(round: Int): String {
    val digits = listOf("〇", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    return if (round <= 10) digits[round] else "$round"
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MoveHistoryPanelPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        MoveHistoryPanel(
            playerMoves = listOf("砲二平五", "馬八进七"),
            aiMoves = listOf("马8进7", "车9平8")
        )
    }
}

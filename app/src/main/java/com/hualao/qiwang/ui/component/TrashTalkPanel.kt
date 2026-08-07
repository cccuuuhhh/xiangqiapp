package com.hualao.qiwang.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hualao.qiwang.data.PersonalityManager
import com.hualao.qiwang.data.PersonalityManager.PersonalityConfig
import com.hualao.qiwang.ui.theme.*
import com.hualao.qiwang.viewmodel.StreamType
import kotlinx.coroutines.delay

/**
 * 流式嘲讽/自夸面板。
 *
 * 功能：
 * - 嘲讽气泡（紫色，右对齐） vs 自夸气泡（金色，左对齐）
 * - 流式逐字显示（真打字机效果）
 * - 自动滚动到最新
 * - 性格头像 + 名称标识
 */
@Composable
fun TrashTalkPanel(
    trashTalks: List<String>,
    selfPraises: List<String>,
    streamingText: String,
    isStreaming: Boolean,
    streamType: StreamType,
    personality: PersonalityConfig?,
    aiThinking: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 合并嘲讽和自夸为统一消息列表，每条记录其类型
    data class ChatMessage(
        val text: String,
        val isTrashTalk: Boolean
    )

    val messages = remember(trashTalks, selfPraises) {
        val result = mutableListOf<ChatMessage>()
        var ti = 0; var si = 0
        // 交替插入（嘲讽在先，因为嘲讽触发在前）
        while (ti < trashTalks.size || si < selfPraises.size) {
            if (ti < trashTalks.size && (si >= selfPraises.size || ti <= si)) {
                result.add(ChatMessage(trashTalks[ti], true))
                ti++
            }
            if (si < selfPraises.size && (ti >= trashTalks.size || si < ti)) {
                result.add(ChatMessage(selfPraises[si], false))
                si++
            }
        }
        result
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            delay(50)
            listState.animateScrollToItem(maxOf(0, messages.size + if (streamingText.isNotEmpty()) 1 else 0) - 1)
        }
    }

    Column(modifier = modifier) {
        // 标题栏
        if (personality != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = personality.avatar,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = personality.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "· ${personality.speakingStyle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }

                // AI 思考指示器
                AnimatedVisibility(visible = aiThinking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "思考中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(messages) { _, msg ->
                ChatBubble(
                    text = msg.text,
                    isTrashTalk = msg.isTrashTalk,
                    avatar = personality?.avatar ?: ""
                )
            }

            // 流式输出进行中的气泡
            if (streamingText.isNotEmpty() || isStreaming) {
                item {
                    ChatBubble(
                        text = streamingText.ifEmpty { "..." },
                        isTrashTalk = streamType == StreamType.TRASH_TALK,
                        avatar = personality?.avatar ?: "",
                        isStreaming = isStreaming
                    )
                }
            }
        }

        // 空状态
        if (messages.isEmpty() && !isStreaming && !aiThinking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "开始对局，AI 会在这里和你「交流」",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 单条气泡消息组件。
 */
@Composable
private fun ChatBubble(
    text: String,
    isTrashTalk: Boolean,
    avatar: String,
    isStreaming: Boolean = false
) {
    val bubbleColor = if (isTrashTalk) TrashTalkBubble else SelfPraiseBubble
    val alignment = if (isTrashTalk) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = if (isTrashTalk) 12.dp else 4.dp,
                topEnd = if (isTrashTalk) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = bubbleColor.copy(alpha = 0.15f),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = avatar, fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isTrashTalk) "嘲讽" else "自夸",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = bubbleColor
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                // 流式光标
                if (isStreaming) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(bubbleColor, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TrashTalkPanelPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        Box(modifier = Modifier.height(300.dp)) {
            TrashTalkPanel(
                trashTalks = listOf("就这水平还敢跟我下？", "你这步棋是闭着眼下的吧？"),
                selfPraises = listOf("我的車已经埋伏好了。"),
                streamingText = "见识一下我的终极奥义——「暗黑車輪」",
                isStreaming = true,
                streamType = StreamType.TRASH_TALK,
                personality = PersonalityManager.PersonalityConfig(
                    id = "chuunibyou",
                    name = "中二少年",
                    avatar = "⚔️",
                    description = "每下一步棋都要喊出招式名",
                    systemPrompt = "",
                    trashTalkFrequency = 0.9,
                    speakingStyle = "中二热血",
                    exampleLines = emptyList()
                ),
                aiThinking = false
            )
        }
    }
}


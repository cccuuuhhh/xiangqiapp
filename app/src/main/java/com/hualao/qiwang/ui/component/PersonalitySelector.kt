package com.hualao.qiwang.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hualao.qiwang.data.PersonalityManager.PersonalityConfig

/**
 * 性格选择器 — 支持折叠/展开两种状态。
 *
 * 折叠态：只显示当前选中的性格名称 + 展开按钮，节省空间。
 * 展开态：显示 LazyRow 横向卡片，可切换性格。
 */
@Composable
fun PersonalitySelector(
    personalities: List<PersonalityConfig>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeIndex = selectedIndex.coerceIn(0, personalities.size - 1)
    val current = personalities.getOrNull(safeIndex)

    Column(modifier = modifier) {
        // 标题 / 折叠态单行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (expanded) {
                Text(
                    text = "选择 AI 性格",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = current?.avatar ?: "🎭",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "AI 性格：${current?.name ?: "默认"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    current?.speakingStyle?.takeIf { it.isNotBlank() }?.let { style ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = style,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onExpandToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 展开态才显示卡片列表
        if (expanded) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(personalities) { index, config ->
                    PersonalityCard(
                        config = config,
                        isSelected = index == safeIndex,
                        onClick = {
                            onSelect(index)
                            // 选中后自动折叠，让出空间给对话面板
                            onExpandToggle()
                        }
                    )
                }

                // 尾部留白
                item { Spacer(Modifier.width(4.dp)) }
            }
        }
    }
}

/**
 * 单张性格卡片。
 */
@Composable
private fun PersonalityCard(
    config: PersonalityConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "cardColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "borderColor"
    )

    Surface(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = cardColor,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像
            Text(
                text = config.avatar,
                fontSize = 28.sp
            )

            Spacer(Modifier.height(4.dp))

            // 名称
            Text(
                text = config.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            // 描述
            Text(
                text = config.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            // 说话风格标签
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = config.speakingStyle,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PersonalitySelectorPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        PersonalitySelector(
            personalities = listOf(
                PersonalityConfig("toxic-master", "毒舌大师", "😈", "嘴比刀子还狠", "", 0.8, "尖酸刻薄", emptyList()),
                PersonalityConfig("elegant-gentleman", "优雅绅士", "🎩", "阴阳怪气大师", "", 0.5, "表面客气", emptyList()),
                PersonalityConfig("chuunibyou", "中二少年", "⚔️", "每步都有招式名", "", 0.9, "中二热血", emptyList()),
            ),
            selectedIndex = 0,
            onSelect = {},
            expanded = true,
            onExpandToggle = {}
        )
    }
}

package com.hualao.qiwang.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 中国象棋经典配色
val ChessBoardWood = Color(0xFFDEB887)        // 棋盘木色
val ChessBoardWoodDark = Color(0xFFB8860B)    // 棋盘深木色
val RedPiece = Color(0xFFCC0000)              // 红方棋子
val RedPieceBright = Color(0xFFFF4444)        // 红方高亮
val BlackPiece = Color(0xFF1A1A1A)            // 黑方棋子
val BlackPieceBright = Color(0xFF444444)      // 黑方高亮
val TrashTalkBubble = Color(0xFF8B5CF6)       // 嘲讽气泡（紫色）
val SelfPraiseBubble = Color(0xFFDAA520)      // 自夸气泡（金色）
val ChessGrid = Color(0xFF4A3728)             // 棋盘网格线
val SelectedPiece = Color(0x44FFD700)         // 选中高亮（半透明金）
val LegalMoveDot = Color(0x4400CC00)          // 合法位置指示（半透明绿）
val KingDanger = Color(0x44FF0000)            // 将军警告（半透红）

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B4513),            // 中国风棕红
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC5),
    onPrimaryContainer = Color(0xFF311300),
    secondary = Color(0xFFCC2929),          // 中国红
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFF556B2F),           // 军绿
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8F5A2),
    onTertiaryContainer = Color(0xFF132200),
    background = Color(0xFFFFF8F0),         // 米白底色
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF2DFD0),
    onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85746A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = Color(0xFF4F1B00),
    primaryContainer = Color(0xFF723600),
    onPrimaryContainer = Color(0xFFFFDBC5),
    secondary = Color(0xFFFFB3AC),
    onSecondary = Color(0xFF680008),
    secondaryContainer = Color(0xFF92000F),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFBCD98B),
    onTertiary = Color(0xFF273A00),
    tertiaryContainer = Color(0xFF3C5200),
    onTertiaryContainer = Color(0xFFD8F5A2),
    background = Color(0xFF1B1B1E),
    onBackground = Color(0xFFE7E1D9),
    surface = Color(0xFF1B1B1E),
    onSurface = Color(0xFFE7E1D9),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD5C3B5),
    outline = Color(0xFF9F8E81)
)

@Composable
fun XiangqiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // 默认关闭动态取色，使用中国风配色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

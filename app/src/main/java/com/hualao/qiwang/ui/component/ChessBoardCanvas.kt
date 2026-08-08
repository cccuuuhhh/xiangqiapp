package com.hualao.qiwang.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hualao.qiwang.engine.FenConverter
import com.hualao.qiwang.engine.MoveGenerator
import com.hualao.qiwang.engine.MoveValidator
import com.hualao.qiwang.model.Board
import com.hualao.qiwang.model.Move
import com.hualao.qiwang.model.Position
import com.hualao.qiwang.model.Side
import com.hualao.qiwang.ui.theme.*

/**
 * 中国象棋棋盘 Canvas 组件。
 *
 * 功能：
 * - 绘制 9 竖线 × 10 横线棋盘，楚河汉界，九宫斜线
 * - 棋子圆心精确对齐横竖线交叉点（非格子内部）
 * - 圆形棋子直径占相邻交叉点间距 65%~72%，视觉饱满
 * - 棋子文字适中，留足内边距，垂直水平居中
 * - 选中高亮 / 合法走法提示 / 最后一步标记 / 将军警告
 * - 触摸选子 / 走子手势
 */
@Composable
fun ChessBoardCanvas(
    board: Board,
    currentSide: Side,
    isPlaying: Boolean,
    lastMove: Move?,
    selectedPiece: Position?,
    kingInCheck: Boolean,
    onSquareTapped: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    // 棋盘边距比例（交叉点区域外的留白）
    val boardPaddingRatio = 0.08f

    // 合法走法目标（由 UI 层计算）
    val legalMoves = remember(board, selectedPiece) {
        if (selectedPiece != null && isPlaying && currentSide == Side.RED) {
            val piece = board[selectedPiece]
            if (piece.isNotEmpty() && board.isOwnPiece(selectedPiece.row, selectedPiece.col, Side.RED)) {
                MoveGenerator(MoveValidator()).generateAllLegalMoves(board, Side.RED)
                    .filter { it.from == selectedPiece }
                    .map { it.to }
            } else emptyList()
        } else emptyList()
    }

    // 动画进度
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "boardAnim"
    )

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(9f / 10f)
            .pointerInput(board, isPlaying, currentSide) {
                detectTapGestures { offset ->
                    val sizeW = size.width.toFloat()
                    val sizeH = size.height.toFloat()
                    val cellW = sizeW / (9 + 2 * boardPaddingRatio)
                    val cellH = sizeH / (10 + 2 * boardPaddingRatio)

                    val col = ((offset.x - cellW * boardPaddingRatio) / cellW).toInt()
                    val row = ((offset.y - cellH * boardPaddingRatio) / cellH).toInt()

                    if (row in 0..9 && col in 0..8) {
                        onSquareTapped(Position(row, col))
                    }
                }
            }
    ) {
        val canvasWidth = with(density) { maxWidth.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / (9 + 2 * boardPaddingRatio)
            val cellH = h / (10 + 2 * boardPaddingRatio)
            val paddingX = cellW * boardPaddingRatio
            val paddingY = cellH * boardPaddingRatio

            // 棋子半径：直径 = 交叉点间距 × 70%（位于 65%~72% 规范区间中间）
            val pieceRadius = minOf(cellW, cellH) * 0.35f

            // 1. 棋盘底色
            drawRect(ChessBoardWood, Offset.Zero, size)

            // 2. 棋盘网格线
            drawBoardGrid(w, h, paddingX, paddingY, cellW, cellH)

            // 3. 楚河汉界
            drawRiverText(paddingX, paddingY, cellW, cellH)

            // 4. 九宫斜线
            drawPalaceDiagonals(paddingX, paddingY, cellW, cellH)

            // 5. 最后一步走棋标记（交叉点中心高亮）
            if (lastMove != null) {
                drawLastMoveHighlight(lastMove, paddingX, paddingY, cellW, cellH)
            }

            // 6. 合法走法提示圆点
            for (target in legalMoves) {
                drawLegalMoveDot(target, paddingX, paddingY, cellW, cellH, pieceRadius)
            }

            // 7. 将军警告光环
            if (kingInCheck) {
                val kingPos = board.findKing(currentSide)
                if (kingPos != null) {
                    drawKingDanger(kingPos, paddingX, paddingY, cellW, cellH, pieceRadius)
                }
            }

            // 8. 选中棋子高亮
            if (selectedPiece != null) {
                drawSelectedHighlight(selectedPiece, paddingX, paddingY, cellW, cellH, pieceRadius)
            }

            // 9. 渲染全部棋子（圆心对齐交叉点）
            drawPieces(
                board = board,
                px = paddingX,
                py = paddingY,
                cellW = cellW,
                cellH = cellH,
                radius = pieceRadius,
                density = density
            )
        }
    }
}

// ==================== 绘制方法 ====================

/**
 * 绘制棋盘网格：9 竖线 × 10 横线，河界处竖线断开。
 */
private fun DrawScope.drawBoardGrid(
    w: Float, h: Float,
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    // 横线（10 条，row 0 ~ row 9）
    for (r in 0..9) {
        val y = py + r * cellH
        drawLine(ChessGrid, Offset(px, y), Offset(px + 8 * cellW, y), strokeWidth = 1.5f)
    }

    // 竖线（9 条，河界处断开：row 0→4, row 5→9）
    for (c in 0..8) {
        val x = px + c * cellW
        drawLine(ChessGrid, Offset(x, py), Offset(x, py + 4 * cellH), strokeWidth = 1.5f)
        drawLine(ChessGrid, Offset(x, py + 5 * cellH), Offset(x, py + 9 * cellH), strokeWidth = 1.5f)
    }

    // 左右边框竖线加粗
    drawLine(ChessGrid, Offset(px, py), Offset(px, py + 9 * cellH), strokeWidth = 2f)
    drawLine(ChessGrid, Offset(px + 8 * cellW, py), Offset(px + 8 * cellW, py + 9 * cellH), strokeWidth = 2f)

    // 上下边框横线加粗
    drawLine(ChessGrid, Offset(px, py), Offset(px + 8 * cellW, py), strokeWidth = 2f)
    drawLine(ChessGrid, Offset(px, py + 9 * cellH), Offset(px + 8 * cellW, py + 9 * cellH), strokeWidth = 2f)
}

private fun DrawScope.drawRiverText(
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    val riverY = py + 4.5f * cellH
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(80, 74, 55, 40)
        textSize = cellW * 0.6f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("KaiTi", android.graphics.Typeface.NORMAL)
    }
    // 左半（列 0-3 左半区域中心）和右半（列 5-8 右半区域中心）
    drawContext.canvas.nativeCanvas.drawText("楚  河", px + 1.5f * cellW, riverY, paint)
    drawContext.canvas.nativeCanvas.drawText("汉  界", px + 6.5f * cellW, riverY, paint)
}

private fun DrawScope.drawPalaceDiagonals(
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    // 上方九宫（row 0-2, col 3-5）
    drawLine(ChessGrid, Offset(px + 3 * cellW, py), Offset(px + 5 * cellW, py + 2 * cellH), strokeWidth = 1f)
    drawLine(ChessGrid, Offset(px + 5 * cellW, py), Offset(px + 3 * cellW, py + 2 * cellH), strokeWidth = 1f)

    // 下方九宫（row 7-9, col 3-5）
    drawLine(ChessGrid, Offset(px + 3 * cellW, py + 7 * cellH), Offset(px + 5 * cellW, py + 9 * cellH), strokeWidth = 1f)
    drawLine(ChessGrid, Offset(px + 5 * cellW, py + 7 * cellH), Offset(px + 3 * cellW, py + 9 * cellH), strokeWidth = 1f)
}

private fun DrawScope.drawLastMoveHighlight(
    move: Move,
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    // 矩形高亮以交叉点为中心
    drawRect(
        SelectedPiece,
        topLeft = Offset(px + move.from.col * cellW - cellW / 2, py + move.from.row * cellH - cellH / 2),
        size = Size(cellW, cellH)
    )
    drawRect(
        SelectedPiece,
        topLeft = Offset(px + move.to.col * cellW - cellW / 2, py + move.to.row * cellH - cellH / 2),
        size = Size(cellW, cellH)
    )
}

private fun DrawScope.drawLegalMoveDot(
    target: Position,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    pieceRadius: Float
) {
    // 圆心在交叉点上
    val cx = px + target.col * cellW
    val cy = py + target.row * cellH
    drawCircle(LegalMoveDot, pieceRadius * 0.35f, Offset(cx, cy))
}

private fun DrawScope.drawKingDanger(
    kingPos: Position,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    pieceRadius: Float
) {
    val cx = px + kingPos.col * cellW
    val cy = py + kingPos.row * cellH
    drawCircle(KingDanger, pieceRadius * 1.3f, Offset(cx, cy))
}

private fun DrawScope.drawSelectedHighlight(
    pos: Position,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    pieceRadius: Float
) {
    val cx = px + pos.col * cellW
    val cy = py + pos.row * cellH
    drawCircle(SelectedPiece, pieceRadius * 1.15f, Offset(cx, cy))
}

/**
 * 渲染全部棋子 — 圆心精确对齐横竖线交叉点。
 *
 * 规格：
 * - 棋子直径 = 相邻交叉点间距 × 70%（在 65%~72% 规范区间内）
 * - 文字字号 = 交叉点间距 × 36%，在圆内有充足内边距
 * - 中文垂直水平居中于圆内
 */
private fun DrawScope.drawPieces(
    board: Board,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    radius: Float,
    density: androidx.compose.ui.unit.Density
) {
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("KaiTi", android.graphics.Typeface.BOLD)
    }

    val borderPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    val fillPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }

    // 文字大小：占圆直径的 ~50%，留足内边距
    val textSize = minOf(cellW, cellH) * 0.36f
    textPaint.textSize = textSize

    for (r in 0 until Board.ROWS) {
        for (c in 0 until Board.COLS) {
            val piece = board[r, c]
            if (piece.isEmpty()) continue

            // ★ 圆心对齐横竖线交叉点（非格子中心）
            val cx = px + c * cellW
            val cy = py + r * cellH
            val isRed = piece.startsWith("r")

            // 阴影
            fillPaint.color = android.graphics.Color.argb(50, 0, 0, 0)
            drawContext.canvas.nativeCanvas.drawCircle(cx + 1.5f, cy + 2f, radius, fillPaint)

            // 棋子底色
            fillPaint.color = android.graphics.Color.argb(255, 250, 235, 180)
            drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius, fillPaint)

            // 外边框
            borderPaint.color = if (isRed)
                android.graphics.Color.argb(255, 180, 80, 20)
            else
                android.graphics.Color.argb(255, 30, 30, 30)
            drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius, borderPaint)

            // 内圈装饰线
            borderPaint.color = if (isRed)
                android.graphics.Color.argb(150, 180, 80, 20)
            else
                android.graphics.Color.argb(150, 60, 60, 60)
            drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius * 0.82f, borderPaint)

            // 棋子文字 — 垂直水平居中
            val chinese = FenConverter.pieceToChinese(piece)
            textPaint.color = if (isRed)
                android.graphics.Color.argb(255, 180, 20, 20)
            else
                android.graphics.Color.argb(255, 20, 20, 20)
            drawContext.canvas.nativeCanvas.drawText(
                chinese,
                cx,
                cy - (textPaint.descent() + textPaint.ascent()) / 2,
                textPaint
            )
        }
    }
}

// ==================== 预览 ====================

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ChessBoardCanvasPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        Box(
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ChessBoardCanvas(
                board = Board.createInitial(),
                currentSide = Side.RED,
                isPlaying = true,
                lastMove = null,
                selectedPiece = Position(9, 4),
                kingInCheck = false,
                onSquareTapped = {}
            )
        }
    }
}

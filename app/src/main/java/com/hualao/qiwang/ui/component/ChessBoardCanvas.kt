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
 * - 绘制 10×9 棋盘线、楚河汉界、九宫斜线
 * - 渲染 32 枚棋子（圆形 + 汉字）
 * - 选中棋子高亮、合法走法提示、最后一步标记、将军警告
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
    // 棋盘边距比例
    val boardPaddingRatio = 0.06f

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
        val canvasWidth = maxWidth.toPx()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / (9 + 2 * boardPaddingRatio)
            val cellH = h / (10 + 2 * boardPaddingRatio)
            val paddingX = cellW * boardPaddingRatio
            val paddingY = cellH * boardPaddingRatio
            val pieceRadius = cellW * 0.40f

            // 1. 棋盘底色
            drawRect(ChessBoardWood, Offset.Zero, size)

            // 2. 棋盘网格线
            drawBoardGrid(w, h, paddingX, paddingY, cellW, cellH)

            // 3. 楚河汉界
            drawRiverText(paddingX, paddingY, cellW, cellH, w)

            // 4. 九宫斜线
            drawPalaceDiagonals(paddingX, paddingY, cellW, cellH)

            // 5. 最后一步走棋标记
            if (lastMove != null) {
                drawLastMoveHighlight(lastMove, paddingX, paddingY, cellW, cellH)
            }

            // 6. 合法走法提示
            for (target in legalMoves) {
                drawLegalMoveDot(target, paddingX, paddingY, cellW, cellH, pieceRadius)
            }

            // 7. 将军警告
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

            // 9. 棋子
            drawPieces(
                board = board,
                paddingX = paddingX,
                paddingY = paddingY,
                cellW = cellW,
                cellH = cellH,
                radius = pieceRadius,
                density = density
            )
        }
    }
}

// ==================== 绘制方法 ====================

private fun DrawScope.drawBoardGrid(
    w: Float, h: Float,
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    // 横线 (10 行)
    for (r in 0..9) {
        val y = py + r * cellH
        val leftX = px
        var rightCol = 8
        if (r == 0 || r == 9) rightCol = 8
        drawLine(ChessGrid, Offset(px, y), Offset(px + 8 * cellW, y), strokeWidth = 1.5f)
    }

    // 竖线 (9 列，但河界处断开)
    for (c in 0..8) {
        val x = px + c * cellW
        // 上半部分：row 0 → row 4
        drawLine(ChessGrid, Offset(x, py), Offset(x, py + 4 * cellH), strokeWidth = 1.5f)
        // 下半部分：row 5 → row 9
        drawLine(ChessGrid, Offset(x, py + 5 * cellH), Offset(x, py + 9 * cellH), strokeWidth = 1.5f)
    }

    // 左右边框竖线贯通
    drawLine(ChessGrid, Offset(px, py), Offset(px, py + 9 * cellH), strokeWidth = 2f)
    drawLine(ChessGrid, Offset(px + 8 * cellW, py), Offset(px + 8 * cellW, py + 9 * cellH), strokeWidth = 2f)

    // 上下边框横线加粗
    drawLine(ChessGrid, Offset(px, py), Offset(px + 8 * cellW, py), strokeWidth = 2f)
    drawLine(ChessGrid, Offset(px, py + 9 * cellH), Offset(px + 8 * cellW, py + 9 * cellH), strokeWidth = 2f)
}

private fun DrawScope.drawRiverText(
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    totalW: Float
) {
    val riverY = py + 4.5f * cellH
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(80, 74, 55, 40)
        textSize = cellW * 0.6f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("KaiTi", android.graphics.Typeface.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawText("楚 河", px + 2.5f * cellW, riverY, paint)
    drawContext.canvas.nativeCanvas.drawText("汉 界", px + 5.5f * cellW, riverY, paint)
}

private fun DrawScope.drawPalaceDiagonals(
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    // 上方九宫 (黑方, row 0-2, col 3-5)
    drawLine(ChessGrid, Offset(px + 3 * cellW, py), Offset(px + 5 * cellW, py + 2 * cellH), strokeWidth = 1f)
    drawLine(ChessGrid, Offset(px + 5 * cellW, py), Offset(px + 3 * cellW, py + 2 * cellH), strokeWidth = 1f)

    // 下方九宫 (红方, row 7-9, col 3-5)
    drawLine(ChessGrid, Offset(px + 3 * cellW, py + 7 * cellH), Offset(px + 5 * cellW, py + 9 * cellH), strokeWidth = 1f)
    drawLine(ChessGrid, Offset(px + 5 * cellW, py + 7 * cellH), Offset(px + 3 * cellW, py + 9 * cellH), strokeWidth = 1f)
}

private fun DrawScope.drawLastMoveHighlight(
    move: Move,
    px: Float, py: Float,
    cellW: Float, cellH: Float
) {
    drawRect(
        SelectedPiece,
        topLeft = Offset(px + move.from.col * cellW, py + move.from.row * cellH),
        size = Size(cellW, cellH)
    )
    drawRect(
        SelectedPiece,
        topLeft = Offset(px + move.to.col * cellW, py + move.to.row * cellH),
        size = Size(cellW, cellH)
    )
}

private fun DrawScope.drawLegalMoveDot(
    target: Position,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    pieceRadius: Float
) {
    val cx = px + target.col * cellW + cellW / 2
    val cy = py + target.row * cellH + cellH / 2
    // 如果目标位置有对方棋子，用环表示可吃
    val dotRadius = pieceRadius * 0.35f
    drawCircle(LegalMoveDot, dotRadius, Offset(cx, cy))
}

private fun DrawScope.drawKingDanger(
    kingPos: Position,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    pieceRadius: Float
) {
    val cx = px + kingPos.col * cellW + cellW / 2
    val cy = py + kingPos.row * cellH + cellH / 2
    drawCircle(KingDanger, pieceRadius * 1.3f, Offset(cx, cy))
}

private fun DrawScope.drawSelectedHighlight(
    pos: Position,
    px: Float, py: Float,
    cellW: Float, cellH: Float,
    pieceRadius: Float
) {
    val cx = px + pos.col * cellW + cellW / 2
    val cy = py + pos.row * cellH + cellH / 2
    drawCircle(SelectedPiece, pieceRadius * 1.15f, Offset(cx, cy))
}

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

    val spToPx = density.run { 18f }
    textPaint.textSize = spToPx

    for (r in 0 until Board.ROWS) {
        for (c in 0 until Board.COLS) {
            val piece = board[r, c]
            if (piece.isEmpty()) continue

            val cx = px + c * cellW + cellW / 2
            val cy = py + r * cellH + cellH / 2
            val isRed = piece.startsWith("r")

            // 棋子底盘（浅色阴影）
            fillPaint.color = android.graphics.Color.argb(60, 0, 0, 0)
            drawContext.canvas.nativeCanvas.drawCircle(cx + 1.5f, cy + 1.5f, radius, fillPaint)

            // 棋子底色
            fillPaint.color = android.graphics.Color.argb(255, 250, 235, 180)
            drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius, fillPaint)

            // 棋子边框
            borderPaint.color = if (isRed)
                android.graphics.Color.argb(255, 180, 80, 20)
            else
                android.graphics.Color.argb(255, 30, 30, 30)
            drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius, borderPaint)

            // 内圈装饰
            borderPaint.color = if (isRed)
                android.graphics.Color.argb(150, 180, 80, 20)
            else
                android.graphics.Color.argb(150, 60, 60, 60)
            drawContext.canvas.nativeCanvas.drawCircle(cx, cy, radius * 0.82f, borderPaint)

            // 棋子文字
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

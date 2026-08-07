package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*

/**
 * FEN 字符串生成 + ICCS 坐标互转。
 *
 * FEN 格式示例: rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1
 *
 * 坐标转换：
 * - 项目坐标: row 0 = 黑方底线, row 9 = 红方底线, col 0-8
 * - ICCS 坐标: 列 a-i (0-8), 行 0 (红方底线) - 9 (黑方底线)
 *
 * iccsToPosition("h2") → Position(row=7, col=7)
 * positionToIccs(Position(7, 7)) → "h2"
 */
object FenConverter {

    /**
     * 将当前棋盘转为中国象棋 FEN 字符串。
     * FEN 行顺序：row 0（黑方底线）→ row 9（红方底线），用 / 分隔。
     * 红方大写 (KABNRC)，黑方小写 (kabnrc)。
     */
    fun boardToFen(board: Board, sideToMove: Side): String {
        val fen = StringBuilder()

        for (r in 0 until Board.ROWS) {
            if (r > 0) fen.append('/')
            var emptyCount = 0
            for (c in 0 until Board.COLS) {
                val piece = board[r, c]
                if (piece.isEmpty()) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount)
                        emptyCount = 0
                    }
                    fen.append(pieceToFenChar(piece))
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount)
            }
        }

        fen.append(' ').append(if (sideToMove == Side.RED) 'w' else 'b')
        fen.append(" - - 0 1")

        return fen.toString()
    }

    /**
     * 棋子编码 → FEN 字符。
     * 红方: rK→K, rA→A, rB→B, rN→N, rR→R, rC→C, rP→P
     * 黑方: bK→k, bA→a, bB→b, bN→n, bR→r, bC→c, bP→p
     */
    private fun pieceToFenChar(code: String): Char {
        if (code.length != 2) return '?'
        val type = code[1]
        return if (code[0] == 'r') type else type.lowercaseChar()
    }

    /**
     * ICCS 坐标（如 "h2"）→ 项目 Position。
     * ICCS: 列 a～i(0～8)，行 0(红方底线) → 9(黑方底线)。
     * 项目: row 0 = 黑方底线，row 9 = 红方底线。
     */
    fun iccsToPosition(iccs: String): Position {
        val col = iccs[0] - 'a'
        val row = 9 - (iccs[1] - '0')
        return Position(row, col)
    }

    /**
     * 项目 Position → ICCS 坐标字符串（如 "h2"）
     */
    fun positionToIccs(pos: Position): String {
        val col = 'a' + pos.col
        val row = '0' + (9 - pos.row)
        return "$col$row"
    }

    /**
     * 项目 Move → UCI 着法字符串（如 "h2e2"）
     */
    fun moveToUciString(move: Move): String {
        return positionToIccs(move.from) + positionToIccs(move.to)
    }

    /**
     * UCI 着法字符串 → 项目 Move
     */
    fun uciToMove(uci: String, board: Board): Move? {
        if (uci.length < 4) return null
        val fromIccs = uci.substring(0, 2)
        val toIccs = uci.substring(2, 4)
        val from = iccsToPosition(fromIccs)
        val to = iccsToPosition(toIccs)
        val piece = board[from]
        if (piece.isEmpty()) return null
        val captured = board[to].ifEmpty { null }
        return Move(from, to, piece, captured)
    }

    /**
     * 获取棋子中文名
     */
    fun pieceToChinese(code: String?): String {
        if (code.isNullOrEmpty() || code.length < 2) return "??"
        val isRed = code[0] == 'r'
        return when (code[1]) {
            'K' -> if (isRed) "帅" else "将"
            'A' -> if (isRed) "仕" else "士"
            'B' -> if (isRed) "相" else "象"
            'N' -> if (isRed) "馬" else "马"
            'R' -> if (isRed) "車" else "车"
            'C' -> if (isRed) "砲" else "炮"
            'P' -> if (isRed) "兵" else "卒"
            else -> "??"
        }
    }

    /**
     * 生成着法的中文描述
     */
    fun describeMove(move: Move): String {
        val pieceName = pieceToChinese(move.piece)
        val uci = moveToUciString(move)
        val fromIccs = uci.substring(0, 2)
        val toIccs = uci.substring(2, 4)
        val action = if (move.captured != null) {
            "吃${pieceToChinese(move.captured)}"
        } else {
            "走到"
        }
        return "${pieceName}从$fromIccs$action$toIccs"
    }
}

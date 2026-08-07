package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*
import kotlin.math.abs
import kotlin.math.sign

/**
 * 走法校验器 — 按棋子类型分发校验逻辑。
 *
 * 包含以下校验器：
 * - 帅/将：九宫内一步（上下左右），不能与对方将/帅对面（飞将）
 * - 仕/士：九宫内斜走一步
 * - 相/象：田字对角（2×2），不能过河，注意塞眼
 * - 馬/马：日字（1×2或2×1拐弯），注意蹩脚
 * - 車/车：直线任意距离，路径不能有子阻挡
 * - 砲/炮：直线任意距离，走子不翻山，吃子必须翻山（炮架）
 * - 兵/卒：未过河只能向前一步，过河后可向前/左/右一步，不能后退
 */
class MoveValidator {

    /**
     * 判断某着法在基本规则层面是否合法（不包含将军检测）
     */
    fun isValidMove(board: Board, from: Position, to: Position): Boolean {
        if (!from.isValid() || !to.isValid()) return false
        if (from == to) return false

        val piece = board[from]
        if (piece.isEmpty()) return false

        // 目标位置不能是己方棋子
        val side = Side.fromPieceCode(piece) ?: return false
        if (board.isOwnPiece(to.row, to.col, side)) return false

        val p = Piece.fromCode(piece) ?: return false

        return when (p.type) {
            PieceType.KING -> isValidKingMove(board, from, to, side)
            PieceType.ADVISOR -> isValidAdvisorMove(from, to, side)
            PieceType.BISHOP -> isValidBishopMove(board, from, to, side)
            PieceType.KNIGHT -> isValidKnightMove(board, from, to)
            PieceType.ROOK -> isValidRookMove(board, from, to)
            PieceType.CANNON -> isValidCannonMove(board, from, to)
            PieceType.PAWN -> isValidPawnMove(from, to, side)
        }
    }

    // ==================== 帅/将 ====================

    private fun isValidKingMove(board: Board, from: Position, to: Position, side: Side): Boolean {
        // 必须在九宫内
        if (!isInPalace(to, side)) return false
        // 只能走一步（上下左右）
        val dr = abs(to.row - from.row)
        val dc = abs(to.col - from.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    // ==================== 仕/士 ====================

    private fun isValidAdvisorMove(from: Position, to: Position, side: Side): Boolean {
        // 必须在九宫内
        if (!isInPalace(to, side)) return false
        // 斜走一步
        return abs(to.row - from.row) == 1 && abs(to.col - from.col) == 1
    }

    // ==================== 相/象 ====================

    private fun isValidBishopMove(board: Board, from: Position, to: Position, side: Side): Boolean {
        // 不能过河
        if (isCrossedRiver(to, side)) return false
        // 田字对角（2×2）
        val dr = to.row - from.row
        val dc = to.col - from.col
        if (abs(dr) != 2 || abs(dc) != 2) return false
        // 塞眼检测：田字中心不能有棋子
        val eyeRow = from.row + dr / 2
        val eyeCol = from.col + dc / 2
        return board[eyeRow, eyeCol].isEmpty()
    }

    // ==================== 馬/马 ====================

    private fun isValidKnightMove(board: Board, from: Position, to: Position): Boolean {
        val dr = to.row - from.row
        val dc = to.col - from.col
        // 日字检测
        return when {
            abs(dr) == 2 && abs(dc) == 1 -> {
                // 蹩脚检测：先竖走再横走时的中间点
                val legRow = from.row + dr / 2
                board[legRow, from.col].isEmpty()
            }
            abs(dr) == 1 && abs(dc) == 2 -> {
                // 蹩脚检测：先横走再竖走时的中间点
                val legCol = from.col + dc / 2
                board[from.row, legCol].isEmpty()
            }
            else -> false
        }
    }

    // ==================== 車/车 ====================

    private fun isValidRookMove(board: Board, from: Position, to: Position): Boolean {
        return isValidStraightMove(board, from, to)
    }

    // ==================== 砲/炮 ====================

    private fun isValidCannonMove(board: Board, from: Position, to: Position): Boolean {
        if (from.row != to.row && from.col != to.col) return false

        val isCapture = board[to].isNotEmpty()
        val mountains = countPiecesBetween(board, from, to)

        return if (isCapture) {
            // 吃子时必须翻山（恰好一个炮架）
            mountains == 1
        } else {
            // 走子时不能翻山
            mountains == 0
        }
    }

    // ==================== 兵/卒 ====================

    private fun isValidPawnMove(from: Position, to: Position, side: Side): Boolean {
        val dr = to.row - from.row
        val dc = abs(to.col - from.col)

        return if (side == Side.RED) {
            // 红方：向前 = row 减小
            val hasCrossed = from.row <= 4 // 已过河（到达黑方半场）
            if (hasCrossed) {
                // 可向前/左/右一步
                (dr == -1 && dc == 0) || (dr == 0 && dc == 1)
            } else {
                // 只能向前一步
                dr == -1 && dc == 0
            }
        } else {
            // 黑方：向前 = row 增大
            val hasCrossed = from.row >= 5 // 已过河（到达红方半场）
            if (hasCrossed) {
                (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
            } else {
                dr == 1 && dc == 0
            }
        }
    }

    // ==================== 辅助方法 ====================

    companion object {
        /**
         * 判断位置是否在指定方的九宫内
         */
        fun isInPalace(pos: Position, side: Side): Boolean {
            val col = pos.col
            if (col < 3 || col > 5) return false
            return if (side == Side.RED) {
                pos.row in 7..9
            } else {
                pos.row in 0..2
            }
        }

        /**
         * 判断指定方的棋子是否已过河
         */
        fun isCrossedRiver(pos: Position, side: Side): Boolean {
            return if (side == Side.RED) {
                pos.row <= 4
            } else {
                pos.row >= 5
            }
        }

        /**
         * 判断 from → to 是否是直线移动且路径无障碍（用于車/将）
         */
        fun isValidStraightMove(board: Board, from: Position, to: Position): Boolean {
            if (from.row != to.row && from.col != to.col) return false
            return countPiecesBetween(board, from, to) == 0
        }

        /**
         * 计算 from → to 直线路径上的棋子数（不含两端）
         */
        fun countPiecesBetween(board: Board, from: Position, to: Position): Int {
            val dr = (to.row - from.row).sign
            val dc = (to.col - from.col).sign

            var count = 0
            var r = from.row + dr
            var c = from.col + dc
            while (r != to.row || c != to.col) {
                if (board[r, c].isNotEmpty()) count++
                r += dr
                c += dc
            }
            return count
        }

        /**
         * 判断某方在前进一步后是否会和对方将/帅对面（飞将）
         * 用于走棋后检测（CheckDetector 中调用）
         */
        fun isKingsFacing(board: Board): Boolean {
            val redKing = board.findKing(Side.RED) ?: return false
            val blackKing = board.findKing(Side.BLACK) ?: return false

            // 同列且之间没有棋子
            if (redKing.col != blackKing.col) return false
            val minRow = minOf(redKing.row, blackKing.row)
            val maxRow = maxOf(redKing.row, blackKing.row)
            for (r in (minRow + 1) until maxRow) {
                if (board[r, redKing.col].isNotEmpty()) return false
            }
            return true
        }
    }
}

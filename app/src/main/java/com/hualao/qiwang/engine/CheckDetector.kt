package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*

/**
 * 将军检测器 — 判断某方是否被将军。
 *
 * 检测逻辑：
 * 1. 找出己方帅/将位置
 * 2. 遍历对方所有棋子，看能否攻击到帅/将
 * 3. 额外检测飞将（双方将/帅对面）
 */
class CheckDetector(private val validator: MoveValidator) {

    /**
     * 判断指定方是否正被将军
     */
    fun isInCheck(board: Board, side: Side): Boolean {
        val kingPos = board.findKing(side) ?: return false

        // 飞将检测：双方将/帅对面即为被将军
        if (MoveValidator.isKingsFacing(board)) return true

        // 检查对方所有棋子是否能攻击到己方帅/将
        val enemy = side.opposite()
        for (r in 0 until Board.ROWS) {
            for (c in 0 until Board.COLS) {
                val piece = board[r, c]
                if (piece.isEmpty()) continue
                if (Side.fromPieceCode(piece) != enemy) continue

                val from = Position(r, c)
                // 注意：只用基本规则校验（不含将军检测自身），否则会递归
                if (validator.isValidMove(board, from, kingPos)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * 判断某着法执行后，己方是否被将军（用于验证走棋合法性）
     */
    fun wouldBeInCheck(board: Board, move: Move, side: Side): Boolean {
        val captured = board.applyMove(move)
        val inCheck = isInCheck(board, side)
        board.undoMove(move, captured)
        return inCheck
    }

    /**
     * 判断是否将杀：将军且无合法应着
     */
    fun isCheckmate(board: Board, side: Side): Boolean {
        if (!isInCheck(board, side)) return false
        val generator = MoveGenerator(validator)
        return generator.generateAllLegalMoves(board, side).isEmpty()
    }

    /**
     * 判断是否困毙：未被将军但无合法应着
     */
    fun isStalemate(board: Board, side: Side): Boolean {
        if (isInCheck(board, side)) return false
        val generator = MoveGenerator(validator)
        return generator.generateAllLegalMoves(board, side).isEmpty()
    }
}

package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*

/**
 * 合法着法生成器 — 生成某方的所有合法着法。
 *
 * 用途：
 * - 将杀/困毙判定（无合法着法）
 * - AI 非法着法回退时的随机保底
 * - 前端合法走法提示
 */
class MoveGenerator(private val validator: MoveValidator) {

    /**
     * 生成某方的所有合法着法（不会导致己方被将军的着法）
     */
    fun generateAllLegalMoves(board: Board, side: Side): List<Move> {
        val moves = mutableListOf<Move>()

        for (r in 0 until Board.ROWS) {
            for (c in 0 until Board.COLS) {
                val piece = board[r, c]
                if (piece.isEmpty()) continue
                if (Side.fromPieceCode(piece) != side) continue

                val from = Position(r, c)
                addCandidateMoves(board, piece, from, moves)
            }
        }

        // 过滤掉会让自己被将军的着法
        val detector = CheckDetector(validator)
        return moves.filterNot { move ->
            val captured = board.applyMove(move)
            val inCheck = detector.isInCheck(board, side)
            board.undoMove(move, captured)
            inCheck
        }
    }

    /**
     * 生成某方在指定位置棋子的所有候选着法（可能包含会导致将军的，需要上层过滤）
     */
    private fun addCandidateMoves(
        board: Board, pieceCode: String, from: Position, out: MutableList<Move>
    ) {
        val piece = Piece.fromCode(pieceCode) ?: return

        val targets = when (piece.type) {
            PieceType.KING -> getKingTargets(from, piece.side)
            PieceType.ADVISOR -> getAdvisorTargets(from, piece.side)
            PieceType.BISHOP -> getBishopTargets(board, from, piece.side)
            PieceType.KNIGHT -> getKnightTargets(board, from)
            PieceType.ROOK -> getRookTargets(board, from)
            PieceType.CANNON -> getCannonTargets(board, from)
            PieceType.PAWN -> getPawnTargets(from, piece.side)
        }

        val side = piece.side
        for (to in targets) {
            if (board.isOwnPiece(to.row, to.col, side)) continue
            val captured = board[to].ifEmpty { null }
            out.add(Move(from, to, pieceCode, captured))
        }
    }

    // ==================== 目标位置生成 ====================

    private fun getKingTargets(from: Position, side: Side): List<Position> {
        val dirs = listOf(
            0 to 1, 0 to -1, 1 to 0, -1 to 0
        )
        return dirs.mapNotNull { (dr, dc) ->
            val to = Position(from.row + dr, from.col + dc)
            if (to.isValid() && MoveValidator.isInPalace(to, side)) to else null
        }
    }

    private fun getAdvisorTargets(from: Position, side: Side): List<Position> {
        val dirs = listOf(
            1 to 1, 1 to -1, -1 to 1, -1 to -1
        )
        return dirs.mapNotNull { (dr, dc) ->
            val to = Position(from.row + dr, from.col + dc)
            if (to.isValid() && MoveValidator.isInPalace(to, side)) to else null
        }
    }

    private fun getBishopTargets(board: Board, from: Position, side: Side): List<Position> {
        val dirs = listOf(
            2 to 2, 2 to -2, -2 to 2, -2 to -2
        )
        return dirs.mapNotNull { (dr, dc) ->
            val to = Position(from.row + dr, from.col + dc)
            if (to.isValid() && !MoveValidator.isCrossedRiver(to, side)) {
                val eyeRow = from.row + dr / 2
                val eyeCol = from.col + dc / 2
                if (board[eyeRow, eyeCol].isEmpty()) to else null
            } else null
        }
    }

    private fun getKnightTargets(board: Board, from: Position): List<Position> {
        // 8个潜在日字位置 + 对应的蹩脚检测
        val jumps = listOf(
            listOf(-2, -1, -1, 0), listOf(-2, 1, -1, 0),
            listOf(2, -1, 1, 0), listOf(2, 1, 1, 0),
            listOf(-1, -2, 0, -1), listOf(-1, 2, 0, 1),
            listOf(1, -2, 0, -1), listOf(1, 2, 0, 1)
        )
        return jumps.mapNotNull { j ->
            val nr = from.row + j[0]
            val nc = from.col + j[1]
            val legR = from.row + j[2]
            val legC = from.col + j[3]
            if (Board.inBounds(nr, nc) && board[legR, legC].isEmpty()) {
                Position(nr, nc)
            } else null
        }
    }

    private fun getRookTargets(board: Board, from: Position): List<Position> {
        return getStraightLineTargets(board, from)
    }

    private fun getCannonTargets(board: Board, from: Position): List<Position> {
        val targets = mutableListOf<Position>()
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

        for ((dr, dc) in dirs) {
            var r = from.row + dr
            var c = from.col + dc

            // 第一阶段：走子（无炮架），遇到任何棋子停止
            while (Board.inBounds(r, c) && board[r, c].isEmpty()) {
                targets.add(Position(r, c))
                r += dr
                c += dc
            }

            // 第二阶段：找炮架（跳过第一个棋子），然后找目标
            if (Board.inBounds(r, c) && board[r, c].isNotEmpty()) {
                // 跳过炮架
                r += dr
                c += dc
                while (Board.inBounds(r, c) && board[r, c].isEmpty()) {
                    r += dr
                    c += dc
                }
                // 炮架后的第一个棋子即为可攻击目标
                if (Board.inBounds(r, c) && board[r, c].isNotEmpty()) {
                    targets.add(Position(r, c))
                }
            }
        }
        return targets
    }

    private fun getPawnTargets(from: Position, side: Side): List<Position> {
        val targets = mutableListOf<Position>()

        if (side == Side.RED) {
            val crossed = from.row <= 4
            // 向前
            Position(from.row - 1, from.col).takeIf { it.isValid() }?.let { targets.add(it) }
            if (crossed) {
                Position(from.row, from.col - 1).takeIf { it.isValid() }?.let { targets.add(it) }
                Position(from.row, from.col + 1).takeIf { it.isValid() }?.let { targets.add(it) }
            }
        } else {
            val crossed = from.row >= 5
            Position(from.row + 1, from.col).takeIf { it.isValid() }?.let { targets.add(it) }
            if (crossed) {
                Position(from.row, from.col - 1).takeIf { it.isValid() }?.let { targets.add(it) }
                Position(from.row, from.col + 1).takeIf { it.isValid() }?.let { targets.add(it) }
            }
        }
        return targets
    }

    private fun getStraightLineTargets(board: Board, from: Position): List<Position> {
        val targets = mutableListOf<Position>()
        val dirs = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

        for ((dr, dc) in dirs) {
            var r = from.row + dr
            var c = from.col + dc
            while (Board.inBounds(r, c)) {
                if (board[r, c].isEmpty()) {
                    targets.add(Position(r, c))
                } else {
                    targets.add(Position(r, c)) // 可吃
                    break
                }
                r += dr
                c += dc
            }
        }
        return targets
    }
}

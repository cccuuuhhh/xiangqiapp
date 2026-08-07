package com.hualao.qiwang.model

/**
 * 棋子模型：由类型 + 颜色决定。
 *
 * 棋盘编码：
 * - 红方：rK(帅) rA(仕) rB(相) rN(馬) rR(車) rC(砲) rP(兵)
 * - 黑方：bK(将) bA(士) bB(象) bN(马) bR(车) bC(炮) bP(卒)
 */
enum class Piece(val side: Side, val type: PieceType) {
    // 红方
    rK(Side.RED, PieceType.KING),
    rA(Side.RED, PieceType.ADVISOR),
    rB(Side.RED, PieceType.BISHOP),
    rN(Side.RED, PieceType.KNIGHT),
    rR(Side.RED, PieceType.ROOK),
    rC(Side.RED, PieceType.CANNON),
    rP(Side.RED, PieceType.PAWN),
    // 黑方
    bK(Side.BLACK, PieceType.KING),
    bA(Side.BLACK, PieceType.ADVISOR),
    bB(Side.BLACK, PieceType.BISHOP),
    bN(Side.BLACK, PieceType.KNIGHT),
    bR(Side.BLACK, PieceType.ROOK),
    bC(Side.BLACK, PieceType.CANNON),
    bP(Side.BLACK, PieceType.PAWN);

    companion object {
        /**
         * 从编码字符串解析棋子，如 "rR" → Piece.rR
         */
        fun fromCode(code: String?): Piece? {
            if (code == null || code.length != 2) return null
            return try {
                Piece.valueOf(code)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

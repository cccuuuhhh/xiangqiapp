package com.hualao.qiwang.model

/**
 * 棋子类型枚举
 */
enum class PieceType(val code: String) {
    KING("K"),       // 帅/将
    ADVISOR("A"),    // 仕/士
    BISHOP("B"),     // 相/象
    KNIGHT("N"),     // 馬/马
    ROOK("R"),       // 車/车
    CANNON("C"),     // 砲/炮
    PAWN("P")        // 兵/卒
}

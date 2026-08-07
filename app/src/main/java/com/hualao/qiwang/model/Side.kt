package com.hualao.qiwang.model

/**
 * 棋子颜色枚举
 */
enum class Side(val value: String) {
    RED("red"),
    BLACK("black");

    fun opposite(): Side = if (this == RED) BLACK else RED

    companion object {
        /**
         * 从棋子编码推断颜色（编码首字符 r=红 b=黑）
         */
        fun fromPieceCode(piece: String?): Side? {
            if (piece.isNullOrEmpty()) return null
            return when (piece[0]) {
                'r' -> RED
                'b' -> BLACK
                else -> null
            }
        }
    }
}

package com.hualao.qiwang.model

/**
 * 着法记录
 */
data class Move(
    val from: Position,
    val to: Position,
    val piece: String,
    val captured: String? = null
) {
    override fun toString(): String {
        return "$piece (${from.row},${from.col})→(${to.row},${to.col})" +
                if (captured != null) " 吃$captured" else ""
    }
}

package com.hualao.qiwang.model

/**
 * 棋盘坐标 (row, col)。
 *
 * 坐标系（唯一标准）：
 * ```
 * row=0  [  ][  ][  ][  ][  ][  ][  ][  ][  ]  黑方底线
 * ...
 * row=9  [  ][  ][  ][  ][  ][  ][  ][  ][  ]  红方底线
 *        col=0  1  2  3  4  5  6  7  8
 * ```
 *
 * 禁止字母混排、中文着法。偏离即 bug。
 */
data class Position(val row: Int, val col: Int) {

    /**
     * 是否在棋盘范围内（0≤row≤9, 0≤col≤8）
     */
    fun isValid(): Boolean = row in 0..9 && col in 0..8

    companion object {
        val INVALID = Position(-1, -1)
    }
}

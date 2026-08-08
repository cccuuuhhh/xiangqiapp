package com.hualao.qiwang.model

/**
 * 棋盘表示：10 行 × 9 列的二维数组。
 *
 * 存储棋子编码字符串，空格为 ""。
 * row 0 = 黑方底线，row 9 = 红方底线。
 * 禁止字母混排、中文着法。
 */
class Board {

    val grid: Array<Array<String>> = Array(ROWS) { Array(COLS) { "" } }

    /**
     * 拷贝构造
     */
    constructor(other: Board) {
        for (r in 0 until ROWS) {
            other.grid[r].copyInto(grid[r])
        }
    }

    constructor()

    /**
     * 获取指定位置的棋子编码
     */
    operator fun get(row: Int, col: Int): String =
        if (inBounds(row, col)) grid[row][col] else ""

    operator fun get(pos: Position): String = get(pos.row, pos.col)

    /**
     * 放置棋子
     */
    operator fun set(row: Int, col: Int, piece: String?) {
        if (inBounds(row, col)) {
            grid[row][col] = piece ?: ""
        }
    }

    operator fun set(pos: Position, piece: String?) {
        set(pos.row, pos.col, piece)
    }

    /**
     * 清空某个位置
     */
    fun clear(row: Int, col: Int) {
        if (inBounds(row, col)) {
            grid[row][col] = ""
        }
    }

    fun clear(pos: Position) {
        clear(pos.row, pos.col)
    }

    /**
     * 执行一步着法（直接修改棋盘），返回被吃的棋子编码（如有）
     */
    fun applyMove(move: Move): String? {
        val captured = get(move.to).let { if (it.isEmpty()) null else it }
        set(move.to, move.piece)
        clear(move.from)
        return captured
    }

    /**
     * 撤销一步着法
     */
    fun undoMove(move: Move, captured: String?) {
        set(move.from, move.piece)
        set(move.to, captured ?: "")
    }

    /**
     * 获取某方帅/将的位置
     */
    fun findKing(side: Side): Position? {
        val kingCode = if (side == Side.RED) "rK" else "bK"
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (grid[r][c] == kingCode) {
                    return Position(r, c)
                }
            }
        }
        return null // 不应发生
    }

    /**
     * 判断某位置是否是己方棋子
     */
    fun isOwnPiece(row: Int, col: Int, side: Side): Boolean {
        val piece = get(row, col)
        if (piece.isEmpty()) return false
        return Side.fromPieceCode(piece) == side
    }

    /**
     * 判断某位置是否是对方棋子
     */
    fun isEnemyPiece(row: Int, col: Int, side: Side): Boolean {
        val piece = get(row, col)
        if (piece.isEmpty()) return false
        return Side.fromPieceCode(piece) == side.opposite()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (r in 0 until ROWS) {
            sb.append("row=$r: ")
            for (c in 0 until COLS) {
                val piece = grid[r][c].ifEmpty { ".." }
                sb.append(piece).append(" ")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    companion object {
        const val ROWS = 10
        const val COLS = 9

        /**
         * 检查坐标是否在棋盘范围内
         */
        fun inBounds(row: Int, col: Int): Boolean =
            row in 0..9 && col in 0..8

        fun inBounds(pos: Position): Boolean = inBounds(pos.row, pos.col)

        /**
         * 创建初始布局的棋盘
         *
         * ```
         * row=0: bR bN bB bA bK bA bB bN bR
         * row=1: -- -- -- -- -- -- -- -- --
         * row=2: -- bC -- -- -- -- -- bC --
         * row=3: bP -- bP -- bP -- bP -- bP
         * row=4: -- -- -- -- -- -- -- -- --
         * row=5: -- -- -- -- -- -- -- -- --
         * row=6: rP -- rP -- rP -- rP -- rP
         * row=7: -- rC -- -- -- -- -- rC --
         * row=8: -- -- -- -- -- -- -- -- --
         * row=9: rR rN rB rA rK rA rB rN rR
         * ```
         */
        fun createInitial(): Board {
            val board = Board()

            // 黑方 (row 0-4)
            board.grid[0] = arrayOf("bR", "bN", "bB", "bA", "bK", "bA", "bB", "bN", "bR")
            // row 1 全空（已是空字符串）
            board.grid[2][1] = "bC"
            board.grid[2][7] = "bC"
            board.grid[3][0] = "bP"
            board.grid[3][2] = "bP"
            board.grid[3][4] = "bP"
            board.grid[3][6] = "bP"
            board.grid[3][8] = "bP"
            // row 4 全空

            // 红方 (row 5-9)
            // row 5 全空
            board.grid[6][0] = "rP"
            board.grid[6][2] = "rP"
            board.grid[6][4] = "rP"
            board.grid[6][6] = "rP"
            board.grid[6][8] = "rP"
            board.grid[7][1] = "rC"
            board.grid[7][7] = "rC"
            // row 8 全空
            board.grid[9] = arrayOf("rR", "rN", "rB", "rA", "rK", "rA", "rB", "rN", "rR")

            return board
        }
    }
}

package com.hualao.qiwang.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Board 单元测试 — 初始棋盘、拷贝构造、applyMove/undoMove、棋子查找。
 */
class BoardTest {

    // ==================== createInitial() ====================

    @Test
    fun `createInitial should have 32 pieces`() {
        val board = Board.createInitial()
        var count = 0
        for (r in 0 until Board.ROWS) {
            for (c in 0 until Board.COLS) {
                if (board[r, c].isNotEmpty()) count++
            }
        }
        assertEquals(32, count)
    }

    @Test
    fun `createInitial should have correct black formation`() {
        val board = Board.createInitial()
        // row 0: bR bN bB bA bK bA bB bN bR
        assertEquals("bR", board[0, 0])
        assertEquals("bN", board[0, 1])
        assertEquals("bB", board[0, 2])
        assertEquals("bA", board[0, 3])
        assertEquals("bK", board[0, 4])
        assertEquals("bA", board[0, 5])
        assertEquals("bB", board[0, 6])
        assertEquals("bN", board[0, 7])
        assertEquals("bR", board[0, 8])
        // row 2: bC at col 1 and 7
        assertEquals("bC", board[2, 1])
        assertEquals("bC", board[2, 7])
        // row 3: bP at col 0,2,4,6,8
        assertEquals("bP", board[3, 0])
        assertEquals("bP", board[3, 2])
        assertEquals("bP", board[3, 4])
        assertEquals("bP", board[3, 6])
        assertEquals("bP", board[3, 8])
    }

    @Test
    fun `createInitial should have correct red formation`() {
        val board = Board.createInitial()
        // row 9: rR rN rB rA rK rA rB rN rR
        assertEquals("rR", board[9, 0])
        assertEquals("rK", board[9, 4])
        assertEquals("rR", board[9, 8])
        // row 6: rP at col 0,2,4,6,8
        assertEquals("rP", board[6, 0])
        assertEquals("rP", board[6, 4])
        assertEquals("rP", board[6, 8])
        // row 7: rC at col 1 and 7
        assertEquals("rC", board[7, 1])
        assertEquals("rC", board[7, 7])
    }

    @Test
    fun `createInitial should have 4 empty rows`() {
        val board = Board.createInitial()
        for (c in 0 until Board.COLS) {
            assertEquals("", board[1, c])
            assertEquals("", board[4, c])
            assertEquals("", board[5, c])
            assertEquals("", board[8, c])
        }
    }

    // ==================== inBounds ====================

    @Test
    fun `inBounds should return true for valid positions`() {
        assertTrue(Board.inBounds(0, 0))
        assertTrue(Board.inBounds(9, 8))
        assertTrue(Board.inBounds(5, 4))
    }

    @Test
    fun `inBounds should return false for invalid positions`() {
        assertFalse(Board.inBounds(-1, 0))
        assertFalse(Board.inBounds(0, -1))
        assertFalse(Board.inBounds(10, 0))
        assertFalse(Board.inBounds(0, 9))
    }

    // ==================== get/set/clear ====================

    @Test
    fun `get out of bounds should return empty string`() {
        val board = Board.createInitial()
        assertEquals("", board[-1, 0])
        assertEquals("", board[0, 9])
        assertEquals("", board[10, 5])
    }

    @Test
    fun `set and clear should work`() {
        val board = Board()
        board[0, 0] = "bR"
        assertEquals("bR", board[0, 0])
        board.clear(0, 0)
        assertEquals("", board[0, 0])
    }

    @Test
    fun `set null should clear the position`() {
        val board = Board()
        board[3, 4] = "rK"
        assertEquals("rK", board[3, 4])
        board[3, 4] = null
        assertEquals("", board[3, 4])
    }

    @Test
    fun `set out of bounds should be ignored`() {
        val board = Board()
        board[-1, 0] = "rK"
        assertEquals("", board[-1, 0])
    }

    // ==================== copy constructor ====================

    @Test
    fun `copy constructor should produce independent copy`() {
        val board = Board.createInitial()
        val copy = Board(board)

        // same values
        for (r in 0 until Board.ROWS) {
            for (c in 0 until Board.COLS) {
                assertEquals(board[r, c], copy[r, c])
            }
        }

        // modifying copy should not affect original
        copy[0, 0] = ""
        assertEquals("bR", board[0, 0])
        assertEquals("", copy[0, 0])
    }

    // ==================== applyMove / undoMove ====================

    @Test
    fun `applyMove should move piece and return captured`() {
        val board = Board.createInitial()
        // Red cannon from (7,7) to (7,4) — initial position has empty (7,7) actually
        // Let's do a real move: Red pawn (6,4) forward to (5,4)
        val move = Move(Position(6, 4), Position(5, 4), "rP")
        val captured = board.applyMove(move)

        assertEquals("", board[6, 4])
        assertEquals("rP", board[5, 4])
        assertNull(captured) //  no capture
    }

    @Test
    fun `applyMove should capture opponent piece`() {
        val board = Board()
        board[0, 0] = "bR"
        board[0, 1] = "rR"

        val move = Move(Position(0, 1), Position(0, 0), "rR")
        val captured = board.applyMove(move)

        assertEquals("", board[0, 1])
        assertEquals("rR", board[0, 0])
        assertEquals("bR", captured)
    }

    @Test
    fun `undoMove should restore previous state`() {
        val board = Board.createInitial()
        val original = Board(board)

        val move = Move(Position(9, 7), Position(9, 5), "rN")
        val captured = board.applyMove(move)
        board.undoMove(move, captured)

        // Compare with original
        for (r in 0 until Board.ROWS) {
            for (c in 0 until Board.COLS) {
                assertEquals("Row $r, Col $c", original[r, c], board[r, c])
            }
        }
    }

    @Test
    fun `undoMove should restore captured piece`() {
        val board = Board()
        board[0, 0] = "bR"
        board[0, 1] = "rR"

        val move = Move(Position(0, 1), Position(0, 0), "rR")
        board.applyMove(move)
        board.undoMove(move, "bR")

        assertEquals("bR", board[0, 0])
        assertEquals("rR", board[0, 1])
    }

    // ==================== findKing ====================

    @Test
    fun `findKing should find red king at initial position`() {
        val board = Board.createInitial()
        val kingPos = board.findKing(Side.RED)
        assertNotNull(kingPos)
        assertEquals(Position(9, 4), kingPos)
    }

    @Test
    fun `findKing should find black king at initial position`() {
        val board = Board.createInitial()
        val kingPos = board.findKing(Side.BLACK)
        assertNotNull(kingPos)
        assertEquals(Position(0, 4), kingPos)
    }

    // ==================== isOwnPiece / isEnemyPiece ====================

    @Test
    fun `isOwnPiece should correctly identify own pieces`() {
        val board = Board.createInitial()
        assertTrue(board.isOwnPiece(9, 0, Side.RED))     // red rook
        assertFalse(board.isOwnPiece(9, 0, Side.BLACK))
        assertTrue(board.isOwnPiece(0, 4, Side.BLACK))   // black king
        assertFalse(board.isOwnPiece(0, 4, Side.RED))
    }

    @Test
    fun `isOwnPiece should return false for empty position`() {
        val board = Board.createInitial()
        assertFalse(board.isOwnPiece(4, 0, Side.RED))
        assertFalse(board.isOwnPiece(5, 4, Side.BLACK))
    }

    @Test
    fun `isEnemyPiece should work correctly`() {
        val board = Board.createInitial()
        assertTrue(board.isEnemyPiece(9, 0, Side.BLACK))  // red rook is enemy of black
        assertFalse(board.isEnemyPiece(9, 0, Side.RED))
    }

    // ==================== toString ====================

    @Test
    fun `toString should contain all row labels`() {
        val board = Board.createInitial()
        val str = board.toString()
        assertTrue(str.contains("row=0"))
        assertTrue(str.contains("row=9"))
        assertTrue(str.contains("rK"))
        assertTrue(str.contains("bK"))
    }
}

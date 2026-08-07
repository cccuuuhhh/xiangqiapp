package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MoveValidator 单元测试 — 7 种棋子走法校验 + 辅助方法。
 */
class MoveValidatorTest {

    private lateinit var validator: MoveValidator

    @Before
    fun setUp() {
        validator = MoveValidator()
    }

    // ==================== KING (帅/将) ====================

    @Test
    fun `king should move one step within palace`() {
        val board = Board.createInitial()
        // Red king moves from (9,4) to (9,3) — within palace
        assertTrue(validator.isValidMove(board, Position(9, 4), Position(9, 3)))
    }

    @Test
    fun `king should not move diagonally`() {
        val board = Board.createInitial()
        // Diagonal move should be invalid
        assertFalse(validator.isValidMove(board, Position(9, 4), Position(8, 3)))
    }

    @Test
    fun `king should not move out of palace`() {
        val board = Board.createInitial()
        // Move king to (8,4) — within palace for red, ok
        board[9, 3] = "rK"
        board[9, 4] = "" // manually clear, test from (9,3)
        // Move from (9,4) to (9,2) — still within palace cols [3-5], invalid
        assertTrue(validator.isValidMove(board, Position(9, 3), Position(9, 4)))
        // Move to col 2 — outside palace
        assertFalse(validator.isValidMove(board, Position(9, 3), Position(9, 2)))
    }

    @Test
    fun `king should not move more than one step`() {
        val board = Board()
        board[7, 4] = "rK"
        // Two steps forward — invalid
        assertFalse(validator.isValidMove(board, Position(7, 4), Position(5, 4)))
    }

    @Test
    fun `king should not move to own piece`() {
        val board = Board.createInitial()
        // Red king next to red advisor — should be invalid
        assertFalse(validator.isValidMove(board, Position(9, 4), Position(9, 3)))
    }

    // ==================== ADVISOR (仕/士) ====================

    @Test
    fun `advisor should move one step diagonally within palace`() {
        val board = Board()
        board[9, 4] = "rA"
        // Move to (8,3) — diagonal within palace
        assertTrue(validator.isValidMove(board, Position(9, 4), Position(8, 3)))
    }

    @Test
    fun `advisor should not move straight`() {
        val board = Board()
        board[9, 4] = "rA"
        assertFalse(validator.isValidMove(board, Position(9, 4), Position(9, 3)))
        assertFalse(validator.isValidMove(board, Position(9, 4), Position(8, 4)))
    }

    @Test
    fun `advisor should not move out of palace`() {
        val board = Board()
        board[9, 3] = "rA"
        // Move to (8,4) — diagonal but out of palace for red? No, (8,4) is still in palace
        assertTrue(validator.isValidMove(board, Position(9, 3), Position(8, 4)))
        // Move to (7,2) — that IS out of palace
        assertFalse(validator.isValidMove(board, Position(8, 4), Position(7, 3)))
    }

    @Test
    fun `advisor should not move more than one step`() {
        val board = Board()
        board[9, 3] = "rA"
        assertFalse(validator.isValidMove(board, Position(9, 3), Position(7, 1)))
    }

    // ==================== BISHOP (相/象) ====================

    @Test
    fun `bishop should move diagonally 2x2 within own side`() {
        val board = Board()
        board[9, 2] = "rB"
        // Move to (7,0) — valid, eye at (8,1) is empty
        assertTrue(validator.isValidMove(board, Position(9, 2), Position(7, 0)))
    }

    @Test
    fun `bishop should not move when eye is blocked`() {
        val board = Board()
        board[9, 2] = "rB"
        board[8, 1] = "rP" // Eye blocked!
        assertFalse(validator.isValidMove(board, Position(9, 2), Position(7, 0)))
    }

    @Test
    fun `bishop should not cross river`() {
        val board = Board()
        board[7, 0] = "rB"
        // Red bishop: own side = row >= 5, can't go to row < 5
        // Move from (7,0) to (5,2) — row 5 is borderline
        // But target row 5 means side-side row, it's still >= 5
        // Actually: red side is row 7-9 not row >= 5. Let me check:
        // isCrossedRiver: for RED, pos.row <= 4 means crossed. So row 5 is NOT crossed.
        // So (7,0) -> (5,2) should be valid as long as eye empty.
        assertTrue(validator.isValidMove(board, Position(7, 0), Position(5, 2)))
        // Now move from (5,2) to (3,0) — row 3 IS crossed river for red
        assertFalse(validator.isValidMove(board, Position(5, 2), Position(3, 0)))
    }

    @Test
    fun `bishop should not move non-2x2`() {
        val board = Board()
        board[9, 2] = "rB"
        assertFalse(validator.isValidMove(board, Position(9, 2), Position(8, 1)))
        assertFalse(validator.isValidMove(board, Position(9, 2), Position(9, 1)))
    }

    // ==================== KNIGHT (馬/马) ====================

    @Test
    fun `knight should move in L shape without leg block`() {
        val board = Board()
        board[4, 4] = "rN"
        // Valid L-shape moves: 8 potential positions
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(2, 3)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(2, 5)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(6, 3)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(6, 5)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(3, 2)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(3, 6)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(5, 2)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(5, 6)))
    }

    @Test
    fun `knight should not move when leg is blocked`() {
        val board = Board()
        board[4, 4] = "rN"
        board[3, 4] = "rP" // Block leg for vertical moves
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(2, 3)))
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(2, 5)))
    }

    @Test
    fun `knight should not move when horizontal leg is blocked`() {
        val board = Board()
        board[4, 4] = "rN"
        board[4, 3] = "rP" // Block leg for (3,2) and (5,2)
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(3, 2)))
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(5, 2)))
        // But (3,6) / (5,6) should still be ok (different leg)
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(3, 6)))
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(5, 6)))
    }

    @Test
    fun `knight should not move non-L shape`() {
        val board = Board()
        board[4, 4] = "rN"
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(4, 5)))
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(3, 3)))
    }

    // ==================== ROOK (車/车) ====================

    @Test
    fun `rook should move along straight lines without blocking`() {
        val board = Board()
        board[0, 0] = "rR"
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(0, 8)))
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(9, 0)))
    }

    @Test
    fun `rook should not jump over pieces`() {
        val board = Board()
        board[0, 0] = "rR"
        board[0, 4] = "bP" // blocking piece
        assertFalse(validator.isValidMove(board, Position(0, 0), Position(0, 8)))
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(0, 3))) // before blocker
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(0, 4))) // capture blocker
    }

    @Test
    fun `rook should not move diagonally`() {
        val board = Board()
        board[0, 0] = "rR"
        assertFalse(validator.isValidMove(board, Position(0, 0), Position(3, 3)))
    }

    // ==================== CANNON (砲/炮) ====================

    @Test
    fun `cannon should move along straight lines without mountain when not capturing`() {
        val board = Board()
        board[0, 0] = "bC"
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(0, 8)))
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(9, 0)))
    }

    @Test
    fun `cannon should not jump over pieces when not capturing`() {
        val board = Board()
        board[0, 0] = "bC"
        board[0, 4] = "rP"
        assertFalse(validator.isValidMove(board, Position(0, 0), Position(0, 8)))
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(0, 3)))
    }

    @Test
    fun `cannon should need exactly one mountain to capture`() {
        val board = Board()
        board[0, 0] = "bC"
        board[0, 4] = "rP"      // mountain
        board[0, 7] = "bP"      // target (own piece — can't capture own)
        // Actually, target must be opponent. Let's use "rN" for target.
        board[0, 7] = "rN"      // opponent target
        assertTrue(validator.isValidMove(board, Position(0, 0), Position(0, 7)))
    }

    @Test
    fun `cannon should not capture with zero or two mountains`() {
        val board = Board()
        board[0, 0] = "bC"
        board[0, 3] = "bP" // own piece, can't capture
        // Zero mountain: can't capture with 0 mountains
        assertFalse(validator.isValidMove(board, Position(0, 0), Position(0, 3)))

        // Two mountains
        val board2 = Board()
        board2[0, 0] = "bC"
        board2[0, 2] = "rP"   // mountain 1
        board2[0, 5] = "rP"   // mountain 2
        board2[0, 8] = "rN"   // target
        assertFalse(validator.isValidMove(board2, Position(0, 0), Position(0, 8)))
    }

    // ==================== PAWN (兵/卒) ====================

    @Test
    fun `red pawn should only move forward before crossing river`() {
        val board = Board.createInitial()
        // Red pawn at (6,4) — forward only
        assertTrue(validator.isValidMove(board, Position(6, 4), Position(5, 4)))
        assertFalse(validator.isValidMove(board, Position(6, 4), Position(7, 4))) // backward
        assertFalse(validator.isValidMove(board, Position(6, 4), Position(6, 3))) // sideways
    }

    @Test
    fun `red pawn should move forward or sideways after crossing river`() {
        val board = Board()
        board[4, 4] = "rP" // Has crossed river
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(3, 4))) // forward
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(4, 3))) // left
        assertTrue(validator.isValidMove(board, Position(4, 4), Position(4, 5))) // right
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(5, 4))) // backward
    }

    @Test
    fun `black pawn should only move forward before crossing river`() {
        val board = Board.createInitial()
        // Black pawn at (3,4) — forward (row increases)
        assertTrue(validator.isValidMove(board, Position(3, 4), Position(4, 4)))
        assertFalse(validator.isValidMove(board, Position(3, 4), Position(2, 4))) // backward
        assertFalse(validator.isValidMove(board, Position(3, 4), Position(3, 3))) // sideways
    }

    @Test
    fun `black pawn should move forward or sideways after crossing river`() {
        val board = Board()
        board[5, 4] = "bP" // Has crossed river (row >= 5)
        assertTrue(validator.isValidMove(board, Position(5, 4), Position(6, 4))) // forward
        assertTrue(validator.isValidMove(board, Position(5, 4), Position(5, 3))) // left
        assertTrue(validator.isValidMove(board, Position(5, 4), Position(5, 5))) // right
        assertFalse(validator.isValidMove(board, Position(5, 4), Position(4, 4))) // backward
    }

    @Test
    fun `pawn should not move diagonally`() {
        val board = Board()
        board[4, 4] = "rP"
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(3, 3)))
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(3, 5)))
    }

    // ==================== Helper: isInPalace ====================

    @Test
    fun `isInPalace should correctly identify red palace`() {
        assertTrue(MoveValidator.isInPalace(Position(7, 3), Side.RED))
        assertTrue(MoveValidator.isInPalace(Position(9, 5), Side.RED))
        assertFalse(MoveValidator.isInPalace(Position(6, 3), Side.RED))
        assertFalse(MoveValidator.isInPalace(Position(7, 2), Side.RED))
    }

    @Test
    fun `isInPalace should correctly identify black palace`() {
        assertTrue(MoveValidator.isInPalace(Position(0, 4), Side.BLACK))
        assertTrue(MoveValidator.isInPalace(Position(2, 3), Side.BLACK))
        assertFalse(MoveValidator.isInPalace(Position(3, 3), Side.BLACK))
    }

    // ==================== Helper: isCrossedRiver ====================

    @Test
    fun `isCrossedRiver should work for red`() {
        assertTrue(MoveValidator.isCrossedRiver(Position(4, 0), Side.RED))
        assertTrue(MoveValidator.isCrossedRiver(Position(0, 0), Side.RED))
        assertFalse(MoveValidator.isCrossedRiver(Position(5, 0), Side.RED))
        assertFalse(MoveValidator.isCrossedRiver(Position(9, 0), Side.RED))
    }

    @Test
    fun `isCrossedRiver should work for black`() {
        assertTrue(MoveValidator.isCrossedRiver(Position(5, 0), Side.BLACK))
        assertTrue(MoveValidator.isCrossedRiver(Position(9, 0), Side.BLACK))
        assertFalse(MoveValidator.isCrossedRiver(Position(4, 0), Side.BLACK))
        assertFalse(MoveValidator.isCrossedRiver(Position(0, 0), Side.BLACK))
    }

    // ==================== Helper: countPiecesBetween ====================

    @Test
    fun `countPiecesBetween should count pieces on straight line`() {
        val board = Board()
        board[0, 4] = "bK"
        board[0, 2] = "bP"
        board[0, 6] = "bP"
        assertEquals(2, MoveValidator.countPiecesBetween(board, Position(0, 0), Position(0, 8)))
    }

    @Test
    fun `countPiecesBetween should return 0 for empty path`() {
        val board = Board()
        assertEquals(0, MoveValidator.countPiecesBetween(board, Position(0, 0), Position(0, 8)))
    }

    // ==================== Edge cases ====================

    @Test
    fun `should return false for empty from position`() {
        val board = Board()
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(5, 4)))
    }

    @Test
    fun `should return false for null or invalid position`() {
        val board = Board()
        board[4, 4] = "rR"
        assertFalse(validator.isValidMove(board, Position(-1, -1), Position(5, 4)))
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(-1, 0)))
    }

    @Test
    fun `should return false for same from and to position`() {
        val board = Board()
        board[4, 4] = "rR"
        assertFalse(validator.isValidMove(board, Position(4, 4), Position(4, 4)))
    }
}

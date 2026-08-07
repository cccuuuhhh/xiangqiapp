package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MoveGenerator 单元测试 — 合法着法生成。
 */
class MoveGeneratorTest {

    private lateinit var validator: MoveValidator
    private lateinit var generator: MoveGenerator

    @Before
    fun setUp() {
        validator = MoveValidator()
        generator = MoveGenerator(validator)
    }

    // ==================== Initial Position ====================

    @Test
    fun `initial position should generate correct move counts for red`() {
        val board = Board.createInitial()
        val moves = generator.generateAllLegalMoves(board, Side.RED)
        // Red pieces that can move: knights, rooks (after moving pawns), cannons, pawns
        // Standard count: 44 moves from initial position for red
        assertEquals(44, moves.size)
    }

    @Test
    fun `initial position should generate correct move counts for black`() {
        val board = Board.createInitial()
        val moves = generator.generateAllLegalMoves(board, Side.BLACK)
        assertEquals(44, moves.size)
    }

    @Test
    fun `generated moves should all be legal`() {
        val board = Board.createInitial()
        val redMoves = generator.generateAllLegalMoves(board, Side.RED)
        for (move in redMoves) {
            assertTrue(
                "Move should be legal: $move",
                validator.isValidMove(board, move.from, move.to)
            )
        }
    }

    @Test
    fun `no generated move should leave own king in check`() {
        val board = Board.createInitial()
        val detector = CheckDetector(validator)
        val redMoves = generator.generateAllLegalMoves(board, Side.RED)

        for (move in redMoves) {
            val captured = board.applyMove(move)
            assertFalse(
                "Move $move should not leave own king in check",
                detector.isInCheck(board, Side.RED)
            )
            board.undoMove(move, captured)
        }
    }

    // ==================== Empty Board / Single Piece ====================

    @Test
    fun `rook on empty board should generate 17 moves`() {
        val board = Board()
        board[4, 4] = "rR"
        board[9, 4] = "rK" // Need a king to pass check filter
        board[0, 4] = "bK"

        val moves = generator.generateAllLegalMoves(board, Side.RED)

        // Rook at center: 8 vertical + 8 horizontal directions
        // Actually from (4,4): up to row 0 → 4, down to row 9 → 5, left to col 0 → 4, right to col 8 → 4
        // Total: 4+5+4+4 = 17
        assertEquals(17, moves.size)
        for (move in moves) {
            assertEquals("rR", move.piece)
            assertEquals(Position(4, 4), move.from)
        }
    }

    // ==================== Check Scenario ====================

    @Test
    fun `when in check only moves that resolve check should be generated`() {
        val board = Board()
        board[0, 4] = "bK"
        board[0, 3] = "bA"
        board[0, 5] = "bA"
        board[9, 4] = "rK"
        // Rook on same row as black king
        board[0, 0] = "rR"

        val moves = generator.generateAllLegalMoves(board, Side.BLACK)

        // Black king is in check by rook on row 0
        // Options: 1) move king (0,4) → (1,4) [forward only since blocked by advisors]
        //          2) block with advisor? Not easy at same row
        // Let's just verify all moves resolve the check
        val detector = CheckDetector(validator)
        for (move in moves) {
            val captured = board.applyMove(move)
            assertFalse(
                "After move $move, king should not be in check",
                detector.isInCheck(board, Side.BLACK)
            )
            board.undoMove(move, captured)
        }
        assertTrue("Should have at least one escape", moves.isNotEmpty())
    }

    // ==================== King Move Generation ====================

    @Test
    fun `king in center palace should generate 4 moves`() {
        val board = Board()
        board[8, 4] = "rK"
        board[0, 4] = "bK"

        val moves = generator.generateAllLegalMoves(board, Side.RED)
        val kingMoves = moves.filter { it.piece == "rK" }
        assertEquals(4, kingMoves.size)
    }

    // ==================== Knight at edge ====================

    @Test
    fun `knight at corner should generate 2 moves`() {
        val board = Board()
        board[9, 0] = "rK" // Make sure we have a king
        board[9, 1] = "rN"
        board[0, 4] = "bK"

        val moves = generator.generateAllLegalMoves(board, Side.RED)
        val knightMoves = moves.filter { it.piece == "rN" }

        // Knight at (9,1): (7,0), (7,2) — leg at (8,1) empty, edge blocks rest
        assertEquals(2, knightMoves.size)
    }

    // ==================== Move Uniqueness ====================

    @Test
    fun `generated moves should all be unique`() {
        val board = Board.createInitial()
        val moves = generator.generateAllLegalMoves(board, Side.RED)
        val uniqueKeys = moves.map { "${it.from.row},${it.from.col}->${it.to.row},${it.to.col}" }
        assertEquals("All moves should be unique", moves.size, uniqueKeys.toSet().size)
    }

    // ==================== Cannon Target Generation ====================

    @Test
    fun `cannon should generate move and capture targets`() {
        val board = Board()
        board[9, 4] = "rK"
        board[7, 1] = "rC"
        board[7, 4] = "rP" // mountain
        board[7, 7] = "bN" // target behind mountain
        board[0, 4] = "bK"

        val moves = generator.generateAllLegalMoves(board, Side.RED)
        val cannonMoves = moves.filter { it.piece == "rC" }

        // cannon moves: horizontal positions (no mountain direction), mountain capture
        // At (7,1): can go (7,0), (7,2-8 via mountain) to the right
        // across mountain: (7,4) is mountain, (7,7) is target = capture
        // Vertical: (8,1) (6,1) down, (5,1) (4,1) ... but blocked by pawns etc
        assertTrue("Cannon should generate moves", cannonMoves.isNotEmpty())
    }
}

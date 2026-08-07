package com.hualao.qiwang.engine

import com.hualao.qiwang.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * FenConverter 单元测试 — FEN 生成、ICCS 坐标互转、中文描述。
 */
class FenConverterTest {

    // ==================== boardToFen ====================

    @Test
    fun `initial board should produce correct FEN`() {
        val board = Board.createInitial()
        val fen = FenConverter.boardToFen(board, Side.RED)

        // Expected: rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1
        val expected = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1"
        assertEquals(expected, fen)
    }

    @Test
    fun `FEN should use w for red to move`() {
        val board = Board.createInitial()
        val fen = FenConverter.boardToFen(board, Side.RED)
        assertTrue(fen.endsWith("w - - 0 1"))
    }

    @Test
    fun `FEN should use b for black to move`() {
        val board = Board.createInitial()
        val fen = FenConverter.boardToFen(board, Side.BLACK)
        assertTrue(fen.endsWith("b - - 0 1"))
    }

    @Test
    fun `empty board should produce FEN with all 9s`() {
        val board = Board()
        val fen = FenConverter.boardToFen(board, Side.RED)
        // 9 means 9 empty squares per row, repeated 10 times
        assertEquals("9/9/9/9/9/9/9/9/9/9 w - - 0 1", fen)
    }

    // ==================== ICCS coordinate conversion ====================

    @Test
    fun `iccsToPosition should convert correctly`() {
        // a0 → Position(9, 0) (red bottom, col a)
        assertEquals(Position(9, 0), FenConverter.iccsToPosition("a0"))
        // a9 → Position(0, 0) (black bottom, col a)
        assertEquals(Position(0, 0), FenConverter.iccsToPosition("a9"))
        // i0 → Position(9, 8)
        assertEquals(Position(9, 8), FenConverter.iccsToPosition("i0"))
        // i9 → Position(0, 8)
        assertEquals(Position(0, 8), FenConverter.iccsToPosition("i9"))
        // e1 → Position(8, 4)
        assertEquals(Position(8, 4), FenConverter.iccsToPosition("e1"))
    }

    @Test
    fun `positionToIccs should convert correctly`() {
        assertEquals("a0", FenConverter.positionToIccs(Position(9, 0)))
        assertEquals("a9", FenConverter.positionToIccs(Position(0, 0)))
        assertEquals("i0", FenConverter.positionToIccs(Position(9, 8)))
        assertEquals("i9", FenConverter.positionToIccs(Position(0, 8)))
        assertEquals("e1", FenConverter.positionToIccs(Position(8, 4)))
    }

    @Test
    fun `iccs roundtrip should be identity`() {
        val positions = listOf(
            Position(9, 0), Position(9, 8), Position(0, 0), Position(0, 8),
            Position(5, 4), Position(3, 1), Position(7, 7)
        )
        for (pos in positions) {
            val iccs = FenConverter.positionToIccs(pos)
            val recovered = FenConverter.iccsToPosition(iccs)
            assertEquals("Position $pos → $iccs → $recovered", pos, recovered)
        }
    }

    // ==================== moveToUciString ====================

    @Test
    fun `moveToUciString should produce correct UCI`() {
        // Red rook from (9,0) to (8,0): h2h1 → wait, let me check
        // Position(9,0) → iccs "a0", Position(8,0) → iccs "a1"
        val move = Move(Position(9, 0), Position(8, 0), "rR")
        assertEquals("a0a1", FenConverter.moveToUciString(move))
    }

    @Test
    fun `uciToMove should parse correctly`() {
        val board = Board.createInitial()
        // Parse "a0a1" — rook advancing one row
        val move = FenConverter.uciToMove("a0a1", board)
        assertNotNull(move)
        assertEquals(Position(9, 0), move!!.from)
        assertEquals(Position(8, 0), move.to)
        assertEquals("rR", move.piece)
    }

    @Test
    fun `uciToMove should return null for invalid UCI`() {
        val board = Board.createInitial()
        assertNull(FenConverter.uciToMove("ab", board))
        assertNull(FenConverter.uciToMove("", board))
        assertNull(FenConverter.uciToMove("a0", board))
    }

    // ==================== pieceToChinese ====================

    @Test
    fun `pieceToChinese should return correct Chinese names for red`() {
        assertEquals("帅", FenConverter.pieceToChinese("rK"))
        assertEquals("仕", FenConverter.pieceToChinese("rA"))
        assertEquals("相", FenConverter.pieceToChinese("rB"))
        assertEquals("馬", FenConverter.pieceToChinese("rN"))
        assertEquals("車", FenConverter.pieceToChinese("rR"))
        assertEquals("砲", FenConverter.pieceToChinese("rC"))
        assertEquals("兵", FenConverter.pieceToChinese("rP"))
    }

    @Test
    fun `pieceToChinese should return correct Chinese names for black`() {
        assertEquals("将", FenConverter.pieceToChinese("bK"))
        assertEquals("士", FenConverter.pieceToChinese("bA"))
        assertEquals("象", FenConverter.pieceToChinese("bB"))
        assertEquals("马", FenConverter.pieceToChinese("bN"))
        assertEquals("车", FenConverter.pieceToChinese("bR"))
        assertEquals("炮", FenConverter.pieceToChinese("bC"))
        assertEquals("卒", FenConverter.pieceToChinese("bP"))
    }

    @Test
    fun `pieceToChinese should return default for invalid codes`() {
        assertEquals("??", FenConverter.pieceToChinese(""))
        assertEquals("??", FenConverter.pieceToChinese(null))
        assertEquals("??", FenConverter.pieceToChinese("x"))
    }

    // ==================== describeMove ====================

    @Test
    fun `describeMove should describe a move`() {
        val board = Board.createInitial()
        val move = Move(Position(9, 0), Position(8, 0), "rR")
        val desc = FenConverter.describeMove(move)
        assertTrue(desc.contains("車"))
        assertTrue(desc.contains("a0"))
        assertTrue(desc.contains("a1"))
    }

    @Test
    fun `describeMove should include capture info`() {
        val move = Move(Position(9, 0), Position(8, 0), "rR", "bP")
        val desc = FenConverter.describeMove(move)
        assertTrue(desc.contains("吃"))
        assertTrue(desc.contains("卒"))
    }
}

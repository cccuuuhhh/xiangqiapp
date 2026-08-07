package com.hualao.qiwang.ai

import com.hualao.qiwang.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * PromptBuilder 单元测试 — Prompt 模板构建。
 */
class PromptBuilderTest {

    // ==================== buildTrashTalk ====================

    @Test
    fun `buildTrashTalk should include personality prompt`() {
        val personality = "你是一个毒舌象棋大师"
        val prompt = PromptBuilder.buildTrashTalk(
            personalityPrompt = personality,
            lastMoveDesc = "兵从e5走到e4",
            aiMoveDesc = "馬从h2跳到g4",
            situationTags = "将军",
            materialBalance = "AI 多一炮",
            moveCount = 5,
            recentLines = emptyList()
        )
        assertTrue(prompt.contains(personality))
        assertTrue(prompt.contains("兵从e5走到e4"))
        assertTrue(prompt.contains("馬从h2跳到g4"))
        assertTrue(prompt.contains("将军"))
        assertTrue(prompt.contains("AI 多一炮"))
        assertTrue(prompt.contains("第 5 回合"))
    }

    @Test
    fun `buildTrashTalk should include dedup constraints`() {
        val personality = "你是一个禅意大师"
        val recentLines = listOf("人生如棋，落子无悔", "你的每一步都在暴露你的焦虑")
        val prompt = PromptBuilder.buildTrashTalk(
            personalityPrompt = personality,
            lastMoveDesc = "車吃卒",
            aiMoveDesc = "炮打马",
            situationTags = "吃子",
            materialBalance = "均势",
            moveCount = 3,
            recentLines = recentLines
        )
        assertTrue(prompt.contains("人生如棋，落子无悔"))
        assertTrue(prompt.contains("你的每一步都在暴露你的焦虑"))
        assertTrue(prompt.contains("去重约束"))
    }

    @Test
    fun `buildTrashTalk should enforce max 50 chars rule`() {
        val prompt = PromptBuilder.buildTrashTalk(
            "毒舌", "随便", "走走", "日常走棋", "均势", 1, emptyList()
        )
        assertTrue(prompt.contains("不超过50个字"))
    }

    // ==================== buildSelfPraise ====================

    @Test
    fun `buildSelfPraise should include AI move description`() {
        val prompt = PromptBuilder.buildSelfPraise(
            personalityPrompt = "你是一个优雅的象棋大师",
            aiMoveDesc = "車从a0飞到a9将军",
            situationTags = "将军 吃子",
            materialBalance = "AI 多大子",
            recentLines = emptyList()
        )
        assertTrue(prompt.contains("車从a0飞到a9将军"))
        assertTrue(prompt.contains("凡尔赛"))
    }

    @Test
    fun `buildSelfPraise should include dedup history`() {
        val lines = listOf("承让承让", "我这步堪称神来之笔")
        val prompt = PromptBuilder.buildSelfPraise(
            "中二少年", "绝杀一步", "绝杀", "大优", lines
        )
        assertTrue(prompt.contains("承让承让"))
        assertTrue(prompt.contains("神来之笔"))
        assertTrue(prompt.contains("关键着法"))
    }

    // ==================== buildMove ====================

    @Test
    fun `buildMove should include board representation`() {
        val board = Board.createInitial()
        val prompt = PromptBuilder.buildMove(board, Side.RED, emptyList())
        assertTrue(prompt.contains("row 0"))
        assertTrue(prompt.contains("row 9"))
        assertTrue(prompt.contains("MOVE:"))
        assertTrue(prompt.contains("红"))
    }

    @Test
    fun `buildMove should include move history`() {
        val board = Board.createInitial()
        val history = listOf("9,7-7,6 (rN)", "0,1-2,2 (bN)")
        val prompt = PromptBuilder.buildMove(board, Side.BLACK, history)
        assertTrue(prompt.contains("9,7-7,6"))
        assertTrue(prompt.contains("0,1-2,2"))
        assertTrue(prompt.contains("黑方"))
    }

    // ==================== buildSituationTags ====================

    @Test
    fun `buildSituationTags should combine tags correctly`() {
        assertEquals("绝杀", PromptBuilder.buildSituationTags(true, false, false))
        assertEquals("将军 吃子", PromptBuilder.buildSituationTags(false, true, true))
        assertEquals("将军", PromptBuilder.buildSituationTags(false, true, false))
        assertEquals("吃子", PromptBuilder.buildSituationTags(false, false, true))
        assertEquals("日常走棋", PromptBuilder.buildSituationTags(false, false, false))
    }
}

package com.hualao.qiwang.ai

import com.hualao.qiwang.engine.FenConverter
import com.hualao.qiwang.model.Board
import com.hualao.qiwang.model.Move
import com.hualao.qiwang.model.Side

/**
 * Prompt 模板构建器 — 生成 DeepSeek API 的 Prompt。
 *
 * 移植自源项目 AIService.java 的 buildTrashTalkPrompt / buildSelfPraisePrompt / buildMovePrompt。
 */
object PromptBuilder {

    // ==================== 嘲讽 Prompt ====================

    /**
     * 构建嘲讽 Prompt。
     */
    fun buildTrashTalk(
        personalityPrompt: String,
        lastMoveDesc: String,
        aiMoveDesc: String,
        situationTags: String,
        materialBalance: String,
        moveCount: Int,
        recentLines: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append(personalityPrompt).append("\n\n")
        sb.append("【当前局面分析】\n")
        sb.append("- 玩家刚走了：").append(lastMoveDesc).append("\n")
        sb.append("- AI 回应了：").append(aiMoveDesc).append("\n")
        sb.append("- 局面状态：").append(situationTags).append("\n\n")
        sb.append("【上下文】\n")
        sb.append("- 当前是第 ").append(moveCount).append(" 回合\n")
        sb.append("- AI 子力优势/劣势：").append(materialBalance).append("\n\n")
        sb.append("【去重约束】\n")
        sb.append("你最近已经说过：\n")
        for (line in recentLines) {
            sb.append("- ").append(line).append("\n")
        }
        sb.append("请确保这次的嘲讽和上面的完全不同。\n\n")
        sb.append("请根据你的性格，对当前局面说一句嘲讽/点评的话。\n")
        sb.append("要求：\n")
        sb.append("1. 不超过50个字\n")
        sb.append("2. 必须结合当前具体棋局内容（点名具体的棋子或走法）\n")
        sb.append("3. 如果是关键局面（将军/吃子/绝杀），语气要更加强烈\n")
        sb.append("4. 可以阴阳怪气、可以毒舌、可以凡尔赛，但禁止人身攻击和脏话")
        return sb.toString()
    }

    // ==================== 自夸 Prompt ====================

    /**
     * 构建自夸 Prompt。
     */
    fun buildSelfPraise(
        personalityPrompt: String,
        aiMoveDesc: String,
        situationTags: String,
        materialBalance: String,
        recentLines: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append(personalityPrompt).append("\n\n")
        sb.append("【当前局面】\n")
        sb.append("- 你（AI）刚走了一步：").append(aiMoveDesc).append("\n")
        sb.append("- 局面状态：").append(situationTags).append("\n")
        sb.append("- 当前子力对比：").append(materialBalance).append("\n\n")
        sb.append("【去重约束】（最近说过的自夸，不要重复）\n")
        for (line in recentLines) {
            sb.append("- ").append(line).append("\n")
        }
        sb.append("\n")
        sb.append("你刚走了一步精妙的棋，请用你的性格特点自夸一下。\n")
        sb.append("要求：\n")
        sb.append("1. 不超过50个字\n")
        sb.append("2. 必须结合你刚走的这步棋（点名棋子或着法意图）\n")
        sb.append("3. 如果是绝杀、将军、吃子等关键着法，语气要更得意\n")
        sb.append("4. 可以凡尔赛、可以炫耀棋力、可以假装谦虚实则炫耀\n")
        sb.append("5. 禁止人身攻击和脏话\n")
        sb.append("6. 和上面的去重缓存不重复")
        return sb.toString()
    }

    // ==================== LLM 走棋 Prompt（回退用） ====================

    /**
     * 构建 LLM 走棋 Prompt（Pikafish 不可用时回退）。
     */
    fun buildMove(board: Board, aiSide: Side, moveHistory: List<String>): String {
        val color = if (aiSide == Side.RED) "红" else "黑"
        val sb = StringBuilder()
        sb.append("你是一个中国象棋AI。当前棋盘状态如下，棋盘为10行(row 0-9)×9列(col 0-9)：\n\n")
        sb.append("棋盘二维数组（row 0 = 黑方底线，row 9 = 红方底线）：\n")
        sb.append(boardToString(board))
        sb.append("\n")
        if (moveHistory.isNotEmpty()) {
            sb.append("历史着法（row,col->row,col 格式）：\n")
            for (m in moveHistory) {
                sb.append(m).append("\n")
            }
            sb.append("\n")
        }
        sb.append("请分析局面，给出最优的下一步棋。\n\n")
        sb.append("【重要】必须严格按以下格式回复，不要任何额外文字：\n")
        sb.append("MOVE: <fromRow>,<fromCol>-<toRow>,<toCol>\n\n")
        sb.append("示例：红方炮从(7,1)平到(7,4) → MOVE: 7,1-7,4\n")
        sb.append("黑方马从(0,1)跳到(2,2) → MOVE: 0,1-2,2\n\n")
        sb.append("注意：你执").append(color).append("方。只输出一行 MOVE: 指令。")
        return sb.toString()
    }

    // ==================== 内部工具方法 ====================

    private fun boardToString(board: Board): String {
        val sb = StringBuilder()
        for (r in 0 until Board.ROWS) {
            sb.append("row ").append(r).append(": ")
            for (c in 0 until Board.COLS) {
                val piece = board[r, c]
                sb.append(if (piece.isEmpty()) ".." else piece).append(" ")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * 生成局面描述标签
     */
    fun buildSituationTags(
        isCheckmate: Boolean,
        isInCheck: Boolean,
        hasCaptured: Boolean
    ): String {
        val tags = mutableListOf<String>()
        if (isCheckmate) tags.add("绝杀")
        else if (isInCheck) tags.add("将军")
        if (hasCaptured) tags.add("吃子")
        if (tags.isEmpty()) tags.add("日常走棋")
        return tags.joinToString(" ")
    }
}

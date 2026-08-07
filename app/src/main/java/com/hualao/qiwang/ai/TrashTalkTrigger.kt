package com.hualao.qiwang.ai

import com.hualao.qiwang.engine.CheckDetector
import com.hualao.qiwang.model.Board
import com.hualao.qiwang.model.Move
import com.hualao.qiwang.model.Side

/**
 * 嘲讽/自夸触发决策引擎。
 *
 * 触发权重参考源项目 GameService.java：
 * - 丢大子（车/炮/马被吃）：×2.0
 * - 将军：×2.0
 * - 绝杀/将杀：×3.0
 * - 妙手：×1.5
 * - 玩家将军 AI：×1.5
 * - 兑子：×1.2
 * - 日常走棋：×1.0
 *
 * 最终触发概率 = min(baseFrequency × 局面权重, 1.0)
 */
class TrashTalkTrigger(
    private val checkDetector: CheckDetector
) {
    companion object {
        /** 自夸基础概率 */
        const val SELF_PRAISE_BASE_PROB = 0.4
    }

    /**
     * 判断是否应该嘲讽玩家
     */
    fun shouldTrashTalk(
        board: Board,
        aiMove: Move,
        baseFrequency: Double
    ): Boolean {
        val weight = getSituationWeight(board, aiMove)
        val prob = (baseFrequency * weight).coerceAtMost(1.0)
        return Math.random() < prob
    }

    /**
     * 判断是否应该自夸
     */
    fun shouldSelfPraise(aiMove: Move): Boolean {
        var weight = 1.0

        if (aiMove.captured != null) {
            val captured = aiMove.captured
            weight = when {
                captured.contains("R") || captured.contains("C") || captured.contains("N") -> 2.0 // 吃大子必定自夸
                else -> 1.5
            }
        }

        val prob = (SELF_PRAISE_BASE_PROB * weight).coerceAtMost(1.0)
        return Math.random() < prob
    }

    /**
     * 根据局面计算嘲讽权重
     */
    private fun getSituationWeight(board: Board, aiMove: Move): Double {
        var weight = 1.0

        // 吃子权重
        if (aiMove.captured != null) {
            val captured = aiMove.captured
            weight *= when {
                captured.contains("R") || captured.contains("C") || captured.contains("N") -> 2.0
                else -> 1.5
            }
        }

        // 将军权重
        val enemy = Side.BLACK.opposite() // 玩家的颜色
        if (checkDetector.isInCheck(board, enemy)) {
            weight *= 2.0
        }

        // 绝杀权重
        if (checkDetector.isCheckmate(board, enemy)) {
            weight *= 3.0
        }

        return weight
    }
}

package com.hualao.qiwang.model

import com.hualao.qiwang.data.PersonalityManager.PersonalityConfig

/**
 * 对局状态 — 封装一局游戏的全部状态。
 *
 * 参考源项目 GameService.java 的内部状态。
 * 所有状态通过 GameViewModel 管理，UI 层通过 StateFlow 观察。
 */
data class GameSession(
    /** 当前棋盘 */
    val board: Board = Board.createInitial(),

    /** 当前轮到哪一方 */
    val currentSide: Side = Side.RED,

    /** 游戏状态 */
    val gameStatus: GameStatus = GameStatus.PLAYING,

    /** 着法历史（ICCS 格式，如 "h2e2"） */
    val moveHistory: List<String> = emptyList(),

    /** 玩家走棋历史（含坐标描述，用于 Prompt 注入） */
    val playerMoveHistory: List<String> = emptyList(),

    /** AI 走棋历史 */
    val aiMoveHistory: List<String> = emptyList(),

    /** 最后一步走棋 */
    val lastMove: Move? = null,

    /** 被吃的棋子列表 */
    val capturedPieces: List<String> = emptyList(),

    /** 嘲讽历史 */
    val trashTalks: List<String> = emptyList(),

    /** 自夸历史 */
    val selfPraises: List<String> = emptyList(),

    /** 当前难度等级 (0-5) */
    val difficulty: Int = 3,

    /** 当前性格配置 */
    val personality: PersonalityConfig? = null,

    /** AI 总回合数 */
    val moveCount: Int = 0,

    /** 总分 */
    val totalMoves: Int = 0
) {

    /**
     * 是否游戏进行中
     */
    val isPlaying: Boolean get() = gameStatus == GameStatus.PLAYING

    /**
     * 是否红方胜利
     */
    val isRedWin: Boolean get() = gameStatus == GameStatus.RED_WIN

    /**
     * 是否黑方胜利
     */
    val isBlackWin: Boolean get() = gameStatus == GameStatus.BLACK_WIN

    /**
     * 是否平局
     */
    val isDraw: Boolean get() = gameStatus == GameStatus.DRAW

    /**
     * 当前是否为 AI 回合
     */
    val isAiTurn: Boolean get() = isPlaying && currentSide == Side.BLACK

    /**
     * 当前是否为玩家回合
     */
    val isPlayerTurn: Boolean get() = isPlaying && currentSide == Side.RED

    /**
     * 用指定配置创建新对局
     */
    fun newGame(
        difficulty: Int = this.difficulty,
        personality: PersonalityConfig? = this.personality
    ): GameSession = copy(
        board = Board.createInitial(),
        currentSide = Side.RED,
        gameStatus = GameStatus.PLAYING,
        moveHistory = emptyList(),
        playerMoveHistory = emptyList(),
        aiMoveHistory = emptyList(),
        lastMove = null,
        capturedPieces = emptyList(),
        trashTalks = emptyList(),
        selfPraises = emptyList(),
        difficulty = difficulty,
        personality = personality,
        moveCount = 0,
        totalMoves = 0
    )
}

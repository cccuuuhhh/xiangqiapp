package com.hualao.qiwang.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hualao.qiwang.ai.*
import com.hualao.qiwang.data.ApiKeyStore
import com.hualao.qiwang.data.PersonalityManager
import com.hualao.qiwang.engine.CheckDetector
import com.hualao.qiwang.engine.FenConverter
import com.hualao.qiwang.engine.MoveGenerator
import com.hualao.qiwang.engine.MoveValidator
import com.hualao.qiwang.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 游戏核心 ViewModel — 管理对局全流程。
 *
 * 核心流程（参考 development-plan.md §2.2）：
 * 玩家走棋 → MoveValidator 校验 → Board.applyMove → CheckDetector 胜负判定
 *   → PikafishEngine.bestMove (IO线程) → Board.applyMove → 胜负判定
 *   → TrashTalkTrigger 决策 → DeepSeekApiClient.chatStream (网络) → UI 逐字显示
 *
 * 参考源项目：GameService.java (298行)
 */
class GameViewModel(context: Context) : ViewModel() {

    // ==================== 依赖注入 ====================

    private val personalityManager = PersonalityManager(context)
    private val apiKeyStore = ApiKeyStore(context)
    private val pikafishEngine = PikafishEngine(context)
    private val moveValidator = MoveValidator()
    private val moveGenerator = MoveGenerator()
    private val checkDetector = CheckDetector()
    private val dedupManager = DedupManager()
    private lateinit var trashTalkTrigger: TrashTalkTrigger

    private var deepSeekClient: DeepSeekApiClient? = null

    // ==================== 状态 ====================

    /** 对局状态 */
    private val _session = MutableStateFlow(GameSession())
    val session: StateFlow<GameSession> = _session.asStateFlow()

    /** 流式嘲讽/自夸文本 — UI 逐字显示 */
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    /** 是否正在流式输出 */
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    /** 流式输出类型（嘲讽/自夸） */
    private val _streamType = MutableStateFlow(StreamType.NONE)
    val streamType: StateFlow<StreamType> = _streamType.asStateFlow()

    /** AI 正在思考 */
    private val _aiThinking = MutableStateFlow(false)
    val aiThinking: StateFlow<Boolean> = _aiThinking.asStateFlow()

    /** 错误信息 */
    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    // ==================== 初始化 ====================

    init {
        // 加载性格配置
        personalityManager.load()

        // 初始化 TrashTalkTrigger
        trashTalkTrigger = TrashTalkTrigger(checkDetector)

        // 尝试获取 API Key 并创建 DeepSeek 客户端
        val key = apiKeyStore.getApiKey()
        if (!key.isNullOrBlank()) {
            deepSeekClient = DeepSeekApiClient(key)
        }

        // 初始化引擎
        viewModelScope.launch(Dispatchers.IO) {
            pikafishEngine.init()
        }
    }

    // ==================== 玩家操作 ====================

    /**
     * 处理玩家走棋。
     *
     * @param from 起始位置
     * @param to   目标位置
     */
    fun playerMove(from: Position, to: Position) {
        val s = _session.value
        if (!s.isPlayerTurn) return

        val piece = s.board[from]
        if (piece.isEmpty()) {
            emitError("请选择您的棋子")
            return
        }
        if (!s.board.isOwnPiece(from.row, from.col, Side.RED)) {
            emitError("不能移动对方的棋子")
            return
        }

        // 校验走法
        if (!moveValidator.isValidMove(s.board, from, to)) {
            emitError("不合法的走法")
            return
        }

        // 执行走棋（先应用再检查是否会让自己被将军）
        val move = Move(from, to, piece, s.board[to].ifEmpty { null })
        val captured = s.board.applyMove(move)

        // 检查走后是否会让己方被将军
        if (checkDetector.isInCheck(s.board, Side.RED)) {
            s.board.undoMove(move, captured)
            emitError("走棋后帅会被将军")
            return
        }

        val moveDesc = FenConverter.describeMove(move)
        val uci = FenConverter.moveToUciString(move)

        // 检查胜负
        val aiStatus = checkGameEnd(move)

        // 更新 session
        _session.value = s.copy(
            board = s.board,
            currentSide = Side.BLACK,
            lastMove = move,
            moveHistory = s.moveHistory + uci,
            playerMoveHistory = s.playerMoveHistory + moveDesc,
            gameStatus = aiStatus
        )

        // 如果游戏结束，跳过 AI 回合
        if (aiStatus != GameStatus.PLAYING) {
            generateTrashTalkForEnd(aiStatus)
            return
        }

        // 触发 AI 走棋
        triggerAiMove()
    }

    /**
     * 悔棋：撤回玩家和 AI 各一步。
     */
    fun undo() {
        val s = _session.value
        if (s.moveHistory.size < 2) {
            emitError("无棋可悔")
            return
        }

        // TODO: 需要存储走棋记录以便悔棋
        // 当前简化实现：仅允许 AI 回合前悔棋
        emitError("悔棋功能开发中")
    }

    /**
     * 认负。
     */
    fun resign() {
        val s = _session.value
        if (!s.isPlaying) return

        _session.value = s.copy(gameStatus = GameStatus.BLACK_WIN)
    }

    /**
     * 开始新局。
     */
    fun newGame() {
        dedupManager.clear()
        _streamingText.value = ""
        _isStreaming.value = false
        _streamType.value = StreamType.NONE

        _session.value = _session.value.newGame()
    }

    // ==================== 设置 ====================

    /**
     * 设置难度等级 (0-5)。
     */
    fun setDifficulty(level: Int) {
        pikafishEngine.setDifficulty(level)
        _session.value = _session.value.copy(difficulty = level)
    }

    /**
     * 切换性格。
     */
    fun switchPersonality(index: Int) {
        val config = personalityManager.switchTo(index)
        _session.value = _session.value.copy(personality = config)
    }

    /**
     * 获取所有性格配置。
     */
    fun getPersonalities() = personalityManager.getAll()

    /**
     * 更新 API Key 并重建客户端。
     */
    fun updateApiKey(key: String) {
        apiKeyStore.saveApiKey(key)
        deepSeekClient = DeepSeekApiClient(key)
    }

    // ==================== AI 走棋 ====================

    private fun triggerAiMove() {
        viewModelScope.launch(Dispatchers.IO) {
            _aiThinking.value = true

            try {
                val s = _session.value
                val aiMove = pikafishEngine.bestMove(s.board, Side.BLACK)

                if (aiMove == null) {
                    // Pikafish 不可用，使用 MoveGenerator 回退
                    val fallbackMoves = moveGenerator.generateMoves(s.board, Side.BLACK)
                    val randomMove = fallbackMoves.randomOrNull()
                    if (randomMove == null) {
                        emitError("AI 无法出棋")
                        _aiThinking.value = false
                        return@launch
                    }
                    applyAiMove(randomMove)
                } else {
                    applyAiMove(aiMove)
                }
            } catch (e: Exception) {
                emitError("AI 引擎异常: ${e.message}")
            } finally {
                _aiThinking.value = false
            }
        }
    }

    private fun applyAiMove(move: Move) {
        val s = _session.value
        val captured = s.board.applyMove(move)
        val moveDesc = FenConverter.describeMove(move)
        val uci = FenConverter.moveToUciString(move)

        // 更新 move 的 captured 信息
        val finalMove = move.copy(captured = captured)

        // 检查胜负
        val gameStatus = checkGameEnd(finalMove)

        // 更新 session
        _session.value = s.copy(
            board = s.board,
            currentSide = Side.RED,
            lastMove = finalMove,
            moveHistory = s.moveHistory + uci,
            aiMoveHistory = s.aiMoveHistory + moveDesc,
            gameStatus = gameStatus,
            moveCount = s.moveCount + 1,
            totalMoves = s.totalMoves + 1
        )

        // 触发嘲讽/自夸
        triggerTrashTalk(finalMove, gameStatus)
    }

    // ==================== 嘲讽/自夸 ====================

    private fun triggerTrashTalk(aiMove: Move, gameStatus: GameStatus) {
        val s = _session.value
        val config = s.personality ?: return

        // 检查 DeepSeek 客户端是否可用
        if (deepSeekClient == null) return

        // 确定说嘲讽还是自夸
        val isCheckmate = gameStatus == GameStatus.BLACK_WIN

        if (isCheckmate || trashTalkTrigger.shouldTrashTalk(
                s.board, aiMove, config.trashTalkFrequency
            )) {
            generateTrashTalk(s, aiMove, isCheckmate)
        } else if (trashTalkTrigger.shouldSelfPraise(aiMove)) {
            generateSelfPraise(s, aiMove, isCheckmate)
        }
    }

    private fun generateTrashTalk(
        s: GameSession,
        aiMove: Move,
        isCheckmate: Boolean
    ) {
        val config = s.personality ?: return
        val client = deepSeekClient ?: return

        val situationTags = PromptBuilder.buildSituationTags(
            isCheckmate = isCheckmate,
            isInCheck = checkDetector.isInCheck(s.board, Side.RED),
            hasCaptured = aiMove.captured != null
        )

        val materialBalance = buildMaterialBalanceString(s.board)

        val prompt = PromptBuilder.buildTrashTalk(
            personalityPrompt = config.systemPrompt,
            lastMoveDesc = s.playerMoveHistory.lastOrNull() ?: "开局",
            aiMoveDesc = FenConverter.describeMove(aiMove),
            situationTags = situationTags,
            materialBalance = materialBalance,
            moveCount = s.moveCount,
            recentLines = dedupManager.getRecentTrashTalks()
        )

        streamAndShow(client, prompt, config.systemPrompt, StreamType.TRASH_TALK) { text ->
            dedupManager.addTrashTalk(text)
            _session.value = _session.value.copy(
                trashTalks = _session.value.trashTalks + text
            )
        }
    }

    private fun generateSelfPraise(
        s: GameSession,
        aiMove: Move,
        isCheckmate: Boolean
    ) {
        val config = s.personality ?: return
        val client = deepSeekClient ?: return

        val situationTags = PromptBuilder.buildSituationTags(
            isCheckmate = isCheckmate,
            isInCheck = checkDetector.isInCheck(s.board, Side.RED),
            hasCaptured = aiMove.captured != null
        )

        val materialBalance = buildMaterialBalanceString(s.board)

        val prompt = PromptBuilder.buildSelfPraise(
            personalityPrompt = config.systemPrompt,
            aiMoveDesc = FenConverter.describeMove(aiMove),
            situationTags = situationTags,
            materialBalance = materialBalance,
            recentLines = dedupManager.getRecentSelfPraises()
        )

        streamAndShow(client, prompt, config.systemPrompt, StreamType.SELF_PRAISE) { text ->
            dedupManager.addSelfPraise(text)
            _session.value = _session.value.copy(
                selfPraises = _session.value.selfPraises + text
            )
        }
    }

    /**
     * 游戏结束时的特殊嘲讽（必定触发）
     */
    private fun generateTrashTalkForEnd(gameStatus: GameStatus) {
        val s = _session.value
        val config = s.personality ?: return
        val client = deepSeekClient ?: return

        val lastMove = s.lastMove ?: return
        val isCheckmate = gameStatus == GameStatus.BLACK_WIN ||
                gameStatus == GameStatus.RED_WIN

        val prompt = PromptBuilder.buildTrashTalk(
            personalityPrompt = config.systemPrompt,
            lastMoveDesc = s.playerMoveHistory.lastOrNull() ?: "终局",
            aiMoveDesc = FenConverter.describeMove(lastMove),
            situationTags = if (gameStatus == GameStatus.BLACK_WIN) "绝杀" else "对局结束",
            materialBalance = buildMaterialBalanceString(s.board),
            moveCount = s.moveCount,
            recentLines = dedupManager.getRecentTrashTalks()
        )

        streamAndShow(client, prompt, config.systemPrompt, StreamType.TRASH_TALK) { text ->
            dedupManager.addTrashTalk(text)
            _session.value = _session.value.copy(
                trashTalks = _session.value.trashTalks + text
            )
        }
    }

    /**
     * 流式调用并逐字推送。
     */
    private fun streamAndShow(
        client: DeepSeekApiClient,
        prompt: String,
        systemPrompt: String,
        type: StreamType,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isStreaming.value = true
            _streamType.value = type
            _streamingText.value = ""

            val fullText = StringBuilder()

            try {
                client.chatStream(
                    prompt = prompt,
                    temperature = DeepSeekApiClient.TRASH_TALK_TEMPERATURE,
                    systemPrompt = systemPrompt
                ).collect { token ->
                    fullText.append(token)
                    _streamingText.value = fullText.toString()
                }

                // 流式完成
                val result = fullText.toString().trim()
                if (result.isNotEmpty()) {
                    // 检查去重（重复则忽略，不加入历史）
                    val isDuplicate = when (type) {
                        StreamType.TRASH_TALK -> dedupManager.isTrashTalkDuplicate(result)
                        StreamType.SELF_PRAISE -> dedupManager.isSelfPraiseDuplicate(result)
                        StreamType.NONE -> false
                    }

                    if (!isDuplicate) {
                        onComplete(result)
                    }
                }
            } catch (e: Exception) {
                // 网络错误静默降级，引擎本地运行不受影响
            } finally {
                _isStreaming.value = false
                _streamType.value = StreamType.NONE
            }
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 检查走棋后游戏是否结束。
     */
    private fun checkGameEnd(move: Move): GameStatus {
        val s = _session.value
        val enemy = if (move.piece.startsWith("r")) Side.BLACK else Side.RED

        return when {
            checkDetector.isCheckmate(s.board, enemy) -> {
                if (enemy == Side.BLACK) GameStatus.RED_WIN
                else GameStatus.BLACK_WIN
            }
            checkDetector.isStalemate(s.board, enemy) -> GameStatus.DRAW
            else -> GameStatus.PLAYING
        }
    }

    /**
     * 构建子力对比字符串（中文描述）。
     */
    private fun buildMaterialBalanceString(board: Board): String {
        val redPieces = mutableListOf<String>()
        val blackPieces = mutableListOf<String>()

        for (r in 0 until Board.ROWS) {
            for (c in 0 until Board.COLS) {
                val piece = board[r, c]
                if (piece.isNotEmpty()) {
                    val name = FenConverter.pieceToChinese(piece)
                    if (piece.startsWith("r")) redPieces.add(name)
                    else blackPieces.add(name)
                }
            }
        }

        val redCount = redPieces.size
        val blackCount = blackPieces.size

        return when {
            redCount > blackCount -> "玩家（红方）${redCount}子 vs AI（黑方）${blackCount}子，红方多${redCount - blackCount}子"
            redCount < blackCount -> "玩家（红方）${redCount}子 vs AI（黑方）${blackCount}子，黑方多${blackCount - redCount}子"
            else -> "双方均剩${redCount}子，子力均衡"
        }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _error.emit(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pikafishEngine.destroy()
    }
}

/**
 * 流式输出类型。
 */
enum class StreamType {
    NONE, TRASH_TALK, SELF_PRAISE
}

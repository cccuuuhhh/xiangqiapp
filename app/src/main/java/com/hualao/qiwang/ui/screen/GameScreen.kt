package com.hualao.qiwang.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hualao.qiwang.model.GameStatus
import com.hualao.qiwang.model.Position
import com.hualao.qiwang.model.Side
import com.hualao.qiwang.ui.component.*
import com.hualao.qiwang.viewmodel.GameViewModel
import com.hualao.qiwang.viewmodel.StreamType

/**
 * 游戏主界面 — 组合所有 UI 组件。
 *
 * 布局（竖屏）：
 * ```
 * ┌─────────────────────┐
 * │  PersonalitySelector │ ← 性格选择横排
 * ├─────────────────────┤
 * │                     │
 * │  ChessBoardCanvas   │ ← 中央棋盘
 * │                     │
 * ├─────────────────────┤
 * │   ControlPanel      │ ← 新局/悔棋/认负/难度
 * ├─────────────────────┤
 * │   TrashTalkPanel    │ ← 嘲讽面板（可展开）
 * │   (可切换到着法历史)  │
 * └─────────────────────┘
 * ```
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.session.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamType by viewModel.streamType.collectAsState()
    val aiThinking by viewModel.aiThinking.collectAsState()

    // 选中的棋子位置
    var selectedPiece by remember { mutableStateOf<Position?>(null) }

    // 将军状态
    var kingInCheck by remember { mutableStateOf(false) }

    // 副面板切换：嘲讽 vs 着法历史
    var showMoveHistory by remember { mutableStateOf(false) }

    // 监听错误
    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            // 错误暂时由 ControlPanel / TrashTalkPanel 内的状态处理
            // 复杂场景可加入 Snackbar
        }
    }

    // 监听 session 变化，更新将军状态
    LaunchedEffect(session.board, session.lastMove) {
        if (session.lastMove != null && session.currentSide == Side.RED) {
            kingInCheck = try {
                com.hualao.qiwang.engine.CheckDetector()
                    .isInCheck(session.board, Side.RED)
            } catch (e: Exception) {
                false
            }
        } else {
            kingInCheck = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ========== 性格选择器 ==========
            PersonalitySelector(
                personalities = viewModel.getPersonalities(),
                selectedIndex = session.personality?.let { p ->
                    viewModel.getPersonalities().indexOfFirst { it.id == p.id }
                } ?: 0,
                onSelect = { viewModel.switchPersonality(it) }
            )

            // ========== 棋盘区域 ==========
            ChessBoardCanvas(
                board = session.board,
                currentSide = session.currentSide,
                isPlaying = session.isPlaying,
                lastMove = session.lastMove,
                selectedPiece = selectedPiece,
                kingInCheck = kingInCheck,
                onSquareTapped = { pos ->
                    handleSquareTap(
                        pos = pos,
                        board = session.board,
                        isPlaying = session.isPlaying,
                        currentSide = session.currentSide,
                        selectedPiece = selectedPiece,
                        onPieceSelected = { selectedPiece = it },
                        onMoveExecuted = { from, to ->
                            selectedPiece = null
                            viewModel.playerMove(from, to)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(4.dp))

            // ========== 控制面板 ==========
            ControlPanel(
                difficulty = session.difficulty,
                gameStatus = session.gameStatus,
                canUndo = session.moveHistory.size >= 2 && session.isPlaying,
                onNewGame = {
                    selectedPiece = null
                    viewModel.newGame()
                },
                onUndo = {
                    selectedPiece = null
                    viewModel.undo()
                },
                onResign = { viewModel.resign() },
                onDifficultyChange = { viewModel.setDifficulty(it) }
            )

            Spacer(Modifier.height(4.dp))

            // ========== 底部面板切换标签 ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = !showMoveHistory,
                    onClick = { showMoveHistory = false },
                    label = { Text("对话") }
                )
                FilterChip(
                    selected = showMoveHistory,
                    onClick = { showMoveHistory = true },
                    label = { Text("着法") }
                )
            }

            // ========== 底部面板 ==========
            if (showMoveHistory) {
                MoveHistoryPanel(
                    playerMoves = session.playerMoveHistory,
                    aiMoves = session.aiMoveHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                TrashTalkPanel(
                    trashTalks = session.trashTalks,
                    selfPraises = session.selfPraises,
                    streamingText = streamingText,
                    isStreaming = isStreaming,
                    streamType = streamType,
                    personality = session.personality,
                    aiThinking = aiThinking,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 处理棋盘触摸事件的核心逻辑。
 *
 * 规则：
 * 1. 当前是玩家回合 → 点击己方棋子：选中 + 显示合法走法
 * 2. 已选中棋子 + 点击合法位置 → 执行走棋
 * 3. 点击对方棋子 / 空格 → 取消选中或切换选中
 */
private fun handleSquareTap(
    pos: Position,
    board: com.hualao.qiwang.model.Board,
    isPlaying: Boolean,
    currentSide: Side,
    selectedPiece: Position?,
    onPieceSelected: (Position?) -> Unit,
    onMoveExecuted: (Position, Position) -> Unit
) {
    if (!isPlaying) {
        onPieceSelected(null)
        return
    }

    if (currentSide != Side.RED) {
        // 不是玩家回合
        onPieceSelected(null)
        return
    }

    val clickedPiece = board[pos]

    if (selectedPiece == null) {
        // 无选中状态 → 尝试选中己方棋子
        if (clickedPiece.isNotEmpty() && board.isOwnPiece(pos.row, pos.col, Side.RED)) {
            onPieceSelected(pos)
        }
    } else {
        // 已有选中状态
        if (clickedPiece.isNotEmpty() && board.isOwnPiece(pos.row, pos.col, Side.RED)) {
            // 点击了另一个己方棋子 → 切换选中
            onPieceSelected(pos)
        } else if (pos == selectedPiece) {
            // 再次点击同一位置 → 取消选中
            onPieceSelected(null)
        } else {
            // 尝试走棋（由 ViewModel 校验）
            onMoveExecuted(selectedPiece, pos)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    com.hualao.qiwang.ui.theme.XiangqiTheme {
        // 预览用静态占位
        Box(modifier = Modifier.fillMaxSize()) {
            Text("GameScreen — 需要 ViewModel 运行时")
        }
    }
}

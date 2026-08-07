package com.hualao.qiwang.ai

import android.content.Context
import com.hualao.qiwang.engine.FenConverter
import com.hualao.qiwang.model.Board
import com.hualao.qiwang.model.Move
import com.hualao.qiwang.model.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Pikafish 中国象棋引擎封装 — 进程内 JNI 模式。
 *
 * 引擎通过 pikafish_wrapper（线程 + 管道）运行在应用进程内，
 * 不再使用 fork/exec（Android 不支持）。
 *
 * UCI 协议通信流程（由 Native Wrapper 自动完成）：
 * 1. pikafish_init() → uci → uciok → setoption → isready → readyok
 * 2. position fen <fen>
 * 3. go depth <n> → bestmove <move>
 *
 * 引擎必须在 IO 线程（Dispatchers.IO）运行。
 * 参考源项目：PikafishEngine.java (346行)
 */
class PikafishEngine(private val context: Context) {

    companion object {
        private val BESTMOVE_PATTERN = Pattern.compile("bestmove\\s+([a-i]\\d)([a-i]\\d)")

        /** 难度映射：等级 → (Skill Level, Depth) */
        data class DifficultyConfig(val skillLevel: Int, val depth: Int)

        val DIFFICULTY_MAP = mapOf(
            0 to DifficultyConfig(1, 4),    // 入门
            1 to DifficultyConfig(3, 6),    // 初级
            2 to DifficultyConfig(6, 8),    // 中级
            3 to DifficultyConfig(10, 12),  // 高级
            4 to DifficultyConfig(15, 15),  // 大师
            5 to DifficultyConfig(20, 20)   // 特级大师
        )

        private const val MAX_DIFFICULTY = 5
    }

    /** 引擎是否就绪 */
    @Volatile
    var isReady: Boolean = false
        private set

    /** 当前搜索深度 */
    private var searchDepth: Int = 10

    /** CPU 线程数 */
    private var threads: Int = 2

    /** 哈希表大小 (MB) */
    private var hash: Int = 128

    /** 当前难度等级 */
    private var currentDifficulty: Int = 3

    // ==================== 生命周期 ====================

    /**
     * 初始化引擎。
     *
     * Native Wrapper 自动完成：
     * - 创建管道 + 启动引擎线程
     * - UCI 握手（uci → uciok）
     * - 设置 Threads/Hash/EvalFile
     * - isready → readyok
     *
     * 必须在 IO 线程调用。
     */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nnuePath = NnueManager.getNnuePath(context)

            // Native Wrapper 完成全部初始化（UCI 握手、选项配置、就绪确认）
            val success = nativeInit(nnuePath)
            isReady = success
            success
        } catch (e: Exception) {
            isReady = false
            false
        }
    }

    /**
     * 获取最佳着法。
     * 必须在 IO 线程调用。
     *
     * @param board 当前棋盘
     * @param side  引擎执子方
     * @return 最佳着法；null 表示引擎不可用或计算失败
     */
    suspend fun bestMove(board: Board, side: Side): Move? = withContext(Dispatchers.IO) {
        if (!isReady) return@withContext null

        try {
            // 1. 生成 FEN 并设置局面
            val fen = FenConverter.boardToFen(board, side)
            sendCommand("position fen $fen")

            // 2. 开始搜索
            sendCommand("go depth $searchDepth")

            // 3. 读取响应，等待 bestmove
            var bestmoveStr: String? = null
            while (true) {
                val line = nativeReadLine()
                if (line.isEmpty()) break
                if (line.startsWith("bestmove")) {
                    bestmoveStr = line
                    break
                }
            }

            if (bestmoveStr == null) return@withContext null

            // 4. 解析 bestmove（格式：bestmove h2e2 [ponder b9c7]）
            val m = BESTMOVE_PATTERN.matcher(bestmoveStr)
            if (!m.find()) return@withContext null

            val fromIccs = m.group(1)
            val toIccs = m.group(2)

            val from = FenConverter.iccsToPosition(fromIccs)
            val to = FenConverter.iccsToPosition(toIccs)

            val piece = board[from]
            if (piece.isEmpty()) return@withContext null
            val captured = board[to].ifEmpty { null }

            Move(from, to, piece, captured)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置难度 (0-5)
     */
    fun setDifficulty(level: Int) {
        val clamped = level.coerceIn(0, MAX_DIFFICULTY)
        currentDifficulty = clamped
        DIFFICULTY_MAP[clamped]?.let {
            searchDepth = it.depth
            sendCommand("setoption name Skill Level value ${it.skillLevel}")
        }
    }

    /**
     * 销毁引擎（发送 quit，等待线程退出，清理资源）
     */
    fun destroy() {
        isReady = false
        nativeDestroy()
    }

    // ==================== 内部方法 ====================

    /**
     * 向引擎发送 UCI 命令
     */
    private fun sendCommand(command: String) {
        nativeSend(command)
    }

    // ==================== JNI Native Methods ====================

    /**
     * 初始化引擎（Native Wrapper 自动完成 UCI 握手）
     * @param nnuePath pikafish.nnue 权重文件路径（传空字符串使用嵌入式网络）
     */
    private external fun nativeInit(nnuePath: String): Boolean
    private external fun nativeSend(command: String)
    private external fun nativeReadLine(): String
    private external fun nativeDestroy()

    init {
        try {
            System.loadLibrary("pikafish_bridge")
        } catch (e: UnsatisfiedLinkError) {
            // 引擎 .so 尚未编译（首次构建前）
        }
    }
}

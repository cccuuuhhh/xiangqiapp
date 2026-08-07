package com.hualao.qiwang.ai

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * NNUE 神经网络评估文件管理。
 *
 * - pikafish.nnue 打包在 APK assets/ 中
 * - 首次启动时从 assets 复制到 internal storage
 * - 引擎初始化时指定 NNUE 文件路径
 */
object NnueManager {

    private const val NNUE_FILENAME = "pikafish.nnue"

    /**
     * 获取 NNUE 文件路径。
     * 如果文件不在 internal storage，从 assets 复制。
     */
    fun getNnuePath(context: Context): String {
        val targetFile = File(context.filesDir, NNUE_FILENAME)

        if (!targetFile.exists()) {
            copyFromAssets(context, targetFile)
        }

        return targetFile.absolutePath
    }

    /**
     * 从 assets 复制 NNUE 文件到 internal storage
     */
    private fun copyFromAssets(context: Context, target: File) {
        try {
            context.assets.open(NNUE_FILENAME).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            // NNUE 文件可能不存在（首次编译引擎时）
        }
    }

    /**
     * 获取 NNUE 文件大小（用于日志/调试）
     */
    fun getNnueFileSize(context: Context): Long {
        val file = File(context.filesDir, NNUE_FILENAME)
        return if (file.exists()) file.length() else -1
    }
}

package com.hualao.qiwang.ai

/**
 * 嘲讽/自夸去重缓存管理。
 *
 * 参考 development-plan.md §6.4：
 * - 维护最近 20 条已用嘲讽/自夸的原文
 * - 每次生成时将已用记录注入 Prompt
 * - 如生成内容与已用记录相似度过高（编辑距离相似度 > 70%），重新生成一次
 * - 嘲讽和自夸各自独立去重缓存
 */
class DedupManager {

    companion object {
        /** 最大缓存条目数 */
        const val MAX_ENTRIES = 20

        /** 相似度阈值：编辑距离归一化后，相似度超过此值视为重复 */
        const val SIMILARITY_THRESHOLD = 0.7
    }

    /** 嘲讽去重缓存（最近 20 条） */
    private val trashTalkCache = ArrayDeque<String>(MAX_ENTRIES)

    /** 自夸去重缓存（最近 20 条） */
    private val selfPraiseCache = ArrayDeque<String>(MAX_ENTRIES)

    // ==================== 添加 ====================

    /**
     * 添加一条嘲讽到去重缓存
     */
    fun addTrashTalk(line: String) {
        addToCache(trashTalkCache, line)
    }

    /**
     * 添加一条自夸到去重缓存
     */
    fun addSelfPraise(line: String) {
        addToCache(selfPraiseCache, line)
    }

    // ==================== 查询 ====================

    /**
     * 获取最近的嘲讽文本列表（用于注入 Prompt）
     */
    fun getRecentTrashTalks(): List<String> = trashTalkCache.toList()

    /**
     * 获取最近的自夸文本列表（用于注入 Prompt）
     */
    fun getRecentSelfPraises(): List<String> = selfPraiseCache.toList()

    // ==================== 相似度检测 ====================

    /**
     * 检查生成的嘲讽是否与缓存中已有内容过于相似。
     *
     * @return true 表示过于重复，需要重新生成
     */
    fun isTrashTalkDuplicate(text: String): Boolean {
        return isDuplicate(trashTalkCache, text)
    }

    /**
     * 检查生成的自夸是否与缓存中已有内容过于相似。
     */
    fun isSelfPraiseDuplicate(text: String): Boolean {
        return isDuplicate(selfPraiseCache, text)
    }

    // ==================== 清空 ====================

    /**
     * 清空所有缓存（新开局时调用）
     */
    fun clear() {
        trashTalkCache.clear()
        selfPraiseCache.clear()
    }

    // ==================== 内部实现 ====================

    private fun addToCache(cache: ArrayDeque<String>, item: String) {
        if (cache.size >= MAX_ENTRIES) {
            cache.removeFirst()
        }
        cache.addLast(item)
    }

    private fun isDuplicate(cache: ArrayDeque<String>, text: String): Boolean {
        return cache.any { cached ->
            similarity(cached, text) > SIMILARITY_THRESHOLD
        }
    }

    /**
     * 计算两个字符串的归一化相似度 [0, 1]。
     * 使用编辑距离（Levenshtein Distance）归一化。
     */
    private fun similarity(s1: String, s2: String): Double {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * 计算 Levenshtein 编辑距离。
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // 用两行滚动数组优化空间
        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,        // 删除
                    curr[j - 1] + 1,    // 插入
                    prev[j - 1] + cost  // 替换
                )
            }
            // 交换引用
            val tmp = prev
            prev = curr
            curr = tmp
        }

        return prev[len2]
    }
}

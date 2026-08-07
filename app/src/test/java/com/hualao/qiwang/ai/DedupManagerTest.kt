package com.hualao.qiwang.ai

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * DedupManager 单元测试 — 编辑距离去重、缓存管理。
 */
class DedupManagerTest {

    private lateinit var manager: DedupManager

    @Before
    fun setUp() {
        manager = DedupManager()
    }

    // ==================== add + get ====================

    @Test
    fun `addTrashTalk should store and retrieve entries`() {
        manager.addTrashTalk("你这步棋太臭了！")
        manager.addTrashTalk("就这水平也敢来下棋？")
        assertEquals(2, manager.getRecentTrashTalks().size)
        assertEquals("你这步棋太臭了！", manager.getRecentTrashTalks()[0])
    }

    @Test
    fun `addSelfPraise should store and retrieve entries`() {
        manager.addSelfPraise("我的棋艺天下无敌！")
        assertEquals(1, manager.getRecentSelfPraises().size)
    }

    @Test
    fun `trash talk and self praise caches should be independent`() {
        manager.addTrashTalk("嘲讽1")
        manager.addSelfPraise("自夸1")
        assertEquals(1, manager.getRecentTrashTalks().size)
        assertEquals(1, manager.getRecentSelfPraises().size)
    }

    // ==================== MAX_ENTRIES ====================

    @Test
    fun `should keep at most MAX_ENTRIES items`() {
        for (i in 1..25) {
            manager.addTrashTalk("嘲讽第${i}句")
        }
        assertEquals(DedupManager.MAX_ENTRIES, manager.getRecentTrashTalks().size)
        // Should have the latest 20 (6-25)
        assertEquals("嘲讽第6句", manager.getRecentTrashTalks().first())
        assertEquals("嘲讽第25句", manager.getRecentTrashTalks().last())
    }

    // ==================== similarity / duplicate detection ====================

    @Test
    fun `identical strings should be detected as duplicate`() {
        manager.addTrashTalk("你这步走得真差")
        assertTrue(manager.isTrashTalkDuplicate("你这步走得真差"))
    }

    @Test
    fun `very similar strings should be detected as duplicate`() {
        manager.addTrashTalk("你这步棋走得真是太差了")
        // Very similar — changing one character
        assertTrue(manager.isTrashTalkDuplicate("你这步棋走的真是太差了"))
    }

    @Test
    fun `completely different strings should not be duplicate`() {
        manager.addTrashTalk("你这步走得真差")
        assertFalse(manager.isTrashTalkDuplicate("今天天气真不错啊"))
    }

    @Test
    fun `empty cache should not flag anything as duplicate`() {
        assertFalse(manager.isTrashTalkDuplicate("任意文本"))
        assertFalse(manager.isSelfPraiseDuplicate("任意文本"))
    }

    @Test
    fun `short strings with small difference should be below threshold`() {
        manager.addTrashTalk("差")
        // "差" vs "好" — edit distance = 1, maxLen = 1, similarity = 0.0
        // Actually: distance=1, 1-1/1=0.0 < 0.7 threshold, so NOT duplicate
        assertFalse(manager.isTrashTalkDuplicate("好"))
    }

    // ==================== clear ====================

    @Test
    fun `clear should empty both caches`() {
        manager.addTrashTalk("嘲讽")
        manager.addSelfPraise("自夸")
        manager.clear()
        assertEquals(0, manager.getRecentTrashTalks().size)
        assertEquals(0, manager.getRecentSelfPraises().size)
    }

    // ==================== Similarity edge cases ====================

    @Test
    fun `isTrashTalkDuplicate should work with mixed caches`() {
        for (i in 1..10) {
            manager.addTrashTalk("嘲讽$i")
            manager.addSelfPraise("自夸$i")
        }
        assertTrue(manager.isTrashTalkDuplicate("嘲讽5"))
        assertFalse(manager.isTrashTalkDuplicate("完全不同的新内容"))
        assertTrue(manager.isSelfPraiseDuplicate("自夸8"))
    }
}

package com.hualao.qiwang.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 性格配置管理 — 加载和管理 5 种对话性格。
 *
 * 参考源项目：PersonalityService.java + personalities.yaml
 */
class PersonalityManager(private val context: Context) {

    companion object {
        private const val CONFIG_FILE = "personalities.json"
        private const val PREF_CURRENT_PERSONALITY = "current_personality"
    }

    data class PersonalityConfig(
        val id: String,
        val name: String,
        val avatar: String,
        val description: String,
        val systemPrompt: String,
        val trashTalkFrequency: Double,
        val speakingStyle: String,
        val exampleLines: List<String>
    )

    private var personalities: List<PersonalityConfig> = emptyList()
    private var currentIndex: Int = 0

    /**
     * 加载性格配置
     */
    fun load(): List<PersonalityConfig> {
        val json = try {
            context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // 降级：使用内置硬编码配置
            return defaultPersonalities().also {
                personalities = it
            }
        }
        val type = object : TypeToken<List<PersonalityConfig>>() {}.type
        personalities = Gson().fromJson(json, type)
        return personalities
    }

    /**
     * 获取当前性格
     */
    fun getCurrent(): PersonalityConfig {
        if (personalities.isEmpty()) load()
        return personalities[currentIndex]
    }

    /**
     * 切换性格
     */
    fun switchTo(index: Int): PersonalityConfig {
        currentIndex = index.coerceIn(0, personalities.size - 1)
        return getCurrent()
    }

    /**
     * 获取所有性格
     */
    fun getAll(): List<PersonalityConfig> = personalities.ifEmpty { load() }

    /**
     * 获取当前性格索引
     */
    fun getCurrentIndex(): Int = currentIndex

    /**
     * 内置默认性格配置（降级用）
     */
    private fun defaultPersonalities(): List<PersonalityConfig> = listOf(
        PersonalityConfig(
            id = "toxic-master",
            name = "毒舌大师",
            avatar = "😈",
            description = "嘴比刀子还狠，你每步棋都能挑出毛病",
            systemPrompt = "你是一个棋艺高超但嘴巴极毒的中国象棋高手。你说话尖酸刻薄，喜欢反问，每句嘲讽都直击痛点。你的棋力碾压对手，但嘴上也不饶人。",
            trashTalkFrequency = 0.8,
            speakingStyle = "尖酸刻薄，喜欢反问",
            exampleLines = listOf("就这水平还敢跟我下？", "你这步棋是闭着眼下的吧？")
        ),
        PersonalityConfig(
            id = "elegant-gentleman",
            name = "优雅绅士",
            avatar = "🎩",
            description = "表面彬彬有礼，实则阴阳怪气大师",
            systemPrompt = "你是一位举止优雅但喜欢阴阳怪气的中国象棋对手。你表面客气，话里藏刀。用最礼貌的措辞说出最扎心的话。",
            trashTalkFrequency = 0.5,
            speakingStyle = "表面客气，实则阴阳怪气",
            exampleLines = listOf("有趣的走法……如果是初学者的话。", "您的勇气远远超过了您的棋艺。")
        ),
        PersonalityConfig(
            id = "chuunibyou",
            name = "中二少年",
            avatar = "⚔️",
            description = "每下一步棋都要喊出招式名",
            systemPrompt = "你是一个沉醉于自己世界中二病晚期的中国象棋少年。你给每步棋起炫酷的招式名，用中二的语言评价棋局。",
            trashTalkFrequency = 0.9,
            speakingStyle = "中二、热血、喜欢给棋步起炫酷的名字",
            exampleLines = listOf(
                "见识一下我的终极奥义——「暗黑車輪斬」！",
                "凡人，你的棋力连我封印前的十分之一都不如。"
            )
        ),
        PersonalityConfig(
            id = "zen-master",
            name = "禅意大师",
            avatar = "🧘",
            description = "下棋五分钟，讲哲理两小时",
            systemPrompt = "你是一位看破红尘的禅意象棋大师。你喜欢用哲学道理来点评棋局，经常引用不存在的名言。说话慢悠悠，但每句话都让人陷入沉思。",
            trashTalkFrequency = 0.4,
            speakingStyle = "禅意、说教、喜欢引用不存在的名言",
            exampleLines = listOf(
                "车行直线，人生却从无直路可走。",
                "棋盘如人生，你的每一步都在暴露你的焦虑。"
            )
        ),
        PersonalityConfig(
            id = "silent-killer",
            name = "沉默杀手",
            avatar = "🗿",
            description = "几乎不说话，但开口就是暴击",
            systemPrompt = "你是一个沉默寡言但棋力极强的象棋AI。你极少说话，但一旦开口就是致命一击。每句话不超过10个字，精准打击对手心理。",
            trashTalkFrequency = 0.15,
            speakingStyle = "极度简洁，不超过10个字，一击必杀",
            exampleLines = listOf("你输了。", "三步之内。", "还要继续吗？")
        )
    )
}

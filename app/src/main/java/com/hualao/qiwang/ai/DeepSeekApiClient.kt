package com.hualao.qiwang.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API 客户端 — 通过 OkHttp 直连 DeepSeek API。
 *
 * 两种调用模式：
 * - chat(): 普通同步调用（LLM 回退走棋用）
 * - chatStream(): 流式调用（嘲讽/自夸用）— 返回 Flow<String> 实现真打字机效果
 *
 * 温度常量（代码硬编码，不可通过配置修改）：
 * - MOVE_TEMPERATURE = 0.15（棋步 AI）
 * - TRASH_TALK_TEMPERATURE = 0.85（嘲讽/自夸）
 *
 * 参考源项目：AIService.java (382行)
 */
class DeepSeekApiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com"
) {

    companion object {
        const val MOVE_TEMPERATURE = 0.15
        const val TRASH_TALK_TEMPERATURE = 0.85
        const val MOVE_MAX_TOKENS = 100
        const val TRASH_TALK_MAX_TOKENS = 300

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * 普通调用 — 返回完整响应文本。
     * 用于 LLM 回退走棋。
     */
    suspend fun chat(
        prompt: String,
        temperature: Double = MOVE_TEMPERATURE,
        maxTokens: Int = MOVE_MAX_TOKENS,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(prompt, temperature, maxTokens, systemPrompt, stream = false)

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DeepSeek API error: ${response.code} ${response.message}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body")
            parseCompletionResponse(body) ?: ""
        }
    }

    /**
     * 流式调用 — 返回 Flow<String>，逐 token 推送。
     * 用于嘲讽/自夸生成（真打字机效果）。
     */
    suspend fun chatStream(
        prompt: String,
        temperature: Double = TRASH_TALK_TEMPERATURE,
        maxTokens: Int = TRASH_TALK_MAX_TOKENS,
        systemPrompt: String? = null
    ): Flow<String> = callbackFlow {
        val requestBody = buildRequestBody(prompt, temperature, maxTokens, systemPrompt, stream = true)

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    close(IOException("DeepSeek API error: ${response.code}"))
                    return
                }

                val reader = response.body?.byteStream()?.bufferedReader()
                    ?: run { close(IOException("Empty stream body")); return }

                try {
                    reader.useLines { lines ->
                        for (line in lines) {
                            if (line.isEmpty()) continue
                            if (!line.startsWith("data: ")) continue
                            val data = line.removePrefix("data: ")
                            if (data == "[DONE]") {
                                close()
                                return@useLines
                            }
                            val token = parseStreamToken(data)
                            if (token != null) {
                                trySend(token)
                            }
                        }
                    }
                    close()
                } catch (e: Exception) {
                    close(e)
                }
            }
        })

        awaitClose()
    }

    /**
     * 验证 API Key 是否有效（轻量测试调用）
     */
    suspend fun validateApiKey(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 1)
            }

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .post(testBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    // ==================== 内部方法 ====================

    private fun buildRequestBody(
        prompt: String,
        temperature: Double,
        maxTokens: Int,
        systemPrompt: String?,
        stream: Boolean
    ): String {
        val messages = JSONArray().apply {
            if (systemPrompt != null) {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        return JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", messages)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("stream", stream)
        }.toString()
    }

    /**
     * 解析普通调用的响应
     */
    private fun parseCompletionResponse(json: String): String? {
        return try {
            val obj = JSONObject(json)
            val choices = obj.getJSONArray("choices")
            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                message.optString("content", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析流式调用的单个 token
     */
    private fun parseStreamToken(data: String): String? {
        return try {
            val obj = JSONObject(data)
            val choices = obj.getJSONArray("choices")
            if (choices.length() > 0) {
                val delta = choices.getJSONObject(0).getJSONObject("delta")
                delta.optString("content", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

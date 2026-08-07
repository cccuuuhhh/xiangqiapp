package com.hualao.qiwang.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * API Key 加密存储。
 *
 * 安全方案：
 * - 存储方式：AndroidX Security EncryptedSharedPreferences
 * - 加密算法：AES-256-GCM（值）+ AES-256-SIV（键）
 * - 密钥管理：Android Keystore 系统级密钥库（硬件隔离）
 * - 密钥绑定：绑定到设备，不可导出
 *
 * 安全红线（agent.md §5.4）：
 * - APK 内不含任何 API Key
 * - 存储必须用 EncryptedSharedPreferences
 * - Key 失效不崩溃
 */
class ApiKeyStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_DEEPSEEK_API = "deepseek_api_key"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 保存 API Key（加密存储）
     */
    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_DEEPSEEK_API, key).apply()
    }

    /**
     * 获取 API Key（从加密存储读取）
     */
    fun getApiKey(): String? {
        return prefs.getString(KEY_DEEPSEEK_API, null)
    }

    /**
     * 清除 API Key
     */
    fun clearApiKey() {
        prefs.edit().remove(KEY_DEEPSEEK_API).apply()
    }

    /**
     * 是否有已存储的 API Key
     */
    fun hasApiKey(): Boolean {
        return getApiKey() != null
    }

    /**
     * 获取掩码处理后的 API Key（用于显示，如 sk-xxxx...xxxx）
     */
    fun getMaskedApiKey(): String? {
        val key = getApiKey() ?: return null
        if (key.length <= 8) return "****"
        return key.substring(0, 4) + "..." + key.substring(key.length - 4)
    }
}

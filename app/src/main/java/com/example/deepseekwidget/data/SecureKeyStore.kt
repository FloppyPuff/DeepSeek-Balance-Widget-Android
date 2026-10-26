package com.example.deepseekwidget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 基于 Android KeyStore (AES256-GCM) 的加密凭证存储库
 * 坚决贯彻 Zero-Plaintext Policy：密钥严禁明文落盘，避免进入 DataStore / Preferences 文件
 */
object SecureKeyStore {
    private const val PREFS_NAME = "deepseek_secure_prefs"
    private const val KEY_API_KEY = "deepseek_api_key"
    private const val KEY_LOW_BALANCE_THRESHOLD = "low_balance_threshold"
    private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
    private const val KEY_NOTIFY_LOW_BALANCE = "notify_low_balance"
    private const val KEY_THEME_MODE = "theme_mode"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return cachedPrefs ?: synchronized(this) {
            cachedPrefs ?: run {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also { cachedPrefs = it }
            }
        }
    }

    fun saveApiKey(context: Context, apiKey: String) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun getApiKey(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun clearApiKey(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_API_KEY)
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        return !getApiKey(context).isNullOrBlank()
    }

    fun getLowBalanceThreshold(context: Context): Float {
        return getEncryptedPrefs(context).getFloat(KEY_LOW_BALANCE_THRESHOLD, 5.0f)
    }

    fun setLowBalanceThreshold(context: Context, threshold: Float) {
        getEncryptedPrefs(context).edit()
            .putFloat(KEY_LOW_BALANCE_THRESHOLD, threshold)
            .apply()
    }

    fun getSyncIntervalMinutes(context: Context): Long {
        return getEncryptedPrefs(context).getLong(KEY_SYNC_INTERVAL_MINUTES, 60L)
    }

    fun setSyncIntervalMinutes(context: Context, minutes: Long) {
        getEncryptedPrefs(context).edit()
            .putLong(KEY_SYNC_INTERVAL_MINUTES, minutes)
            .apply()
    }

    fun isLowBalanceNotificationEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_NOTIFY_LOW_BALANCE, true)
    }

    fun setLowBalanceNotificationEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context).edit()
            .putBoolean(KEY_NOTIFY_LOW_BALANCE, enabled)
            .apply()
    }

    fun getThemeMode(context: Context): ThemeMode {
        val name = getEncryptedPrefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun getMaskedApiKey(context: Context): String? {
        val key = getApiKey(context) ?: return null
        return if (key.length <= 8) {
            "••••••••"
        } else {
            val prefix = if (key.startsWith("sk-")) "sk-" else key.take(3)
            val suffix = key.takeLast(4)
            "${prefix}••••••••••••${suffix}"
        }
    }
}

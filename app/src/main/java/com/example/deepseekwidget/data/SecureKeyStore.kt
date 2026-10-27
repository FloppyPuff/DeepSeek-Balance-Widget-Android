package com.example.deepseekwidget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import android.os.Build

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
    private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
    private const val KEY_PRIVACY_MODE = "privacy_mode"
    private const val KEY_LAST_NOTIFIED_LOW_BALANCE_TIME = "last_notified_low_balance_time"
    private const val KEY_LAST_NOTIFIED_LOW_BALANCE_AMOUNT = "last_notified_low_balance_amount"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    @Volatile
    private var cachedApiKey: String? = null
    @Volatile
    private var cachedApiKeyLoaded = false

    @Volatile
    private var cachedThreshold: Float? = null

    @Volatile
    private var cachedSyncInterval: Long? = null

    @Volatile
    private var cachedNotifyLowBalance: Boolean? = null

    @Volatile
    private var cachedThemeMode: ThemeMode? = null

    @Volatile
    private var cachedAutoCheckUpdate: Boolean? = null

    @Volatile
    private var cachedPrivacyMode: Boolean? = null

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return cachedPrefs ?: synchronized(this) {
            cachedPrefs ?: createEncryptedPrefsWithFallback(context).also { cachedPrefs = it }
        }
    }

    private fun createEncryptedPrefsWithFallback(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        return try {
            createEncryptedPrefs(appContext)
        } catch (e: Exception) {
            // 当系统恢复备份、换机或 Keystore 主密钥损坏时，旧密文文件无法被当前硬件解密
            // 兜底机制：物理擦除失效的加密文件，重置 KeyStore 根密钥并重新初始化，避免启动崩溃
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appContext.deleteSharedPreferences(PREFS_NAME)
                } else {
                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
                }
            }
            createEncryptedPrefs(appContext)
        }
    }

    private fun createEncryptedPrefs(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val trimmed = apiKey.trim()
        cachedApiKey = trimmed
        cachedApiKeyLoaded = true
        runCatching {
            getEncryptedPrefs(context).edit()
                .putString(KEY_API_KEY, trimmed)
                .apply()
        }
    }

    fun getApiKey(context: Context): String? {
        if (cachedApiKeyLoaded) return cachedApiKey
        return runCatching {
            getEncryptedPrefs(context).getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        }.getOrNull().also {
            cachedApiKey = it
            cachedApiKeyLoaded = true
        }
    }

    fun clearApiKey(context: Context) {
        cachedApiKey = null
        cachedApiKeyLoaded = true
        runCatching {
            getEncryptedPrefs(context).edit()
                .remove(KEY_API_KEY)
                .apply()
        }
    }

    fun isConfigured(context: Context): Boolean {
        return !getApiKey(context).isNullOrBlank()
    }

    fun getLowBalanceThreshold(context: Context): Float {
        cachedThreshold?.let { return it }
        return runCatching {
            getEncryptedPrefs(context).getFloat(KEY_LOW_BALANCE_THRESHOLD, 5.0f)
        }.getOrDefault(5.0f).also { cachedThreshold = it }
    }

    fun setLowBalanceThreshold(context: Context, threshold: Float) {
        cachedThreshold = threshold
        runCatching {
            getEncryptedPrefs(context).edit()
                .putFloat(KEY_LOW_BALANCE_THRESHOLD, threshold)
                .apply()
        }
    }

    fun getSyncIntervalMinutes(context: Context): Long {
        cachedSyncInterval?.let { return it }
        return runCatching {
            getEncryptedPrefs(context).getLong(KEY_SYNC_INTERVAL_MINUTES, 60L)
        }.getOrDefault(60L).also { cachedSyncInterval = it }
    }

    fun setSyncIntervalMinutes(context: Context, minutes: Long) {
        cachedSyncInterval = minutes
        runCatching {
            getEncryptedPrefs(context).edit()
                .putLong(KEY_SYNC_INTERVAL_MINUTES, minutes)
                .apply()
        }
    }

    fun isLowBalanceNotificationEnabled(context: Context): Boolean {
        cachedNotifyLowBalance?.let { return it }
        return runCatching {
            getEncryptedPrefs(context).getBoolean(KEY_NOTIFY_LOW_BALANCE, true)
        }.getOrDefault(true).also { cachedNotifyLowBalance = it }
    }

    fun setLowBalanceNotificationEnabled(context: Context, enabled: Boolean) {
        cachedNotifyLowBalance = enabled
        runCatching {
            getEncryptedPrefs(context).edit()
                .putBoolean(KEY_NOTIFY_LOW_BALANCE, enabled)
                .apply()
        }
    }

    fun getThemeMode(context: Context): ThemeMode {
        cachedThemeMode?.let { return it }
        return runCatching {
            val name = getEncryptedPrefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM).also { cachedThemeMode = it }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        cachedThemeMode = mode
        runCatching {
            getEncryptedPrefs(context).edit()
                .putString(KEY_THEME_MODE, mode.name)
                .apply()
        }
    }

    fun isAutoCheckUpdateEnabled(context: Context): Boolean {
        cachedAutoCheckUpdate?.let { return it }
        return runCatching {
            getEncryptedPrefs(context).getBoolean(KEY_AUTO_CHECK_UPDATE, true)
        }.getOrDefault(true).also { cachedAutoCheckUpdate = it }
    }

    fun setAutoCheckUpdateEnabled(context: Context, enabled: Boolean) {
        cachedAutoCheckUpdate = enabled
        runCatching {
            getEncryptedPrefs(context).edit()
                .putBoolean(KEY_AUTO_CHECK_UPDATE, enabled)
                .apply()
        }
    }

    fun isPrivacyModeEnabled(context: Context): Boolean {
        cachedPrivacyMode?.let { return it }
        return runCatching {
            getEncryptedPrefs(context).getBoolean(KEY_PRIVACY_MODE, false)
        }.getOrDefault(false).also { cachedPrivacyMode = it }
    }

    fun setPrivacyModeEnabled(context: Context, enabled: Boolean) {
        cachedPrivacyMode = enabled
        runCatching {
            getEncryptedPrefs(context).edit()
                .putBoolean(KEY_PRIVACY_MODE, enabled)
                .apply()
        }
    }

    fun getLastNotifiedLowBalanceTime(context: Context): Long {
        return runCatching {
            getEncryptedPrefs(context).getLong(KEY_LAST_NOTIFIED_LOW_BALANCE_TIME, 0L)
        }.getOrDefault(0L)
    }

    fun getLastNotifiedLowBalanceAmount(context: Context): String? {
        return runCatching {
            getEncryptedPrefs(context).getString(KEY_LAST_NOTIFIED_LOW_BALANCE_AMOUNT, null)
        }.getOrNull()
    }

    fun recordLowBalanceNotified(context: Context, amount: String) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .putLong(KEY_LAST_NOTIFIED_LOW_BALANCE_TIME, System.currentTimeMillis())
                .putString(KEY_LAST_NOTIFIED_LOW_BALANCE_AMOUNT, amount)
                .apply()
        }
    }

    fun clearLowBalanceNotified(context: Context) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .remove(KEY_LAST_NOTIFIED_LOW_BALANCE_TIME)
                .remove(KEY_LAST_NOTIFIED_LOW_BALANCE_AMOUNT)
                .apply()
        }
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

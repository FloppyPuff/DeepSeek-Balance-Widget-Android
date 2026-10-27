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

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

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
        runCatching {
            getEncryptedPrefs(context).edit()
                .putString(KEY_API_KEY, apiKey.trim())
                .apply()
        }
    }

    fun getApiKey(context: Context): String? {
        return runCatching {
            getEncryptedPrefs(context).getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun clearApiKey(context: Context) {
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
        return runCatching {
            getEncryptedPrefs(context).getFloat(KEY_LOW_BALANCE_THRESHOLD, 5.0f)
        }.getOrDefault(5.0f)
    }

    fun setLowBalanceThreshold(context: Context, threshold: Float) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .putFloat(KEY_LOW_BALANCE_THRESHOLD, threshold)
                .apply()
        }
    }

    fun getSyncIntervalMinutes(context: Context): Long {
        return runCatching {
            getEncryptedPrefs(context).getLong(KEY_SYNC_INTERVAL_MINUTES, 60L)
        }.getOrDefault(60L)
    }

    fun setSyncIntervalMinutes(context: Context, minutes: Long) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .putLong(KEY_SYNC_INTERVAL_MINUTES, minutes)
                .apply()
        }
    }

    fun isLowBalanceNotificationEnabled(context: Context): Boolean {
        return runCatching {
            getEncryptedPrefs(context).getBoolean(KEY_NOTIFY_LOW_BALANCE, true)
        }.getOrDefault(true)
    }

    fun setLowBalanceNotificationEnabled(context: Context, enabled: Boolean) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .putBoolean(KEY_NOTIFY_LOW_BALANCE, enabled)
                .apply()
        }
    }

    fun getThemeMode(context: Context): ThemeMode {
        return runCatching {
            val name = getEncryptedPrefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .putString(KEY_THEME_MODE, mode.name)
                .apply()
        }
    }

    fun isAutoCheckUpdateEnabled(context: Context): Boolean {
        return runCatching {
            getEncryptedPrefs(context).getBoolean(KEY_AUTO_CHECK_UPDATE, true)
        }.getOrDefault(true)
    }

    fun setAutoCheckUpdateEnabled(context: Context, enabled: Boolean) {
        runCatching {
            getEncryptedPrefs(context).edit()
                .putBoolean(KEY_AUTO_CHECK_UPDATE, enabled)
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

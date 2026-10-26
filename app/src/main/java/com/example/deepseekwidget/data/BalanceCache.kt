package com.example.deepseekwidget.data

import android.content.Context

data class CachedBalance(
    val data: BalanceData?,
    val updatedAt: String?,
    val updatedAtMillis: Long,
    val hasError: Boolean,
    val errorMsg: String?
)

/**
 * 非敏感展示数据持久化缓存
 * API Key 绝不保存在此处（遵循 Zero-Plaintext Policy）。
 * 用于在新小组件添加到桌面、网络刷新失败或离线时，确保桌面小组件与应用均可瞬间展示最后一次成功拉取的准实时余额。
 */
object BalanceCache {
    private const val PREFS_NAME = "deepseek_balance_cache"
    private const val KEY_TOTAL = "total_balance"
    private const val KEY_CURRENCY = "currency_symbol"
    private const val KEY_GRANTED = "granted_balance"
    private const val KEY_TOPPED_UP = "topped_up_balance"
    private const val KEY_AVAILABLE = "is_available"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_UPDATED_AT_MILLIS = "updated_at_millis"
    private const val KEY_HAS_ERROR = "has_error"
    private const val KEY_ERROR_MSG = "error_msg"

    @Synchronized
    fun save(context: Context, data: BalanceData, timeStr: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOTAL, data.totalBalance)
            .putString(KEY_CURRENCY, data.currencySymbol)
            .putString(KEY_GRANTED, data.grantedBalance)
            .putString(KEY_TOPPED_UP, data.toppedUpBalance)
            .putBoolean(KEY_AVAILABLE, data.isAvailable)
            .putString(KEY_UPDATED_AT, timeStr)
            .putLong(KEY_UPDATED_AT_MILLIS, System.currentTimeMillis())
            .putBoolean(KEY_HAS_ERROR, false)
            .remove(KEY_ERROR_MSG)
            .apply()
    }

    @Synchronized
    fun markError(context: Context, errorMsg: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HAS_ERROR, true)
            .putString(KEY_ERROR_MSG, errorMsg)
            .apply()
    }

    @Synchronized
    fun get(context: Context): CachedBalance {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.getString(KEY_TOTAL, null)
        val currency = prefs.getString(KEY_CURRENCY, "¥") ?: "¥"
        val granted = prefs.getString(KEY_GRANTED, null)
        val toppedUp = prefs.getString(KEY_TOPPED_UP, null)
        val available = prefs.getBoolean(KEY_AVAILABLE, true)
        val updatedAt = prefs.getString(KEY_UPDATED_AT, null)
        val millis = prefs.getLong(KEY_UPDATED_AT_MILLIS, 0L)
        val hasError = prefs.getBoolean(KEY_HAS_ERROR, false)
        val errorMsg = prefs.getString(KEY_ERROR_MSG, null)

        val data = if (total != null) {
            BalanceData(
                isAvailable = available,
                currencySymbol = currency,
                totalBalance = total,
                grantedBalance = granted ?: "0.00",
                toppedUpBalance = toppedUp ?: "0.00"
            )
        } else {
            null
        }

        return CachedBalance(
            data = data,
            updatedAt = updatedAt,
            updatedAtMillis = millis,
            hasError = hasError,
            errorMsg = errorMsg
        )
    }

    @Synchronized
    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}

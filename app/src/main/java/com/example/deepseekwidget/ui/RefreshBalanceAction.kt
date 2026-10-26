package com.example.deepseekwidget.ui

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.deepseekwidget.data.BalanceCache
import com.example.deepseekwidget.data.DeepSeekApiClient
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 响应刷新按钮点击的 ActionCallback
 *
 * 遵循三步流水线：
 * 1. 入口 3 秒防抖（Debounce）拦截，防止频繁连击引起并发竞争与 API 429。
 * 2. 立即将 UI 状态置为 Loading ("Syncing...")，给用户 0 延迟的即时反馈。
 * 3. 异步解密 API Key -> 网络请求 -> 写回不可变状态 -> 触发 Glance 重绘。
 */
class RefreshBalanceAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 第一步：防抖检查（Debounce 3000ms）
        val currentState: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val isLoading = currentState[WidgetPrefsKeys.IS_LOADING] ?: false
        val lastTimestamp = currentState[WidgetPrefsKeys.LAST_REFRESH_TIMESTAMP] ?: 0L
        val now = System.currentTimeMillis()

        if (isLoading || (now - lastTimestamp < WidgetSyncRepository.MIN_REFRESH_INTERVAL_MS)) {
            // 防抖静默拦截，避免连击
            return
        }

        // 第二步：立即将 UI 置为 Loading，给予用户即时视觉反馈
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefsKeys.IS_LOADING] = true
            prefs[WidgetPrefsKeys.LAST_REFRESH_TIMESTAMP] = now
        }
        DeepSeekBalanceWidget().update(context, glanceId)

        // 第三步：读取加密存储的 API Key 并执行请求
        val apiKey = SecureKeyStore.getApiKey(context)
        if (apiKey.isNullOrBlank()) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[WidgetPrefsKeys.IS_CONFIGURED] = false
                prefs[WidgetPrefsKeys.IS_LOADING] = false
                prefs[WidgetPrefsKeys.HAS_ERROR] = true
                prefs[WidgetPrefsKeys.ERROR_MSG] = "Missing API Key"
            }
            DeepSeekBalanceWidget().update(context, glanceId)
            return
        }

        val result = DeepSeekApiClient.fetchBalance(apiKey)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val threshold = SecureKeyStore.getLowBalanceThreshold(context).toDouble()

        result.onSuccess { data ->
            BalanceCache.save(context, data, timeStr)
        }.onFailure { error ->
            BalanceCache.markError(context, error.localizedMessage ?: "Sync Failed")
        }

        // 第四步：写回最新数据并触发重绘
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefsKeys.IS_CONFIGURED] = true
            prefs[WidgetPrefsKeys.IS_LOADING] = false

            result.onSuccess { data ->
                prefs[WidgetPrefsKeys.TOTAL_BALANCE] = data.totalBalance
                prefs[WidgetPrefsKeys.CURRENCY_SYMBOL] = data.currencySymbol
                prefs[WidgetPrefsKeys.GRANTED_BALANCE] = data.grantedBalance
                prefs[WidgetPrefsKeys.TOPPED_UP_BALANCE] = data.toppedUpBalance
                prefs[WidgetPrefsKeys.UPDATED_AT] = timeStr
                prefs[WidgetPrefsKeys.HAS_ERROR] = false
                prefs[WidgetPrefsKeys.ERROR_MSG] = ""
                prefs[WidgetPrefsKeys.IS_LOW_BALANCE] = data.isLowBalance(threshold)
            }.onFailure { error ->
                prefs[WidgetPrefsKeys.HAS_ERROR] = true
                prefs[WidgetPrefsKeys.ERROR_MSG] = error.localizedMessage ?: "Sync Failed"
            }
        }
        DeepSeekBalanceWidget().update(context, glanceId)
    }
}

package com.example.deepseekwidget.ui

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.deepseekwidget.data.WidgetSyncRepository

/**
 * 响应桌面小组件刷新按钮点击的 ActionCallback
 *
 * 1. 执行 3 秒防抖（Debounce）拦截，防止频繁连击触发 API 429。
 * 2. 立即将对应实例置为 Loading ("Syncing...")，给用户 0 延迟即时反馈。
 * 3. 统一委托给 WidgetSyncRepository 执行网络拉取、多币种解析、本地缓存写回与红灯通知。
 */
class RefreshBalanceAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val currentState: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val isLoading = currentState[WidgetPrefsKeys.IS_LOADING] ?: false
        val lastTimestamp = currentState[WidgetPrefsKeys.LAST_REFRESH_TIMESTAMP] ?: 0L
        val now = System.currentTimeMillis()

        if (isLoading || (now - lastTimestamp < WidgetSyncRepository.MIN_REFRESH_INTERVAL_MS)) {
            return
        }

        // 立即给予用户即时视觉反馈
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefsKeys.IS_LOADING] = true
            prefs[WidgetPrefsKeys.LAST_REFRESH_TIMESTAMP] = now
        }
        DeepSeekBalanceWidget().update(context, glanceId)

        // 委托至业务仓库统一执行数据同步
        val repository = WidgetSyncRepository(context)
        repository.syncWidget(glanceId)
    }
}

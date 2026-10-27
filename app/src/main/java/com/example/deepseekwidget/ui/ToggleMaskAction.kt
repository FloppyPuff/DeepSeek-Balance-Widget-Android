package com.example.deepseekwidget.ui

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.deepseekwidget.data.SecureKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 小组件脱敏倒计时调度器
 *
 * 彻底解决 BroadcastReceiver 内部 delay 导致的：
 * 1. 广播超时与 Android 系统级 BroadcastQueue 串行阻塞（导致连续点击出现数秒巨幅延迟与卡顿）。
 * 2. 连续快速点击竞争导致的计时器错乱。
 */
object WidgetMaskManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val autoMaskJobs = ConcurrentHashMap<GlanceId, Job>()

    /**
     * 安排 5 秒后自动恢复脱敏隐去
     */
    fun scheduleAutoMask(context: Context, glanceId: GlanceId) {
        val appContext = context.applicationContext
        autoMaskJobs[glanceId]?.cancel()
        autoMaskJobs[glanceId] = scope.launch {
            try {
                delay(5000L)
                updateAppWidgetState(appContext, glanceId) { prefs ->
                    prefs[WidgetPrefsKeys.IS_MASKED] = true
                }
                DeepSeekBalanceWidget().update(appContext, glanceId)
            } catch (e: Exception) {
                // 忽略被取消或组件已移除的异常
            } finally {
                autoMaskJobs.remove(glanceId)
            }
        }
    }

    /**
     * 取消自动脱敏倒计时（当用户主动点击隐去，或快速反复点击时）
     */
    fun cancelAutoMask(glanceId: GlanceId) {
        autoMaskJobs[glanceId]?.cancel()
        autoMaskJobs.remove(glanceId)
    }
}

/**
 * 响应小组件眼睛图标及可用余额点击的脱敏切换 ActionCallback
 *
 * 1. 若当前处于脱敏状态（例如 ¥••••），点击后立即展示明文，并在后台调度 5 秒自动恢复。
 * 2. 若当前已明文，点击立即取消倒计时并切换回脱敏，实现无缝瞬时响应。
 * 3. 绝不阻塞 onAction，确保 Android 系统 BroadcastReceiver 毫秒级返回，彻底告别卡顿与排队延迟。
 */
class ToggleMaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        var willBeMasked = false

        // 单次原子更新状态，避免多次读取 DataStore
        updateAppWidgetState(appContext, glanceId) { prefs ->
            val isCurrentlyMasked = prefs[WidgetPrefsKeys.IS_MASKED]
                ?: SecureKeyStore.isPrivacyModeEnabled(appContext)
            willBeMasked = !isCurrentlyMasked
            prefs[WidgetPrefsKeys.IS_MASKED] = willBeMasked
        }

        // 立即触发小组件重绘并同步释放 BroadcastReceiver，绝不在 onAction 内部 delay
        DeepSeekBalanceWidget().update(appContext, glanceId)

        if (!willBeMasked) {
            // 用户点击临时展示明文：启动独立协程倒计时 5 秒后自动隐去
            WidgetMaskManager.scheduleAutoMask(appContext, glanceId)
        } else {
            // 用户主动点击隐去：立即取消任何正在进行的倒计时
            WidgetMaskManager.cancelAutoMask(glanceId)
        }
    }
}

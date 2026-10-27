package com.example.deepseekwidget.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.deepseekwidget.R
import com.example.deepseekwidget.ui.DeepSeekBalanceWidget
import com.example.deepseekwidget.ui.MainActivity
import com.example.deepseekwidget.ui.WidgetPrefsKeys
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 小组件统一同步与状态写回仓库
 * 统一处理网络请求、状态机转换、低余额预警与所有 Widget 实例的批量广播更新
 */
class WidgetSyncRepository(private val context: Context) {

    companion object {
        const val LOW_BALANCE_CHANNEL_ID = "deepseek_low_balance_channel"
        const val NOTIFICATION_ID_LOW_BALANCE = 1001
        const val MIN_REFRESH_INTERVAL_MS = 3000L // 3 秒防抖
    }

    /**
     * 更新告警阈值并立即重算全量小组件预警红/蓝指示灯状态
     */
    suspend fun updateThreshold(newThreshold: Float) {
        SecureKeyStore.setLowBalanceThreshold(context, newThreshold)
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = runCatching {
            manager.getGlanceIds(DeepSeekBalanceWidget::class.java)
        }.getOrDefault(emptyList())
        val thresholdDouble = newThreshold.toDouble()
        val cached = BalanceCache.get(context)

        glanceIds.forEach { id ->
            runCatching {
                updateAppWidgetState(context, id) { prefs ->
                    val currentTotal = prefs[WidgetPrefsKeys.TOTAL_BALANCE]?.toDoubleOrNull()
                        ?: cached.data?.totalBalanceNumber
                    if (currentTotal != null) {
                        prefs[WidgetPrefsKeys.IS_LOW_BALANCE] = currentTotal < thresholdDouble
                    }
                }
                DeepSeekBalanceWidget().update(context, id)
            }
        }
    }

    /**
     * 执行单组件同步刷新
     * @return Result<BalanceData> 同步结果
     */
    suspend fun syncWidget(glanceId: GlanceId, force: Boolean = false): Result<BalanceData> {
        val apiKey = SecureKeyStore.getApiKey(context)
        if (apiKey.isNullOrBlank()) {
            runCatching {
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[WidgetPrefsKeys.IS_LOADING] = false
                    prefs[WidgetPrefsKeys.HAS_ERROR] = true
                    prefs[WidgetPrefsKeys.ERROR_MSG] = "Missing API Key"
                }
                DeepSeekBalanceWidget().update(context, glanceId)
            }
            return Result.failure(IllegalStateException("API Key not configured"))
        }

        val result = DeepSeekApiClient.fetchBalance(apiKey)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val threshold = SecureKeyStore.getLowBalanceThreshold(context).toDouble()

        result.onSuccess { data ->
            BalanceCache.save(context, data, timeStr)
        }.onFailure { err ->
            BalanceCache.markError(context, err.localizedMessage ?: "Sync Failed")
        }

        runCatching {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[WidgetPrefsKeys.IS_LOADING] = false
                prefs[WidgetPrefsKeys.LAST_REFRESH_TIMESTAMP] = System.currentTimeMillis()

                result.onSuccess { data ->
                    prefs[WidgetPrefsKeys.TOTAL_BALANCE] = data.totalBalance
                    prefs[WidgetPrefsKeys.CURRENCY_SYMBOL] = data.currencySymbol
                    prefs[WidgetPrefsKeys.GRANTED_BALANCE] = data.grantedBalance
                    prefs[WidgetPrefsKeys.TOPPED_UP_BALANCE] = data.toppedUpBalance
                    prefs[WidgetPrefsKeys.UPDATED_AT] = timeStr
                    prefs[WidgetPrefsKeys.HAS_ERROR] = false
                    prefs[WidgetPrefsKeys.ERROR_MSG] = ""
                    val isLow = data.isLowBalance(threshold)
                    prefs[WidgetPrefsKeys.IS_LOW_BALANCE] = isLow

                    checkAndNotifyLowBalance(data, threshold)
                }.onFailure { error ->
                    prefs[WidgetPrefsKeys.HAS_ERROR] = true
                    prefs[WidgetPrefsKeys.ERROR_MSG] = error.localizedMessage ?: "Sync Failed"
                }
            }
            DeepSeekBalanceWidget().update(context, glanceId)
        }
        return result
    }

    /**
     * 批量同步所有已放置在桌面的组件
     */
    suspend fun syncAllWidgets(): Result<BalanceData> {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = runCatching {
            manager.getGlanceIds(DeepSeekBalanceWidget::class.java)
        }.getOrDefault(emptyList())

        val apiKey = SecureKeyStore.getApiKey(context)
        if (apiKey.isNullOrBlank()) {
            glanceIds.forEach { id ->
                runCatching {
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[WidgetPrefsKeys.IS_LOADING] = false
                    }
                    DeepSeekBalanceWidget().update(context, id)
                }
            }
            return Result.failure(IllegalStateException("API Key not configured"))
        }

        // 统一网络拉取一次，分发至所有小组件实例
        val result = DeepSeekApiClient.fetchBalance(apiKey)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val threshold = SecureKeyStore.getLowBalanceThreshold(context).toDouble()

        result.onSuccess { data ->
            BalanceCache.save(context, data, timeStr)
            checkAndNotifyLowBalance(data, threshold)
        }.onFailure { err ->
            BalanceCache.markError(context, err.localizedMessage ?: "Sync Failed")
        }

        glanceIds.forEach { id ->
            runCatching {
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetPrefsKeys.IS_LOADING] = false
                    prefs[WidgetPrefsKeys.LAST_REFRESH_TIMESTAMP] = System.currentTimeMillis()

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
                DeepSeekBalanceWidget().update(context, id)
            }
        }

        return result
    }

    private fun checkAndNotifyLowBalance(data: BalanceData, threshold: Double) {
        val isLow = data.isLowBalance(threshold)
        if (isLow) {
            if (SecureKeyStore.isLowBalanceNotificationEnabled(context)) {
                val lastNotifiedTime = SecureKeyStore.getLastNotifiedLowBalanceTime(context)
                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                val shouldNotify = (lastNotifiedTime == 0L) || (now - lastNotifiedTime > oneDayMs)
                if (shouldNotify) {
                    sendLowBalanceNotification(data.currencySymbol, data.totalBalance)
                    SecureKeyStore.recordLowBalanceNotified(context, data.totalBalance)
                }
            }
        } else {
            // 余额正常或回升（例如充值后），重置通知记录，下次再次跌破时能及时响应
            SecureKeyStore.clearLowBalanceNotified(context)
        }
    }

    private fun sendLowBalanceNotification(symbol: String, balance: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LOW_BALANCE_CHANNEL_ID,
                "DeepSeek 余额告警",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当 DeepSeek API 余额低于警戒阈值时发送通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.createChooser(this, null).flags
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LOW_BALANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refresh_minimal)
            .setContentTitle("DeepSeek 余额过低提醒")
            .setContentText("当前可用余额仅为 $symbol$balance，请及时充值以防 API 服务中断。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_LOW_BALANCE, notification)
    }
}

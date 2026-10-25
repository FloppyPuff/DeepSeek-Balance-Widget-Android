package com.example.deepseekwidget

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import com.example.deepseekwidget.work.BalanceSyncWorker

/**
 * 应用程序入口
 */
class DeepSeekWidgetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initBackgroundSync()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WidgetSyncRepository.LOW_BALANCE_CHANNEL_ID,
                "DeepSeek 余额预警",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "DeepSeek API 账户余额低于设定阈值时发出强提醒"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initBackgroundSync() {
        val interval = SecureKeyStore.getSyncIntervalMinutes(this)
        BalanceSyncWorker.schedulePeriodicSync(this, interval)
    }
}

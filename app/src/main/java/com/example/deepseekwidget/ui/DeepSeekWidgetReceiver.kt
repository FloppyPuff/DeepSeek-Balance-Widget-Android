package com.example.deepseekwidget.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 桌面小组件广播接收器
 */
class DeepSeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeepSeekBalanceWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        triggerWidgetSync(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        triggerWidgetSync(context)
    }

    private fun triggerWidgetSync(context: Context) {
        if (SecureKeyStore.isConfigured(context)) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WidgetSyncRepository(context).syncAllWidgets()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

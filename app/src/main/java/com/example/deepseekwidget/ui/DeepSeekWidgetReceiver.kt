package com.example.deepseekwidget.ui

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 桌面小组件广播接收器
 */
class DeepSeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeepSeekBalanceWidget()
}

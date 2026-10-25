package com.example.deepseekwidget.ui

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.ui.render.BalanceTypographyRenderer

/**
 * DeepSeek 余额监控小组件（Jetpack Glance）
 *
 * 设计定位：极简主义（Minimalism）、高对比度排版（Editorial Typography）、零冗余交互。
 * 布局结构：
 * - 顶栏：状态指示灯 (●) + 品牌标识 (DEEPSEEK) + 更新时间/同步中 + 独立无序刷新键 [↻]
 * - 核心区：离屏 Canvas 矢量化位图艺术字体渲染（¥ 128.45）
 * - 底栏：副文案胶囊标签（Granted: ¥10.00 | Top-up: ¥118.45）
 */
class DeepSeekBalanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasKey = SecureKeyStore.isConfigured(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val isConfigured = (prefs[WidgetPrefsKeys.IS_CONFIGURED] ?: false) && hasKey

            if (!isConfigured) {
                UnconfiguredCard()
            } else {
                val balance = prefs[WidgetPrefsKeys.TOTAL_BALANCE] ?: "--.--"
                val symbol = prefs[WidgetPrefsKeys.CURRENCY_SYMBOL] ?: "¥"
                val granted = prefs[WidgetPrefsKeys.GRANTED_BALANCE]
                val toppedUp = prefs[WidgetPrefsKeys.TOPPED_UP_BALANCE]
                val updatedAt = prefs[WidgetPrefsKeys.UPDATED_AT] ?: "Never"
                val isLoading = prefs[WidgetPrefsKeys.IS_LOADING] ?: false
                val hasError = prefs[WidgetPrefsKeys.HAS_ERROR] ?: false
                val isLow = prefs[WidgetPrefsKeys.IS_LOW_BALANCE] ?: false

                MinimalistBalanceCard(
                    symbol = symbol,
                    balance = balance,
                    granted = granted,
                    toppedUp = toppedUp,
                    updatedAt = updatedAt,
                    isLoading = isLoading,
                    hasError = hasError,
                    isLowBalance = isLow
                )
            }
        }
    }

    /**
     * 未配置状态卡片：引导点击打开主界面输入 API Key
     */
    @Composable
    private fun UnconfiguredCard() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xE6121316)))
                .cornerRadius(22.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "● ",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFB300)),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "DEEPSEEK",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF8E95A5)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = "Tap to configure Key",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Text(
                text = "点击小组件进入应用配置 API Key",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF8E95A5)),
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.padding(top = 2.dp)
            )

            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }

    /**
     * 已配置状态卡片：遵循 80% 视觉重心归于数字、20% 归于状态与交互的极简原则
     */
    @Composable
    private fun MinimalistBalanceCard(
        symbol: String,
        balance: String,
        granted: String?,
        toppedUp: String?,
        updatedAt: String,
        isLoading: Boolean,
        hasError: Boolean,
        isLowBalance: Boolean
    ) {
        val context = LocalContext.current

        // 生成高分辨率艺术字体 Bitmap
        val balanceBitmap = BalanceTypographyRenderer.renderBalanceBitmap(
            context = context,
            currencySymbol = symbol,
            balanceStr = balance,
            primaryColor = Color.parseColor("#FFFFFF"),
            secondaryColor = Color.parseColor("#8E95A5")
        )

        // 状态指示点颜色逻辑：
        // 报错或低余额预警时显示红色/警戒色 (#FF5252)，平时显示 DeepSeek 标志性电光蓝 (#4D6BFE)
        val statusDotColor = when {
            hasError || isLowBalance -> androidx.compose.ui.graphics.Color(0xFFFF5252)
            else -> androidx.compose.ui.graphics.Color(0xFF4D6BFE)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xE6121316)))
                .cornerRadius(22.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // 1. 顶部状态栏：品牌名 + 同步状态/时间戳 + 独立刷新热区
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "● ",
                    style = TextStyle(
                        color = ColorProvider(statusDotColor),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "DEEPSEEK",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF8E95A5)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = if (isLoading) "Syncing..." else updatedAt,
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF636B78)),
                        fontSize = 11.sp
                    ),
                    modifier = GlanceModifier.padding(end = 6.dp)
                )

                // 独立刷新按钮热区（28dp 圆形胶囊，内部 14dp 矢量图标，手感极佳）
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .cornerRadius(14.dp)
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0x1AFFFFFF)))
                        .clickable(onClick = actionRunCallback<RefreshBalanceAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh_minimal),
                        contentDescription = "Refresh Balance",
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 2. 核心视觉区：艺术字体余额位图展示
            Image(
                provider = ImageProvider(balanceBitmap),
                contentDescription = "Balance $symbol$balance",
                modifier = GlanceModifier.height(48.dp)
            )

            // 3. 底栏副文案（如果有充值与赠送明细，微弱对比度展示）
            if (!granted.isNullOrBlank() && !toppedUp.isNullOrBlank()) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Granted: $symbol$granted  |  Top-up: $symbol$toppedUp",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF636B78)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

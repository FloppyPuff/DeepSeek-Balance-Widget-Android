package com.example.deepseekwidget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
 * 全面支持系统日间/暗夜模式自适应（Day/Night Mode Auto-Switching）：
 * - 纯资源语义绑定（values/colors.xml 与 values-night/colors.xml）：
 *   底层采用系统级 ColorProvider(R.color.*)，在系统切换暗色/亮色模式时由 Android RemoteViews
 *   引擎直接自动重绘，零延迟、零能耗、无需手动刷新。
 * - 艺术字排版位图自适应染色：
 *   离屏 Canvas 输出高精度 Alpha 遮罩，经由 ColorFilter.tint(ColorProvider(R.color.primary_ink))
 *   在日间（Alabaster 深墨字）与暗夜（Obsidian 亮白字）间自动完成完美色彩过渡。
 */
class DeepSeekBalanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasKey = SecureKeyStore.isConfigured(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val isConfigured = (prefs[WidgetPrefsKeys.IS_CONFIGURED] ?: false) && hasKey
            val size = LocalSize.current

            if (!isConfigured) {
                UnconfiguredCard(size = size)
            } else {
                val balance = prefs[WidgetPrefsKeys.TOTAL_BALANCE] ?: "--.--"
                val symbol = prefs[WidgetPrefsKeys.CURRENCY_SYMBOL] ?: "¥"
                val granted = prefs[WidgetPrefsKeys.GRANTED_BALANCE]
                val toppedUp = prefs[WidgetPrefsKeys.TOPPED_UP_BALANCE]
                val updatedAt = prefs[WidgetPrefsKeys.UPDATED_AT] ?: "Never"
                val isLoading = prefs[WidgetPrefsKeys.IS_LOADING] ?: false
                val hasError = prefs[WidgetPrefsKeys.HAS_ERROR] ?: false
                val isLow = prefs[WidgetPrefsKeys.IS_LOW_BALANCE] ?: false

                AdaptiveBalanceCard(
                    size = size,
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
     * 未配置状态卡片（自适应日间/暗夜模式）
     */
    @Composable
    private fun UnconfiguredCard(size: DpSize) {
        val isTall = size.height >= 105.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.surface_bg))
                .cornerRadius(22.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "● ",
                    style = TextStyle(
                        color = ColorProvider(R.color.status_warning),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "DEEPSEEK",
                    style = TextStyle(
                        color = ColorProvider(R.color.secondary_ink),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = "Tap to configure Key",
                style = TextStyle(
                    color = ColorProvider(R.color.primary_ink),
                    fontSize = if (isTall) 16.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            if (isTall) {
                Text(
                    text = "点击小组件进入应用配置 API Key",
                    style = TextStyle(
                        color = ColorProvider(R.color.secondary_ink),
                        fontSize = 11.sp
                    ),
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }

    /**
     * 自适应尺寸与日夜模式卡片
     */
    @Composable
    private fun AdaptiveBalanceCard(
        size: DpSize,
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
        val isTall = size.height >= 105.dp
        val isWide = size.width >= 195.dp

        // 生成高动态范围艺术字体自适应遮罩位图
        val balanceBitmap = BalanceTypographyRenderer.renderAdaptiveBalanceBitmap(
            context = context,
            currencySymbol = symbol,
            balanceStr = balance
        )

        // 状态指示点颜色绑定
        val statusDotColor = when {
            hasError || isLowBalance -> ColorProvider(R.color.status_warning)
            else -> ColorProvider(R.color.accent_deepseek)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.surface_bg))
                .cornerRadius(22.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // 1. 顶部状态栏：根据宽度决定是否在顶栏容纳时间戳，彻底规避挤压裁切
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧品牌
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "● ",
                        style = TextStyle(
                            color = statusDotColor,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = "DEEPSEEK",
                        style = TextStyle(
                            color = ColorProvider(R.color.secondary_ink),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 仅在宽屏（>=195dp，如 3×1、4×1）时在顶栏显示更新时间
                if (isWide) {
                    Text(
                        text = if (isLoading) "Syncing..." else updatedAt,
                        style = TextStyle(
                            color = ColorProvider(R.color.muted_ink),
                            fontSize = 11.sp
                        ),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                }

                // 独立刷新按钮（日夜模式自适应底色与图标）
                RefreshButton()
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 2. 核心视觉区：艺术字体余额位图展示（配合 ColorFilter 自动随日间/暗夜模式着色）
            Image(
                provider = ImageProvider(balanceBitmap),
                contentDescription = "Balance $symbol$balance",
                colorFilter = ColorFilter.tint(ColorProvider(R.color.primary_ink)),
                modifier = GlanceModifier.height(if (isTall) 50.dp else 44.dp)
            )

            // 3. 底栏区域：根据尺寸自适应排版
            if (isTall) {
                // 2×2 / 3×2 等高尺寸：双列清晰明细 + 独立时间戳，杜绝文字折行重叠
                Spacer(modifier = GlanceModifier.defaultWeight())

                if (!granted.isNullOrBlank() || !toppedUp.isNullOrBlank()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!granted.isNullOrBlank()) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "Granted",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.secondary_ink),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "$symbol$granted",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.primary_ink),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        if (!toppedUp.isNullOrBlank()) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "Top-up",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.secondary_ink),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "$symbol$toppedUp",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.primary_ink),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // 2×2 底部专属状态/更新时间行
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLoading) "● Syncing..." else "Updated $updatedAt",
                        style = TextStyle(
                            color = if (isLoading) ColorProvider(R.color.accent_deepseek) else ColorProvider(R.color.muted_ink),
                            fontSize = 10.sp
                        )
                    )
                }
            } else {
                // 2×1 / 3×1 等矮尺寸：
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWide && !granted.isNullOrBlank() && !toppedUp.isNullOrBlank()) {
                        Text(
                            text = "Granted: $symbol$granted  |  Top-up: $symbol$toppedUp",
                            style = TextStyle(
                                color = ColorProvider(R.color.muted_ink),
                                fontSize = 10.sp
                            )
                        )
                    } else {
                        // 2×1 窄版：显示单行状态提示
                        Text(
                            text = if (isLoading) "● Syncing..." else "Updated $updatedAt",
                            style = TextStyle(
                                color = if (isLoading) ColorProvider(R.color.accent_deepseek) else ColorProvider(R.color.muted_ink),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RefreshButton() {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .cornerRadius(14.dp)
                .background(ColorProvider(R.color.widget_button_bg))
                .clickable(onClick = actionRunCallback<RefreshBalanceAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh_minimal),
                contentDescription = "Refresh Balance",
                colorFilter = ColorFilter.tint(ColorProvider(R.color.primary_ink)),
                modifier = GlanceModifier.size(14.dp)
            )
        }
    }
}

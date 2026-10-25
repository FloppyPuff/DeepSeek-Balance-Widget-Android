package com.example.deepseekwidget.ui

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.layout.width
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
 * 自适应响应：
 * - 2×1（紧凑横幅）：单行顶栏（品牌+刷新键），核心艺术字，底部紧凑更新状态。
 * - 2×2（方形大卡）：顶栏极简放宽防挤压（品牌与独立刷新键，绝无溢出裁切），中部超大艺术字，
 *   底部双列清晰展示 Granted 与 Top-up 明细，并在独立行标注更新时间，彻底告别单行文字折行挤压。
 * - 3×1 / 4×1 / 3×2（宽屏尺寸）：展开展示完整品牌、更新时间、胶囊明细。
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
     * 未配置状态卡片
     */
    @Composable
    private fun UnconfiguredCard(size: DpSize) {
        val isTall = size.height >= 105.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xE6121316)))
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
                    fontSize = if (isTall) 16.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            if (isTall) {
                Text(
                    text = "点击小组件进入应用配置 API Key",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF8E95A5)),
                        fontSize = 11.sp
                    ),
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }

    /**
     * 自适应尺寸渲染卡片
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

        // 生成艺术字体 Bitmap
        val balanceBitmap = BalanceTypographyRenderer.renderBalanceBitmap(
            context = context,
            currencySymbol = symbol,
            balanceStr = balance,
            primaryColor = Color.parseColor("#FFFFFF"),
            secondaryColor = Color.parseColor("#8E95A5")
        )

        val statusDotColor = when {
            hasError || isLowBalance -> androidx.compose.ui.graphics.Color(0xFFFF5252)
            else -> androidx.compose.ui.graphics.Color(0xFF4D6BFE)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xE6121316)))
                .cornerRadius(22.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // 1. 顶部状态栏：根据宽度决定是否在顶栏容纳时间戳，彻底规避 2×2 / 2×1 挤压剪裁
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧品牌
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 仅在宽屏（>=195dp，如 3×1、4×1）时在顶栏显示更新时间
                if (isWide) {
                    Text(
                        text = if (isLoading) "Syncing..." else updatedAt,
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF636B78)),
                            fontSize = 11.sp
                        ),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                }

                // 独立刷新按钮（固定 28dp，安全锚定在右边界内部，杜绝裁切）
                RefreshButton()
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 2. 核心视觉区：艺术字体余额位图展示
            Image(
                provider = ImageProvider(balanceBitmap),
                contentDescription = "Balance $symbol$balance",
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
                                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF8E95A5)),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "$symbol$granted",
                                    style = TextStyle(
                                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
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
                                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF8E95A5)),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "$symbol$toppedUp",
                                    style = TextStyle(
                                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
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
                            color = ColorProvider(
                                androidx.compose.ui.graphics.Color(if (isLoading) 0xFF4D6BFE else 0xFF636B78)
                            ),
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
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF636B78)),
                                fontSize = 10.sp
                            )
                        )
                    } else {
                        // 2×1 窄版：显示单行状态提示
                        Text(
                            text = if (isLoading) "● Syncing..." else "Updated $updatedAt",
                            style = TextStyle(
                                color = ColorProvider(
                                    androidx.compose.ui.graphics.Color(if (isLoading) 0xFF4D6BFE else 0xFF636B78)
                                ),
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
}

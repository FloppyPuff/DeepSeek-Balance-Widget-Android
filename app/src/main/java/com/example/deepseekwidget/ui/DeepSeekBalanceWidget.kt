package com.example.deepseekwidget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.BalanceCache
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.ui.render.BalanceTypographyRenderer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * DeepSeek 余额监控小组件（Jetpack Glance）
 *
 * 全面支持响应式排版自适应（2×1, 3×1, 4×1, 2×2, 3×2, 2×4, 3×4, 4×4）：
 * - 矮高度单行（2×1、3×1、4×1）：隐藏拥挤底栏，将辅助信息（时间/明细）无缝上收至顶栏，
 *   居中展示核心余额位图，杜绝任何字符被挤压截断。
 * - 紧凑双列（2×2、3×2）：经典双列 Granted / Top-up 结构与独立时间戳。
 * - 超高大图（2×4、3×4）：针对长垂直空间优化，采用纵向堆叠明细或杂志式大仪表板布局，杜绝大片留白。
 * - 日夜模式自适应（Day/Night Mode）：纯语义化 ColorProvider 与 Canvas 遮罩位图染色。
 */
class DeepSeekBalanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasKey = SecureKeyStore.isConfigured(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val size = LocalSize.current

            if (!hasKey) {
                UnconfiguredCard(size = size)
            } else {
                val cached = remember { BalanceCache.get(context) }
                val balance = prefs[WidgetPrefsKeys.TOTAL_BALANCE] ?: cached.data?.totalBalance ?: "--.--"
                val symbol = prefs[WidgetPrefsKeys.CURRENCY_SYMBOL] ?: cached.data?.currencySymbol ?: "¥"
                val granted = prefs[WidgetPrefsKeys.GRANTED_BALANCE] ?: cached.data?.grantedBalance
                val toppedUp = prefs[WidgetPrefsKeys.TOPPED_UP_BALANCE] ?: cached.data?.toppedUpBalance
                val updatedAt = prefs[WidgetPrefsKeys.UPDATED_AT] ?: cached.updatedAt ?: "Never"
                val isLoading = prefs[WidgetPrefsKeys.IS_LOADING] ?: false
                val hasError = prefs[WidgetPrefsKeys.HAS_ERROR] ?: cached.hasError
                val threshold = SecureKeyStore.getLowBalanceThreshold(context).toDouble()
                val isLow = prefs[WidgetPrefsKeys.IS_LOW_BALANCE]
                    ?: (cached.data?.isLowBalance(threshold) ?: false)

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
        val compactHeight = size.height < 100.dp
        val isVeryTall = size.height >= 200.dp
        val horizontalPadding = if (size.width < 180.dp) 12.dp else 16.dp
        val verticalPadding = if (compactHeight) 8.dp else if (isVeryTall) 16.dp else 12.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.surface_bg))
                .cornerRadius(22.dp)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // 顶栏品牌
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "● ",
                    style = TextStyle(
                        color = ColorProvider(R.color.status_warning),
                        fontSize = if (compactHeight) 8.sp else 10.sp
                    )
                )
                Text(
                    text = "DEEPSEEK",
                    style = TextStyle(
                        color = ColorProvider(R.color.secondary_ink),
                        fontSize = if (compactHeight) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            if (isVeryTall) {
                Text(
                    text = "API KEY REQUIRED",
                    style = TextStyle(
                        color = ColorProvider(R.color.muted_ink),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "未配置密钥",
                    style = TextStyle(
                        color = ColorProvider(R.color.primary_ink),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "点击小组件进入应用配置 API Key",
                    style = TextStyle(
                        color = ColorProvider(R.color.secondary_ink),
                        fontSize = 11.sp
                    )
                )
            } else {
                Text(
                    text = if (compactHeight) "点击配置 API 密钥" else "Tap to configure Key",
                    style = TextStyle(
                        color = ColorProvider(R.color.primary_ink),
                        fontSize = if (compactHeight) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!compactHeight && size.height >= 120.dp) {
                    Text(
                        text = "点击小组件进入应用配置 API Key",
                        style = TextStyle(
                            color = ColorProvider(R.color.secondary_ink),
                            fontSize = 11.sp
                        ),
                        modifier = GlanceModifier.padding(top = 4.dp)
                    )
                }
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
        val density = context.resources.displayMetrics.density

        // 尺寸分类：高度划分（矮/中/超高）与宽度划分（窄2列/中3列/宽4列）
        val compactHeight = size.height < 100.dp
        val isVeryTall = size.height >= 200.dp
        val isNarrow = size.width < 180.dp
        val isMediumWidth = size.width in 180.dp..249.dp
        val isWide = size.width >= 250.dp

        // 内边距自适应策略
        val horizontalPadding = if (isNarrow) 12.dp else 16.dp
        val verticalPadding = when {
            compactHeight -> if (size.height < 70.dp) 6.dp else 8.dp
            isVeryTall -> 16.dp
            else -> 12.dp
        }

        // 顶栏高度预留
        val headerHeight = if (compactHeight) 24.dp else 28.dp

        // 计算余额字体自适应 Sp 基准
        val baseSizeSp: Float = when {
            compactHeight -> {
                val widthCap = when {
                    isNarrow -> 28f
                    isMediumWidth -> 34f
                    else -> 38f
                }
                val heightCap = when {
                    size.height < 68.dp -> 24f
                    size.height < 82.dp -> 28f
                    else -> 32f
                }
                minOf(widthCap, heightCap)
            }
            isVeryTall -> {
                if (isNarrow) 40f else 50f
            }
            else -> {
                // 中等高度 (2×2, 3×2)
                if (isNarrow) 38f else 44f
            }
        }

        // 计算给 Hero Bitmap 的宽度和高度预算（像素）
        val widthBudgetDp = (size.width - horizontalPadding * 2).coerceAtLeast(60.dp)
        val heightBudgetDp = when {
            compactHeight -> (size.height - headerHeight - verticalPadding * 2 - 2.dp).coerceAtLeast(18.dp)
            isVeryTall -> (size.height * 0.30f).coerceAtLeast(44.dp)
            else -> (size.height - 68.dp - verticalPadding * 2).coerceAtLeast(28.dp)
        }

        val maxWidthPx = (widthBudgetDp.value * density).roundToInt().coerceAtLeast(1)
        val maxHeightPx = (heightBudgetDp.value * density).roundToInt().coerceAtLeast(1)

        // 生成高动态范围艺术字体自适应遮罩位图
        val balanceBitmap = BalanceTypographyRenderer.renderAdaptiveBalanceBitmap(
            context = context,
            currencySymbol = symbol,
            balanceStr = balance,
            baseSizeSp = baseSizeSp,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
        )

        val heroWidthDp = max(1f, balanceBitmap.width / density).dp
        val heroHeightDp = max(1f, balanceBitmap.height / density).dp

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
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // ==========================================
            // 1. 顶部状态栏
            // ==========================================
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧指示灯 + 品牌
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "● ",
                        style = TextStyle(
                            color = statusDotColor,
                            fontSize = if (compactHeight) 8.sp else 10.sp
                        )
                    )
                    Text(
                        text = "DEEPSEEK",
                        style = TextStyle(
                            color = ColorProvider(R.color.secondary_ink),
                            fontSize = if (compactHeight) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 顶栏右侧辅助信息展示：
                // - 矮高度宽屏（如 4×1）：直接在顶栏优雅展示 Granted / Top-up 摘要
                // - 矮高度中屏（如 3×1）或中/高屏：展示更新时间
                if (compactHeight && isWide && !granted.isNullOrBlank() && !toppedUp.isNullOrBlank()) {
                    Text(
                        text = "G: $symbol$granted  T: $symbol$toppedUp",
                        style = TextStyle(
                            color = ColorProvider(R.color.muted_ink),
                            fontSize = 9.sp
                        ),
                        modifier = GlanceModifier.padding(end = 8.dp)
                    )
                } else if ((compactHeight && isMediumWidth) || (!compactHeight && !isNarrow)) {
                    Text(
                        text = if (isLoading) "Syncing..." else updatedAt,
                        style = TextStyle(
                            color = ColorProvider(R.color.muted_ink),
                            fontSize = if (compactHeight) 9.sp else 10.sp
                        ),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                }

                // 独立刷新按钮
                RefreshButton(
                    size = if (compactHeight) 24.dp else if (isVeryTall) 30.dp else 28.dp,
                    iconSize = if (compactHeight) 12.dp else if (isVeryTall) 15.dp else 14.dp
                )
            }

            // ==========================================
            // 2. 核心视觉区：根据尺寸布局
            // ==========================================
            when {
                // ------------------------------------------
                // A. 矮高度单行模式（2×1、3×1、4×1）
                // 彻底去除底栏，数字垂直严格居中，自适应缩小绝不截断
                // ------------------------------------------
                compactHeight -> {
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(balanceBitmap),
                        contentDescription = "Balance $symbol$balance",
                        colorFilter = ColorFilter.tint(ColorProvider(R.color.primary_ink)),
                        modifier = GlanceModifier.size(width = heroWidthDp, height = heroHeightDp)
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())
                }

                // ------------------------------------------
                // B. 超高模式（2×4、3×4、4×4）
                // 优雅充盈纵向空间：带标题标签、超大数字、分隔线与结构化明细
                // ------------------------------------------
                isVeryTall -> {
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Text(
                        text = if (isNarrow) "AVAILABLE BALANCE" else "TOTAL AVAILABLE BALANCE",
                        style = TextStyle(
                            color = ColorProvider(R.color.muted_ink),
                            fontSize = if (isNarrow) 8.sp else 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(if (isNarrow) 4.dp else 6.dp))

                    Image(
                        provider = ImageProvider(balanceBitmap),
                        contentDescription = "Balance $symbol$balance",
                        colorFilter = ColorFilter.tint(ColorProvider(R.color.primary_ink)),
                        modifier = GlanceModifier.size(width = heroWidthDp, height = heroHeightDp)
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // 浅色微细线分隔
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProvider(R.color.hairline_border))
                    ) {}

                    Spacer(modifier = GlanceModifier.height(if (isNarrow) 8.dp else 10.dp))

                    if (isNarrow) {
                        // 2×4 纵向卡片明细堆叠
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                text = "GRANTED",
                                style = TextStyle(
                                    color = ColorProvider(R.color.secondary_ink),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "$symbol${granted ?: "--"}",
                                style = TextStyle(
                                    color = ColorProvider(R.color.primary_ink),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Text(
                                text = "TOP-UP",
                                style = TextStyle(
                                    color = ColorProvider(R.color.secondary_ink),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "$symbol${toppedUp ?: "--"}",
                                style = TextStyle(
                                    color = ColorProvider(R.color.primary_ink),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        // 3×4 / 4×4 横向大双列明细
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "GRANTED BALANCE",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.secondary_ink),
                                        fontSize = 9.sp
                                    )
                                )
                                Text(
                                    text = "$symbol${granted ?: "--"}",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.primary_ink),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "TOP-UP BALANCE",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.secondary_ink),
                                        fontSize = 9.sp
                                    )
                                )
                                Text(
                                    text = "$symbol${toppedUp ?: "--"}",
                                    style = TextStyle(
                                        color = ColorProvider(R.color.primary_ink),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // 底部常驻同步状态行
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoading) "● Syncing..." else "Updated $updatedAt",
                            style = TextStyle(
                                color = if (isLoading) ColorProvider(R.color.accent_deepseek) else ColorProvider(R.color.muted_ink),
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                // ------------------------------------------
                // C. 中等尺寸模式（2×2、3×2）
                // 经典双列 Granted / Top-up 与清晰底部时间行
                // ------------------------------------------
                else -> {
                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(balanceBitmap),
                        contentDescription = "Balance $symbol$balance",
                        colorFilter = ColorFilter.tint(ColorProvider(R.color.primary_ink)),
                        modifier = GlanceModifier.size(width = heroWidthDp, height = heroHeightDp)
                    )

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
                                            fontSize = 9.sp
                                        )
                                    )
                                    Text(
                                        text = "$symbol$granted",
                                        style = TextStyle(
                                            color = ColorProvider(R.color.primary_ink),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            if (!toppedUp.isNullOrBlank()) {
                                Column(
                                    modifier = GlanceModifier.defaultWeight(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Top-up",
                                        style = TextStyle(
                                            color = ColorProvider(R.color.secondary_ink),
                                            fontSize = 9.sp
                                        )
                                    )
                                    Text(
                                        text = "$symbol$toppedUp",
                                        style = TextStyle(
                                            color = ColorProvider(R.color.primary_ink),
                                            fontSize = 11.sp,
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
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RefreshButton(
        size: Dp = 28.dp,
        iconSize: Dp = 14.dp
    ) {
        Box(
            modifier = GlanceModifier
                .size(size)
                .cornerRadius(size / 2)
                .background(ColorProvider(R.color.widget_button_bg))
                .clickable(onClick = actionRunCallback<RefreshBalanceAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh_minimal),
                contentDescription = "Refresh Balance",
                colorFilter = ColorFilter.tint(ColorProvider(R.color.primary_ink)),
                modifier = GlanceModifier.size(iconSize)
            )
        }
    }
}

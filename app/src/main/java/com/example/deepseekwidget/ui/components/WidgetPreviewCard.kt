package com.example.deepseekwidget.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.BalanceData
import com.example.deepseekwidget.ui.render.BalanceTypographyRenderer
import com.example.deepseekwidget.ui.theme.DisplayFontFamily
import com.example.deepseekwidget.ui.theme.LocalPalette
import com.example.deepseekwidget.ui.theme.NumberStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SAMPLE_TOTAL = "128.45"
private const val SAMPLE_GRANTED = "10.00"
private const val SAMPLE_TOP_UP = "118.45"

@Composable
fun WidgetPreviewCard(
    isConfigured: Boolean,
    balanceData: BalanceData?,
    isLoading: Boolean,
    errorMessage: String?,
    is2x2: Boolean,
    lowBalanceThreshold: Float,
    onRefresh: () -> Unit,
    isMasked: Boolean = false,
    onToggleMask: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(22.dp)
    val refreshDescription = stringResource(R.string.action_refresh)
    val sampleTag = stringResource(R.string.sample_tag)

    val isSample = !isConfigured
    val hasData = isConfigured && balanceData != null

    val total = when {
        hasData -> balanceData.totalBalance
        isSample -> SAMPLE_TOTAL
        else -> null
    }
    val granted = when {
        hasData -> balanceData.grantedBalance
        isSample -> SAMPLE_GRANTED
        else -> null
    }
    val toppedUp = when {
        hasData -> balanceData.toppedUpBalance
        isSample -> SAMPLE_TOP_UP
        else -> null
    }
    val symbol = balanceData?.currencySymbol ?: "¥"

    val displayTotal = if (isMasked) "••••" else total
    val displayGranted = if (isMasked) (if (granted != null) "••••" else null) else granted
    val displayToppedUp = if (isMasked) (if (toppedUp != null) "••••" else null) else toppedUp

    val totalNum = balanceData?.totalBalanceNumber ?: 0.0
    val isLowBalance = hasData && (totalNum < lowBalanceThreshold.toDouble())
    val isAvailable = balanceData?.isAvailable ?: true

    val dotColor = when {
        isSample -> palette.accent
        isLoading -> palette.accent
        errorMessage != null -> palette.warning
        !isAvailable -> palette.warning
        isLowBalance -> palette.warning
        else -> palette.accent
    }
    val statusLabel = when {
        errorMessage != null -> "ERROR"
        hasData && !isAvailable -> "UNAVAILABLE"
        isLowBalance -> "LOW"
        else -> null
    }

    val background = if (palette.isDark) {
        Brush.verticalGradient(listOf(palette.surfaceHighlight, palette.surface))
    } else {
        Brush.verticalGradient(listOf(Color.White, Color(0xFFF7F8FA)))
    }
    val borderColor = if (palette.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    val cardHeight = if (is2x2) 186.dp else 118.dp
    val verticalPadding = if (is2x2) 14.dp else 10.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (palette.isDark) 0.6f else 0.12f),
                spotColor = palette.accent.copy(alpha = if (palette.isDark) 0.20f else 0.10f),
            )
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = verticalPadding),
    ) {
        val primaryColorInt = if (hasData || isSample) {
            palette.primaryInk.toArgb()
        } else {
            palette.secondaryInk.copy(alpha = 0.50f).toArgb()
        }
        val secondaryColorInt = if (hasData || isSample) {
            palette.secondaryInk.toArgb()
        } else {
            palette.tertiaryInk.toArgb()
        }

        val bitmap = remember(
            displayTotal,
            symbol,
            palette.isDark,
            primaryColorInt,
            secondaryColorInt,
            is2x2,
            isMasked
        ) {
            BalanceTypographyRenderer.renderBalanceBitmap(
                context = context,
                currencySymbol = symbol,
                balanceStr = displayTotal ?: "--.--",
                primaryColor = primaryColorInt,
                secondaryColor = secondaryColorInt,
                baseSizeSp = if (is2x2) 36f else 30f,
            )
        }
        val heroWidth = with(density) { (bitmap.width / density.density).dp }
        val heroHeight = with(density) { (bitmap.height / density.density).dp }

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：指示灯 + DEEPSEEK + 状态 + 时间 + 刷新键
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape),
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    text = "DEEPSEEK",
                    color = palette.secondaryInk,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                )
                if (!is2x2 && onToggleMask != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleMask
                        )
                    ) {
                        EyeGlyph(
                            visible = !isMasked,
                            tint = palette.secondaryInk,
                            size = 12.dp
                        )
                    }
                }
                if (statusLabel != null) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = statusLabel,
                        color = if (errorMessage != null || isLowBalance || !isAvailable) {
                            palette.warning
                        } else {
                            palette.tertiaryInk
                        },
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 7.sp,
                        letterSpacing = 0.6.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        strokeWidth = 1.2.dp,
                        color = palette.accent,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "SYNCING",
                        color = palette.accent,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                    )
                } else if (isSample) {
                    Text(
                        text = sampleTag,
                        color = palette.tertiaryInk,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 8.sp,
                        letterSpacing = 1.4.sp,
                    )
                } else {
                    Text(
                        text = formatTime(System.currentTimeMillis()),
                        color = palette.tertiaryInk,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                    )
                    Spacer(Modifier.size(8.dp))
                    RefreshCircle(
                        enabled = !isLoading,
                        onClick = onRefresh,
                        refreshDescription = refreshDescription,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // 核心余额艺术位图
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = if (displayTotal != null) {
                    "余额 ${symbol}${displayTotal}${if (isSample) "，示例预览" else ""}"
                } else {
                    "余额尚未同步"
                },
                modifier = Modifier
                    .size(width = heroWidth, height = heroHeight),
            )

            if (is2x2) {
                // 模仿参考图：在余额下方展示「可用余额 👁」脱敏开关
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .then(if (onToggleMask != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggleMask) else Modifier)
                ) {
                    Text(
                        text = "可用余额",
                        color = palette.secondaryInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.width(4.dp))
                    EyeGlyph(
                        visible = !isMasked,
                        tint = palette.secondaryInk,
                        size = 12.dp
                    )
                }

                Spacer(Modifier.weight(1f))

                // 2×2 底栏区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.hairline),
                )

                // 2×2 双列明细
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    DetailLabel(
                        label = "GRANTED",
                        value = if (displayGranted != null) "$symbol$displayGranted" else "—",
                        muted = displayGranted == null,
                    )
                    DetailLabel(
                        label = "TOP-UP",
                        value = if (displayToppedUp != null) "$symbol$displayToppedUp" else "—",
                        muted = displayToppedUp == null,
                        alignEnd = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLabel(
    label: String,
    value: String,
    muted: Boolean,
    alignEnd: Boolean = false,
) {
    val palette = LocalPalette.current
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label,
            color = palette.tertiaryInk,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.5.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = value,
            color = if (muted) palette.tertiaryInk else palette.secondaryInk,
            style = NumberStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun RefreshCircle(
    enabled: Boolean,
    onClick: () -> Unit,
    refreshDescription: String,
) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = refreshDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.chipFill)
                .border(1.dp, palette.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RefreshGlyph(
                tint = if (enabled) palette.secondaryInk else palette.tertiaryInk,
                size = 13.dp,
            )
        }
    }
}

private fun formatTime(epochMillis: Long?): String {
    if (epochMillis == null) return "—:—"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

package com.example.deepseekwidget.ui.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import kotlin.math.ceil

/**
 * Glance 艺术字体渲染引擎（Bitmap Font Engine）
 *
 * 受限于 Android RemoteViews 无法直接从 assets/fonts 加载自定义字体，
 * 在内存中使用原生 Canvas + TextPaint 离屏光栅化，将余额文本渲染为高清晰度抗锯齿位图。
 *
 * 核心特性：
 * 1. 货币符号、超大整数与小数共享同一基线（Baseline Alignment）。
 * 2. 紧凑字间距（letterSpacing = -0.03f）彰显先锋几何感。
 * 3. 严格限制 Bitmap 尺寸（仅裁剪文本区域），彻底规避 Binder 1.5MB TransactionTooLargeException。
 */
object BalanceTypographyRenderer {

    @Volatile
    private var cachedTypeface: Typeface? = null

    private fun getArtisticTypeface(context: Context): Typeface {
        return cachedTypeface ?: synchronized(this) {
            cachedTypeface ?: runCatching {
                Typeface.createFromAsset(context.assets, "fonts/SpaceGrotesk-Bold.ttf")
            }.getOrElse {
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }.also { cachedTypeface = it }
        }
    }

    /**
     * 将余额字符串渲染为高动态自适应遮罩 Bitmap
     * 整数主体采用纯白不透明 (255)，货币符号与小数采用约 65% 透明度 (166)
     * 配合 Glance 的 ColorFilter.tint(ColorProvider(R.color.primary_ink))，
     * 可在日间/暗夜模式下由系统 RemoteViews 自动完成对比度染色并实时无缝切换。
     */
    fun renderAdaptiveBalanceBitmap(
        context: Context,
        currencySymbol: String,
        balanceStr: String
    ): Bitmap {
        return renderBalanceBitmap(
            context = context,
            currencySymbol = currencySymbol,
            balanceStr = balanceStr,
            primaryColor = Color.argb(255, 255, 255, 255),
            secondaryColor = Color.argb(166, 255, 255, 255)
        )
    }

    /**
     * 将余额字符串渲染为带有主次层级艺术字体的高清 Bitmap
     *
     * @param context Android 上下文
     * @param currencySymbol 货币符号，如 "¥" 或 "$"
     * @param balanceStr 原始余额字符串，如 "128.45"
     * @param primaryColor 整数主体文字颜色（例如 #FFFFFF）
     * @param secondaryColor 货币符号与小数位文字颜色（例如 #8E95A5）
     */
    fun renderBalanceBitmap(
        context: Context,
        currencySymbol: String,
        balanceStr: String,
        primaryColor: Int,
        secondaryColor: Int
    ): Bitmap {
        val parts = balanceStr.split(".")
        val intPart = parts.getOrNull(0)?.ifEmpty { "0" } ?: "0"
        val decPart = if (parts.size > 1) ".${parts[1]}" else ".00"

        val artisticTypeface = getArtisticTypeface(context)
        val density = context.resources.displayMetrics.scaledDensity.coerceAtLeast(1.0f)

        // 1. 定义三段式画笔：货币符号、整数核心、小数部分
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = artisticTypeface
            textSize = 22f * density
            color = secondaryColor
        }

        val intPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = artisticTypeface
            textSize = 46f * density
            color = primaryColor
            letterSpacing = -0.03f // 紧凑艺术字间距
        }

        val decPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = artisticTypeface
            textSize = 24f * density
            color = secondaryColor
        }

        // 2. 精确测量宽度与高度
        val gap = 6f * density
        val symbolWidth = symbolPaint.measureText(currencySymbol)
        val intWidth = intPaint.measureText(intPart)
        val decWidth = decPaint.measureText(decPart)

        val totalWidth = ceil(symbolWidth + gap + intWidth + decWidth + 8f).toInt().coerceAtLeast(1)
        val fontMetrics = intPaint.fontMetrics
        val totalHeight = ceil(fontMetrics.descent - fontMetrics.ascent + 8f).toInt().coerceAtLeast(1)
        val baselineY = -fontMetrics.ascent + 4f

        // 3. 离屏绘制
        val bitmap = createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 绘制货币符号（与主数字共享基线）
        canvas.drawText(currencySymbol, 2f, baselineY, symbolPaint)
        // 绘制整数部分
        canvas.drawText(intPart, 2f + symbolWidth + gap, baselineY, intPaint)
        // 绘制小数部分
        canvas.drawText(decPart, 2f + symbolWidth + gap + intWidth, baselineY, decPaint)

        return bitmap
    }
}

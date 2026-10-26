package com.example.deepseekwidget.ui.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import kotlin.math.ceil
import kotlin.math.max

/**
 * Glance 艺术字体渲染引擎（Adaptive Bitmap Font Engine）
 *
 * 受限于 Android RemoteViews 无法直接从 assets/fonts 加载自定义字体，
 * 在内存中使用原生 Canvas + TextPaint 离屏光栅化，将余额文本渲染为高清晰度抗锯齿位图。
 *
 * 核心特性：
 * 1. 货币符号、超大整数与小数共享同一基线（Baseline Alignment）。
 * 2. 紧凑字间距（letterSpacing = -0.03f）彰显先锋几何感。
 * 3. 严格的多尺寸自适应降级循环（Auto-Downscale Loop）：依据 maxWidthPx 与 maxHeightPx 自动微调字号，绝不截断。
 * 4. 严格限制 Bitmap 尺寸（仅裁剪文本区域），彻底规避 Binder 1.5MB TransactionTooLargeException。
 */
object BalanceTypographyRenderer {

    private const val MAX_BITMAP_WIDTH_PX = 960
    private const val MAX_BITMAP_HEIGHT_PX = 320

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
     * 整数主体采用纯白不透明 (255)，货币符号与小数采用约 67% 透明度 (170)
     * 配合 Glance 的 ColorFilter.tint(ColorProvider(R.color.primary_ink))，
     * 可在日间/暗夜模式下由系统 RemoteViews 自动完成对比度染色并实时无缝切换。
     */
    fun renderAdaptiveBalanceBitmap(
        context: Context,
        currencySymbol: String,
        balanceStr: String,
        baseSizeSp: Float = 44f,
        maxWidthPx: Int = Int.MAX_VALUE,
        maxHeightPx: Int = Int.MAX_VALUE,
    ): Bitmap {
        return renderBalanceBitmap(
            context = context,
            currencySymbol = currencySymbol,
            balanceStr = balanceStr,
            primaryColor = Color.argb(255, 255, 255, 255),
            secondaryColor = Color.argb(170, 255, 255, 255),
            baseSizeSp = baseSizeSp,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
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
     * @param baseSizeSp 基准字号（Sp）
     * @param maxWidthPx 最大允许宽度像素
     * @param maxHeightPx 最大允许高度像素
     */
    fun renderBalanceBitmap(
        context: Context,
        currencySymbol: String,
        balanceStr: String,
        primaryColor: Int,
        secondaryColor: Int,
        baseSizeSp: Float = 44f,
        maxWidthPx: Int = Int.MAX_VALUE,
        maxHeightPx: Int = Int.MAX_VALUE,
    ): Bitmap {
        val parts = balanceStr.split(".")
        val intPart = parts.getOrNull(0)?.ifEmpty { "0" } ?: "0"
        val decPart = if (parts.size > 1) ".${parts[1]}" else ".00"

        val artisticTypeface = getArtisticTypeface(context)
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity.coerceAtLeast(1.0f)

        val maxWidth = maxWidthPx.coerceIn(1, MAX_BITMAP_WIDTH_PX)
        val maxHeight = maxHeightPx.coerceIn(1, MAX_BITMAP_HEIGHT_PX)

        var currentSp = baseSizeSp.coerceAtLeast(11f)

        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = artisticTypeface
            color = secondaryColor
            isDither = true
        }

        val intPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = artisticTypeface
            color = primaryColor
            letterSpacing = -0.03f
            isDither = true
        }

        val decPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = artisticTypeface
            color = secondaryColor
            isDither = true
        }

        val horizontalPaddingPx = max(2f, 2f * density)
        val symbolGap = max(2f, 3.5f * density)

        var totalWidth = 0
        var totalHeight = 0
        var baselineY = 0f
        var symbolWidth = 0f
        var intWidth = 0f

        // 动态自适应降级循环：确保在小尺寸（如 2×1、3×1、4×1）或大数字下绝不发生溢出截断
        while (true) {
            val basePx = currentSp * scaledDensity
            symbolPaint.textSize = max(basePx * 0.40f, 9.5f * scaledDensity)
            intPaint.textSize = basePx
            decPaint.textSize = max(basePx * 0.50f, 11f * scaledDensity)

            symbolWidth = symbolPaint.measureText(currencySymbol)
            intWidth = intPaint.measureText(intPart)
            val decWidth = decPaint.measureText(decPart)

            val contentWidth = symbolWidth + symbolGap + intWidth + decWidth
            totalWidth = ceil(contentWidth + horizontalPaddingPx * 2f).toInt().coerceAtLeast(1)

            val fontMetrics = intPaint.fontMetrics
            val contentHeight = fontMetrics.descent - fontMetrics.ascent
            totalHeight = ceil(contentHeight + 4f * density).toInt().coerceAtLeast(1)
            baselineY = 2f * density - fontMetrics.ascent

            val overflows = totalWidth > maxWidth || totalHeight > maxHeight
            if (!overflows || currentSp <= 11f) {
                break
            }
            currentSp = (currentSp - 1f).coerceAtLeast(11f)
        }

        // 离屏位图光栅化
        val bitmap = createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var drawX = horizontalPaddingPx
        // 1. 绘制货币符号
        canvas.drawText(currencySymbol, drawX, baselineY, symbolPaint)
        drawX += symbolWidth + symbolGap
        // 2. 绘制整数部分
        canvas.drawText(intPart, drawX, baselineY, intPaint)
        drawX += intWidth
        // 3. 绘制小数部分
        canvas.drawText(decPart, drawX, baselineY, decPaint)

        return bitmap
    }
}

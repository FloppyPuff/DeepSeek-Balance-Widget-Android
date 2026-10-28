package com.example.deepseekwidget.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SlidersGlyph(
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.075f
        val knobRadius = this.size.minDimension * 0.125f
        val rows = listOf(0.28f to 0.36f, 0.5f to 0.66f, 0.72f to 0.46f)
        rows.forEach { (yFraction, knobFraction) ->
            val y = this.size.height * yFraction
            drawLine(
                color = tint.copy(alpha = 0.9f),
                start = Offset(0f, y),
                end = Offset(this.size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val knobX = this.size.width * knobFraction
            drawCircle(color = tint, radius = knobRadius, center = Offset(knobX, y))
            drawCircle(
                color = background,
                radius = knobRadius * 0.45f,
                center = Offset(knobX, y),
            )
        }
    }
}

@Composable
fun LockGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        val body = Rect(w * 0.18f, h * 0.44f, w * 0.82f, h * 0.88f)
        drawRoundRect(
            color = tint,
            topLeft = body.topLeft,
            size = body.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.3f, h * 0.16f),
            size = Size(w * 0.4f, h * 0.42f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawCircle(color = tint, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.65f))
    }
}

@Composable
fun EyeGlyph(
    visible: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.075f
        val eyePath = Path().apply {
            moveTo(w * 0.08f, h * 0.5f)
            cubicTo(w * 0.27f, h * 0.24f, w * 0.73f, h * 0.24f, w * 0.92f, h * 0.5f)
            cubicTo(w * 0.73f, h * 0.76f, w * 0.27f, h * 0.76f, w * 0.08f, h * 0.5f)
            close()
        }
        drawPath(eyePath, color = tint, style = Stroke(width = stroke))
        drawCircle(color = tint, radius = w * 0.115f, center = Offset(w * 0.5f, h * 0.5f))
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(w * 0.16f, h * 0.84f),
                end = Offset(w * 0.84f, h * 0.16f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun RefreshGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.1f
        drawArc(
            color = tint,
            startAngle = 50f,
            sweepAngle = 285f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.16f),
            size = Size(w * 0.68f, h * 0.68f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val arrow = Path().apply {
            moveTo(w * 0.73f, h * 0.12f)
            lineTo(w * 0.92f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.32f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun PlusGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ChevronRightGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.36f, h * 0.2f)
            lineTo(w * 0.66f, h * 0.5f)
            lineTo(w * 0.36f, h * 0.8f)
        }
        drawPath(
            path,
            color = tint,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun ShieldGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.84f, h * 0.22f)
            cubicTo(w * 0.84f, h * 0.54f, w * 0.70f, h * 0.78f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.30f, h * 0.78f, w * 0.16f, h * 0.54f, w * 0.16f, h * 0.22f)
            close()
        }
        drawPath(shieldPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        val checkPath = Path().apply {
            moveTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.47f, h * 0.62f)
            lineTo(w * 0.65f, h * 0.38f)
        }
        drawPath(checkPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

package com.example.deepseekwidget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekwidget.BuildConfig
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.BalanceData
import com.example.deepseekwidget.data.ThemeMode
import com.example.deepseekwidget.ui.theme.DisplayFontFamily
import com.example.deepseekwidget.ui.theme.LocalPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    hasKey: Boolean,
    currentThreshold: Float,
    customThresholdInput: String,
    currentBalance: BalanceData?,
    currentInterval: Long,
    onThresholdSelected: (Float) -> Unit,
    onCustomThresholdChanged: (String) -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    onRemoveKeyRequest: () -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.primaryInk,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 2.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(palette.tertiaryInk.copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = palette.primaryInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(24.dp))

            // 外观模式三态切换
            Text(
                text = stringResource(R.string.theme_title),
                color = palette.primaryInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            ThemeModeSelector(
                selected = themeMode,
                onSelect = onThemeModeChange,
            )

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.hairline),
            )
            Spacer(Modifier.height(20.dp))

            // 低余额警戒线设定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.threshold_title),
                    color = palette.primaryInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "当前阈值：¥$currentThreshold",
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(10.dp))

            // 预设快捷金额 Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2.0f, 5.0f, 10.0f, 20.0f, 50.0f).forEach { value ->
                    val selected = currentThreshold == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) palette.accent else palette.fieldFill)
                            .border(1.dp, if (selected) palette.accent else palette.hairline, RoundedCornerShape(10.dp))
                            .clickable { onThresholdSelected(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "¥${value.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) palette.onAccent else palette.secondaryInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 自定义金额输入框
            OutlinedTextField(
                value = customThresholdInput,
                onValueChange = onCustomThresholdChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("输入自定义告警金额，如 2.50, 8.00 等", fontSize = 12.sp, color = palette.tertiaryInk)
                },
                leadingIcon = {
                    Text(
                        text = "¥",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.fieldFill,
                    unfocusedContainerColor = palette.fieldFill,
                    focusedBorderColor = palette.accent.copy(alpha = 0.75f),
                    unfocusedBorderColor = palette.hairline,
                    focusedTextColor = palette.primaryInk,
                    unfocusedTextColor = palette.primaryInk,
                    cursorColor = palette.accent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // 实时预警对照状态反馈
            if (currentBalance != null) {
                Spacer(Modifier.height(10.dp))
                val isTriggered = currentBalance.totalBalanceNumber < currentThreshold.toDouble()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isTriggered) palette.warning.copy(alpha = 0.12f) else palette.accent.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTriggered) palette.warning.copy(alpha = 0.4f) else palette.accent.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (isTriggered) palette.warning else palette.accent, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isTriggered) {
                                "当前可用余额 ¥${currentBalance.totalBalance} 低于告警线 ¥${currentThreshold}，小组件点亮红灯"
                            } else {
                                "当前可用余额 ¥${currentBalance.totalBalance} 高于告警线 ¥${currentThreshold}，状态正常"
                            },
                            fontSize = 11.sp,
                            color = if (isTriggered) palette.warning else palette.secondaryInk,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.hairline),
            )
            Spacer(Modifier.height(20.dp))

            // 后台静默同步周期
            Text(
                text = stringResource(R.string.interval_title),
                color = palette.primaryInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15L to "15m", 30L to "30m", 60L to "1h", 120L to "2h").forEach { (minutes, label) ->
                    val selected = currentInterval == minutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) palette.accent else palette.fieldFill)
                            .border(1.dp, if (selected) palette.accent else palette.hairline, RoundedCornerShape(10.dp))
                            .clickable { onIntervalSelected(minutes) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) palette.onAccent else palette.secondaryInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.hairline),
            )

            // 移除凭据操作（仅已配置时显示）
            if (hasKey) {
                Text(
                    text = stringResource(R.string.key_remove),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onRemoveKeyRequest)
                        .padding(vertical = 16.dp),
                    color = palette.warning,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.hairline),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.about_title),
                color = palette.secondaryInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                color = palette.tertiaryInk,
                fontFamily = DisplayFontFamily,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_note),
                color = palette.tertiaryInk,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val palette = LocalPalette.current
    val options = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val shape = RoundedCornerShape(14.dp)
    val knobShape = RoundedCornerShape(11.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(palette.fieldFill)
            .border(1.dp, palette.hairline, shape)
            .padding(4.dp),
    ) {
        val itemWidth = maxWidth / options.size
        val offsetX by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "themeIndicator",
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToPx(), 0) }
                .width(itemWidth)
                .fillMaxHeight()
                .clip(knobShape)
                .background(palette.surface)
                .border(1.dp, palette.hairline, knobShape),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            options.forEachIndexed { index, (mode, labelRes) ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) palette.primaryInk else palette.secondaryInk,
                    label = "themeText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(knobShape)
                        .clickable(enabled = !isSelected) { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(labelRes),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

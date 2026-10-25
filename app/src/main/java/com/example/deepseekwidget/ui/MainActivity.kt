package com.example.deepseekwidget.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.BalanceData
import com.example.deepseekwidget.ui.render.BalanceTypographyRenderer
import com.example.deepseekwidget.ui.theme.AccentDeepSeek
import com.example.deepseekwidget.ui.theme.DeepSeekWidgetTheme
import com.example.deepseekwidget.ui.theme.ObsidianBorder
import com.example.deepseekwidget.ui.theme.ObsidianCardBg
import com.example.deepseekwidget.ui.theme.ObsidianMutedInk
import com.example.deepseekwidget.ui.theme.ObsidianPrimaryInk
import com.example.deepseekwidget.ui.theme.ObsidianSecondaryInk
import com.example.deepseekwidget.ui.theme.ObsidianSurfaceBg
import com.example.deepseekwidget.ui.theme.StatusSuccess
import com.example.deepseekwidget.ui.theme.StatusWarningDark

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeepSeekWidgetTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianSurfaceBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 顶栏：品牌标识与状态
            HeaderSection(isConfigured = uiState.isConfigured)

            // 凭证配置区
            KeyInputCard(
                apiKey = uiState.apiKey,
                isKeyVisible = uiState.isKeyVisible,
                isLoading = uiState.isLoading,
                onKeyChanged = { viewModel.onApiKeyChanged(it) },
                onToggleVisibility = { viewModel.toggleKeyVisibility() },
                onPaste = {
                    clipboardManager.getText()?.text?.let { viewModel.onApiKeyChanged(it) }
                },
                onTestConnection = { viewModel.testAndFetchBalance() },
                onSaveAndSync = { viewModel.saveKeyAndSyncWidgets() },
                onClear = { viewModel.clearKey() }
            )

            // 状态提示反馈
            StatusFeedback(
                errorMessage = uiState.errorMessage,
                successMessage = uiState.successMessage
            )

            // 实时余额卡片
            uiState.balanceData?.let { data ->
                LiveBalanceCard(data = data)
            }

            // 小组件所见即所得渲染预览
            WidgetPreviewSection(
                uiState = uiState,
                onToggleMode = { viewModel.togglePreviewMode() },
                onToggleSize = { viewModel.togglePreviewSize() },
                onRefreshClick = { viewModel.testAndFetchBalance() }
            )

            // 同步与告警配置
            PreferencesSection(
                currentThreshold = uiState.lowBalanceThreshold,
                customThresholdInput = uiState.customThresholdInput,
                currentBalance = uiState.balanceData,
                currentInterval = uiState.syncIntervalMinutes,
                onThresholdSelected = { viewModel.setThreshold(it) },
                onCustomThresholdChanged = { viewModel.onCustomThresholdInputChanged(it) },
                onIntervalSelected = { viewModel.setInterval(it) },
                onPinWidget = {
                    val pinned = viewModel.requestPinWidget(context)
                    val msg = if (pinned) "已发送添加小组件请求至桌面" else "当前桌面暂不支持自动添加，请手动长按桌面添加"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )

            // 架构与安全背书
            ArchitectureSecurityCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeaderSection(isConfigured: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConfigured) AccentDeepSeek else StatusWarningDark)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEEPSEEK API",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianSecondaryInk,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "余额监控小组件",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianPrimaryInk,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Text(
                text = "v1.0 Glance",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AccentDeepSeek,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun KeyInputCard(
    apiKey: String,
    isKeyVisible: Boolean,
    isLoading: Boolean,
    onKeyChanged: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onPaste: () -> Unit,
    onTestConnection: () -> Unit,
    onSaveAndSync: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentDeepSeek,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "API Key 凭证配置",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianPrimaryInk
                )
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = onKeyChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("sk-...", color = ObsidianMutedInk, fontSize = 14.sp)
                },
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = ObsidianSecondaryInk
                            )
                        }
                        IconButton(onClick = onPaste) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = ObsidianSecondaryInk
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentDeepSeek,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = ObsidianPrimaryInk,
                    unfocusedTextColor = ObsidianPrimaryInk,
                    cursorColor = AccentDeepSeek
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ObsidianPrimaryInk),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    enabled = !isLoading && apiKey.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ObsidianPrimaryInk, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("测试连接", fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = onSaveAndSync,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDeepSeek),
                    enabled = !isLoading && apiKey.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存并同步组件", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (apiKey.isNotBlank()) {
                Text(
                    text = "清除本地加密凭证",
                    fontSize = 12.sp,
                    color = StatusWarningDark,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onClear() }
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun StatusFeedback(errorMessage: String?, successMessage: String?) {
    if (!errorMessage.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0x26FF5252),
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarningDark)
        ) {
            Text(
                text = errorMessage,
                color = StatusWarningDark,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    } else if (!successMessage.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0x2600C853),
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess)
        ) {
            Text(
                text = successMessage,
                color = StatusSuccess,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun LiveBalanceCard(data: BalanceData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "账户余额详情",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianPrimaryInk
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (data.isAvailable) Color(0x2600C853) else Color(0x26FF5252)
                ) {
                    Text(
                        text = if (data.isAvailable) "API 可用" else "服务受限",
                        color = if (data.isAvailable) StatusSuccess else StatusWarningDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 大数字展示
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = data.currencySymbol,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianSecondaryInk,
                    modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
                )
                Text(
                    text = data.totalBalance,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianPrimaryInk
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "赠送金余额", fontSize = 11.sp, color = ObsidianSecondaryInk)
                    Text(
                        text = "${data.currencySymbol}${data.grantedBalance}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ObsidianPrimaryInk
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "充值金余额", fontSize = 11.sp, color = ObsidianSecondaryInk)
                    Text(
                        text = "${data.currencySymbol}${data.toppedUpBalance}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ObsidianPrimaryInk
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewSection(
    uiState: MainUiState,
    onToggleMode: () -> Unit,
    onToggleSize: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = uiState.previewDarkMode
    val is2x2 = uiState.previewIs2x2

    val displaySymbol = uiState.balanceData?.currencySymbol ?: "¥"
    val displayBalance = uiState.balanceData?.totalBalance ?: "2.43"
    val displayGranted = uiState.balanceData?.grantedBalance ?: "0.00"
    val displayToppedUp = uiState.balanceData?.toppedUpBalance ?: "2.43"

    // 艺术字 Bitmap 离屏渲染测试
    val previewBitmap = BalanceTypographyRenderer.renderBalanceBitmap(
        context = context,
        currencySymbol = displaySymbol,
        balanceStr = displayBalance,
        primaryColor = if (isDark) AndroidColor.parseColor("#FFFFFF") else AndroidColor.parseColor("#111318"),
        secondaryColor = if (isDark) AndroidColor.parseColor("#8E95A5") else AndroidColor.parseColor("#636B78")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = AccentDeepSeek,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "桌面小组件预览 (${if (is2x2) "2×2 方形大卡" else "2×1 紧凑横幅"})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianPrimaryInk
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentDeepSeek.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onToggleSize() }
                ) {
                    Text(
                        text = if (is2x2) "切到 2×1" else "切到 2×2",
                        color = AccentDeepSeek,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onToggleMode) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Mode",
                        tint = ObsidianSecondaryInk
                    )
                }
            }
        }

        // 小组件仿真实体容器（宽度固定类似 2 列宽度，模拟真实桌面）
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(if (is2x2) 180.dp else 260.dp)
                    .height(if (is2x2) 180.dp else 120.dp),
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) Color(0xE6121316) else Color(0xF2F7F8FA),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // 1. 顶栏：左侧品牌，右侧刷新键，彻底杜绝折行挤压与裁切
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "● ",
                                color = AccentDeepSeek,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "DEEPSEEK",
                                color = if (isDark) Color(0xFF8E95A5) else Color(0xFF636B78),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (!is2x2) {
                            Text(
                                text = if (uiState.isLoading) "Syncing..." else "23:45",
                                color = if (isDark) Color(0xFF636B78) else Color(0xFF9EA6B4),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x12000000))
                                .clickable { onRefreshClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh_minimal),
                                contentDescription = "Refresh",
                                tint = if (isDark) Color.White else Color(0xFF111318),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 2. 核心区：艺术字位图
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Balance",
                        modifier = Modifier.height(if (is2x2) 50.dp else 44.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 3. 底栏区域
                    if (is2x2) {
                        // 2×2：双列明细 + 独立行时间戳，零文字折叠挤压
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Granted",
                                    color = if (isDark) Color(0xFF8E95A5) else Color(0xFF636B78),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "$displaySymbol$displayGranted",
                                    color = if (isDark) Color.White else Color(0xFF111318),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Top-up",
                                    color = if (isDark) Color(0xFF8E95A5) else Color(0xFF636B78),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "$displaySymbol$displayToppedUp",
                                    color = if (isDark) Color.White else Color(0xFF111318),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (uiState.isLoading) "● Syncing..." else "Updated 23:45",
                            color = if (isDark) Color(0xFF636B78) else Color(0xFF9EA6B4),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        // 2×1
                        Text(
                            text = "Granted: $displaySymbol$displayGranted  |  Top-up: $displaySymbol$displayToppedUp",
                            color = if (isDark) Color(0xFF636B78) else Color(0xFF9EA6B4),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferencesSection(
    currentThreshold: Float,
    customThresholdInput: String,
    currentBalance: BalanceData?,
    currentInterval: Long,
    onThresholdSelected: (Float) -> Unit,
    onCustomThresholdChanged: (String) -> Unit,
    onIntervalSelected: (Long) -> Unit,
    onPinWidget: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = AccentDeepSeek,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "策略与小组件设定",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianPrimaryInk
                )
            }

            // 低余额警戒阈值选择（支持预设与自定义）
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "低余额警戒阈值",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ObsidianPrimaryInk
                    )
                    Text(
                        text = "当前阈值：¥$currentThreshold",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentDeepSeek
                    )
                }

                // 快捷预设金额 Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2.0f, 5.0f, 10.0f, 20.0f, 50.0f).forEach { value ->
                        val selected = currentThreshold == value
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) AccentDeepSeek else ObsidianSurfaceBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AccentDeepSeek else ObsidianBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onThresholdSelected(value) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "¥${value.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else ObsidianSecondaryInk
                                )
                            }
                        }
                    }
                }

                // 自定义告警余额输入框
                OutlinedTextField(
                    value = customThresholdInput,
                    onValueChange = onCustomThresholdChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义告警金额", fontSize = 12.sp, color = ObsidianSecondaryInk) },
                    placeholder = { Text("如 2.50, 8.00, 15 等任意金额", fontSize = 13.sp, color = ObsidianMutedInk) },
                    leadingIcon = {
                        Text(
                            text = "¥",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentDeepSeek,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentDeepSeek,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = ObsidianPrimaryInk,
                        unfocusedTextColor = ObsidianPrimaryInk,
                        cursorColor = AccentDeepSeek
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // 实时对比预警状态反馈卡片
                if (currentBalance != null) {
                    val currentTotal = currentBalance.totalBalanceNumber
                    val isTriggered = currentTotal < currentThreshold
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTriggered) Color(0x26FF5252) else Color(0x1A4D6BFE),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTriggered) StatusWarningDark.copy(alpha = 0.5f) else AccentDeepSeek.copy(alpha = 0.3f)
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isTriggered) StatusWarningDark else AccentDeepSeek)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTriggered) {
                                    "当前可用余额 ¥${currentBalance.totalBalance} 低于告警线 ¥${currentThreshold}，小组件已点亮红灯并推送通知"
                                } else {
                                    "当前可用余额 ¥${currentBalance.totalBalance} 高于告警线 ¥${currentThreshold}，状态正常（电光蓝）"
                                },
                                fontSize = 12.sp,
                                color = if (isTriggered) StatusWarningDark else ObsidianSecondaryInk
                            )
                        }
                    }
                }
            }

            // 静默自动同步周期
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "后台静默同步周期 (WorkManager Connected)",
                    fontSize = 12.sp,
                    color = ObsidianSecondaryInk
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15L to "15m", 30L to "30m", 60L to "1h", 120L to "2h").forEach { (minutes, label) ->
                        val selected = currentInterval == minutes
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) AccentDeepSeek else ObsidianSurfaceBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) AccentDeepSeek else ObsidianBorder
                            ),
                            modifier = Modifier.clickable { onIntervalSelected(minutes) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else ObsidianSecondaryInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 快捷添加组件按钮
            Button(
                onClick = onPinWidget,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232731))
            ) {
                Icon(imageVector = Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("一键将小组件添加至手机桌面", fontSize = 13.sp, color = ObsidianPrimaryInk)
            }
        }
    }
}

@Composable
fun ArchitectureSecurityCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "零明文凭据安全与边界保障",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianPrimaryInk
                )
            }

            Text(
                text = "• Zero-Plaintext Policy: API Key 经由 Android KeyStore 硬件根 AES256-GCM 全程加密，绝不进入明文 DataStore。\n" +
                        "• IPC Binder Limit: 仅对排版字符区域离屏光栅化，严格将 Bitmap 控制在 200KB 以内，杜绝 1.5MB Binder 溢出崩溃。\n" +
                        "• Refresh Debounce: 客户端 3 秒硬件级事件防抖，彻底消除并发竞争与频繁调用。",
                fontSize = 12.sp,
                color = ObsidianSecondaryInk,
                lineHeight = 18.sp
            )
        }
    }
}

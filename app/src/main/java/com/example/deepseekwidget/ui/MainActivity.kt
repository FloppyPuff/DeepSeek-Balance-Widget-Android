package com.example.deepseekwidget.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.example.deepseekwidget.BuildConfig
import com.example.deepseekwidget.data.UpdateCheckState
import com.example.deepseekwidget.data.UpdateInfo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.deepseekwidget.R
import com.example.deepseekwidget.ui.components.EyeGlyph
import com.example.deepseekwidget.ui.components.LockGlyph
import com.example.deepseekwidget.ui.components.PlusGlyph
import com.example.deepseekwidget.ui.components.RefreshGlyph
import com.example.deepseekwidget.ui.components.SettingsSheet
import com.example.deepseekwidget.ui.components.SlidersGlyph
import com.example.deepseekwidget.ui.components.WidgetPreviewCard
import com.example.deepseekwidget.ui.theme.DeepSeekTheme
import com.example.deepseekwidget.ui.theme.DisplayFontFamily
import com.example.deepseekwidget.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEEPSEEK_USAGE_URL = "https://platform.deepseek.com/usage"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DeepSeekTheme(themeMode = uiState.themeMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val palette = LocalPalette.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var editingKey by rememberSaveable { mutableStateOf(false) }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf("") }
    var localErrorText by remember { mutableStateOf<String?>(null) }
    val keyFocusRequester = remember { FocusRequester() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.isConfigured) {
        if (uiState.isConfigured) {
            checkAndRequestNotificationPermission()
        }
    }

    // 启动时自动检测更新
    LaunchedEffect(Unit) {
        if (uiState.autoCheckUpdateEnabled) {
            viewModel.checkForUpdates(BuildConfig.VERSION_NAME, isAuto = true)
        }
    }

    // 检测到新版本时自动弹出更新说明弹窗
    LaunchedEffect(updateState) {
        if (updateState is UpdateCheckState.HasUpdate) {
            showUpdateDialog = true
        }
    }

    LaunchedEffect(editingKey) {
        if (editingKey) {
            delay(150)
            runCatching { keyFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (uiState.isConfigured && !uiState.isLoading) {
                    viewModel.syncAllWidgets()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun submitKey() {
        if (uiState.isLoading) return
        val trimmed = keyDraft.trim()
        when {
            trimmed.isEmpty() -> localErrorText = "请输入有效的 DeepSeek API 密钥"
            trimmed.length < 12 -> localErrorText = "密钥长度过短 (少于 12 位)"
            else -> {
                localErrorText = null
                keyboard?.hide()
                viewModel.onApiKeyChanged(trimmed)
                viewModel.saveKeyAndSyncWidgets()
                editingKey = false
                keyDraft = ""
                keyVisible = false
                checkAndRequestNotificationPermission()
            }
        }
    }

    fun addWidgetToHome() {
        val pinned = viewModel.requestPinWidget(context)
        Toast.makeText(
            context,
            if (pinned) R.string.toast_widget_pin_prompt else R.string.toast_widget_pin_unavailable,
            Toast.LENGTH_SHORT
        ).show()
    }

    val glowColor = palette.accent.copy(alpha = if (palette.isDark) 0.13f else 0.055f)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(palette.canvas, palette.canvasEdge),
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        // 背景顶部右上方微弱径向氛围光
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val radius = size.width * 0.95f
                    val center = Offset(size.width * 0.88f, -size.height * 0.02f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            // 顶栏：品牌标识 + 状态指示灯 + 设置入口
            Header(
                isConfigured = uiState.isConfigured,
                onSettingsClick = {
                    showSettings = true
                    checkAndRequestNotificationPermission()
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                ) {
                    Spacer(Modifier.height(14.dp))

                    // 小组件预览区标题与尺寸切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.preview_label),
                            color = palette.secondaryInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.chipFill)
                                .border(1.dp, palette.hairline, RoundedCornerShape(8.dp))
                                .clickable { viewModel.togglePreviewSize() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.previewIs2x2) "2 × 2" else "2 × 1",
                                color = palette.accent,
                                fontFamily = DisplayFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 小组件实时所见即所得卡片
                    WidgetPreviewCard(
                        isConfigured = uiState.isConfigured,
                        balanceData = uiState.balanceData,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        is2x2 = uiState.previewIs2x2,
                        lowBalanceThreshold = uiState.lowBalanceThreshold,
                        onRefresh = { viewModel.syncAllWidgets() },
                    )

                    // 预览底部元数据与重试
                    PreviewMeta(
                        isConfigured = uiState.isConfigured,
                        errorMessage = uiState.errorMessage,
                        isLoading = uiState.isLoading,
                        onRetry = { viewModel.syncAllWidgets() },
                    )

                    Spacer(Modifier.height(28.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.hairline),
                    )
                    Spacer(Modifier.height(22.dp))

                    // API 密钥区域标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.key_section_title),
                            color = palette.primaryInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    if (uiState.isConfigured) palette.accent else palette.tertiaryInk,
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (uiState.isConfigured) R.string.key_configured
                                else R.string.key_not_configured,
                            ),
                            color = palette.tertiaryInk,
                            fontSize = 11.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // API 密钥输入 / 锁定状态卡片
                    KeySection(
                        isConfigured = uiState.isConfigured,
                        maskedKey = uiState.maskedKey,
                        keyDraft = keyDraft,
                        keyVisible = keyVisible,
                        editingKey = editingKey,
                        localErrorText = localErrorText,
                        isLoading = uiState.isLoading,
                        onKeyDraftChange = {
                            keyDraft = it
                            localErrorText = null
                        },
                        onToggleVisibility = { keyVisible = !keyVisible },
                        onEditRequest = {
                            editingKey = true
                            keyDraft = ""
                            keyVisible = false
                            localErrorText = null
                        },
                        onSubmit = ::submitKey,
                        onPaste = {
                            clipboardManager.getText()?.text?.let {
                                keyDraft = it.trim()
                                localErrorText = null
                            }
                        },
                        focusRequester = keyFocusRequester,
                    )

                    // 操作按钮组
                    if (editingKey || !uiState.isConfigured) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PrimaryActionButton(
                                text = stringResource(
                                    when {
                                        uiState.isLoading -> R.string.key_saving
                                        uiState.isConfigured -> R.string.key_save
                                        else -> R.string.key_save_sync
                                    },
                                ),
                                enabled = keyDraft.isNotBlank() && !uiState.isLoading,
                                loading = uiState.isLoading,
                                onClick = ::submitKey,
                                modifier = Modifier.weight(1f),
                            )
                            if (uiState.isConfigured) {
                                TextButton(
                                    onClick = {
                                        editingKey = false
                                        keyDraft = ""
                                        keyVisible = false
                                        localErrorText = null
                                        keyboard?.hide()
                                    },
                                    enabled = !uiState.isLoading,
                                ) {
                                    Text(
                                        text = stringResource(R.string.key_cancel),
                                        color = palette.secondaryInk,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PrimaryActionButton(
                                text = stringResource(
                                    if (uiState.isLoading) R.string.state_syncing
                                    else R.string.action_sync,
                                ),
                                enabled = !uiState.isLoading,
                                loading = uiState.isLoading,
                                leadingIcon = {
                                    if (!uiState.isLoading) {
                                        RefreshGlyph(tint = palette.onAccent, size = 15.dp)
                                    }
                                },
                                onClick = { viewModel.syncAllWidgets() },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryActionButton(
                                text = stringResource(R.string.action_add_widget),
                                onClick = ::addWidgetToHome,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // DeepSeek 开放平台极简跳转
                    Spacer(Modifier.height(8.dp))
                    DeepSeekPlatformLinkButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEEPSEEK_USAGE_URL))
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )


                }
            }
        }

        // 底部提示浮条
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = palette.surfaceHighlight,
                contentColor = palette.primaryInk,
                actionColor = palette.accent,
                shape = RoundedCornerShape(14.dp),
            )
        }
    }

    // 设置底部抽屉
    if (showSettings) {
        SettingsSheet(
            themeMode = uiState.themeMode,
            onThemeModeChange = { viewModel.setThemeMode(it) },
            hasKey = uiState.isConfigured,
            currentThreshold = uiState.lowBalanceThreshold,
            customThresholdInput = uiState.customThresholdInput,
            currentBalance = uiState.balanceData,
            currentInterval = uiState.syncIntervalMinutes,
            onThresholdSelected = { viewModel.setThreshold(it) },
            onCustomThresholdChanged = { viewModel.onCustomThresholdInputChanged(it) },
            onIntervalSelected = { viewModel.setInterval(it) },
            onDismiss = { showSettings = false },
            onRemoveKeyRequest = {
                showSettings = false
                showRemoveDialog = true
            },
            updateState = updateState,
            autoCheckUpdate = uiState.autoCheckUpdateEnabled,
            onAutoCheckUpdateChange = { viewModel.setAutoCheckUpdateEnabled(it) },
            onCheckUpdate = { viewModel.checkForUpdates(BuildConfig.VERSION_NAME) },
            onShowUpdateDialog = { showUpdateDialog = true },
        )
    }

    // 发现新版本更新弹窗
    if (showUpdateDialog && updateState is UpdateCheckState.HasUpdate) {
        UpdateDialog(
            updateInfo = (updateState as UpdateCheckState.HasUpdate).updateInfo,
            onDismiss = { showUpdateDialog = false }
        )
    }

    // 移除凭据确认弹窗
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.primaryInk,
            textContentColor = palette.secondaryInk,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.remove_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.remove_dialog_body),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        viewModel.clearKey()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.remove_confirm),
                        color = palette.warning,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(
                        text = stringResource(R.string.remove_cancel),
                        color = palette.secondaryInk,
                        fontSize = 13.sp,
                    )
                }
            },
        )
    }
}

@Composable
private fun Header(
    isConfigured: Boolean,
    onSettingsClick: () -> Unit
) {
    val palette = LocalPalette.current
    val settingsDescription = stringResource(R.string.action_settings)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    (if (isConfigured) palette.accent else palette.tertiaryInk).copy(alpha = 0.16f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isConfigured) palette.accent else palette.tertiaryInk,
                        CircleShape
                    ),
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = "DEEPSEEK",
            color = palette.primaryInk,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 2.2.sp,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(12.dp)
                .background(palette.hairline),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.header_subtitle),
            color = palette.secondaryInk,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onSettingsClick)
                .semantics {
                    contentDescription = settingsDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            SlidersGlyph(
                tint = palette.secondaryInk,
                background = palette.canvas,
                size = 18.dp,
            )
        }
    }
}

@Composable
private fun PreviewMeta(
    isConfigured: Boolean,
    errorMessage: String?,
    isLoading: Boolean,
    onRetry: () -> Unit,
) {
    val palette = LocalPalette.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currentTime = remember(isLoading) { timeFormat.format(Date()) }

    val metaText = if (!isConfigured) {
        stringResource(R.string.configure_to_start)
    } else {
        val sync = stringResource(R.string.last_sync, currentTime)
        "$sync  ·  ${stringResource(R.string.auto_sync_note)}"
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        if (errorMessage != null && isConfigured) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(palette.warning, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    modifier = Modifier.weight(1f),
                    color = palette.warning,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Text(
                    text = stringResource(R.string.action_retry),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = !isLoading,
                            role = Role.Button,
                            onClick = onRetry,
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    color = if (isLoading) palette.tertiaryInk else palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Text(
            text = metaText,
            color = palette.tertiaryInk,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun KeySection(
    isConfigured: Boolean,
    maskedKey: String?,
    keyDraft: String,
    keyVisible: Boolean,
    editingKey: Boolean,
    localErrorText: String?,
    isLoading: Boolean,
    onKeyDraftChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onEditRequest: () -> Unit,
    onSubmit: () -> Unit,
    onPaste: () -> Unit,
    focusRequester: FocusRequester,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    val keyFieldDescription = stringResource(R.string.key_section_title)
    val eyeDescription = stringResource(
        if (keyVisible) R.string.action_hide_key else R.string.action_show_key,
    )

    when {
        isConfigured && !editingKey -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(shape)
                    .background(palette.fieldFill)
                    .border(1.dp, palette.hairline, shape)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LockGlyph(tint = palette.tertiaryInk, size = 15.dp)
                Spacer(Modifier.width(11.dp))
                Text(
                    text = maskedKey ?: "••••••••",
                    color = palette.secondaryInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onEditRequest)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.key_change),
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        else -> {
            OutlinedTextField(
                value = keyDraft,
                onValueChange = onKeyDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = keyFieldDescription },
                enabled = !isLoading,
                singleLine = true,
                isError = localErrorText != null,
                shape = shape,
                textStyle = TextStyle(
                    color = palette.primaryInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp,
                ),
                placeholder = {
                    Text(
                        text = stringResource(R.string.key_placeholder),
                        color = palette.tertiaryInk,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                },
                leadingIcon = {
                    LockGlyph(tint = palette.tertiaryInk, size = 16.dp)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (keyDraft.isEmpty()) {
                            Text(
                                text = "粘贴",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onPaste() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onToggleVisibility,
                                )
                                .semantics {
                                    contentDescription = eyeDescription
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            EyeGlyph(
                                visible = keyVisible,
                                tint = palette.secondaryInk,
                                size = 18.dp,
                            )
                        }
                    }
                },
                visualTransformation = if (keyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.fieldFill,
                    unfocusedContainerColor = palette.fieldFill,
                    errorContainerColor = palette.fieldFill,
                    focusedBorderColor = palette.accent.copy(alpha = 0.75f),
                    unfocusedBorderColor = palette.hairline,
                    errorBorderColor = palette.warning.copy(alpha = 0.7f),
                    cursorColor = palette.accent,
                    focusedTextColor = palette.primaryInk,
                    unfocusedTextColor = palette.primaryInk,
                    disabledTextColor = palette.secondaryInk,
                ),
            )
        }
    }

    Spacer(Modifier.height(9.dp))
    if (localErrorText != null) {
        Text(
            text = localErrorText,
            color = palette.warning,
            fontSize = 11.sp,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LockGlyph(tint = palette.tertiaryInk, size = 11.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.key_hint),
                color = palette.tertiaryInk,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.onAccent,
            disabledContainerColor = palette.accent.copy(alpha = 0.40f),
            disabledContentColor = palette.onAccent.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = palette.onAccent,
                strokeWidth = 1.6.dp,
            )
            Spacer(Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun DeepSeekPlatformLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${stringResource(R.string.action_open_deepseek_platform)} ↗",
                color = palette.secondaryInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.hairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = palette.secondaryInk,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        PlusGlyph(tint = palette.secondaryInk, size = 14.dp)
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun formatMarkdownReleaseNotes(notes: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val lines = notes.lines()
        lines.forEachIndexed { index, rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                    val heading = trimmed.removePrefix("### ").removePrefix("## ").removePrefix("# ").trim()
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 12.5.sp)) {
                        append(heading)
                    }
                }
                else -> {
                    var i = 0
                    while (i < rawLine.length) {
                        val startBold = rawLine.indexOf("**", i)
                        if (startBold != -1) {
                            append(rawLine.substring(i, startBold))
                            val endBold = rawLine.indexOf("**", startBold + 2)
                            if (endBold != -1) {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = primaryColor)) {
                                    append(rawLine.substring(startBold + 2, endBold))
                                }
                                i = endBold + 2
                            } else {
                                append(rawLine.substring(startBold))
                                i = rawLine.length
                            }
                        } else {
                            append(rawLine.substring(i))
                            i = rawLine.length
                        }
                    }
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val formattedNotes = remember(updateInfo.releaseNotes, palette.primaryInk) {
        formatMarkdownReleaseNotes(updateInfo.releaseNotes, palette.primaryInk)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.primaryInk,
        textContentColor = palette.secondaryInk,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(palette.accent, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "发现新版本 v${updateInfo.latestVersion}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "更新内容：",
                        color = palette.primaryInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.fieldFill)
                            .border(1.dp, palette.hairline, RoundedCornerShape(10.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = formattedNotes,
                            color = palette.secondaryInk,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                } else {
                    Text(
                        text = "DeepSeek Balance 有新版本可用，建议前往下载体验最新功能与修复。",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent,
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "前往下载",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "稍后再说",
                    color = palette.secondaryInk,
                    fontSize = 13.sp,
                )
            }
        },
    )
}



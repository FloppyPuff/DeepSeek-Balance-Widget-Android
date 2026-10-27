package com.example.deepseekwidget.ui

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepseekwidget.data.BalanceCache
import com.example.deepseekwidget.data.BalanceData
import com.example.deepseekwidget.data.DeepSeekApiClient
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import com.example.deepseekwidget.work.BalanceSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.deepseekwidget.data.ThemeMode
import com.example.deepseekwidget.data.UpdateCheckState
import com.example.deepseekwidget.data.UpdateChecker
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val apiKey: String = "",
    val maskedKey: String? = null,
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val balanceData: BalanceData? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lowBalanceThreshold: Float = 5.0f,
    val customThresholdInput: String = "5",
    val syncIntervalMinutes: Long = 60L,
    val previewIs2x2: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoCheckUpdateEnabled: Boolean = true,
    val isPrivacyModeEnabled: Boolean = false,
    val isLowBalanceNotificationEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val syncRepository = WidgetSyncRepository(application)

    init {
        loadSettings()
    }

    fun loadSettings() {
        val context = getApplication<Application>()
        val key = SecureKeyStore.getApiKey(context) ?: ""
        val masked = SecureKeyStore.getMaskedApiKey(context)
        val threshold = SecureKeyStore.getLowBalanceThreshold(context)
        val interval = SecureKeyStore.getSyncIntervalMinutes(context)
        val themeMode = SecureKeyStore.getThemeMode(context)
        val autoCheckUpdate = SecureKeyStore.isAutoCheckUpdateEnabled(context)
        val privacyMode = SecureKeyStore.isPrivacyModeEnabled(context)
        val lowBalanceNotification = SecureKeyStore.isLowBalanceNotificationEnabled(context)
        val isConfigured = key.isNotBlank()
        val formattedThreshold = if (threshold % 1f == 0f) threshold.toInt().toString() else threshold.toString()

        val cached = BalanceCache.get(context)
        _uiState.update {
            it.copy(
                apiKey = key,
                maskedKey = masked,
                isConfigured = isConfigured,
                balanceData = cached.data ?: it.balanceData,
                lowBalanceThreshold = threshold,
                customThresholdInput = formattedThreshold,
                syncIntervalMinutes = interval,
                themeMode = themeMode,
                autoCheckUpdateEnabled = autoCheckUpdate,
                isPrivacyModeEnabled = privacyMode,
                isLowBalanceNotificationEnabled = lowBalanceNotification
            )
        }

        if (isConfigured) {
            syncAllWidgets()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        val context = getApplication<Application>()
        SecureKeyStore.setThemeMode(context, mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey, errorMessage = null, successMessage = null) }
    }

    fun togglePreviewSize() {
        _uiState.update { it.copy(previewIs2x2 = !it.previewIs2x2) }
    }

    fun onCustomThresholdInputChanged(input: String) {
        _uiState.update { it.copy(customThresholdInput = input) }
        val parsed = input.toFloatOrNull()
        if (parsed != null && parsed >= 0f) {
            val context = getApplication<Application>()
            SecureKeyStore.setLowBalanceThreshold(context, parsed)
            _uiState.update { it.copy(lowBalanceThreshold = parsed) }
            viewModelScope.launch {
                syncRepository.updateThreshold(parsed)
            }
        }
    }

    fun setThreshold(threshold: Float) {
        val context = getApplication<Application>()
        val formatted = if (threshold % 1f == 0f) threshold.toInt().toString() else threshold.toString()
        SecureKeyStore.setLowBalanceThreshold(context, threshold)
        _uiState.update {
            it.copy(
                lowBalanceThreshold = threshold,
                customThresholdInput = formatted
            )
        }
        viewModelScope.launch {
            syncRepository.updateThreshold(threshold)
        }
    }

    fun setInterval(minutes: Long) {
        val context = getApplication<Application>()
        SecureKeyStore.setSyncIntervalMinutes(context, minutes)
        BalanceSyncWorker.schedulePeriodicSync(context, minutes)
        _uiState.update { it.copy(syncIntervalMinutes = minutes) }
    }

    fun syncAllWidgets() {
        val key = _uiState.value.apiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入有效的 DeepSeek API Key") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val syncResult = syncRepository.syncAllWidgets()
            syncResult.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balanceData = data,
                        errorMessage = null,
                        successMessage = "同步成功：余额 ${data.currencySymbol}${data.totalBalance}，小组件已同步更新"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "同步失败: ${err.localizedMessage ?: "网络连接异常"}"
                    )
                }
            }
        }
    }

    fun saveKeyAndSyncWidgets() {
        val context = getApplication<Application>()
        val key = _uiState.value.apiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(errorMessage = "API Key 不能为空") }
            return
        }

        SecureKeyStore.saveApiKey(context, key)
        val masked = SecureKeyStore.getMaskedApiKey(context)
        _uiState.update { it.copy(isLoading = true, isConfigured = true, maskedKey = masked, errorMessage = null) }

        viewModelScope.launch {
            val syncResult = syncRepository.syncAllWidgets()
            syncResult.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balanceData = data,
                        successMessage = "凭证已加密保存 (AES256-GCM)，小组件已全量更新"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "凭证已保存，但组件同步失败: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearKey() {
        val context = getApplication<Application>()
        SecureKeyStore.clearApiKey(context)
        BalanceCache.clear(context)
        _uiState.update {
            it.copy(
                apiKey = "",
                maskedKey = null,
                isConfigured = false,
                balanceData = null,
                successMessage = "已彻底清空本地加密凭证"
            )
        }
        viewModelScope.launch {
            syncRepository.syncAllWidgets()
        }
    }

    fun setAutoCheckUpdateEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        SecureKeyStore.setAutoCheckUpdateEnabled(context, enabled)
        _uiState.update { it.copy(autoCheckUpdateEnabled = enabled) }
    }

    fun setPrivacyModeEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        SecureKeyStore.setPrivacyModeEnabled(context, enabled)
        _uiState.update { it.copy(isPrivacyModeEnabled = enabled) }
        viewModelScope.launch {
            syncRepository.syncAllWidgets()
        }
    }

    fun setLowBalanceNotificationEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        SecureKeyStore.setLowBalanceNotificationEnabled(context, enabled)
        _uiState.update { it.copy(isLowBalanceNotificationEnabled = enabled) }
    }

    fun checkForUpdates(currentVersion: String, isAuto: Boolean = false) {
        if (_updateState.value is UpdateCheckState.Checking) return
        _updateState.value = UpdateCheckState.Checking
        viewModelScope.launch {
            UpdateChecker.checkLatestRelease(currentVersion)
                .onSuccess { info ->
                    if (info.hasUpdate) {
                        _updateState.value = UpdateCheckState.HasUpdate(info)
                    } else {
                        _updateState.value = UpdateCheckState.UpToDate
                        if (!isAuto) {
                            _uiState.update { it.copy(successMessage = "当前已是最新版本 (v$currentVersion)") }
                        }
                    }
                }
                .onFailure { err ->
                    if (isAuto) {
                        _updateState.value = UpdateCheckState.Idle
                    } else {
                        _updateState.value = UpdateCheckState.Error(err.localizedMessage ?: "检查更新失败")
                        _uiState.update { it.copy(errorMessage = "检查更新失败: ${err.localizedMessage ?: "网络异常"}") }
                    }
                }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateCheckState.Idle
    }

    fun requestPinWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
            val myProvider = ComponentName(context, DeepSeekWidgetReceiver::class.java)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                val pinnedWidgetCallbackIntent = Intent(context, MainActivity::class.java)
                val successCallback = PendingIntent.getActivity(
                    context,
                    0,
                    pinnedWidgetCallbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                return appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
            }
        }
        return false
    }
}

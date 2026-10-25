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
import com.example.deepseekwidget.data.BalanceData
import com.example.deepseekwidget.data.DeepSeekApiClient
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import com.example.deepseekwidget.work.BalanceSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val apiKey: String = "",
    val isKeyVisible: Boolean = false,
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val balanceData: BalanceData? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lowBalanceThreshold: Float = 5.0f,
    val syncIntervalMinutes: Long = 60L,
    val previewDarkMode: Boolean = true,
    val previewIs2x2: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val syncRepository = WidgetSyncRepository(application)

    init {
        loadSettings()
    }

    fun loadSettings() {
        val context = getApplication<Application>()
        val key = SecureKeyStore.getApiKey(context) ?: ""
        val threshold = SecureKeyStore.getLowBalanceThreshold(context)
        val interval = SecureKeyStore.getSyncIntervalMinutes(context)
        val isConfigured = key.isNotBlank()

        _uiState.update {
            it.copy(
                apiKey = key,
                isConfigured = isConfigured,
                lowBalanceThreshold = threshold,
                syncIntervalMinutes = interval
            )
        }

        if (isConfigured) {
            testAndFetchBalance()
        }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey, errorMessage = null, successMessage = null) }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(previewDarkMode = !it.previewDarkMode) }
    }

    fun togglePreviewSize() {
        _uiState.update { it.copy(previewIs2x2 = !it.previewIs2x2) }
    }

    fun setThreshold(threshold: Float) {
        val context = getApplication<Application>()
        SecureKeyStore.setLowBalanceThreshold(context, threshold)
        _uiState.update { it.copy(lowBalanceThreshold = threshold) }
    }

    fun setInterval(minutes: Long) {
        val context = getApplication<Application>()
        SecureKeyStore.setSyncIntervalMinutes(context, minutes)
        BalanceSyncWorker.schedulePeriodicSync(context, minutes)
        _uiState.update { it.copy(syncIntervalMinutes = minutes) }
    }

    fun testAndFetchBalance() {
        val key = _uiState.value.apiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入有效的 DeepSeek API Key") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = DeepSeekApiClient.fetchBalance(key)
            result.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balanceData = data,
                        errorMessage = null,
                        successMessage = "获取成功：余额 ${data.currencySymbol}${data.totalBalance}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "连接失败，请检查网络或 Key 是否有效"
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
        _uiState.update { it.copy(isLoading = true, isConfigured = true, errorMessage = null) }

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
        _uiState.update {
            it.copy(
                apiKey = "",
                isConfigured = false,
                balanceData = null,
                successMessage = "已彻底清空本地加密凭证"
            )
        }
        viewModelScope.launch {
            syncRepository.syncAllWidgets()
        }
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

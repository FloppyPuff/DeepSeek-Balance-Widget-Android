package com.example.deepseekwidget.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.deepseekwidget.R
import com.example.deepseekwidget.data.BalanceCache
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import com.example.deepseekwidget.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 下拉控制中心快捷图块（Quick Settings Tile）
 *
 * 1. 磁贴副标实时展示当前余额（例如：¥18.50）
 * 2. 下拉后轻触快捷磁贴直接触发后台静默同步刷新，磁贴副标反馈「同步中...」-> 最新金额
 * 3. 未配置密钥时提示「未配置」，点击唤起伴生 App 进行配置
 * 4. 支持系统原生快捷长按与点击交互
 */
class DeepSeekTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (!SecureKeyStore.isConfigured(this)) {
            openMainActivity()
            return
        }

        // 磁贴状态置为同步中
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "同步中..."
        }
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()

        serviceScope.launch {
            val syncRepository = WidgetSyncRepository(applicationContext)
            val result = syncRepository.syncAllWidgets()
            result.onSuccess { data ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val isMasked = SecureKeyStore.isPrivacyModeEnabled(applicationContext)
                    tile.subtitle = if (isMasked) "${data.currencySymbol}••••" else "${data.currencySymbol}${data.totalBalance}"
                }
                tile.state = Tile.STATE_ACTIVE
                tile.updateTile()
            }.onFailure {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "同步失败"
                }
                tile.state = Tile.STATE_INACTIVE
                tile.updateTile()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.label = "DeepSeek"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_refresh_minimal)

        if (!SecureKeyStore.isConfigured(this)) {
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "未配置"
            }
        } else {
            tile.state = Tile.STATE_ACTIVE
            val cached = BalanceCache.get(this)
            val data = cached.data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val isMasked = SecureKeyStore.isPrivacyModeEnabled(this)
                if (data != null) {
                    tile.subtitle = if (isMasked) "${data.currencySymbol}••••" else "${data.currencySymbol}${data.totalBalance}"
                } else {
                    tile.subtitle = "待同步"
                }
            }
        }
        tile.updateTile()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

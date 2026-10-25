package com.example.deepseekwidget.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.deepseekwidget.data.SecureKeyStore
import com.example.deepseekwidget.data.WidgetSyncRepository
import java.util.concurrent.TimeUnit

/**
 * WorkManager 后台静默兜底同步 Worker
 *
 * 遵循 Section 5.3 后台保活与兜底策略：
 * 周期性在满足联网约束时自动同步，更新全部桌面的小组件实例，确保用户解锁屏幕所见即准实时余额。
 */
class BalanceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = WidgetSyncRepository(applicationContext)
        val syncResult = repository.syncAllWidgets()

        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            // 网络异常或暂时无法连通时，指示系统稍后重试
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "deepseek_balance_periodic_sync"

        /**
         * 启动或更新后台定期同步任务
         * @param intervalMinutes 同步间隔（分钟，WorkManager 限制最小为 15 分钟）
         */
        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 60L) {
            val validInterval = intervalMinutes.coerceAtLeast(15L)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BalanceSyncWorker>(
                validInterval, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // flex interval
            )
                .setConstraints(constraints)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_TAG)
        }
    }
}

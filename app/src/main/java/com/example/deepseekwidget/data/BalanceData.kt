package com.example.deepseekwidget.data

/**
 * DeepSeek API 余额数据实体
 */
data class BalanceData(
    val isAvailable: Boolean,
    val currencySymbol: String,
    val totalBalance: String,
    val grantedBalance: String,
    val toppedUpBalance: String
) {
    val totalBalanceNumber: Double
        get() = totalBalance.toDoubleOrNull() ?: 0.0

    fun isLowBalance(threshold: Double = 5.0): Boolean {
        return totalBalanceNumber < threshold
    }
}

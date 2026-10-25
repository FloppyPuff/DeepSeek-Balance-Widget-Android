package com.example.deepseekwidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API 客户端
 * 基于协程与 OkHttp，无第三方重量级网络库依赖
 */
object DeepSeekApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchBalance(apiKey: String): Result<BalanceData> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedKey = apiKey.trim()
            require(trimmedKey.isNotBlank()) { "API Key cannot be blank" }

            val request = Request.Builder()
                .url("https://api.deepseek.com/user/balance")
                .addHeader("Authorization", "Bearer $trimmedKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "HTTP 401: Invalid API Key"
                        402 -> "HTTP 402: Insufficient Balance"
                        429 -> "HTTP 429: Rate Limit Exceeded"
                        500, 502, 503 -> "HTTP ${response.code}: DeepSeek Server Error"
                        else -> "HTTP ${response.code}: Request Failed"
                    }
                    throw IllegalStateException(errorMsg)
                }

                val bodyStr = response.body?.string() ?: throw IllegalStateException("Empty body")
                parseBalanceJson(bodyStr)
            }
        }
    }

    /**
     * 解析 DeepSeek 余额 JSON
     */
    fun parseBalanceJson(jsonStr: String): BalanceData {
        val json = JSONObject(jsonStr)
        val isAvailable = json.optBoolean("is_available", false)
        val infos = json.optJSONArray("balance_infos")
            ?: throw IllegalStateException("Missing 'balance_infos' in response")

        if (infos.length() == 0) {
            return BalanceData(
                isAvailable = isAvailable,
                currencySymbol = "¥",
                totalBalance = "0.00",
                grantedBalance = "0.00",
                toppedUpBalance = "0.00"
            )
        }

        // 优先选取余额大于 0 的币种，默认回退至第一个元素
        var selectedObj = infos.getJSONObject(0)
        for (i in 0 until infos.length()) {
            val item = infos.getJSONObject(i)
            val total = item.optString("total_balance").toDoubleOrNull() ?: 0.0
            if (total > 0.0) {
                selectedObj = item
                break
            }
        }

        val currency = selectedObj.optString("currency", "CNY")
        return BalanceData(
            isAvailable = isAvailable,
            currencySymbol = if (currency.equals("USD", ignoreCase = true)) "$" else "¥",
            totalBalance = selectedObj.optString("total_balance", "0.00"),
            grantedBalance = selectedObj.optString("granted_balance", "0.00"),
            toppedUpBalance = selectedObj.optString("topped_up_balance", "0.00")
        )
    }
}

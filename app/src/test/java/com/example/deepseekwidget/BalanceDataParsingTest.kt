package com.example.deepseekwidget

import com.example.deepseekwidget.data.DeepSeekApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceDataParsingTest {

    @Test
    fun testParseNormalResponse() {
        val sampleJson = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "CNY",
                        "total_balance": "128.45",
                        "granted_balance": "10.00",
                        "topped_up_balance": "118.45"
                    }
                ]
            }
        """.trimIndent()

        val data = DeepSeekApiClient.parseBalanceJson(sampleJson)
        assertTrue(data.isAvailable)
        assertEquals("¥", data.currencySymbol)
        assertEquals("128.45", data.totalBalance)
        assertEquals("10.00", data.grantedBalance)
        assertEquals("118.45", data.toppedUpBalance)
        assertFalse(data.isLowBalance(5.0))
    }

    @Test
    fun testPriorityToPositiveBalanceCurrency() {
        val multiCurrencyJson = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "USD",
                        "total_balance": "0.00",
                        "granted_balance": "0.00",
                        "topped_up_balance": "0.00"
                    },
                    {
                        "currency": "CNY",
                        "total_balance": "66.88",
                        "granted_balance": "5.00",
                        "topped_up_balance": "61.88"
                    }
                ]
            }
        """.trimIndent()

        val data = DeepSeekApiClient.parseBalanceJson(multiCurrencyJson)
        assertEquals("¥", data.currencySymbol)
        assertEquals("66.88", data.totalBalance)
        assertEquals("5.00", data.grantedBalance)
        assertEquals("61.88", data.toppedUpBalance)
    }

    @Test
    fun testUsdCurrencySymbol() {
        val usdJson = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "USD",
                        "total_balance": "25.50",
                        "granted_balance": "0.00",
                        "topped_up_balance": "25.50"
                    }
                ]
            }
        """.trimIndent()

        val data = DeepSeekApiClient.parseBalanceJson(usdJson)
        assertEquals("$", data.currencySymbol)
        assertEquals("25.50", data.totalBalance)
    }

    @Test
    fun testLowBalanceDetection() {
        val lowJson = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "CNY",
                        "total_balance": "2.40",
                        "granted_balance": "0.00",
                        "topped_up_balance": "2.40"
                    }
                ]
            }
        """.trimIndent()

        val data = DeepSeekApiClient.parseBalanceJson(lowJson)
        assertTrue(data.isLowBalance(5.0))
        assertFalse(data.isLowBalance(2.0))
    }
}

package com.example.deepseekwidget

import com.example.deepseekwidget.data.BalanceData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThresholdTest {

    @Test
    fun testCustomThresholdComparison() {
        val balance = BalanceData(
            isAvailable = true,
            currencySymbol = "¥",
            totalBalance = "2.43",
            grantedBalance = "0.00",
            toppedUpBalance = "2.43"
        )

        // Threshold set to 5.0 -> 2.43 < 5.0 -> isLowBalance should be true
        assertTrue(balance.isLowBalance(5.0))

        // Threshold set to 2.0 -> 2.43 >= 2.0 -> isLowBalance should be false
        assertFalse(balance.isLowBalance(2.0))

        // Threshold set to custom float 2.44 -> 2.43 < 2.44 -> true
        assertTrue(balance.isLowBalance(2.44))

        // Threshold set to custom float 2.42 -> 2.43 >= 2.42 -> false
        assertFalse(balance.isLowBalance(2.42))
    }

    @Test
    fun testCustomInputParsing() {
        val validInputs = listOf("2.5", "10", "0.88", "100.00")
        validInputs.forEach { input ->
            val parsed = input.toFloatOrNull()
            assertTrue(parsed != null && parsed >= 0f)
        }

        val invalidInputs = listOf("-5", "abc", "", "   ")
        invalidInputs.forEach { input ->
            val parsed = input.toFloatOrNull()
            val isValid = parsed != null && parsed >= 0f
            assertFalse(isValid)
        }
    }
}

package com.example.deepseekwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyNumberFormatTest {

    private fun splitBalance(balanceStr: String): Pair<String, String> {
        val parts = balanceStr.split(".")
        val intPart = parts.getOrNull(0)?.ifEmpty { "0" } ?: "0"
        val decPart = if (parts.size > 1) ".${parts[1]}" else ".00"
        return Pair(intPart, decPart)
    }

    @Test
    fun testStandardBalanceSplit() {
        val (integer, decimal) = splitBalance("128.45")
        assertEquals("128", integer)
        assertEquals(".45", decimal)
    }

    @Test
    fun testZeroBalanceSplit() {
        val (integer, decimal) = splitBalance("0.00")
        assertEquals("0", integer)
        assertEquals(".00", decimal)
    }

    @Test
    fun testNoDecimalBalanceSplit() {
        val (integer, decimal) = splitBalance("500")
        assertEquals("500", integer)
        assertEquals(".00", decimal)
    }

    @Test
    fun testLeadingDecimalSplit() {
        val (integer, decimal) = splitBalance(".88")
        assertEquals("0", integer)
        assertEquals(".88", decimal)
    }
}

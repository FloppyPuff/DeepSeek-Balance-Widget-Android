package com.example.deepseekwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyNumberFormatTest {

    private fun splitBalance(balanceStr: String): Pair<String, String> {
        val isMasked = balanceStr.contains("•") || balanceStr.contains("*")
        val parts = balanceStr.split(".")
        val intPart = parts.getOrNull(0)?.ifEmpty { if (isMasked) balanceStr else "0" } ?: if (isMasked) balanceStr else "0"
        val decPart = if (parts.size > 1) {
            ".${parts[1]}"
        } else if (isMasked || balanceStr == "—" || balanceStr == "--" || balanceStr == "--.--") {
            ""
        } else {
            ".00"
        }
        return Pair(intPart, decPart)
    }

    @Test
    fun testMaskedDotsBalanceSplit() {
        val (integer, decimal) = splitBalance("••••")
        assertEquals("••••", integer)
        assertEquals("", decimal)
    }

    @Test
    fun testMaskedAsterisksBalanceSplit() {
        val (integer, decimal) = splitBalance("***.**")
        assertEquals("***", integer)
        assertEquals(".**", decimal)
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

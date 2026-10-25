package com.example.deepseekwidget

import com.example.deepseekwidget.data.WidgetSyncRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebounceLogicTest {

    private fun shouldAllowRefresh(isLoading: Boolean, lastRefreshTimestamp: Long, currentTimestamp: Long): Boolean {
        if (isLoading) return false
        if (currentTimestamp - lastRefreshTimestamp < WidgetSyncRepository.MIN_REFRESH_INTERVAL_MS) {
            return false
        }
        return true
    }

    @Test
    fun testRefreshAllowedInitially() {
        val allowed = shouldAllowRefresh(isLoading = false, lastRefreshTimestamp = 0L, currentTimestamp = 100000L)
        assertTrue(allowed)
    }

    @Test
    fun testRefreshBlockedWhenLoading() {
        val allowed = shouldAllowRefresh(isLoading = true, lastRefreshTimestamp = 0L, currentTimestamp = 100000L)
        assertFalse(allowed)
    }

    @Test
    fun testRefreshBlockedWithinDebounceWindow() {
        val lastTime = 100000L
        val rapidClickTime = 101500L // 1.5s later, less than 3s
        val allowed = shouldAllowRefresh(isLoading = false, lastRefreshTimestamp = lastTime, currentTimestamp = rapidClickTime)
        assertFalse(allowed)
    }

    @Test
    fun testRefreshAllowedAfterDebounceWindow() {
        val lastTime = 100000L
        val nextClickTime = 103500L // 3.5s later, greater than 3s
        val allowed = shouldAllowRefresh(isLoading = false, lastRefreshTimestamp = lastTime, currentTimestamp = nextClickTime)
        assertTrue(allowed)
    }
}

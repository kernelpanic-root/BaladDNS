package com.kernelpanic.baladdns.data.nextdns.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AnalyticsScopeTest {
    @Test
    fun `wire values cover all dashboard periods`() {
        assertEquals(
            listOf("-30m", "-6h", "-24h", "-7d", "-30d", "-3M"),
            AnalyticsPeriod.entries.map(AnalyticsPeriod::wireValue),
        )
    }

    @Test
    fun `device is part of cache scope identity`() {
        val allDevices = AnalyticsScope(AnalyticsPeriod.Days30, null)
        val oneDevice = AnalyticsScope(AnalyticsPeriod.Days30, "device-id")

        assertNotEquals(allDevices, oneDevice)
        assertEquals(allDevices, allDevices.copy())
    }
}

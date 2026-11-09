package org.mz.botanicalsmock

import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopWindowSizingTest {
    @Test fun largeMonitorsKeepTheStoreWindowWithinTheCompanyMaximums() {
        val size = desktopWindowSizing(3840, 2100)
        assertTrue(size.width in 840..1440)
        assertTrue(size.height in 600..1000)
        assertTrue(size.minimumWidth <= size.width)
        assertTrue(size.minimumHeight <= size.height)
    }

    @Test fun smallAndScaledWorkAreasKeepTheWindowAndMinimumOnScreen() {
        for ((width, height) in listOf(1280 to 680, 1024 to 568, 640 to 440, 320 to 240)) {
            val size = desktopWindowSizing(width, height)
            assertTrue(size.width in 1..width)
            assertTrue(size.height in 1..height)
            assertTrue(size.minimumWidth in 1..size.width)
            assertTrue(size.minimumHeight in 1..size.height)
        }
    }

    @Test fun unavailableScreenMetricsUseUsableFallbackDimensions() {
        val size = desktopWindowSizing(0, -1)
        assertTrue(size.width >= 480)
        assertTrue(size.height >= 400)
    }
}

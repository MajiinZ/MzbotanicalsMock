package org.mz.botanicalsmock

import kotlin.math.roundToInt

internal data class DesktopWindowSizing(
    val width: Int,
    val height: Int,
    val minimumWidth: Int,
    val minimumHeight: Int,
)

/** Company window sizing in AWT logical units, respecting the current work area. */
internal fun desktopWindowSizing(availableWidth: Int, availableHeight: Int): DesktopWindowSizing {
    val usableWidth = availableWidth.takeIf { it > 0 } ?: 1280
    val usableHeight = availableHeight.takeIf { it > 0 } ?: 800
    val width = minOf(1280, (usableWidth * 0.92).roundToInt()).coerceAtLeast(minOf(480, usableWidth))
    val height = minOf(820, (usableHeight * 0.92).roundToInt()).coerceAtLeast(minOf(400, usableHeight))
    return DesktopWindowSizing(width, height, minOf(480, width), minOf(560, height))
}

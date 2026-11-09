package org.mz.botanicalsmock

import androidx.compose.runtime.Composable

/** Match system icon contrast to the mock's current branded surface. */
@Composable
expect fun MockSystemBars(darkBackground: Boolean)

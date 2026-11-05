package org.mz.botanicalsmock
import androidx.compose.runtime.Composable
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

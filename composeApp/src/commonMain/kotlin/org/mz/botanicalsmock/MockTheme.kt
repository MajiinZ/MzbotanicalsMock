package org.mz.botanicalsmock

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun MockTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = BrandGreen,
        onPrimary = BrandPaper,
        primaryContainer = BrandGreenSoft,
        onPrimaryContainer = BrandDeep,
        secondary = BrandGreen,
        onSecondary = BrandPaper,
        secondaryContainer = BrandGreenSoft,
        onSecondaryContainer = BrandDeep,
        background = BrandCream,
        onBackground = BrandDeep,
        surface = BrandPaper,
        onSurface = BrandDeep,
        surfaceVariant = BrandGreenSoft,
        onSurfaceVariant = BrandTextMuted,
        outline = BrandTextMuted,
        outlineVariant = BrandOutline,
        error = Color(0xFFB3261E),
        onError = BrandPaper,
        surfaceTint = BrandGreen,
    )
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = BrandDisplayFont, fontSize = 52.sp, lineHeight = 58.sp),
        displayMedium = TextStyle(fontFamily = BrandDisplayFont, fontSize = 44.sp, lineHeight = 50.sp),
        displaySmall = TextStyle(fontFamily = BrandDisplayFont, fontSize = 36.sp, lineHeight = 42.sp),
        headlineLarge = TextStyle(fontFamily = BrandDisplayFont, fontSize = 32.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = BrandDisplayFont, fontSize = 28.sp, lineHeight = 34.sp),
        headlineSmall = TextStyle(fontFamily = BrandDisplayFont, fontSize = 24.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = BrandBodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = BrandBodyFont, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = BrandBodyFont, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = BrandBodyFont, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = BrandBodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = BrandBodyFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

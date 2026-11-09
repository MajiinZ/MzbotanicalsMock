package org.mz.botanicalsmock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.mz.botanicalsmock.generated.resources.Res
import org.mz.botanicalsmock.generated.resources.back_arrow
import org.mz.botanicalsmock.generated.resources.close
import org.mz.botanicalsmock.generated.resources.exo2_regular
import org.mz.botanicalsmock.generated.resources.hamburger_menu
import org.mz.botanicalsmock.generated.resources.home
import org.mz.botanicalsmock.generated.resources.leaf
import org.mz.botanicalsmock.generated.resources.manage_account
import org.mz.botanicalsmock.generated.resources.mz_brand
import org.mz.botanicalsmock.generated.resources.mz_welcome
import org.mz.botanicalsmock.generated.resources.payments
import org.mz.botanicalsmock.generated.resources.profile
import org.mz.botanicalsmock.generated.resources.settings
import org.mz.botanicalsmock.generated.resources.shopping_cart
import org.mz.botanicalsmock.model.formatMoney

// Keep these values aligned with the company application's shared/Colors.kt.
val BrandGreen = Color(0xFF124735)
val BrandCream = Color(0xFFF7F5EC)
val BrandGreenSoft = Color(0xFFE2ECD5)
val BrandTextMuted = Color(0xFF59665C)
val BrandDeep = Color(0xFF0B3428)
val BrandPaper = Color(0xFFFFFDF7)
val BrandOutline = Color(0xFFD8DECF)
val BrandGold = Color(0xFF9A682E)
val BrandDisplayFont = FontFamily.Serif
val BrandBodyFont = FontFamily.SansSerif

@Composable
fun Exo2FontRegular(): FontFamily = FontFamily(Font(Res.font.exo2_regular))

enum class CompanyIcon { HOME, EXPLORE, CART, PROFILE, MENU, ORDERS, SETTINGS, ADMIN, BACK, CLOSE }

/** The same bundled vectors used by the company header, drawer and bottom bar. */
@Composable
fun CompanyIcon(
    icon: CompanyIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = BrandGreen,
) {
    val resource = when (icon) {
        CompanyIcon.HOME -> Res.drawable.home
        CompanyIcon.EXPLORE -> Res.drawable.leaf
        CompanyIcon.CART -> Res.drawable.shopping_cart
        CompanyIcon.PROFILE -> Res.drawable.profile
        CompanyIcon.MENU -> Res.drawable.hamburger_menu
        CompanyIcon.ORDERS -> Res.drawable.payments
        CompanyIcon.SETTINGS -> Res.drawable.settings
        CompanyIcon.ADMIN -> Res.drawable.manage_account
        CompanyIcon.BACK -> Res.drawable.back_arrow
        CompanyIcon.CLOSE -> Res.drawable.close
    }
    MaterialIcon(painterResource(resource), contentDescription, modifier, tint)
}

@Composable
fun CompanyBrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.mz_brand),
        contentDescription = "MZ Botanicals",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun CompanyWelcomeArtwork(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(Res.drawable.mz_welcome),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Composable
fun DemoHeading(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = BrandDeep)
        if (subtitle.isNotBlank()) Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BrandTextMuted,
        )
    }
}

@Composable
fun DemoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = BrandPaper,
        border = BorderStroke(1.dp, BrandOutline),
    ) {
        Column(
            Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun DemoMessage(message: String) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = BrandGreenSoft,
    ) {
        Text(
            message,
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = BrandDeep,
        )
    }
}

/** Mirrors the company PrimaryButton's size, shape, type and disabled state. */
@Composable
fun DemoPrimary(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandDeep,
            contentColor = BrandPaper,
            disabledContainerColor = BrandGreenSoft,
            disabledContentColor = BrandTextMuted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
    ) {
        Text(text, fontFamily = BrandBodyFont, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DemoMoneyRow(label: String, cents: Long, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium
        Text(label, Modifier.weight(1f), style = style, color = if (emphasized) BrandDeep else BrandTextMuted)
        Text(formatMoney(cents), style = style, color = if (emphasized) BrandDeep else BrandGreen)
    }
}

/** Match the company ProductCard image fallback without fetching live images. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DemoArt(productId: String, modifier: Modifier = Modifier) {
    Box(modifier.background(BrandGreenSoft)) {
        Image(
            painter = painterResource(Res.drawable.mz_brand),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

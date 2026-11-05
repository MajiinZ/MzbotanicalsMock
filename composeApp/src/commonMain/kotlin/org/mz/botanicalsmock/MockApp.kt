package org.mz.botanicalsmock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mz.botanicalsmock.model.DemoStore
import org.mz.botanicalsmock.model.formatMoney

val BrandGreen = Color(0xFF124735)
val BrandCream = Color(0xFFF7F5EC)
val BrandGreenSoft = Color(0xFFE2ECD5)
val BrandTextMuted = Color(0xFF59665C)
val BrandDeep = Color(0xFF0B3428)
val BrandPaper = Color(0xFFFFFDF7)
val BrandOutline = Color(0xFFD8DECF)
val BrandDisplayFont = FontFamily.Serif
val BrandBodyFont = FontFamily.SansSerif

private enum class DemoPage { Shop, Cart, Checkout, Orders, Order, Product, Admin, Profile }

@Composable
fun MockApp(store: DemoStore) {
    val state by store.state.collectAsState()
    var page by remember { mutableStateOf(DemoPage.Shop) }
    var productId by remember { mutableStateOf("") }
    var orderId by remember { mutableStateOf("") }
    var orderBack by remember { mutableStateOf(DemoPage.Orders) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf(store.loadError) }
    val tabs = listOf(DemoPage.Shop, DemoPage.Cart, DemoPage.Orders, DemoPage.Admin, DemoPage.Profile)
    val icons = listOf(Icons.Outlined.LocalFlorist, Icons.Outlined.ShoppingBag, Icons.Outlined.ReceiptLong, Icons.Outlined.Dashboard, Icons.Outlined.Person)
    PlatformBackHandler(page != DemoPage.Shop) {
        page = when (page) {
            DemoPage.Checkout -> DemoPage.Cart
            DemoPage.Product -> DemoPage.Shop
            DemoPage.Order -> orderBack
            else -> DemoPage.Shop
        }
        notice = null
    }
    MockTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            containerColor = BrandCream,
            topBar = {
                Column(Modifier.fillMaxWidth()) {
                    Surface(color = BrandDeep, contentColor = BrandCream) {
                        Text("OFFLINE DEMO · No real orders or payments", Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("MZ Botanicals", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Surface(shape = RoundedCornerShape(50), color = BrandGreenSoft) {
                            Text("Mock", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = BrandPaper, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = page == tab || (tab == DemoPage.Cart && page == DemoPage.Checkout) || (tab == DemoPage.Shop && page == DemoPage.Product) || (tab == DemoPage.Orders && page == DemoPage.Order)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { page = tab; notice = null },
                            icon = {
                                if (tab == DemoPage.Cart && state.cart.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(state.cart.sumOf { it.quantity }.coerceAtMost(99).toString()) } }) { Icon(icons[index], null) }
                                } else Icon(icons[index], null)
                            },
                            label = { Text(tab.name, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 1400.dp).fillMaxSize()) {
                    notice?.let { DemoMessage(it) }
                    key(page, productId, orderId) {
                        when (page) {
                            DemoPage.Shop -> ShopScreen(state, store, { productId = it; page = DemoPage.Product }, { page = DemoPage.Cart })
                            DemoPage.Product -> ProductScreen(productId, state, store, { page = DemoPage.Shop }, { page = DemoPage.Cart })
                            DemoPage.Cart -> CartScreen(state, store, { page = DemoPage.Shop }, { page = DemoPage.Checkout })
                            DemoPage.Checkout -> CheckoutScreen(state, store, { page = DemoPage.Cart }) {
                                orderId = it; orderBack = DemoPage.Orders; page = DemoPage.Order
                                notice = "Demo order placed. No real payment was processed."
                            }
                            DemoPage.Orders -> OrdersScreen(state, { orderId = it; orderBack = DemoPage.Orders; page = DemoPage.Order }, { page = DemoPage.Shop })
                            DemoPage.Order -> OrderScreen(orderId, state, store, { page = orderBack; notice = null })
                            DemoPage.Admin -> AdminScreen(state, store, { orderId = it; orderBack = DemoPage.Admin; page = DemoPage.Order })
                            DemoPage.Profile -> ProfileScreen(state, store)
                        }
                    }
                }
            }
        }
        loadError?.let { error ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Saved demo needs attention") },
                text = { Text("$error\n\nReset replaces local demo data with fictional examples. The company app is unaffected.") },
                confirmButton = {
                    TextButton(onClick = {
                        val result = store.resetDemo()
                        if (result == null) { loadError = null; page = DemoPage.Shop } else loadError = result
                    }) { Text("Reset demo data") }
                }
            )
        }
    }
}

@Composable
fun DemoHeading(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DemoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = BrandPaper, border = androidx.compose.foundation.BorderStroke(1.dp, BrandOutline)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun DemoMessage(message: String) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = BrandGreenSoft) {
        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = BrandDeep)
    }
}

@Composable
fun DemoPrimary(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(14.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DemoMoneyRow(label: String, cents: Long, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        Text(label, Modifier.weight(1f), style = style)
        Text(formatMoney(cents), style = style)
    }
}

/** Native artwork stays available without downloading photos or contacting a real store. */
@Composable
fun DemoArt(productId: String, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFFDEE8D6), Color(0xFFE6DBCD), Color(0xFFD9E4D7), Color(0xFFE4DFEC), Color(0xFFE9E0C5), Color(0xFFD3E5DF))
    val index = (productId.hashCode().toLong().let { if (it < 0) -it else it } % colors.size).toInt()
    Canvas(modifier) {
        drawRoundRect(colors[index], cornerRadius = CornerRadius(20.dp.toPx()))
        val w = size.width
        val h = size.height
        drawCircle(BrandPaper.copy(alpha = 0.5f), minOf(w, h) * 0.44f, Offset(w * 0.72f, h * 0.2f))
        val pw = minOf(w * 0.46f, h * 0.62f)
        val ph = h * 0.78f
        val left = (w - pw) / 2
        val top = (h - ph) / 2
        drawRoundRect(BrandPaper, Offset(left, top), Size(pw, ph), CornerRadius(10.dp.toPx()))
        drawRoundRect(BrandGreen, Offset(left + pw * 0.12f, top + ph * 0.12f), Size(pw * 0.76f, ph * 0.7f), CornerRadius(5.dp.toPx()))
        val stemX = w / 2
        drawLine(BrandGreenSoft, Offset(stemX, top + ph * 0.68f), Offset(stemX, top + ph * 0.27f), strokeWidth = 3.dp.toPx())
        for (side in listOf(-1, 1)) {
            for (row in 0..2) {
                val y = top + ph * (0.38f + row * 0.105f)
                val leaf = Path().apply {
                    moveTo(stemX, y + ph * 0.08f)
                    quadraticBezierTo(stemX + side * pw * 0.36f, y + ph * 0.09f, stemX + side * pw * 0.26f, y - ph * 0.08f)
                    quadraticBezierTo(stemX + side * pw * 0.04f, y - ph * 0.05f, stemX, y + ph * 0.08f)
                    close()
                }
                drawPath(leaf, BrandGreenSoft)
            }
        }
        drawLine(BrandOutline, Offset(left + pw * 0.2f, top + ph * 0.9f), Offset(left + pw * 0.8f, top + ph * 0.9f), 2.dp.toPx())
        drawCircle(BrandGreen.copy(alpha = 0.4f), 4.dp.toPx(), Offset(w * 0.18f, h * 0.78f))
        drawCircle(BrandGreen.copy(alpha = 0.3f), 3.dp.toPx(), Offset(w * 0.82f, h * 0.72f))
    }
}

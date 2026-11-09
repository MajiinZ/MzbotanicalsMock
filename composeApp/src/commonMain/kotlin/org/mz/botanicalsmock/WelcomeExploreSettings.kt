package org.mz.botanicalsmock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mz.botanicalsmock.model.DemoState

@Composable
internal fun MockWelcomeScreen(onEnter: () -> Unit) {
    MockSystemBars(darkBackground = true)
    BoxWithConstraints(Modifier.fillMaxSize().background(BrandDeep).safeDrawingPadding()) {
        val compact = maxHeight < 480.dp
        val panel = if (compact) Modifier.width((maxWidth * 0.55f).coerceAtMost(480.dp))
            else Modifier.widthIn(max = 480.dp).fillMaxWidth()
        CompanyWelcomeArtwork(
            if (compact) Modifier.fillMaxHeight().fillMaxWidth(0.45f) else Modifier.fillMaxSize(),
            contentScale = if (compact) ContentScale.Fit else ContentScale.Crop
        )
        Column(
            Modifier.align(if (compact) Alignment.CenterEnd else Alignment.BottomCenter)
                .then(panel).heightIn(max = maxHeight).padding(24.dp)
                .background(BrandDeep.copy(alpha = 0.94f), RoundedCornerShape(28.dp))
                .border(1.dp, BrandCream.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("THE GENETICS COLLECTION", color = BrandCream.copy(alpha = 0.8f),
                fontFamily = BrandBodyFont, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Text("MZ Botanicals", Modifier.fillMaxWidth(), color = BrandCream,
                fontFamily = BrandDisplayFont, fontSize = 36.sp, lineHeight = 42.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Explore the genetics collection.", Modifier.fillMaxWidth(), color = BrandCream.copy(alpha = 0.9f),
                fontFamily = BrandBodyFont, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onEnter, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandCream, contentColor = BrandDeep)) {
                Text("Enter demo store", fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Text("Offline demo · No real orders or payments", color = BrandCream.copy(alpha = 0.88f),
                fontFamily = BrandBodyFont, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun ExploreScreen(state: DemoState, onCategory: (String) -> Unit) {
    val categories = state.products.filter { it.published }.map { it.category }.distinct().sorted()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(Modifier.fillMaxSize().background(BrandCream), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 1000.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 28.dp)) {
            item {
                Text("THE COLLECTION", color = BrandTextMuted, fontFamily = Exo2FontRegular(), fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 3.2.sp)
                Spacer(Modifier.height(18.dp))
                Text("Find your\nnext favorite.", color = BrandGreen, fontFamily = BrandDisplayFont,
                    fontSize = 43.sp, lineHeight = 45.sp)
                Spacer(Modifier.height(14.dp))
                Text("Explore genetics, your way.", color = BrandTextMuted, fontFamily = Exo2FontRegular(), fontSize = 16.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(24.dp))
                if (categories.isEmpty()) DemoCard {
                    Text("The collection is being prepared", style = MaterialTheme.typography.titleMedium)
                    Text("Publish a sample product in the admin catalog to see its collection here.")
                }
            }
            itemsIndexed(categories, key = { _, category -> category }) { index, category ->
                val colors = when (index % 3) {
                    0 -> listOf(Color(0xFF0B6248), Color(0xFF197052))
                    1 -> listOf(Color(0xFF416A53), Color(0xFF5A7C65))
                    else -> listOf(Color(0xFF24574B), Color(0xFF386658))
                }
                Box(Modifier.fillMaxWidth().heightIn(min = 138.dp + 60.dp * (fontScale - 1f))
                    .clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(colors))
                    .clickable(role = Role.Button, onClickLabel = "Explore $category", onClick = { onCategory(category) })
                    .padding(horizontal = 26.dp, vertical = 20.dp)) {
                    CategoryLeaf(Modifier.matchParentSize(), index)
                    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        Text(category, color = BrandCream, fontFamily = BrandDisplayFont, fontSize = 31.sp, lineHeight = 36.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Explore collection", color = BrandCream, fontFamily = Exo2FontRegular(), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("↗", Modifier.padding(start = 10.dp), color = BrandCream, fontSize = 20.sp)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun CategoryLeaf(modifier: Modifier, variant: Int) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val baseX = w * if (variant % 3 == 1) 0.68f else 0.76f
        val baseY = h * 1.1f
        val tipX = w * if (variant % 3 == 2) 0.93f else 1.02f
        val tipY = -h * 0.12f
        val leaf = Path().apply {
            moveTo(baseX, baseY)
            cubicTo(w * 0.66f, h * 0.56f, w * 0.76f, h * 0.12f, tipX, tipY)
            cubicTo(w * 1.08f, h * 0.35f, w * 1.06f, h * 0.82f, baseX, baseY)
            close()
        }
        drawPath(leaf, Color.White.copy(alpha = 0.035f))
        drawPath(leaf, Color.White.copy(alpha = 0.13f), style = Stroke(1.5.dp.toPx()))
        drawLine(Color.White.copy(alpha = 0.13f), Offset(baseX, baseY), Offset(tipX, tipY), 1.25.dp.toPx())
    }
}

@Composable
internal fun DemoSettingsScreen(onProfile: () -> Unit, onOrders: () -> Unit, onWelcome: () -> Unit) {
    var policy by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DemoHeading("Settings", "Your profile, orders and store information.")
            DemoCard {
                Text("Your account", style = MaterialTheme.typography.titleMedium)
                Text("This demonstration uses a fictional profile. Your changes stay on this device.")
                TextButton(onClick = onProfile, modifier = Modifier.heightIn(min = 48.dp)) { Text("View profile") }
                TextButton(onClick = onOrders, modifier = Modifier.heightIn(min = 48.dp)) { Text("View orders") }
            }
            Surface(color = BrandGreenSoft, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Support and policies", style = MaterialTheme.typography.titleMedium)
                    Text("MZ Botanicals demo store")
                    Text("Preview the policy screens below. They are examples for presentations.")
                    listOf("Privacy policy", "Terms", "Refund policy").forEach { title ->
                        TextButton(onClick = { policy = title }, modifier = Modifier.heightIn(min = 48.dp)) { Text(title) }
                    }
                }
            }
            DemoCard {
                Text("Demo checkout", style = MaterialTheme.typography.titleMedium)
                Text("Payments, refunds and tracking are simulated. No money is collected and no products are shipped.")
                Text("Sample shipping: $5.00 · Sample tax: 8% of merchandise. These are fictional amounts.")
            }
            DemoCard {
                Text("About this app", style = MaterialTheme.typography.titleMedium)
                Text("mzbotanicalMock · Version 1.0.1")
                Text("The same MZ Botanicals presentation with fictional, offline store activity. Reset the sample data from your profile when you finish a presentation.")
                OutlinedButton(onClick = onWelcome, modifier = Modifier.heightIn(min = 48.dp)) { Text("Return to welcome") }
            }
        }
    }
    policy?.let { title ->
        AlertDialog(onDismissRequest = { policy = null }, containerColor = BrandCream,
            title = { Text(title, color = BrandGreen) },
            text = {
                Text(when (title) {
                    "Privacy policy" -> "Demonstration only. Your fictional profile, cart and orders are saved on this device. No information is sent to a store or payment provider. Use Reset demo data in Profile to restore the sample records."
                    "Refund policy" -> "Demonstration only. Eligible sample orders can be cancelled or refunded from the admin workspace. Those actions update fictional order status and sample inventory; they do not move money."
                    else -> "Demonstration only. All products, customers and orders shown in this app are fictional. Checkout does not create a real purchase, payment or shipment. These example terms are not a merchant agreement."
                })
            },
            confirmButton = { TextButton(onClick = { policy = null }) { Text("Close") } })
    }
}

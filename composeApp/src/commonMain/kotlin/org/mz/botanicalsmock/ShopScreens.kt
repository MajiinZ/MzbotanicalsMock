package org.mz.botanicalsmock

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mz.botanicalsmock.model.*
import kotlin.random.Random

@Composable
fun CheckoutScreen(
    state: DemoState,
    store: DemoStore,
    onBack: () -> Unit,
    onPlaced: (String) -> Unit,
) {
    var name by remember { mutableStateOf(state.profile.name) }
    var email by remember { mutableStateOf(state.profile.email) }
    var line1 by remember { mutableStateOf(state.profile.line1) }
    var city by remember { mutableStateOf(state.profile.city) }
    var region by remember { mutableStateOf(state.profile.state) }
    var postalCode by remember { mutableStateOf(state.profile.postalCode) }
    var outcome by remember { mutableStateOf(PaymentOutcome.APPROVED) }
    var message by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var placedOrder by remember { mutableStateOf<String?>(null) }
    val requestId = remember { "checkout-${Random.nextLong().toString().replace('-', 'n')}-${Random.nextLong().toString().replace('-', 'n')}" }
    val totals = quoteCart(state)
    val form: @Composable ColumnScope.() -> Unit = {
        DemoCard {
            Text("Delivery details", style = MaterialTheme.typography.headlineSmall)
            Text("Use fictional details. The sample address is ready for a demo order.", style = MaterialTheme.typography.bodyMedium)
            ShopField("Full name", name, { name = it })
            ShopField("Email", email, { email = it }, KeyboardType.Email)
            ShopField("Street address", line1, { line1 = it })
            ShopField("City", city, { city = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShopField("State", region, { region = it.uppercase().take(2) }, modifier = Modifier.weight(1f))
                ShopField("ZIP code", postalCode, { postalCode = it }, KeyboardType.Number, Modifier.weight(1f))
            }
            Text("California demo addresses only. This does not determine real shipping eligibility.", style = MaterialTheme.typography.bodySmall)
        }
        DemoCard {
            Text("Simulated payment", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a response to demonstrate successful checkout or recovery from a failed payment. No card or bank account is needed.")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PaymentOutcome.entries.forEach { value ->
                    FilterChip(
                        selected = outcome == value,
                        onClick = { outcome = value; message = null },
                        label = {
                            Text(when (value) {
                                PaymentOutcome.APPROVED -> "Approve demo payment"
                                PaymentOutcome.DECLINED -> "Decline demo payment"
                                PaymentOutcome.TIMEOUT -> "Simulate a timeout"
                            })
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
    val summary: @Composable ColumnScope.() -> Unit = {
        DemoCard {
            Text("Your selection", style = MaterialTheme.typography.headlineSmall)
            state.cart.forEach { line ->
                val product = state.products.firstOrNull { it.id == line.productId }
                if (product != null) {
                    Text("${line.quantity} × ${product.name}", style = MaterialTheme.typography.bodyMedium)
                    Text("${product.packSize} seeds / pack", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            DemoMoneyRow("Products", totals.subtotalCents)
            DemoMoneyRow("Demo shipping", totals.shippingCents)
            DemoMoneyRow("Demo tax · illustrative 8%", totals.taxCents)
            DemoMoneyRow("Demo total", totals.totalCents, emphasized = true)
            Text("SIMULATION · No money is collected. No products will be shipped.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            message?.let { DemoMessage(it) }
            DemoPrimary(
                text = if (submitting) "Saving demo order…" else "Place demo order · ${formatMoney(totals.totalCents)}",
                enabled = !submitting && placedOrder == null && state.cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!submitting && placedOrder == null) {
                        submitting = true
                        message = null
                        val address = DemoAddress(name.trim(), email.trim(), line1.trim(), city.trim(), region.trim().uppercase(), postalCode.trim())
                        val result = store.checkout(CheckoutInput(address, requestId), outcome)
                        submitting = false
                        val orderId = result.orderId
                        if (orderId != null) {
                            placedOrder = orderId
                            onPlaced(orderId)
                        } else {
                            message = result.error ?: "The demo order could not be saved. Your cart is still here."
                        }
                    }
                },
            )
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to cart") }
        }
    }
    Column(
        Modifier.widthIn(max = 1200.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DemoHeading("Delivery & total", "Review your selection and try the complete order flow with a simulated payment.")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 850.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(18.dp), content = form)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp), content = summary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    form()
                    summary()
                }
            }
        }
    }
}

@Composable
private fun ShopField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BrandPaper,
            unfocusedContainerColor = BrandPaper,
            focusedBorderColor = BrandGreen,
            unfocusedBorderColor = BrandOutline,
        ),
    )
}

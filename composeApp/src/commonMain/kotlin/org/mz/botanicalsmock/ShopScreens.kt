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
fun ShopScreen(
    state: DemoState,
    store: DemoStore,
    onProduct: (String) -> Unit,
    onCart: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val published = state.products.filter { it.published }
    val categories = listOf("All") + published.map { it.category }.distinct().sorted()
    val selectedCategory = category.takeIf { it in categories } ?: "All"
    val filtered = published.filter { product ->
        (selectedCategory == "All" || product.category == selectedCategory) &&
            (query.isBlank() || listOf(product.name, product.description, product.category)
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale
        val columns = when {
            fontScale > 1.2f -> GridCells.Adaptive(240.dp * fontScale)
            maxWidth < 360.dp -> GridCells.Fixed(1)
            maxWidth < 650.dp -> GridCells.Fixed(2)
            else -> GridCells.Adaptive(245.dp)
        }
        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(
                        Brush.linearGradient(listOf(Color(0xFF0B3428), Color(0xFF245A42)))
                    ).padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("THE DEMO COLLECTION", color = Color(0xFFD7E7BD), style = MaterialTheme.typography.labelMedium)
                    Text("Discover your\nnext grow.", color = Color(0xFFFFFDF7), style = MaterialTheme.typography.displaySmall)
                    Text(
                        "A curated collection. A complete store experience. Explore fictional genetics, build a cart and follow an order from checkout to delivery.",
                        color = Color(0xFFE1E9DB), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.widthIn(max = 650.dp),
                    )
                    OutlinedButton(
                        onClick = onCart,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text("View your cart · ${state.cart.sumOf { it.quantity }} packs") }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DemoHeading("The collection", "Every product and order in this app is fictional.")
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search products") },
                        placeholder = { Text("Name, category or description") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        trailingIcon = if (query.isNotEmpty()) {
                            { TextButton(onClick = { query = "" }) { Text("Clear") } }
                        } else null,
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { value ->
                            FilterChip(selected = selectedCategory == value, onClick = { category = value }, label = { Text(value) })
                        }
                    }
                    Text("${filtered.size} ${if (filtered.size == 1) "product" else "products"}", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DemoCard {
                        DemoHeading("No matches yet", "Try another search or explore the whole collection.")
                        DemoPrimary("Show all products", onClick = { query = ""; category = "All" })
                    }
                }
            }
            items(filtered, key = { it.id }) { product ->
                ShopProductCard(product, onClick = { onProduct(product.id) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "LOCAL DEMO · No real inventory, payments or shipments. Open Admin to change products and move orders through fulfillment.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ShopProductCard(product: DemoProduct, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        DemoArt(product.id, Modifier.fillMaxWidth().height(155.dp))
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(product.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                product.name, style = MaterialTheme.typography.titleLarge,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text("${product.packSize} seeds / pack", style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(product.priceCents), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (product.stock > 0) "${product.stock} packs available" else "Out of stock",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.stock > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
            Text("View selection →", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
fun ProductScreen(
    productId: String,
    state: DemoState,
    store: DemoStore,
    onBack: () -> Unit,
    onCart: () -> Unit,
) {
    val product = state.products.firstOrNull { it.id == productId }
    var requestedQuantity by remember(productId) { mutableStateOf(1) }
    var message by remember(productId) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ShopBackRow("Collection", onBack, "Cart · ${state.cart.sumOf { it.quantity }}", onCart)
        if (product == null) {
            DemoMessage("This product is no longer available. Return to the collection to choose another.")
        } else {
            val quantity = requestedQuantity.coerceIn(1, product.stock.coerceIn(1, 99))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 780.dp
                val productContent: @Composable ColumnScope.() -> Unit = {
                    DemoHeading(product.name, "${product.category} · Fictional demo genetics")
                    Text(product.description, style = MaterialTheme.typography.bodyLarge)
                    DemoCard {
                        Text("Your selection", style = MaterialTheme.typography.titleLarge)
                        Text("Pack size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${product.packSize} seeds per pack", style = MaterialTheme.typography.bodyLarge)
                        DemoMoneyRow("Price per pack", product.priceCents)
                        Text("Quantity in packs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ShopQuantity(
                            quantity = quantity,
                            onMinus = { requestedQuantity = (quantity - 1).coerceAtLeast(1) },
                            onPlus = { requestedQuantity = quantity + 1 },
                            minusEnabled = quantity > 1,
                            plusEnabled = quantity < product.stock.coerceAtMost(99),
                        )
                        Text(
                            if (product.stock > 0) "${product.stock} packs available" else "Currently out of stock",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider()
                        DemoMoneyRow("Selection total", product.priceCents * quantity, emphasized = true)
                        Text("Illustrative shipping and tax are shown at checkout.", style = MaterialTheme.typography.bodySmall)
                        DemoPrimary(
                            text = if (!product.published || product.stock == 0) "Unavailable" else "Add to cart",
                            enabled = product.published && product.stock > 0,
                            onClick = {
                                val error = store.addToCart(product.id, quantity)
                                message = error ?: "Added $quantity ${if (quantity == 1) "pack" else "packs"} to your cart."
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        message?.let { DemoMessage(it) }
                        TextButton(onClick = onCart, modifier = Modifier.heightIn(min = 48.dp)) { Text("View cart") }
                    }
                }
                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        DemoArt(product.id, Modifier.weight(1f).height(490.dp).clip(RoundedCornerShape(28.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp), content = productContent)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DemoArt(product.id, Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp)))
                        productContent()
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    state: DemoState,
    store: DemoStore,
    onShop: () -> Unit,
    onCheckout: () -> Unit,
) {
    var message by remember { mutableStateOf<String?>(null) }
    val totals = quoteCart(state)
    val ready = state.cart.isNotEmpty() && state.cart.all { line ->
        state.products.any { it.id == line.productId && it.published && it.stock >= line.quantity && line.quantity > 0 }
    }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { DemoHeading("Your cart", "${state.cart.sumOf { it.quantity }} packs selected · Demo checkout only") }
        if (state.cart.isEmpty()) {
            item {
                DemoCard {
                    Text("Good things start with a selection.", style = MaterialTheme.typography.headlineSmall)
                    Text("Explore the collection and add a few fictional products to try the complete checkout experience.")
                    DemoPrimary("Explore collection", onClick = onShop)
                }
            }
        } else {
            items(state.cart, key = { it.productId }) { line ->
                val product = state.products.firstOrNull { it.id == line.productId }
                DemoCard {
                    if (product == null) {
                        Text("Unavailable product", style = MaterialTheme.typography.titleMedium)
                        Text("Remove this item to continue.")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            DemoArt(product.id, Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium)
                                Text("${product.packSize} seeds / pack", style = MaterialTheme.typography.bodySmall)
                                Text("${formatMoney(product.priceCents)} per pack", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (!product.published || product.stock < line.quantity) {
                            DemoMessage(if (!product.published) "This product is unavailable. Remove it to continue." else "Only ${product.stock} packs remain. Reduce the quantity or remove this item.")
                        }
                        ShopQuantity(
                            quantity = line.quantity,
                            onMinus = {
                                val next = (line.quantity - 1).coerceAtMost(product.stock).coerceAtLeast(0)
                                message = if (next == 0) store.removeFromCart(line.productId) else store.updateCart(line.productId, next)
                            },
                            onPlus = { message = store.updateCart(line.productId, line.quantity + 1) },
                            minusEnabled = true,
                            plusEnabled = product.published && line.quantity < product.stock.coerceAtMost(99),
                        )
                        DemoMoneyRow("Item total", product.priceCents * line.quantity, emphasized = true)
                    }
                    TextButton(onClick = { message = store.removeFromCart(line.productId) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove item") }
                }
            }
            item {
                DemoCard {
                    Text("Order summary", style = MaterialTheme.typography.titleLarge)
                    DemoMoneyRow("Products", totals.subtotalCents)
                    DemoMoneyRow("Demo shipping", totals.shippingCents)
                    DemoMoneyRow("Demo tax · illustrative 8%", totals.taxCents)
                    HorizontalDivider()
                    DemoMoneyRow("Demo total", totals.totalCents, emphasized = true)
                    Text("Shipping and tax are fictional demo amounts. No payment is collected.", style = MaterialTheme.typography.bodySmall)
                    message?.let { DemoMessage(it) }
                    DemoPrimary("Continue to demo checkout", onClick = onCheckout, enabled = ready, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = onShop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Continue shopping") }
                    TextButton(onClick = { message = store.clearCart() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear cart") }
                }
            }
        }
    }
}

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
            Text("Shipping details", style = MaterialTheme.typography.titleLarge)
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
            Text("Simulated payment", style = MaterialTheme.typography.titleLarge)
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
            Text("Review your order", style = MaterialTheme.typography.titleLarge)
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DemoHeading("Demo checkout", "Try the complete order flow without a real transaction.")
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
private fun ShopBackRow(backLabel: String, onBack: () -> Unit, actionLabel: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("← $backLabel") }
        TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(actionLabel) }
    }
}

@Composable
private fun ShopQuantity(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit, minusEnabled: Boolean, plusEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("−") }
        Text(quantity.toString(), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onPlus, enabled = plusEnabled, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
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
    )
}

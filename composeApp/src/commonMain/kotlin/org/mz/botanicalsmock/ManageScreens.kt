package org.mz.botanicalsmock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import org.mz.botanicalsmock.model.*

private fun OrderStatus.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
private fun CustomerApproval.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun OrdersScreen(state: DemoState, onOrder: (String) -> Unit, onShop: () -> Unit) {
    var filter by remember { mutableStateOf("All") }
    val allOrders = state.orders.filter { it.customerId == "demo-shopper" }.sortedByDescending { it.createdAt }
    val orders = allOrders.filter {
        when (filter) {
            "Active" -> it.status in listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPED)
            "Completed" -> it.status in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)
            else -> true
        }
    }
    ManagementBody(1000.dp) {
        OrderText("Order status and shipping updates from the store.", secondary = true)
        ManagementBanner("SAMPLE ORDER HISTORY", "Orders, payments and delivery updates are fictional and stay on this device.")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Active", "Completed").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) }, colors = managementChipColors())
            }
        }
        if (orders.isEmpty()) {
            ManagementCard {
                Text(if (allOrders.isEmpty()) "Your collection starts here" else "No ${filter.lowercase()} orders", style = MaterialTheme.typography.titleLarge)
                Text(if (allOrders.isEmpty()) "Place a simulated order to see it here." else "Choose another filter to explore your demo history.")
                DemoPrimary("Explore the collection", onClick = onShop)
            }
        }
        orders.forEach { order ->
            ManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OrderText("Order #${order.id.takeLast(8).uppercase()}", bold = true, title = true)
                    OrderText(dateLabel(order.createdAt), secondary = true)
                }
                StatusPill(order.status)
                if (order.tracking.isNotBlank()) OrderText("Tracking: ${order.tracking}", secondary = true)
                order.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OrderText(item.name, bold = true)
                            OrderText("${item.quantity} × ${formatMoney(item.unitPriceCents)}", secondary = true)
                            OrderText("${item.packSize} seeds per pack", secondary = true)
                        }
                        OrderText(formatMoney(item.unitPriceCents * item.quantity))
                    }
                }
                HorizontalDivider(color = BrandGreenSoft)
                DemoMoneyRow("Merchandise", order.subtotalCents)
                DemoMoneyRow("Demo shipping", order.shippingCents)
                DemoMoneyRow("Demo tax", order.taxCents)
                DemoMoneyRow("Order total", order.totalCents, emphasized = true)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OrderText("Shipping details", bold = true)
                    OrderText(order.customerName)
                    OrderText("${order.shipping.line1}, ${order.shipping.city}, ${order.shipping.state} ${order.shipping.postalCode}", secondary = true)
                    OrderText(order.shipping.email, secondary = true)
                }
                OutlinedButton(onClick = { onOrder(order.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("View order & updates")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun OrderScreen(orderId: String, state: DemoState, store: DemoStore, onBack: () -> Unit) {
    val order = state.orders.find { it.id == orderId }
    var message by remember(orderId) { mutableStateOf<String?>(null) }
    var confirmCancel by remember(orderId) { mutableStateOf(false) }
    ManagementBody(1000.dp) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("← Back") }
        if (order == null) {
            DemoHeading("Order unavailable", "This order may have been removed when the demo was reset.")
            return@ManagementBody
        }
        Text("Order #${order.id.takeLast(8).uppercase()}", fontFamily = Exo2FontRegular(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = BrandGreen)
        OrderText("Placed ${dateLabel(order.createdAt)} · simulated purchase", secondary = true)
        OrderText("Reference: ${order.id}", secondary = true)
        message?.let { DemoMessage(it) }
        ManagementCard {
            StatusPill(order.status)
            Text(statusDescription(order.status), style = MaterialTheme.typography.titleMedium)
            Text(paymentDescription(order.status), style = MaterialTheme.typography.bodyMedium)
            if (order.tracking.isNotBlank()) {
                HorizontalDivider()
                Text("Simulated tracking reference", style = MaterialTheme.typography.labelLarge)
                Text(order.tracking)
                Text("Demo tracking only. No carrier shipment has been created.", style = MaterialTheme.typography.bodySmall)
            }
        }
        ManagementCard {
            Text("Order journey", style = MaterialTheme.typography.titleLarge)
            order.events.sortedBy { it.atMillis }.forEachIndexed { index, event ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraLarge) {
                        Text("${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(event.status.displayName(), style = MaterialTheme.typography.titleMedium)
                        Text(dateLabel(event.atMillis), style = MaterialTheme.typography.bodySmall)
                        if (event.note.isNotBlank()) Text(event.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        ManagementCard {
            Text("Inside your order", style = MaterialTheme.typography.titleLarge)
            order.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    DemoArt(item.productId, Modifier.width(66.dp).height(72.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("${item.packSize} seeds / pack · ${item.quantity} ${if (item.quantity == 1) "pack" else "packs"}", style = MaterialTheme.typography.bodySmall)
                        Text(formatMoney(item.unitPriceCents * item.quantity), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            HorizontalDivider()
            DemoMoneyRow("Merchandise", order.subtotalCents)
            DemoMoneyRow("Demo shipping", order.shippingCents)
            DemoMoneyRow("Fictional demo tax", order.taxCents)
            DemoMoneyRow("Total", order.totalCents, emphasized = true)
        }
        ManagementCard {
            Text("Delivery details", style = MaterialTheme.typography.titleLarge)
            Text(order.customerName)
            Text(order.shipping.email)
            Text(order.shipping.line1)
            Text("${order.shipping.city}, ${order.shipping.state} ${order.shipping.postalCode}")
            Text("Fictional address. Nothing will be shipped.", style = MaterialTheme.typography.bodySmall)
        }
        if (order.customerId == "demo-shopper" && order.status in listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING)) {
            OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Cancel this demo order")
            }
        }
    }
    if (confirmCancel && order != null) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel demo order?") },
            text = { Text("The demo order will be cancelled and its reserved stock restored. No real money is involved.") },
            confirmButton = {
                TextButton(onClick = {
                    message = store.transitionOrder(order.id, OrderStatus.CANCELLED) ?: "Demo order cancelled. Stock has been restored."
                    confirmCancel = false
                }) { Text("Cancel order") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep order") } }
        )
    }
}

@Composable
fun AdminScreen(state: DemoState, store: DemoStore, onOrder: (String) -> Unit) {
    var tab by remember { mutableStateOf("Overview") }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<DemoProduct?>(null) }
    var reviewing by remember { mutableStateOf<DemoCustomer?>(null) }
    var productFilter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    ManagementBody(1120.dp, tab) {
        ManagementBanner("ADMIN WORKSPACE", "Products, orders and customer reviews use fictional records. Changes are saved in the mock app on this device.")
        if (tab != "Overview") {
            TextButton(onClick = { tab = "Overview"; query = ""; message = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("← Admin overview") }
        }
        Text(
            when (tab) { "Products" -> "Products & Inventory"; "Orders" -> "Orders & Fulfillment"; "Customers" -> "Customers & Verification"; "Settings" -> "Store setup"; else -> "Your business, at a glance" },
            color = BrandGreen, fontSize = 26.sp, fontWeight = FontWeight.SemiBold
        )
        Text(
            when (tab) { "Products" -> "Catalog, stock, and product records"; "Orders" -> "Payments, packing, and delivery"; "Customers" -> "Store accounts and manual approval"; "Settings" -> "Business details, policies, and setup status"; else -> "Explore the four areas of your admin workspace." },
            color = BrandTextMuted, style = MaterialTheme.typography.bodyMedium
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Overview", "Products", "Orders", "Customers", "Settings").forEach { label ->
                FilterChip(selected = tab == label, onClick = { tab = label; query = ""; message = null }, label = { Text(label) }, colors = managementChipColors())
            }
        }
        if (tab != "Overview" && tab != "Settings") {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text(if (tab == "Products") "Search name, category, or product ID" else "Search ${tab.lowercase()}") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
            )
        }
        message?.let { DemoMessage(it) }
        when (tab) {
            "Overview" -> {
                AdminWorkspaceCard("Products & Inventory", "Catalog, stock, and product records", "${state.products.size} products · ${state.products.sumOf { it.stock }} packs in inventory", CompanyIcon.EXPLORE) { tab = "Products" }
                AdminWorkspaceCard("Orders & Fulfillment", "Payments, packing, and delivery", "${state.orders.count { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.PACKING }} orders to prepare", CompanyIcon.ORDERS) { tab = "Orders" }
                AdminWorkspaceCard("Customers & Verification", "Store accounts and manual approval", "${state.customers.count { it.approval == CustomerApproval.PENDING }} customers awaiting review", CompanyIcon.PROFILE) { tab = "Customers" }
                AdminWorkspaceCard("Store setup", "Business details, policies, and setup status", "Review the demonstration configuration", CompanyIcon.SETTINGS) { tab = "Settings" }
                ManagementCard {
                    Text("Store at a glance", color = BrandGreen, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${state.products.count { it.stock <= 5 && it.published }} low-stock products", color = BrandTextMuted)
                    DemoMoneyRow("Simulated merchandise sales", state.orders.filter { it.status !in listOf(OrderStatus.CANCELLED, OrderStatus.REFUNDED) }.sumOf { it.subtotalCents }, emphasized = true)
                }
            }
            "Orders" -> {
                val orders = state.orders.sortedByDescending { it.createdAt }.filter {
                    query.isBlank() || it.id.contains(query, true) || it.customerName.contains(query, true) || it.status.displayName().contains(query, true)
                }
                if (orders.isEmpty()) DemoMessage("No demo orders match your search.")
                orders.forEach { order -> AdminOrderCard(order, store, onOrder) }
            }
            "Products" -> {
                Button(onClick = {
                    editing = DemoProduct(
                        id = "demo-product-${(state.products.size + 1)}-${state.products.sumOf { it.id.length }}",
                        name = "", category = "Feminized", description = "", packSize = 5,
                        priceCents = 4000, stock = 10, published = false, featured = false
                    )
                }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) { Text("Add product") }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Drafts", "Published", "Low stock").forEach { filter ->
                        FilterChip(selected = productFilter == filter, onClick = { productFilter = filter }, label = { Text(filter) }, colors = managementChipColors())
                    }
                }
                val products = state.products.filter {
                    (query.isBlank() || "${it.name} ${it.category} ${it.id}".contains(query.trim(), true)) &&
                        when (productFilter) { "Drafts" -> !it.published; "Published" -> it.published; "Low stock" -> it.stock <= 5; else -> true }
                }
                Text("${products.size} products · ${products.sumOf { it.stock }} packs in matching inventory", color = BrandGreen, fontSize = 13.sp)
                if (products.isEmpty()) DemoMessage("No demo products match your search.")
                products.forEach { product ->
                    ManagementCard(color = Color.White) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            DemoArt(product.id, Modifier.width(80.dp).height(90.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium)
                                Text("${product.category} · ${product.packSize} seeds / pack", style = MaterialTheme.typography.bodySmall)
                                Text("${product.stock} packs in stock · ${formatMoney(product.priceCents)} / pack")
                                Text(if (product.published) "Published" else "Draft", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        OutlinedButton(onClick = { editing = product }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Edit product") }
                    }
                }
            }
            "Customers" -> {
                Text("Manual sample approval only. This does not verify anyone's identity or age.", style = MaterialTheme.typography.bodyMedium)
                val customers = state.customers.filter { query.isBlank() || it.name.contains(query, true) || it.email.contains(query, true) || it.approval.displayName().contains(query, true) }
                if (customers.isEmpty()) DemoMessage("No demo customers match your search.")
                customers.forEach { customer ->
                    Card(onClick = { reviewing = customer }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(customer.name, color = BrandGreen, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(customer.email, color = BrandTextMuted, fontSize = 13.sp)
                            Surface(color = BrandGreenSoft, shape = RoundedCornerShape(30.dp)) {
                                Text(customer.approval.displayName(), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = BrandGreen, fontSize = 12.sp)
                            }
                            Text("Review account ›", color = BrandGreen, fontSize = 13.sp)
                        }
                    }
                }
            }
            "Settings" -> AdminSetupDetails()
        }
        Spacer(Modifier.height(12.dp))
    }
    editing?.let { product ->
        ProductEditor(product, onDismiss = { editing = null }, onSave = { updated ->
            val error = store.saveProduct(updated)
            if (error == null) {
                editing = null
                message = "${updated.name} saved."
            }
            error
        })
    }
    reviewing?.let { customer ->
        CustomerReviewDialog(customer, onDismiss = { reviewing = null }, onSave = { approval ->
            val error = store.reviewCustomer(customer.id, approval)
            if (error == null) { message = "${customer.name}: ${approval.displayName()} (demo)."; reviewing = null }
            error
        })
    }
}

@Composable
private fun AdminOrderCard(order: DemoOrder, store: DemoStore, onOrder: (String) -> Unit) {
    var message by remember(order.id) { mutableStateOf<String?>(null) }
    var tracking by remember(order.id, order.tracking) { mutableStateOf(order.tracking.ifBlank { "DEMO-${order.id.takeLast(8)}" }) }
    var confirmation by remember(order.id) { mutableStateOf<OrderStatus?>(null) }
    ManagementCard(color = Color.White) {
        StatusPill(order.status)
        Text("Order #${order.id.takeLast(8).uppercase()}", color = BrandGreen, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(order.customerName)
        Text("${dateLabel(order.createdAt)} · ${order.items.sumOf { it.quantity }} packs", style = MaterialTheme.typography.bodySmall)
        DemoMoneyRow("Simulated total", order.totalCents)
        Text(paymentDescription(order.status), style = MaterialTheme.typography.bodySmall)
        message?.let { DemoMessage(it) }
        OutlinedButton(onClick = { onOrder(order.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("View full order") }
        if (order.status == OrderStatus.PACKING) {
            OutlinedTextField(
                value = tracking, onValueChange = { tracking = it },
                label = { Text("Simulated tracking reference") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val next = when (order.status) {
            OrderStatus.CONFIRMED -> OrderStatus.PACKING
            OrderStatus.PACKING -> OrderStatus.SHIPPED
            OrderStatus.SHIPPED -> OrderStatus.DELIVERED
            else -> null
        }
        if (next != null) {
            DemoPrimary("Mark ${next.displayName().lowercase()}", onClick = {
                message = store.transitionOrder(order.id, next, if (next == OrderStatus.SHIPPED) tracking else order.tracking)
                    ?: "Demo order marked ${next.displayName().lowercase()}."
            })
        }
        if (order.status in listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING)) {
            TextButton(onClick = { confirmation = OrderStatus.CANCELLED }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel demo order") }
        }
        if (order.status in listOf(OrderStatus.SHIPPED, OrderStatus.DELIVERED)) {
            TextButton(onClick = { confirmation = OrderStatus.REFUNDED }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Simulate full refund") }
        }
    }
    confirmation?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(if (target == OrderStatus.REFUNDED) "Simulate a full refund?" else "Cancel demo order?") },
            text = { Text("This updates the fictional order and restores its demo inventory once. No payment provider or real money is involved.") },
            confirmButton = {
                TextButton(onClick = {
                    message = store.transitionOrder(order.id, target) ?: "Demo order ${target.displayName().lowercase()}."
                    confirmation = null
                }) { Text(if (target == OrderStatus.REFUNDED) "Simulate refund" else "Cancel order") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Keep order") } }
        )
    }
}

@Composable
private fun ProductEditor(product: DemoProduct, onDismiss: () -> Unit, onSave: (DemoProduct) -> String?) {
    var name by remember(product.id) { mutableStateOf(product.name) }
    var category by remember(product.id) { mutableStateOf(product.category) }
    var description by remember(product.id) { mutableStateOf(product.description) }
    var packSize by remember(product.id) { mutableStateOf(product.packSize.toString()) }
    var price by remember(product.id) { mutableStateOf("${product.priceCents / 100}.${(product.priceCents % 100).toString().padStart(2, '0')}") }
    var stock by remember(product.id) { mutableStateOf(product.stock.toString()) }
    var published by remember(product.id) { mutableStateOf(product.published) }
    var featured by remember(product.id) { mutableStateOf(product.featured) }
    var error by remember(product.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product.name.isBlank()) "Add demo product" else "Edit demo product") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Changes appear immediately in this device's demo collection.")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Product description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = packSize, onValueChange = { packSize = it }, label = { Text("Seeds in each pack") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price per pack (USD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Packs available in stock") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = published, onCheckedChange = { published = it })
                    Text("Published in the collection", Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured product", Modifier.weight(1f))
                }
                error?.let { DemoMessage(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedPack = packSize.trim().toIntOrNull()
                val parsedStock = stock.trim().toIntOrNull()
                val cents = parsePrice(price)
                error = when {
                    name.isBlank() -> "Enter a product name."
                    category.isBlank() -> "Enter a category."
                    description.isBlank() -> "Enter a product description."
                    parsedPack == null || parsedPack !in 1..1000 -> "Pack size must be between 1 and 1,000 seeds."
                    parsedStock == null || parsedStock !in 0..1000000 -> "Stock must be between 0 and 1,000,000 packs."
                    cents == null -> "Enter a valid USD price with up to two decimal places."
                    else -> onSave(product.copy(name = name.trim(), category = category.trim(), description = description.trim(), packSize = parsedPack, stock = parsedStock, priceCents = cents, published = published, featured = featured))
                }
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } }
    )
}

@Composable
fun ProfileScreen(state: DemoState, store: DemoStore) {
    var firstName by remember(state.profile) { mutableStateOf(state.profile.name.substringBefore(' ')) }
    var lastName by remember(state.profile) { mutableStateOf(state.profile.name.substringAfter(' ', "")) }
    var email by remember(state.profile) { mutableStateOf(state.profile.email) }
    var line1 by remember(state.profile) { mutableStateOf(state.profile.line1) }
    var city by remember(state.profile) { mutableStateOf(state.profile.city) }
    var region by remember(state.profile) { mutableStateOf(state.profile.state) }
    var postal by remember(state.profile) { mutableStateOf(state.profile.postalCode) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    ManagementBody(840.dp) {
        Text("My Profile", color = BrandGreen, fontFamily = Exo2FontRegular(), fontSize = 30.sp)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompanyBrandLogo(Modifier.size(88.dp).clip(CircleShape))
            Text(state.profile.name, color = BrandGreen, fontFamily = Exo2FontRegular(), fontWeight = FontWeight.SemiBold)
            Text("Demo customer · saved on this device", color = BrandTextMuted, fontSize = 13.sp)
        }
        ManagementBanner("DEMO PROFILE", "Use fictional details. This local profile is separate from real store accounts.")
        message?.let { DemoMessage(it) }
        ManagementCard {
            Text("Personal details", color = BrandGreen, fontFamily = Exo2FontRegular(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 560.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email address") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            HorizontalDivider(color = BrandGreenSoft)
            Text("Delivery details", color = BrandGreen, fontFamily = Exo2FontRegular(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = line1, onValueChange = { line1 = it }, label = { Text("Street address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("State abbreviation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = postal, onValueChange = { postal = it }, label = { Text("ZIP code") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            DemoPrimary("Update", onClick = {
                message = store.saveProfile(DemoAddress(listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" "), email.trim(), line1.trim(), city.trim(), region.trim().uppercase(), postal.trim())) ?: "Demo profile saved on this device."
            })
        }
        ManagementCard {
            Text("Start a fresh presentation", style = MaterialTheme.typography.titleLarge)
            Text("Reset products, stock, cart, customer reviews, profile and orders to the original sample data. This only changes the mock app on this device.")
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Reset all demo data") }
        }
        ManagementCard {
            Text("Made for demonstrations", style = MaterialTheme.typography.titleLarge)
            Text("Orders, payments, refunds, taxes, shipping and customer reviews are simulated. There are no live payment services, carrier connections, emails or customer identity checks.")
            Text("The company setup app is a separate installation and repository.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset this demo?") },
            text = { Text("All changes and newly created orders in this mock app will be replaced with the original sample data. Your separate company setup app is unaffected.") },
            confirmButton = { TextButton(onClick = {
                val error = store.resetDemo()
                if (error == null) {
                    // Also discard unsaved edits when the saved profile already
                    // equals the seed and remember(state.profile) does not reset.
                    val restored = store.state.value.profile
                    firstName = restored.name.substringBefore(' ')
                    lastName = restored.name.substringAfter(' ', "")
                    email = restored.email
                    line1 = restored.line1
                    city = restored.city
                    region = restored.state
                    postal = restored.postalCode
                }
                message = error ?: "Fresh sample data is ready."
                confirmReset = false
            }) { Text("Reset demo data") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Keep my changes") } }
        )
    }
}

@Composable
private fun StatusPill(status: OrderStatus) {
    Surface(
        color = if (status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("${status.displayName()} · Simulated payment", Modifier.padding(horizontal = 10.dp, vertical = 8.dp), fontFamily = Exo2FontRegular(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Match the company screens' centered desktop widths and mobile content padding. */
@Composable
private fun ManagementBody(maxWidth: Dp, resetKey: Any? = null, content: @Composable ColumnScope.() -> Unit) {
    key(resetKey) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = maxWidth).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ManagementCard(modifier: Modifier = Modifier, color: Color = BrandCream, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = color, border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreenSoft)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ManagementBanner(title: String, message: String) {
    Surface(color = BrandGreenSoft, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(message, color = BrandGreen, fontSize = 13.sp)
        }
    }
}

@Composable
private fun OrderText(text: String, secondary: Boolean = false, bold: Boolean = false, title: Boolean = false) {
    Text(text, color = BrandGreen.copy(alpha = if (secondary) 0.65f else 1f), fontFamily = Exo2FontRegular(), fontSize = if (title) 18.sp else 14.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable
private fun managementChipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandGreen, selectedLabelColor = BrandCream, labelColor = BrandGreen)

@Composable
private fun AdminWorkspaceCard(title: String, description: String, summary: String, icon: CompanyIcon, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CompanyIcon(icon, null, Modifier.size(30.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(description, color = BrandTextMuted, fontSize = 13.sp)
                Text(summary, color = BrandGreen, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
            Text("›", color = BrandGreen, fontSize = 28.sp)
        }
    }
}

@Composable
private fun CustomerReviewDialog(customer: DemoCustomer, onDismiss: () -> Unit, onSave: (CustomerApproval) -> String?) {
    var approval by remember(customer.id) { mutableStateOf(customer.approval) }
    var error by remember(customer.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BrandCream,
        title = { Text(customer.name, color = BrandGreen) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(customer.email)
                Text("Current decision: ${customer.approval.displayName()}")
                CustomerApproval.entries.forEach { choice ->
                    FilterChip(selected = approval == choice, onClick = { approval = choice }, label = { Text(choice.displayName()) }, colors = managementChipColors())
                }
                Text("This records a manual decision for a fictional customer. It does not verify identity or age. Pending or rejected demo shoppers cannot place new demo orders.", color = BrandTextMuted, fontSize = 13.sp)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { error = onSave(approval) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save store decision") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") } }
    )
}

@Composable
private fun AdminSetupDetails() {
    ManagementBanner("DEMONSTRATION CONFIGURATION", "This setup belongs to the offline mock. No business account, payment provider, tax service or carrier is connected.")
    ManagementCard(color = Color.White) {
        Text("Business details", color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text("MZ Botanicals · Sample store", color = BrandGreen)
        Text("All customers, product records and addresses are fictional. Store records and profile edits are saved only on this device.", color = BrandTextMuted)
    }
    ManagementCard(color = Color.White) {
        Text("Checkout & fulfillment", color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text("Payments: simulated approval, decline and timeout scenarios", color = BrandGreen)
        Text("Shipping: fictional California addresses and a $5.00 sample charge", color = BrandGreen)
        Text("Tax: illustrative 8% of merchandise, not a real tax calculation", color = BrandGreen)
        Text("Tracking, cancellations and refunds: local demonstrations only", color = BrandGreen)
    }
    ManagementCard(color = Color.White) {
        Text("Presentation setup", color = BrandGreen, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Text("Use Products to manage stock and publication, Customers to demonstrate manual approval, and Orders to progress fulfillment.", color = BrandTextMuted)
        Text("Profile contains the editable demo shipping address and a confirmed reset action for restoring all samples.", color = BrandTextMuted)
        Text("The company application is installed separately. Its configuration and customer data are unaffected.", color = BrandTextMuted)
    }
}

private fun statusDescription(status: OrderStatus): String = when (status) {
    OrderStatus.CONFIRMED -> "Your demo order is confirmed."
    OrderStatus.PACKING -> "Your sample order is being prepared."
    OrderStatus.SHIPPED -> "Your demo order is on its way."
    OrderStatus.DELIVERED -> "Your demo order has arrived."
    OrderStatus.CANCELLED -> "This demo order was cancelled."
    OrderStatus.REFUNDED -> "A full demo refund was recorded."
}

private fun paymentDescription(status: OrderStatus): String = when (status) {
    OrderStatus.REFUNDED -> "Simulated payment refunded. No real funds were moved."
    OrderStatus.CANCELLED -> "Simulated payment voided. No real funds were moved."
    else -> "Simulated payment approved. No real charge was made."
}

private fun parsePrice(value: String): Long? {
    val text = value.trim()
    if (!Regex("[0-9]{1,6}(\\.[0-9]{1,2})?").matches(text)) return null
    val pieces = text.split('.')
    val dollars = pieces[0].toLongOrNull() ?: return null
    val cents = pieces.getOrNull(1)?.padEnd(2, '0')?.toLongOrNull() ?: 0
    return dollars * 100 + cents
}

/** Gregorian UTC date for the offline timeline, without requiring a platform clock API. */
private fun dateLabel(atMillis: Long): String {
    val timestamp = atMillis.coerceAtLeast(0)
    val z = timestamp / 86_400_000 + 719468
    val era = z / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var year = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = mp + if (mp < 10) 3 else -9
    if (month <= 2) year++
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val hour = ((timestamp / 3_600_000) % 24).toString().padStart(2, '0')
    val minute = ((timestamp / 60_000) % 60).toString().padStart(2, '0')
    return "${months[(month - 1).toInt()]} $day, $year · $hour:$minute UTC"
}

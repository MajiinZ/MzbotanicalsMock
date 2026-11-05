package org.mz.botanicalsmock.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CompletedCheckout(val requestId: String, val address: DemoAddress, val orderId: String)

@Serializable
private data class SavedDemo(val version: Int = 1, val state: DemoState, val completedCheckouts: List<CompletedCheckout> = emptyList())

/**
 * Offline commerce state machine. Call operations serially on the UI thread.
 * Saves are written before a successful state is exposed to observers. A failed
 * disk write leaves the cart, inventory, history and idempotency records intact.
 */
class DemoStore(private val storage: DemoStorage, private val nowMillis: () -> Long) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var errorOnLoad: String? = null
    private var saved = initialSnapshot()
    private val mutableState = MutableStateFlow(saved.state)
    val state: StateFlow<DemoState> = mutableState.asStateFlow()
    val loadError: String? get() = errorOnLoad

    private fun initialSnapshot(): SavedDemo {
        return try {
            val raw = storage.load()
            if (raw == null) SavedDemo(state = seedDemo(nowMillis().coerceAtLeast(0)))
            else {
                require(raw.length <= 4_000_000)
                json.decodeFromString<SavedDemo>(raw).also(::validateSaved)
            }
        } catch (_: Exception) {
            errorOnLoad = "Saved demo data could not be loaded. It has been preserved. Reset the demo to start again."
            // The UI may display safe samples, but all mutations remain blocked
            // until the user explicitly resets the unreadable local data.
            SavedDemo(state = seedDemo(nowMillis().coerceAtLeast(0)))
        }
    }

    fun addToCart(id: String, quantity: Int = 1): String? = change {
        require(quantity in 1..MAX_QUANTITY) { "Choose a quantity between 1 and $MAX_QUANTITY." }
        val product = requireProduct(id)
        require(product.published) { "This product is not currently published." }
        val previous = saved.state.cart.firstOrNull { it.productId == id }?.quantity ?: 0
        val combined = previous + quantity
        require(combined <= MAX_QUANTITY && combined <= product.stock) { "There are not enough packs in stock." }
        val newCart = saved.state.cart.filterNot { it.productId == id } + DemoCartItem(id, combined)
        saved.copy(state = saved.state.copy(cart = newCart))
    }

    fun updateCart(id: String, quantity: Int): String? = change {
        require(quantity in 0..MAX_QUANTITY) { "Choose a valid quantity." }
        require(saved.state.cart.any { it.productId == id }) { "This product is not in your cart." }
        if (quantity == 0) saved.copy(state = saved.state.copy(cart = saved.state.cart.filterNot { it.productId == id }))
        else {
            val product = requireProduct(id)
            require(product.published && quantity <= product.stock) { "There are not enough available packs in stock." }
            saved.copy(state = saved.state.copy(cart = saved.state.cart.map { if (it.productId == id) it.copy(quantity = quantity) else it }))
        }
    }

    fun removeFromCart(id: String): String? = change {
        saved.copy(state = saved.state.copy(cart = saved.state.cart.filterNot { it.productId == id }))
    }

    fun clearCart(): String? = change { saved.copy(state = saved.state.copy(cart = emptyList())) }

    fun saveProfile(address: DemoAddress): String? = change {
        val normalized = normalizeAddress(address)
        validateAddress(normalized)
        saved.copy(state = saved.state.copy(
            profile = normalized,
            customers = saved.state.customers.map { if (it.id == SHOPPER_ID) it.copy(name = normalized.name, email = normalized.email) else it },
        ))
    }

    fun saveProduct(product: DemoProduct): String? = change {
        val normalized = product.copy(id = product.id.trim(), name = product.name.trim(), category = product.category.trim(), description = product.description.trim())
        validateProduct(normalized)
        val exists = saved.state.products.any { it.id == normalized.id }
        require(exists || saved.state.products.size < 200) { "The demo catalog is full. Reset it to start again." }
        val products = if (exists) saved.state.products.map { if (it.id == normalized.id) normalized else it } else saved.state.products + normalized
        saved.copy(state = saved.state.copy(products = products))
    }

    fun reviewCustomer(id: String, approval: CustomerApproval): String? = change {
        require(saved.state.customers.any { it.id == id }) { "This sample customer could not be found." }
        saved.copy(state = saved.state.copy(customers = saved.state.customers.map { if (it.id == id) it.copy(approval = approval) else it }))
    }

    fun checkout(input: CheckoutInput, outcome: PaymentOutcome): CheckoutResult {
        errorOnLoad?.let { return CheckoutResult(error = it) }
        return try {
            val address = normalizeAddress(input.address)
            require(input.requestId.isNotBlank() && input.requestId.length <= 160) { "Reopen checkout to create a valid demo request." }
            val prior = saved.completedCheckouts.firstOrNull { it.requestId == input.requestId }
            if (prior != null) {
                require(prior.address == address) { "This checkout request was already used for another address. Reopen checkout." }
                return CheckoutResult(orderId = prior.orderId)
            }
            require(saved.state.orders.size < 1_000) { "The demo order history is full. Reset it to start again." }
            validateAddress(address)
            require(saved.state.cart.isNotEmpty()) { "Add a product to your cart first." }
            val shopper = requireNotNull(saved.state.customers.firstOrNull { it.id == SHOPPER_ID }) { "The sample shopper is unavailable." }
            require(shopper.approval == CustomerApproval.APPROVED) { "This demo customer needs admin approval before checkout." }
            val lines = saved.state.cart.map { item ->
                val product = requireProduct(item.productId)
                require(product.published) { "${product.name} is no longer available. Remove it from your cart." }
                require(item.quantity in 1..MAX_QUANTITY && item.quantity <= product.stock) { "There are not enough packs of ${product.name}. Update your cart." }
                DemoOrderLine(product.id, product.name, product.packSize, item.quantity, product.priceCents)
            }
            val quote = quoteCart(saved.state)
            when (outcome) {
                PaymentOutcome.DECLINED -> return CheckoutResult(error = "Demo payment declined. Nothing was charged; your cart is unchanged.")
                PaymentOutcome.TIMEOUT -> return CheckoutResult(error = "Demo payment timed out. Nothing was charged; retry when you are ready.")
                PaymentOutcome.APPROVED -> Unit
            }
            val now = nowMillis().coerceAtLeast(0)
            val id = "MZ-DEMO-$now-${saved.state.orders.size + 1}"
            require(saved.state.orders.none { it.id == id }) { "This demo order already exists. Reopen checkout." }
            val order = DemoOrder(
                id, SHOPPER_ID, address.name, lines, address,
                quote.subtotalCents, quote.shippingCents, quote.taxCents, quote.totalCents,
                OrderStatus.CONFIRMED, now, "",
                listOf(DemoOrderEvent(OrderStatus.CONFIRMED, now, "Demo payment approved. No money was charged.")),
            )
            val quantities = lines.associate { it.productId to it.quantity }
            val nextState = saved.state.copy(
                products = saved.state.products.map { it.copy(stock = it.stock - (quantities[it.id] ?: 0)) },
                cart = emptyList(),
                orders = listOf(order) + saved.state.orders,
                profile = address,
                customers = saved.state.customers.map { if (it.id == SHOPPER_ID) it.copy(name = address.name, email = address.email) else it },
            )
            val next = saved.copy(state = nextState, completedCheckouts = saved.completedCheckouts + CompletedCheckout(input.requestId, address, id))
            persist(next)
            CheckoutResult(orderId = id)
        } catch (invalid: IllegalArgumentException) {
            CheckoutResult(error = invalid.message ?: "Review your demo checkout details.")
        } catch (_: Exception) {
            CheckoutResult(error = "The demo order could not be saved. Your cart and stock are unchanged. Please try again.")
        }
    }

    fun transitionOrder(id: String, status: OrderStatus, tracking: String = ""): String? = change {
        val order = requireNotNull(saved.state.orders.firstOrNull { it.id == id }) { "This sample order could not be found." }
        if (order.status == status) return@change saved
        require(isAllowedTransition(order.status, status)) { "This order cannot move from ${order.status} to $status." }
        require(tracking.length <= 120 && tracking.none { it.isISOControl() }) { "Use a tracking reference of 120 characters or fewer." }
        val eventTime = maxOf(nowMillis().coerceAtLeast(0), order.events.lastOrNull()?.atMillis ?: order.createdAt)
        val shipmentTracking = if (status == OrderStatus.SHIPPED) tracking.trim().ifBlank { "DEMO-TRACK-${order.id.removePrefix("MZ-DEMO-")}" } else order.tracking
        val note = when (status) {
            OrderStatus.CONFIRMED -> "Demo order confirmed."
            OrderStatus.PACKING -> "Sample order is being prepared."
            OrderStatus.SHIPPED -> "Simulated dispatch. Tracking is fictional."
            OrderStatus.DELIVERED -> "Simulated delivery completed."
            OrderStatus.CANCELLED -> "Demo order cancelled. Sample stock returned."
            OrderStatus.REFUNDED -> "Simulated refund and inventory return. No money was moved."
        }
        val changedOrder = order.copy(status = status, tracking = shipmentTracking, events = order.events + DemoOrderEvent(status, eventTime, note))
        val restoreStock = status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED
        val restored = if (restoreStock) order.items.associate { it.productId to it.quantity } else emptyMap()
        val products = saved.state.products.map { product ->
            val stock = product.stock + (restored[product.id] ?: 0)
            require(stock <= 1_000_000) { "Inventory is too large to restore this order. Reduce sample stock first." }
            product.copy(stock = stock)
        }
        saved.copy(state = saved.state.copy(products = products, orders = saved.state.orders.map { if (it.id == id) changedOrder else it }))
    }

    fun resetDemo(): String? = try {
        val next = SavedDemo(state = seedDemo(nowMillis().coerceAtLeast(0)))
        persist(next)
        errorOnLoad = null
        null
    } catch (_: Exception) {
        "The demo could not be reset. Existing local data was preserved. Please try again."
    }

    private fun requireProduct(id: String): DemoProduct = requireNotNull(saved.state.products.firstOrNull { it.id == id }) { "This product could not be found." }

    private inline fun change(build: () -> SavedDemo): String? {
        errorOnLoad?.let { return it }
        return try {
            val next = build()
            if (next != saved) persist(next)
            null
        } catch (invalid: IllegalArgumentException) {
            invalid.message ?: "Review the demo information and try again."
        } catch (_: Exception) {
            "Your change could not be saved. Existing demo data is unchanged. Please try again."
        }
    }

    private fun persist(next: SavedDemo) {
        validateSaved(next)
        storage.save(json.encodeToString(next))
        saved = next
        mutableState.value = next.state
    }
}

private fun normalizeAddress(address: DemoAddress): DemoAddress = address.copy(
    name = address.name.trim(), email = address.email.trim(), line1 = address.line1.trim(),
    city = address.city.trim(), state = address.state.trim().uppercase(), postalCode = address.postalCode.trim(),
)

private fun validateAddress(address: DemoAddress) {
    require(address.name.length in 2..100 && address.name.none { it.isISOControl() }) { "Enter the recipient's name." }
    require(address.email.length <= 160 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(address.email)) { "Enter a valid email address." }
    require(address.line1.length in 3..160 && address.line1.none { it.isISOControl() }) { "Enter a street address." }
    require(address.city.length in 2..100 && address.city.none { it.isISOControl() }) { "Enter a city." }
    require(address.state == "CA") { "Use CA for this fictional California shipping demonstration." }
    require(Regex("^[0-9]{5}(-[0-9]{4})?$").matches(address.postalCode)) { "Enter a five-digit ZIP code or ZIP+4." }
}

private fun validateProduct(product: DemoProduct) {
    require(Regex("^[A-Za-z0-9][A-Za-z0-9_-]{0,79}$").matches(product.id)) { "Use a product ID with letters, numbers, hyphens or underscores." }
    require(product.name.isNotBlank() && product.name.length <= 120 && product.name.none { it.isISOControl() }) { "Enter a product name of 120 characters or fewer." }
    require(product.category.isNotBlank() && product.category.length <= 60 && product.category.none { it.isISOControl() }) { "Enter a product category." }
    require(product.description.isNotBlank() && product.description.length <= 4_000) { "Enter a product description of 4,000 characters or fewer." }
    require(product.packSize in 1..1_000) { "Pack size must be between 1 and 1,000." }
    require(product.priceCents in 0..MAX_PRICE_CENTS) { "Enter a valid nonnegative demo price." }
    require(product.stock in 0..1_000_000) { "Stock must be between 0 and 1,000,000 packs." }
}

private fun isAllowedTransition(from: OrderStatus, to: OrderStatus): Boolean = when (from) {
    OrderStatus.CONFIRMED -> to == OrderStatus.PACKING || to == OrderStatus.CANCELLED
    OrderStatus.PACKING -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED
    OrderStatus.SHIPPED -> to == OrderStatus.DELIVERED || to == OrderStatus.REFUNDED
    OrderStatus.DELIVERED -> to == OrderStatus.REFUNDED
    OrderStatus.CANCELLED, OrderStatus.REFUNDED -> false
}

private fun validateSaved(snapshot: SavedDemo) {
    require(snapshot.version == 1)
    val state = snapshot.state
    require(state.products.size in 1..200 && state.cart.size <= 200 && state.orders.size <= 1_000 && state.customers.size in 1..200)
    require(state.products.map { it.id }.distinct().size == state.products.size)
    require(state.customers.map { it.id }.distinct().size == state.customers.size)
    require(state.orders.map { it.id }.distinct().size == state.orders.size)
    require(state.cart.map { it.productId }.distinct().size == state.cart.size)
    state.products.forEach(::validateProduct)
    val productIds = state.products.map { it.id }.toSet()
    state.cart.forEach { require(it.productId in productIds && it.quantity in 1..MAX_QUANTITY) }
    val customerIds = state.customers.map { it.id }.toSet()
    require(SHOPPER_ID in customerIds)
    state.customers.forEach {
        require(it.id.isNotBlank() && it.id.length <= 80 && it.name.isNotBlank() && it.name.length <= 100)
        require(it.email.length <= 160 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it.email))
    }
    validateAddress(state.profile)
    val shopper = state.customers.first { it.id == SHOPPER_ID }
    require(shopper.name == state.profile.name && shopper.email == state.profile.email)
    quoteCart(state)
    state.orders.forEach { order ->
        require(order.id.isNotBlank() && order.id.length <= 160 && order.customerId in customerIds)
        require(order.customerName.isNotBlank() && order.customerName.length <= 100)
        require(order.createdAt >= 0 && order.items.size in 1..200 && order.events.size in 1..20)
        validateAddress(order.shipping)
        require(order.items.map { it.productId }.distinct().size == order.items.size)
        order.items.forEach { line ->
            require(line.productId in productIds && line.quantity in 1..MAX_QUANTITY && line.unitPriceCents in 0..MAX_PRICE_CENTS)
            require(line.name.isNotBlank() && line.name.length <= 120 && line.packSize in 1..1_000)
        }
        val totals = quoteSubtotal(order.items.sumOf { it.unitPriceCents * it.quantity })
        require(order.subtotalCents == totals.subtotalCents && order.shippingCents == totals.shippingCents && order.taxCents == totals.taxCents && order.totalCents == totals.totalCents)
        require(order.events.first().status == OrderStatus.CONFIRMED && order.events.first().atMillis == order.createdAt)
        require(order.events.last().status == order.status)
        order.events.forEach { require(it.atMillis >= order.createdAt && it.note.length <= 500) }
        order.events.zipWithNext().forEach { (earlier, later) ->
            require(later.atMillis >= earlier.atMillis && isAllowedTransition(earlier.status, later.status))
        }
        require(order.tracking.length <= 120 && order.tracking.none { it.isISOControl() })
        if (order.events.any { it.status == OrderStatus.SHIPPED }) require(order.tracking.isNotBlank())
    }
    require(snapshot.completedCheckouts.size <= 1_000 && snapshot.completedCheckouts.map { it.requestId }.distinct().size == snapshot.completedCheckouts.size)
    snapshot.completedCheckouts.forEach { request ->
        require(request.requestId.isNotBlank() && request.requestId.length <= 160)
        val order = requireNotNull(state.orders.firstOrNull { it.id == request.orderId })
        require(order.customerId == SHOPPER_ID && order.shipping == request.address)
    }
}

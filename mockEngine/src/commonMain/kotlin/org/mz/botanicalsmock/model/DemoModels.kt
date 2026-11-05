package org.mz.botanicalsmock.model

import kotlinx.serialization.Serializable

/** Every record in this module is fictional and local to the demonstration app. */
@Serializable
data class DemoProduct(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val packSize: Int,
    val priceCents: Long,
    val stock: Int,
    val published: Boolean = true,
    val featured: Boolean = false,
)

@Serializable
data class DemoCartItem(val productId: String, val quantity: Int)

@Serializable
enum class CustomerApproval { PENDING, APPROVED, REJECTED }

@Serializable
data class DemoCustomer(
    val id: String,
    val name: String,
    val email: String,
    val approval: CustomerApproval,
)

@Serializable
enum class OrderStatus { CONFIRMED, PACKING, SHIPPED, DELIVERED, CANCELLED, REFUNDED }

enum class PaymentOutcome { APPROVED, DECLINED, TIMEOUT }

@Serializable
data class DemoOrderLine(
    val productId: String,
    val name: String,
    val packSize: Int,
    val quantity: Int,
    val unitPriceCents: Long,
)

@Serializable
data class DemoOrderEvent(val status: OrderStatus, val atMillis: Long, val note: String)

@Serializable
data class DemoAddress(
    val name: String,
    val email: String,
    val line1: String,
    val city: String,
    val state: String,
    val postalCode: String,
)

@Serializable
data class DemoOrder(
    val id: String,
    val customerId: String,
    val customerName: String,
    val items: List<DemoOrderLine>,
    val shipping: DemoAddress,
    val subtotalCents: Long,
    val shippingCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val status: OrderStatus,
    val createdAt: Long,
    val tracking: String,
    val events: List<DemoOrderEvent>,
)

@Serializable
data class DemoState(
    val products: List<DemoProduct>,
    val cart: List<DemoCartItem>,
    val orders: List<DemoOrder>,
    val customers: List<DemoCustomer>,
    val profile: DemoAddress,
)

data class CheckoutInput(val address: DemoAddress, val requestId: String)

data class CheckoutResult(val orderId: String? = null, val error: String? = null)

data class DemoTotals(
    val subtotalCents: Long,
    val shippingCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

interface DemoStorage {
    fun load(): String?
    /** Implementations must replace the saved value atomically, or throw without replacing it. */
    fun save(value: String)
}

/** Exact cents; no device locale or floating point is involved. */
fun formatMoney(cents: Long): String {
    require(cents != Long.MIN_VALUE) { "Amount is outside the supported range." }
    val absolute = if (cents < 0) -cents else cents
    val dollars = (absolute / 100).toString()
    val grouped = dollars.reversed().chunked(3).joinToString(",").reversed()
    return "${if (cents < 0) "-" else ""}\$$grouped.${(absolute % 100).toString().padStart(2, '0')}"
}

/** Fictional demonstration arithmetic. This is not a tax or carrier calculation. */
fun quoteCart(state: DemoState): DemoTotals {
    if (state.cart.isEmpty()) return DemoTotals(0, 0, 0, 0)
    val products = state.products.associateBy { it.id }
    val subtotal = state.cart.sumOf { line ->
        val product = requireNotNull(products[line.productId]) { "A cart product is unavailable." }
        require(line.quantity in 1..MAX_QUANTITY && product.priceCents in 0..MAX_PRICE_CENTS) {
            "A cart item has an invalid quantity or price."
        }
        product.priceCents * line.quantity
    }
    return quoteSubtotal(subtotal)
}

internal fun quoteSubtotal(subtotal: Long): DemoTotals {
    require(subtotal in 0..MAX_SUBTOTAL_CENTS) { "The demo order amount is too large." }
    val shipping = 500L
    val tax = (subtotal * 8 + 50) / 100
    return DemoTotals(subtotal, shipping, tax, subtotal + shipping + tax)
}

internal const val MAX_QUANTITY = 1_000
internal const val MAX_PRICE_CENTS = 100_000_000L
internal const val MAX_SUBTOTAL_CENTS = 20_000_000_000_000L
internal const val SHOPPER_ID = "demo-shopper"

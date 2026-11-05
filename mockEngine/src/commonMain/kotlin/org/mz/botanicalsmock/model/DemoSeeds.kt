package org.mz.botanicalsmock.model

/** No customer names, contact details or orders are copied from a live store. */
internal fun seedDemo(now: Long): DemoState {
    val products = listOf(
        DemoProduct("coastal-haze", "Coastal Haze", "Feminized", "A fictional coastal collection with bright citrus character and an easygoing botanical story. Sample genetics for a sales demonstration only.", 5, 4_000, 35, featured = true),
        DemoProduct("emerald-og", "Emerald OG", "Feminized", "Deep forest tones and a classic collection profile. This fictional cultivar illustrates detailed product descriptions and pack selection.", 5, 4_500, 24, featured = true),
        DemoProduct("sunset-bloom", "Sunset Bloom", "Autoflower", "Warm sunset color and a distinctive floral identity. A fictional autoflower collection created for the MZ Botanicals demo.", 3, 3_000, 40),
        DemoProduct("moonlit-mint", "Moonlit Mint", "Regular", "A cool, clean botanical profile with a distinctive mint-inspired identity. Fictional sample stock, pricing and genetics.", 10, 6_000, 18, featured = true),
        DemoProduct("golden-canopy", "Golden Canopy", "Autoflower", "A bright collection inspired by the golden California canopy. Sample content demonstrates catalog browsing and inventory editing.", 5, 4_200, 12),
        DemoProduct("wildflower-reserve", "Wildflower Reserve", "Regular", "A small fictional reserve collection with a rich botanical story. Use it to explore out-of-stock states and admin restocking.", 10, 7_500, 0),
    )
    val profile = DemoAddress("Alex Morgan (Demo)", "alex@example.com", "123 Demo Lane", "Sacramento", "CA", "95814")
    val customers = listOf(
        DemoCustomer(SHOPPER_ID, profile.name, profile.email, CustomerApproval.APPROVED),
        DemoCustomer("demo-jordan", "Jordan Lee (Demo)", "jordan@example.com", CustomerApproval.APPROVED),
        DemoCustomer("demo-sam", "Sam Rivera (Demo)", "sam@example.com", CustomerApproval.PENDING),
        DemoCustomer("demo-taylor", "Taylor Chen (Demo)", "taylor@example.com", CustomerApproval.REJECTED),
    )
    val day = 86_400_000L
    fun seededOrder(number: Int, customerId: String, productIndex: Int, quantity: Int, statuses: List<OrderStatus>, daysAgo: Int): DemoOrder {
        val customer = customers.first { it.id == customerId }
        val product = products[productIndex]
        val created = (now - daysAgo * day).coerceAtLeast(0)
        val quote = quoteSubtotal(product.priceCents * quantity)
        val finalStatus = statuses.last()
        return DemoOrder(
            id = "MZ-DEMO-${1000 + number}",
            customerId = customerId,
            customerName = customer.name,
            items = listOf(DemoOrderLine(product.id, product.name, product.packSize, quantity, product.priceCents)),
            shipping = profile.copy(name = customer.name, email = customer.email, line1 = "${100 + number} Example Avenue"),
            subtotalCents = quote.subtotalCents,
            shippingCents = quote.shippingCents,
            taxCents = quote.taxCents,
            totalCents = quote.totalCents,
            status = finalStatus,
            createdAt = created,
            tracking = if (statuses.contains(OrderStatus.SHIPPED)) "DEMO-TRACK-${1000 + number}" else "",
            events = statuses.mapIndexed { index, status ->
                DemoOrderEvent(status, created + index * 3_600_000L, seedEventNote(status))
            },
        )
    }
    return DemoState(
        products = products,
        cart = emptyList(),
        orders = listOf(
            seededOrder(8, SHOPPER_ID, 0, 1, listOf(OrderStatus.CONFIRMED), 0),
            seededOrder(7, SHOPPER_ID, 1, 2, listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING), 1),
            seededOrder(6, SHOPPER_ID, 2, 1, listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPED), 3),
            seededOrder(5, "demo-jordan", 3, 1, listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING), 4),
            seededOrder(4, SHOPPER_ID, 4, 1, listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPED, OrderStatus.DELIVERED), 7),
            seededOrder(3, "demo-jordan", 0, 2, listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED), 9),
            seededOrder(2, SHOPPER_ID, 3, 1, listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED), 12),
            seededOrder(1, SHOPPER_ID, 1, 1, listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.REFUNDED), 15),
        ),
        customers = customers,
        profile = profile,
    )
}

private fun seedEventNote(status: OrderStatus): String = when (status) {
    OrderStatus.CONFIRMED -> "Demo payment approved. No money was charged."
    OrderStatus.PACKING -> "Sample order is being prepared."
    OrderStatus.SHIPPED -> "Simulated dispatch. Tracking is fictional."
    OrderStatus.DELIVERED -> "Simulated delivery completed."
    OrderStatus.CANCELLED -> "Sample order cancelled and stock returned."
    OrderStatus.REFUNDED -> "Simulated refund and inventory return. No money was moved."
}

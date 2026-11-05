package org.mz.botanicalsmock.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DemoStoreTest {
    private val clock = 1_793_750_400_000L

    private class MemoryStorage(var value: String? = null) : DemoStorage {
        var failWrites = false
        var writes = 0
        override fun load(): String? = value
        override fun save(value: String) {
            if (failWrites) error("Simulated storage failure")
            this.value = value
            writes++
        }
    }

    private fun store(storage: MemoryStorage = MemoryStorage()) = DemoStore(storage) { clock }
    private fun DemoStore.input(request: String = "checkout-one") = CheckoutInput(state.value.profile, request)
    private fun DemoStore.product(id: String = "coastal-haze") = state.value.products.first { it.id == id }
    private fun DemoStore.order(id: String) = state.value.orders.first { it.id == id }

    @Test
    fun seedsAreFictionalAndCoverTheEntireOrderLifecycle() {
        val store = store()
        assertNull(store.loadError)
        assertEquals(6, store.state.value.products.size)
        assertTrue(store.state.value.customers.all { it.email.endsWith("@example.com") })
        assertEquals(OrderStatus.entries.toSet(), store.state.value.orders.map { it.status }.toSet())
        assertEquals(emptyList(), store.state.value.cart)
        assertTrue(store.state.value.products.any { it.stock == 0 })
        assertTrue(store.state.value.orders.all { it.id.startsWith("MZ-DEMO-") })
    }

    @Test
    fun addingUpdatingAndRemovingCartArePersistent() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze", 2))
        assertNull(store.addToCart("coastal-haze", 1))
        assertEquals(listOf(DemoCartItem("coastal-haze", 3)), store.state.value.cart)
        assertNull(store.updateCart("coastal-haze", 5))
        assertEquals(store.state.value, store(storage).state.value)
        assertNull(store.updateCart("coastal-haze", 0))
        assertTrue(store(storage).state.value.cart.isEmpty())
        assertNull(store.addToCart("emerald-og"))
        assertNull(store.removeFromCart("emerald-og"))
        assertTrue(store.state.value.cart.isEmpty())
    }

    @Test
    fun stockAndQuantityBoundsCannotBeBypassed() {
        val store = store()
        val original = store.state.value
        assertNotNull(store.addToCart("missing"))
        assertNotNull(store.addToCart("wildflower-reserve"))
        assertNotNull(store.addToCart("coastal-haze", 0))
        assertNotNull(store.addToCart("coastal-haze", -1))
        assertNotNull(store.addToCart("coastal-haze", Int.MAX_VALUE))
        assertNotNull(store.addToCart("coastal-haze", 36))
        assertEquals(original, store.state.value)
        assertNull(store.addToCart("coastal-haze", 35))
        assertNotNull(store.addToCart("coastal-haze", 1))
        assertNotNull(store.updateCart("coastal-haze", -1))
        assertEquals(35, store.state.value.cart.single().quantity)
    }

    @Test
    fun quoteUsesIntegerCentsAndDeterministicRounding() {
        val store = store()
        assertEquals(DemoTotals(0, 0, 0, 0), quoteCart(store.state.value))
        assertNull(store.saveProduct(store.product().copy(priceCents = 333)))
        assertNull(store.addToCart("coastal-haze", 3))
        assertEquals(DemoTotals(999, 500, 80, 1_579), quoteCart(store.state.value))
        assertEquals("$15.79", formatMoney(1_579))
        assertEquals("$0.00", formatMoney(0))
        assertEquals("$1,234,567.89", formatMoney(123_456_789))
        assertEquals("-$0.01", formatMoney(-1))
    }

    @Test
    fun successfulCheckoutSavesOneOrderAndDeductsInventory() {
        val storage = MemoryStorage()
        val store = store(storage)
        val stock = store.product().stock
        val previousOrders = store.state.value.orders.size
        assertNull(store.addToCart("coastal-haze", 2))
        val quote = quoteCart(store.state.value)
        val input = store.input().copy(address = store.state.value.profile.copy(name = " Demo Buyer ", city = " Sacramento ", state = " ca "))
        val result = store.checkout(input, PaymentOutcome.APPROVED)
        assertNull(result.error)
        val id = assertNotNull(result.orderId)
        val order = store.order(id)
        assertEquals(previousOrders + 1, store.state.value.orders.size)
        assertEquals(stock - 2, store.product().stock)
        assertTrue(store.state.value.cart.isEmpty())
        assertEquals(quote.totalCents, order.totalCents)
        assertEquals(OrderStatus.CONFIRMED, order.status)
        assertEquals("Demo Buyer", order.shipping.name)
        assertEquals("CA", order.shipping.state)
        assertEquals(order.shipping, store.state.value.profile)
        assertEquals("Demo Buyer", store.state.value.customers.first { it.id == "demo-shopper" }.name)
        assertEquals(store.state.value, store(storage).state.value)
    }

    @Test
    fun successfulCheckoutRetriesRemainIdempotentAfterRestart() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        val input = store.input()
        val first = store.checkout(input, PaymentOutcome.APPROVED)
        val stateAfterFirst = store.state.value
        val writesAfterFirst = storage.writes
        assertEquals(first, store.checkout(input, PaymentOutcome.APPROVED))
        assertEquals(stateAfterFirst, store.state.value)
        assertEquals(writesAfterFirst, storage.writes)
        val reopened = store(storage)
        assertEquals(first, reopened.checkout(input, PaymentOutcome.APPROVED))
        assertEquals(stateAfterFirst, reopened.state.value)
        assertNotNull(reopened.checkout(input.copy(address = input.address.copy(line1 = "456 Different Avenue")), PaymentOutcome.APPROVED).error)
        assertEquals(stateAfterFirst, reopened.state.value)
    }

    @Test
    fun failedPaymentOutcomesPreserveCartStockAndAllowRetry() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("emerald-og", 2))
        val before = store.state.value
        val writesBefore = storage.writes
        val input = store.input()
        for (outcome in listOf(PaymentOutcome.DECLINED, PaymentOutcome.TIMEOUT)) {
            val result = store.checkout(input, outcome)
            assertNull(result.orderId)
            assertNotNull(result.error)
            assertEquals(before, store.state.value)
            assertEquals(writesBefore, storage.writes)
        }
        assertNotNull(store.checkout(input, PaymentOutcome.APPROVED).orderId)
    }

    @Test
    fun checkoutStorageFailureDoesNotCommitAnOrderOrConsumeRequest() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        val before = store.state.value
        val encodedBefore = storage.value
        storage.failWrites = true
        val input = store.input()
        assertNotNull(store.checkout(input, PaymentOutcome.APPROVED).error)
        assertEquals(before, store.state.value)
        assertEquals(encodedBefore, storage.value)
        storage.failWrites = false
        assertNotNull(store.checkout(input, PaymentOutcome.APPROVED).orderId)
        assertEquals(before.orders.size + 1, store.state.value.orders.size)
    }

    @Test
    fun checkoutRevalidatesAdminStockAndPublishingChanges() {
        val store = store()
        assertNull(store.addToCart("coastal-haze", 2))
        assertNull(store.saveProduct(store.product().copy(stock = 1)))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).error)
        assertEquals(2, store.state.value.cart.single().quantity)
        assertNull(store.saveProduct(store.product().copy(stock = 10, published = false)))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).error)
        assertNull(store.saveProduct(store.product().copy(published = true)))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).orderId)
    }

    @Test
    fun invalidAddressesAndEmptyCartNeverCreateOrders() {
        val store = store()
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).error)
        assertNull(store.addToCart("coastal-haze"))
        val before = store.state.value
        val address = store.state.value.profile
        val invalid = listOf(
            address.copy(name = ""), address.copy(email = "not-email"), address.copy(email = "a@b c.com"),
            address.copy(line1 = ""), address.copy(city = ""), address.copy(state = "NY"),
            address.copy(postalCode = "ABCDE"), address.copy(postalCode = "123"),
        )
        invalid.forEachIndexed { index, bad ->
            assertNotNull(store.checkout(CheckoutInput(bad, "bad-$index"), PaymentOutcome.APPROVED).error)
            assertEquals(before, store.state.value)
        }
        assertNotNull(store.checkout(CheckoutInput(address, ""), PaymentOutcome.APPROVED).error)
        assertEquals(before, store.state.value)
    }

    @Test
    fun customerApprovalChangesGateCheckoutAndPersist() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        assertNull(store.reviewCustomer("demo-shopper", CustomerApproval.PENDING))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).error)
        assertNull(store.reviewCustomer("demo-shopper", CustomerApproval.REJECTED))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).error)
        assertEquals(CustomerApproval.REJECTED, store(storage).state.value.customers.first { it.id == "demo-shopper" }.approval)
        assertNull(store.reviewCustomer("demo-shopper", CustomerApproval.APPROVED))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).orderId)
        assertNotNull(store.reviewCustomer("missing", CustomerApproval.APPROVED))
    }

    @Test
    fun fulfillmentHasGuardedTransitionsAndPersistentTrackingTimeline() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        val id = assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).orderId)
        val stockAfterCheckout = store.product().stock
        assertNotNull(store.transitionOrder(id, OrderStatus.SHIPPED))
        assertEquals(OrderStatus.CONFIRMED, store.order(id).status)
        assertNull(store.transitionOrder(id, OrderStatus.PACKING))
        assertNull(store.transitionOrder(id, OrderStatus.SHIPPED, "DEMO-TRACK-42"))
        assertEquals("DEMO-TRACK-42", store.order(id).tracking)
        assertNotNull(store.transitionOrder(id, OrderStatus.PACKING))
        assertNull(store.transitionOrder(id, OrderStatus.DELIVERED))
        val order = store(storage).order(id)
        assertEquals(listOf(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPED, OrderStatus.DELIVERED), order.events.map { it.status })
        assertEquals(stockAfterCheckout, store.product().stock)
    }

    @Test
    fun cancelRestoresInventoryOnceIncludingAfterRestart() {
        val storage = MemoryStorage()
        val store = store(storage)
        val originalStock = store.product().stock
        assertNull(store.addToCart("coastal-haze", 3))
        val id = assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).orderId)
        assertNull(store.transitionOrder(id, OrderStatus.CANCELLED))
        assertEquals(originalStock, store.product().stock)
        val writesAfter = storage.writes
        assertNull(store.transitionOrder(id, OrderStatus.CANCELLED))
        assertEquals(originalStock, store.product().stock)
        assertEquals(writesAfter, storage.writes)
        val reopened = store(storage)
        assertNull(reopened.transitionOrder(id, OrderStatus.CANCELLED))
        assertEquals(originalStock, reopened.product().stock)
        assertNotNull(reopened.transitionOrder(id, OrderStatus.REFUNDED))
        assertNotNull(reopened.transitionOrder(id, OrderStatus.PACKING))
    }

    @Test
    fun refundRestoresInventoryOnceAndNeverCreatesRealPaymentActivity() {
        val store = store()
        val stock = store.product().stock
        assertNull(store.addToCart("coastal-haze", 2))
        val id = assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).orderId)
        assertNull(store.transitionOrder(id, OrderStatus.PACKING))
        assertNull(store.transitionOrder(id, OrderStatus.SHIPPED))
        assertTrue(store.order(id).tracking.startsWith("DEMO-TRACK-"))
        assertNull(store.transitionOrder(id, OrderStatus.DELIVERED))
        assertNull(store.transitionOrder(id, OrderStatus.REFUNDED))
        assertEquals(stock, store.product().stock)
        assertNull(store.transitionOrder(id, OrderStatus.REFUNDED))
        assertEquals(stock, store.product().stock)
        assertTrue(store.order(id).events.last().note.contains("No money was moved"))
        assertNotNull(store.transitionOrder(id, OrderStatus.CANCELLED))
    }

    @Test
    fun orderSnapshotsKeepOriginalProductPriceAndPackAfterAdminEdits() {
        val store = store()
        assertNull(store.addToCart("coastal-haze", 2))
        val id = assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).orderId)
        val originalOrder = store.order(id)
        assertNull(store.saveProduct(store.product().copy(name = "Renamed Sample", priceCents = 9_999, packSize = 10)))
        assertEquals(originalOrder, store.order(id))
    }

    @Test
    fun adminCanCreateAndPublishProductButCannotSaveInvalidValues() {
        val storage = MemoryStorage()
        val store = store(storage)
        val newProduct = DemoProduct("new-demo", "New Demo", "Regular", "A fictional new product.", 3, 2_500, 10, published = false)
        assertNull(store.saveProduct(newProduct))
        assertNotNull(store.addToCart(newProduct.id))
        assertNull(store.saveProduct(newProduct.copy(published = true)))
        assertNull(store.addToCart(newProduct.id))
        assertEquals(store.state.value, store(storage).state.value)
        val before = store.state.value
        listOf(
            newProduct.copy(priceCents = -1), newProduct.copy(stock = -1), newProduct.copy(packSize = 0),
            newProduct.copy(priceCents = Long.MAX_VALUE), newProduct.copy(stock = Int.MAX_VALUE),
            newProduct.copy(id = "../path"), newProduct.copy(name = ""),
        ).forEach { invalid ->
            assertNotNull(store.saveProduct(invalid))
            assertEquals(before, store.state.value)
        }
    }

    @Test
    fun ordinarySaveFailureLeavesCurrentAndPersistedStateUnchanged() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        val before = store.state.value
        val encodedBefore = storage.value
        storage.failWrites = true
        assertNotNull(store.saveProduct(store.product().copy(stock = 99)))
        assertNotNull(store.saveProfile(before.profile.copy(name = "Changed Shopper")))
        assertNotNull(store.clearCart())
        assertEquals(before, store.state.value)
        assertEquals(encodedBefore, storage.value)
    }

    @Test
    fun corruptLocalDataIsPreservedAndBlocksAllWritesUntilExplicitReset() {
        val storage = MemoryStorage("{ broken saved data")
        val store = store(storage)
        assertNotNull(store.loadError)
        assertNotNull(store.addToCart("coastal-haze"))
        assertNotNull(store.saveProduct(store.product().copy(stock = 99)))
        assertNotNull(store.reviewCustomer("demo-shopper", CustomerApproval.APPROVED))
        assertNotNull(store.checkout(store.input(), PaymentOutcome.APPROVED).error)
        assertEquals("{ broken saved data", storage.value)
        assertEquals(0, storage.writes)
        storage.failWrites = true
        assertNotNull(store.resetDemo())
        assertNotNull(store.loadError)
        assertEquals("{ broken saved data", storage.value)
        storage.failWrites = false
        assertNull(store.resetDemo())
        assertNull(store.loadError)
        assertNull(store.addToCart("coastal-haze"))
        assertNull(store(storage).loadError)
    }

    @Test
    fun structurallyValidButTamperedFinancialDataIsRejected() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        val valid = assertNotNull(storage.value)
        storage.value = valid.replaceFirst("\"totalCents\":", "\"totalCents\":999999,\"previousTotal\":")
        val tampered = storage.value
        val reopened = store(storage)
        assertNotNull(reopened.loadError)
        assertNotNull(reopened.clearCart())
        assertEquals(tampered, storage.value)
    }

    @Test
    fun unknownStorageVersionIsNotSilentlyOverwritten() {
        val storage = MemoryStorage()
        val store = store(storage)
        assertNull(store.addToCart("coastal-haze"))
        storage.value = assertNotNull(storage.value).replaceFirst("\"version\":1", "\"version\":99")
        val before = storage.value
        val reopened = store(storage)
        assertNotNull(reopened.loadError)
        assertEquals(before, storage.value)
    }

    @Test
    fun resetRestoresSamplesClearsNewOrdersAndResetsCheckoutHistory() {
        val storage = MemoryStorage()
        val store = store(storage)
        val original = store.state.value
        assertNull(store.addToCart("coastal-haze", 2))
        val input = store.input()
        assertNotNull(store.checkout(input, PaymentOutcome.APPROVED).orderId)
        assertNull(store.reviewCustomer("demo-shopper", CustomerApproval.REJECTED))
        assertNull(store.resetDemo())
        assertEquals(original, store.state.value)
        assertEquals(original, store(storage).state.value)
        assertNull(store.addToCart("coastal-haze"))
        assertNotNull(store.checkout(input, PaymentOutcome.APPROVED).orderId)
        assertFalse(store.state.value.cart.isNotEmpty())
    }
}

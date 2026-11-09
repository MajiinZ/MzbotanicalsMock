package org.mz.botanicalsmock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.mz.botanicalsmock.model.DemoStore

internal enum class DemoPage { Welcome, Home, Explore, Category, Cart, Checkout, Orders, Order, Product, Admin, Profile, Settings }

@Composable
fun MockApp(store: DemoStore) {
    val state by store.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(DemoPage.Welcome) }
    var productId by rememberSaveable { mutableStateOf("") }
    var productBack by rememberSaveable { mutableStateOf(DemoPage.Home) }
    var orderId by rememberSaveable { mutableStateOf("") }
    var orderBack by rememberSaveable { mutableStateOf(DemoPage.Orders) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf(store.loadError) }

    fun navigate(destination: DemoPage) {
        drawerOpen = false
        notice = null
        if (destination == DemoPage.Home || destination == DemoPage.Explore) category = null
        page = destination
    }
    fun openProduct(id: String) {
        productBack = page
        productId = id
        navigate(DemoPage.Product)
    }
    fun back() {
        if (drawerOpen) { drawerOpen = false; return }
        navigate(when (page) {
            DemoPage.Product -> productBack
            DemoPage.Checkout -> DemoPage.Cart
            DemoPage.Order -> orderBack
            DemoPage.Category -> DemoPage.Explore
            else -> DemoPage.Home
        })
    }

    PlatformBackHandler(drawerOpen || (page != DemoPage.Home && page != DemoPage.Welcome), ::back)
    MockTheme {
        if (page == DemoPage.Welcome) {
            MockWelcomeScreen { navigate(DemoPage.Home) }
        } else {
            MockChrome(
                page = page,
                cartCount = state.cart.sumOf { it.quantity },
                profileName = state.profile.name,
                drawerOpen = drawerOpen,
                onDrawerChange = { drawerOpen = it },
                onNavigate = ::navigate,
                onBack = ::back,
            ) {
                Column(Modifier.fillMaxSize()) {
                    notice?.let { DemoMessage(it) }
                    key(page, productId, orderId, category) {
                        when (page) {
                            DemoPage.Home -> ShopScreen(state, store, ::openProduct, { navigate(DemoPage.Cart) })
                            DemoPage.Explore -> ExploreScreen(state) { selected ->
                                category = selected
                                navigate(DemoPage.Category)
                            }
                            DemoPage.Category -> ShopScreen(state, store, ::openProduct,
                                { navigate(DemoPage.Cart) }, initialCategory = category)
                            DemoPage.Product -> ProductScreen(productId, state, store, ::back, { navigate(DemoPage.Cart) })
                            DemoPage.Cart -> CartScreen(state, store, { navigate(DemoPage.Home) },
                                { navigate(DemoPage.Checkout) }, onProduct = ::openProduct)
                            DemoPage.Checkout -> CheckoutScreen(state, store, ::back) {
                                orderId = it
                                orderBack = DemoPage.Orders
                                navigate(DemoPage.Order)
                                notice = "Demo order placed. No real payment was processed."
                            }
                            DemoPage.Orders -> OrdersScreen(state, {
                                orderId = it; orderBack = DemoPage.Orders; navigate(DemoPage.Order)
                            }, { navigate(DemoPage.Home) })
                            DemoPage.Order -> OrderScreen(orderId, state, store, ::back)
                            DemoPage.Admin -> AdminScreen(state, store) {
                                orderId = it; orderBack = DemoPage.Admin; navigate(DemoPage.Order)
                            }
                            DemoPage.Profile -> ProfileScreen(state, store)
                            DemoPage.Settings -> DemoSettingsScreen(
                                onProfile = { navigate(DemoPage.Profile) },
                                onOrders = { navigate(DemoPage.Orders) },
                                onWelcome = { navigate(DemoPage.Welcome) }
                            )
                            DemoPage.Welcome -> Unit
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
                        if (result == null) { loadError = null; navigate(DemoPage.Home) } else loadError = result
                    }) { Text("Reset demo data") }
                }
            )
        }
    }
}

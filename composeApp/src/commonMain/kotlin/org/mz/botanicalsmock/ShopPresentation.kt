package org.mz.botanicalsmock

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.mz.botanicalsmock.model.*

@Composable
fun ShopScreen(
    state: DemoState,
    store: DemoStore,
    onProduct: (String) -> Unit,
    onCart: () -> Unit,
    initialCategory: String? = null,
) {
    var query by remember(initialCategory) { mutableStateOf("") }
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    val collection = state.products.filter { it.published }.distinctBy { it.id }
    val newProducts = collection.filter { it.featured }.take(6)
    val categories = collection.map { it.category }.distinct().sorted()
    val category = selectedCategory?.takeIf { it in categories }
    val shown = collection.filter {
        (category == null || it.category == category) &&
            (query.isBlank() || it.name.contains(query.trim(), true) || it.description.contains(query.trim(), true))
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val toolsIndex = if (newProducts.isEmpty()) 1 else 3
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1280.dp).fillMaxSize()) {
            val expanded = maxWidth >= 840.dp
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val twoColumns = maxWidth >= 360.dp && fontScale <= 1.15f
            LazyVerticalGrid(
                state = gridState,
                columns = when {
                    expanded -> GridCells.Adaptive(300.dp * fontScale)
                    twoColumns -> GridCells.Fixed(2)
                    else -> GridCells.Fixed(1)
                },
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = if (expanded) 24.dp else 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (expanded) 20.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "introduction", span = { GridItemSpan(maxLineSpan) }) {
                    CollectionIntroduction(expanded) { scope.launch { gridState.animateScrollToItem(toolsIndex) } }
                }
                if (newProducts.isNotEmpty()) {
                    item(key = "new-heading", span = { GridItemSpan(maxLineSpan) }) {
                        CatalogSectionHeading("JUST ADDED", "New arrivals")
                    }
                    item(key = "new-arrivals", span = { GridItemSpan(maxLineSpan) }) {
                        NewArrivals(newProducts, expanded, onProduct)
                    }
                }
                item(key = "tools", span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CatalogSectionHeading("THE FULL COLLECTION", "Explore the collection",
                            "${shown.size} ${if (shown.size == 1) "genetic" else "genetics"}")
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search genetics") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = BrandGreen) },
                            trailingIcon = if (query.isNotEmpty()) {
                                { TextButton(onClick = { query = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear") } }
                            } else null,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen, unfocusedBorderColor = BrandOutline,
                                focusedContainerColor = BrandPaper, unfocusedContainerColor = BrandPaper,
                            ),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { CollectionFilter("All genetics", category == null) { selectedCategory = null } }
                            items(categories, key = { it }) { item ->
                                CollectionFilter(item, category == item) { selectedCategory = item }
                            }
                        }
                    }
                }
                if (shown.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        DemoCard(Modifier.fillMaxWidth()) {
                            Text("No matches this time", fontFamily = BrandDisplayFont, fontSize = 26.sp)
                            Text("Try another name or category to explore more of the collection.", color = BrandTextMuted)
                            TextButton(onClick = { query = ""; selectedCategory = null }) { Text("Clear filters") }
                        }
                    }
                }
                items(shown, key = { "product-${it.id}" }) { product ->
                    ShopProductCard(product) { onProduct(product.id) }
                }
                item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("End of collection", Modifier.weight(1f), color = BrandTextMuted, fontSize = 12.sp)
                        TextButton(onClick = { scope.launch { gridState.animateScrollToItem(0) } }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Back to top")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionIntroduction(expanded: Boolean, onExplore: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(if (expanded) 28.dp else 22.dp))
        .background(Brush.horizontalGradient(listOf(BrandDeep, BrandGreen)))) {
        BotanicalLinework(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth(if (expanded) 0.73f else 1f).padding(if (expanded) 32.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("THE MZ BOTANICALS COLLECTION", color = BrandCream.copy(alpha = 0.8f), fontFamily = BrandBodyFont,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
            Text("Discover your\nnext grow.", color = BrandCream, fontFamily = BrandDisplayFont,
                fontSize = if (expanded) 48.sp else 34.sp, lineHeight = if (expanded) 49.sp else 36.sp)
            Text("Explore seed packs and find your next selection.", color = BrandCream.copy(alpha = 0.86f),
                fontFamily = BrandBodyFont, fontSize = 14.sp, lineHeight = 20.sp)
            Button(onClick = onExplore, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandCream, contentColor = BrandDeep)) {
                Text("Explore collection", fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text("↗", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun BotanicalLinework(modifier: Modifier) {
    Canvas(modifier) {
        val stem = Path().apply {
            moveTo(size.width * 0.78f, size.height * 1.15f)
            cubicTo(size.width * 0.66f, size.height * 0.65f, size.width * 0.95f, size.height * 0.38f,
                size.width * 0.82f, -size.height * 0.12f)
        }
        drawPath(stem, BrandGreenSoft.copy(alpha = 0.20f), style = Stroke(width = 1.5.dp.toPx()))
        repeat(5) { index ->
            val y = size.height * (0.10f + index * 0.19f)
            val x = size.width * (if (index % 2 == 0) 0.81f else 0.86f)
            val direction = if (index % 2 == 0) -1f else 1f
            val leaf = Path().apply {
                moveTo(x, y + size.height * 0.12f)
                cubicTo(x + direction * size.width * 0.03f, y - size.height * 0.05f,
                    x + direction * size.width * 0.15f, y - size.height * 0.10f,
                    x + direction * size.width * 0.18f, y - size.height * 0.11f)
                cubicTo(x + direction * size.width * 0.16f, y + size.height * 0.08f,
                    x + direction * size.width * 0.06f, y + size.height * 0.16f, x, y + size.height * 0.12f)
                close()
            }
            drawPath(leaf, BrandGreenSoft.copy(alpha = 0.07f))
            drawPath(leaf, BrandGreenSoft.copy(alpha = 0.14f), style = Stroke(width = 1.dp.toPx()))
        }
        drawOval(BrandGold.copy(alpha = 0.22f), Offset(size.width * 0.88f, -size.height * 0.55f),
            Size(size.width * 0.4f, size.height * 1.4f), style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun CatalogSectionHeading(eyebrow: String, title: String, trailing: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(eyebrow, color = BrandTextMuted, fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp, letterSpacing = 1.6.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), color = BrandGreen, fontFamily = BrandDisplayFont, fontSize = 28.sp, lineHeight = 31.sp)
            trailing?.let { Text(it, color = BrandTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun CollectionFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        label = { Text(label, fontFamily = BrandBodyFont, fontSize = 13.sp) },
        modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(containerColor = BrandPaper, labelColor = BrandGreen,
            selectedContainerColor = BrandGreen, selectedLabelColor = BrandCream))
}

@Composable
private fun NewArrivals(products: List<DemoProduct>, expanded: Boolean, onProduct: (String) -> Unit) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val cardHeight = 238.dp + 100.dp * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = if (expanded) 340.dp else (maxWidth * 0.85f).coerceAtMost(340.dp)
            LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(products, key = { it.id }) { product ->
                    FeaturedProduct(product, expanded, Modifier.width(cardWidth).height(cardHeight)) { onProduct(product.id) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${products.size} ${if (products.size == 1) "new arrival" else "new arrivals"}",
                Modifier.weight(1f), color = BrandTextMuted, fontSize = 12.sp)
            TextButton(enabled = state.canScrollBackward, modifier = Modifier.heightIn(min = 48.dp),
                onClick = { scope.launch { state.animateScrollToItem((state.firstVisibleItemIndex - 1).coerceAtLeast(0)) } }) { Text("Previous") }
            TextButton(enabled = state.canScrollForward, modifier = Modifier.heightIn(min = 48.dp),
                onClick = { scope.launch { state.animateScrollToItem((state.firstVisibleItemIndex + 1).coerceAtMost(products.lastIndex)) } }) { Text("Next") }
        }
    }
}

@Composable
private fun FeaturedProduct(product: DemoProduct, expanded: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier.clip(shape).background(BrandDeep).border(1.dp, BrandOutline.copy(alpha = 0.7f), shape)
        .clickable(role = Role.Button, onClickLabel = "View ${product.name}", onClick = onClick)) {
        DemoArt(product.id, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.06f),
            0.28f to Color.Black.copy(alpha = 0.12f), 1f to BrandDeep.copy(alpha = 0.98f))))
        Text("NEW ARRIVAL", Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(9.dp))
            .background(BrandCream).padding(horizontal = 10.dp, vertical = 6.dp), color = BrandDeep,
            fontFamily = BrandBodyFont, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(product.category.uppercase(), color = BrandCream.copy(alpha = 0.86f), fontSize = 10.sp, letterSpacing = 1.6.sp)
            Text(product.name, color = BrandCream, fontFamily = BrandDisplayFont, fontSize = if (expanded) 28.sp else 26.sp,
                lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                Text(if (product.stock <= 0) "Out of stock" else "${product.packSize} seeds / pack", Modifier.weight(1f),
                    color = BrandCream.copy(alpha = 0.86f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatMoney(product.priceCents), color = BrandCream, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ShopProductCard(product: DemoProduct, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(if (hovered || focused) BrandGreen else BrandOutline)
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(BrandPaper).border(1.dp, borderColor, shape)
        .hoverable(interaction).clickable(interactionSource = interaction, indication = LocalIndication.current,
            role = Role.Button, onClickLabel = "View ${product.name}", onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(BrandGreenSoft)) {
            DemoArt(product.id, Modifier.matchParentSize())
            ProductCategoryLabel(product.category, Modifier.align(Alignment.TopStart).padding(10.dp))
            if (product.stock <= 0) ProductCategoryLabel("Out of stock", Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(product.name, color = BrandDeep, fontFamily = BrandBodyFont, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, lineHeight = 22.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${product.packSize} seeds per pack", color = BrandTextMuted, fontSize = 12.sp,
                lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatMoney(product.priceCents), color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp)
            Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("View details", color = BrandTextMuted, fontSize = 12.sp)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = BrandGreen)
            }
        }
    }
}

@Composable
private fun ProductCategoryLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.clip(RoundedCornerShape(8.dp)).background(BrandPaper).padding(horizontal = 9.dp, vertical = 5.dp),
        color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun ProductScreen(productId: String, state: DemoState, store: DemoStore, onBack: () -> Unit, onCart: () -> Unit) {
    val product = state.products.firstOrNull { it.id == productId }
    var quantity by remember(productId) { mutableStateOf(1) }
    var message by remember(productId) { mutableStateOf<String?>(null) }
    var enlarged by remember(productId) { mutableStateOf(false) }
    if (product == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            DemoCard { DemoHeading("This selection is unavailable", "Return to the collection to choose another."); DemoPrimary("Explore collection", onBack) }
        }
        return
    }
    if (enlarged) AlertDialog(onDismissRequest = { enlarged = false }, containerColor = BrandPaper,
        title = { Text(product.name, fontFamily = BrandDisplayFont) },
        text = { DemoArt(product.id, Modifier.fillMaxWidth().heightIn(max = 420.dp).aspectRatio(1f)) },
        confirmButton = { TextButton(onClick = { enlarged = false }) { Text("Close") } })
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1200.dp).fillMaxSize()) {
            val expanded = maxWidth >= 840.dp
            val safeQuantity = quantity.coerceIn(1, product.stock.coerceIn(1, 99))
            val purchase: @Composable () -> Unit = {
                ProductPurchaseDetails(product, safeQuantity, expanded,
                    onQuantity = { quantity = it },
                    onAdd = { message = store.addToCart(product.id, safeQuantity) ?: "Added to your cart." },
                    onCart = onCart, message = message)
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = if (expanded) 28.dp else 16.dp).padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    Column(Modifier.weight(1.08f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        ProductPhoto(product) { enlarged = true }
                        ProductOverview(product)
                    }
                    Column(Modifier.weight(1f)) { purchase() }
                } else {
                    ProductPhoto(product) { enlarged = true }
                    purchase()
                    ProductOverview(product)
                }
            }
        }
    }
}

@Composable
private fun ProductPhoto(product: DemoProduct, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1.12f).clip(RoundedCornerShape(28.dp)).background(BrandGreenSoft)
        .clickable(role = Role.Button, onClickLabel = "Enlarge ${product.name} image", onClick = onClick)) {
        DemoArt(product.id, Modifier.matchParentSize())
        Surface(Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = RoundedCornerShape(50), color = BrandPaper.copy(alpha = 0.94f)) {
            Text("View image", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductPurchaseDetails(product: DemoProduct, quantity: Int, expanded: Boolean, onQuantity: (Int) -> Unit,
    onAdd: () -> Unit, onCart: () -> Unit, message: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("MZ BOTANICALS", color = BrandTextMuted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        Text(product.name, color = BrandDeep, fontFamily = BrandDisplayFont, fontSize = if (expanded) 42.sp else 34.sp,
            lineHeight = if (expanded) 47.sp else 39.sp, fontWeight = FontWeight.Medium, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailPill(product.category)
            if (product.featured) DetailPill("New arrival")
            DetailPill(when { !product.published -> "Currently unavailable"; product.stock > 0 -> "In stock"; else -> "Out of stock" })
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatMoney(product.priceCents), color = BrandDeep, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("per pack", Modifier.padding(bottom = 3.dp), color = BrandTextMuted, fontSize = 14.sp)
        }
        Surface(shape = RoundedCornerShape(24.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandOutline)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionLabel("PACK SIZE")
                    Surface(shape = RoundedCornerShape(12.dp), color = BrandGreenSoft, border = BorderStroke(1.dp, BrandGreen)) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = BrandGreen)
                            Text("${product.packSize} seeds per pack", color = BrandGreen, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionLabel("QUANTITY · PACKS")
                    ShopQuantity(quantity, { onQuantity(quantity - 1) }, { onQuantity(quantity + 1) }, quantity > 1, quantity < product.stock.coerceAtMost(99))
                }
                HorizontalDivider(color = BrandOutline)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Item total", color = BrandGreen, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("$quantity ${if (quantity == 1) "pack" else "packs"}", color = BrandTextMuted, fontSize = 12.sp)
                    }
                    Text(formatMoney(product.priceCents * quantity), color = BrandDeep, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(when { !product.published -> "This selection is currently unavailable."; product.stock <= 0 -> "This selection is currently out of stock.";
                    else -> "${product.packSize * quantity} seeds in your selection." },
                    color = if (!product.published || product.stock <= 0) MaterialTheme.colorScheme.error else BrandTextMuted,
                    fontSize = 13.sp, lineHeight = 19.sp)
                DemoPrimary("Add to cart", onAdd, enabled = product.published && product.stock > 0, modifier = Modifier.fillMaxWidth())
                Text("Shipping and tax are not included in this item total.", color = BrandTextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                message?.let {
                    DemoMessage(it)
                    TextButton(onClick = onCart, modifier = Modifier.heightIn(min = 48.dp)) { Text("View cart") }
                }
            }
        }
    }
}

@Composable
private fun DetailPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = BrandGreenSoft) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = BrandGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectionLabel(label: String) {
    Text(label, color = BrandTextMuted, fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ProductOverview(product: DemoProduct) {
    Surface(shape = RoundedCornerShape(24.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandOutline)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("The details", color = BrandDeep, fontFamily = BrandDisplayFont, fontSize = 26.sp, modifier = Modifier.semantics { heading() })
            Text(product.description, color = BrandGreen, fontSize = 15.sp, lineHeight = 24.sp)
            HorizontalDivider(color = BrandOutline)
            Text("${product.packSize} seeds per pack · ${product.category}", color = BrandTextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun CartScreen(state: DemoState, store: DemoStore, onShop: () -> Unit, onCheckout: () -> Unit, onProduct: (String) -> Unit = {}) {
    var message by remember { mutableStateOf<String?>(null) }
    val totals = quoteCart(state)
    val ready = state.cart.isNotEmpty() && state.cart.all { line ->
        state.products.any { it.id == line.productId && it.published && it.stock >= line.quantity && line.quantity > 0 }
    }
    if (state.cart.isEmpty()) { EmptyCart(onShop); return }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1160.dp).fillMaxSize()) {
            val expanded = maxWidth >= 880.dp && LocalDensity.current.fontScale <= 1.3f
            val line: @Composable (DemoCartItem) -> Unit = { item ->
                CartLine(item, state.products.firstOrNull { it.id == item.productId },
                    onProduct = { onProduct(item.productId) },
                    onMinus = {
                        val product = state.products.firstOrNull { it.id == item.productId }
                        val next = (item.quantity - 1).coerceAtMost(product?.stock ?: 0).coerceAtLeast(0)
                        message = if (next == 0) store.removeFromCart(item.productId) else store.updateCart(item.productId, next)
                    },
                    onPlus = { message = store.updateCart(item.productId, item.quantity + 1) },
                    onRemove = { message = store.removeFromCart(item.productId) })
            }
            val summary: @Composable () -> Unit = {
                CartSummary(totals, ready, state.cart.sumOf { it.quantity }, message, onCheckout, onShop) { message = store.clearCart() }
            }
            if (expanded) {
                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    CartHeading(state.cart.sumOf { it.quantity }, state.cart.size)
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) { items(state.cart, key = { it.productId }) { line(it) } }
                        Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { summary() }
                    }
                }
            } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { CartHeading(state.cart.sumOf { it.quantity }, state.cart.size) }
                items(state.cart, key = { it.productId }) { line(it) }
                item { summary() }
            }
        }
    }
}

@Composable
private fun CartHeading(packs: Int, selections: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("YOUR COLLECTION", color = BrandTextMuted, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Medium)
        Text("A good place to start.", color = BrandDeep, fontFamily = BrandDisplayFont, fontSize = 32.sp, lineHeight = 38.sp,
            modifier = Modifier.semantics { heading() })
        Text("$packs ${if (packs == 1) "pack" else "packs"} · $selections ${if (selections == 1) "selection" else "selections"}",
            color = BrandTextMuted, fontSize = 14.sp)
    }
}

@Composable
private fun CartLine(item: DemoCartItem, product: DemoProduct?, onProduct: () -> Unit, onMinus: () -> Unit, onPlus: () -> Unit, onRemove: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandOutline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (product == null) Text("Unavailable product", color = BrandDeep, fontWeight = FontWeight.SemiBold)
            else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button,
                        onClickLabel = "View ${product.name}", onClick = onProduct)) { DemoArt(product.id, Modifier.matchParentSize()) }
                    Column(Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = "View ${product.name}", onClick = onProduct),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(product.name, color = BrandDeep, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${product.packSize} seeds per pack", color = BrandTextMuted, fontSize = 12.sp)
                        Text("${formatMoney(product.priceCents)} / pack", color = BrandGreen, fontSize = 14.sp)
                    }
                }
                if (!product.published || product.stock < item.quantity) Text(
                    if (!product.published) "This selection is unavailable. Remove it to continue." else "Available: ${product.stock} packs. Reduce the quantity.",
                    color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 19.sp)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val total: @Composable () -> Unit = { Text(formatMoney(product.priceCents * item.quantity), color = BrandDeep,
                        fontSize = 21.sp, fontWeight = FontWeight.SemiBold) }
                    val quantity: @Composable () -> Unit = { ShopQuantity(item.quantity, onMinus, onPlus, true,
                        product.published && item.quantity < product.stock.coerceAtMost(99)) }
                    if (maxWidth < 270.dp || LocalDensity.current.fontScale > 1.2f) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { quantity(); total() }
                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { quantity(); total() }
                }
            }
            TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove selection")
            }
        }
    }
}

@Composable
private fun CartSummary(totals: DemoTotals, ready: Boolean, packs: Int, message: String?, onCheckout: () -> Unit, onShop: () -> Unit, onClear: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = BrandPaper, border = BorderStroke(1.dp, BrandOutline)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Your selection", color = BrandDeep, fontFamily = BrandDisplayFont, fontSize = 27.sp, modifier = Modifier.semantics { heading() })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Merchandise", color = BrandGreen, fontSize = 14.sp)
                    Text("$packs ${if (packs == 1) "pack" else "packs"}", color = BrandTextMuted, fontSize = 12.sp)
                }
                Text(formatMoney(totals.subtotalCents), color = BrandDeep, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = BrandOutline)
            SummaryNote("Shipping", "At demo checkout")
            SummaryNote("Tax", "At demo checkout")
            if (!ready) Text("A selection needs attention. Check its pack size or availability before continuing.",
                color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 19.sp)
            Surface(shape = RoundedCornerShape(14.dp), color = BrandGreenSoft) {
                Text("Review delivery details to see fictional shipping, tax and your demo total. No real payment is collected.",
                    Modifier.padding(14.dp), color = BrandGreen, fontSize = 12.sp, lineHeight = 18.sp)
            }
            message?.let { DemoMessage(it) }
            DemoPrimary("Review delivery & total", onCheckout, ready && totals.subtotalCents > 0, Modifier.fillMaxWidth())
            TextButton(onClick = onShop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Continue shopping") }
            TextButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear cart") }
        }
    }
}

@Composable
private fun SummaryNote(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = BrandGreen, fontSize = 13.sp)
        Text(value, color = BrandTextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyCart(onShop: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 430.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(shape = RoundedCornerShape(28.dp), color = BrandGreenSoft) {
                Icon(Icons.Outlined.ShoppingCart, null, Modifier.padding(24.dp).size(42.dp), tint = BrandGreen)
            }
            Text("Room for something good.", color = BrandDeep, fontFamily = BrandDisplayFont, fontSize = 34.sp, lineHeight = 40.sp,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text("Choose a favorite from the collection and select a pack to get started.", color = BrandTextMuted,
                fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            DemoPrimary("Explore the collection", onShop, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ShopQuantity(quantity: Int, onMinus: () -> Unit, onPlus: () -> Unit, minusEnabled: Boolean, plusEnabled: Boolean) {
    Surface(shape = RoundedCornerShape(14.dp), color = BrandCream, border = BorderStroke(1.dp, BrandOutline)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Remove, "Decrease packs", Modifier.size(16.dp))
            }
            Box(Modifier.widthIn(min = 52.dp), contentAlignment = Alignment.Center) {
                Text(quantity.toString(), color = BrandDeep, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onPlus, enabled = plusEnabled, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Add, "Increase packs", Modifier.size(16.dp))
            }
        }
    }
}

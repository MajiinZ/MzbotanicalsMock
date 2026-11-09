package org.mz.botanicalsmock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MockTab(val page: DemoPage, val title: String, val icon: CompanyIcon)
private val homeTabs = listOf(
    MockTab(DemoPage.Home, "Home", CompanyIcon.HOME),
    MockTab(DemoPage.Explore, "Explore", CompanyIcon.EXPLORE),
    MockTab(DemoPage.Cart, "Cart", CompanyIcon.CART),
    MockTab(DemoPage.Profile, "Profile", CompanyIcon.PROFILE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MockChrome(
    page: DemoPage,
    cartCount: Int,
    profileName: String,
    drawerOpen: Boolean,
    onDrawerChange: (Boolean) -> Unit,
    onNavigate: (DemoPage) -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    MockSystemBars(darkBackground = false)
    val focus = LocalFocusManager.current
    val drawerFocus = remember { FocusRequester() }
    val fontScale = LocalDensity.current.fontScale
    val fullScreen = page in setOf(DemoPage.Product, DemoPage.Checkout, DemoPage.Admin, DemoPage.Order)
    val title = when (page) {
        DemoPage.Home -> "MZ Botanicals"
        DemoPage.Explore, DemoPage.Category -> "Explore"
        DemoPage.Cart -> "Your cart"
        DemoPage.Profile -> "Your profile"
        DemoPage.Orders, DemoPage.Order -> "Orders"
        DemoPage.Admin -> "MZ Admin"
        DemoPage.Settings -> "Settings"
        DemoPage.Checkout -> "Checkout"
        DemoPage.Product -> "THE COLLECTION"
        else -> "MZ Botanicals"
    }
    val selected = when (page) {
        DemoPage.Category -> DemoPage.Explore
        DemoPage.Cart -> DemoPage.Cart
        DemoPage.Profile -> DemoPage.Profile
        DemoPage.Explore -> DemoPage.Explore
        else -> DemoPage.Home
    }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) { focus.clearFocus(); drawerFocus.requestFocus() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().background(BrandCream)
        .onPreviewKeyEvent {
            if (drawerOpen && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                onDrawerChange(false); true
            } else false
        }) {
        val drawerWidth = minOf(maxWidth * 0.68f, 360.dp)
        val offset by animateDpAsState(if (drawerOpen) drawerWidth else 0.dp)
        val scale by animateFloatAsState(if (drawerOpen) 0.9f else 1f)
        val radius by animateDpAsState(if (drawerOpen) 20.dp else 0.dp)
        if (drawerOpen) MockDrawer(
            Modifier.width(drawerWidth).focusRequester(drawerFocus).focusable(), profileName,
            onNavigate, { onDrawerChange(false) }
        )
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(radius)).offset(x = offset).scale(scale)
            .shadow(if (drawerOpen) 20.dp else 0.dp, RoundedCornerShape(20.dp))) {
            Scaffold(
                modifier = if (drawerOpen) Modifier.clearAndSetSemantics {} else Modifier,
                containerColor = BrandCream,
                topBar = {
                    Column {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(title, fontFamily = if (page == DemoPage.Product) BrandBodyFont else BrandDisplayFont,
                                        fontSize = if (page == DemoPage.Product) 12.sp else if (fontScale > 1.2f) 21.sp else 25.sp,
                                        letterSpacing = if (page == DemoPage.Product) 2.sp else 0.sp,
                                        color = BrandGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (page == DemoPage.Home && fontScale <= 1.2f) {
                                        Text("GENETICS COLLECTION", fontFamily = BrandBodyFont, fontSize = 10.sp,
                                            lineHeight = 14.sp, letterSpacing = 1.6.sp, color = BrandTextMuted)
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    modifier = Modifier.padding(start = 8.dp).size(48.dp)
                                        .then(if (fullScreen) Modifier else Modifier.background(BrandPaper, RoundedCornerShape(16.dp))
                                            .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))),
                                    onClick = { if (fullScreen) onBack() else onDrawerChange(!drawerOpen) }
                                ) { CompanyIcon(if (fullScreen) CompanyIcon.BACK else CompanyIcon.MENU,
                                    if (fullScreen) "Back" else "Open menu", Modifier.size(22.dp)) }
                            },
                            actions = {
                                if (page != DemoPage.Cart && page != DemoPage.Checkout) {
                                    IconButton(onClick = { onNavigate(DemoPage.Cart) },
                                        modifier = Modifier.padding(end = 8.dp).size(48.dp)
                                            .background(BrandPaper, RoundedCornerShape(16.dp))
                                            .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))) {
                                        CartIcon(cartCount, false)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandCream,
                                scrolledContainerColor = BrandCream)
                        )
                        Text("DEMO STORE · No real orders or payments", Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                            color = BrandTextMuted, fontFamily = BrandBodyFont, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                },
                bottomBar = {
                    if (!fullScreen) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Row(Modifier.widthIn(max = 560.dp).fillMaxWidth()
                                .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = BrandGreen.copy(alpha = 0.08f), spotColor = BrandGreen.copy(alpha = 0.1f))
                                .clip(RoundedCornerShape(26.dp)).background(BrandPaper)
                                .border(1.dp, BrandOutline, RoundedCornerShape(26.dp)).selectableGroup().padding(6.dp)) {
                                homeTabs.forEach { tab ->
                                    val chosen = selected == tab.page
                                    val fill by animateColorAsState(if (chosen) BrandGreen else androidx.compose.ui.graphics.Color.Transparent)
                                    val tint by animateColorAsState(if (chosen) BrandPaper else BrandTextMuted)
                                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(fill)
                                        .selectable(selected = chosen, role = Role.Tab, onClick = { onNavigate(tab.page) })
                                        .heightIn(min = 60.dp).padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        if (tab.page == DemoPage.Cart) CartIcon(cartCount, chosen)
                                        else CompanyIcon(tab.icon, null, Modifier.size(22.dp), tint)
                                        Spacer(Modifier.height(5.dp))
                                        Text(tab.title, color = tint, fontFamily = BrandBodyFont, fontSize = 12.sp, lineHeight = 16.sp,
                                            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 1400.dp).fillMaxSize()) { content() }
                }
            }
            if (drawerOpen) Box(Modifier.fillMaxSize().background(BrandGreen.copy(alpha = 0.08f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClickLabel = "Close menu", onClick = { onDrawerChange(false) }))
        }
    }
}

@Composable
private fun CartIcon(count: Int, selected: Boolean) {
    Box(Modifier.semantics { contentDescription = if (count > 0) "Cart, $count packs" else "Cart" }, contentAlignment = Alignment.TopEnd) {
        CompanyIcon(CompanyIcon.CART, null, Modifier.size(22.dp), if (selected) BrandPaper else BrandGreen)
        if (count > 0) Box(Modifier.offset(x = 11.dp, y = (-5).dp).sizeIn(minWidth = 17.dp, minHeight = 17.dp)
            .clip(CircleShape).background(if (selected) BrandCream else BrandGreen)
            .padding(horizontal = 4.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(if (count > 99) "99+" else count.toString(), color = if (selected) BrandGreen else BrandPaper,
                fontFamily = BrandBodyFont, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MockDrawer(modifier: Modifier, profileName: String, onNavigate: (DemoPage) -> Unit, onClose: () -> Unit) {
    Column(modifier.fillMaxHeight().background(BrandCream).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MZ Botanicals", color = BrandGreen, fontFamily = BrandDisplayFont, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text("THE COLLECTION", color = BrandTextMuted, fontFamily = BrandBodyFont, fontSize = 10.sp, letterSpacing = 1.6.sp)
            }
            IconButton(onClick = onClose) { CompanyIcon(CompanyIcon.CLOSE, "Close menu", Modifier.size(22.dp)) }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BrandPaper)
            .border(1.dp, BrandOutline, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = { onNavigate(DemoPage.Profile) }).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            CompanyBrandLogo(Modifier.size(52.dp).clip(CircleShape))
            Column(Modifier.padding(start = 12.dp)) {
                Text(profileName, color = BrandGreen, fontFamily = Exo2FontRegular(), fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("View profile", color = BrandGreen, fontFamily = Exo2FontRegular(), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("MENU", fontFamily = Exo2FontRegular(), fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        listOf(
            Triple(DemoPage.Explore, "Explore", CompanyIcon.EXPLORE),
            Triple(DemoPage.Cart, "Cart", CompanyIcon.CART),
            Triple(DemoPage.Orders, "Orders", CompanyIcon.ORDERS),
            Triple(DemoPage.Settings, "Settings", CompanyIcon.SETTINGS),
        ).forEach { (page, label, icon) -> DrawerRow(label, icon) { onNavigate(page) } }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = BrandGreen.copy(alpha = 0.14f))
        Spacer(Modifier.height(16.dp))
        Text("ADMIN", fontFamily = Exo2FontRegular(), fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        DrawerRow("Admin panel", CompanyIcon.ADMIN, highlighted = true) { onNavigate(DemoPage.Admin) }
        Spacer(Modifier.height(12.dp))
        DrawerRow("Return to welcome", CompanyIcon.BACK) { onNavigate(DemoPage.Welcome) }
        Text("Offline demonstration. All accounts, orders and payments are fictional.", Modifier.padding(vertical = 20.dp),
            fontFamily = BrandBodyFont, color = BrandTextMuted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun DrawerRow(label: String, icon: CompanyIcon, highlighted: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(14.dp))
        .background(if (highlighted) BrandGreenSoft else androidx.compose.ui.graphics.Color.Transparent)
        .clickable(role = Role.Button, onClick = onClick).heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        CompanyIcon(icon, null, Modifier.size(22.dp))
        Text(label, Modifier.padding(start = 14.dp), fontFamily = Exo2FontRegular(), fontSize = 16.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium, color = BrandGreen)
    }
}

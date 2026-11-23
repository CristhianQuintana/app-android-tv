package com.cotasperu.tv.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import coil.compose.AsyncImage
import com.cotasperu.tv.R
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.CatalogKind
import com.cotasperu.tv.ui.theme.CotasBg
import com.cotasperu.tv.ui.theme.CotasCardMediaEmpty
import com.cotasperu.tv.ui.theme.CotasCategoryRail
import com.cotasperu.tv.ui.theme.CotasCyanFocus
import com.cotasperu.tv.ui.theme.CotasError
import com.cotasperu.tv.ui.theme.CotasFocus
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnPrimary
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasOutline
import com.cotasperu.tv.ui.theme.CotasPrimary
import com.cotasperu.tv.ui.theme.CotasPrimaryStrong
import com.cotasperu.tv.ui.theme.CotasResume
import com.cotasperu.tv.ui.theme.CotasSurface
import com.cotasperu.tv.ui.theme.CotasSurfaceHigh
import com.cotasperu.tv.ui.theme.CotasTopbarStart
import com.cotasperu.tv.ui.theme.cotasFocusRing
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** Barras overlay. Expand/collapse con slide corto. */
private val NavCollapsed = 50.dp
private val NavExpanded = 151.dp
private val CatCollapsed = 40.dp
private val CatExpanded = 135.dp
private const val HOME_RAIL_VISIBLE = 8
private const val RailSlideMs = 180

@Composable
fun CatalogShell(
    displayName: String,
    versionName: String,
    nav: NavItem,
    homeRails: List<RailUi>,
    sectionRails: List<RailUi>,
    categories: List<CatalogEntry>,
    categoryId: String?,
    detail: DetailUi?,
    notice: String?,
    authorizing: Boolean,
    favoriteIds: Set<String>,
    searchQuery: String,
    searchResults: List<SearchHit>,
    searching: Boolean,
    keepSession: Boolean,
    liveMode: LiveMode,
    sectionFilterQuery: String,
    epgQuery: String,
    epgHits: List<com.cotasperu.tv.domain.EpgSearchHit>,
    epgSearching: Boolean,
    epgSearchStatus: String?,
    epgSearchHasMore: Boolean,
    guideByStreamId: Map<String, com.cotasperu.tv.domain.ChannelEpgGuide>,
    guideLoading: Boolean,
    guideStatus: String?,
    guideDayOffset: Int,
    guideWindowShiftHours: Int,
    onNav: (NavItem) -> Unit,
    onSelectCategory: (CatalogEntry) -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
    onOpenChild: (CatalogEntry) -> Unit,
    onCloseDetail: () -> Unit,
    onPlay: (CatalogEntry) -> Unit,
    onToggleFavorite: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onSectionFilter: (String) -> Unit,
    onLiveMode: (LiveMode) -> Unit,
    onEpgQuery: (String) -> Unit,
    onEpgSearch: () -> Unit,
    onEpgLoadMore: () -> Unit,
    onEpgHit: (com.cotasperu.tv.domain.EpgSearchHit) -> Unit,
    onGuideDayOffset: (Int) -> Unit,
    onGuideShiftWindow: (Int) -> Unit,
    onGuideVisibleStreamIds: (List<String>) -> Unit,
    onGuideOpenChannel: (CatalogEntry) -> Unit,
    onKeepSession: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onRetry: () -> Unit,
) {
    var railExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }
    val firstNav = remember { FocusRequester() }
    // No enfocar el rail al entrar: el foco inicial va al botón Reproducir del carrusel (Home).

    // Categorías: Canales (modo Canales), Películas, Series. Ocultas en Programas.
    val showCategories = (nav == NavItem.LIVE && liveMode == LiveMode.CHANNELS) ||
        nav == NavItem.MOVIES ||
        nav == NavItem.SERIES
    val selectedCategoryName = categories.firstOrNull { it.id == categoryId }?.title ?: "All"
    // Slide al expandir/colapsar (sale de la barra → animación de recogida).
    val navWidth by animateDpAsState(
        targetValue = if (railExpanded) NavExpanded else NavCollapsed,
        animationSpec = tween(RailSlideMs),
        label = "navWidth",
    )
    val catWidth by animateDpAsState(
        targetValue = when {
            !showCategories -> 0.dp
            catExpanded -> CatExpanded
            else -> CatCollapsed
        },
        animationSpec = tween(RailSlideMs),
        label = "catWidth",
    )
    // Reserva fija del strip colapsado; al expandir la barra flota encima.
    val contentInset = NavCollapsed + if (showCategories) CatCollapsed else 0.dp

    Box(Modifier.fillMaxSize().background(CotasBg)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = contentInset, top = 6.dp, end = 16.dp, bottom = 6.dp),
        ) {
            TopBar(displayName)
            Spacer(Modifier.height(4.dp))
            when {
                detail != null -> DetailDrill(
                    detail = detail,
                    authorizing = authorizing,
                    notice = notice,
                    onBack = onCloseDetail,
                    onPlay = onPlay,
                    onOpenChild = onOpenChild,
                    onToggleFavorite = onToggleFavorite,
                )
                nav == NavItem.SEARCH -> SearchPanel(
                    query = searchQuery,
                    results = searchResults,
                    searching = searching,
                    favoriteIds = favoriteIds,
                    notice = notice,
                    onQuery = onSearchQuery,
                    onSearch = onSearch,
                    onOpen = onOpen,
                )
                nav == NavItem.CONFIG -> ConfigPanel(
                    displayName = displayName,
                    versionName = versionName,
                    keepSession = keepSession,
                    onKeepSession = onKeepSession,
                    onLogout = onLogout,
                )
                nav == NavItem.HOME -> HomeBrowse(
                    rails = homeRails,
                    notice = notice,
                    favoriteIds = favoriteIds,
                    onRetry = onRetry,
                    onOpen = onOpen,
                    onSeeMore = { kind ->
                        when (kind) {
                            CatalogKind.LIVE_STREAMS -> onNav(NavItem.LIVE)
                            CatalogKind.VOD_STREAMS -> onNav(NavItem.MOVIES)
                            CatalogKind.SERIES -> onNav(NavItem.SERIES)
                            else -> Unit
                        }
                    },
                )
                nav == NavItem.LIVE -> LiveHub(
                    liveMode = liveMode,
                    categoryName = selectedCategoryName,
                    channelQuery = sectionFilterQuery,
                    channelRails = sectionRails,
                    notice = notice,
                    favoriteIds = favoriteIds,
                    epgQuery = epgQuery,
                    guideByStreamId = guideByStreamId,
                    guideLoading = guideLoading,
                    guideStatus = guideStatus,
                    guideDayOffset = guideDayOffset,
                    guideWindowShiftHours = guideWindowShiftHours,
                    onLiveMode = onLiveMode,
                    onChannelFilter = onSectionFilter,
                    onEpgQuery = onEpgQuery,
                    onGuideDayOffset = onGuideDayOffset,
                    onGuideShiftWindow = onGuideShiftWindow,
                    onGuideVisibleStreamIds = onGuideVisibleStreamIds,
                    onGuideOpenChannel = onGuideOpenChannel,
                    onRetry = onRetry,
                    onOpen = onOpen,
                )
                else -> SectionBrowse(
                    rails = sectionRails,
                    categoryName = selectedCategoryName,
                    notice = notice,
                    favoriteIds = favoriteIds,
                    filterQuery = sectionFilterQuery,
                    searchPlaceholder = when (nav) {
                        NavItem.MOVIES -> "Buscar película…"
                        NavItem.SERIES -> "Buscar serie…"
                        else -> "Buscar…"
                    },
                    onFilterQuery = onSectionFilter,
                    onRetry = onRetry,
                    onOpen = onOpen,
                )
            }
        }

        // Overlay a la izquierda: expandir no contrae el contenido ni la otra barra.
        Row(Modifier.align(Alignment.CenterStart).fillMaxHeight()) {
            NavRail(
                nav = nav,
                width = navWidth,
                expanded = railExpanded,
                firstFocus = firstNav,
                onExpand = { railExpanded = it },
                onNav = onNav,
            )
            if (showCategories) {
                CategoryRail(
                    width = catWidth,
                    expanded = catExpanded,
                    categories = categories.ifEmpty { listOf(CatalogEntry(HomeViewModel.ALL_ID, "All")) },
                    selectedId = categoryId,
                    onExpand = { catExpanded = it },
                    onSelect = onSelectCategory,
                )
            }
        }

        if (authorizing && detail == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = CotasPrimary)
                    Text("Preparando…", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NavRail(
    nav: NavItem,
    width: androidx.compose.ui.unit.Dp,
    expanded: Boolean,
    firstFocus: FocusRequester,
    onExpand: (Boolean) -> Unit,
    onNav: (NavItem) -> Unit,
) {
    var railFocusCount by remember { mutableStateOf(0) }
    // Expand instantáneo; colapso corto solo para no parpadear entre ítems.
    // Retraso al expandir: evita que un foco transitorio (remount/OSK) abra Buscar.
    LaunchedEffect(railFocusCount) {
        if (railFocusCount > 0) {
            delay(160)
            if (railFocusCount > 0) onExpand(true)
        } else {
            delay(40)
            if (railFocusCount == 0) onExpand(false)
        }
    }
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(CotasBg.copy(alpha = 0.94f))
            .padding(vertical = 6.dp, horizontal = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        NavItem.entries.forEach { item ->
            val selected = item == nav
            var focused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    // Ancla de rail en Home (no en Buscar) para D-pad izquierda.
                    .then(if (item == NavItem.HOME) Modifier.focusRequester(firstFocus) else Modifier)
                    .onFocusChanged { state ->
                        val now = state.isFocused
                        if (now != focused) {
                            focused = now
                            railFocusCount = (railFocusCount + if (now) 1 else -1).coerceAtLeast(0)
                        }
                    }
                    // Sin cajas: solo anillo al tener foco.
                    .cotasFocusRing(focused, radius = 5.dp, showIdleOutline = false)
                    .clickable { onNav(item) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        navGlyph(item),
                        color = when {
                            focused -> CotasPrimary
                            selected -> CotasPrimary
                            else -> CotasOnSurface
                        },
                        fontSize = 14.sp,
                    )
                    if (expanded) {
                        Text(
                            item.label,
                            color = when {
                                focused -> CotasPrimary
                                selected -> CotasPrimary
                                else -> CotasOnSurface
                            },
                            fontSize = 12.sp,
                            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRail(
    width: androidx.compose.ui.unit.Dp,
    expanded: Boolean,
    categories: List<CatalogEntry>,
    selectedId: String?,
    onExpand: (Boolean) -> Unit,
    onSelect: (CatalogEntry) -> Unit,
) {
    var catFocusCount by remember { mutableStateOf(0) }
    LaunchedEffect(catFocusCount) {
        if (catFocusCount > 0) onExpand(true)
        else {
            delay(40)
            if (catFocusCount == 0) onExpand(false)
        }
    }
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(CotasCategoryRail.copy(alpha = 0.96f))
            .padding(vertical = 5.dp, horizontal = 2.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            itemsIndexed(categories, key = { _, cat -> cat.id }) { index, cat ->
                var focused by remember { mutableStateOf(false) }
                val selected = cat.id == selectedId || (selectedId == null && cat.id == HomeViewModel.ALL_ID)
                val number = (index + 1).toString()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .onFocusChanged { state ->
                            val now = state.isFocused
                            if (now != focused) {
                                focused = now
                                catFocusCount = (catFocusCount + if (now) 1 else -1).coerceAtLeast(0)
                            }
                        }
                        // Sin bloques: solo anillo cuando hay foco.
                        .cotasFocusRing(focused, radius = 4.dp, showIdleOutline = false)
                        .clickable { onSelect(cat) }
                        .padding(horizontal = 4.dp),
                    contentAlignment = if (expanded) Alignment.CenterStart else Alignment.Center,
                ) {
                    if (expanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                number,
                                color = when {
                                    focused -> CotasPrimary
                                    selected -> CotasOnSurface
                                    else -> CotasMuted
                                },
                                fontSize = 11.sp,
                                fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.SemiBold,
                            )
                            Text(
                                cat.title,
                                color = when {
                                    focused -> CotasPrimary
                                    selected -> CotasOnSurface
                                    else -> CotasMuted
                                },
                                fontSize = 11.sp,
                                fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            number,
                            color = when {
                                focused -> CotasPrimary
                                selected -> CotasOnSurface
                                else -> CotasMuted
                            },
                            fontSize = 10.sp,
                            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun TopBar(displayName: String) {
    var clock by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = currentTime()
            delay(30_000)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(CotasTopbarStart, Color.Transparent)),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandLogo(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(clock, color = CotasOnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(displayName, color = CotasOnSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ColumnScope.HomeBrowse(
    rails: List<RailUi>,
    notice: String?,
    favoriteIds: Set<String>,
    onRetry: () -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
    onSeeMore: (CatalogKind) -> Unit,
) {
    val heroPool = remember(rails) { buildHeroPool(rails) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val heroPlayFocus = remember { FocusRequester() }
    val firstRailFocus = remember { FocusRequester() }
    val firstReadyIndex = remember(rails) {
        rails.indexOfFirst { it.status == RailStatus.Ready && it.items.isNotEmpty() }
    }
    val hasNotice = !notice.isNullOrBlank()
    // Índice del primer rail en LazyColumn (hero=0, notice opcional=1).
    val firstRailListIndex = 1 + if (hasNotice) 1 else 0
    // Al entrar a Home: foco en Reproducir del carrusel (no en el menú Buscar).
    LaunchedEffect(heroPool.firstOrNull()?.entry?.id) {
        if (heroPool.isEmpty()) return@LaunchedEffect
        delay(80)
        runCatching { heroPlayFocus.requestFocus() }
    }
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        // Hero a viewport completo: canales/series/películas solo al scrollear.
        val heroHeight = maxHeight
        val heroActive by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset < 48
            }
        }
        fun snapHeroFull() {
            scope.launch { listState.scrollToItem(0) }
        }
        fun goToFirstRail() {
            scope.launch {
                listState.scrollToItem(firstRailListIndex)
                delay(24)
                try {
                    firstRailFocus.requestFocus()
                } catch (_: IllegalStateException) {
                    // aún no montado
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(CotasBg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item(key = "hero") {
                HomeHeroRotator(
                    pool = heroPool,
                    active = heroActive,
                    onPlay = onOpen,
                    onHeroFocused = { snapHeroFull() },
                    playFocus = heroPlayFocus,
                    downFocus = if (firstReadyIndex >= 0) firstRailFocus else null,
                    onRequestDown = { goToFirstRail() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight),
                )
            }
            if (hasNotice) {
                item(key = "notice") { Text(notice!!, color = CotasError, fontSize = 14.sp) }
            }
            itemsIndexed(rails, key = { _, rail -> rail.kind.name }) { index, rail ->
                val isFirstReady = firstReadyIndex >= 0 && index == firstReadyIndex
                Swimlane(
                    rail = rail,
                    favoriteIds = favoriteIds,
                    showSeeMore = true,
                    onRetry = onRetry,
                    onOpen = onOpen,
                    onSeeMore = { onSeeMore(rail.kind) },
                    firstItemFocus = if (isFirstReady) firstRailFocus else null,
                    upFocus = if (isFirstReady) heroPlayFocus else null,
                )
            }
        }
    }
}

/**
 * Hub Canales: barra + desplegable estables (no se desmontan al cambiar Canal/Programa),
 * así el foco no salta al menú Buscar.
 */
@Composable
private fun ColumnScope.LiveHub(
    liveMode: LiveMode,
    categoryName: String,
    channelQuery: String,
    channelRails: List<RailUi>,
    notice: String?,
    favoriteIds: Set<String>,
    epgQuery: String,
    guideByStreamId: Map<String, com.cotasperu.tv.domain.ChannelEpgGuide>,
    guideLoading: Boolean,
    guideStatus: String?,
    guideDayOffset: Int,
    guideWindowShiftHours: Int,
    onLiveMode: (LiveMode) -> Unit,
    onChannelFilter: (String) -> Unit,
    onEpgQuery: (String) -> Unit,
    onGuideDayOffset: (Int) -> Unit,
    onGuideShiftWindow: (Int) -> Unit,
    onGuideVisibleStreamIds: (List<String>) -> Unit,
    onGuideOpenChannel: (CatalogEntry) -> Unit,
    onRetry: () -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
) {
    var oskOpen by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val dropdownFocus = remember { FocusRequester() }
    androidx.activity.compose.BackHandler(enabled = oskOpen) { oskOpen = false }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }

    val searchQuery = if (liveMode == LiveMode.CHANNELS) channelQuery else epgQuery

    Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (oskOpen) 268.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LiveSearchRow(
                mode = liveMode,
                query = searchQuery,
                oskOpen = oskOpen,
                onActivateSearch = { oskOpen = true },
                onMode = onLiveMode,
                focusRequester = fieldFocus,
                dropdownFocusRequester = dropdownFocus,
                requestDropdownFocus = false,
            )
            if (liveMode == LiveMode.CHANNELS) {
                Text("Categoría: $categoryName", color = CotasMuted, fontSize = 13.sp)
            }
            when (liveMode) {
                LiveMode.CHANNELS -> ChannelGridBody(
                    rails = channelRails,
                    notice = notice,
                    favoriteIds = favoriteIds,
                    filterQuery = channelQuery,
                    onRetry = onRetry,
                    onOpen = onOpen,
                )
                LiveMode.PROGRAMS -> TvGuideBody(
                    channels = channelRails
                        .filter { it.kind == CatalogKind.LIVE_STREAMS }
                        .flatMap { it.items },
                    guideByStreamId = guideByStreamId,
                    loading = guideLoading,
                    status = guideStatus,
                    filterQuery = epgQuery,
                    dayOffset = guideDayOffset,
                    windowShiftHours = guideWindowShiftHours,
                    onDayOffset = onGuideDayOffset,
                    onShiftWindow = onGuideShiftWindow,
                    onVisibleStreamIds = onGuideVisibleStreamIds,
                    onOpenChannel = onGuideOpenChannel,
                )
            }
        }

        if (oskOpen) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                CotasOsk(
                    actionLabel = "LISTO",
                    resetKey = liveMode,
                    onChar = { ch ->
                        if (liveMode == LiveMode.CHANNELS) {
                            onChannelFilter((channelQuery + ch).take(64))
                        } else {
                            onEpgQuery((epgQuery + ch).take(80))
                        }
                    },
                    onBackspace = {
                        if (liveMode == LiveMode.CHANNELS) {
                            if (channelQuery.isNotEmpty()) onChannelFilter(channelQuery.dropLast(1))
                        } else if (epgQuery.isNotEmpty()) {
                            onEpgQuery(epgQuery.dropLast(1))
                        }
                    },
                    onClear = {
                        if (liveMode == LiveMode.CHANNELS) onChannelFilter("") else onEpgQuery("")
                    },
                    onAction = {
                        oskOpen = false
                        try {
                            fieldFocus.requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                    },
                    onClose = {
                        oskOpen = false
                        try {
                            fieldFocus.requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ChannelGridBody(
    rails: List<RailUi>,
    notice: String?,
    favoriteIds: Set<String>,
    filterQuery: String,
    onRetry: () -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
) {
    val rail = rails.firstOrNull()
    val filtered = remember(rail?.items, filterQuery) {
        rail?.items.orEmpty().filter { matchesCatalogQuery(it.title, it.subtitle, filterQuery) }
    }
    if (!notice.isNullOrBlank()) Text(notice, color = CotasError, fontSize = 13.sp)
    when (rail?.status) {
        null, RailStatus.Loading -> CircularProgressIndicator(color = CotasPrimary)
        RailStatus.Empty -> Text(rail.message ?: "No hay contenido disponible.", color = CotasMuted)
        RailStatus.Error -> Column {
            Text(rail.message ?: "No se pudo cargar. Inténtalo de nuevo.", color = CotasError)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry, scale = cotasButtonScale()) {
                CotasButtonLabel("Reintentar", fontSize = 16.sp)
            }
        }
        RailStatus.Ready -> {
            if (filtered.isEmpty() && filterQuery.isNotBlank()) {
                Text("Sin resultados para «$filterQuery».", color = CotasMuted, fontSize = 13.sp)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = filtered,
                    key = { it.id + it.title },
                    contentType = { "poster" },
                ) { entry ->
                    PosterCard(
                        entry = entry,
                        favorite = favoriteIds.contains(entry.id),
                        liveStyle = true,
                        onClick = { onOpen(entry, rail.kind) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SectionBrowse(
    rails: List<RailUi>,
    categoryName: String,
    notice: String?,
    favoriteIds: Set<String>,
    filterQuery: String,
    searchPlaceholder: String,
    onFilterQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
) {
    val rail = rails.firstOrNull()
    var oskOpen by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    androidx.activity.compose.BackHandler(enabled = oskOpen) { oskOpen = false }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }
    val filtered = remember(rail?.items, filterQuery) {
        rail?.items.orEmpty().filter { matchesCatalogQuery(it.title, it.subtitle, filterQuery) }
    }

    Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (oskOpen) 268.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TvSearchField(
                value = filterQuery,
                placeholder = searchPlaceholder,
                active = oskOpen,
                onActivate = { oskOpen = true },
                focusRequester = fieldFocus,
                maxWidth = 320.dp,
            )
            Text("Categoría: $categoryName", color = CotasMuted, fontSize = 13.sp)
            if (!notice.isNullOrBlank()) Text(notice, color = CotasError, fontSize = 13.sp)
            when (rail?.status) {
                null, RailStatus.Loading -> CircularProgressIndicator(color = CotasPrimary)
                RailStatus.Empty -> Text(rail.message ?: "No hay contenido disponible.", color = CotasMuted)
                RailStatus.Error -> Column {
                    Text(rail.message ?: "No se pudo cargar. Inténtalo de nuevo.", color = CotasError)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry, scale = cotasButtonScale()) {
                        CotasButtonLabel("Reintentar", fontSize = 16.sp)
                    }
                }
                RailStatus.Ready -> {
                    if (filtered.isEmpty() && filterQuery.isNotBlank()) {
                        Text("Sin resultados para «$filterQuery».", color = CotasMuted, fontSize = 13.sp)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = filtered,
                            key = { it.id + it.title },
                            contentType = { "poster" },
                        ) { entry ->
                            PosterCard(
                                entry = entry,
                                favorite = favoriteIds.contains(entry.id),
                                liveStyle = false,
                                onClick = { onOpen(entry, rail.kind) },
                            )
                        }
                    }
                }
            }
        }

        if (oskOpen) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                CotasOsk(
                    actionLabel = "LISTO",
                    onChar = { ch -> onFilterQuery((filterQuery + ch).take(64)) },
                    onBackspace = { if (filterQuery.isNotEmpty()) onFilterQuery(filterQuery.dropLast(1)) },
                    onClear = { onFilterQuery("") },
                    onAction = {
                        oskOpen = false
                        try {
                            fieldFocus.requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                    },
                    onClose = {
                        oskOpen = false
                        try {
                            fieldFocus.requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Swimlane(
    rail: RailUi,
    favoriteIds: Set<String>,
    showSeeMore: Boolean,
    onRetry: () -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
    onSeeMore: () -> Unit,
    firstItemFocus: FocusRequester? = null,
    upFocus: FocusRequester? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(rail.title, color = CotasOnSurface, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        when (rail.status) {
            RailStatus.Loading -> CircularProgressIndicator(color = CotasPrimary, modifier = Modifier.size(28.dp))
            RailStatus.Empty -> Text(rail.message ?: "No hay contenido disponible.", color = CotasMuted, fontSize = 13.sp)
            RailStatus.Error -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(rail.message ?: "Error", color = CotasError, fontSize = 13.sp)
                Button(onClick = onRetry, scale = cotasButtonScale()) {
                    CotasButtonLabel("Reintentar", fontSize = 16.sp)
                }
            }
            RailStatus.Ready -> LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Home: pocas tarjetas → menos jank al mover el foco.
                val visible = if (showSeeMore) rail.items.take(HOME_RAIL_VISIBLE) else rail.items
                itemsIndexed(visible, key = { _, it -> rail.kind.name + it.id }) { index, entry ->
                    PosterCard(
                        entry = entry,
                        favorite = favoriteIds.contains(entry.id),
                        liveStyle = rail.kind == CatalogKind.LIVE_STREAMS,
                        onClick = { onOpen(entry, rail.kind) },
                        focusRequester = if (index == 0) firstItemFocus else null,
                        upFocus = if (index == 0) upFocus else null,
                    )
                }
                if (showSeeMore) {
                    item {
                        SeeMoreCard(onClick = onSeeMore)
                    }
                }
            }
        }
    }
}

@Composable
fun PosterCard(
    entry: CatalogEntry,
    favorite: Boolean = false,
    liveStyle: Boolean = false,
    selected: Boolean = false,
    resume: Boolean = false,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    upFocus: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val cardW = if (liveStyle) 176.dp else 182.dp
    val mediaHeight = if (liveStyle) 112.dp else 154.dp
    val posterRequest = rememberPosterRequest(entry.imageUrl, cardW, mediaHeight)
    Box(
        modifier = Modifier
            .width(cardW)
            .height(if (liveStyle) 156.dp else 188.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(
                if (upFocus != null) {
                    Modifier.focusProperties { up = upFocus }
                } else {
                    Modifier
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 10.dp, selected = selected)
            .clip(RoundedCornerShape(10.dp))
            .background(CotasBg)
            .clickable(onClick = onClick),
    ) {
        val mediaMod = Modifier
            .fillMaxWidth()
            .height(mediaHeight)
            .align(Alignment.TopCenter)
            .clip(RoundedCornerShape(8.dp))
            .background(if (liveStyle) CotasBg else CotasCardMediaEmpty)
        if (posterRequest != null) {
            AsyncImage(
                model = posterRequest,
                contentDescription = entry.title,
                modifier = mediaMod,
                contentScale = if (liveStyle) ContentScale.Fit else ContentScale.Crop,
            )
        } else {
            Box(mediaMod, contentAlignment = Alignment.Center) {
                Text(entry.title.take(1), color = CotasPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.68f to Color.Black.copy(alpha = 0.92f),
                        1.00f to Color.Black,
                    ),
                )
                .padding(12.dp),
        ) {
            Text(
                entry.title,
                color = CotasOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!entry.subtitle.isNullOrBlank() && entry.subtitle!!.contains('·')) {
            val rating = entry.subtitle!!.substringAfter('·').trim()
            if (rating.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CotasPrimary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(rating, color = CotasOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (favorite) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_heart_filled),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFF5A7A)),
                )
            }
        }
        if (resume) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CotasResume)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("●", color = Color.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SeeMoreCard(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .width(182.dp)
            .height(188.dp)
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CotasSurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("Ver más →", color = CotasOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ColumnScope.DetailDrill(
    detail: DetailUi,
    authorizing: Boolean,
    notice: String?,
    onBack: () -> Unit,
    onPlay: (CatalogEntry) -> Unit,
    onOpenChild: (CatalogEntry) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val backFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val favFocus = remember { FocusRequester() }
    val canPlay = detail.playbackTarget != null && !authorizing
    LaunchedEffect(detail.entry.id, canPlay, detail.isSeriesDrill) {
        try {
            if (canPlay) playFocus.requestFocus() else backFocus.requestFocus()
        } catch (_: IllegalStateException) {
            // foco aún no montado
        }
    }
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var backFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .focusRequester(backFocus)
                    .focusProperties {
                        down = if (canPlay) playFocus else favFocus
                    }
                    .onFocusChanged { backFocused = it.isFocused }
                    .cotasFocusRing(backFocused, radius = 6.dp, showIdleOutline = false)
                    .background(if (backFocused) CotasPrimary else CotasSurfaceHigh)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "← Volver",
                    color = if (backFocused) CotasOnPrimary else CotasOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (!notice.isNullOrBlank()) {
            Text(notice, color = CotasError, fontSize = 13.sp)
        }

        DetailInfoBlock(
            detail = detail,
            authorizing = authorizing,
            canPlay = canPlay,
            playFocus = playFocus,
            favFocus = favFocus,
            backFocus = backFocus,
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
        )

        if (detail.isLiveChannel) {
            ChannelEpgBlock(
                guide = detail.channelEpg,
                loading = detail.epgLoading,
                error = detail.epgError,
                onPlayLive = { onPlay(detail.entry) },
            )
        }

        if (detail.isSeriesDrill || detail.childKind == CatalogKind.SERIES_SEASONS) {
            Text("Temporadas", color = CotasOnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (detail.loading) {
                Text("Cargando temporadas…", color = CotasMuted, fontSize = 13.sp)
                CircularProgressIndicator(color = CotasPrimary, modifier = Modifier.size(28.dp))
            }
            if (!detail.error.isNullOrBlank()) {
                Text(detail.error!!, color = CotasError, fontSize = 13.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                detail.children.forEach { season ->
                    SeasonAccordionRow(
                        season = season,
                        expanded = detail.expandedSeasonId == season.id,
                        episodes = if (detail.expandedSeasonId == season.id) detail.episodes else emptyList(),
                        episodesLoading = detail.expandedSeasonId == season.id && detail.episodesLoading,
                        resumeEpisodeId = detail.resumeEpisodeId,
                        onToggle = { onOpenChild(season) },
                        onEpisode = onOpenChild,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailInfoBlock(
    detail: DetailUi,
    authorizing: Boolean,
    canPlay: Boolean,
    playFocus: FocusRequester,
    favFocus: FocusRequester,
    backFocus: FocusRequester,
    onPlay: (CatalogEntry) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val entry = detail.entry
    val backdrop = entry.backdropUrl ?: entry.imageUrl
    val poster = entry.imageUrl ?: entry.backdropUrl
    // Proporciones Samsung: banda compacta (~40–45% visual), tipografía chica.
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 248.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CotasSurface),
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0.00f to Color.Black.copy(alpha = 0.86f),
                        0.40f to Color.Black.copy(alpha = 0.58f),
                        0.78f to Color.Black.copy(alpha = 0.28f),
                        1.00f to Color.Black.copy(alpha = 0.48f),
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Black.copy(alpha = 0.28f),
                        0.55f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )

        // Favorito: solo icono de corazón (vector), no texto ni emoji.
        var favFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(36.dp)
                .focusRequester(favFocus)
                .focusProperties {
                    up = backFocus
                    left = if (canPlay) playFocus else FocusRequester.Default
                }
                .onFocusChanged { favFocused = it.isFocused }
                .cotasFocusRing(favFocused, radius = 18.dp, showIdleOutline = false)
                .clip(CircleShape)
                .background(
                    when {
                        favFocused -> CotasPrimary.copy(alpha = 0.35f)
                        else -> Color.Black.copy(alpha = 0.35f)
                    },
                )
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    if (detail.favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline,
                ),
                contentDescription = "Favorito",
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    when {
                        favFocused -> Color.White
                        detail.favorite -> Color(0xFFFF5A7A)
                        else -> Color.White
                    },
                ),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 52.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (poster != null) {
                    AsyncImage(
                        model = poster,
                        contentDescription = entry.title,
                        modifier = Modifier
                            .width(112.dp)
                            .height(168.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (!entry.rating.isNullOrBlank()) {
                    Text(
                        "★".repeat(ratingStars(entry.rating)) + "  ${entry.rating}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    displayTitle(entry),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetaLine("Dirigido por", entry.director)
                MetaLine("Estreno", entry.year)
                MetaLine("Duración", entry.duration)
                MetaLine("Género", entry.genre)
                MetaLine("Reparto", entry.cast)
                Row(
                    Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var playFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { onPlay(entry) },
                        enabled = canPlay || detail.isSeriesDrill,
                        modifier = Modifier
                            .focusRequester(playFocus)
                            .focusProperties {
                                up = backFocus
                                right = favFocus
                            }
                            .onFocusChanged { playFocused = it.isFocused }
                            .widthIn(min = 120.dp)
                            .height(32.dp)
                            .border(
                                width = if (playFocused) 2.dp else 1.dp,
                                color = if (playFocused) CotasCyanFocus else Color.White.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(999.dp),
                            ),
                        scale = cotasButtonScale(),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(999.dp)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.92f),
                            contentColor = CotasPrimaryStrong,
                            focusedContainerColor = CotasCyanFocus,
                            focusedContentColor = Color.White,
                            pressedContainerColor = CotasPrimary,
                            pressedContentColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.55f),
                            disabledContentColor = CotasPrimary.copy(alpha = 0.55f),
                        ),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when {
                                    authorizing -> "Preparando…"
                                    !detail.continueLabel.isNullOrBlank() -> detail.continueLabel!!
                                    else -> "Reproducir"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
                val plot = entry.plot?.takeIf { it.isNotBlank() }
                if (plot != null) {
                    Text(
                        plot,
                        color = Color(0xFFE8E8E8),
                        fontSize = 12.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.92f),
                    )
                } else if (detail.loading) {
                    Text("Cargando descripción…", color = CotasMuted, fontSize = 12.sp)
                }
                if (detail.loading && !detail.isSeriesDrill) {
                    CircularProgressIndicator(color = CotasPrimary, modifier = Modifier.size(22.dp))
                }
                if (!detail.error.isNullOrBlank() && !detail.isSeriesDrill) {
                    Text(detail.error!!, color = CotasError, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Text(
        "$label: $value",
        color = Color.White,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun ratingStars(raw: String?): Int {
    val n = raw?.toFloatOrNull() ?: return 0
    return when {
        n >= 9f -> 5
        n >= 7f -> 4
        n >= 5f -> 3
        n >= 3f -> 2
        n > 0f -> 1
        else -> 0
    }
}

/** Evita títulos tipo «Arcane (2021) (2021)». */
private fun displayTitle(entry: CatalogEntry): String {
    val year = entry.year?.takeIf { it.length == 4 }
    val cleaned = entry.title.replace(Regex("""\s*\((\d{4})\)(?:\s*\(\1\))+"""), " ($1)").trim()
    return if (year != null && !cleaned.contains("($year)")) "$cleaned ($year)" else cleaned
}

@Composable
private fun SeasonAccordionRow(
    season: CatalogEntry,
    expanded: Boolean,
    episodes: List<CatalogEntry>,
    episodesLoading: Boolean,
    resumeEpisodeId: String?,
    onToggle: () -> Unit,
    onEpisode: (CatalogEntry) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CotasSurface)
            .border(1.dp, if (expanded) CotasPrimary.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(10.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .onFocusChanged { focused = it.isFocused }
                .cotasFocusRing(focused, radius = 8.dp, showIdleOutline = false)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (expanded) "▾" else "▸",
                    color = CotasPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    season.title.ifBlank { "Temporada ${season.id}" },
                    color = CotasOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!season.subtitle.isNullOrBlank()) {
                Text(season.subtitle!!, color = CotasMuted, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (expanded) {
            if (episodesLoading) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    CircularProgressIndicator(color = CotasPrimary, modifier = Modifier.size(28.dp))
                }
            } else if (episodes.isEmpty()) {
                Text(
                    "No hay episodios en esta temporada.",
                    color = CotasMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                Column(
                    Modifier.padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    episodes.forEachIndexed { epIndex, ep ->
                        val isResume = ep.id == resumeEpisodeId
                        var epFocused by remember { mutableStateOf(false) }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(horizontal = 8.dp)
                                .onFocusChanged { epFocused = it.isFocused }
                                .cotasFocusRing(epFocused, radius = 6.dp, showIdleOutline = false)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        epFocused -> CotasPrimary.copy(alpha = 0.35f)
                                        isResume -> CotasResume.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    },
                                )
                                .clickable { onEpisode(ep) }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Cap. ${epIndex + 1}  ${ep.title}",
                                color = CotasOnSurface,
                                fontSize = 13.sp,
                                fontWeight = if (epFocused || isResume) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SearchPanel(
    query: String,
    results: List<SearchHit>,
    searching: Boolean,
    favoriteIds: Set<String>,
    notice: String?,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (CatalogEntry, CatalogKind) -> Unit,
) {
    var oskOpen by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    androidx.activity.compose.BackHandler(enabled = oskOpen) { oskOpen = false }

    Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (oskOpen) 268.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Buscar", color = CotasOnSurface, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Encuentra canales, películas y series.", color = CotasMuted, fontSize = 15.sp)
            TvSearchField(
                value = query,
                placeholder = "Escribe para buscar…",
                active = oskOpen,
                onActivate = { oskOpen = true },
                focusRequester = fieldFocus,
                maxWidth = 340.dp,
            )
            if (!notice.isNullOrBlank()) Text(notice, color = CotasError, fontSize = 14.sp)
            if (searching) CircularProgressIndicator(color = CotasPrimary)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { "${it.kind}:${it.entry.id}" }) { hit ->
                    PosterCard(
                        hit.entry,
                        favorite = favoriteIds.contains(hit.entry.id),
                        onClick = { onOpen(hit.entry, hit.kind) },
                    )
                }
            }
        }

        if (oskOpen) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                CotasOsk(
                    actionLabel = "BUSCAR",
                    onChar = { ch -> onQuery((query + ch).take(80)) },
                    onBackspace = { if (query.isNotEmpty()) onQuery(query.dropLast(1)) },
                    onClear = { onQuery("") },
                    onAction = {
                        oskOpen = false
                        onSearch()
                    },
                    onClose = { oskOpen = false },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ConfigPanel(
    displayName: String,
    versionName: String,
    keepSession: Boolean,
    onKeepSession: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    var keepFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.62f)
                .clip(RoundedCornerShape(12.dp))
                .background(CotasSurface)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Configuración", color = CotasOnSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Cuenta", color = CotasMuted, fontSize = 12.sp)
            Text(displayName, color = CotasOnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Versión $versionName", color = CotasMuted, fontSize = 12.sp)
            // Visible en idle: borde blanco + texto claro (no solo al foco).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .onFocusChanged { keepFocused = it.isFocused }
                    .border(
                        width = if (keepFocused) 2.dp else 1.dp,
                        color = when {
                            keepFocused -> CotasCyanFocus
                            keepSession -> CotasPrimary
                            else -> Color.White.copy(alpha = 0.75f)
                        },
                        shape = RoundedCornerShape(999.dp),
                    )
                    .background(
                        when {
                            keepFocused -> CotasCyanFocus.copy(alpha = 0.18f)
                            keepSession -> CotasPrimary.copy(alpha = 0.20f)
                            else -> Color.Black
                        },
                    )
                    .clickable { onKeepSession(!keepSession) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Mantener sesión: ${if (keepSession) "Sí" else "No"}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = onLogout,
                modifier = Modifier.focusRequester(focus).fillMaxWidth().height(42.dp),
                scale = cotasButtonScale(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.colors(
                    containerColor = CotasPrimaryStrong,
                    contentColor = Color.White,
                    focusedContainerColor = CotasCyanFocus,
                    focusedContentColor = Color.White,
                ),
            ) {
                CotasButtonLabel("Cerrar sesión", fontSize = 14.sp, fillWidth = true, color = Color.White)
            }
        }
    }
}

private fun navGlyph(item: NavItem): String = when (item) {
    NavItem.SEARCH -> "⌕"
    NavItem.HOME -> "⌂"
    NavItem.LIVE -> "▶"
    NavItem.MOVIES -> "▣"
    NavItem.SERIES -> "☰"
    NavItem.CONFIG -> "⚙"
}

private fun currentTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

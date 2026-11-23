package com.cotasperu.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cotasperu.tv.core.prefs.WatchProgress
import com.cotasperu.tv.core.security.PlaybackDecision
import com.cotasperu.tv.core.security.PlaybackUrlPolicy
import com.cotasperu.tv.core.security.StreamIdPolicy
import com.cotasperu.tv.di.AppGraph
import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.CatalogKind
import com.cotasperu.tv.domain.ChannelEpgGuide
import com.cotasperu.tv.domain.EpgSearchHit
import com.cotasperu.tv.domain.PlaybackSession
import com.cotasperu.tv.domain.PlaybackTarget
import com.cotasperu.tv.domain.UserMessages
import com.cotasperu.tv.domain.toKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class NavItem(val label: String) {
    SEARCH("Buscar"),
    HOME("Home"),
    LIVE("Canales"),
    MOVIES("Películas"),
    SERIES("Series"),
    CONFIG("Configuración"),
}

enum class LiveMode { CHANNELS, PROGRAMS }

enum class RailStatus { Loading, Ready, Empty, Error }

data class RailUi(
    val title: String,
    val kind: CatalogKind,
    val status: RailStatus,
    val items: List<CatalogEntry> = emptyList(),
    val message: String? = null,
)

data class DetailUi(
    val entry: CatalogEntry,
    val childrenTitle: String?,
    val children: List<CatalogEntry>,
    val childKind: CatalogKind?,
    val loading: Boolean,
    val error: String?,
    val canPlay: Boolean,
    val playbackTarget: PlaybackTarget? = null,
    val seriesId: String? = null,
    val seasonNumber: String? = null,
    val favorite: Boolean = false,
    val continueLabel: String? = null,
    val resumeEpisodeId: String? = null,
    /** Drill de serie estilo Samsung: cabecera + acordeón de temporadas. */
    val isSeriesDrill: Boolean = false,
    val expandedSeasonId: String? = null,
    val episodes: List<CatalogEntry> = emptyList(),
    val episodesLoading: Boolean = false,
    /** Canal en vivo: ficha informativa + guía EPG (sin auto-play). */
    val isLiveChannel: Boolean = false,
    val channelEpg: ChannelEpgGuide? = null,
    val epgLoading: Boolean = false,
    val epgError: String? = null,
)

data class SearchHit(
    val entry: CatalogEntry,
    val kind: CatalogKind,
)

class HomeViewModel(
    private val graph: AppGraph,
) : ViewModel() {
    var nav by mutableStateOf(NavItem.HOME)
        private set
    var homeRails by mutableStateOf<List<RailUi>>(emptyList())
        private set
    var sectionRails by mutableStateOf<List<RailUi>>(emptyList())
        private set
    var categories by mutableStateOf<List<CatalogEntry>>(emptyList())
        private set
    var categoryId by mutableStateOf<String?>(ALL_ID)
        private set
    var detail by mutableStateOf<DetailUi?>(null)
        private set
    var playback by mutableStateOf<PlaybackSession?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var sessionExpired by mutableStateOf(false)
        private set
    var authorizing by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var keepSession by mutableStateOf(graph.prefs.keepSession)
        private set
    var favoriteIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var liveMode by mutableStateOf(LiveMode.CHANNELS)
        private set
    /** Filtro de grilla por apartado (persiste al cambiar de categoría). */
    var sectionFilterQuery by mutableStateOf("")
        private set
    private val sectionFilters = mutableMapOf(
        NavItem.LIVE to "",
        NavItem.MOVIES to "",
        NavItem.SERIES to "",
    )
    var epgQuery by mutableStateOf("")
        private set
    var epgHits by mutableStateOf<List<EpgSearchHit>>(emptyList())
        private set
    var epgSearching by mutableStateOf(false)
        private set
    var epgSearchStatus by mutableStateOf<String?>(null)
        private set
    var epgSearchTotal by mutableStateOf(0)
        private set
    var epgSearchPage by mutableStateOf(1)
        private set
    var epgSearchHasMore by mutableStateOf(false)
        private set
    /** Tras abrir canal desde Programas, Volver restaura esa pestaña. */
    var returnToEpgSearch by mutableStateOf(false)
        private set
    /** Guía TV (modo Programas): EPG por canal. */
    var guideByStreamId by mutableStateOf<Map<String, ChannelEpgGuide>>(emptyMap())
        private set
    var guideLoading by mutableStateOf(false)
        private set
    var guideStatus by mutableStateOf<String?>(null)
        private set
    var guideDayOffset by mutableStateOf(0)
        private set
    var guideWindowShiftHours by mutableStateOf(0)
        private set

    private var homeJob: Job? = null
    private var sectionJob: Job? = null
    private var detailJob: Job? = null
    private var playJob: Job? = null
    private var searchJob: Job? = null
    private var epgSearchJob: Job? = null
    private var guideJob: Job? = null
    private var pendingTitle: String? = null
    private var pendingTarget: PlaybackTarget? = null
    private var homeRendered = false
    private var sectionRequestSeq: Long = 0L
    private var epgSearchSeq: Long = 0L
    private var guideSeq: Long = 0L
    private val guideInFlight = mutableSetOf<String>()
    /** Cache por apartado: categorías visibles al instante al reentrar. */
    private val categoryCache = mutableMapOf<NavItem, List<CatalogEntry>>()

    fun showSession() {
        homeJob?.cancel()
        sectionJob?.cancel()
        detailJob?.cancel()
        playJob?.cancel()
        searchJob?.cancel()
        nav = NavItem.HOME
        detail = null
        notice = null
        categoryId = ALL_ID
        authorizing = false
        keepSession = graph.prefs.keepSession
        refreshFavoriteIds()
        closePlayer()
        sessionExpired = false
        if (!homeRendered || homeRails.isEmpty()) {
            loadHome()
        }
        // Precarga categorías de apartados en segundo plano.
        prefetchSectionCategories()
    }

    fun openNav(item: NavItem) {
        if (nav == item && detail == null && !(item == NavItem.LIVE && liveMode != LiveMode.CHANNELS)) return
        nav = item
        detail = null
        notice = null
        if (item != NavItem.LIVE) {
            liveMode = LiveMode.CHANNELS
            returnToEpgSearch = false
        } else if (!returnToEpgSearch) {
            liveMode = LiveMode.CHANNELS
        }
        when (item) {
            NavItem.HOME -> {
                categoryId = ALL_ID
                if (!homeRendered || homeRails.isEmpty()) loadHome()
            }
            NavItem.LIVE, NavItem.MOVIES, NavItem.SERIES -> {
                categoryId = ALL_ID
                sectionFilterQuery = sectionFilters[item].orEmpty()
                // Instantáneo: restaurar cache (o al menos All) antes de la red.
                categories = categoryCache[item] ?: listOf(CatalogEntry(ALL_ID, "All"))
                loadSection(item, preserveCategories = (categoryCache[item]?.size ?: 0) > 1)
            }
            NavItem.SEARCH -> {
                sectionRails = emptyList()
            }
            NavItem.CONFIG -> {
                sectionRails = emptyList()
                keepSession = graph.prefs.keepSession
            }
        }
    }

    fun selectCategory(entry: CatalogEntry) {
        val id = if (entry.id == ALL_ID) ALL_ID else entry.id
        categoryId = if (categoryId == id && id != ALL_ID) ALL_ID else id
        detail = null
        notice = null
        // El filtro del apartado se mantiene; solo cambia la categoría.
        loadSection(nav, preserveCategories = true)
    }

    fun onSectionFilter(value: String) {
        val key = when (nav) {
            NavItem.LIVE, NavItem.MOVIES, NavItem.SERIES -> nav
            else -> return
        }
        val trimmed = value.take(64)
        sectionFilters[key] = trimmed
        sectionFilterQuery = trimmed
    }

    fun openEntry(entry: CatalogEntry, kind: CatalogKind) {
        notice = null
        when (kind) {
            CatalogKind.SERIES -> openSeries(entry)
            CatalogKind.VOD_STREAMS -> loadMovie(entry)
            CatalogKind.LIVE_STREAMS -> openLiveChannel(entry, fromEpgSearch = false)
            CatalogKind.LIVE_CATEGORIES, CatalogKind.VOD_CATEGORIES, CatalogKind.SERIES_CATEGORIES ->
                selectCategory(entry)
            else -> showEntry(entry, null)
        }
    }

    fun openChild(child: CatalogEntry) {
        val current = detail ?: return
        notice = null
        when {
            current.isSeriesDrill && current.episodes.any { it.id == child.id } -> playEpisode(child)
            current.isSeriesDrill || current.childKind == CatalogKind.SERIES_SEASONS -> toggleSeason(child)
            current.childKind == CatalogKind.SERIES_EPISODES -> playEpisode(child)
            else -> showEntry(child, null)
        }
    }

    fun toggleSeason(season: CatalogEntry) {
        val current = detail ?: return
        if (!current.isSeriesDrill && current.childKind != CatalogKind.SERIES_SEASONS) return
        notice = null
        if (current.expandedSeasonId == season.id) {
            detail = current.copy(
                expandedSeasonId = null,
                episodes = emptyList(),
                episodesLoading = false,
                seasonNumber = null,
            )
            return
        }
        val seriesId = current.seriesId ?: current.entry.id
        detailJob?.cancel()
        detail = current.copy(
            expandedSeasonId = season.id,
            seasonNumber = season.id,
            episodes = emptyList(),
            episodesLoading = true,
            error = null,
        )
        detailJob = viewModelScope.launch {
            when (val result = graph.catalog.load(CatalogKind.SERIES_EPISODES, seriesId, season.id)) {
                is ApiResult.Ok -> detail = detail?.copy(
                    episodes = result.value,
                    episodesLoading = false,
                )
                is ApiResult.Err -> {
                    markSession(result.error)
                    detail = detail?.copy(
                        episodesLoading = false,
                        error = UserMessages.forError(result.error),
                    )
                }
            }
        }
    }

    private fun playEpisode(episode: CatalogEntry) {
        val current = detail ?: return
        val seriesId = current.seriesId ?: current.entry.id
        val season = current.seasonNumber?.toIntOrNull()
            ?: current.expandedSeasonId?.toIntOrNull()
        val target = if (
            StreamIdPolicy.isSafe(episode.id) &&
            StreamIdPolicy.isSafe(seriesId) &&
            season != null &&
            season in 0..999
        ) {
            PlaybackTarget.SeriesEpisode(episode.id, seriesId, season)
        } else {
            null
        }
        if (target != null && season != null) {
            graph.watch.save(
                WatchProgress(
                    seriesId = seriesId,
                    seasonNumber = season,
                    episodeId = episode.id,
                    episodeTitle = episode.title,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            detail = current.copy(
                playbackTarget = target,
                canPlay = true,
                continueLabel = "Continuar en cap. ${episode.id}",
                resumeEpisodeId = episode.id,
            )
            startPlayback(episode.title, target)
        } else {
            notice = "Este episodio no se puede autorizar todavía."
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        val restorePrograms = returnToEpgSearch
        detail = null
        if (restorePrograms) {
            returnToEpgSearch = false
            nav = NavItem.LIVE
            liveMode = LiveMode.PROGRAMS
        }
    }

    fun selectLiveMode(mode: LiveMode) {
        if (liveMode == mode) return
        liveMode = mode
        detail = null
        notice = null
        if (mode == LiveMode.CHANNELS) {
            returnToEpgSearch = false
            sectionFilterQuery = sectionFilters[NavItem.LIVE].orEmpty()
            if (sectionRails.isEmpty() || sectionRails.firstOrNull()?.kind != CatalogKind.LIVE_STREAMS) {
                loadSection(NavItem.LIVE, preserveCategories = true)
            }
        } else {
            returnToEpgSearch = true
            guideDayOffset = 0
            guideWindowShiftHours = 0
            ensureLiveChannelsForGuide()
            refreshTvGuide(force = guideByStreamId.isEmpty())
        }
    }

    fun onEpgQuery(value: String) {
        // En Programas la búsqueda filtra canales de la guía (local).
        epgQuery = value.take(80)
        epgSearchJob?.cancel()
        epgHits = emptyList()
        epgSearching = false
        epgSearchHasMore = false
        epgSearchStatus = null
    }

    fun runEpgSearch() {
        // Compat: ENTER/BUSCAR en Programas solo cierra teclado; la guía ya está visible.
    }

    fun selectGuideDay(offset: Int) {
        guideDayOffset = offset.coerceIn(-1, 6)
        guideWindowShiftHours = 0
    }

    fun shiftGuideWindow(deltaHours: Int) {
        guideWindowShiftHours = (guideWindowShiftHours + deltaHours).coerceIn(-12, 18)
    }

    fun refreshTvGuide(force: Boolean = false) {
        val channels = liveChannelEntries()
        if (channels.isEmpty()) {
            guideStatus = if (sectionRails.firstOrNull()?.status == RailStatus.Loading) {
                "Cargando canales…"
            } else {
                "No hay canales para mostrar la guía."
            }
            return
        }
        ensureGuideForStreamIds(channels.take(20).map { it.id }, force = force)
    }

    fun ensureGuideCoverage(fromIndex: Int, toIndex: Int, force: Boolean = false) {
        val channels = liveChannelEntries()
        if (channels.isEmpty()) return
        val start = fromIndex.coerceAtLeast(0)
        val endExclusive = (toIndex + 8).coerceIn(start, channels.size)
        ensureGuideForStreamIds(channels.subList(start, endExclusive).map { it.id }, force = force)
    }

    fun ensureGuideForStreamIds(streamIds: List<String>, force: Boolean = false) {
        val ids = streamIds.map { it.trim() }.filter { StreamIdPolicy.isSafe(it) }.distinct()
        val pending = if (force) {
            ids
        } else {
            ids.filter { it !in guideByStreamId && it !in guideInFlight }
        }
        if (pending.isEmpty()) {
            if (guideByStreamId.isNotEmpty()) guideStatus = null
            return
        }
        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            val requestId = ++guideSeq
            guideLoading = guideByStreamId.isEmpty()
            if (guideLoading) guideStatus = "Cargando guía…"
            pending.chunked(20).forEach { chunk ->
                if (requestId != guideSeq) return@launch
                guideInFlight.addAll(chunk)
                when (val result = graph.catalog.batchEpg(chunk, limit = 12)) {
                    is ApiResult.Ok -> {
                        if (requestId != guideSeq) return@launch
                        val received = result.value.associateBy { it.streamId }
                        val next = guideByStreamId.toMutableMap()
                        chunk.forEach { id ->
                            next[id] = received[id] ?: ChannelEpgGuide(
                                streamId = id,
                                generatedAt = null,
                                now = null,
                                next = null,
                                items = emptyList(),
                            )
                        }
                        guideByStreamId = next
                        guideStatus = null
                    }
                    is ApiResult.Err -> {
                        if (requestId != guideSeq) return@launch
                        markSession(result.error)
                        guideStatus = UserMessages.forError(result.error)
                    }
                }
                guideInFlight.removeAll(chunk.toSet())
            }
            if (requestId == guideSeq) guideLoading = false
        }
    }

    fun openGuideChannel(entry: CatalogEntry) {
        returnToEpgSearch = true
        openLiveChannel(entry, fromEpgSearch = true)
    }

    private fun liveChannelEntries(): List<CatalogEntry> {
        return sectionRails
            .filter { it.kind == CatalogKind.LIVE_STREAMS }
            .flatMap { it.items }
    }

    private fun ensureLiveChannelsForGuide() {
        val rail = sectionRails.firstOrNull()
        val ready = rail != null &&
            rail.kind == CatalogKind.LIVE_STREAMS &&
            rail.status == RailStatus.Ready &&
            rail.items.isNotEmpty()
        if (!ready) {
            loadSection(NavItem.LIVE, preserveCategories = true)
        }
    }

    fun loadMoreEpgSearch() {
        if (!epgSearchHasMore || epgSearching) return
        epgSearchJob?.cancel()
        epgSearchJob = viewModelScope.launch {
            runEpgSearch(page = epgSearchPage + 1, append = true)
        }
    }

    fun openEpgHit(hit: EpgSearchHit) {
        val streamId = hit.streamId?.takeIf { StreamIdPolicy.isSafe(it) }
        if (streamId == null) {
            notice = "Canal no disponible para este resultado."
            return
        }
        returnToEpgSearch = true
        // Reutilizar logo/fondo del catálogo de canales (misma ficha que al abrir desde grilla).
        openLiveChannel(resolveLiveEntry(streamId, hit), fromEpgSearch = true)
    }

    private fun resolveLiveEntry(streamId: String, hit: EpgSearchHit): CatalogEntry {
        val cached = (
            sectionRails.filter { it.kind == CatalogKind.LIVE_STREAMS }.flatMap { it.items } +
                homeRails.filter { it.kind == CatalogKind.LIVE_STREAMS }.flatMap { it.items }
            ).firstOrNull { it.id == streamId }
        return CatalogEntry(
            id = streamId,
            title = cached?.title
                ?: hit.channelName?.takeIf { it.isNotBlank() }
                ?: hit.title,
            subtitle = hit.title,
            imageUrl = cached?.imageUrl,
            backdropUrl = cached?.backdropUrl?.takeIf { it.isNotBlank() },
            categoryId = cached?.categoryId,
            plot = hit.description?.takeIf { it.isNotBlank() } ?: cached?.plot,
            genre = cached?.genre,
            rating = cached?.rating,
        )
    }

    private suspend fun runEpgSearch(page: Int, append: Boolean) {
        val q = epgQuery.trim()
        if (q.length < 2) return
        val requestId = ++epgSearchSeq
        epgSearching = true
        epgSearchStatus = if (append) "Cargando más…" else "Buscando…"
        when (val result = graph.catalog.searchEpg(q, page = page, limit = 20)) {
            is ApiResult.Ok -> {
                if (requestId != epgSearchSeq) return
                epgHits = if (append) epgHits + result.value.items else result.value.items
                epgSearchPage = result.value.page
                epgSearchTotal = result.value.total
                epgSearchHasMore = result.value.page < result.value.totalPages
                epgSearchStatus = when {
                    result.value.items.isEmpty() && !append ->
                        "No encontramos ese programa en la guía disponible."
                    result.value.total > 0 ->
                        if (result.value.total == result.value.items.size) {
                            "${result.value.total} resultados"
                        } else {
                            "${epgHits.size} de ${result.value.total} resultados"
                        }
                    else -> null
                }
            }
            is ApiResult.Err -> {
                if (requestId != epgSearchSeq) return
                markSession(result.error)
                if (result.error is AppError.NotFound) {
                    epgSearchStatus = "La búsqueda de programas no está disponible en este momento."
                } else {
                    epgSearchStatus = UserMessages.forError(result.error)
                }
                if (!append) epgHits = emptyList()
            }
        }
        epgSearching = false
    }

    private fun openLiveChannel(entry: CatalogEntry, fromEpgSearch: Boolean) {
        val target = PlaybackTarget.Live(entry.id).takeIf { StreamIdPolicy.isSafe(entry.id) }
        if (target == null) {
            showEntry(entry, null, favoriteKind = "live")
            notice = "Este canal no tiene un identificador válido para reproducir."
            return
        }
        returnToEpgSearch = fromEpgSearch
        detailJob?.cancel()
        detail = DetailUi(
            entry = entry,
            childrenTitle = null,
            children = emptyList(),
            childKind = null,
            loading = false,
            error = null,
            canPlay = true,
            playbackTarget = target,
            favorite = graph.favorites.isFavorite("live", entry.id),
            continueLabel = "Reproducir",
            isLiveChannel = true,
            channelEpg = null,
            epgLoading = true,
            epgError = null,
        )
        detailJob = viewModelScope.launch {
            when (val result = graph.catalog.channelEpg(entry.id, limit = 24)) {
                is ApiResult.Ok -> detail = detail?.copy(
                    channelEpg = result.value,
                    epgLoading = false,
                    epgError = null,
                )
                is ApiResult.Err -> {
                    if (result.error is AppError.Unauthorized || result.error is AppError.SessionExpired) {
                        markSession(result.error)
                    }
                    detail = detail?.copy(
                        epgLoading = false,
                        epgError = when (result.error) {
                            is AppError.NotFound -> "Guía no disponible para este canal."
                            else -> UserMessages.forError(result.error)
                        },
                    )
                }
            }
        }
    }

    fun play(entry: CatalogEntry) {
        val current = detail
        val target = current?.takeIf { it.entry.id == entry.id || current.isSeriesDrill }?.playbackTarget
            ?: resumeTarget(current)
        if (target != null) {
            startPlayback(
                if (current?.isSeriesDrill == true && target is PlaybackTarget.SeriesEpisode) {
                    entry.title
                } else {
                    entry.title
                },
                target,
            )
            return
        }
        // Serie sin progreso: reproducir el primer episodio disponible.
        if (current?.isSeriesDrill == true) {
            playSeriesFromStart(current)
            return
        }
        notice = "Este contenido todavía no incluye datos de reproducción del servicio."
    }

    fun toggleFavorite() {
        val current = detail ?: return
        val kind = when {
            current.playbackTarget is PlaybackTarget.Live -> "live"
            current.playbackTarget is PlaybackTarget.Vod -> "vod"
            current.seriesId != null || current.childKind == CatalogKind.SERIES_SEASONS -> "series"
            current.playbackTarget is PlaybackTarget.SeriesEpisode -> "series"
            else -> graph.favorites.kindOf(CatalogKind.VOD_STREAMS) ?: return
        }
        val nowFavorite = graph.favorites.toggle(current.entry, kind)
        detail = current.copy(favorite = nowFavorite)
        refreshFavoriteIds()
        if (nav == NavItem.LIVE || nav == NavItem.MOVIES || nav == NavItem.SERIES) {
            loadSection(nav, preserveCategories = true)
        }
    }

    fun onSearchQuery(value: String) {
        searchQuery = value.take(80)
    }

    fun runSearch() {
        val q = searchQuery.trim()
        if (q.length < 2) {
            notice = "Escribe al menos 2 caracteres para buscar."
            return
        }
        searchJob?.cancel()
        searching = true
        notice = null
        searchJob = viewModelScope.launch {
            try {
                val results = coroutineScope {
                    val live = async { CatalogKind.LIVE_STREAMS to graph.catalog.load(CatalogKind.LIVE_STREAMS, maxPages = 2) }
                    val vod = async { CatalogKind.VOD_STREAMS to graph.catalog.load(CatalogKind.VOD_STREAMS, maxPages = 2) }
                    val series = async { CatalogKind.SERIES to graph.catalog.load(CatalogKind.SERIES, maxPages = 2) }
                    listOf(live.await(), vod.await(), series.await())
                }
                val needle = q.lowercase()
                val merged = results.flatMap { (kind, result) ->
                    when (result) {
                        is ApiResult.Ok -> result.value
                            .filter { it.title.lowercase().contains(needle) }
                            .map { SearchHit(it, kind) }
                        is ApiResult.Err -> {
                            markSession(result.error)
                            emptyList()
                        }
                    }
                }.distinctBy { "${it.kind}:${it.entry.id}" }
                searchResults = merged
                if (merged.isEmpty()) notice = "No hay resultados para tu búsqueda."
            } finally {
                searching = false
            }
        }
    }

    fun updateKeepSession(value: Boolean) {
        keepSession = value
        graph.prefs.keepSession = value
    }

    fun retryPlayback() {
        val target = playback?.target ?: pendingTarget
        val title = playback?.title ?: pendingTitle
        if (target == null || title == null) {
            closePlayer()
            return
        }
        startPlayback(title, target)
    }

    fun closePlayer() {
        playJob?.cancel()
        authorizing = false
        playback = null
        pendingTitle = null
        pendingTarget = null
    }

    fun consumeSessionExpired() {
        sessionExpired = false
    }

    fun reload() {
        when (nav) {
            NavItem.HOME -> loadHome(force = true)
            NavItem.LIVE, NavItem.MOVIES, NavItem.SERIES -> loadSection(nav)
            else -> Unit
        }
    }

    private fun loadHome(force: Boolean = false) {
        if (!force && homeRendered && homeRails.any { it.status == RailStatus.Ready }) return
        homeJob?.cancel()
        homeRails = listOf(
            RailUi("TV en vivo", CatalogKind.LIVE_STREAMS, RailStatus.Loading),
            RailUi("Películas", CatalogKind.VOD_STREAMS, RailStatus.Loading),
            RailUi("Series", CatalogKind.SERIES, RailStatus.Loading),
        )
        homeJob = viewModelScope.launch {
            homeRails = loadContentRails(
                listOf(
                    "TV en vivo" to CatalogKind.LIVE_STREAMS,
                    "Películas" to CatalogKind.VOD_STREAMS,
                    "Series" to CatalogKind.SERIES,
                ),
                categoryFilter = null,
                maxPages = 1,
            )
            homeRendered = homeRails.any { it.status == RailStatus.Ready }
            // Listados traen backdrop vacío; el detalle sí. Enriquecer hero.
            enrichHomeHeroBackdrops()
        }
    }

    /**
     * El listado VOD/Series deja `backdropUrl` vacío; el GET detalle lo trae.
     * Rellena backdrops del carrusel Home sin tocar el resto del catálogo.
     */
    private fun enrichHomeHeroBackdrops() {
        viewModelScope.launch {
            val snapshot = homeRails
            val enrichedRails = snapshot.map { rail ->
                if (rail.kind != CatalogKind.VOD_STREAMS && rail.kind != CatalogKind.SERIES) {
                    return@map rail
                }
                if (rail.status != RailStatus.Ready || rail.items.isEmpty()) return@map rail
                val detailKind = if (rail.kind == CatalogKind.VOD_STREAMS) {
                    CatalogKind.VOD_STREAM
                } else {
                    CatalogKind.SERIES_DETAIL
                }
                val items = rail.items.toMutableList()
                val limit = minOf(10, items.size)
                val jobs = (0 until limit).map { index ->
                    async {
                        val entry = items[index]
                        val existing = entry.backdropUrl
                        if (!existing.isNullOrBlank() &&
                            !existing.equals(entry.imageUrl, ignoreCase = true)
                        ) {
                            return@async null
                        }
                        when (val detail = graph.catalog.load(detailKind, entry.id)) {
                            is ApiResult.Ok -> {
                                val richer = detail.value.firstOrNull() ?: return@async null
                                val backdrop = richer.backdropUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?.takeIf { !it.equals(entry.imageUrl, ignoreCase = true) }
                                if (backdrop.isNullOrBlank()) return@async null
                                index to entry.copy(
                                    imageUrl = richer.imageUrl ?: entry.imageUrl,
                                    backdropUrl = backdrop,
                                    plot = richer.plot ?: entry.plot,
                                    year = richer.year ?: entry.year,
                                    genre = richer.genre ?: entry.genre,
                                    rating = richer.rating ?: entry.rating,
                                )
                            }
                            is ApiResult.Err -> null
                        }
                    }
                }
                jobs.awaitAll().forEach { pair ->
                    if (pair != null) items[pair.first] = pair.second
                }
                rail.copy(items = items)
            }
            if (enrichedRails != snapshot) {
                homeRails = enrichedRails
            }
        }
    }

    private fun prefetchSectionCategories() {
        viewModelScope.launch {
            listOf(
                NavItem.LIVE to CatalogKind.LIVE_CATEGORIES,
                NavItem.MOVIES to CatalogKind.VOD_CATEGORIES,
                NavItem.SERIES to CatalogKind.SERIES_CATEGORIES,
            ).map { (item, kind) ->
                async {
                    if ((categoryCache[item]?.size ?: 0) > 1) return@async
                    when (val cats = graph.catalog.load(kind, maxPages = 2)) {
                        is ApiResult.Ok -> {
                            val list = listOf(CatalogEntry(ALL_ID, "All")) + cats.value
                            categoryCache[item] = list
                            if (nav == item) categories = list
                        }
                        is ApiResult.Err -> Unit
                    }
                }
            }.awaitAll()
        }
    }

    private fun loadSection(item: NavItem, preserveCategories: Boolean = false) {
        val (catKind, contentKind, contentTitle) = when (item) {
            NavItem.LIVE -> Triple(CatalogKind.LIVE_CATEGORIES, CatalogKind.LIVE_STREAMS, "Canales")
            NavItem.MOVIES -> Triple(CatalogKind.VOD_CATEGORIES, CatalogKind.VOD_STREAMS, "Películas")
            NavItem.SERIES -> Triple(CatalogKind.SERIES_CATEGORIES, CatalogKind.SERIES, "Series")
            else -> return
        }
        sectionJob?.cancel()
        // Nunca vaciar el rail: usar cache o placeholder All.
        if (categories.isEmpty()) {
            categories = categoryCache[item] ?: listOf(CatalogEntry(ALL_ID, "All"))
        }
        // Mantener contenido anterior mientras carga (evita flash de error / vacío).
        val previous = sectionRails.firstOrNull()
            ?.takeIf { it.kind == contentKind && it.status == RailStatus.Ready }
        sectionRails = listOf(
            previous?.copy(status = RailStatus.Ready)
                ?: RailUi(contentTitle, contentKind, RailStatus.Loading),
        )
        val requestId = ++sectionRequestSeq
        sectionJob = viewModelScope.launch {
            val filter = categoryId?.takeIf { it != ALL_ID }
            val cached = categoryCache[item]
            val needCats = !preserveCategories || (cached?.size ?: 0) <= 1 || categories.size <= 1
            // Categorías en paralelo; contenido en 2 pasos: rápido (2 páginas) + completo.
            val fullPages = if (filter == null) 20 else 10
            val catsDeferred = if (needCats) {
                async { graph.catalog.load(catKind, maxPages = 2) }
            } else {
                null
            }
            val quickDeferred = async {
                graph.catalog.load(kind = contentKind, categoryId = filter, maxPages = 2)
            }
            if (catsDeferred != null) {
                when (val cats = catsDeferred.await()) {
                    is ApiResult.Ok -> {
                        if (requestId != sectionRequestSeq) return@launch
                        val list = listOf(CatalogEntry(ALL_ID, "All")) + cats.value
                        categoryCache[item] = list
                        categories = list
                    }
                    is ApiResult.Err -> {
                        if (cats.error is AppError.Unauthorized || cats.error is AppError.SessionExpired) {
                            markSession(cats.error)
                        }
                        if (requestId != sectionRequestSeq) return@launch
                        val fallback = categoryCache[item] ?: listOf(CatalogEntry(ALL_ID, "All"))
                        categoryCache[item] = fallback
                        categories = fallback
                    }
                }
            } else if (cached != null) {
                categories = cached
            }
            val favKind = when (item) {
                NavItem.LIVE -> "live"
                NavItem.MOVIES -> "vod"
                NavItem.SERIES -> "series"
                else -> null
            }
            val favorites = favKind?.let { graph.favorites.list(it) }.orEmpty()
                .filter { filter == null || it.categoryId == null || it.categoryId == filter }
                .map { it.toEntry() }

            fun publish(items: List<CatalogEntry>) {
                val merged = (favorites + items).distinctBy { it.id }
                sectionRails = listOf(
                    if (merged.isEmpty()) {
                        RailUi(contentTitle, contentKind, RailStatus.Empty, message = "No hay contenido disponible.")
                    } else {
                        RailUi(contentTitle, contentKind, RailStatus.Ready, merged)
                    },
                )
                if (item == NavItem.LIVE && liveMode == LiveMode.PROGRAMS && merged.isNotEmpty()) {
                    ensureGuideForStreamIds(merged.take(20).map { it.id }, force = guideByStreamId.isEmpty())
                }
            }

            when (val quick = quickDeferred.await()) {
                is ApiResult.Ok -> {
                    if (requestId != sectionRequestSeq) return@launch
                    publish(quick.value)
                }
                is ApiResult.Err -> {
                    markSession(quick.error)
                    if (requestId != sectionRequestSeq) return@launch
                    if (favorites.isNotEmpty()) {
                        publish(emptyList())
                        notice = UserMessages.forError(quick.error)
                    } else if (previous != null && previous.items.isNotEmpty()) {
                        sectionRails = listOf(previous)
                        notice = UserMessages.forError(quick.error)
                    } else {
                        sectionRails = listOf(
                            RailUi(contentTitle, contentKind, RailStatus.Error, message = UserMessages.forError(quick.error)),
                        )
                    }
                    return@launch
                }
            }

            if (fullPages > 2) {
                when (val full = graph.catalog.load(kind = contentKind, categoryId = filter, maxPages = fullPages)) {
                    is ApiResult.Ok -> {
                        if (requestId != sectionRequestSeq) return@launch
                        if (full.value.size > (sectionRails.firstOrNull()?.items?.size ?: 0)) {
                            publish(full.value)
                        }
                    }
                    is ApiResult.Err -> {
                        // Ya hay grilla rápida; no romper con el fallo del resto.
                        markSession(full.error)
                    }
                }
            }
        }
    }

    private fun openSeries(entry: CatalogEntry) {
        val progress = graph.watch.get(entry.id)
        val continueLabel = progress?.let { "Continuar en cap. ${it.episodeId}" } ?: "Reproducir"
        val resumeTarget = progress?.let {
            PlaybackTarget.SeriesEpisode(it.episodeId, entry.id, it.seasonNumber)
                .takeIf { t -> StreamIdPolicy.isSafe(t.streamId) && StreamIdPolicy.isSafe(t.seriesId) }
        }
        detailJob?.cancel()
        detail = DetailUi(
            entry = entry,
            childrenTitle = "Temporadas",
            children = emptyList(),
            childKind = CatalogKind.SERIES_SEASONS,
            loading = true,
            error = null,
            // Siempre habilitado (blanco): sin progreso reproduce desde el inicio.
            canPlay = true,
            playbackTarget = resumeTarget,
            seriesId = entry.id,
            favorite = graph.favorites.isFavorite("series", entry.id),
            continueLabel = continueLabel,
            resumeEpisodeId = progress?.episodeId,
            isSeriesDrill = true,
            expandedSeasonId = null,
            episodes = emptyList(),
        )
        detailJob = viewModelScope.launch {
            val detailDeferred = async { graph.catalog.load(CatalogKind.SERIES_DETAIL, entry.id) }
            val seasonsDeferred = async { graph.catalog.load(CatalogKind.SERIES_SEASONS, entry.id) }
            val richer = when (val d = detailDeferred.await()) {
                is ApiResult.Ok -> d.value.firstOrNull()
                is ApiResult.Err -> {
                    if (d.error is AppError.Unauthorized || d.error is AppError.SessionExpired) {
                        markSession(d.error)
                    }
                    null
                }
            }
            val merged = mergeDetail(entry, richer)
            // Pintar ficha textual de inmediato (como películas).
            detail = detail?.copy(entry = merged)
            when (val result = seasonsDeferred.await()) {
                is ApiResult.Ok -> {
                    // Sin filtrar por red aquí: evita latencia al abrir la ficha.
                    val seasons = result.value.filterNot {
                        it.id == "0" && it.title.contains("especial", ignoreCase = true)
                    }.ifEmpty { result.value }
                    detail = detail?.copy(
                        entry = merged,
                        children = seasons,
                        loading = false,
                        canPlay = true,
                    )
                }
                is ApiResult.Err -> {
                    markSession(result.error)
                    detail = detail?.copy(
                        entry = merged,
                        loading = false,
                        error = UserMessages.forError(result.error),
                        canPlay = detail?.playbackTarget != null,
                    )
                }
            }
        }
    }

    private fun playSeriesFromStart(current: DetailUi) {
        val seriesId = current.seriesId ?: current.entry.id
        val season = current.children.firstOrNull()
        if (season == null) {
            notice = if (current.loading) "Cargando temporadas…" else "No hay temporadas disponibles."
            return
        }
        notice = null
        detailJob?.cancel()
        detail = current.copy(
            expandedSeasonId = season.id,
            seasonNumber = season.id,
            episodesLoading = true,
        )
        detailJob = viewModelScope.launch {
            when (val result = graph.catalog.load(CatalogKind.SERIES_EPISODES, seriesId, season.id)) {
                is ApiResult.Ok -> {
                    val first = result.value.firstOrNull()
                    detail = detail?.copy(
                        episodes = result.value,
                        episodesLoading = false,
                    )
                    if (first == null) {
                        notice = "No hay episodios en la primera temporada."
                    } else {
                        playEpisode(first)
                    }
                }
                is ApiResult.Err -> {
                    markSession(result.error)
                    detail = detail?.copy(episodesLoading = false, error = UserMessages.forError(result.error))
                    notice = UserMessages.forError(result.error)
                }
            }
        }
    }

    private fun showEntry(entry: CatalogEntry, target: PlaybackTarget?, favoriteKind: String? = null) {
        val kind = favoriteKind ?: when (target) {
            is PlaybackTarget.Live -> "live"
            is PlaybackTarget.Vod -> "vod"
            is PlaybackTarget.SeriesEpisode -> "series"
            null -> null
        }
        detail = DetailUi(
            entry = entry,
            childrenTitle = null,
            children = emptyList(),
            childKind = null,
            loading = false,
            error = null,
            canPlay = target != null,
            playbackTarget = target,
            favorite = kind?.let { graph.favorites.isFavorite(it, entry.id) } ?: false,
            continueLabel = if (target != null) "Reproducir" else null,
            isLiveChannel = target is PlaybackTarget.Live,
        )
    }

    private fun loadMovie(entry: CatalogEntry) {
        detailJob?.cancel()
        val target = PlaybackTarget.Vod(entry.id).takeIf { StreamIdPolicy.isSafe(entry.id) }
        detail = DetailUi(
            entry = entry,
            childrenTitle = null,
            children = emptyList(),
            childKind = null,
            loading = true,
            error = null,
            canPlay = target != null,
            playbackTarget = target,
            favorite = graph.favorites.isFavorite("vod", entry.id),
            continueLabel = "Reproducir",
        )
        detailJob = viewModelScope.launch {
            when (val result = graph.catalog.load(CatalogKind.VOD_STREAM, entry.id)) {
                is ApiResult.Ok -> {
                    val richer = result.value.firstOrNull()
                    showEntry(mergeDetail(entry, richer), target, favoriteKind = "vod")
                }
                is ApiResult.Err -> {
                    markSession(result.error)
                    showEntry(entry, target, favoriteKind = "vod")
                    if (result.error != AppError.NotFound) notice = UserMessages.forError(result.error)
                }
            }
        }
    }

    private fun loadChildren(
        entry: CatalogEntry,
        kind: CatalogKind,
        title: String,
        id: String,
        secondaryId: String?,
    ) {
        detailJob?.cancel()
        val seriesId = if (kind == CatalogKind.SERIES_EPISODES || kind == CatalogKind.SERIES_SEASONS) id else null
        val seasonNumber = if (kind == CatalogKind.SERIES_EPISODES) secondaryId else null
        val progress = seriesId?.let { graph.watch.get(it) }
        detail = DetailUi(
            entry = entry,
            childrenTitle = title,
            children = emptyList(),
            childKind = kind,
            loading = true,
            error = null,
            canPlay = false,
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            favorite = seriesId?.let { graph.favorites.isFavorite("series", it) } ?: false,
            resumeEpisodeId = progress?.episodeId,
        )
        detailJob = viewModelScope.launch {
            when (val result = graph.catalog.load(kind, id, secondaryId)) {
                is ApiResult.Ok -> detail = detail?.copy(
                    children = result.value,
                    loading = false,
                )
                is ApiResult.Err -> {
                    markSession(result.error)
                    detail = detail?.copy(
                        loading = false,
                        error = UserMessages.forError(result.error),
                    )
                }
            }
        }
    }

    private fun resumeTarget(current: DetailUi?): PlaybackTarget? {
        val seriesId = current?.seriesId ?: return null
        val progress = graph.watch.get(seriesId) ?: return null
        return PlaybackTarget.SeriesEpisode(progress.episodeId, seriesId, progress.seasonNumber)
            .takeIf { StreamIdPolicy.isSafe(it.streamId) && StreamIdPolicy.isSafe(it.seriesId) }
    }

    private fun startPlayback(title: String, target: PlaybackTarget) {
        if (authorizing) return
        playJob?.cancel()
        notice = null
        pendingTitle = title
        pendingTarget = target
        authorizing = true
        playback = null
        playJob = viewModelScope.launch {
            try {
                when (val result = graph.catalog.authorize(target)) {
                    is ApiResult.Ok -> {
                        when (val decision = PlaybackUrlPolicy.decide(result.value.streamUrl, graph.allowInsecurePlayback)) {
                            is PlaybackDecision.Allowed -> {
                                playback = PlaybackSession(
                                    title = result.value.title ?: title,
                                    kind = result.value.toKind(),
                                    protocolHint = result.value.protocolHint,
                                    streamUrl = decision.url,
                                    target = target,
                                )
                            }
                            PlaybackDecision.Missing ->
                                notice = if (target is PlaybackTarget.Live) {
                                    UserMessages.forLiveChannelDown()
                                } else {
                                    UserMessages.forPlaybackMissing()
                                }
                            PlaybackDecision.Rejected ->
                                notice = if (target is PlaybackTarget.Live) {
                                    UserMessages.forLiveChannelDown()
                                } else {
                                    UserMessages.forPlaybackRejected()
                                }
                        }
                    }
                    is ApiResult.Err -> {
                        markSession(result.error)
                        notice = if (target is PlaybackTarget.Live) {
                            UserMessages.forAuthorizeLive(result.error)
                        } else {
                            UserMessages.forError(result.error)
                        }
                    }
                }
            } finally {
                authorizing = false
            }
        }
    }

    private suspend fun loadContentRails(
        specs: List<Pair<String, CatalogKind>>,
        categoryFilter: String?,
        maxPages: Int,
    ): List<RailUi> = coroutineScope {
        specs.map { (title, kind) ->
            async {
                when (val result = graph.catalog.load(kind, categoryId = categoryFilter, maxPages = maxPages)) {
                    is ApiResult.Ok -> if (result.value.isEmpty()) {
                        RailUi(title, kind, RailStatus.Empty, message = "No hay contenido disponible.")
                    } else {
                        RailUi(title, kind, RailStatus.Ready, result.value)
                    }
                    is ApiResult.Err -> {
                        markSession(result.error)
                        RailUi(title, kind, RailStatus.Error, message = UserMessages.forError(result.error))
                    }
                }
            }
        }.awaitAll()
    }

    private fun refreshFavoriteIds() {
        favoriteIds = (
            graph.favorites.list("live") +
                graph.favorites.list("vod") +
                graph.favorites.list("series")
            ).map { it.id }.toSet()
    }

    private fun markSession(error: AppError) {
        if (error == AppError.SessionExpired || error == AppError.Unauthorized) {
            sessionExpired = true
        }
    }

    private fun mergeDetail(base: CatalogEntry, richer: CatalogEntry?): CatalogEntry {
        if (richer == null) return base
        return base.copy(
            title = richer.title.ifBlank { base.title },
            subtitle = richer.subtitle ?: base.subtitle,
            imageUrl = richer.imageUrl ?: base.imageUrl,
            plot = richer.plot ?: base.plot,
            director = richer.director ?: base.director,
            genre = richer.genre ?: base.genre,
            cast = richer.cast ?: base.cast,
            year = richer.year ?: base.year,
            rating = richer.rating ?: base.rating,
            duration = richer.duration ?: base.duration,
            backdropUrl = richer.backdropUrl
                ?.takeIf { it.isNotBlank() }
                ?: base.backdropUrl?.takeIf { it.isNotBlank() },
            categoryId = richer.categoryId ?: base.categoryId,
        )
    }

    companion object {
        const val ALL_ID = "__all__"
    }
}

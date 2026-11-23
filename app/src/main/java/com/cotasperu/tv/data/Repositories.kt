package com.cotasperu.tv.data

import com.cotasperu.tv.core.json.CatalogParser
import com.cotasperu.tv.core.json.EpgParser
import com.cotasperu.tv.core.json.PayloadException
import com.cotasperu.tv.core.network.CotasApi
import com.cotasperu.tv.core.network.CotasPaths
import com.cotasperu.tv.core.security.StreamIdPolicy
import com.cotasperu.tv.core.session.SessionCoordinator
import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.CatalogKind
import com.cotasperu.tv.domain.ChannelEpgGuide
import com.cotasperu.tv.domain.EpgSearchPage
import com.cotasperu.tv.domain.HealthSnapshot
import com.cotasperu.tv.domain.PlaybackAuthorization
import com.cotasperu.tv.domain.PlaybackTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthRepository(
    private val api: CotasApi,
) {
    suspend fun check(): ApiResult<HealthSnapshot> = withContext(Dispatchers.IO) {
        api.health()
    }
}

class CatalogRepository(
    private val sessions: SessionCoordinator,
    private val api: CotasApi,
) {
    suspend fun load(
        kind: CatalogKind,
        id: String? = null,
        secondaryId: String? = null,
        categoryId: String? = null,
        maxPages: Int = MAX_PAGES,
    ): ApiResult<List<CatalogEntry>> {
        val path = CotasPaths.forKind(kind, id, secondaryId) ?: return ApiResult.Err(AppError.NotFound)
        val filter = categoryId?.takeIf { StreamIdPolicy.isSafe(it) && kind in FILTERABLE }
        val detailObject = kind in DETAIL_OBJECTS
        fun fetch(token: String): ApiResult<List<CatalogEntry>> {
            return if (kind in PAGED_LISTS) {
                loadPages(path, token, filter, maxPages.coerceIn(1, MAX_PAGES))
            } else {
                loadOnce(path, token, emptyMap(), detailObject)
            }
        }
        val first = sessions.authorized(::fetch)
        // Un reintento silencioso ante fallos de red transitorios (categoría / LAN).
        val transient = first is ApiResult.Err &&
            (first.error is AppError.Network || first.error is AppError.Timeout)
        if (!transient) return first
        return sessions.authorized(::fetch)
    }

    suspend fun authorize(target: PlaybackTarget): ApiResult<PlaybackAuthorization> {
        return sessions.authorized { token -> api.authorizePlayback(token, target) }
    }

    suspend fun channelEpg(streamId: String, limit: Int = 24): ApiResult<ChannelEpgGuide> {
        val path = CotasPaths.liveStreamEpg(streamId) ?: return ApiResult.Err(AppError.Validation)
        val query = mapOf("limit" to limit.coerceIn(1, 50).toString())
        return sessions.authorized { token ->
            when (val raw = api.catalog(path, token, query)) {
                is ApiResult.Ok -> try {
                    ApiResult.Ok(EpgParser.parseChannelGuide(raw.value), raw.correlationId)
                } catch (_: PayloadException) {
                    ApiResult.Err(AppError.UnexpectedResponse(raw.correlationId))
                }
                is ApiResult.Err -> raw
            }
        }
    }

    suspend fun searchEpg(query: String, page: Int = 1, limit: Int = 20): ApiResult<EpgSearchPage> {
        val q = query.trim()
        if (q.length < 2) return ApiResult.Err(AppError.Validation)
        val params = mapOf(
            "q" to q.take(80),
            "page" to page.coerceAtLeast(1).toString(),
            "limit" to limit.coerceIn(1, 50).toString(),
        )
        return sessions.authorized { token ->
            when (val raw = api.catalog(CotasPaths.LIVE_EPG_SEARCH, token, params)) {
                is ApiResult.Ok -> try {
                    ApiResult.Ok(EpgParser.parseSearch(raw.value), raw.correlationId)
                } catch (_: PayloadException) {
                    ApiResult.Err(AppError.UnexpectedResponse(raw.correlationId))
                }
                is ApiResult.Err -> raw
            }
        }
    }

    /** Guía por lotes (máx. 20 streamIds). */
    suspend fun batchEpg(streamIds: List<String>, limit: Int = 12): ApiResult<List<ChannelEpgGuide>> {
        val ids = streamIds
            .map { it.trim() }
            .filter { StreamIdPolicy.isSafe(it) }
            .distinct()
            .take(20)
        if (ids.isEmpty()) return ApiResult.Ok(emptyList())
        val params = mapOf(
            "streamIds" to ids.joinToString(","),
            "limit" to limit.coerceIn(1, 50).toString(),
        )
        return sessions.authorized { token ->
            when (val raw = api.catalog(CotasPaths.LIVE_EPG, token, params)) {
                is ApiResult.Ok -> try {
                    ApiResult.Ok(EpgParser.parseBatchGuide(raw.value), raw.correlationId)
                } catch (_: PayloadException) {
                    ApiResult.Err(AppError.UnexpectedResponse(raw.correlationId))
                }
                is ApiResult.Err -> raw
            }
        }
    }

    private fun loadPages(
        path: String,
        token: String,
        categoryId: String?,
        maxPages: Int,
    ): ApiResult<List<CatalogEntry>> {
        val items = ArrayList<CatalogEntry>()
        var page = 1
        var totalPages = 1
        var correlation: String? = null
        while (page <= totalPages && page <= maxPages) {
            val query = linkedMapOf(
                "page" to page.toString(),
                "limit" to PAGE_LIMIT,
            )
            if (categoryId != null) query["categoryId"] = categoryId
            when (val raw = api.catalog(path, token, query)) {
                is ApiResult.Err -> return raw
                is ApiResult.Ok -> {
                    correlation = raw.correlationId
                    val parsed = try {
                        CatalogParser.parsePage(raw.value)
                    } catch (_: PayloadException) {
                        return ApiResult.Err(AppError.UnexpectedResponse(raw.correlationId))
                    }
                    items += parsed.items
                    totalPages = parsed.totalPages
                    if (parsed.items.isEmpty() || totalPages == 0) break
                    page += 1
                }
            }
        }
        return ApiResult.Ok(items, correlation)
    }

    private fun loadOnce(
        path: String,
        token: String,
        query: Map<String, String>,
        detailObject: Boolean,
    ): ApiResult<List<CatalogEntry>> {
        return when (val raw = api.catalog(path, token, query)) {
            is ApiResult.Ok -> try {
                val items = if (detailObject) {
                    listOf(CatalogParser.parseDetail(raw.value))
                } else {
                    CatalogParser.parseList(raw.value)
                }
                ApiResult.Ok(items, raw.correlationId)
            } catch (_: PayloadException) {
                ApiResult.Err(AppError.UnexpectedResponse(raw.correlationId))
            }
            is ApiResult.Err -> raw
        }
    }

    private companion object {
        const val PAGE_LIMIT = "50"
        const val MAX_PAGES = 20
        val PAGED_LISTS = setOf(
            CatalogKind.LIVE_CATEGORIES,
            CatalogKind.LIVE_STREAMS,
            CatalogKind.VOD_CATEGORIES,
            CatalogKind.VOD_STREAMS,
            CatalogKind.SERIES_CATEGORIES,
            CatalogKind.SERIES,
        )
        val DETAIL_OBJECTS = setOf(
            CatalogKind.SERIES_DETAIL,
            CatalogKind.VOD_STREAM,
        )
        val FILTERABLE = setOf(
            CatalogKind.LIVE_STREAMS,
            CatalogKind.VOD_STREAMS,
            CatalogKind.SERIES,
        )
    }
}

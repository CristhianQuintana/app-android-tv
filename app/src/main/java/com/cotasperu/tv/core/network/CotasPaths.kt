package com.cotasperu.tv.core.network

import com.cotasperu.tv.core.security.ResourceIdPolicy
import com.cotasperu.tv.core.security.StreamIdPolicy
import com.cotasperu.tv.domain.CatalogKind

object CotasPaths {
    const val HEALTH_LIVE = "/api/v1/health/live"
    const val HEALTH_READY = "/api/v1/health/ready"
    const val AUTH_LOGIN = "/api/v1/auth/login"
    const val AUTH_REGISTER = "/api/v1/auth/register"
    const val AUTH_REFRESH = "/api/v1/auth/refresh"
    const val AUTH_LOGOUT = "/api/v1/auth/logout"
    const val AUTH_ME = "/api/v1/auth/me"
    const val AUTH_QR_SESSION = "/api/v1/auth/tv/qr/session"
    @Deprecated("Use AUTH_QR_SESSION")
    const val AUTH_DEVICE_LINK_SESSION = "/api/v1/auth/device-link/session"

    fun deviceLinkPoll(linkCode: String): String? {
        val code = linkCode.trim().uppercase()
        if (!LINK_CODE.matches(code)) return null
        return "$AUTH_DEVICE_LINK_SESSION/$code"
    }

    private val LINK_CODE = Regex("^[A-Z0-9]{6,16}$")
    const val LIVE_CATEGORIES = "/api/v1/catalog/live/categories"
    const val LIVE_STREAMS = "/api/v1/catalog/live/streams"
    const val LIVE_EPG = "/api/v1/catalog/live/epg"
    const val LIVE_EPG_SEARCH = "/api/v1/catalog/live/epg/search"
    const val VOD_CATEGORIES = "/api/v1/catalog/vod/categories"
    const val VOD_STREAMS = "/api/v1/catalog/vod/streams"
    const val SERIES_CATEGORIES = "/api/v1/catalog/series/categories"
    const val SERIES = "/api/v1/catalog/series"
    const val SERIES_STREAMS = "/api/v1/catalog/series/streams"
    const val PLAYBACK_AUTHORIZATIONS = "/api/v1/playback/authorizations"

    fun forKind(kind: CatalogKind, id: String? = null, secondaryId: String? = null): String? {
        return when (kind) {
            CatalogKind.LIVE_CATEGORIES -> LIVE_CATEGORIES
            CatalogKind.LIVE_STREAMS -> LIVE_STREAMS
            CatalogKind.VOD_CATEGORIES -> VOD_CATEGORIES
            CatalogKind.VOD_STREAMS -> VOD_STREAMS
            CatalogKind.VOD_STREAM -> id.safe()?.let { "$VOD_STREAMS/$it" }
            CatalogKind.SERIES_CATEGORIES -> SERIES_CATEGORIES
            CatalogKind.SERIES -> SERIES
            CatalogKind.SERIES_STREAMS -> null
            CatalogKind.SERIES_DETAIL -> id.safe()?.let { "$SERIES/$it" }
            CatalogKind.SERIES_SEASONS -> id.safe()?.let { "$SERIES/$it/seasons" }
            CatalogKind.SERIES_EPISODES -> {
                val seriesId = id.safe() ?: return null
                val seasonId = secondaryId.safe() ?: return null
                "$SERIES/$seriesId/seasons/$seasonId/episodes"
            }
        }
    }

    fun liveStreamEpg(streamId: String): String? =
        streamId.takeIf(StreamIdPolicy::isSafe)?.let { "$LIVE_STREAMS/$it/epg" }

    private fun String?.safe(): String? = this?.takeIf(ResourceIdPolicy::isSafe)
}

package com.cotasperu.tv.domain

data class HealthSnapshot(
    val apiReachable: Boolean,
    val databaseUp: Boolean,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessExpiresAtEpochMs: Long?,
    val displayName: String?,
) {
    override fun toString(): String {
        return "AuthTokens(hasAccess=${accessToken.isNotEmpty()}, hasRefresh=${!refreshToken.isNullOrEmpty()}, expiresAt=$accessExpiresAtEpochMs)"
    }
}

data class UserProfile(
    val displayName: String,
)

/** Respuesta POST /auth/device-link/session */
data class DeviceLinkCreated(
    val sessionId: String = "",
    val linkCode: String,
    val qrPayload: String,
    val authorizationUrl: String = qrPayload,
    val pollSecret: String = "",
    val expiresInSec: Int,
    val pollIntervalMs: Long,
    val deviceId: String,
)

enum class DeviceLinkStatus {
    PENDING,
    APPROVED,
    AUTHORIZED,
    EXPIRED,
    DENIED,
    REJECTED,
    CONSUMED,
    CANCELLED,
}

/** Respuesta GET /auth/device-link/session/{code} */
data class DeviceLinkPoll(
    val status: DeviceLinkStatus,
    val tokens: AuthTokens? = null,
    val deviceName: String? = null,
)

data class CatalogEntry(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val categoryId: String? = null,
    val playbackUrl: String? = null,
    val plot: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val cast: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val duration: String? = null,
    val backdropUrl: String? = null,
) {
    override fun toString(): String = "CatalogEntry(id=$id, title=$title)"
}

sealed class PlaybackTarget {
    data class Live(val streamId: String) : PlaybackTarget()
    data class Vod(val streamId: String) : PlaybackTarget()
    data class SeriesEpisode(
        val streamId: String,
        val seriesId: String,
        val seasonNumber: Int,
    ) : PlaybackTarget()
}

data class PlaybackAuthorization(
    val contentType: String,
    val streamId: String,
    val title: String?,
    val protocolHint: String?,
    val streamUrl: String,
) {
    override fun toString(): String =
        "PlaybackAuthorization(contentType=$contentType, streamId=$streamId, protocolHint=$protocolHint, hasUrl=${streamUrl.isNotEmpty()})"
}

enum class PlaybackKind {
    LIVE,
    VOD,
    SERIES,
}

data class PlaybackSession(
    val title: String,
    val kind: PlaybackKind,
    val protocolHint: String?,
    val streamUrl: String,
    val target: PlaybackTarget,
) {
    val isLive: Boolean get() = kind == PlaybackKind.LIVE

    override fun toString(): String =
        "PlaybackSession(title=$title, kind=$kind, protocolHint=$protocolHint, hasUrl=${streamUrl.isNotEmpty()})"
}

fun PlaybackAuthorization.toKind(): PlaybackKind = when (contentType) {
    "live" -> PlaybackKind.LIVE
    "series" -> PlaybackKind.SERIES
    else -> PlaybackKind.VOD
}

enum class CatalogKind {
    LIVE_CATEGORIES,
    LIVE_STREAMS,
    VOD_CATEGORIES,
    VOD_STREAMS,
    VOD_STREAM,
    SERIES_CATEGORIES,
    SERIES,
    SERIES_STREAMS,
    SERIES_DETAIL,
    SERIES_SEASONS,
    SERIES_EPISODES,
}

sealed class AppError {
    data object InvalidCredentials : AppError()
    data object Validation : AppError()
    data object Conflict : AppError()
    data class RateLimited(val correlationId: String?) : AppError()
    data object Unauthorized : AppError()
    data object Forbidden : AppError()
    /** Límite de sesiones/dispositivos concurrentes (login 403 DEVICE_LIMIT). */
    data object DeviceLimit : AppError()
    data object DeviceLinkInvalid : AppError()
    data object DeviceLinkExpired : AppError()
    data object NotFound : AppError()
    data object Network : AppError()
    data object Timeout : AppError()
    data object Unavailable : AppError()
    data class UnexpectedResponse(val correlationId: String?) : AppError()
    data object SessionExpired : AppError()
    data object DatabaseUnavailable : AppError()
    data object TransportRejected : AppError()
    data object PayloadTooLarge : AppError()
}

sealed class ApiResult<out T> {
    data class Ok<T>(val value: T, val correlationId: String? = null) : ApiResult<T>()
    data class Err(val error: AppError) : ApiResult<Nothing>()
}

enum class CredentialIssue {
    OK,
    BLANK_USER,
    INVALID_USER,
    BLANK_PASSWORD,
    PASSWORD_TOO_LONG,
}

sealed class LogoutOutcome {
    data object Revoked : LogoutOutcome()
    data object ClearedLocally : LogoutOutcome()
}

sealed class RestoreOutcome {
    data class Ready(val profile: UserProfile) : RestoreOutcome()
    data object NeedsLogin : RestoreOutcome()
    data class Unverified(val error: AppError) : RestoreOutcome()
}

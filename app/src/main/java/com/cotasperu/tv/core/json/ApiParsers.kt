package com.cotasperu.tv.core.json

import com.cotasperu.tv.core.security.TokenPolicy
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.AuthTokens
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.DeviceLinkCreated
import com.cotasperu.tv.domain.DeviceLinkPoll
import com.cotasperu.tv.domain.DeviceLinkStatus
import com.cotasperu.tv.domain.PlaybackAuthorization
import com.cotasperu.tv.domain.UserProfile

object HealthParser {
    fun isLive(json: String): Boolean = runCatching {
        JsonTree.parse(json).string("status") == "ok"
    }.getOrDefault(false)

    fun isDatabaseUp(json: String): Boolean = runCatching {
        val root = JsonTree.parse(json)
        root.string("status") == "ok" && root.objectChild("checks")?.string("database") == "up"
    }.getOrDefault(false)
}

object ApiErrorParser {
    private val correlation = Regex("^[A-Za-z0-9-]{8,80}$")

    fun parse(json: String?, httpStatus: Int, headerCorrelation: String?): AppError {
        val reference = sanitize(headerCorrelation)
        val body = json?.takeIf { it.isNotBlank() }?.let {
            runCatching { JsonTree.parse(it) }.getOrNull()
        }
        val code = body?.string("code")
        val bodyReference = sanitize(body?.string("correlationId")) ?: reference
        return when {
            code == "INVALID_CREDENTIALS" -> AppError.InvalidCredentials
            code == "UNAUTHORIZED" -> AppError.Unauthorized
            // 401 sin código (body vacío / proxy): en login es credencial inválida, no sesión.
            httpStatus == 401 -> AppError.InvalidCredentials
            code == "VALIDATION_ERROR" || httpStatus == 400 -> AppError.Validation
            code == "CONFLICT" || httpStatus == 409 -> AppError.Conflict
            code == "RATE_LIMITED" || httpStatus == 429 -> AppError.RateLimited(bodyReference)
            code == "DEVICE_LIMIT" -> AppError.DeviceLimit
            code == "DEVICE_LINK_INVALID" || (httpStatus == 404 && code == "DEVICE_LINK_INVALID") ->
                AppError.DeviceLinkInvalid
            code == "DEVICE_LINK_EXPIRED" || httpStatus == 410 -> AppError.DeviceLinkExpired
            httpStatus == 403 -> AppError.Forbidden
            code == "NOT_FOUND" || httpStatus == 404 -> AppError.NotFound
            code == "DEPENDENCY_TIMEOUT" || httpStatus == 504 -> AppError.Timeout
            code == "PLAYBACK_REFERENCE_REJECTED" || httpStatus == 502 -> AppError.Unavailable
            httpStatus in 500..599 -> AppError.Unavailable
            else -> AppError.UnexpectedResponse(bodyReference)
        }
    }

    private fun sanitize(value: String?): String? = value?.takeIf { correlation.matches(it) }
}

object AuthParser {
    private const val MAX_EXPIRES_SECONDS = 90L * 24L * 60L * 60L

    fun parseSession(json: String, nowEpochMs: Long): AuthTokens {
        val root = JsonTree.parse(json)
        val carrier = when {
            root.string("accessToken") != null -> root
            root.objectChild("data")?.string("accessToken") != null -> root.objectChild("data")
            else -> null
        } ?: throw PayloadException()
        val access = carrier.string("accessToken")?.takeIf(TokenPolicy::isUsable) ?: throw PayloadException()
        val refresh = when {
            carrier.child("refreshToken") == null -> null
            else -> carrier.string("refreshToken")?.takeIf(TokenPolicy::isUsable) ?: throw PayloadException()
        }
        val tokenType = carrier.string("tokenType")
        if (tokenType != null && !tokenType.equals("Bearer", ignoreCase = true)) throw PayloadException()
        val expiresIn = carrier.child("expiresIn")?.integral()
        val expiresAt = expiresIn?.takeIf { it in 1..MAX_EXPIRES_SECONDS }?.let { nowEpochMs + it * 1000L }
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            accessExpiresAtEpochMs = expiresAt,
            displayName = displayName(carrier),
        )
    }

    fun parseProfile(json: String): UserProfile {
        val root = JsonTree.parse(json)
        val carrier = root.objectChild("data") ?: root
        val name = displayName(carrier) ?: displayName(carrier.objectChild("user") ?: carrier)
        return UserProfile(name ?: "Cuenta COTAS")
    }

    fun parseDeviceLinkCreated(json: String): DeviceLinkCreated {
        val root = JsonTree.parse(json)
        val node = root.objectChild("data") ?: root
        val sessionId = node.string("sessionId")?.trim().orEmpty()
        val pollSecret = node.string("pollSecret")?.trim().orEmpty()
        val authorizationUrl = node.string("authorizationUrl")?.trim().orEmpty()
        val linkCode = node.string("linkCode")?.trim()?.uppercase().orEmpty()
        val qrPayload = authorizationUrl.ifEmpty { node.string("qrPayload")?.trim().orEmpty() }
        val deviceId = node.string("deviceId")?.trim().orEmpty()
        if (qrPayload.isEmpty() || (!sessionId.isEmpty() && pollSecret.isEmpty())) throw PayloadException()
        if (sessionId.isNotEmpty()) {
            if (!qrPayload.startsWith("https://")) throw PayloadException()
        } else {
            if (linkCode.isEmpty() || deviceId.isEmpty()) throw PayloadException()
            if (!qrPayload.startsWith("cotas://device-link?code=", ignoreCase = true)) throw PayloadException()
        }
        val expiresIn = node.child("expiresIn")?.integral()?.toInt()?.coerceIn(1, 600) ?: 180
        val pollMs = node.child("pollIntervalMs")?.integral()?.coerceIn(1000L, 10_000L) ?: 2500L
        return DeviceLinkCreated(
            sessionId = sessionId,
            linkCode = linkCode,
            qrPayload = qrPayload,
            authorizationUrl = authorizationUrl.ifEmpty { qrPayload },
            pollSecret = pollSecret,
            expiresInSec = expiresIn,
            pollIntervalMs = pollMs,
            deviceId = deviceId,
        )
    }

    fun parseDeviceLinkPoll(json: String, nowEpochMs: Long): DeviceLinkPoll {
        val root = JsonTree.parse(json)
        val node = root.objectChild("data") ?: root
        val statusRaw = node.string("status")?.trim()?.lowercase().orEmpty()
        val status = when (statusRaw) {
            "pending" -> DeviceLinkStatus.PENDING
            "approved" -> DeviceLinkStatus.APPROVED
            "authorized" -> DeviceLinkStatus.AUTHORIZED
            "expired" -> DeviceLinkStatus.EXPIRED
            "denied" -> DeviceLinkStatus.DENIED
            "rejected" -> DeviceLinkStatus.REJECTED
            "consumed" -> DeviceLinkStatus.CONSUMED
            "cancelled", "canceled" -> DeviceLinkStatus.CANCELLED
            else -> throw PayloadException()
        }
        val tokens = if (status == DeviceLinkStatus.APPROVED || status == DeviceLinkStatus.AUTHORIZED) {
            runCatching { parseSession(json, nowEpochMs) }.getOrNull()
                ?: throw PayloadException()
        } else {
            null
        }
        val deviceName = node.objectChild("device")?.string("deviceName")?.trim()?.take(64)
        return DeviceLinkPoll(status = status, tokens = tokens, deviceName = deviceName)
    }

    private fun displayName(node: JsonValue?): String? {
        if (node == null) return null
        val raw = node.string("username") ?: node.string("displayName") ?: node.objectChild("user")?.string("username")
        val clean = raw?.trim()?.take(80) ?: return null
        if (clean.isEmpty() || clean.any { it.isISOControl() }) return null
        return clean
    }
}

data class CatalogPage(
    val items: List<CatalogEntry>,
    val page: Int,
    val totalPages: Int,
)

object CatalogParser {
    private val idKeys = listOf("id", "category_id", "categoryId", "stream_id", "streamId", "series_id", "seriesId", "season_id", "seasonId", "episode_id", "episodeId", "number")
    private val titleKeys = listOf("name", "category_name", "categoryName", "title", "stream_display_name", "series_name", "season_name", "episode_name")
    private val plotKeys = listOf("plot", "description", "overview")
    private val posterKeys = listOf("posterUrl", "cover", "poster", "stream_icon", "iconUrl", "logo", "movie_image")
    private val backdropKeys = listOf(
        "backdropUrl",
        "backdropThumbUrl",
        "backdrop",
        "backdrop_path",
        "cover_big",
        "fanart",
        "banner",
    )
    private val categoryKeys = listOf("category_id", "categoryId")
    private val listKeys = listOf("data", "items", "categories", "streams", "series", "seasons", "episodes", "results")

    fun parseList(json: String): List<CatalogEntry> = parsePage(json).items

    fun parsePage(json: String): CatalogPage {
        val root = JsonTree.parse(json)
        val items = readItems(root)
        val page = root.child("page")?.integral()?.toInt()?.coerceAtLeast(1) ?: 1
        val totalPagesRaw = root.child("totalPages")?.integral()?.toInt()
        val totalPages = when {
            totalPagesRaw == null -> if (items.isEmpty()) 0 else 1
            totalPagesRaw <= 0 -> 0
            else -> totalPagesRaw
        }
        return CatalogPage(items, page, totalPages)
    }

    /**
     * Ficha única (serie/película): el JSON trae seasons/episodes anidados;
     * no hay que interpretar esos arrays como el ítem principal.
     */
    fun parseDetail(json: String): CatalogEntry {
        val root = JsonTree.parse(json)
        val node = when (root) {
            is JsonValue.Obj -> root.objectChild("data") ?: root
            else -> root
        }
        val item = when (node) {
            is JsonValue.Obj -> entry(node)
            is JsonValue.Arr -> node.items.firstNotNullOfOrNull(::entry)
            else -> null
        } ?: throw PayloadException()
        return item
    }

    private fun readItems(root: JsonValue): List<CatalogEntry> {
        return when (root) {
            is JsonValue.Arr -> root.items.mapNotNull(::entry)
            is JsonValue.Obj -> {
                val nested = listKeys.firstNotNullOfOrNull { key -> root.arrayChild(key) }
                when {
                    nested != null -> nested.mapNotNull(::entry)
                    entry(root) != null -> listOfNotNull(entry(root))
                    else -> throw PayloadException()
                }
            }
            else -> throw PayloadException()
        }
    }

    private fun entry(value: JsonValue): CatalogEntry? {
        val id = firstString(value, idKeys)?.trim().orEmpty()
        val title = firstString(value, titleKeys)?.trim().orEmpty()
        if (id.isEmpty() || title.isEmpty()) return null
        if (id.length > 80 || title.length > 240) return null
        if (id.any { it.isISOControl() } || title.any { it.isISOControl() }) return null
        val plot = firstString(value, plotKeys)?.trim()?.take(1200)
        val year = firstString(value, listOf("year"))?.trim()?.takeIf { it.isNotEmpty() }
        val rating = firstString(value, listOf("rating"))?.trim()?.takeIf { it.isNotEmpty() && it != "0" }
        val genre = firstString(value, listOf("genre"))?.trim()?.take(200)
        val director = firstString(value, listOf("director"))?.trim()?.take(200)
        val cast = firstString(value, listOf("cast", "actors"))?.trim()?.take(300)
        val duration = firstString(value, listOf("duration", "runtime"))?.trim()?.take(32)
        val poster = firstImage(value, posterKeys)
        // Listados a menudo traen backdropUrl vacío; no sustituir con el póster.
        val backdrop = firstImage(value, backdropKeys)
            ?.takeIf { poster == null || !it.equals(poster, ignoreCase = true) }
        val episodeCount = value.child("episodeCount")?.integral()?.toInt()?.takeIf { it > 0 }
            ?.let { if (it == 1) "1 episodio" else "$it episodios" }
        val cardSub = listOfNotNull(year, rating, genre?.take(40), episodeCount)
            .joinToString(" · ")
            .takeIf { it.isNotEmpty() }
            ?: plot?.take(120)
        return CatalogEntry(
            id = id,
            title = title,
            subtitle = cardSub,
            imageUrl = poster ?: backdrop,
            categoryId = firstString(value, categoryKeys)?.trim()?.takeIf { it.length <= 80 }
                ?: firstCategoryId(value),
            playbackUrl = null,
            plot = plot,
            director = director,
            genre = genre,
            cast = cast,
            year = year,
            rating = rating,
            duration = duration,
            backdropUrl = backdrop,
        )
    }

    /** String o primer elemento de array (p. ej. backdrop_path XUI). */
    private fun firstImage(value: JsonValue, keys: List<String>): String? {
        for (key in keys) {
            val child = value.child(key) ?: continue
            when (child) {
                is JsonValue.Str -> {
                    val img = safeImage(child.value)
                    if (img != null) return img
                }
                is JsonValue.Arr -> {
                    for (item in child.items) {
                        val text = (item as? JsonValue.Str)?.value ?: continue
                        val img = safeImage(text)
                        if (img != null) return img
                    }
                }
                else -> Unit
            }
        }
        return null
    }

    private fun firstCategoryId(value: JsonValue): String? {
        val items = value.arrayChild("categoryIds") ?: return null
        for (item in items) {
            val text = when (item) {
                is JsonValue.Str -> item.value.trim()
                is JsonValue.Num -> item.integral()?.toString()
                else -> null
            }
            if (!text.isNullOrEmpty() && text.length <= 80) return text
        }
        return null
    }

    private fun firstString(value: JsonValue, keys: List<String>): String? {
        for (key in keys) {
            val text = value.string(key)
            if (!text.isNullOrBlank()) return text
            val number = value.child(key)?.integral()
            if (number != null) return number.toString()
        }
        return null
    }

    private fun safeImage(raw: String): String? {
        val value = raw.trim()
        if (value.length > 2048 || value.any { it.isWhitespace() }) return null
        val scheme = value.substringBefore(':', "")
        if (!scheme.equals("https", true) && !scheme.equals("http", true)) return null
        if (value.substringAfter("://", "").contains("@")) return null
        return value
    }
}

object PlaybackParser {
    fun parse(json: String): PlaybackAuthorization {
        val root = JsonTree.parse(json)
        val node = root.objectChild("data") ?: root
        val contentType = node.string("contentType")?.trim().orEmpty()
        val streamId = node.string("streamId")?.trim().orEmpty()
        val streamUrl = node.string("streamUrl")?.trim().orEmpty()
        if (contentType !in setOf("live", "vod", "series")) throw PayloadException()
        if (streamId.isEmpty() || streamUrl.isEmpty()) throw PayloadException()
        val title = node.string("title")?.trim()?.take(180)?.takeIf { it.isNotEmpty() }
        val protocolHint = node.string("protocolHint")?.trim()?.take(32)?.takeIf { it.isNotEmpty() }
        return PlaybackAuthorization(
            contentType = contentType,
            streamId = streamId,
            title = title,
            protocolHint = protocolHint,
            streamUrl = streamUrl,
        )
    }
}

object JsonBodies {
    fun login(
        username: String,
        password: String,
        deviceId: String,
        platform: String,
        deviceName: String,
    ): String = objectBody(
        "username" to username,
        "password" to password,
        "deviceId" to deviceId,
        "platform" to platform,
        "deviceName" to deviceName,
    )

    fun deviceLinkSession(deviceId: String, platform: String, deviceName: String): String = objectBody(
        "deviceId" to deviceId,
        "platform" to platform,
        "deviceName" to deviceName,
    )

    fun register(email: String, password: String): String = objectBody(
        "email" to email,
        "password" to password,
    )

    fun refresh(refreshToken: String): String = objectBody("refreshToken" to refreshToken)

    fun playbackLive(streamId: String): String = objectBody(
        "contentType" to "live",
        "streamId" to streamId,
    )

    fun playbackVod(streamId: String): String = objectBody(
        "contentType" to "vod",
        "streamId" to streamId,
    )

    fun playbackSeries(streamId: String, seriesId: String, seasonNumber: Int): String {
        val buffer = okio.Buffer()
        val writer = com.squareup.moshi.JsonWriter.of(buffer)
        writer.beginObject()
        writer.name("contentType").value("series")
        writer.name("streamId").value(streamId)
        writer.name("seriesId").value(seriesId)
        writer.name("seasonNumber").value(seasonNumber.toLong())
        writer.endObject()
        writer.close()
        return buffer.readUtf8()
    }

    private fun objectBody(vararg fields: Pair<String, String>): String {
        val buffer = okio.Buffer()
        val writer = com.squareup.moshi.JsonWriter.of(buffer)
        writer.beginObject()
        for ((name, value) in fields) {
            writer.name(name).value(value)
        }
        writer.endObject()
        writer.close()
        return buffer.readUtf8()
    }
}

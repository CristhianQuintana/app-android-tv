package com.cotasperu.tv.core.network

import com.cotasperu.tv.core.json.ApiErrorParser
import com.cotasperu.tv.core.json.AuthParser
import com.cotasperu.tv.core.json.HealthParser
import com.cotasperu.tv.core.json.JsonBodies
import com.cotasperu.tv.core.json.PayloadException
import com.cotasperu.tv.core.json.PlaybackParser
import com.cotasperu.tv.core.logging.DiagnosticLog
import com.cotasperu.tv.core.security.StreamIdPolicy
import com.cotasperu.tv.core.security.TokenPolicy
import com.cotasperu.tv.core.security.TransportPolicy
import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.AuthTokens
import com.cotasperu.tv.domain.DeviceLinkCreated
import com.cotasperu.tv.domain.DeviceLinkPoll
import com.cotasperu.tv.domain.HealthSnapshot
import com.cotasperu.tv.domain.PlaybackAuthorization
import com.cotasperu.tv.domain.PlaybackTarget
import com.cotasperu.tv.domain.UserProfile
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class CotasApiClient(
    baseUrl: String,
    private val lanHttpOrigin: String,
    private val enforcePinnedOrigin: Boolean = true,
    private val client: OkHttpClient = defaultClient(),
    private val log: DiagnosticLog = DiagnosticLog.silent(),
    private val now: () -> Long = System::currentTimeMillis,
    private val bodyLimitBytes: Long = MAX_BODY_BYTES,
) : CotasApi {
    private val origin = baseUrl.trim().trimEnd('/')
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    override fun health(): ApiResult<HealthSnapshot> {
        val live = get(CotasPaths.HEALTH_LIVE, token = null, operation = "health_live")
        if (live is ApiResult.Err) return live
        val liveBody = (live as ApiResult.Ok).value
        if (!HealthParser.isLive(liveBody)) {
            return ApiResult.Err(AppError.UnexpectedResponse(live.correlationId))
        }
        val ready = get(CotasPaths.HEALTH_READY, token = null, operation = "health_ready")
        if (ready is ApiResult.Err) return ready
        val readyBody = (ready as ApiResult.Ok).value
        val databaseUp = HealthParser.isDatabaseUp(readyBody)
        if (!databaseUp && !readyBody.contains("database")) {
            return ApiResult.Err(AppError.UnexpectedResponse(ready.correlationId))
        }
        return ApiResult.Ok(
            HealthSnapshot(apiReachable = true, databaseUp = databaseUp),
            ready.correlationId,
        )
    }

    override fun login(
        username: String,
        password: String,
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<AuthTokens> {
        val body = JsonBodies.login(
            username = username.trim(),
            password = password,
            deviceId = deviceId.trim(),
            platform = platform.trim().ifEmpty { "tv" },
            deviceName = deviceName.trim().ifEmpty { "Google TV" },
        )
        return post(CotasPaths.AUTH_LOGIN, body, token = null, operation = "login") { json, _ ->
            AuthParser.parseSession(json, now())
        }
    }

    override fun register(email: String, password: String): ApiResult<AuthTokens> {
        val body = JsonBodies.register(email.trim(), password)
        return post(CotasPaths.AUTH_REGISTER, body, token = null, operation = "register") { json, _ ->
            AuthParser.parseSession(json, now())
        }
    }

    override fun createDeviceLinkSession(
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<DeviceLinkCreated> {
        val body = JsonBodies.deviceLinkSession(
            deviceId = deviceId.trim(),
            platform = platform.trim().ifEmpty { "tv" },
            deviceName = deviceName.trim().ifEmpty { "Google TV" },
        )
        return post(CotasPaths.AUTH_QR_SESSION, body, token = null, operation = "qr_session_create") { json, _ ->
            AuthParser.parseDeviceLinkCreated(json)
        }
    }

    override fun pollDeviceLinkSession(linkCode: String, deviceId: String): ApiResult<DeviceLinkPoll> {
        val path = CotasPaths.deviceLinkPoll(linkCode) ?: return ApiResult.Err(AppError.Validation)
        val query = mapOf("deviceId" to deviceId.trim())
        return get(path, token = null, operation = "device_link_poll", query = query).map { json ->
            AuthParser.parseDeviceLinkPoll(json, now())
        }
    }

    override fun pollQrSession(sessionId: String, pollSecret: String): ApiResult<DeviceLinkPoll> {
        val id = sessionId.trim()
        if (id.isEmpty() || id.length > 128) return ApiResult.Err(AppError.Validation)
        val path = "${CotasPaths.AUTH_QR_SESSION}/$id/status"
        val request = request(path, pollSecret).get().build()
        return execute(request, "qr_session_poll") { json, _ ->
            AuthParser.parseDeviceLinkPoll(json, now())
        }
    }

    override fun cancelQrSession(sessionId: String, pollSecret: String): ApiResult<Unit> {
        val id = sessionId.trim()
        if (id.isEmpty() || id.length > 128) return ApiResult.Err(AppError.Validation)
        val path = "${CotasPaths.AUTH_QR_SESSION}/$id/cancel"
        return post(path, "{}", pollSecret, operation = "qr_session_cancel") { _, _ -> Unit }
    }

    override fun refresh(refreshToken: String): ApiResult<AuthTokens> {
        if (!TokenPolicy.isUsable(refreshToken)) return ApiResult.Err(AppError.SessionExpired)
        val body = JsonBodies.refresh(refreshToken)
        return post(CotasPaths.AUTH_REFRESH, body, token = null, operation = "refresh") { json, _ ->
            AuthParser.parseSession(json, now())
        }
    }

    override fun logout(accessToken: String): ApiResult<Unit> {
        return post(CotasPaths.AUTH_LOGOUT, "{}", accessToken, operation = "logout") { _, _ -> Unit }
    }

    override fun profile(accessToken: String): ApiResult<UserProfile> {
        return get(CotasPaths.AUTH_ME, accessToken, operation = "profile").map { json ->
            AuthParser.parseProfile(json)
        }
    }

    override fun catalog(path: String, accessToken: String, query: Map<String, String>): ApiResult<String> {
        if (!path.startsWith("/api/v1/catalog/") || path.contains('?')) return ApiResult.Err(AppError.NotFound)
        if (query.keys.any { it !in ALLOWED_QUERY }) return ApiResult.Err(AppError.Validation)
        return get(path, accessToken, operation = "catalog", query = query)
    }

    override fun authorizePlayback(accessToken: String, target: PlaybackTarget): ApiResult<PlaybackAuthorization> {
        val body = when (target) {
            is PlaybackTarget.Live -> {
                if (!StreamIdPolicy.isSafe(target.streamId)) return ApiResult.Err(AppError.Validation)
                JsonBodies.playbackLive(target.streamId)
            }
            is PlaybackTarget.Vod -> {
                if (!StreamIdPolicy.isSafe(target.streamId)) return ApiResult.Err(AppError.Validation)
                JsonBodies.playbackVod(target.streamId)
            }
            is PlaybackTarget.SeriesEpisode -> {
                if (!StreamIdPolicy.isSafe(target.streamId) || !StreamIdPolicy.isSafe(target.seriesId)) {
                    return ApiResult.Err(AppError.Validation)
                }
                if (target.seasonNumber !in 0..999) return ApiResult.Err(AppError.Validation)
                JsonBodies.playbackSeries(target.streamId, target.seriesId, target.seasonNumber)
            }
        }
        return post(CotasPaths.PLAYBACK_AUTHORIZATIONS, body, accessToken, operation = "playback_auth") { json, _ ->
            PlaybackParser.parse(json)
        }
    }

    private fun get(
        path: String,
        token: String?,
        operation: String,
        query: Map<String, String> = emptyMap(),
    ): ApiResult<String> {
        val request = request(path, token, query).get().build()
        return execute(request, operation) { json, _ -> json }
    }

    private fun <T> post(
        path: String,
        jsonBody: String,
        token: String?,
        operation: String,
        parse: (String, String?) -> T,
    ): ApiResult<T> {
        val request = request(path, token)
            .post(jsonBody.toRequestBody(jsonType))
            .build()
        return execute(request, operation, parse)
    }

    private fun request(
        path: String,
        token: String?,
        query: Map<String, String> = emptyMap(),
    ): Request.Builder {
        val builder = origin.toHttpUrlOrNull()?.newBuilder() ?: return Request.Builder().url("https://invalid.invalid/")
        builder.encodedPath(path)
        query.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        val url = builder.build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
        if (token != null) {
            if (!TokenPolicy.isUsable(token)) return Request.Builder().url("https://invalid.invalid/")
            request.header("Authorization", "Bearer $token")
        }
        return request
    }

    private fun <T> execute(
        request: Request,
        operation: String,
        parse: (String, String?) -> T,
    ): ApiResult<T> {
        if (enforcePinnedOrigin && !TransportPolicy.allows(origin, lanHttpOrigin)) {
            log.event(operation, status = 0)
            return ApiResult.Err(AppError.TransportRejected)
        }
        if (request.url.host == "invalid.invalid") {
            return ApiResult.Err(AppError.SessionExpired)
        }
        return try {
            client.newCall(request).execute().use { response ->
                val correlation = response.header("x-correlation-id")
                log.event(operation, response.code, correlation)
                if (response.code in 300..399) {
                    return ApiResult.Err(AppError.UnexpectedResponse(sanitizeCorrelation(correlation)))
                }
                val text = readLimited(response)
                if (response.isSuccessful) {
                    try {
                        ApiResult.Ok(parse(text, correlation), sanitizeCorrelation(correlation))
                    } catch (_: PayloadException) {
                        ApiResult.Err(AppError.UnexpectedResponse(sanitizeCorrelation(correlation)))
                    } catch (_: RuntimeException) {
                        ApiResult.Err(AppError.UnexpectedResponse(sanitizeCorrelation(correlation)))
                    }
                } else {
                    ApiResult.Err(ApiErrorParser.parse(text, response.code, correlation))
                }
            }
        } catch (_: PayloadTooLargeException) {
            log.event(operation, status = 0)
            ApiResult.Err(AppError.PayloadTooLarge)
        } catch (_: SocketTimeoutException) {
            log.event(operation, status = 0)
            ApiResult.Err(AppError.Timeout)
        } catch (_: IOException) {
            log.event(operation, status = 0)
            ApiResult.Err(AppError.Network)
        }
    }

    private fun readLimited(response: Response): String {
        val body = response.body ?: return ""
        val advertised = body.contentLength()
        if (advertised > bodyLimitBytes) throw PayloadTooLargeException()
        val source = body.source()
        val buffer = Buffer()
        var total = 0L
        while (true) {
            val read = source.read(buffer, 8_192)
            if (read == -1L) break
            total += read
            if (total > bodyLimitBytes) throw PayloadTooLargeException()
        }
        return buffer.readUtf8()
    }

    private fun sanitizeCorrelation(value: String?): String? {
        return value?.takeIf { it.length in 8..80 && it.all { char -> char.isLetterOrDigit() || char == '-' } }
    }

    private class PayloadTooLargeException : IOException()

    companion object {
        const val MAX_BODY_BYTES = 2_000_000L
        val ALLOWED_QUERY = setOf("page", "limit", "categoryId", "search", "q", "streamIds")

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}

private fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Ok -> {
            try {
                ApiResult.Ok(transform(value), correlationId)
            } catch (_: PayloadException) {
                ApiResult.Err(AppError.UnexpectedResponse(correlationId))
            } catch (_: RuntimeException) {
                ApiResult.Err(AppError.UnexpectedResponse(correlationId))
            }
        }
        is ApiResult.Err -> this
    }
}

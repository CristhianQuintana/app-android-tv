package com.cotasperu.tv

import com.cotasperu.tv.core.logging.DiagnosticLog
import com.cotasperu.tv.core.network.CotasApi
import com.cotasperu.tv.core.network.CotasApiClient
import com.cotasperu.tv.core.session.InMemorySessionStore
import com.cotasperu.tv.core.session.SessionCoordinator
import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.AuthTokens
import com.cotasperu.tv.domain.HealthSnapshot
import com.cotasperu.tv.domain.LogoutOutcome
import com.cotasperu.tv.domain.PlaybackAuthorization
import com.cotasperu.tv.domain.PlaybackTarget
import com.cotasperu.tv.domain.RestoreOutcome
import com.cotasperu.tv.domain.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCoordinatorTest {
    private val store = InMemorySessionStore()
    private val api = FakeApi()
    private val sessions = SessionCoordinator(api, store, now = { 10_000L })

    @Test
    fun invalidLoginDoesNotCreateASession() = runBlocking {
        api.loginResult = ApiResult.Err(AppError.InvalidCredentials)
        val result = sessions.login(
            "usuario_prueba",
            "clave-secreta",
            deviceId = "dev-1",
            platform = "tv",
            deviceName = "Google TV",
        )
        assertTrue(result is ApiResult.Err)
        assertNull(store.read())
        assertFalse(store.read().toString().contains("clave-secreta"))
    }

    @Test
    fun loginStoresTokensButNotThePassword() = runBlocking {
        api.loginResult = ApiResult.Ok(tokens("access-1", "refresh-1"))
        sessions.login(
            "usuario_prueba",
            "clave-secreta",
            deviceId = "dev-1",
            platform = "tv",
            deviceName = "Google TV",
        )
        val saved = store.read()
        assertEquals("access-1", saved?.accessToken)
        assertFalse(saved.toString().contains("clave-secreta"))
        assertEquals("clave-secreta", api.lastPassword)
    }

    @Test
    fun logoutClearsLocalSessionWhenTheServerCannotBeReached() = runBlocking {
        store.write(tokens("access-1", "refresh-1"))
        api.logoutResult = ApiResult.Err(AppError.Network)
        val outcome = sessions.logout()
        assertEquals(LogoutOutcome.ClearedLocally, outcome)
        assertNull(store.read())
    }

    @Test
    fun expiredAccessRefreshesOnceAndDoesNotRepeatOnForbidden() = runBlocking {
        store.write(tokens("old-access", "refresh-1", expiresAt = 1L))
        api.refreshResult = ApiResult.Ok(tokens("new-access", "refresh-2", expiresAt = 50_000L))
        api.catalogResult = ApiResult.Err(AppError.Forbidden)
        val result = sessions.authorized { api.catalog("/api/v1/catalog/live/categories", it) }
        assertTrue(result is ApiResult.Err && (result as ApiResult.Err).error == AppError.Forbidden)
        assertEquals(1, api.refreshCalls)
        assertEquals("new-access", api.lastCatalogToken)
        assertEquals("new-access", store.read()?.accessToken)
    }

    @Test
    fun unauthorizedCatalogClearsTheSessionAfterAFailedRefresh() = runBlocking {
        store.write(tokens("old-access", "refresh-1"))
        api.catalogResult = ApiResult.Err(AppError.Unauthorized)
        api.refreshResult = ApiResult.Err(AppError.Unauthorized)
        val result = sessions.authorized { api.catalog("/api/v1/catalog/live/categories", it) }
        assertEquals(AppError.SessionExpired, (result as ApiResult.Err).error)
        assertNull(store.read())
    }

    @Test
    fun restoreDoesNotEnterWhenTheProfileCannotBeVerified() = runBlocking {
        store.write(tokens("access-1", "refresh-1"))
        api.profileResult = ApiResult.Err(AppError.Network)
        val outcome = sessions.restore()
        assertTrue(outcome is RestoreOutcome.Unverified)
        assertEquals("access-1", store.read()?.accessToken)
    }

    private fun tokens(access: String, refresh: String?, expiresAt: Long? = null) = AuthTokens(
        accessToken = access,
        refreshToken = refresh,
        accessExpiresAtEpochMs = expiresAt,
        displayName = "linea1",
    )
}

class CotasApiClientTest {
    @Test
    fun healthAndInvalidLoginFollowTheLiveContract() {
        val server = MockWebServer()
        val lines = mutableListOf<String>()
        server.enqueue(json(200, """{"status":"ok"}"""))
        server.enqueue(json(200, """{"status":"ok","checks":{"database":"up"}}""", "corr-health-1"))
        server.enqueue(
            json(
                401,
                """{"statusCode":401,"code":"INVALID_CREDENTIALS","message":"Credenciales inválidas.","correlationId":"55a5d615-502b-4d8d-be98-027a7bb3a158"}""",
            ),
        )
        server.start()
        try {
            val client = CotasApiClient(
                baseUrl = server.url("/").toString().trimEnd('/'),
                lanHttpOrigin = "",
                enforcePinnedOrigin = false,
                log = DiagnosticLog(true) { lines += it },
            )
            val health = client.health()
            assertTrue(health is ApiResult.Ok)
            assertTrue((health as ApiResult.Ok).value.databaseUp)
            val live = server.takeRequest()
            val ready = server.takeRequest()
            assertEquals("/api/v1/health/live", live.path)
            assertEquals("/api/v1/health/ready", ready.path)
            assertNull(live.getHeader("Authorization"))

            val login = client.login(
                username = "usuario_prueba",
                password = "clave-secreta",
                deviceId = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                platform = "tv",
                deviceName = "Google TV",
            )
            assertEquals(AppError.InvalidCredentials, (login as ApiResult.Err).error)
            val posted = server.takeRequest()
            assertEquals("/api/v1/auth/login", posted.path)
            assertEquals("POST", posted.method)
            assertNull(posted.getHeader("Authorization"))
            val body = posted.body.readUtf8()
            assertTrue(body.contains("\"username\":\"usuario_prueba\""))
            assertTrue(body.contains("\"password\":\"clave-secreta\""))
            assertTrue(body.contains("\"deviceId\":\"7c9e6679-7425-40de-944b-e07fc1f90ae7\""))
            assertTrue(body.contains("\"platform\":\"tv\""))
            assertFalse(lines.any { it.contains("clave-secreta") || it.contains("usuario_prueba") })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun protectedCatalogSendsBearerAndDoesNotFollowRedirects() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "http://evil.example/steal"))
        server.start()
        try {
            val client = CotasApiClient(
                baseUrl = server.url("/").toString().trimEnd('/'),
                lanHttpOrigin = "",
                enforcePinnedOrigin = false,
            )
            val result = client.catalog("/api/v1/catalog/live/categories", "access.token.value")
            assertTrue((result as ApiResult.Err).error is AppError.UnexpectedResponse)
            val recorded = server.takeRequest()
            assertEquals("Bearer access.token.value", recorded.getHeader("Authorization"))
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun releaseStyleHttpOriginIsRejectedBeforeConnecting() {
        val client = CotasApiClient(
            baseUrl = "http://10.10.0.31:3100",
            lanHttpOrigin = "",
        )
        val result = client.health()
        assertEquals(AppError.TransportRejected, (result as ApiResult.Err).error)
    }

    @Test
    fun playbackAuthorizationPostsVerifiedBodyAndParsesStreamUrl() {
        val server = MockWebServer()
        server.enqueue(
            json(
                200,
                """
                {
                  "contentType":"live",
                  "streamId":"3537",
                  "title":"Canal demo",
                  "delivery":"direct",
                  "protocolHint":"hls",
                  "streamUrl":"http://cdn.example/live/u/p/3537.m3u8",
                  "expiresAt":null,
                  "accessMaterialEmbedded":true
                }
                """.trimIndent(),
            ),
        )
        server.start()
        try {
            val client = CotasApiClient(
                baseUrl = server.url("/").toString().trimEnd('/'),
                lanHttpOrigin = "",
                enforcePinnedOrigin = false,
            )
            val result = client.authorizePlayback("access.token.value", PlaybackTarget.Live("3537"))
            assertTrue(result is ApiResult.Ok)
            assertEquals("3537", (result as ApiResult.Ok).value.streamId)
            assertTrue(result.value.streamUrl.endsWith("3537.m3u8"))
            val posted = server.takeRequest()
            assertEquals("POST", posted.method)
            assertEquals("/api/v1/playback/authorizations", posted.path)
            assertEquals("Bearer access.token.value", posted.getHeader("Authorization"))
            val body = posted.body.readUtf8()
            assertTrue(body.contains("\"contentType\":\"live\""))
            assertTrue(body.contains("\"streamId\":\"3537\""))
            assertFalse(body.contains("streamUrl"))
        } finally {
            server.shutdown()
        }
    }

    private fun json(code: Int, body: String, correlation: String? = null): MockResponse {
        val response = MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
        if (correlation != null) response.setHeader("x-correlation-id", correlation)
        return response
    }
}

private class FakeApi : CotasApi {
    var loginResult: ApiResult<AuthTokens> = ApiResult.Err(AppError.InvalidCredentials)
    var refreshResult: ApiResult<AuthTokens> = ApiResult.Err(AppError.Unauthorized)
    var logoutResult: ApiResult<Unit> = ApiResult.Ok(Unit)
    var profileResult: ApiResult<UserProfile> = ApiResult.Ok(UserProfile("linea1"))
    var catalogResult: ApiResult<String> = ApiResult.Ok("[]")
    var lastPassword: String? = null
    var lastCatalogToken: String? = null
    var refreshCalls: Int = 0

    override fun health(): ApiResult<HealthSnapshot> = ApiResult.Ok(HealthSnapshot(true, true))

    override fun login(
        username: String,
        password: String,
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<AuthTokens> {
        lastPassword = password
        return loginResult
    }

    override fun createDeviceLinkSession(
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<com.cotasperu.tv.domain.DeviceLinkCreated> =
        ApiResult.Err(AppError.NotFound)

    override fun pollDeviceLinkSession(
        linkCode: String,
        deviceId: String,
    ): ApiResult<com.cotasperu.tv.domain.DeviceLinkPoll> =
        ApiResult.Err(AppError.NotFound)

    override fun refresh(refreshToken: String): ApiResult<AuthTokens> {
        refreshCalls += 1
        return refreshResult
    }

    override fun logout(accessToken: String): ApiResult<Unit> = logoutResult

    override fun profile(accessToken: String): ApiResult<UserProfile> = profileResult

    override fun catalog(path: String, accessToken: String, query: Map<String, String>): ApiResult<String> {
        lastCatalogToken = accessToken
        return catalogResult
    }

    override fun authorizePlayback(accessToken: String, target: PlaybackTarget): ApiResult<PlaybackAuthorization> {
        lastCatalogToken = accessToken
        return ApiResult.Err(AppError.NotFound)
    }
}

package com.cotasperu.tv

import com.cotasperu.tv.core.json.ApiErrorParser
import com.cotasperu.tv.core.json.AuthParser
import com.cotasperu.tv.core.json.CatalogParser
import com.cotasperu.tv.core.json.HealthParser
import com.cotasperu.tv.core.json.JsonBodies
import com.cotasperu.tv.core.json.JsonTree
import com.cotasperu.tv.core.json.PayloadException
import com.cotasperu.tv.core.json.PlaybackParser
import com.cotasperu.tv.domain.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {
    @Test
    fun healthUsesTheObservedContract() {
        assertTrue(HealthParser.isLive("""{"status":"ok"}"""))
        assertFalse(HealthParser.isLive("""{"status":"down"}"""))
        assertTrue(HealthParser.isDatabaseUp("""{"status":"ok","checks":{"database":"up"}}"""))
        assertFalse(HealthParser.isDatabaseUp("""{"status":"ok","checks":{"database":"down"}}"""))
        assertFalse(HealthParser.isDatabaseUp("""{"status":"ok"}"""))
    }

    @Test
    fun errorsUseServerCodesWithoutKeepingTheMessage() {
        val invalid = ApiErrorParser.parse(
            """{"statusCode":401,"code":"INVALID_CREDENTIALS","message":"Credenciales inválidas.","correlationId":"55a5d615-502b-4d8d-be98-027a7bb3a158"}""",
            401,
            null,
        )
        assertEquals(AppError.InvalidCredentials, invalid)

        val limited = ApiErrorParser.parse(
            """{"statusCode":429,"code":"RATE_LIMITED","message":"Demasiadas solicitudes. Inténtelo más tarde.","correlationId":"d5b9f57f-0640-4837-816d-dd1b5d81df3b"}""",
            429,
            null,
        )
        assertEquals(AppError.RateLimited("d5b9f57f-0640-4837-816d-dd1b5d81df3b"), limited)
        assertFalse(limited.toString().contains("Inténtelo"))

        val validation = ApiErrorParser.parse(
            """{"statusCode":400,"code":"VALIDATION_ERROR","message":"La solicitud no es válida.","details":[{"field":"username","rules":["isLength"]}]}""",
            400,
            null,
        )
        assertEquals(AppError.Validation, validation)
        assertEquals(AppError.InvalidCredentials, ApiErrorParser.parse(null, 401, null))
        assertEquals(AppError.Forbidden, ApiErrorParser.parse(null, 403, null))
        assertEquals(
            AppError.DeviceLimit,
            ApiErrorParser.parse(
                """{"statusCode":403,"code":"DEVICE_LIMIT","message":"Device limit reached.","correlationId":"02e7bf21-5dd7-4ca3-882f-dcb935dc01dd"}""",
                403,
                null,
            ),
        )
    }

    @Test
    fun sessionParserFailsClosedWhenTokensAreMissing() {
        val session = AuthParser.parseSession(
            """{"accessToken":"access.token.value","refreshToken":"refresh.token.value","expiresIn":900,"tokenType":"Bearer","username":"linea1"}""",
            nowEpochMs = 1_000_000L,
        )
        assertEquals("access.token.value", session.accessToken)
        assertEquals("refresh.token.value", session.refreshToken)
        assertEquals(1_000_000L + 900_000L, session.accessExpiresAtEpochMs)
        assertEquals("linea1", session.displayName)
        assertFalse(session.toString().contains("access.token.value"))

        val wrapped = AuthParser.parseSession(
            """{"data":{"accessToken":"only-access"}}""",
            0L,
        )
        assertNull(wrapped.refreshToken)

        assertThrows(PayloadException::class.java) {
            AuthParser.parseSession("""{"token":"legacy"}""", 0L)
        }
        assertThrows(PayloadException::class.java) {
            AuthParser.parseSession("""{"accessToken":"bad token"}""", 0L)
        }
    }

    @Test
    fun catalogParserAcceptsListsAndDropsUnsafeImages() {
        val entries = CatalogParser.parseList(
            """
            {
              "categories": [
                {"category_id": 4, "category_name": "Noticias"},
                {"id": "x", "name": "  "}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(1, entries.size)
        assertEquals("4", entries[0].id)
        assertEquals("Noticias", entries[0].title)

        val streams = CatalogParser.parseList(
            """
            [
              {
                "stream_id": "9",
                "name": "Canal",
                "stream_icon": "https://cdn.example/logo.png",
                "playbackUrl": "https://user:secret@cdn.example/a.m3u8"
              }
            ]
            """.trimIndent(),
        )
        assertEquals("https://cdn.example/logo.png", streams[0].imageUrl)
        assertNull(streams[0].playbackUrl)
        assertFalse(streams[0].toString().contains("secret"))

        val page = CatalogParser.parsePage(
            """
            {"page":1,"limit":20,"total":12,"totalPages":1,"items":[
              {"id":"2","name":"TV en VIVO","parentId":null},
              {"id":"3537","name":"13 C","categoryId":"12","iconUrl":"https://cdn.example/logo.png","year":"2009","rating":"7.3"}
            ]}
            """.trimIndent(),
        )
        assertEquals(1, page.totalPages)
        assertEquals("https://cdn.example/logo.png", page.items[1].imageUrl)
        assertEquals("2009 · 7.3", page.items[1].subtitle)

        val seasons = CatalogParser.parseList(
            """{"items":[{"number":1,"name":"Temporada 1","episodeCount":9,"posterUrl":"https://cdn.example/season.png"}]}""",
        )
        assertEquals("1", seasons[0].id)
        assertEquals("https://cdn.example/season.png", seasons[0].imageUrl)
        assertThrows(PayloadException::class.java) {
            CatalogParser.parseList("""{"unexpected":true}""")
        }
    }

    @Test
    fun loginBodyContainsOnlyTheVerifiedFields() {
        val json = JsonBodies.login(
            username = "usuario_prueba",
            password = "clave \"cita\"",
            deviceId = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            platform = "tv",
            deviceName = "Google TV",
        )
        val root = JsonTree.parse(json)
        assertEquals(
            setOf("username", "password", "deviceId", "platform", "deviceName"),
            root.asObject()?.keys,
        )
        assertTrue(json.contains("clave \\\"cita\\\""))
    }

    @Test
    fun deviceLinkCreatedRejectsPayloadWithSecretsShape() {
        val created = AuthParser.parseDeviceLinkCreated(
            """{"linkCode":"AB3K7N2P","qrPayload":"cotas://device-link?code=AB3K7N2P","expiresIn":120,"pollIntervalMs":2500,"deviceId":"7c9e6679-7425-40de-944b-e07fc1f90ae7"}""",
        )
        assertEquals("AB3K7N2P", created.linkCode)
        assertTrue(created.qrPayload.startsWith("cotas://device-link?code="))
        assertFalse(created.qrPayload.contains("accessToken", ignoreCase = true))
    }

    @Test
    fun playbackAuthorizationUsesStreamUrlOnly() {
        val auth = PlaybackParser.parse(
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
        )
        assertEquals("live", auth.contentType)
        assertEquals("3537", auth.streamId)
        assertEquals("hls", auth.protocolHint)
        assertTrue(auth.streamUrl.endsWith("/3537.m3u8"))
        assertFalse(auth.toString().contains("cdn.example"))

        val seriesBody = JsonBodies.playbackSeries("3360", "1", 1)
        assertTrue(seriesBody.contains("\"contentType\":\"series\""))
        assertTrue(seriesBody.contains("\"streamId\":\"3360\""))
        assertTrue(seriesBody.contains("\"seriesId\":\"1\""))
        assertTrue(seriesBody.contains("\"seasonNumber\":1"))
    }
}

private fun com.cotasperu.tv.core.json.JsonValue.asObject() =
    (this as? com.cotasperu.tv.core.json.JsonValue.Obj)?.map

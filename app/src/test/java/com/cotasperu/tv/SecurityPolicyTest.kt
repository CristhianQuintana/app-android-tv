package com.cotasperu.tv

import com.cotasperu.tv.core.logging.DiagnosticLog
import com.cotasperu.tv.core.network.CotasPaths
import com.cotasperu.tv.core.security.CredentialPolicy
import com.cotasperu.tv.core.security.PlaybackDecision
import com.cotasperu.tv.core.security.PlaybackUrlPolicy
import com.cotasperu.tv.core.security.TransportPolicy
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.CatalogKind
import com.cotasperu.tv.domain.CredentialIssue
import com.cotasperu.tv.domain.UserMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPolicyTest {
    @Test
    fun transportAllowsHttpsAndOnlyTheKnownLanOrigin() {
        val lan = "http://10.10.0.31:3100"
        assertTrue(TransportPolicy.allows("https://api.example.com", lanHttpOrigin = ""))
        assertTrue(TransportPolicy.allows("https://api.cotasperu.com/api/v1", lanHttpOrigin = ""))
        assertTrue(TransportPolicy.allows(lan, lanHttpOrigin = lan))
        assertFalse(TransportPolicy.allows(lan, lanHttpOrigin = ""))
        assertFalse(TransportPolicy.allows("http://10.10.0.31:3101", lanHttpOrigin = lan))
        assertFalse(TransportPolicy.allows("http://evil.example", lanHttpOrigin = lan))
        assertFalse(TransportPolicy.allows("https://user:pass@api.example.com", lanHttpOrigin = ""))
    }

    @Test
    fun playbackAllowsPublicApiProxyTicket() {
        val url =
            "https://api.cotasperu.com/api/v1/playback/media/56fe70de84484fde94c369dc403f953d"
        val decision = PlaybackUrlPolicy.decide(url, allowInsecureHttp = false)
        assertTrue(decision is PlaybackDecision.Allowed)
        assertEquals(url, (decision as PlaybackDecision.Allowed).url)
    }

    @Test
    fun credentialsMatchObservedLoginRules() {
        assertEquals(CredentialIssue.OK, CredentialPolicy.validate("usuario_prueba", "x"))
        assertEquals(CredentialIssue.INVALID_USER, CredentialPolicy.validate("ab", "clave"))
        assertEquals(CredentialIssue.INVALID_USER, CredentialPolicy.validate("bad user", "clave"))
        assertEquals(CredentialIssue.INVALID_USER, CredentialPolicy.validate("user@mail.com", "clave"))
        assertEquals(CredentialIssue.BLANK_PASSWORD, CredentialPolicy.validate("usuario_prueba", ""))
        assertEquals(CredentialIssue.PASSWORD_TOO_LONG, CredentialPolicy.validate("usuario_prueba", "p".repeat(129)))
    }

    @Test
    fun invalidCredentialsMessageDoesNotRevealAccounts() {
        val message = UserMessages.forError(AppError.InvalidCredentials)
        assertFalse(message.contains("no existe", ignoreCase = true))
        assertFalse(message.contains("usuario_prueba"))
    }

    @Test
    fun playbackRejectsCredentialsInTheUrl() {
        assertTrue(PlaybackUrlPolicy.decide("https://cdn.example/live/index.m3u8", false) is PlaybackDecision.Allowed)
        assertTrue(PlaybackUrlPolicy.decide("http://10.1.1.8/live/index.m3u8", true) is PlaybackDecision.Allowed)
        assertTrue(
            PlaybackUrlPolicy.decide("http://10.1.1.8:8080/live/demoUser/demoPass/3537.m3u8", true)
                is PlaybackDecision.Allowed,
        )
        // Proxy LAN de pruebas: streamUrl del authorize sin reescritura.
        val proxy = PlaybackUrlPolicy.decide(
            "http://10.10.0.31:3100/api/v1/playback/media/56fe70de84484fde94c369dc403f953d",
            true,
        )
        assertTrue(proxy is PlaybackDecision.Allowed)
        assertEquals(
            "http://10.10.0.31:3100/api/v1/playback/media/56fe70de84484fde94c369dc403f953d",
            (proxy as PlaybackDecision.Allowed).url,
        )
        assertEquals(PlaybackDecision.Rejected, PlaybackUrlPolicy.decide("http://10.1.1.8/live/index.m3u8", false))
        assertEquals(PlaybackDecision.Rejected, PlaybackUrlPolicy.decide("https://user:secret@cdn.example/a.m3u8", true))
        assertEquals(PlaybackDecision.Rejected, PlaybackUrlPolicy.decide("https://cdn.example/a.m3u8?password=secret", true))
        assertEquals(PlaybackDecision.Missing, PlaybackUrlPolicy.decide("  ", true))
    }

    @Test
    fun resourceIdsCannotChangeTheRequestPath() {
        assertEquals("/api/v1/catalog/series/abc/seasons/12/episodes", CotasPaths.forKind(CatalogKind.SERIES_EPISODES, "abc", "12"))
        assertNull(CotasPaths.forKind(CatalogKind.SERIES_EPISODES, "../admin", "1"))
        assertNull(CotasPaths.forKind(CatalogKind.VOD_STREAM, "a/b"))
        assertNull(CotasPaths.forKind(CatalogKind.SERIES_STREAMS))
        assertEquals("/api/v1/playback/authorizations", CotasPaths.PLAYBACK_AUTHORIZATIONS)
    }

    @Test
    fun diagnosticLogDropsSecretsAndBodies() {
        val lines = mutableListOf<String>()
        val log = DiagnosticLog(enabled = true, sink = lines::add)
        log.event("login", 401, "55a5d615-502b-4d8d-be98-027a7bb3a158")
        log.event("login password=secret token=abc", 500, "not a correlation")
        assertEquals(2, lines.size)
        assertFalse(lines.joinToString().contains("secret"))
        assertFalse(lines.joinToString().contains("token"))
        assertTrue(lines[0].contains("correlation=55a5d615-502b-4d8d-be98-027a7bb3a158"))
    }
}

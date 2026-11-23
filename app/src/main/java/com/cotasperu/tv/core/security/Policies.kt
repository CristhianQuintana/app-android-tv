package com.cotasperu.tv.core.security

import com.cotasperu.tv.domain.CredentialIssue
import java.net.URI

object TransportPolicy {
    /**
     * HTTPS con host válido (p. ej. https://api.cotasperu.com o .../api/v1).
     * HTTP solo si coincide exactamente con [lanHttpOrigin] (debug LAN).
     */
    fun allows(baseUrl: String, lanHttpOrigin: String): Boolean {
        val normalized = baseUrl.trim().trimEnd('/')
        if (normalized.startsWith("https://", ignoreCase = true)) {
            return isSafeHttpsOrigin(normalized)
        }
        val allowedOrigin = lanHttpOrigin.trim().trimEnd('/')
        return allowedOrigin.isNotEmpty() &&
            normalized.equals(allowedOrigin, ignoreCase = true) &&
            allowedOrigin.startsWith("http://", ignoreCase = true)
    }

    private fun isSafeHttpsOrigin(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo.isNullOrBlank() &&
                uri.host!!.none { it.isWhitespace() }
        } catch (_: Exception) {
            false
        }
    }
}

object CredentialPolicy {
    private val username = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{2,99}$")
    const val MAX_PASSWORD = 128

    fun validate(rawUsername: String, password: String): CredentialIssue {
        val user = rawUsername.trim()
        if (user.isEmpty()) return CredentialIssue.BLANK_USER
        if (!username.matches(user)) return CredentialIssue.INVALID_USER
        if (password.isEmpty()) return CredentialIssue.BLANK_PASSWORD
        if (password.length > MAX_PASSWORD) return CredentialIssue.PASSWORD_TOO_LONG
        return CredentialIssue.OK
    }
}

object TokenPolicy {
    fun isUsable(token: String): Boolean {
        return token.length in 1..4096 && token.none { it <= ' ' || it == '\u007f' }
    }
}

object ResourceIdPolicy {
    private val allowed = Regex("^[A-Za-z0-9_-]{1,80}$")

    fun isSafe(id: String): Boolean = allowed.matches(id)
}

object StreamIdPolicy {
    private val digits = Regex("^[0-9]{1,12}$")

    fun isSafe(id: String): Boolean = digits.matches(id)
}

sealed class PlaybackDecision {
    data class Allowed(val url: String) : PlaybackDecision()
    data object Missing : PlaybackDecision()
    data object Rejected : PlaybackDecision()
}

object PlaybackUrlPolicy {
    private val sensitiveQuery = Regex("(^|[?&])(password|token|access_token|refresh_token|username)=", RegexOption.IGNORE_CASE)

    /**
     * Acepta exactamente el streamUrl del authorize (mismo host API /playback/media/{ticket}).
     * No reescribe hosts. HTTP solo si [allowInsecureHttp] (debug LAN).
     */
    fun decide(raw: String?, allowInsecureHttp: Boolean): PlaybackDecision {
        if (raw.isNullOrBlank()) return PlaybackDecision.Missing
        val value = raw.trim()
        if (value.length > 2048 || value.any { it.isWhitespace() || it == '\u0000' }) {
            return PlaybackDecision.Rejected
        }
        val scheme = value.substringBefore(':', "")
        val isHttps = scheme.equals("https", ignoreCase = true)
        val isHttp = scheme.equals("http", ignoreCase = true)
        if (!isHttps && !(isHttp && allowInsecureHttp)) return PlaybackDecision.Rejected
        val remainder = value.substringAfter("://", "")
        if (remainder.isEmpty() || remainder.startsWith("@") || remainder.contains("@")) {
            return PlaybackDecision.Rejected
        }
        if (sensitiveQuery.containsMatchIn(value)) return PlaybackDecision.Rejected
        return PlaybackDecision.Allowed(value)
    }
}

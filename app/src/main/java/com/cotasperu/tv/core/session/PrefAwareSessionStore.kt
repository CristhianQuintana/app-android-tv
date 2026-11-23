package com.cotasperu.tv.core.session

import com.cotasperu.tv.core.prefs.PrefsStore
import com.cotasperu.tv.domain.AuthTokens

/**
 * Persiste tokens solo si “Mantener sesión” está activo (paridad Samsung).
 */
class PrefAwareSessionStore(
    private val persistent: SessionStore,
    private val prefs: PrefsStore,
    private val memory: InMemorySessionStore = InMemorySessionStore(),
) : SessionStore {
    override fun read(): AuthTokens? {
        if (!prefs.keepSession) return memory.read()
        return persistent.read() ?: memory.read()
    }

    override fun write(tokens: AuthTokens) {
        memory.write(tokens)
        if (prefs.keepSession) {
            persistent.write(tokens)
        } else {
            persistent.clear()
        }
    }

    override fun clear() {
        memory.clear()
        persistent.clear()
    }
}

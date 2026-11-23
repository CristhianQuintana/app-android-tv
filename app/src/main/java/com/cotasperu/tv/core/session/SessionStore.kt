@file:Suppress("DEPRECATION")

package com.cotasperu.tv.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cotasperu.tv.core.security.TokenPolicy
import com.cotasperu.tv.domain.AuthTokens

interface SessionStore {
    fun read(): AuthTokens?
    fun write(tokens: AuthTokens)
    fun clear()
}

class InMemorySessionStore : SessionStore {
    private var current: AuthTokens? = null

    override fun read(): AuthTokens? = current

    override fun write(tokens: AuthTokens) {
        current = tokens
    }

    override fun clear() {
        current = null
    }
}

class EncryptedSessionStore(context: Context) : SessionStore {
    private val memory = InMemorySessionStore()
    private val preferences: SharedPreferences? = open(context)

    override fun read(): AuthTokens? {
        val stored = preferences?.let(::readPreferences)
        return stored ?: memory.read()
    }

    override fun write(tokens: AuthTokens) {
        if (!TokenPolicy.isUsable(tokens.accessToken)) return
        memory.write(tokens)
        val prefs = preferences ?: return
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putLong(KEY_EXPIRES, tokens.accessExpiresAtEpochMs ?: 0L)
            .putString(KEY_NAME, tokens.displayName)
            .apply()
    }

    override fun clear() {
        memory.clear()
        preferences?.edit()?.clear()?.apply()
    }

    private fun readPreferences(prefs: SharedPreferences): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        if (!TokenPolicy.isUsable(access)) return null
        val refresh = prefs.getString(KEY_REFRESH, null)?.takeIf(TokenPolicy::isUsable)
        val expires = prefs.getLong(KEY_EXPIRES, 0L).takeIf { it > 0L }
        val name = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
        return AuthTokens(access, refresh, expires, name)
    }

    private fun open(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val FILE_NAME = "cotas_session"
        const val KEY_ACCESS = "at"
        const val KEY_REFRESH = "rt"
        const val KEY_EXPIRES = "exp"
        const val KEY_NAME = "name"
    }
}

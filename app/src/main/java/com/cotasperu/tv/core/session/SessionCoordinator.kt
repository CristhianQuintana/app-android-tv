package com.cotasperu.tv.core.session

import com.cotasperu.tv.core.network.CotasApi
import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.AuthTokens
import com.cotasperu.tv.domain.LogoutOutcome
import com.cotasperu.tv.domain.RestoreOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class SessionCoordinator(
    private val api: CotasApi,
    private val store: SessionStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun register(
        email: String,
        password: String,
    ): ApiResult<AuthTokens> = withContext(Dispatchers.IO) {
        mutex.withLock {
            when (val result = api.register(email.trim(), password)) {
                is ApiResult.Ok -> {
                    store.write(result.value)
                    result
                }
                is ApiResult.Err -> result
            }
        }
    }

    suspend fun login(
        username: String,
        password: String,
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<AuthTokens> = withContext(Dispatchers.IO) {
        mutex.withLock {
            when (
                val result = api.login(
                    username = username.trim(),
                    password = password,
                    deviceId = deviceId,
                    platform = platform,
                    deviceName = deviceName,
                )
            ) {
                is ApiResult.Ok -> {
                    store.write(result.value)
                    result
                }
                is ApiResult.Err -> result
            }
        }
    }

    /** Guarda tokens obtenidos por device-link (poll approved). */
    suspend fun acceptTokens(tokens: AuthTokens): ApiResult<AuthTokens> = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.write(tokens)
            ApiResult.Ok(tokens)
        }
    }

    suspend fun createDeviceLinkSession(
        deviceId: String,
        platform: String,
        deviceName: String,
    ): ApiResult<com.cotasperu.tv.domain.DeviceLinkCreated> = withContext(Dispatchers.IO) {
        api.createDeviceLinkSession(deviceId, platform, deviceName)
    }

    suspend fun pollDeviceLinkSession(
        linkCode: String,
        deviceId: String,
    ): ApiResult<com.cotasperu.tv.domain.DeviceLinkPoll> = withContext(Dispatchers.IO) {
        api.pollDeviceLinkSession(linkCode, deviceId)
    }

    suspend fun apiPollQrSession(
        sessionId: String,
        pollSecret: String,
    ): ApiResult<com.cotasperu.tv.domain.DeviceLinkPoll> = withContext(Dispatchers.IO) {
        api.pollQrSession(sessionId, pollSecret)
    }

    suspend fun logout(): LogoutOutcome = withContext(Dispatchers.IO) {
        // Limpia local al instante; revocación remota con tope corto (no bloquear la UI).
        val token = mutex.withLock {
            val access = store.read()?.accessToken
            store.clear()
            access
        }
        if (token.isNullOrBlank()) return@withContext LogoutOutcome.ClearedLocally
        return@withContext try {
            withTimeout(2_500) {
                when (api.logout(token)) {
                    is ApiResult.Ok -> LogoutOutcome.Revoked
                    is ApiResult.Err -> LogoutOutcome.ClearedLocally
                }
            }
        } catch (_: TimeoutCancellationException) {
            LogoutOutcome.ClearedLocally
        }
    }

    suspend fun restore(): RestoreOutcome = withContext(Dispatchers.IO) {
        mutex.withLock {
            when (val ready = usableSession()) {
                is SessionStep.Ready -> when (val profile = api.profile(ready.tokens.accessToken)) {
                    is ApiResult.Ok -> RestoreOutcome.Ready(profile.value)
                    is ApiResult.Err -> if (profile.error == AppError.Unauthorized) {
                        recoverProfile(ready.tokens)
                    } else {
                        RestoreOutcome.Unverified(profile.error)
                    }
                }
                is SessionStep.Missing -> RestoreOutcome.NeedsLogin
                is SessionStep.Failed -> RestoreOutcome.Unverified(ready.error)
            }
        }
    }

    /**
     * Toma el token bajo mutex y libera el candado durante el HTTP,
     * para que catálogo / categorías puedan ir en paralelo (menos timeouts).
     */
    suspend fun <T> authorized(block: (String) -> ApiResult<T>): ApiResult<T> = withContext(Dispatchers.IO) {
        val access = mutex.withLock {
            when (val ready = usableSession()) {
                is SessionStep.Missing -> return@withContext ApiResult.Err(AppError.SessionExpired)
                is SessionStep.Failed -> return@withContext ApiResult.Err(ready.error)
                is SessionStep.Ready -> ready.tokens.accessToken
            }
        }
        when (val first = block(access)) {
            is ApiResult.Ok -> first
            is ApiResult.Err -> if (first.error != AppError.Unauthorized) {
                first
            } else {
                val refreshedAccess = mutex.withLock {
                    val current = store.read() ?: return@withLock null
                    when (val step = refresh(current)) {
                        is SessionStep.Ready -> step.tokens.accessToken
                        is SessionStep.Missing, is SessionStep.Failed -> null
                    }
                } ?: run {
                    mutex.withLock { store.clear() }
                    return@withContext ApiResult.Err(AppError.SessionExpired)
                }
                when (val second = block(refreshedAccess)) {
                    is ApiResult.Ok -> second
                    is ApiResult.Err -> if (second.error == AppError.Unauthorized) {
                        mutex.withLock { store.clear() }
                        ApiResult.Err(AppError.SessionExpired)
                    } else {
                        second
                    }
                }
            }
        }
    }

    private fun recoverProfile(tokens: AuthTokens): RestoreOutcome {
        return when (val refreshed = refresh(tokens)) {
            is SessionStep.Ready -> when (val again = api.profile(refreshed.tokens.accessToken)) {
                is ApiResult.Ok -> RestoreOutcome.Ready(again.value)
                is ApiResult.Err -> if (again.error == AppError.Unauthorized) {
                    store.clear()
                    RestoreOutcome.NeedsLogin
                } else {
                    RestoreOutcome.Unverified(again.error)
                }
            }
            is SessionStep.Missing -> RestoreOutcome.NeedsLogin
            is SessionStep.Failed -> RestoreOutcome.Unverified(refreshed.error)
        }
    }

    private fun usableSession(): SessionStep {
        val current = store.read() ?: return SessionStep.Missing
        val expiresAt = current.accessExpiresAtEpochMs
        if (expiresAt != null && now() >= expiresAt) return refresh(current)
        return SessionStep.Ready(current)
    }

    private fun refresh(current: AuthTokens): SessionStep {
        val refreshToken = current.refreshToken
        if (refreshToken == null) {
            store.clear()
            return SessionStep.Missing
        }
        return when (val refreshed = api.refresh(refreshToken)) {
            is ApiResult.Ok -> {
                val merged = refreshed.value.copy(displayName = refreshed.value.displayName ?: current.displayName)
                store.write(merged)
                SessionStep.Ready(merged)
            }
            is ApiResult.Err -> if (isTransient(refreshed.error)) {
                SessionStep.Failed(refreshed.error)
            } else {
                store.clear()
                SessionStep.Missing
            }
        }
    }

    private fun isTransient(error: AppError): Boolean {
        return error is AppError.Network || error is AppError.Timeout || error is AppError.Unavailable || error is AppError.RateLimited
    }

    private sealed interface SessionStep {
        data class Ready(val tokens: AuthTokens) : SessionStep
        data object Missing : SessionStep
        data class Failed(val error: AppError) : SessionStep
    }
}

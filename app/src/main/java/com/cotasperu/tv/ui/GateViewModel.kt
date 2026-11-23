package com.cotasperu.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cotasperu.tv.core.security.CredentialPolicy
import com.cotasperu.tv.core.security.LoginAttemptGuard
import com.cotasperu.tv.core.prefs.AppAccount
import com.cotasperu.tv.di.AppGraph
import com.cotasperu.tv.domain.ApiResult
import com.cotasperu.tv.domain.AppError
import com.cotasperu.tv.domain.DeviceLinkStatus
import com.cotasperu.tv.domain.HealthSnapshot
import com.cotasperu.tv.domain.RestoreOutcome
import com.cotasperu.tv.domain.UserMessages
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface Gate {
    data object Checking : Gate
    data class Unavailable(val message: String) : Gate
    data class Login(val message: String?) : Gate
    data object Selector : Gate
    data class Unverified(val message: String) : Gate
    data class Ready(val displayName: String) : Gate
}

class GateViewModel(
    private val graph: AppGraph,
) : ViewModel() {
    var gate by mutableStateOf<Gate>(Gate.Checking)
        private set
    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var submitting by mutableStateOf(false)
        private set
    var keepSession by mutableStateOf(graph.prefs.keepSession)
        private set
    var registrationName by mutableStateOf("")
        private set
    var registrationMode by mutableStateOf(false)
        private set
    var loginLockSeconds by mutableStateOf(0)
        private set

    /** Columna QR */
    var qrLinkCode by mutableStateOf<String?>(null)
        private set
    var qrPayload by mutableStateOf<String?>(null)
        private set
    var qrStatusText by mutableStateOf<String?>(null)
        private set
    var qrExpiresInSec by mutableStateOf(0)
        private set
    var qrLoading by mutableStateOf(false)
        private set

    private val attemptGuard = LoginAttemptGuard()
    private var coolTicker: Job? = null
    private var bootJob: Job? = null
    private var deviceLinkJob: Job? = null
    private var countdownJob: Job? = null

    private val deviceId get() = graph.prefs.deviceId()
    private val deviceName get() = graph.prefs.deviceName
    private val platform = "androidtv"
    private var activeQrSessionId: String? = null
    private var activeQrPollSecret: String? = null

    init {
        start()
    }

    fun start() {
        if (submitting) return
        bootJob?.cancel()
        stopDeviceLink()
        bootJob = viewModelScope.launch {
            gate = Gate.Checking
            when (val health = graph.health.check()) {
                is ApiResult.Err -> gate = Gate.Unavailable(UserMessages.forError(health.error))
                is ApiResult.Ok -> openAfterHealth(health.value)
            }
        }
    }

    fun ensureLoginVisible() {
        if (gate is Gate.Checking) {
            gate = Gate.Login(null)
            startDeviceLink()
        }
    }

    fun onUsername(value: String) {
        username = value.take(100)
    }

    fun onPassword(value: String) {
        password = value.take(CredentialPolicy.MAX_PASSWORD)
    }

    fun updateKeepSession(value: Boolean) {
        keepSession = value
        graph.prefs.keepSession = value
    }

    fun onRegistrationName(value: String) {
        registrationName = value.take(60)
    }

    fun toggleRegistrationMode(enabled: Boolean) {
        registrationMode = enabled
        password = ""
        gate = Gate.Login(null)
    }

    fun register() {
        if (submitting) return
        val email = username.trim()
        val emailValid = email.length <= 254 &&
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
        if (!emailValid) {
            gate = Gate.Login("Ingresa un correo válido.")
            return
        }
        if (password.length < 12 || password.length > 128) {
            gate = Gate.Login("La contraseña debe tener 12 caracteres o más.")
            return
        }
        submitting = true
        gate = Gate.Login("Creando tu cuenta…")
        viewModelScope.launch {
            val result = graph.session.register(email, password)
            submitting = false
            when (result) {
                is ApiResult.Ok -> {
                    password = ""
                    graph.appAccount.write(
                        AppAccount(
                            email = email,
                            name = registrationName.trim().ifBlank { result.value.displayName ?: "Cuenta COTAS" },
                            accessToken = result.value.accessToken,
                            refreshToken = result.value.refreshToken,
                            since = System.currentTimeMillis(),
                        )
                    )
                    stopDeviceLink()
                    gate = Gate.Selector
                }
                is ApiResult.Err -> gate = Gate.Login(
                    when (result.error) {
                        is AppError.RateLimited -> "Demasiados intentos. Espera un momento antes de volver a intentar."
                        AppError.Conflict -> "Ese correo ya está registrado. Inicia sesión."
                        AppError.Validation -> "Ingresa un correo válido."
                        AppError.NotFound -> "El registro no está disponible por ahora."
                        else -> "No se pudo completar la operación. Verifica tu conexión e inténtalo de nuevo."
                    }
                )
            }
        }
    }

    fun login() {
        if (submitting) return
        attemptGuard.blockMessageOrNull()?.let { cooling ->
            refreshCoolUi(cooling)
            return
        }
        val issue = CredentialPolicy.validate(username, password)
        val localMessage = UserMessages.forCredential(issue)
        if (localMessage != null) {
            gate = Gate.Login(localMessage)
            return
        }
        graph.prefs.keepSession = keepSession
        val currentPassword = password
        submitting = true
        gate = Gate.Login(null)
        viewModelScope.launch {
            val result = try {
                graph.session.login(
                    username = username.trim(),
                    password = currentPassword,
                    deviceId = deviceId,
                    platform = platform,
                    deviceName = deviceName,
                )
            } catch (_: Exception) {
                ApiResult.Err(AppError.Network)
            }
            submitting = false
            gate = when (result) {
                is ApiResult.Ok -> {
                    attemptGuard.registerSuccess()
                    stopCoolTicker()
                    loginLockSeconds = 0
                    password = ""
                    stopDeviceLink()
                    graph.appAccount.write(
                        AppAccount(
                            email = username.trim(),
                            name = result.value.displayName ?: "Cuenta COTAS",
                            accessToken = result.value.accessToken,
                            refreshToken = result.value.refreshToken,
                            since = System.currentTimeMillis(),
                        )
                    )
                    Gate.Selector
                }
                is ApiResult.Err -> {
                    val msg = when (result.error) {
                        AppError.InvalidCredentials -> {
                            password = ""
                            attemptGuard.registerCredentialFailure()
                                ?: UserMessages.forError(result.error)
                        }
                        AppError.DeviceLimit -> {
                            password = ""
                            UserMessages.forError(result.error)
                        }
                        is AppError.RateLimited -> {
                            password = ""
                            UserMessages.forError(result.error)
                        }
                        else -> UserMessages.forError(result.error)
                    }
                    if (attemptGuard.isCoolingDown()) startCoolTicker()
                    Gate.Login(msg)
                }
            }
        }
    }

    fun regenerateQr() {
        startDeviceLink()
    }

    fun enterCatalog(displayName: String) {
        gate = Gate.Ready(displayName)
    }

    /**
     * Siempre POST /auth/device-link/session fresco.
     * Nunca reutiliza linkCode/qrPayload previos (ni disco ni memoria).
     */
    fun startDeviceLink() {
        deviceLinkJob?.cancel()
        countdownJob?.cancel()
        clearQrMemory(loading = true, status = "Preparando código…")
        deviceLinkJob = viewModelScope.launch {
            // Bucle: cada terminal expired/consumed/denied → nuevo POST (no reusar código).
            while (currentCoroutineContext().isActive && gate is Gate.Login) {
                clearQrMemory(loading = true, status = "Preparando código…")
                when (
                    val created = graph.session.createDeviceLinkSession(
                        deviceId = deviceId,
                        platform = platform,
                        deviceName = deviceName,
                    )
                ) {
                    is ApiResult.Err -> {
                        qrLoading = false
                        qrStatusText = when (created.error) {
                            AppError.NotFound ->
                                "Vinculación QR aún no disponible en el servidor."
                            else -> UserMessages.forError(created.error)
                        }
                        delay(2_500)
                        continue
                    }
                    is ApiResult.Ok -> {
                        val session = created.value
                        qrLoading = false
                        activeQrSessionId = session.sessionId.ifBlank { null }
                        activeQrPollSecret = session.pollSecret.ifBlank { null }
                        // Solo en memoria de UI; no SharedPreferences / disco.
                        qrLinkCode = session.linkCode
                        qrPayload = session.qrPayload
                        val ttl = session.expiresInSec.coerceIn(30, 180)
                        qrExpiresInSec = ttl
                        qrStatusText = "Esperando conexión..."
                        startCountdown(ttl)
                        val outcome = pollUntilTerminal(
                            linkCode = session.linkCode,
                            sessionId = session.sessionId,
                            pollSecret = session.pollSecret,
                            intervalMs = session.pollIntervalMs,
                            expiresInSec = ttl,
                        )
                        countdownJob?.cancel()
                        when (outcome) {
                            PollOutcome.ApprovedDone -> return@launch
                            PollOutcome.LeftLogin -> return@launch
                            PollOutcome.NeedFreshQr -> {
                                clearQrMemory(loading = true, status = qrStatusText)
                                delay(500)
                                // continue → nuevo POST
                            }
                            PollOutcome.Unavailable -> return@launch
                        }
                    }
                }
            }
        }
    }

    private enum class PollOutcome {
        ApprovedDone,
        NeedFreshQr,
        LeftLogin,
        Unavailable,
    }

    private suspend fun pollUntilTerminal(
        linkCode: String,
        sessionId: String,
        pollSecret: String,
        intervalMs: Long,
        expiresInSec: Int,
    ): PollOutcome {
        val wait = intervalMs.coerceIn(1000L, 10_000L)
        val deadlineMs = System.currentTimeMillis() + expiresInSec * 1000L
        while (currentCoroutineContext().isActive && gate is Gate.Login) {
            // TTL local (120 s): no seguir con el mismo código.
            if (System.currentTimeMillis() >= deadlineMs || qrExpiresInSec <= 0) {
                qrStatusText = "Código vencido. Generando uno nuevo…"
                return PollOutcome.NeedFreshQr
            }
            delay(wait)
            if (gate !is Gate.Login) return PollOutcome.LeftLogin
            val poll = if (sessionId.isNotBlank() && pollSecret.isNotBlank()) {
                graph.session.apiPollQrSession(sessionId, pollSecret)
            } else {
                graph.session.pollDeviceLinkSession(linkCode, deviceId)
            }
            when (poll) {
                is ApiResult.Err -> when (poll.error) {
                    AppError.DeviceLinkExpired, AppError.DeviceLinkInvalid -> {
                        qrStatusText = UserMessages.forError(poll.error)
                        return PollOutcome.NeedFreshQr
                    }
                    is AppError.RateLimited -> Unit
                    AppError.NotFound -> {
                        qrStatusText = "Vinculación QR aún no disponible en el servidor."
                        return PollOutcome.Unavailable
                    }
                    else -> qrStatusText = UserMessages.forError(poll.error)
                }
                is ApiResult.Ok -> when (poll.value.status) {
                    DeviceLinkStatus.PENDING -> {
                        qrStatusText = "Esperando conexión..."
                    }
                    DeviceLinkStatus.APPROVED, DeviceLinkStatus.AUTHORIZED -> {
                        val tokens = poll.value.tokens
                        if (tokens == null) {
                            qrStatusText = "Código ya usado. Generando uno nuevo…"
                            return PollOutcome.NeedFreshQr
                        }
                        qrStatusText = "Dispositivo conectado"
                        // Tokens una sola vez; el código queda consumido en backend.
                        return when (val accepted = graph.session.acceptTokens(tokens)) {
                            is ApiResult.Ok -> {
                                stopDeviceLink()
                                graph.appAccount.write(
                                    AppAccount(
                                        email = "",
                                        name = poll.value.deviceName ?: deviceName,
                                        accessToken = accepted.value.accessToken,
                                        refreshToken = accepted.value.refreshToken,
                                        since = System.currentTimeMillis(),
                                    )
                                )
                                gate = Gate.Selector
                                PollOutcome.ApprovedDone
                            }
                            is ApiResult.Err -> {
                                qrStatusText = UserMessages.forError(accepted.error)
                                PollOutcome.NeedFreshQr
                            }
                        }
                    }
                    DeviceLinkStatus.EXPIRED -> {
                        qrStatusText = "Código vencido. Generando uno nuevo…"
                        return PollOutcome.NeedFreshQr
                    }
                    DeviceLinkStatus.DENIED -> {
                        qrStatusText = "Rechazado en el celular. Generando código nuevo…"
                        return PollOutcome.NeedFreshQr
                    }
                    DeviceLinkStatus.REJECTED -> {
                        qrStatusText = "Acceso rechazado desde el celular. Generando un código nuevo…"
                        return PollOutcome.NeedFreshQr
                    }
                    DeviceLinkStatus.CANCELLED -> {
                        qrStatusText = "Actualizando código…"
                        return PollOutcome.NeedFreshQr
                    }
                    DeviceLinkStatus.CONSUMED -> {
                        qrStatusText = "Código ya usado. Generando uno nuevo…"
                        return PollOutcome.NeedFreshQr
                    }
                }
            }
        }
        return PollOutcome.LeftLogin
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var left = seconds.coerceAtLeast(0)
            qrExpiresInSec = left
            while (isActive && left > 0 && gate is Gate.Login) {
                delay(1000)
                left -= 1
                qrExpiresInSec = left
            }
        }
    }

    /** Borra código/QR de memoria. Nunca se escribe en prefs/disco. */
    private fun clearQrMemory(loading: Boolean = false, status: String? = null) {
        qrLinkCode = null
        qrPayload = null
        qrExpiresInSec = 0
        qrLoading = loading
        qrStatusText = status
    }

    private fun stopDeviceLink() {
        deviceLinkJob?.cancel()
        deviceLinkJob = null
        countdownJob?.cancel()
        countdownJob = null
        clearQrMemory()
    }

    fun retrySession() {
        viewModelScope.launch {
            stopDeviceLink()
            gate = Gate.Checking
            if (graph.appAccount.read() != null) {
                gate = Gate.Selector
            } else {
                gate = Gate.Login(null)
                startDeviceLink()
            }
        }
    }

    fun useAnotherAccount() {
        viewModelScope.launch {
            graph.session.logout()
            graph.appAccount.clear()
            password = ""
            gate = Gate.Login(null)
            startDeviceLink()
        }
    }

    fun logout() {
        viewModelScope.launch {
            val outcome = graph.session.logout()
            graph.appAccount.clear()
            password = ""
            keepSession = graph.prefs.keepSession
            gate = Gate.Login(UserMessages.forLogout(outcome))
            startDeviceLink()
        }
    }

    fun onSessionLost() {
        password = ""
        gate = Gate.Login(UserMessages.forError(AppError.SessionExpired))
        startDeviceLink()
    }

    private fun refreshCoolUi(message: String) {
        loginLockSeconds = ((attemptGuard.remainingCoolDownMs() + 999L) / 1000L).toInt().coerceAtLeast(1)
        gate = Gate.Login(message)
        startCoolTicker()
    }

    private fun startCoolTicker() {
        coolTicker?.cancel()
        coolTicker = viewModelScope.launch {
            while (attemptGuard.isCoolingDown()) {
                val left = attemptGuard.remainingCoolDownMs()
                loginLockSeconds = ((left + 999L) / 1000L).toInt().coerceAtLeast(1)
                val login = gate as? Gate.Login
                if (login != null) {
                    gate = Gate.Login(attemptGuard.blockMessageOrNull())
                }
                delay(250)
            }
            loginLockSeconds = 0
            val login = gate as? Gate.Login
            if (login != null && login.message?.contains("Espera", ignoreCase = true) == true) {
                gate = Gate.Login(null)
            }
        }
    }

    private fun stopCoolTicker() {
        coolTicker?.cancel()
        coolTicker = null
    }

    private suspend fun openAfterHealth(health: HealthSnapshot) {
        if (!health.databaseUp) {
            gate = Gate.Unavailable(UserMessages.forError(AppError.DatabaseUnavailable))
            return
        }
        if (graph.appAccount.read() != null) {
            gate = Gate.Selector
        } else {
            gate = Gate.Login(null)
            startDeviceLink()
        }
    }

    override fun onCleared() {
        stopDeviceLink()
        stopCoolTicker()
        super.onCleared()
    }
}

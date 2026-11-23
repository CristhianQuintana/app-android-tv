package com.cotasperu.tv.core.security

/**
 * Defensa local ante fuerza bruta sin bloquear 1 minuto al 4.º intento.
 * Aplica una pausa corta y creciente; el rate-limit duro lo impone el API.
 */
class LoginAttemptGuard(
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private var failures = 0
    private var coolUntilMs = 0L

    fun remainingCoolDownMs(now: Long = clockMs()): Long =
        (coolUntilMs - now).coerceAtLeast(0L)

    fun isCoolingDown(now: Long = clockMs()): Boolean = remainingCoolDownMs(now) > 0L

    fun blockMessageOrNull(now: Long = clockMs()): String? {
        val left = remainingCoolDownMs(now)
        if (left <= 0L) return null
        val seconds = ((left + 999L) / 1000L).toInt().coerceAtLeast(1)
        return "Espera $seconds s antes de volver a intentarlo."
    }

    fun registerSuccess() {
        failures = 0
        coolUntilMs = 0L
    }

    /**
     * Registra fallo de credenciales.
     * @return mensaje de espera si aplica cooldown; null si puede reintentar ya.
     */
    fun registerCredentialFailure(now: Long = clockMs()): String? {
        failures += 1
        val delayMs = coolDownForFailures(failures)
        if (delayMs > 0L) {
            coolUntilMs = now + delayMs
            return blockMessageOrNull(now)
        }
        return null
    }

    fun clear() {
        failures = 0
        coolUntilMs = 0L
    }

    companion object {
        /** Sin pausa en los primeros intentos; luego 3 s / 8 s / 15 s (tope). */
        fun coolDownForFailures(failures: Int): Long = when {
            failures < 4 -> 0L
            failures == 4 -> 3_000L
            failures == 5 -> 8_000L
            else -> 15_000L
        }
    }
}

package com.cotasperu.tv.domain

/**
 * Mensajes orientados al cliente final.
 * Nunca incluir hosts, IPs, puertos, paths ni correlation IDs.
 */
object UserMessages {
    fun forError(error: AppError): String = when (error) {
        AppError.InvalidCredentials ->
            "No se pudo iniciar sesión. Revisa el usuario y la contraseña."
        AppError.Validation ->
            "Revisa los datos ingresados."
        AppError.Conflict ->
            "Ese correo ya está registrado. Inicia sesión."
        is AppError.RateLimited ->
            "Demasiados intentos. Espera un momento y vuelve a intentarlo."
        AppError.Unauthorized, AppError.SessionExpired ->
            "La sesión ya no es válida. Vuelve a iniciar sesión."
        AppError.DeviceLimit ->
            "Superó su límite de sesiones. Cierre una sesión en otro dispositivo e inténtelo de nuevo."
        AppError.DeviceLinkInvalid ->
            "Código QR no válido. Se generará uno nuevo."
        AppError.DeviceLinkExpired ->
            "El código QR venció. Se generará uno nuevo."
        AppError.Forbidden ->
            "No tienes autorización para este contenido."
        AppError.NotFound ->
            "El contenido ya no está disponible."
        AppError.Network, AppError.Timeout ->
            "No se pudo conectar. Revisa tu Internet e inténtalo de nuevo."
        AppError.Unavailable, AppError.DatabaseUnavailable ->
            "El servicio no está disponible en este momento. Inténtalo más tarde."
        AppError.TransportRejected ->
            "No se pudo conectar. Inténtalo de nuevo."
        AppError.PayloadTooLarge, is AppError.UnexpectedResponse ->
            "Algo salió mal. Inténtalo de nuevo en unos momentos."
    }

    fun forCredential(issue: CredentialIssue): String? = when (issue) {
        CredentialIssue.OK -> null
        CredentialIssue.BLANK_USER -> "Ingresa el usuario."
        CredentialIssue.INVALID_USER -> "El usuario no tiene un formato válido."
        CredentialIssue.BLANK_PASSWORD -> "Ingresa la contraseña."
        CredentialIssue.PASSWORD_TOO_LONG -> "La contraseña supera el tamaño permitido."
    }

    fun forLogout(outcome: LogoutOutcome): String = when (outcome) {
        LogoutOutcome.Revoked -> "Sesión cerrada."
        LogoutOutcome.ClearedLocally -> "Sesión cerrada."
    }

    /** Canal caído / stream no disponible / error de reproducción en vivo. */
    fun forLiveChannelDown(): String = "Canal en mantenimiento"

    fun forPlaybackNetwork(): String =
        "No se pudo reproducir. Revisa tu Internet e inténtalo de nuevo."

    fun forPlaybackFormat(): String =
        "El contenido no está disponible en este momento."

    fun forPlaybackUnauthorized(): String =
        "No se pudo iniciar la reproducción. Prueba otra vez."

    fun forPlaybackMissing(): String =
        "Este contenido no está disponible para reproducir."

    fun forPlaybackRejected(): String =
        "No se pudo iniciar la reproducción. Inténtalo de nuevo."

    fun forAuthorizeLive(error: AppError): String = when (error) {
        AppError.Network, AppError.Timeout, AppError.Unavailable,
        AppError.NotFound, AppError.Forbidden, is AppError.UnexpectedResponse,
        AppError.PayloadTooLarge -> forLiveChannelDown()
        else -> forError(error)
    }
}

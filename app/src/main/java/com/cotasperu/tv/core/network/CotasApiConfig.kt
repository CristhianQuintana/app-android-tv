package com.cotasperu.tv.core.network

/**
 * Contrato público COTAS (informe 2026-11-04).
 * Producción: solo HTTPS en api.cotasperu.com — sin hosts LAN ni XUI.
 */
object CotasApiConfig {
    const val PUBLIC_API_BASE_URL = "https://api.cotasperu.com/api/v1"
    const val PUBLIC_HOST = "api.cotasperu.com"
}

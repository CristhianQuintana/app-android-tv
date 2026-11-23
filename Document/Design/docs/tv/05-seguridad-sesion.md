# 05. Seguridad y gestión de sesión

**Estado:** requisitos previos a implementación.

## Autenticación
- Enviar credenciales solo al endpoint aprobado.
- No persistir la contraseña tras el login.
- No registrar credenciales, tokens, encabezados Authorization ni URLs firmadas.
- Mensajes de error sin revelar si una cuenta concreta existe.
- Diferenciar 401 (sesión no válida según contrato) y 403 (operación no autorizada).
- Evitar solicitudes de login duplicadas.

## Sesión
La estrategia exacta depende del contrato real:
- Persistir solo el material de sesión necesario.
- Usar almacenamiento protegido apropiado para Android.
- Definir expiración, renovación, revocación y cierre de sesión.
- Limpiar datos locales al cerrar sesión.
- No asumir refresh token ni endpoint de renovación hasta confirmarlo.

## Transporte y configuración
- Preferir HTTPS con validación de certificados.
- No desactivar TLS para facilitar pruebas.
- Si pruebas usan HTTP en LAN, registrar la limitación; no tratarla como configuración de producción.
- No embebir credenciales de XUI, Astra, VOD o base de datos.

## Catálogo y reproducción
- Obtener catálogo y autorización mediante el backend.
- No confiar en ocultar controles de UI como autorización.
- Validar errores de recurso no autorizado o URL expirada.
- No eludir controles de acceso del servicio de streaming.

## Logs y pruebas negativas
Registrar solo contexto técnico mínimo y saneado. Probar credenciales inválidas, API caída, sesión expirada, recurso no autorizado, respuesta malformada, URL expirada, liberación del reproductor e inspección de APK/logs.

Este documento define requisitos; no confirma que estén implementados.

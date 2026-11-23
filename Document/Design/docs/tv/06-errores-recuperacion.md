# 06. Errores y recuperación

**Estado:** política propuesta.

| Situación | Comportamiento | Recuperación |
|---|---|---|
| Credenciales incorrectas | Mensaje controlado | Corregir datos e intentar de nuevo |
| API caída/timeout | Aviso de conectividad | Reintento manual limitado |
| HTTP 401 | Sesión no válida según contrato | Renovar solo si existe contrato; si no, volver a login |
| HTTP 403 | Operación no autorizada | No repetir automáticamente |
| Catálogo vacío | Estado vacío, no error técnico | Volver o actualizar cuando corresponda |
| Error de catálogo | Mensaje y estado estable | Reintento explícito |
| Recurso no autorizado | No iniciar reproducción | Volver al catálogo |
| URL expirada | Error de reproducción/autorización | Solicitar nueva autorización solo si API lo permite |
| Formato incompatible | Mensaje controlado | Registrar diagnóstico sin URL sensible |
| Salida del reproductor | Detener y liberar recursos | Volver a pantalla estable |
| Pérdida de red | Diferenciar red de formato | Recuperación limitada y compatible |

## Reglas
- Sin reintentos infinitos.
- No repetir operaciones sensibles automáticamente.
- Cancelar solicitudes obsoletas al abandonar pantalla.
- Cada pantalla de red debe modelar carga, éxito, vacío y error.
- No registrar tokens, contraseñas ni URLs firmadas completas.

## Criterios de aceptación
La app no queda cargando indefinidamente; diferencia errores de sesión y red; ofrece recuperación válida; libera el reproductor y asocia errores reproducibles a pruebas.

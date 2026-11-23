# Contratos API requeridos — Google TV / Android TV

**Estado:** inventario inicial; validar con OpenAPI o backend real.

## Evidencia previa
| Operación | Ruta | Evidencia |
|---|---|---|
| Liveness | `GET /api/v1/health/live` | HTTP 200 en pruebas |
| Readiness | `GET /api/v1/health/ready` | HTTP 200; base de datos `up` |
| Login | `POST /api/v1/auth/login` | Existe, pero prueba previa devolvió `401 INVALID_CREDENTIALS`; no se considera validado |
| Categorías live | `GET /api/v1/catalog/live/categories` | Sin token devuelve HTTP 401, endpoint protegido |

Base observada en pruebas: `http://10.10.0.31:3100`. Es una dirección de pruebas y no debe codificarse como URL de producción.

## Contratos que faltan por documentar
### Autenticación
Método/ruta, cuerpo, respuesta, transporte de token, duración, renovación/revocación, cierre de sesión y formato de errores.

### Catálogo
Endpoints/modelos de canales live, películas, series, temporadas/episodios; paginación, filtros, campos opcionales e imágenes.

### Reproducción
Endpoint de autorización/preparación, identificador de contenido, URL y metadatos de respuesta, formatos/protocolos, expiración, errores y DRM si aplica.

## Dependencia bloqueante conocida
El login mediante la API COTAS devolvió `401 INVALID_CREDENTIALS`, aunque una solicitud directa autenticada a XUI respondió correctamente. Se observó discrepancia entre la configuración XUI corregida en el archivo de entorno y las variables efectivas del contenedor. Debe corregirse y repetirse la prueba antes de aceptar integración real.

## Entregable antes de integrar
Obtener contrato versionado; probar login válido/inválido, catálogo con token y autorización de reproducción; guardar evidencia saneada; acordar modelos Kotlin y mapeo de errores.

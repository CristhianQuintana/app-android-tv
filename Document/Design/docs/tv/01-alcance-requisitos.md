# 01. Alcance y requisitos

**Estado:** borrador previo al desarrollo.

## Objetivo
Definir un cliente nativo para Google TV / Android TV que autentique al usuario, consulte el catálogo autorizado y reproduzca contenido usando el backend COTAS como punto central de autenticación, reglas de negocio y autorización.

## MVP incluido
1. Inicio y cierre de sesión.
2. Gestión de sesión.
3. Consulta del catálogo autorizado.
4. Navegación con control remoto.
5. Selección de contenido.
6. Solicitud de autorización/preparación de reproducción al backend.
7. Reproducción con AndroidX Media3 / ExoPlayer, cuando protocolo y formato estén confirmados.
8. Estados de carga, vacío, error, desconexión y sesión expirada.
9. Pruebas y build de prueba documentado.

## Fuera del alcance inicial
- Cliente Samsung Tizen y panel administrativo.
- Perfiles múltiples, favoritos, recomendaciones, descargas o control parental sin aprobación.
- Acceso directo a PostgreSQL, XUI, Astra o VOD.
- Secretos de infraestructura en el APK.
- Publicación final en Google Play.
- Compatibilidad universal sin matriz de dispositivos probados.

## Requisitos funcionales
| ID | Requisito | Criterio |
|---|---|---|
| FR-01 | Login | Cuenta de prueba válida autentica contra API real. |
| FR-02 | Credenciales inválidas | Error controlado; no se crea sesión. |
| FR-03 | Cierre de sesión | Se elimina la sesión local. |
| FR-04 | Catálogo | Los datos proceden de la API, no de datos ficticios en producción. |
| FR-05 | Navegación TV | Flujo principal operable con D-pad, OK y Atrás. |
| FR-06 | Reproducción | Se autoriza en backend y reproduce un recurso probado. |
| FR-07 | Estados | Carga, vacío, error, desconexión y sesión expirada controlados. |
| FR-08 | Configuración | Build de prueba usa el entorno correcto sin secretos embebidos. |
| FR-09 | QA | Pruebas críticas con resultado y evidencia. |

## Requisitos no funcionales
- UI legible a distancia y foco visible.
- Operaciones de red fuera del hilo principal.
- Reintentos limitados y recuperación controlada.
- HTTPS donde corresponda; nunca registrar credenciales o tokens.
- Separación entre UI, estado de pantalla y datos.
- Declarar soporte solo para dispositivos y versiones probados.

## Decisiones pendientes
Modelos/versiones objetivo; `minSdk`, `compileSdk`, `targetSdk`; versiones Kotlin/Gradle; contratos de sesión y catálogo; endpoint de autorización de reproducción; protocolos/códecs/DRM; política de almacenamiento de sesión; diseño visual; dispositivos físicos disponibles y distribución interna.

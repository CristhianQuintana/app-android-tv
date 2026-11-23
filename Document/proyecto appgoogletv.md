Contexto del proyecto — Aplicación Google TV / Android TV
Empresa: COTAS PERU S.A.C.  
Producto: Aplicación de streaming para Google TV / Android TV  
Documento: Contexto funcional y técnico específico del cliente Android TV  
Estado: Documento base para validar antes de iniciar la implementación
---
1. Propósito de la aplicación
La aplicación Google TV / Android TV será el cliente de streaming prioritario de COTAS PERU S.A.C. Su finalidad es permitir que un cliente acceda desde un televisor compatible, se autentique con la modalidad de cuenta definida por la empresa, consulte el contenido autorizado y reproduzca canales de televisión en vivo, películas y series.
La aplicación es un cliente de consumo. No es un panel administrativo, una herramienta para configurar infraestructura ni un reproductor que deba conectarse directamente a los servidores internos.
2. Usuarios y contexto de uso
Usuario principal: cliente de COTAS autorizado para utilizar el servicio de streaming.
Dispositivo: televisor o dispositivo externo compatible con Google TV / Android TV.
Interacción: control remoto, navegación direccional, botón de selección y botón Atrás.
Entorno de uso: sala u otro espacio donde el usuario observa la pantalla a distancia.
Cuenta de acceso: la modalidad exacta de identificación y credenciales debe coincidir con el contrato de autenticación aprobado por el backend. No se deben asumir campos o reglas que la API no haya confirmado.
La cifra de referencia mencionada para la primera versión es de aproximadamente 150 clientes. Este número representa una orientación de clientes del servicio y no equivale a 150 reproducciones simultáneas. La concurrencia real debe confirmarse con la empresa y evaluarse según la infraestructura disponible.
3. Servicios existentes y responsabilidad de la app
La empresa dispone de servicios existentes relacionados con la distribución de contenido, incluidos Astra, XUI/Xprom y VOD. La aplicación Android TV debe reutilizarlos a través del backend COTAS, siempre que las interfaces y capacidades necesarias hayan sido verificadas.
La aplicación no debe:
Conectarse directamente a PostgreSQL.
Invocar directamente XUI/Xprom, Astra o VOD.
Incluir credenciales administrativas ni secretos de infraestructura.
Construir o exponer accesos internos que evadan la autorización del backend.
Suponer que todos los protocolos o formatos de reproducción funcionan en todos los dispositivos.
El backend centraliza autenticación, reglas de negocio, acceso al catálogo y autorización/preparación de reproducción. El reproductor de la app recibe únicamente los datos necesarios que el backend autorice.
4. Tipos de contenido previstos
El catálogo del servicio contempla, según las pruebas directas realizadas contra XUI, estas familias de contenido:
Familia	Datos observados en la prueba directa de XUI	Implicación para la app
Categorías de TV en vivo	12	Navegación por categorías y canales, sujeto al contrato del backend.
Canales/streams en vivo	150	Listado y selección de canales, sujeto a autorización y modelo API.
Categorías VOD	20	Navegación por categorías de películas.
Streams VOD	592	Catálogo de películas, sujeto al contrato de la API COTAS.
Categorías de series	18	Navegación por categorías de series.
Series	96	Acceso a series; temporadas y episodios deben verificarse en el contrato API.
Estas cantidades son resultados de una prueba directa de XUI y pueden cambiar. No son una garantía de cantidades disponibles en la app: el cliente solo debe mostrar datos devueltos y autorizados por la API COTAS.
5. Flujo funcional principal
El usuario abre la aplicación en el televisor.
La app determina si existe una sesión local válida de acuerdo con la política acordada.
Si no hay sesión, presenta la pantalla de acceso.
La app envía la solicitud de autenticación al backend COTAS.
Si la autenticación es válida, conserva únicamente el material de sesión necesario y permitido.
La app consulta el catálogo mediante la API.
El usuario navega por categorías y selecciona un canal, película o contenido de serie disponible.
La app solicita al backend la autorización o preparación de reproducción.
Si el backend autoriza el recurso, la app inicia la reproducción mediante AndroidX Media3 / ExoPlayer, una vez confirmado el protocolo y formato.
La app controla errores, expiración de sesión, retorno al catálogo y liberación de recursos al salir del reproductor.
El flujo completo no se considera integrado hasta que login, catálogo y reproducción se hayan probado contra los servicios reales.
6. Arquitectura de referencia de la aplicación
Plataforma: Android TV / Google TV.
Lenguaje: Kotlin.
IDE: Android Studio.
UI de referencia: Jetpack Compose for TV, pendiente de validar contra el repositorio y los dispositivos objetivo.
Reproducción: AndroidX Media3 / ExoPlayer, pendiente de confirmar compatibilidad con los protocolos, formatos y DRM que realmente se utilicen.
Red: Retrofit + OkHttp como referencia, sujetos a la estructura y dependencias existentes.
Asincronía: Coroutines y Flow como referencia.
Backend: API REST de COTAS implementada con NestJS, según el estado del proyecto.
Persistencia propia: PostgreSQL queda detrás del backend; la app no se conecta a ella.
No se deben fijar versiones de SDK, Kotlin, Gradle o dependencias hasta inspeccionar el proyecto Android existente y acordar la matriz de dispositivos.
7. Backend y estado de integración conocido
El backend de pruebas está publicado en la red interna en `http://10.10.0.31:3100`, con puerto interno 3000. Esta dirección corresponde al entorno de pruebas observado y no debe tratarse como una URL de producción ni codificarse permanentemente en la interfaz.
Evidencia técnica registrada:
`GET /api/v1/health/live` respondió HTTP 200.
`GET /api/v1/health/ready` respondió HTTP 200 e indicó que la base de datos estaba disponible.
`GET /api/v1/catalog/live/categories` sin token respondió HTTP 401, consistente con un endpoint protegido.
La consulta directa autenticada a `player_api.php` de XUI respondió HTTP 200 y reportó la línea de prueba como activa.
La autenticación mediante `POST /api/v1/auth/login` desde la API COTAS respondió `401 INVALID_CREDENTIALS`.
Se observó una discrepancia entre los valores de XUI en `/opt/cotas/env/.env` y los valores que el contenedor backend tenía efectivamente cargados. La fuente de configuración del despliegue debe corregirse y el login debe volver a probarse.
Por tanto, el estado de salud del backend está verificado, pero el login integrado no se debe considerar resuelto. El cliente puede avanzar en documentación, estructura visual y pruebas aisladas; la integración end-to-end depende de resolver este bloqueo.
8. Seguridad y restricciones
Toda autenticación y autorización se valida en el backend.
La comunicación con entornos no confiables debe usar HTTPS con validación de certificados.
No incluir credenciales de XUI/Astra/VOD, claves privadas, tokens ni secretos en el APK o el repositorio.
No registrar contraseñas, tokens, encabezados de autorización ni URLs firmadas completas.
No guardar la contraseña del usuario después del login.
Definir formalmente expiración, renovación y cierre de sesión antes de implementarlos.
Limitar reintentos y evitar llamadas duplicadas.
No modificar el servidor principal como parte de las pruebas del cliente.
No añadir CDN, microservicios, Redis, colas ni otro VPS por defecto; cualquier componente nuevo debe justificarse con una necesidad demostrada.
No prometer compatibilidad con modelos no probados.
9. Alcance inicial del cliente
Incluido
Inicio y cierre de sesión.
Catálogo autorizado.
Navegación con control remoto.
Selección de contenido.
Autorización de reproducción mediante el backend.
Reproducción de contenido compatible.
Estados de carga, vacío, error y sesión expirada.
Pruebas unitarias, de integración y funcionales.
Build de prueba e instrucciones de instalación.
Fuera del alcance salvo aprobación
Panel administrativo.
Aplicación Samsung Tizen.
Perfiles múltiples, favoritos sincronizados, recomendaciones, descargas offline o control parental.
Compras, suscripciones y facturación.
Acceso directo a sistemas internos.
Publicación definitiva en tiendas.
Compatibilidad universal sin una matriz de pruebas.
10. Dependencias que deben cerrarse antes de la integración real
[ ] Repositorio Android actual inspeccionado y cambios preservados.
[ ] Modelos y versiones de Android TV objetivo acordados.
[ ] Contrato de login y sesión documentado.
[ ] Login válido e inválido probados contra el backend COTAS.
[ ] Endpoints y modelos de catálogo documentados.
[ ] Contrato de autorización/preparación de reproducción documentado.
[ ] Protocolos, formatos, códecs y DRM confirmados, si aplica.
[ ] Cuenta y contenido de prueba autorizados.
[ ] Política de sesión y almacenamiento aprobada.
[ ] Dispositivo físico o emulador de referencia disponible.
[ ] Identidad visual y navegación aprobadas.
11. Criterio de éxito del MVP
El MVP se considera funcional cuando un usuario autorizado puede iniciar sesión contra el backend real, consultar contenido real autorizado, navegar usando únicamente el control remoto, solicitar reproducción y reproducir un recurso permitido en un dispositivo y protocolo documentados; además, los errores y la sesión se manejan de forma segura y las pruebas tienen evidencia reproducible.
Una interfaz con datos simulados o un reproductor que no haya probado autorización del backend no cumple este criterio.
12. Decisiones pendientes y responsables
Decisión	Estado	Responsable sugerido
Modelos/versiones de TV objetivo	Por confirmar	Proyecto / QA
Contrato definitivo de login	Bloqueado por incidencia de integración	Backend
Contratos de catálogo	Por documentar/verificar	Backend
Contrato de autorización de reproducción	Por documentar/verificar	Backend / streaming
Protocolos y formatos de reproducción	Por confirmar con pruebas	Infraestructura / streaming
Diseño visual	Por aprobar	Producto / frontend
Configuración Android existente	Por inspeccionar	Desarrollo Android
Política de sesión y secretos	Por aprobar	Backend / seguridad
---
Nota de control documental: este archivo define el contexto de producto y las restricciones para la app Google TV / Android TV. Debe actualizarse cuando las decisiones pendientes se confirmen con evidencia. No declara terminadas funcionalidades que todavía no se han probado.
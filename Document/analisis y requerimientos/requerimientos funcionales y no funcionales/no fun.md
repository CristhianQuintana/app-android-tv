Informe de Requerimientos No Funcionales
Aplicaciones de televisión COTAS PERU S.A.C. — Google TV / Android TV y Samsung Smart TV
Versión: 1.0  
Fecha: 22 de octubre de 2026  
Estado: Documento base para revisión técnica y aprobación  
Ámbito: Calidad, seguridad, rendimiento, compatibilidad, operación y mantenimiento de ambas aplicaciones
---
1. Propósito
Establecer los atributos de calidad y restricciones técnicas que deben cumplir las aplicaciones de televisión de COTAS PERU S.A.C. Los requisitos aplican a Google TV / Android TV y Samsung Smart TV, salvo que se indique una condición específica de plataforma.
Los requisitos no funcionales definen cómo de segura, estable, compatible y mantenible debe ser la solución. Las métricas propuestas son objetivos iniciales para probar y validar; no sustituyen la aprobación de producto ni las mediciones en equipos reales.
2. Principios de arquitectura
Las aplicaciones de TV consumen la API del backend COTAS mediante HTTPS.
Las aplicaciones no se conectan directamente a PostgreSQL.
Las aplicaciones no incorporan credenciales administrativas de XUI ni secretos de infraestructura.
El backend centraliza autenticación, autorización, reglas de dispositivos, sesiones y mediación del catálogo/reproducción.
PostgreSQL permanece en una red privada y solo acepta conexiones de componentes autorizados.
La lógica funcional debe ser equivalente en las dos plataformas; la implementación visual y multimedia puede ser específica.
El cliente no debe confiar en decisiones de autorización tomadas exclusivamente en la interfaz.
3. Clasificación de prioridad
P0: requisito de salida obligatoria del MVP.
P1: requisito importante que debe resolverse antes de producción o según la aceptación de riesgos.
P2: objetivo de evolución o mejora posterior.
4. Requerimientos no funcionales
4.1 Seguridad
ID	Prioridad	Requerimiento	Método de verificación
RNF-SEG-001	P0	Toda comunicación entre la aplicación y el backend en producción debe usar HTTPS con TLS vigente y certificado válido.	Inspección de configuración y pruebas de conexión; rechazar certificados inválidos.
RNF-SEG-002	P0	La aplicación no debe contener credenciales administrativas de XUI, credenciales de PostgreSQL, claves privadas ni secretos de servicio.	Inspección de código, recursos, configuración compilada y paquete distribuible.
RNF-SEG-003	P0	Los tokens de sesión se almacenarán usando mecanismos de almacenamiento seguro disponibles en cada plataforma, con la menor persistencia necesaria.	Revisión de implementación y pruebas sobre el almacenamiento local.
RNF-SEG-004	P0	Las contraseñas no se mostrarán en texto plano ni se registrarán en logs, analítica, reportes de error o mensajes de soporte.	Revisión de código y pruebas con captura de logs.
RNF-SEG-005	P0	La autorización de usuario, dispositivo, catálogo y reproducción se aplicará en el backend.	Pruebas negativas con tokens ausentes, inválidos, vencidos y usuarios sin permisos.
RNF-SEG-006	P0	La aplicación debe tratar los errores de API de forma que no exponga stack traces, rutas internas, secretos o respuestas completas del proveedor.	Pruebas de errores y revisión de mensajes.
RNF-SEG-007	P0	La aplicación debe validar y limitar los datos enviados por el usuario antes de realizar solicitudes.	Pruebas de entradas vacías, longitudes extremas y caracteres no esperados.
RNF-SEG-008	P0	No se debe confiar en identificadores de dispositivo enviados por el cliente como única prueba de identidad o autorización.	Revisión de contrato de API y pruebas de manipulación de identificadores.
RNF-SEG-009	P0	La aplicación debe minimizar los datos personales almacenados localmente y eliminar los datos de sesión al cerrar sesión.	Inspección de almacenamiento antes y después del cierre de sesión.
RNF-SEG-010	P0	La aplicación no debe permitir el acceso a contenido protegido cuando la sesión ha sido revocada o expirada, una vez que el cliente reciba o detecte ese estado.	Pruebas de expiración y revocación.
RNF-SEG-011	P1	Los endpoints de autenticación y reproducción deben contar con protección contra abuso aplicada principalmente en el backend.	Pruebas de límites, monitoreo y revisión de configuración del backend.
RNF-SEG-012	P1	Los logs locales y remotos deben excluir tokens completos, contraseñas, claves de reproducción sensibles y URLs firmadas que otorguen acceso.	Revisión de logs en flujos exitosos y fallidos.
RNF-SEG-013	P1	Las dependencias deben mantenerse actualizadas y someterse a análisis de vulnerabilidades en el ciclo de desarrollo.	Informe de dependencias y revisión de vulnerabilidades conocidas.
RNF-SEG-014	P1	Los paquetes de distribución deben generarse desde un proceso controlado y firmarse según los requisitos de cada plataforma.	Verificación de firma, procedencia y versión del paquete.
4.2 Rendimiento y capacidad de respuesta
ID	Prioridad	Requerimiento	Objetivo inicial / verificación
RNF-REN-001	P0	La interfaz debe responder a la navegación del control remoto sin bloqueos prolongados.	En pruebas de referencia, el percentil 95 de respuesta a navegación local debería ser inferior a 200 ms, excluyendo operaciones de red y animaciones deliberadas.
RNF-REN-002	P0	Las operaciones de red y lectura de catálogo no deben bloquear el hilo principal de interfaz.	Perfilado y revisión de ejecución en dispositivos de referencia.
RNF-REN-003	P0	La aplicación debe mostrar un indicador de carga cuando una operación no finalice de inmediato.	Pruebas de API lenta y carga inicial.
RNF-REN-004	P0	La aplicación debe evitar solicitudes duplicadas causadas por selección repetida, recomposición de interfaz o reintentos no controlados.	Inspección de tráfico y pruebas de interacción rápida.
RNF-REN-005	P0	La aplicación debe liberar recursos del reproductor, imágenes y tareas al abandonar una pantalla o cerrar la aplicación.	Perfilado de memoria y pruebas de navegación repetida.
RNF-REN-006	P1	La primera pantalla de catálogo debe aparecer en un tiempo objetivo de hasta 3 segundos en la red y equipo de referencia, una vez recibida una respuesta normal de API.	Medición repetida; registrar dispositivo, tamaño de respuesta y latencia de red.

RNF-REN-007	P1	La reproducción debe comenzar dentro de un objetivo inicial de 5 segundos para un stream compatible en condiciones normales de red.	Medir tiempo desde selección hasta primer fotograma, con un conjunto definido de streams.
RNF-REN-008	P1	El desplazamiento de listas extensas debe mantener una navegación fluida mediante carga por páginas o estrategia equivalente cuando el catálogo lo requiera.	Pruebas con catálogos representativos y medición de frames perdidos.
RNF-REN-009	P1	Las imágenes de portada y logotipos deben cargarse de forma asíncrona y no impedir la navegación principal.	Perfilado y pruebas con red lenta.
Los objetivos de tiempo son metas iniciales y deben revisarse con dispositivos, tamaños de catálogo y condiciones de red reales. No deben usarse como garantía contractual hasta acordar el entorno de medición.
4.3 Disponibilidad, resiliencia y recuperación
ID	Prioridad	Requerimiento	Método de verificación
RNF-DIS-001	P0	La aplicación debe gestionar una desconexión de red sin cerrarse inesperadamente.	Desactivar y restaurar la red durante login, catálogo y reproducción.
RNF-DIS-002	P0	Los errores transitorios deben mostrarse de manera controlada y permitir reintentar cuando sea seguro.	Simular timeout, HTTP 5xx y pérdida de conexión.
RNF-DIS-003	P0	Los reintentos deben tener límites, espera progresiva cuando corresponda y evitar ciclos infinitos.	Pruebas de indisponibilidad prolongada del backend.
RNF-DIS-004	P0	Si la autenticación no puede verificarse, la aplicación no debe conceder acceso protegido de forma optimista.	Pruebas con backend no disponible y sesión inválida.
RNF-DIS-005	P0	La aplicación debe poder volver a una pantalla estable tras un error de reproducción.	Simular stream no disponible y fallo del reproductor.
RNF-DIS-006	P1	La aplicación debe recuperar el catálogo al volver la conectividad, sin requerir reinstalación.	Pruebas de pérdida y recuperación de red.
RNF-DIS-007	P1	La aplicación debe conservar un estado de navegación coherente después de un error, siempre que la sesión siga válida.	Pruebas de recuperación en pantallas de catálogo y detalles.
4.4 Compatibilidad y portabilidad
ID	Prioridad	Requerimiento	Método de verificación
RNF-COM-001	P0	Google TV / Android TV y Samsung Smart TV deben implementar el mismo alcance funcional aprobado.	Ejecutar una matriz de aceptación común en ambas plataformas.
RNF-COM-002	P0	Cada aplicación debe respetar las APIs, políticas de firma, restricciones de ejecución y capacidades multimedia de su plataforma.	Pruebas en hardware real y revisión de los requisitos oficiales de la plataforma.
RNF-COM-003	P0	La aplicación debe funcionar sin pantalla táctil y sin depender de mouse o teclado físico para el flujo principal.	Pruebas exclusivamente con control remoto.
RNF-COM-004	P0	La interfaz debe soportar la resolución mínima y los modelos definidos en la matriz oficial de dispositivos.	Pruebas en el conjunto de equipos aprobados.
RNF-COM-005	P0	La lista de modelos, versiones de sistema y códecs admitidos debe documentarse antes de producción.	Matriz de compatibilidad aprobada por producto y desarrollo.
RNF-COM-006	P1	La aplicación debe adaptarse a diferentes relaciones de aspecto y zonas seguras sin ocultar acciones esenciales.	Pruebas de resolución y configuración de pantalla.
RNF-COM-007	P1	La implementación de reproducción debe poder sustituirse o ajustarse por plataforma sin alterar el contrato funcional de backend.	Revisión de arquitectura y pruebas de integración.
Nota: la compatibilidad de Samsung debe confirmarse en los modelos reales objetivo. El método de instalación, certificación y disponibilidad de APIs multimedia depende de la plataforma y del modelo; no se presume que una instalación por USB esté garantizada.
4.5 Usabilidad y accesibilidad en TV
ID	Prioridad	Requerimiento	Método de verificación
RNF-USA-001	P0	La interfaz debe priorizar lectura a distancia, tipografía legible, contraste y controles grandes.	Evaluación visual desde distancia habitual de uso.
RNF-USA-002	P0	Todos los controles esenciales deben ser alcanzables mediante navegación direccional.	Recorrido completo con control remoto.
RNF-USA-003	P0	El foco actual debe ser visible en todos los componentes interactivos.	Inspección de todas las pantallas y estados.
RNF-USA-004	P0	El orden de foco debe ser predecible y consistente con la disposición visual.	Pruebas de navegación arriba/abajo/izquierda/derecha.
RNF-USA-005	P0	Los mensajes de error deben indicar el problema de manera clara y, cuando proceda, el siguiente paso.	Revisión de textos y pruebas con usuarios.
RNF-USA-006	P1	Las animaciones no deben retrasar acciones esenciales ni impedir el uso del control remoto.	Pruebas de interacción durante animaciones.
RNF-USA-007	P1	Los estados vacíos deben distinguirse de los errores de conexión o autorización.	Pruebas con catálogo vacío y API no disponible.
4.6 Mantenibilidad y calidad de software
ID	Prioridad	Requerimiento	Método de verificación
RNF-MAN-001	P0	El código debe separarse por responsabilidades, evitando acoplar UI, acceso a red, sesión y reproducción.	Revisión de arquitectura y dependencias.
RNF-MAN-002	P0	Las URL base, límites de tiempo y otros parámetros no secretos deben configurarse por entorno sin modificar la lógica de negocio.	Pruebas de build/configuración de desarrollo y producción.
RNF-MAN-003	P0	Los secretos no deben incluirse en repositorios ni archivos de configuración distribuidos al cliente.	Escaneo del repositorio y artefactos.
RNF-MAN-004	P0	Los contratos de API usados por ambas apps deben documentarse y versionarse.	Revisión de OpenAPI o documentación equivalente y pruebas de contrato.
RNF-MAN-005	P0	Los cambios de API deben comprobarse contra ambas aplicaciones para evitar regresiones.	CI con pruebas de contrato/integración cuando esté disponible.
RNF-MAN-006	P1	La lógica compartida debe tener pruebas automatizadas donde la plataforma lo permita.	Reporte de pruebas unitarias y de integración.
RNF-MAN-007	P1	El proyecto debe incluir instrucciones de compilación, configuración local, firma y despliegue.	Revisión de documentación por otro desarrollador.
RNF-MAN-008	P1	Las dependencias y versiones del SDK deben estar fijadas y registradas para builds reproducibles.	Compilación limpia desde un entorno documentado.
4.7 Observabilidad, auditoría y soporte
ID	Prioridad	Requerimiento	Método de verificación
RNF-OBS-001	P0	Los errores relevantes deben poder diagnosticarse sin registrar contraseñas ni tokens.	Revisión de eventos y logs.
RNF-OBS-002	P0	La aplicación debe distinguir fallos de autenticación, red, catálogo y reproducción cuando los datos disponibles lo permitan.	Pruebas con fallos controlados.
RNF-OBS-003	P1	Los eventos sensibles de autenticación, sesión y acceso deben registrarse en el backend según la política de auditoría.	Verificación en los registros del backend.
RNF-OBS-004	P1	Los mensajes de soporte pueden incluir una referencia de error generada por el backend, sin datos sensibles.	Pruebas de error y revisión del contenido.
RNF-OBS-005	P1	Deben documentarse los procedimientos para recopilar logs de diagnóstico en cada plataforma.	Revisión del manual de soporte.
4.8 Privacidad y gestión de datos
ID	Prioridad	Requerimiento	Método de verificación
RNF-PRI-001	P0	La aplicación solo debe recopilar los datos necesarios para autenticación, autorización, reproducción y soporte.	Revisión de datos transmitidos y almacenados.
RNF-PRI-002	P0	La aplicación debe evitar guardar contraseñas de línea IPTV de manera persistente en el televisor.	Inspección de almacenamiento y memoria cuando sea posible.
RNF-PRI-003	P0	Los datos de sesión deben eliminarse al cerrar sesión y no reutilizarse después de revocación conocida.	Pruebas de cierre de sesión y revocación.
RNF-PRI-004	P1	La retención de auditoría y datos de dispositivo debe definirse en el backend conforme a la política de COTAS.	Revisión de políticas y configuración de persistencia.
RNF-PRI-005	P1	La aplicación debe proporcionar una referencia a la política de privacidad aprobada, cuando el producto y el canal de distribución lo requieran.	Revisión de interfaz y metadatos de distribución.
4.9 Consumo de recursos y estabilidad
ID	Prioridad	Requerimiento	Método de verificación
RNF-EST-001	P0	La aplicación no debe sufrir cierres inesperados durante los flujos principales.	Pruebas repetidas de login, catálogo y reproducción.
RNF-EST-002	P0	La aplicación debe liberar decodificadores, superficies y recursos multimedia al cambiar de contenido o salir.	Pruebas de cambios repetidos y perfilado.
RNF-EST-003	P0	La aplicación debe evitar fugas de memoria en navegación prolongada.	Pruebas de estrés y herramientas de perfilado disponibles.
RNF-EST-004	P1	La aplicación debe soportar una sesión de uso prolongada de al menos 2 horas en un dispositivo de referencia sin degradación severa.	Prueba de estabilidad con cambios de canal y navegación representativos.
RNF-EST-005	P1	Los errores del proveedor de streaming no deben dejar la aplicación en un estado que requiera reiniciar el televisor.	Pruebas con URLs y streams inválidos o interrumpidos.
4.10 Distribución, configuración y despliegue
ID	Prioridad	Requerimiento	Método de verificación
RNF-DES-001	P0	Los paquetes de desarrollo y producción deben diferenciarse mediante configuración explícita.	Inspección de builds y endpoints configurados.
RNF-DES-002	P0	La versión de cada paquete debe ser identificable.	Verificación de versión y registro de release.
RNF-DES-003	P0	Las aplicaciones de producción no deben apuntar a IP, endpoints o credenciales de prueba.	Inspección de artefactos de producción.
RNF-DES-004	P0	El proceso de firma y distribución debe estar documentado por plataforma.	Revisión de procedimiento de release.
RNF-DES-005	P1	Debe existir una estrategia de actualización y reversión compatible con el método de distribución elegido.	Simulación de actualización y revisión de release.
RNF-DES-006	P1	La instalación y actualización en Samsung deben validarse con los modelos y el canal de distribución aprobados.	Prueba real de instalación, actualización y desinstalación.
5. Seguridad de red e infraestructura
El backend debe ser el único componente que accede a PostgreSQL.
PostgreSQL no debe exponerse directamente a redes de televisores ni a Internet.
El acceso del backend a XUI debe restringirse a la red y credenciales necesarias.
Los secretos deben administrarse en el entorno del servidor, no en el cliente.
Los endpoints de prueba y producción deben estar separados.
Las reglas de firewall deben permitir solo el tráfico necesario.
Los logs del backend deben redactar secretos y datos de autenticación.
La disponibilidad y la seguridad de Astra/XUI son dependencias operativas que deben gestionarse en la infraestructura, no desde la aplicación de TV.
6. Matriz mínima de validación
Área	Prueba mínima	Resultado esperado
Autenticación	Credenciales válidas e inválidas	Acceso correcto o rechazo controlado
Sesión	Token vencido o revocado	Se bloquea contenido protegido
Red	Sin conexión, timeout, HTTP 5xx	Mensaje claro, sin cierre inesperado
Catálogo	Vacío, pequeño y grande	Estados correctos y navegación estable
Reproducción	Stream válido, inválido e interrumpido	Reproduce o muestra error recuperable
Control remoto	Recorrido de todas las pantallas	Foco visible y navegación completa
Seguridad	Inspección del paquete y logs	Sin secretos incrustados ni contraseñas registradas
Memoria	Navegación y cambios de canal repetidos	Sin crecimiento sostenido anormal
Compatibilidad	Dispositivos Android TV y Samsung objetivo	Cumple los flujos aprobados
Distribución	Instalación y actualización	Paquete firmado e instalable por el método aprobado
7. Condiciones técnicas pendientes
Resolver el rechazo `401 INVALID_CREDENTIALS` en la autenticación backend-XUI y ejecutar una prueba completa.
Confirmar URL pública, TLS, versión y rutas definitivas de la API.
Confirmar la política de renovación, rotación y revocación de sesiones.
Definir el límite de dispositivos y sesiones por cuenta.
Confirmar los formatos y mecanismos reales de reproducción para live, películas y series.
Aprobar la matriz de televisores, versiones y códecs.
Validar la distribución e instalación de Samsung en equipos reales.
Aprobar los objetivos de rendimiento usando hardware y condiciones de red representativos.
8. Criterios de salida a producción
La versión no debe publicarse hasta que:
Todos los RNF P0 tengan evidencia de prueba.
Los riesgos de los RNF P1 estén resueltos o aceptados formalmente.
La autenticación y reproducción se hayan probado de extremo a extremo.
No existan secretos en el paquete cliente ni conexiones directas a PostgreSQL.
Se hayan ejecutado pruebas con control remoto en hardware real.
Se haya aprobado el proceso de firma, distribución y actualización de ambas plataformas.
Exista documentación de soporte, versión y rollback.
9. Control de cambios
Toda modificación de objetivos, plataformas soportadas, seguridad o rendimiento debe registrar la razón del cambio, el impacto sobre Android TV y Samsung, los casos de prueba afectados y la aprobación correspondiente.
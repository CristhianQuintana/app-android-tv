Informe de Requerimientos Funcionales
Aplicaciones de televisión COTAS PERU S.A.C. — Google TV / Android TV y Samsung Smart TV
Versión: 1.0  
Fecha: 22 de octubre de 2026  
Estado: Documento base para revisión y validación funcional  
Ámbito: Aplicación de cliente IPTV para Google TV / Android TV y Samsung Smart TV
---
1. Propósito
Definir las capacidades funcionales que deben ofrecer las aplicaciones de televisión de COTAS PERU S.A.C. Las dos aplicaciones compartirán el mismo propósito, flujo funcional, catálogo y experiencia de usuario. La implementación, empaquetado y reproducción multimedia serán específicos de cada plataforma.
Este documento describe qué debe hacer el producto, no la tecnología concreta que debe utilizar cada pantalla o módulo.
2. Objetivo del producto
Permitir que un cliente autorizado acceda desde su televisor al servicio IPTV de COTAS, autentique su cuenta, consulte el catálogo disponible y reproduzca contenido autorizado de televisión en vivo, películas y series, según las capacidades que confirme la infraestructura actual.
3. Alcance
3.1 Incluido en la primera versión
Inicio de sesión mediante las credenciales definidas para el servicio.
Validación de las credenciales mediante el backend de COTAS.
Registro o reconocimiento del dispositivo de televisión.
Gestión de sesión de la aplicación y cierre de sesión.
Consulta de categorías y contenidos disponibles.
Navegación por televisión en vivo, películas y series, si cada catálogo está habilitado para la cuenta.
Consulta de detalles del contenido cuando los datos estén disponibles.
Reproducción de contenido autorizado.
Controles básicos del reproductor.
Estados de carga, vacío, error, sesión vencida y pérdida de conectividad.
Navegación mediante control remoto.
Recuperación controlada ante fallos de red o reproducción.
Registro de eventos relevantes en el backend cuando corresponda.
3.2 Fuera del alcance inicial, salvo aprobación posterior
Contratación, pagos, renovación de suscripciones o facturación dentro de la TV app.
Administración de clientes, roles, permisos o líneas IPTV.
Edición del catálogo por parte del usuario final.
Descargas para reproducción sin conexión.
Perfiles infantiles, controles parentales avanzados y clasificación personalizada.
Favoritos, listas de seguimiento, historial sincronizado, recomendaciones o búsqueda avanzada, salvo que se aprueben expresamente.
Grabación de programas, pausa en vivo o retroceso de televisión en vivo, salvo que la fuente y el producto lo soporten.
Transcodificación, distribución de canales o administración de Astra/XUI desde la aplicación.
Uso directo de credenciales administrativas de XUI desde el televisor.
4. Actores y sistemas relacionados
Actor o sistema	Responsabilidad
Cliente	Inicia sesión, navega por el catálogo y reproduce contenido autorizado.
Aplicación Google TV / Android TV	Presenta la interfaz para Android TV y gestiona reproducción mediante componentes compatibles con esa plataforma.
Aplicación Samsung Smart TV	Presenta la misma experiencia funcional mediante una implementación compatible con el sistema operativo y reproductor admitidos por el modelo de TV.
Backend COTAS	Expone la API, autentica, autoriza, gestiona sesiones y dispositivos, y coordina el acceso al catálogo y reproducción.
PostgreSQL	Conserva datos propios de identidad, dispositivos, sesiones, permisos, auditoría y otros datos persistentes definidos por el backend.
XUI.one	Fuente externa de información IPTV y/o resolución de contenido, según la integración habilitada.
Infraestructura Astra / streaming	Entrega el flujo audiovisual a los clientes autorizados a través de los mecanismos aprobados por COTAS.
Regla de arquitectura: las aplicaciones de televisión no se conectarán directamente a PostgreSQL ni utilizarán las credenciales administrativas de XUI. Consumirán la API del backend COTAS.
5. Requerimientos funcionales
Las prioridades son: P0 indispensable para el MVP; P1 importante, sujeto a disponibilidad de datos o confirmación; P2 mejora posterior. Los códigos son identificadores para trazabilidad y pruebas.
5.1 Acceso y autenticación
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-001	P0	La aplicación debe mostrar una pantalla de inicio de sesión clara y operable con control remoto.	El cliente puede identificar los campos, la acción de ingreso y los mensajes de validación.
RF-002	P0	La aplicación debe permitir ingresar el identificador de usuario y la contraseña que defina el servicio.	Los campos aceptan entrada, permiten corregirla y no muestran la contraseña en texto plano.
RF-003	P0	La aplicación debe enviar las credenciales al backend COTAS por el canal seguro configurado.	La solicitud llega al endpoint aprobado; no se conecta directamente a XUI ni a PostgreSQL.
RF-004	P0	El backend debe validar las credenciales y determinar si el usuario puede utilizar el servicio.	Solo se crea una sesión autenticada cuando la validación y las reglas de acceso tienen éxito.
RF-005	P0	La aplicación debe informar de forma comprensible cuando el acceso sea rechazado.	Se muestra un mensaje genérico, sin exponer secretos, detalles internos o respuestas sensibles del proveedor.
RF-006	P0	La aplicación debe distinguir errores de credenciales de errores de conectividad o indisponibilidad del servicio cuando la API lo permita.	El cliente recibe una indicación accionable y no se presenta una falla de red como contraseña incorrecta.
RF-007	P0	La aplicación debe conservar la sesión solo mediante el mecanismo de sesión aprobado por el backend.	Los tokens o credenciales no se guardan en texto plano ni se imprimen en registros.
RF-008	P0	La aplicación debe permitir cerrar sesión.	El backend invalida o revoca la sesión según el contrato de API y la aplicación elimina los datos locales de sesión.
RF-009	P1	La aplicación debe manejar una sesión vencida o revocada.	El contenido protegido deja de estar accesible y se solicita iniciar sesión nuevamente cuando corresponda.
RF-010	P1	La aplicación debe recuperar la sesión al reabrirse, si la política de seguridad lo permite.	La restauración respeta vencimiento, revocación y límites de sesiones definidos por el backend.
Dependencia conocida: la autenticación de prueba contra el backend presentó `401 INVALID_CREDENTIALS`, aunque una cuenta de prueba funcionó directamente contra XUI. La integración de autenticación debe resolverse y validarse antes de aceptar RF-004 como terminado. No debe asumirse que el flujo actual está operativo hasta superar la prueba integral.
5.2 Dispositivo y sesiones
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-011	P0	La aplicación debe identificar el dispositivo de forma compatible con las reglas de registro definidas por el backend.	Se envía un identificador de instalación/dispositivo permitido, sin depender únicamente de un dato fácilmente falsificable.
RF-012	P0	La aplicación debe registrar o actualizar la plataforma del dispositivo.	El backend puede distinguir, como mínimo, Google TV/Android TV de Samsung Tizen si ambos están habilitados en el modelo de datos.
RF-013	P0	El backend debe aplicar las reglas de autorización y límites de dispositivos o sesiones que se aprueben para el servicio.	Una solicitud que excede una regla recibe una respuesta controlada; los límites reales deben ser confirmados por negocio e infraestructura.
RF-014	P1	La aplicación debe mostrar un mensaje útil si el dispositivo no está autorizado o requiere una acción adicional.	El mensaje explica el siguiente paso sin mostrar información interna.
RF-015	P1	La aplicación debe reaccionar ante una revocación de sesión o dispositivo.	Se bloquean las operaciones protegidas y se solicita una nueva autenticación cuando corresponda.
5.3 Catálogo y navegación
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-016	P0	Tras autenticarse, la aplicación debe solicitar al backend el catálogo autorizado para la cuenta.	No se presenta contenido protegido antes de completar la autorización.
RF-017	P0	La aplicación debe presentar las secciones disponibles: televisión en vivo, películas y series, según los servicios habilitados.	Las secciones no habilitadas no se muestran como si contuvieran contenido disponible.
RF-018	P0	La aplicación debe mostrar categorías del catálogo recibidas desde el backend.	Cada categoría visible corresponde a datos entregados por la API.
RF-019	P0	La aplicación debe mostrar los elementos de una categoría en una lista o cuadrícula adecuada para TV.	Se puede recorrer el contenido con el control remoto y distinguir el elemento enfocado.
RF-020	P0	La aplicación debe soportar estados de carga, catálogo vacío y error al consultar el catálogo.	El usuario no queda ante una pantalla indefinida; puede reintentar cuando sea apropiado.
RF-021	P1	La aplicación debe mostrar información descriptiva del contenido cuando la API la proporcione.	Se presenta únicamente información disponible, por ejemplo título, descripción, imagen, año o duración.
RF-022	P1	La aplicación debe permitir actualizar el catálogo mediante una nueva consulta al backend.	La actualización no requiere reinstalar la aplicación y respeta la autorización vigente.
RF-023	P1	La aplicación debe evitar mostrar duplicados causados por paginación o actualización, si la API utiliza esos mecanismos.	Los elementos mantienen una identidad estable en la interfaz.
RF-024	P2	La aplicación podrá ofrecer búsqueda por título.	La búsqueda se implementa solo cuando se acuerden el contrato de API, la experiencia y los criterios de aceptación.
RF-025	P2	La aplicación podrá permitir marcar favoritos o continuar viendo.	Requiere aprobación de alcance, persistencia y sincronización entre dispositivos.
5.4 Televisión en vivo
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-026	P0	La aplicación debe mostrar los canales en vivo autorizados para el cliente.	Solo aparecen canales devueltos por la API para esa cuenta.
RF-027	P0	La aplicación debe permitir seleccionar un canal e iniciar su reproducción.	Al seleccionar un canal válido, se solicita al backend la información o autorización de reproducción y se inicia el flujo aprobado.
RF-028	P0	La aplicación debe mostrar el nombre del canal y, si está disponible, su logotipo.	La información visual coincide con los datos disponibles del catálogo.
RF-029	P0	El reproductor debe permitir pausar o reanudar solo cuando el tipo de flujo y reproductor lo soporten.	La interfaz no promete una función no soportada por el flujo.
RF-030	P0	La aplicación debe permitir salir del reproductor y volver al catálogo.	El usuario puede regresar con el control remoto sin bloquear la aplicación.
RF-031	P1	La aplicación debe permitir cambiar de canal desde la interfaz de reproducción si se aprueba el diseño.	El cambio utiliza un canal autorizado y presenta el nuevo estado de carga/error.
RF-032	P1	La aplicación debe mostrar la guía de programación (EPG) si la fuente y la API la proporcionan.	Si no existe información de EPG, la aplicación no debe inventar horarios ni programas.
RF-033	P1	La aplicación debe manejar errores de reproducción y permitir reintentar de manera controlada.	Se muestra un mensaje comprensible y se evita un bucle de reintentos ilimitado.
5.5 Películas y series
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-034	P0	La aplicación debe mostrar las categorías de películas disponibles para la cuenta.	La lista corresponde al catálogo autorizado y disponible.
RF-035	P0	La aplicación debe permitir abrir una película y solicitar su reproducción.	El backend confirma el acceso y devuelve los datos necesarios para reproducirla.
RF-036	P0	La aplicación debe mostrar las categorías de series disponibles para la cuenta.	La lista corresponde al catálogo autorizado y disponible.
RF-037	P0	La aplicación debe mostrar temporadas y episodios cuando la fuente de datos los exponga.	El usuario puede seleccionar un episodio existente y autorizado.
RF-038	P0	La aplicación debe iniciar la reproducción de un episodio seleccionado.	El flujo se obtiene mediante el mecanismo aprobado por el backend.
RF-039	P1	La aplicación debe mostrar detalles como descripción, año, duración, portada o metadatos disponibles.	Los campos ausentes se omiten de forma limpia.
RF-040	P1	La aplicación debe gestionar errores de contenido no disponible, enlace inválido o acceso denegado.	No se muestran URL privadas, credenciales de línea ni respuestas internas sin filtrar.
RF-041	P2	La aplicación podrá recordar el último episodio o posición de reproducción.	Solo se implementa si se acuerdan persistencia, precisión y sincronización.
Validación técnica pendiente: se debe comprobar el método real de entrega de películas y series, formatos admitidos, estructura de episodios y compatibilidad de reproducción en ambas plataformas. No debe asumirse que todos los formatos de XUI funcionan de la misma manera en Android TV y Samsung.
5.6 Reproductor y experiencia con control remoto
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-042	P0	La aplicación debe operar con las teclas de navegación, selección, atrás y reproducción disponibles en el dispositivo.	Las acciones principales pueden completarse sin mouse ni pantalla táctil.
RF-043	P0	La interfaz debe mostrar claramente el elemento que tiene el foco.	El foco es visible y no se pierde en la navegación normal.
RF-044	P0	El reproductor debe mostrar estados de preparación, reproducción, pausa, finalización y error que correspondan al contenido.	Los estados de interfaz coinciden con el estado real del reproductor.
RF-045	P0	La aplicación debe permitir regresar a la pantalla anterior sin cerrar accidentalmente la sesión.	El botón atrás sigue una jerarquía de navegación consistente.
RF-046	P0	La aplicación debe liberar los recursos de reproducción al salir o cambiar de contenido.	No queda audio de fondo ni una sesión de reproducción local activa sin intención del usuario.
RF-047	P1	La aplicación debe recuperar de forma controlada errores transitorios de conexión o decodificación.	Los reintentos tienen límites y ofrecen salida al catálogo.
RF-048	P1	La aplicación debe evitar que un doble clic o selección repetida genere múltiples reproducciones simultáneas.	Solo se mantiene la reproducción solicitada y autorizada.
5.7 Comunicación, errores y soporte
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-049	P0	La aplicación debe comunicarse con el backend usando las rutas y el contrato de API aprobados.	Las rutas se documentan y prueban; no se inventan endpoints en el cliente.
RF-050	P0	La aplicación debe manejar ausencia de red, timeout, respuestas no válidas y errores HTTP.	Se informa del problema y se conserva la estabilidad de la aplicación.
RF-051	P0	La aplicación debe tratar un `401` o `403` de acuerdo con el significado definido por la API.	Se actualiza la sesión o se informa de falta de autorización según corresponda.
RF-052	P1	La aplicación debe permitir reintentar las consultas que fallen por causas transitorias.	El reintento es explícito o acotado y no repite operaciones sensibles de manera indiscriminada.
RF-053	P1	La aplicación debe facilitar un código de error o referencia no sensible para soporte cuando el backend lo proporcione.	El dato no incluye contraseña, token, URL privada ni información personal innecesaria.
5.8 Compatibilidad entre plataformas
ID	Prioridad	Requerimiento funcional	Criterio de aceptación resumido
RF-054	P0	Ambas aplicaciones deben conservar el mismo flujo funcional y las mismas reglas de negocio.	Los casos de uso aprobados producen resultados equivalentes en ambas plataformas.
RF-055	P0	Cada aplicación debe usar el mecanismo de reproducción compatible con su plataforma.	Android TV y Samsung usan implementaciones nativas adecuadas sin exigir que compartan el mismo código de reproductor.
RF-056	P0	La aplicación debe respetar las limitaciones de hardware, sistema operativo y códecs del dispositivo.	Los dispositivos oficialmente admitidos superan la matriz de compatibilidad acordada.
RF-057	P1	La aplicación debe adaptar la presentación a las resoluciones y áreas seguras admitidas.	No se cortan textos, botones ni controles esenciales en los equipos objetivo.
6. Flujo funcional principal
El cliente abre la aplicación.
La aplicación verifica si existe una sesión local que pueda restaurarse de forma segura.
Si no hay una sesión válida, solicita las credenciales.
La aplicación envía la solicitud al backend COTAS.
El backend valida la identidad, el estado de la cuenta, el dispositivo y las reglas de sesión.
El backend devuelve el resultado y los datos de sesión permitidos.
La aplicación consulta las secciones y el catálogo autorizados.
El cliente navega por categorías y selecciona un canal, película o episodio.
La aplicación solicita al backend los datos o la autorización necesarios para reproducir.
La aplicación inicia el reproductor de la plataforma y presenta los estados correspondientes.
Al salir del contenido, vuelve al catálogo. Al cerrar sesión, invalida la sesión según el contrato del backend y elimina los datos locales.
7. Reglas funcionales y decisiones por confirmar
Credenciales de acceso: confirmar si el cliente iniciará sesión con la cuenta de aplicación COTAS, con las credenciales de la línea IPTV o mediante un mecanismo de vinculación. La integración actual usa provisionalmente credenciales de la línea XUI.
Alta automática: confirmar si un usuario se crea automáticamente después de autenticar correctamente contra XUI o si el usuario debe existir previamente.
Líneas por usuario: la regla de una línea XUI por usuario de aplicación es provisional; debe aprobarse antes de fijar el comportamiento.
Límite de dispositivos y sesiones: negocio debe definir el número permitido y qué ocurre al excederlo.
Playback: infraestructura debe confirmar cómo se entregan y autorizan los streams live, VOD y episodios.
EPG, búsqueda, favoritos e historial: no se consideran garantizados en el MVP.
Samsung: el método de instalación y la lista de modelos compatibles deben validarse por separado.
Rutas API: los nombres de endpoints mencionados en borradores no son definitivos hasta contrastarlos con el backend implementado.
8. Dependencias externas
Backend NestJS operativo con API versionada bajo `/api/v1`.
Base de datos PostgreSQL accesible únicamente desde los servicios autorizados.
Integración de autenticación con XUI verificada de extremo a extremo.
Catálogo de live, películas y series accesible desde la cuenta de prueba.
URLs, protocolos, credenciales de servicio y comportamiento de reproducción confirmados por infraestructura.
Certificado TLS válido para el endpoint usado por las aplicaciones.
Definición de los modelos y versiones de televisores admitidos.
Revisión de derechos y autorización de distribución del contenido.
9. Criterios de aceptación del MVP
El MVP se considerará funcionalmente aceptable cuando:
El cliente pueda iniciar sesión con una cuenta de prueba válida y reciba un rechazo controlado con una cuenta inválida.
El backend identifique el dispositivo y aplique las reglas de acceso acordadas.
La aplicación muestre las categorías y el contenido autorizado.
Se pueda reproducir al menos un canal live, una película y un episodio de serie en cada plataforma compatible, siempre que esos tipos de contenido formen parte del alcance comercial y estén disponibles en el servidor.
La navegación completa funcione mediante control remoto.
La aplicación responda correctamente a pérdida de red, sesión vencida y error de reproducción.
El cierre de sesión elimine la sesión local y la invalide en el backend cuando corresponda.
No haya conexión directa desde el televisor a PostgreSQL ni credenciales administrativas de XUI en la aplicación.
Las pruebas de aceptación se ejecuten en dispositivos reales de las plataformas objetivo.
10. Trazabilidad y mantenimiento
Cada requerimiento debe relacionarse con una o más historias de usuario, casos de prueba y, cuando corresponda, endpoints de API. Los cambios de alcance deberán registrar la versión, el motivo y su impacto en ambas aplicaciones.
Aprobaciones pendientes: responsable de producto/negocio, backend, infraestructura IPTV y responsables de aplicaciones TV.
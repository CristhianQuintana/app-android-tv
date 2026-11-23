Informe de Historias de Usuario
Cliente de las aplicaciones COTAS PERU S.A.C. — Google TV / Android TV y Samsung Smart TV
Versión: 1.0  
Fecha: 22 de octubre de 2026  
Estado: Backlog inicial para validación de producto  
Rol principal: Cliente autorizado del servicio IPTV  
Plataformas: Google TV / Android TV y Samsung Smart TV
---
1. Propósito
Definir las historias de usuario del cliente final para las aplicaciones de televisión de COTAS PERU S.A.C. Las historias describen necesidades y resultados desde la perspectiva del cliente y proporcionan criterios de aceptación verificables.
Las dos aplicaciones compartirán las historias y reglas de negocio. Cada plataforma tendrá su implementación específica, especialmente en navegación del sistema, reproducción, empaquetado, instalación y ciclo de vida.
2. Convenciones
Formato: Como [actor], quiero [capacidad], para [beneficio].
Prioridades:
P0: imprescindible para el MVP.
P1: importante, sujeto a confirmación de alcance o disponibilidad de infraestructura.
P2: mejora posterior.
Los puntos de historia no se estiman en este documento porque requieren refinamiento con el equipo técnico. Los criterios de aceptación se expresan en lenguaje verificable y podrán convertirse en pruebas manuales o automatizadas.
3. Épica ÉP-01: Acceso a la aplicación
HU-CLI-001 — Iniciar sesión
Prioridad: P0  
Como cliente con una cuenta autorizada de COTAS, quiero iniciar sesión en la aplicación del televisor, para acceder al contenido incluido en mi servicio.
Criterios de aceptación
Dado que la aplicación está abierta y no existe una sesión válida, cuando ingreso credenciales válidas y confirmo, entonces la aplicación envía la solicitud al backend COTAS.
Si el backend confirma la autenticación y autorización, la aplicación abre la experiencia principal y solicita el catálogo autorizado.
Si las credenciales son rechazadas, se muestra un mensaje comprensible y puedo corregir los datos.
La contraseña se oculta mientras la ingreso.
La aplicación no conecta directamente con PostgreSQL ni con XUI.
Si el servicio no responde, la aplicación informa de un problema de conexión y no lo presenta incorrectamente como credenciales inválidas.
Dependencias: contrato de autenticación de la API; definición final del tipo de credencial.  
Nota: el uso de credenciales de la línea XUI es provisional y debe confirmarse. La integración de prueba ha presentado `401 INVALID_CREDENTIALS` desde el backend y debe resolverse antes de cerrar esta historia.
HU-CLI-002 — Corregir credenciales incorrectas
Prioridad: P0  
Como cliente que no pudo iniciar sesión, quiero corregir mis credenciales sin reiniciar la aplicación, para volver a intentar el acceso fácilmente.
Criterios de aceptación
El mensaje de rechazo no revela si existe una cuenta en el sistema cuando esa información deba mantenerse privada.
Los campos pueden editarse y volver a enviarse.
La aplicación no conserva la contraseña incorrecta después de un rechazo, salvo que el diseño aprobado indique lo contrario de forma segura.
Los reintentos no generan solicitudes duplicadas involuntarias ni bloquean la interfaz.
HU-CLI-003 — Mantener una sesión segura
Prioridad: P0  
Como cliente autenticado, quiero que la aplicación gestione mi sesión de forma segura, para no tener que ingresar mis credenciales en cada navegación y evitar que otra persona use una sesión inválida.
Criterios de aceptación
La aplicación usa el mecanismo de sesión definido por el backend.
Los datos de sesión se almacenan de forma segura según la plataforma.
Si la sesión vence o se revoca, la aplicación deja de permitir operaciones protegidas y solicita autenticación cuando corresponda.
No se registran tokens ni contraseñas en logs.
Una sesión restaurada al abrir la aplicación debe ser validada conforme a la política del backend.
HU-CLI-004 — Cerrar sesión
Prioridad: P0  
Como cliente, quiero cerrar sesión desde la aplicación, para impedir que otra persona use mi cuenta en el televisor.
Criterios de aceptación
La opción de cerrar sesión es localizable desde la interfaz acordada.
La aplicación solicita al backend invalidar la sesión cuando el contrato lo permita.
Se eliminan los datos de sesión locales.
Después del cierre de sesión no puedo abrir contenido protegido sin autenticarme nuevamente.
Si la solicitud al backend falla, la aplicación elimina localmente la sesión y comunica el estado sin afirmar que el servidor la revocó si no se confirmó.
4. Épica ÉP-02: Registro y autorización del televisor
HU-CLI-005 — Reconocer el televisor
Prioridad: P0  
Como cliente, quiero que el servicio reconozca el televisor desde el que accedo, para aplicar las reglas de seguridad y autorización correspondientes.
Criterios de aceptación
Después de autenticarse, la aplicación registra o presenta al backend la información de dispositivo permitida.
El backend distingue la plataforma de la aplicación.
El identificador de dispositivo no se considera por sí solo una prueba suficiente de identidad.
Si el dispositivo no está autorizado, se muestra una explicación y el siguiente paso definido por COTAS.
Los datos enviados no incluyen información del dispositivo que no sea necesaria para el servicio.
HU-CLI-006 — Gestionar el límite de dispositivos o sesiones
Prioridad: P1  
Como cliente, quiero recibir una explicación si he alcanzado el límite de dispositivos o sesiones de mi cuenta, para saber por qué no puedo continuar.
Criterios de aceptación
El backend determina si se ha alcanzado el límite configurado.
La aplicación muestra un mensaje claro y no inventa un límite numérico.
La aplicación no intenta evadir el control abriendo sesiones adicionales.
El mensaje indica el procedimiento aprobado para resolver el caso, si existe.
Dependencia: negocio debe confirmar el número máximo de dispositivos/sesiones y la política de reemplazo.
5. Épica ÉP-03: Explorar el catálogo
HU-CLI-007 — Acceder a la pantalla principal
Prioridad: P0  
Como cliente autenticado, quiero ver una pantalla principal con las secciones disponibles, para elegir rápidamente qué tipo de contenido explorar.
Criterios de aceptación
La pantalla principal presenta solo las secciones habilitadas para la cuenta y disponibles en la API.
El foco inicial es visible y permite comenzar la navegación con el control remoto.
Si el catálogo está cargando, se muestra un estado de carga.
Si no hay secciones disponibles, se muestra un estado vacío comprensible.
Un error de red se distingue de un catálogo vacío.
HU-CLI-008 — Explorar categorías de televisión en vivo
Prioridad: P0  
Como cliente, quiero explorar las categorías de canales en vivo, para encontrar el tipo de programación que busco.
Criterios de aceptación
La aplicación solicita las categorías autorizadas al backend.
Las categorías se muestran con el nombre recibido y un orden consistente.
Si no hay categorías, se presenta un estado vacío.
Si la consulta falla, puedo reintentar cuando el servicio esté disponible.
No se muestran elementos a los que mi cuenta no tenga acceso.
HU-CLI-009 — Explorar canales de una categoría
Prioridad: P0  
Como cliente, quiero ver los canales de una categoría, para seleccionar el canal que deseo ver.
Criterios de aceptación
Al seleccionar una categoría, se muestran sus canales disponibles.
Cada canal muestra al menos el nombre disponible; el logotipo se presenta cuando existe.
El elemento enfocado es visible y el control remoto permite recorrer la lista.
La aplicación gestiona correctamente listas vacías, carga y errores.
Los datos se obtienen de la API autorizada.
HU-CLI-010 — Explorar películas
Prioridad: P0  
Como cliente, quiero navegar por las categorías de películas, para encontrar una película disponible en mi servicio.
Criterios de aceptación
La aplicación presenta las categorías de películas habilitadas.
Al entrar a una categoría, muestra las películas devueltas por el backend.
La portada y los metadatos se muestran solo si están disponibles.
Si no hay resultados, se muestra un estado vacío.
No se muestran enlaces de reproducción ni datos internos sensibles en la interfaz.
HU-CLI-011 — Consultar detalles de una película
Prioridad: P1  
Como cliente, quiero consultar la información disponible de una película, para decidir si deseo reproducirla.
Criterios de aceptación
Al seleccionar una película, se presenta una vista de detalle.
Se muestran título y demás metadatos que existan, como descripción, año, duración o imagen.
La ausencia de un metadato no rompe el diseño.
Si el contenido ya no está disponible, se muestra un error recuperable.
La acción de reproducir solo está disponible si el backend permite el acceso.
HU-CLI-012 — Explorar series, temporadas y episodios
Prioridad: P0  
Como cliente, quiero explorar series y sus episodios disponibles, para elegir el episodio que deseo ver.
Criterios de aceptación
La aplicación muestra las categorías de series autorizadas.
Al seleccionar una serie, presenta temporadas y episodios si la fuente los proporciona.
Cada episodio puede identificarse por los datos disponibles, como título o número.
Si una serie no tiene episodios disponibles, se muestra un estado vacío.
La aplicación no inventa temporadas, episodios ni metadatos ausentes.
6. Épica ÉP-04: Reproducir contenido
HU-CLI-013 — Ver un canal en vivo
Prioridad: P0  
Como cliente, quiero seleccionar un canal e iniciar su reproducción, para ver la programación en directo incluida en mi servicio.
Criterios de aceptación
Al seleccionar un canal autorizado, la aplicación solicita al backend los datos o la autorización de reproducción.
La reproducción comienza si la autorización y el flujo son válidos y compatibles con el dispositivo.
Mientras se prepara el flujo, se muestra un indicador de carga.
Si el canal no está disponible, se presenta un error comprensible y puedo volver al catálogo.
La aplicación no expone credenciales de línea ni secretos en pantalla o logs.
HU-CLI-014 — Usar los controles básicos del reproductor
Prioridad: P0  
Como cliente que está viendo contenido, quiero usar los controles de reproducción que soporte el flujo, para manejar la visualización de forma sencilla.
Criterios de aceptación
Los controles disponibles corresponden a capacidades reales del reproductor y del tipo de contenido.
La navegación con control remoto permite mostrar, usar y ocultar los controles.
La aplicación no ofrece pausa, búsqueda temporal u otras acciones que no estén soportadas.
Los estados de carga, reproducción, pausa y error se reflejan correctamente.
Las acciones repetidas no inician múltiples reproducciones simultáneas.
HU-CLI-015 — Reproducir una película
Prioridad: P0  
Como cliente, quiero reproducir una película seleccionada, para verla en mi televisor.
Criterios de aceptación
La aplicación verifica el acceso mediante el backend.
La reproducción comienza si el contenido y el formato son compatibles.
Si el contenido está bloqueado, eliminado o no disponible, se muestra un mensaje recuperable.
Al salir del reproductor, regreso a una pantalla coherente del catálogo.
Los recursos de reproducción se liberan al finalizar o salir.
HU-CLI-016 — Reproducir un episodio de una serie
Prioridad: P0  
Como cliente, quiero seleccionar un episodio e iniciar su reproducción, para ver el contenido de una serie.
Criterios de aceptación
Puedo seleccionar un episodio existente y autorizado.
El backend entrega o autoriza los datos de reproducción.
Si el episodio no puede reproducirse, recibo un mensaje claro.
Al salir, puedo regresar a la serie o lista de episodios.
La aplicación libera los recursos multimedia al terminar.
HU-CLI-017 — Recuperarse de un error de reproducción
Prioridad: P0  
Como cliente, quiero recibir una explicación si un contenido no puede reproducirse, para saber si debo reintentar o elegir otro contenido.
Criterios de aceptación
La aplicación diferencia, cuando sea posible, un problema de red, un error del reproductor y un rechazo de autorización.
Se ofrece reintentar cuando sea apropiado y sin repetir indefinidamente la solicitud.
Se puede volver al catálogo sin cerrar toda la aplicación.
No se muestran URL privadas, tokens, contraseñas ni detalles internos.
El error no deja el audio activo después de abandonar el reproductor.
HU-CLI-018 — Volver al catálogo desde el reproductor
Prioridad: P0  
Como cliente, quiero salir de la reproducción y volver al catálogo, para elegir otro contenido.
Criterios de aceptación
El botón atrás o la acción de salida aprobada regresa al contexto anterior.
La reproducción se detiene al salir, salvo que el diseño aprobado defina expresamente otro comportamiento.
No queda audio o vídeo reproduciéndose accidentalmente en segundo plano.
El foco vuelve a un elemento lógico de la pantalla anterior.
HU-CLI-019 — Consultar la guía de programación
Prioridad: P1  
Como cliente, quiero consultar la programación de un canal, para conocer qué contenido se emite y cuándo.
Criterios de aceptación
La guía solo se muestra si la API y la fuente proporcionan datos EPG.
Se indican los horarios con la zona horaria definida por el producto.
Si no hay información, se informa que la guía no está disponible.
La aplicación no genera nombres de programas ni horarios ficticios.
Dependencia: confirmar disponibilidad, calidad y zona horaria de los datos EPG.
7. Épica ÉP-05: Navegación y manejo de errores
HU-CLI-020 — Navegar solo con el control remoto
Prioridad: P0  
Como cliente que usa un televisor, quiero completar el flujo principal con el control remoto, para utilizar la aplicación desde el sofá sin necesitar una pantalla táctil.
Criterios de aceptación
Puedo recorrer todos los controles esenciales con las teclas direccionales.
Puedo confirmar una selección y volver a la pantalla anterior.
El foco es visible en todo momento.
No hay callejones sin salida en los recorridos principales.
Las acciones principales no requieren mouse o teclado físico.
HU-CLI-021 — Recuperarme de una pérdida de conexión
Prioridad: P0  
Como cliente, quiero saber cuándo la aplicación no puede comunicarse con el servicio y volver a intentarlo, para continuar cuando la conexión se recupere.
Criterios de aceptación
La aplicación presenta un mensaje de conexión cuando corresponde.
No se cierra inesperadamente ni muestra un catálogo vacío como si fuera un resultado normal.
Puedo reintentar la operación cuando sea seguro.
Los reintentos tienen límites y no bloquean la navegación.
Una sesión no válida no se considera válida solo porque la red esté desconectada.
HU-CLI-022 — Entender por qué no tengo acceso a un contenido
Prioridad: P1  
Como cliente, quiero recibir un mensaje comprensible cuando no tenga autorización para un contenido, para distinguir un problema de acceso de un fallo técnico.
Criterios de aceptación
La aplicación presenta un mensaje apropiado según el error informado por el backend.
No expone detalles internos de autorización.
No afirma que el contenido esté dañado si el problema es de permisos.
Si existe un procedimiento de soporte aprobado, se indica de forma breve.
HU-CLI-023 — Volver a abrir la aplicación
Prioridad: P1  
Como cliente, quiero que al abrir nuevamente la aplicación se gestione mi sesión de forma coherente, para continuar sin repetir pasos innecesarios cuando sea seguro.
Criterios de aceptación
La aplicación comprueba la validez de la sesión según el contrato del backend.
Si la sesión sigue válida y la política lo permite, abre la pantalla principal.
Si venció o fue revocada, solicita autenticación.
No inicia automáticamente contenido que requiera una nueva autorización.
Los datos temporales de reproducción se limpian según las reglas de ciclo de vida de la plataforma.
8. Épica ÉP-06: Mejoras posteriores sujetas a aprobación
Las siguientes historias no forman parte del MVP hasta que producto apruebe alcance, diseño, persistencia y API.
HU-CLI-024 — Buscar contenido (P2)
Como cliente, quiero buscar por título, para encontrar un canal, película o serie sin recorrer todas las categorías.
Criterios de aceptación
La búsqueda consulta el catálogo autorizado.
Se pueden recorrer los resultados con el control remoto.
Se muestra un estado cuando no hay coincidencias.
La búsqueda gestiona errores de red y entradas vacías.
Se define si la búsqueda es local, remota o combinada antes de implementarla.
HU-CLI-025 — Marcar contenido como favorito (P2)
Como cliente, quiero marcar contenido como favorito, para acceder más rápido a lo que veo habitualmente.
Criterios de aceptación
El estado de favorito se conserva según la política aprobada.
El cambio se confirma visualmente.
La aplicación maneja errores al guardar o retirar un favorito.
Se define si los favoritos se sincronizan entre televisores.
HU-CLI-026 — Continuar viendo (P2)
Como cliente, quiero continuar una película o episodio desde el punto donde lo dejé, para no tener que buscar manualmente la posición anterior.
Criterios de aceptación
La aplicación guarda la posición solo si el contenido y el reproductor lo permiten.
La posición se recupera para la cuenta y el contenido correctos.
La política de sincronización entre dispositivos queda definida.
La función no se presenta como disponible para flujos live si no corresponde.
HU-CLI-027 — Consultar ayuda o información de soporte (P2)
Como cliente, quiero encontrar información básica de ayuda, para saber qué hacer ante problemas de acceso o reproducción.
Criterios de aceptación
La sección contiene únicamente información aprobada por COTAS.
Puede mostrar la versión de la aplicación y una referencia de error no sensible.
No expone configuraciones de infraestructura ni credenciales.
Los canales de contacto se incorporan solo cuando negocio los confirme.
9. Mapa de trazabilidad con requerimientos funcionales
Historia	Requerimientos funcionales relacionados
HU-CLI-001	RF-001 a RF-006, RF-049 a RF-051
HU-CLI-002	RF-005, RF-006, RF-050
HU-CLI-003	RF-007, RF-009, RF-010
HU-CLI-004	RF-008
HU-CLI-005	RF-011, RF-012
HU-CLI-006	RF-013 a RF-015
HU-CLI-007	RF-016, RF-017, RF-020
HU-CLI-008	RF-018, RF-020
HU-CLI-009	RF-019, RF-026, RF-028
HU-CLI-010	RF-034
HU-CLI-011	RF-039, RF-040
HU-CLI-012	RF-036, RF-037
HU-CLI-013	RF-026, RF-027, RF-033, RF-042 a RF-048
HU-CLI-014	RF-029, RF-042 a RF-048
HU-CLI-015	RF-035, RF-039, RF-040, RF-046
HU-CLI-016	RF-037, RF-038, RF-046
HU-CLI-017	RF-033, RF-040, RF-047, RF-050 a RF-053
HU-CLI-018	RF-030, RF-045, RF-046
HU-CLI-019	RF-032
HU-CLI-020	RF-042, RF-043, RF-045
HU-CLI-021	RF-050, RF-052
HU-CLI-022	RF-040, RF-051, RF-053
HU-CLI-023	RF-007, RF-009, RF-010
HU-CLI-024	RF-024
HU-CLI-025	RF-025
HU-CLI-026	RF-041
HU-CLI-027	RF-053
10. Orden sugerido del backlog del MVP
HU-CLI-001, HU-CLI-002, HU-CLI-003 y HU-CLI-004: autenticación y sesión.
HU-CLI-005 y HU-CLI-006: dispositivo y reglas de acceso.
HU-CLI-007 a HU-CLI-012: pantalla principal y catálogo.
HU-CLI-013 a HU-CLI-018: reproducción live, películas, series y recuperación de errores.
HU-CLI-020 a HU-CLI-023: control remoto, conectividad y ciclo de vida.
HU-CLI-019 y las historias P2: incorporar solo después de validar dependencias y aprobar alcance.
El orden es una propuesta inicial; el equipo debe ajustarlo según dependencias reales del backend, XUI, streaming y dispositivos.
11. Condiciones de validación antes de cerrar las historias
Confirmar qué credenciales usa el cliente y cómo se crea o vincula su cuenta.
Resolver el `401 INVALID_CREDENTIALS` del flujo de autenticación backend-XUI.
Documentar el contrato real de la API y probarlo desde ambas aplicaciones.
Confirmar los tipos de contenido y formatos de reproducción disponibles.
Definir el límite de dispositivos y sesiones.
Validar los flujos en equipos reales Google TV/Android TV y Samsung objetivo.
Mantener las mismas reglas de negocio y criterios de aceptación para ambas plataformas.
12. Definición de terminado (DoD) para cada historia
Una historia se considera terminada cuando:
Cumple todos sus criterios de aceptación.
La implementación respeta los requerimientos no funcionales aplicables.
Tiene pruebas registradas y resultados reproducibles.
Los estados de carga, error y ausencia de datos están implementados.
Se verificó navegación con control remoto.
Se probó en la plataforma correspondiente y se comprobó la equivalencia funcional con la otra plataforma cuando aplique.
No se han introducido secretos ni conexiones directas a PostgreSQL/XUI en el cliente.
La documentación y trazabilidad están actualizadas.
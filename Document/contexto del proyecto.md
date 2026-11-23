INFORME MAESTRO DEL PROYECTO
Plataforma Multiplataforma de Streaming — COTAS PERU S.A.C.
Versión: 1.0  
Fecha: 16 de octubre de 2026  
Estado: Documento maestro de alcance y arquitectura objetivo; pendiente de validación técnica y aprobación formal  
Destinatarios: Dirección del proyecto, desarrolladores, arquitectura, infraestructura, QA, seguridad y operaciones  
Documento relacionado: Arquitectura completa y escalable del sistema multiplataforma
Propósito del documento
Este informe reúne en un solo documento la visión del producto, alcance, arquitectura objetivo, tecnologías por componente, integraciones, seguridad, infraestructura, metodología de desarrollo, planificación por sprints, pruebas, criterios de aceptación, riesgos y entregables de la plataforma de streaming de COTAS PERU S.A.C.
Su finalidad es que cualquier integrante del equipo pueda comprender:
Qué producto se desarrollará y qué problema resolverá.
Qué componentes forman parte de la solución.
Qué responsabilidad tiene cada componente y cómo se comunica con los demás.
Qué tecnologías se utilizarán como base.
Qué sistemas existentes se deben reutilizar.
Qué se construirá primero y qué quedará para fases posteriores.
Cómo se probará cada entrega y qué evidencia permitirá considerarla terminada.
Qué decisiones deben verificarse antes de implementar o desplegar.
Cómo se pasará del servidor de pruebas al servidor principal de forma controlada.
Este documento describe la arquitectura objetivo y el plan de trabajo. No certifica que las integraciones, los endpoints, los recursos de infraestructura o los despliegues ya estén implementados. Cada afirmación de funcionamiento deberá respaldarse con inspección, pruebas y evidencias.
Resumen ejecutivo
COTAS PERU S.A.C. desarrollará una plataforma de aplicaciones de streaming para que los usuarios accedan a los servicios audiovisuales de la empresa desde televisores y, según el alcance que se apruebe, otros dispositivos.
La solución reutilizará la infraestructura de streaming existente —XUI/Xprom, Astra y VOD— en lugar de reemplazarla. Se desarrollará un backend central para administrar los casos de uso de la aplicación, centralizar la autenticación y autorización, normalizar el catálogo disponible y coordinar la autorización de reproducción. Los clientes recibirán instrucciones de reproducción autorizadas y el video se distribuirá directamente desde la infraestructura de streaming hacia el dispositivo, sin atravesar la API ni la base de datos.
La prioridad de implementación es Google TV / Android TV. Samsung Smart TV con Tizen se desarrollará como cliente adicional. La arquitectura general contempla también dispositivos TV Box/Stick, una aplicación móvil Android/iOS y un panel administrativo web; sin embargo, estos últimos deben distinguirse entre alcance inicial confirmado y expansión prevista, para no ampliar el MVP sin aprobación.
El backend se plantea como una aplicación modular con Node.js, TypeScript y NestJS, expuesta mediante API REST documentada con OpenAPI. PostgreSQL almacenará los datos propios que realmente necesite la aplicación y Prisma se utilizará como capa de acceso a datos si se confirma en el repositorio. El panel administrativo se plantea con React, TypeScript y Vite y deberá permanecer restringido a la red interna o VPN.
La arquitectura completa dibujada incluye DNS/TLS, WAF, API Gateway o balanceador, instancias horizontales del backend, Redis, cola de tareas/workers, observabilidad, gestión de archivos, PostgreSQL principal, réplica de lectura y copias de seguridad. Estos elementos conforman la arquitectura escalable de referencia, pero su implementación debe ser progresiva y justificarse con requisitos, capacidad disponible y mediciones. No se añadirán servicios de forma automática si la primera versión puede funcionar de manera segura y mantenible con una topología más sencilla.
El desarrollo se realizará primero en el servidor de pruebas proporcionado por la empresa, con acceso autorizado a los servicios necesarios. El servidor principal no se modificará hasta que la solución esté implementada, integrada, probada, documentada y aprobada para producción.
Contexto y problema que resuelve
La empresa ya cuenta con infraestructura para gestionar y distribuir contenido audiovisual. El proyecto no busca sustituir dicha infraestructura, sino crear aplicaciones y una capa de negocio que permitan acceder a esos servicios desde distintas plataformas.
El trabajo debe resolver las siguientes necesidades:
Proporcionar una experiencia de usuario apropiada para televisores y sus controles remotos.
Centralizar las operaciones de autenticación, catálogo y autorización de reproducción.
Integrar los sistemas existentes mediante interfaces verificadas.
Evitar que los clientes tengan acceso directo a bases de datos o credenciales administrativas.
Separar el panel administrativo de los servicios públicos usados por los clientes.
Disponer de un proceso de pruebas y despliegue que reduzca el riesgo de afectar la operación existente.
Mantener los costos bajo control, reutilizando infraestructura y evitando complejidad prematura.
Facilitar la incorporación futura de otros clientes mediante contratos de API estables.
Objetivos del proyecto
4.1 Objetivo general
Diseñar, desarrollar, integrar y validar una plataforma multiplataforma de streaming que permita a los usuarios autorizados acceder a los contenidos de COTAS PERU S.A.C. desde aplicaciones de televisión y otros clientes aprobados, reutilizando la infraestructura audiovisual existente y aplicando controles adecuados de seguridad, mantenibilidad y operación.
4.2 Objetivos específicos
Desarrollar primero un cliente para Google TV / Android TV.
Desarrollar un cliente para Samsung Tizen compatible con los modelos definidos.
Diseñar e implementar una API central documentada.
Definir e implementar los flujos de autenticación, catálogo y autorización de reproducción.
Verificar las interfaces y versiones reales de XUI/Xprom, Astra y VOD.
Crear adaptadores para desacoplar el backend de las peculiaridades de cada sistema externo.
Persistir únicamente los datos propios que sean necesarios, con PostgreSQL si se confirma la decisión técnica.
Implementar un panel administrativo web para operaciones aprobadas, restringido a red interna/VPN.
Implementar controles de seguridad, logs, métricas básicas, respaldos y recuperación.
Validar la solución en el servidor de pruebas antes de migrarla a producción.
Documentar instalación, configuración, pruebas, operación y reversión.
Mantener trazabilidad entre requisitos, tareas, pruebas y evidencias.
Alcance del producto
5.1 Alcance inicial prioritario (MVP)
El MVP debe concentrarse en un flujo vertical completo:
Autenticación → catálogo autorizado → selección de contenido → autorización de reproducción → reproducción real en Google TV / Android TV.
Incluye:
Cliente Google TV / Android TV.
Backend y API REST.
Integración real con las fuentes de contenido que tengan interfaces verificadas.
Persistencia mínima de datos propios.
Seguridad básica de autenticación, autorización, configuración y comunicaciones.
Entorno de pruebas reproducible.
Pruebas unitarias, de integración, funcionales y en dispositivos disponibles.
Documentación y criterios de aceptación.
5.2 Alcance de la primera versión ampliada
Cliente Samsung Tizen, con compatibilidad declarada únicamente para los modelos y versiones realmente probados.
Panel administrativo interno con roles y operaciones aprobadas.
Logs y métricas operativas básicas.
Gestión de errores, sesiones, conectividad y reproducción.
Proceso de despliegue y reversión documentado.
Pruebas de seguridad, rendimiento y regresión.
5.3 Arquitectura objetivo escalable
El diagrama general contempla los siguientes clientes y servicios:
Samsung Smart TV (Tizen).
Android TV / Google TV.
Dispositivo externo TV Box / Stick.
Aplicación móvil Android/iOS.
Panel administrativo web.
DNS, TLS/HTTPS, WAF y API Gateway/balanceador.
Instancias de backend NestJS.
Módulos de autenticación/usuarios, dispositivos/activaciones, catálogo, reproducción, notificaciones y administración.
Redis para caché/datos temporales, si se justifica.
Cola de tareas y workers, por ejemplo BullMQ, si se requieren tareas asíncronas.
Observabilidad y monitoreo.
Gestión de archivos.
PostgreSQL principal, posible réplica de lectura y copias de seguridad.
Adaptadores para XUI/Xprom, Astra y VOD.
Distribución directa de video desde la infraestructura existente a los dispositivos.
La presencia de estos elementos en el diseño objetivo no significa que todos sean necesarios en la primera entrega. Cada uno debe tener un motivo funcional, operativo o de capacidad documentado.
5.4 Fuera del alcance inicial salvo aprobación
Sustituir o reconstruir XUI/Xprom, Astra o VOD.
Reconstruir la infraestructura de distribución de video.
Incorporar una CDN en la primera versión.
Comprar otro VPS como solución predeterminada.
Adoptar microservicios sin una necesidad demostrada.
Agregar Redis, workers, réplica de lectura o autoescalado sin evaluar requisitos y mediciones.
Exponer públicamente el panel administrativo o la base de datos.
Cambiar servicios críticos de streaming sin autorización.
Declarar compatibilidad con dispositivos no probados.
Publicar aplicaciones en tiendas sin cumplir los procesos de firma, revisión y aprobación correspondientes.
Usuarios y actores
Los actores deben validarse con el responsable del producto. Como base:
Actor	Necesidad	Acceso previsto
Usuario de TV	Iniciar sesión, consultar catálogo y reproducir contenido autorizado	Aplicación de TV y API
Usuario de TV Box/Stick	Acceder al contenido desde el dispositivo aprobado	Cliente compatible y API; fase sujeta a alcance
Usuario móvil	Acceder desde Android/iOS	Aplicación móvil; confirmar si entra en el MVP
Administrador	Ejecutar operaciones administrativas autorizadas	Panel interno/VPN
Operador/soporte	Consultar información operativa permitida y atender incidencias	Panel según permisos aprobados
Equipo de desarrollo	Construir y probar la solución	Repositorio y entorno de pruebas autorizados
Infraestructura/operaciones	Administrar servidor, red, certificados y despliegues	Acceso administrativo restringido
Sistemas existentes	Proveer datos, permisos o contenido mediante interfaces disponibles	Integraciones verificadas
No deben asumirse roles, permisos o procesos comerciales que todavía no estén definidos. La matriz de roles debe aprobarse antes de implementar acciones administrativas.
7. Arquitectura general del sistema
La arquitectura se organiza en siete capas.
Capa 1 — Clientes y aplicaciones
Samsung Smart TV (Tizen):
Cliente específico para el entorno Tizen.
Navegación con control remoto y gestión de foco.
Comunicación HTTPS con la API.
Reproducción mediante AVPlay cuando sea compatible.
Empaquetado, firma y distribución según los requisitos de Tizen.
Google TV / Android TV:
Cliente prioritario.
Kotlin, Android SDK y Jetpack Compose for TV.
Media3/ExoPlayer para reproducción compatible con los formatos y protocolos confirmados.
Navegación por control remoto, estados de carga/error y recuperación de sesión.
TV Box / Stick:
En la arquitectura objetivo puede utilizar el cliente Android TV cuando el dispositivo sea compatible.
La compatibilidad no debe generalizarse a todos los dispositivos; se debe definir y probar la lista objetivo.
Aplicación móvil Android/iOS:
Está representada en la arquitectura completa.
Su inclusión y prioridad deben confirmarse en el alcance del MVP.
Si se aprueba, deberá tener cliente y diseño de interfaz propios, consumiendo los mismos contratos de API.
Panel administrativo web:
React, TypeScript y Vite como stack de referencia.
Acceso interno o VPN.
Autenticación y permisos validados en backend.
Nunca accede directamente a PostgreSQL.
Capa 2 — Acceso y seguridad perimetral
Elementos del diagrama:
DNS para resolver el dominio de la API.
TLS/HTTPS para proteger la comunicación.
WAF para protección de solicitudes web/API, si está disponible y se justifica.
API Gateway o balanceador para enrutar tráfico hacia las instancias de backend.
La topología inicial puede ser más sencilla que el diagrama escalable. Deben verificarse la infraestructura disponible, la necesidad de WAF y balanceador, el certificado TLS y las reglas de firewall. No se debe exponer un servicio solo porque aparezca en el diagrama.
Capa 3 — Backend central escalable
Tecnología base: Node.js + TypeScript + NestJS.
El backend central expone API REST documentada con OpenAPI. La arquitectura inicial será un monolito modular: una aplicación con módulos y límites claros, preparada para ejecutar más de una instancia si las mediciones y la infraestructura lo requieren.
Módulos de negocio previstos:
Autenticación y usuarios: autenticación y gestión de identidad según la fuente de verdad aprobada.
Dispositivos y activaciones: registro o control de dispositivos cuando el producto y los sistemas existentes lo requieran.
Catálogo: normalización y entrega de canales, películas, series u otros elementos que realmente estén disponibles.
Reproducción: verificación de permisos y coordinación de la autorización de reproducción.
Notificaciones: correo y eventos, si los casos de uso aprobados los requieren.
Administración: operaciones administrativas autorizadas.
Integraciones: adaptadores hacia XUI/Xprom, Astra y VOD.
Auditoría: registro de acciones y eventos operativos relevantes.
Salud/configuración: comprobaciones operativas y validación de configuración.
Los nombres son una estructura de referencia; los módulos finales deben corresponder a requisitos reales. No deben duplicarse usuarios, suscripciones o catálogos si otro sistema ya es su fuente de verdad, salvo justificación aprobada.
Escalado horizontal:
El diagrama propone múltiples instancias de la misma aplicación.
Las instancias deben ser equivalentes y no depender de estado de sesión guardado solo en memoria local.
La configuración y los secretos deben gestionarse por entorno.
El escalado y el autoescalado solo se implementarán si la infraestructura lo soporta y las métricas lo justifican.
El balanceador no sustituye las medidas de seguridad de la API.
Capa 4 — Servicios compartidos y tareas de soporte
Redis (opcional, sujeto a justificación):
Caché de datos temporales o sesiones cuando el diseño lo requiera.
Tokens o datos temporales solo con controles de seguridad apropiados.
No introducirlo solo por anticipar escalabilidad.
Cola de tareas/workers (opcional):
Procesamiento asíncrono de correos, notificaciones, eventos o tareas programadas.
BullMQ es una alternativa de referencia si se aprueba Redis y existe una necesidad real de cola.
Las tareas deben tener política de reintento, idempotencia, observabilidad y manejo de fallos.
Observabilidad:
Logs estructurados.
Métricas de API y dependencias.
Alertas y trazas cuando sean necesarias.
Dashboards para operación.
No registrar contraseñas, tokens completos ni URLs sensibles de reproducción.
Gestión de archivos:
Imágenes, recursos multimedia, archivos de respaldo o integración cloud/local solo cuando los casos de uso lo requieran.
Definir permisos, retención, tamaño máximo y almacenamiento.
No convertir el backend en intermediario de video si la infraestructura existente puede entregar el flujo directamente.
Capa 5 — Persistencia de datos
PostgreSQL: base de datos principal para los datos propios de la aplicación que realmente se necesiten.
Prisma: ORM de referencia, sujeto a confirmar el estado del repositorio y la decisión técnica.
Réplica de lectura: componente de arquitectura objetivo para consultas/reportes cuando exista una necesidad de rendimiento y la infraestructura permita administrarla. No se requiere automáticamente en el MVP.
Copias de seguridad: deben ser independientes del servidor principal, tener política de retención y contar con pruebas de restauración. Una copia no se considera válida solo porque el proceso de backup haya terminado; debe verificarse que puede restaurarse.
Principios:
La base de datos no se expone a Internet.
Solo el backend y las herramientas autorizadas pueden conectarse.
Usar privilegios mínimos.
Versionar migraciones y revisar cambios de esquema.
Definir la fuente de verdad de usuarios, suscripciones, activaciones y catálogo.
No almacenar secretos en texto plano.
No duplicar datos de XUI/Xprom u otros sistemas sin una razón funcional.
Capa 6 — Integración con la infraestructura existente
Sistemas identificados:
XUI/Xprom: gestión de usuarios/permisos, catálogo o autorización de contenido, según las capacidades reales instaladas.
Astra: gestión de señales y canales en vivo, según la configuración real.
Servidor VOD: películas, series y archivos multimedia, según los protocolos y servicios disponibles.
No se da por confirmado ningún endpoint, versión, autenticación o esquema de datos. Antes de desarrollar un adaptador se debe inspeccionar la instalación y documentar las operaciones permitidas.
La capa de integración debe:
Encapsular los detalles de cada sistema.
Normalizar respuestas a un modelo interno.
Manejar timeouts, errores, indisponibilidad y reintentos limitados.
Evitar credenciales administrativas en clientes.
Evitar escribir en bases de datos internas de estos sistemas sin autorización expresa.
Contar con pruebas de contrato/integración y evidencia real.
Capa 7 — Distribución de video independiente del backend
El backend coordina identidad, permisos, catálogo y autorización, pero no debe transportar el flujo audiovisual. Una vez autorizado, el cliente reproduce desde la fuente correspondiente de la infraestructura existente.
El protocolo y el mecanismo exactos —por ejemplo, HLS, DASH u otro— se deben verificar según los sistemas, contenido y dispositivos reales. No se debe asumir que todas las fuentes o televisores soportan los mismos formatos.
Las URLs o tokens de reproducción, si existen, se deben tratar como información sensible, limitar su exposición y evitar registrarlos completos.
8. Stack tecnológico por componente
Componente	Stack de referencia	Estado
Android TV / Google TV	Kotlin, Android SDK, Jetpack Compose for TV, Media3/ExoPlayer, Coroutines/Flow	Base acordada; verificar repositorio y dispositivos
Samsung Tizen	TypeScript, APIs Tizen, React/Vite si compatible, AVPlay cuando corresponda	Base acordada; validar versión/modelos
TV Box/Stick	Cliente Android TV si el dispositivo es compatible	Alcance por confirmar y pruebas necesarias
Aplicación móvil	Android/iOS; el diagrama muestra Flutter/Dart como opción	Alcance y stack deben confirmarse antes de incluirlo en el MVP
Backend	Node.js, TypeScript, NestJS, API REST	Base acordada
Contrato API	OpenAPI	Requerido
Base de datos	PostgreSQL	Tecnología de referencia
ORM	Prisma	Propuesto; confirmar en el repositorio
Panel administrativo	React, TypeScript, Vite	Base de referencia
Caché	Redis	Opcional, condicionado a necesidad
Procesamiento asíncrono	Worker/cola; BullMQ como alternativa	Opcional, condicionado a necesidad
Perímetro	DNS, TLS/HTTPS, WAF, API Gateway/balanceador	Implementar según infraestructura y riesgo
Pruebas	Unitarias, contrato, integración, E2E, seguridad, rendimiento, regresión	Requeridas según el componente
Observabilidad	Logs, métricas, alertas y trazas según necesidad	Nivel básico requerido; expansión progresiva
Las decisiones tecnológicas deben registrarse en un documento de decisiones arquitectónicas (ADR) cuando afecten seguridad, costos, despliegue o compatibilidad.
9. Contratos de API y flujos principales
Antes de desarrollar clientes y backend en paralelo, deben acordarse los contratos API. OpenAPI será la referencia para rutas, esquemas, errores, autenticación y versionado.
Como mínimo se deben especificar los casos de uso:
Inicio de sesión.
Cierre de sesión/revocación, según mecanismo elegido.
Consulta del perfil o contexto autorizado.
Consulta del catálogo.
Consulta de detalle de contenido, si corresponde.
Solicitud de autorización de reproducción.
Gestión de dispositivos/activaciones, si se incluye.
Funciones administrativas aprobadas.
Health checks internos.
Los nombres y rutas definitivos se definirán después de revisar la fuente de verdad y las interfaces existentes. No se deben inventar rutas de XUI/Xprom, Astra o VOD ni confundir los endpoints internos de esos sistemas con los de la nueva API.
Flujo A: autenticación
El cliente envía credenciales mediante HTTPS.
El backend valida la identidad con la fuente de verdad aprobada.
El backend aplica las reglas de acceso y emite o establece la sesión según el mecanismo elegido.
El cliente almacena la información de sesión de manera apropiada para su plataforma.
La expiración y revocación se gestionan de forma explícita.
Los errores no revelan información sensible.
Flujo B: catálogo
El cliente solicita el catálogo.
El backend valida sesión y permisos.
El servicio de catálogo obtiene datos desde la fuente de verdad verificada.
El adaptador normaliza la respuesta.
El backend entrega solo los elementos autorizados.
Se registran errores de integración sin exponer secretos.
Flujo C: reproducción
El usuario selecciona un contenido.
El cliente solicita autorización.
El backend valida sesión y permisos.
El adaptador consulta el mecanismo real de autorización cuando sea necesario.
El backend devuelve los datos de reproducción permitidos.
El reproductor del dispositivo abre la fuente autorizada directamente.
Los errores se reportan de forma controlada y se evita registrar tokens o URLs sensibles.
Flujo D: administración
El administrador accede desde red interna/VPN.
El backend autentica y valida el rol.
El panel muestra solo funciones autorizadas.
Cada operación vuelve a validar permisos en el backend.
Las acciones relevantes se registran en auditoría.
La base de datos y los sistemas de streaming no quedan expuestos directamente al navegador.
10. Seguridad y privacidad
Identidad y permisos
La API valida permisos del lado del servidor.
Aplicar principio de mínimo privilegio.
Definir roles y permisos antes de construir funciones administrativas.
Gestionar expiración, revocación y almacenamiento de sesión.
Aplicar limitación de intentos y rate limiting a operaciones sensibles.
Revisar acceso a dispositivos y activaciones si esa funcionalidad se aprueba.
Red y exposición
API pública solo por los puertos estrictamente necesarios, normalmente HTTPS.
PostgreSQL y servicios administrativos solo accesibles desde redes/componentes autorizados.
Panel administrativo restringido a red interna o VPN.
DNS y certificado TLS válidos antes de habilitar acceso externo.
Revisar reglas de firewall y WAF; no asumir que su presencia por sí sola garantiza seguridad.
Secretos
No incluir contraseñas, tokens, claves privadas o credenciales de servicios en el código.
Separar configuración de desarrollo, pruebas y producción.
Mantener secretos en configuración protegida del entorno.
No usar credenciales administrativas de XUI/Xprom, Astra o VOD desde clientes.
No incluir secretos en capturas, informes o logs.
Datos y auditoría
Minimizar los datos personales almacenados.
Definir retención y acceso a logs.
Registrar operaciones administrativas relevantes.
No guardar tokens completos ni datos de reproducción sensibles en logs.
Definir procedimiento de revocación de credenciales y respuesta ante incidentes.
Cambios críticos
No realizar cambios destructivos ni modificar producción, firewall, DNS, certificados, base de datos o servicios de streaming críticos sin explicar el impacto, disponer de respaldo y obtener la aprobación correspondiente.
11. Infraestructura y despliegue
11.1 Desarrollo
El equipo desarrollará principalmente en sus equipos de trabajo. Debe existir una configuración local reproducible, con datos de prueba y sin secretos de producción.
11.2 Servidor de pruebas
La empresa proporciona un servidor de pruebas con acceso a los servicios necesarios para desarrollar e integrar la plataforma. En ese entorno se debe:
Verificar conectividad y versiones de los servicios.
Desplegar backend y persistencia de prueba según la arquitectura aprobada.
Ejecutar pruebas con cuentas y contenido autorizados.
Validar integraciones reales.
Medir recursos y registrar incidencias.
Evitar que las pruebas afecten a clientes reales o a servicios críticos.
No copiar datos sensibles de producción sin autorización.
11.3 Servidor principal
El servidor principal se utilizará únicamente cuando:
El alcance y la arquitectura estén aprobados.
Los flujos críticos estén integrados y probados.
Se hayan corregido los defectos que bloquean la salida.
Se conozcan las dependencias y los recursos disponibles.
Existan respaldos verificables y un plan de reversión.
Se apruebe una ventana de despliegue y responsables.
11.4 Recursos de referencia
Como punto de evaluación inicial para los componentes de aplicación y datos se ha propuesto:
4 vCPU.
8 GB de RAM.
30 GB SSD/NVMe.
PostgreSQL.
Respaldo independiente.
API accesible por HTTPS.
Panel administrativo privado.
Estos valores son una referencia para comparar contra la infraestructura real, no una garantía de capacidad. Antes de decidir el despliegue se debe inspeccionar el servidor, sus servicios actuales, espacio disponible, CPU/RAM, red, almacenamiento y carga de streaming. La cantidad de clientes registrados debe diferenciarse de las sesiones activas y reproducciones simultáneas.
11.5 DNS, TLS y dominio
La API necesita un dominio o subdominio corporativo confirmado por el responsable de infraestructura, resolución DNS hacia el destino correcto y un certificado TLS válido. `api.cotasperu.com` es solo un ejemplo de referencia hasta que se confirme la disponibilidad y configuración del dominio. No se debe afirmar que DNS o certificados están configurados si no se han verificado.
11.6 Topología escalable
La arquitectura objetivo admite balanceador, varias instancias de API, Redis, workers, réplica de lectura y almacenamiento de respaldo. La incorporación se hará por etapas:
Primero, una topología de pruebas simple, segura y reproducible.
Después, medición de rendimiento y puntos de saturación.
Luego, incorporación de componentes que resuelvan un problema demostrado.
Finalmente, validación de escalado y recuperación antes de depender de ellos en producción.
No se añadirá CDN en la primera versión. No se recomienda un segundo VPS por defecto.
12. Calidad, pruebas y criterios de terminado
Cada funcionalidad se considera terminada únicamente cuando:
Cumple los requisitos y criterios de aceptación.
Está integrada con contratos reales.
Supera las pruebas correspondientes.
Gestiona los errores previsibles.
No expone secretos ni introduce vulnerabilidades evidentes.
Está documentada cuando corresponde.
Puede ejecutarse y verificarse siguiendo instrucciones reproducibles.
Sus limitaciones conocidas están registradas.
Existe evidencia verificable del resultado.
Tipos de prueba
Tipo	Objetivo
Unitarias	Verificar reglas de negocio y módulos aislados
Contrato API	Comprobar esquemas, errores y compatibilidad entre clientes y backend
Integración	Verificar adaptadores, persistencia y sistemas externos
Funcionales	Probar autenticación, catálogo, permisos y administración
E2E	Ejecutar el flujo completo en dispositivos y fuentes reales
Seguridad	Probar autenticación, autorización, exposición, configuración y secretos
Rendimiento	Medir latencia, tasa de error y estabilidad bajo carga controlada
Regresión	Verificar que los cambios no rompan funcionalidades ya aprobadas
Recuperación	Probar manejo de fallos, respaldos y reversión
Las pruebas de carga se ejecutarán únicamente en entornos autorizados y con límites acordados. No se realizarán pruebas destructivas contra producción.
13. Metodología de trabajo y control del proyecto
El desarrollo se organiza en fases y sprints. Las fases pueden solaparse si los contratos están definidos, pero no se declara una etapa terminada sin evidencia.
Para cada tarea se registrará:
Objetivo.
Responsable.
Prioridad.
Dependencias.
Archivos/componentes afectados.
Criterios de aceptación.
Pruebas requeridas.
Evidencias.
Estado.
Riesgos y siguientes acciones.
Estados sugeridos:
Pendiente.
En curso.
Bloqueado.
En revisión.
En pruebas.
Aprobado.
Cerrado.
Las decisiones que cambien el alcance, costos, seguridad o arquitectura requieren una justificación y aprobación. Los bloqueos de infraestructura o de interfaces deben documentarse, no ocultarse mediante simulaciones.
14. Plan maestro por sprints
Sprint 0 — Levantamiento y cierre de decisiones base
Objetivo: conocer el estado real del repositorio, servidor, servicios, tecnologías y requisitos.
Tareas:
Recopilar requisitos y alcance.
Inspeccionar el repositorio existente.
Inventariar el servidor de pruebas y sus servicios.
Verificar versiones e interfaces de XUI/Xprom, Astra y VOD.
Confirmar dispositivos y versiones objetivo.
Comparar stack propuesto con el estado real.
Registrar riesgos y bloqueos.
Elaborar arquitectura preliminar.
Definir estrategia de pruebas.
Preparar backlog de Sprint 1.
Entregables: inventario técnico, registro de decisiones, matriz de riesgos, arquitectura preliminar y backlog.
Criterio de aceptación: el equipo conoce qué existe, qué falta y qué depende de validación. No se inventan endpoints ni se realizan cambios críticos.
Sprint 1 — Arquitectura y contratos
Objetivo: cerrar límites, datos, API, seguridad y flujos antes de construir componentes interdependientes.
Tareas:
Revisar resultados de Sprint 0.
Definir arquitectura de componentes.
Definir módulos y responsabilidades del backend.
Documentar contratos API con OpenAPI.
Definir modelo de datos y fuentes de verdad.
Definir autenticación, sesiones y permisos.
Diseñar catálogo y autorización de reproducción.
Definir seguridad y amenazas principales.
Definir entornos, configuración y despliegue.
Preparar plan de pruebas.
Priorizar backlog de Sprint 2.
Entregables: arquitectura aprobable, contratos API, modelo de datos, flujos, controles de seguridad y backlog.
Criterio de aceptación: los contratos y responsabilidades permiten trabajar en backend y clientes sin suposiciones incompatibles.
Sprint 2 — Backend e infraestructura base
Objetivo: disponer de una base ejecutable, segura y comprobable en el entorno de pruebas.
Tareas:
Preparar repositorio y aplicación NestJS.
Configurar variables y secretos por entorno.
Configurar PostgreSQL/Prisma si se aprueba su uso.
Implementar validación de entradas y errores uniformes.
Implementar autenticación inicial según fuente de verdad aprobada.
Publicar documentación OpenAPI.
Añadir health checks y logs básicos.
Implementar pruebas automatizadas.
Desplegar al servidor de pruebas.
Preparar backlog de Sprint 3.
Entregables: backend base, configuración, migraciones aprobadas, documentación API y pruebas.
Criterio de aceptación: el backend se ejecuta en pruebas, sus controles básicos funcionan y el despliegue es reproducible. No se modifica el servidor principal.
Sprint 3 — Integraciones de streaming
Objetivo: integrar el backend con las interfaces verificadas de XUI/Xprom, Astra y VOD.
Tareas:
Confirmar las interfaces reales disponibles.
Implementar adaptador de XUI/Xprom cuando sea posible.
Implementar adaptadores de Astra/VOD si son necesarios y están soportados.
Normalizar catálogo y metadatos.
Implementar autorización de reproducción.
Gestionar timeouts, errores y disponibilidad.
Aplicar controles de seguridad a las integraciones.
Ejecutar pruebas de contrato, integración y E2E.
Documentar interfaces y limitaciones.
Preparar backlog de Sprint 4.
Entregables: adaptadores, catálogo integrado, flujo de autorización, pruebas y documentación.
Criterio de aceptación: la integración se valida con interfaces y contenido de prueba reales. Si una interfaz no existe o no está autorizada, el bloqueo se documenta.
Sprint 4 — Cliente Google TV / Android TV
Objetivo: construir el cliente prioritario y completar el primer flujo vertical.
Tareas:
Configurar proyecto Android.
Definir arquitectura del cliente.
Implementar UI para TV y control remoto.
Implementar autenticación.
Implementar catálogo.
Integrar Media3/ExoPlayer con el mecanismo confirmado.
Manejar errores, sesiones y reconexión.
Ejecutar pruebas unitarias y funcionales.
Generar build y documentar instalación.
Preparar backlog de Sprint 5.
Entregables: cliente Android TV, build de prueba, pruebas y guía.
Criterio de aceptación: autenticación, catálogo y reproducción real funcionan en un dispositivo objetivo disponible. Una compilación o un video simulado no equivale a integración completa.
Sprint 5 — Cliente Samsung Tizen
Objetivo: desarrollar y validar el cliente para los modelos Samsung definidos.
Tareas:
Confirmar versiones/modelos Tizen.
Crear/configurar proyecto.
Separar UI, API y reproducción.
Implementar navegación por control remoto.
Integrar autenticación.
Integrar catálogo.
Implementar reproducción con AVPlay cuando sea compatible.
Preparar certificado, firma y paquete.
Ejecutar pruebas en emulador y televisor físico cuando sea posible.
Documentar limitaciones y preparar Sprint 6.
Entregables: código Tizen, paquete de prueba, matriz de compatibilidad y evidencias.
Criterio de aceptación: se declara compatibilidad solo con los modelos/versiones probados; la reproducción real debe verificarse cuando la fuente y el dispositivo lo permitan.
Sprint 6 — Panel administrativo interno
Objetivo: implementar una interfaz de operación para las funciones aprobadas, con acceso restringido.
Tareas:
Confirmar módulos, acciones, roles y endpoints.
Configurar React/TypeScript/Vite.
Implementar estructura de interfaz.
Integrar autenticación.
Aplicar autorización por roles en coordinación con backend.
Implementar resumen operativo con datos reales.
Implementar módulos aprobados.
Centralizar errores y trazabilidad.
Ejecutar pruebas funcionales y de permisos.
Desplegar en pruebas y documentar.
Entregables: panel, matriz de roles, pruebas, evidencia de restricción y guía.
Criterio de aceptación: no hay acceso directo a base de datos, los permisos se verifican en backend y el panel permanece privado.
Sprint 7 — Validación integral
Objetivo: validar el sistema completo y determinar si está listo para producción.
Tareas:
Consolidar requisitos y trazabilidad.
Preparar entorno de validación.
Ejecutar pruebas E2E.
Validar contratos e integraciones.
Ejecutar regresión de clientes.
Ejecutar pruebas de seguridad.
Evaluar rendimiento y estabilidad.
Corregir defectos prioritarios.
Elaborar informe de calidad y decisión de salida.
Preparar plan de despliegue y reversión.
Entregables: matriz de pruebas, informes de seguridad/rendimiento, defectos y recomendación de salida.
Criterio de aceptación: riesgos residuales y bloqueos están documentados; no se considera listo si existen defectos críticos sin resolver o integraciones críticas sin validar.
Sprint 8 — Despliegue y estabilización
Objetivo: desplegar de forma controlada, verificar la operación y entregar procedimientos.
Tareas:
Revisar aprobación de salida.
Congelar versiones y artefactos.
Preparar entorno principal.
Verificar respaldos y plan de reversión.
Desplegar backend según procedimiento aprobado.
Desplegar panel y comprobar acceso privado.
Distribuir clientes conforme al proceso de cada plataforma.
Ejecutar pruebas de humo.
Monitorear y estabilizar.
Entregar documentación y acta de cierre.
Entregables: registros de despliegue, pruebas de humo, monitoreo, plan de reversión y guía operativa.
Criterio de aceptación: la versión desplegada corresponde a la validada, los servicios críticos están comprobados y existe aceptación formal. Si se detecta un riesgo grave, se detiene o revierte el cambio.
15. Requisitos no funcionales
Seguridad
API por HTTPS cuando sea accesible a través de redes no confiables.
Base de datos y panel administrativo no expuestos públicamente.
Autorización en backend.
Gestión segura de secretos y sesiones.
Logs sin datos sensibles innecesarios.
Rendimiento
Definir umbrales de latencia, errores y concurrencia antes de las pruebas.
Medir consumo de recursos en el servidor real.
Diferenciar usuarios registrados, sesiones activas y reproducciones simultáneas.
Evitar que el backend transporte video.
Compatibilidad
Mantener una matriz de modelos y versiones probados.
Verificar protocolos y formatos de reproducción por dispositivo.
No afirmar compatibilidad universal por haber probado un solo modelo.
Mantenibilidad
Módulos con responsabilidades claras.
Contratos API documentados.
Configuración por entorno.
Pruebas automatizadas y manuales registradas.
Instrucciones reproducibles.
Recuperación
Respaldos independientes.
Restauración probada.
Procedimiento de rollback.
Registro de cambios y responsables.
Costos
Reutilizar infraestructura corporativa.
No introducir CDN en la primera versión.
No comprar otro VPS como decisión predeterminada.
Incorporar Redis, workers, réplica y autoescalado solo si la necesidad está demostrada.
16. Observabilidad y soporte
Métricas mínimas:
Estado de salud del backend.
Latencia y tasa de errores de la API.
Uso de CPU, RAM, almacenamiento y red.
Disponibilidad de PostgreSQL.
Errores y latencia de integraciones externas.
Fallos de autenticación/autorización.
Incidencias de reproducción reportadas por los clientes cuando puedan medirse.
Usuarios registrados, sesiones activas y reproducciones simultáneas como métricas distintas.
Logs:
Estructurados y con fecha/hora.
Identificador de correlación cuando sea posible.
Sin contraseñas, tokens completos o URLs sensibles.
Acceso restringido y retención definida.
El nivel de observabilidad debe comenzar con lo necesario para operar y crecer según las necesidades medidas.
17. Riesgos principales
Riesgo	Consecuencia	Mitigación
Interfaces de XUI/Astra/VOD desconocidas	Bloqueo de integración	Inspección técnica y prueba de contrato antes de programar
Fuente de verdad de usuarios no definida	Duplicidad e inconsistencias	Decidir quién administra identidad, permisos y suscripciones
Diferencias de reproducción entre plataformas	Contenido no reproducible en ciertos modelos	Verificar protocolo/formato y probar dispositivos reales
Capacidad insuficiente del servidor	Degradación de servicios	Medir carga actual y concurrencia esperada
Exposición del panel o base de datos	Compromiso de sistemas	Restricción de red, mínimo privilegio y pruebas negativas
Credenciales en cliente o logs	Acceso no autorizado	Secretos solo en backend, revisión de artefactos y logs
Uso de datos simulados como si fueran reales	Falsa aceptación de funcionalidades	Etiquetar simulaciones y exigir pruebas reales
Añadir infraestructura prematuramente	Más costo y operación	Justificar cada componente con requisitos o mediciones
Despliegue sin reversión	Interrupción prolongada	Respaldo, checklist y rollback antes del cambio
Alcance móvil no confirmado	Desviación de plazo	Confirmar si la aplicación móvil pertenece al MVP o a una fase posterior
Autoescalado o réplica sin pruebas	Complejidad y riesgo operacional	Implementar solo después de validar requisitos y procedimiento
18. Decisiones pendientes y responsables
Estas decisiones deben cerrarse durante Sprint 0 y Sprint 1:
Decisión pendiente	Evidencia necesaria	Responsable sugerido
Versión y capacidades de XUI/Xprom	Inspección autorizada y documentación disponible	Infraestructura / arquitectura
Interfaces de Astra	Configuración, documentación y prueba controlada	Infraestructura / streaming
Interfaces de VOD	Protocolo, autenticación y prueba real	Infraestructura / streaming
Fuente de verdad de usuarios y suscripciones	Proceso operativo y contratos disponibles	Producto / infraestructura
Modelos Android TV y Tizen objetivo	Lista de dispositivos y pruebas	Producto / desarrollo
Inclusión de móvil en MVP	Aprobación de alcance	Responsable del proyecto
Necesidad de Redis y cola de tareas	Casos asíncronos, volumen y mediciones	Arquitectura / desarrollo
Necesidad de réplica PostgreSQL	Carga de consultas y objetivos de disponibilidad	Infraestructura / DBA
WAF/API Gateway/balanceador disponible	Inventario de infraestructura y requisitos de exposición	Infraestructura / seguridad
Dominio y certificado de API	DNS y titularidad verificados	Infraestructura
Roles del panel	Matriz de permisos aprobada	Producto / operaciones
Concurrencia esperada	Estimación de uso real	Producto / operaciones
Política de backup y retención	Requisitos de recuperación y almacenamiento	Infraestructura
Proceso de publicación de apps	Requisitos de firma, tienda y dispositivos	Desarrollo / responsable de producto
19. Entregables globales del proyecto
Informe maestro aprobado.
Documento de arquitectura y decisiones técnicas.
Repositorio con código fuente organizado y versionado.
Contratos de API/OpenAPI.
Modelo de datos y migraciones.
Adaptadores e integración documentada con los sistemas existentes.
Cliente Android TV y sus artefactos de prueba.
Cliente Samsung Tizen y sus artefactos de prueba.
Panel administrativo interno.
Pruebas automatizadas y matrices de pruebas manuales.
Informes de seguridad, rendimiento y compatibilidad.
Guía de configuración por entorno.
Guía de despliegue, verificación y reversión.
Procedimientos de respaldo y restauración.
Registro de defectos, decisiones y riesgos residuales.
Acta de aceptación y cierre.
Los entregables de TV Box y aplicación móvil se incorporarán al listado de compromiso de entrega solo cuando su alcance y prioridad estén aprobados.
20. Criterios globales de aceptación
El proyecto no se considera completo hasta verificar:
[ ] El alcance final distingue MVP, primera versión ampliada y arquitectura futura.
[ ] La arquitectura y el stack se contrastaron con el repositorio y la infraestructura real.
[ ] Los contratos API están documentados y son consistentes entre clientes.
[ ] La autenticación y autorización funcionan en el backend.
[ ] El catálogo proviene de una fuente verificada y respeta permisos.
[ ] La reproducción real funciona con contenido autorizado en los dispositivos objetivo aprobados.
[ ] Las integraciones externas tienen pruebas y documentación.
[ ] No se exponen base de datos, panel administrativo ni credenciales internas.
[ ] Los errores previsibles se gestionan de forma controlada.
[ ] Las pruebas críticas tienen evidencia.
[ ] Las limitaciones de compatibilidad están documentadas.
[ ] La capacidad se evalúa con métricas y concurrencia realista.
[ ] Los respaldos y la reversión están documentados y verificados.
[ ] La versión de producción coincide con la versión validada.
[ ] Existe aprobación formal de los responsables.
[ ] La documentación operativa está entregada.
21. Forma de trabajo del desarrollador
El desarrollador debe trabajar con estas reglas:
Inspeccionar primero el repositorio y el entorno real; no reescribir ni duplicar componentes sin revisar lo existente.
Diferenciar decisiones confirmadas, propuestas y pendientes.
No inventar endpoints, variables, credenciales, tablas o servicios.
No afirmar que algo está probado, integrado o desplegado sin evidencia.
Implementar por tareas pequeñas, con criterios de aceptación.
Documentar archivos modificados, comandos de ejecución y resultados esperados.
Ejecutar pruebas relevantes antes de declarar una tarea terminada.
Registrar bloqueos y solicitar la información o acceso estrictamente necesarios.
No ejecutar cambios destructivos ni tocar servicios críticos sin aprobación.
Mantener la documentación y el backlog sincronizados con el estado real.
No cambiar unilateralmente el alcance ni añadir infraestructura sin justificar costo, beneficio y riesgo.
Separar resultados simulados de resultados obtenidos con servicios reales.
22. Conclusión
El proyecto se plantea como una plataforma multiplataforma apoyada en la infraestructura audiovisual existente de COTAS PERU S.A.C. La API central controlará la lógica de negocio y la autorización, mientras que los clientes de TV reproducirán el contenido directamente desde la infraestructura de streaming autorizada.
La prioridad de desarrollo es completar y validar primero el flujo vertical de Google TV / Android TV. Después se incorporarán el cliente Tizen y el panel administrativo interno, seguidos de validación integral, despliegue controlado y estabilización. Los clientes móviles y otros componentes de escalabilidad representados en la arquitectura completa deberán planificarse según el alcance aprobado y las necesidades verificadas.
La arquitectura escalable incluye opciones de balanceo, múltiples instancias, Redis, workers, réplica de lectura, observabilidad y respaldos, pero la incorporación debe ser progresiva y justificada. La seguridad, la reutilización de infraestructura, el control de costos, la verificación de integraciones y la trazabilidad de pruebas son condiciones transversales.
Estado del informe: listo como documento maestro de referencia para el equipo; requiere revisión y aprobación de alcance, y validación técnica de las decisiones pendientes antes de considerarse arquitectura definitiva.
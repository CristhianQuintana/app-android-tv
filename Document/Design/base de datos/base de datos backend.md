INFORME TÉCNICO DE DISEÑO DE BASE DE DATOS
Backend de la Plataforma IPTV — COTAS PERU S.A.C.
Tipo de documento: Especificación técnica para diseño e implementación  
Proyecto: Plataforma IPTV multiplataforma COTAS  
Backend: NestJS + TypeScript  
Base de datos: PostgreSQL  
ORM: Prisma 6  
Arquitectura: Monolito modular con separación por dominios  
API: REST, prefijo `/api/v1`  
Estado: Propuesta técnica para revisión y aprobación  
Prioridad: Alta  
Objetivo: Definir un modelo de datos seguro, mantenible, escalable y preparado para la evolución de la plataforma.
---
1. RESUMEN EJECUTIVO
El presente documento establece las especificaciones para diseñar, revisar e implementar la base de datos del backend de la plataforma IPTV de COTAS PERU S.A.C.
La base de datos debe permitir administrar las cuentas de los usuarios, los roles y permisos, los dispositivos, las sesiones autenticadas, la vinculación con las líneas IPTV, las operaciones de seguridad y la auditoría de las acciones relevantes.
La solución utilizará PostgreSQL como sistema de persistencia y Prisma como herramienta de modelado, acceso a datos y gestión de migraciones.
La plataforma se integrará con XUI.one mediante un adaptador independiente del resto del dominio. XUI continuará siendo la fuente de los catálogos IPTV y el responsable de servir los manifiestos y segmentos de video. PostgreSQL almacenará los datos propios de COTAS, sin duplicar innecesariamente la información que ya administra XUI.
El diseño deberá priorizar:
Integridad y consistencia de los datos.
Seguridad de las cuentas y credenciales.
Separación entre identidad de la aplicación y autenticación ante XUI.
Consultas eficientes mediante índices justificados.
Relaciones explícitas y restricciones de integridad.
Evolución mediante migraciones versionadas.
Compatibilidad con Android, Android TV y otros clientes autorizados.
Capacidad de incorporar nuevas funcionalidades sin rediseñar toda la base de datos.
Observabilidad, auditoría y mantenimiento operativo.
Condición fundamental: antes de modificar el esquema existente, el desarrollador deberá inspeccionar `prisma/schema.prisma`, las migraciones aplicadas y la base de datos actual. Este informe define el modelo objetivo, pero no autoriza a recrear una base de datos existente ni a eliminar datos.
2. CONTEXTO Y ALCANCE
2.1. Contexto técnico
El proyecto cuenta con un backend NestJS y una integración con XUI que se encuentra en proceso de desarrollo.
El cierre del Sprint 0 confirmó consultas autenticadas a XUI desde Windows y Ubuntu, acceso a categorías y contenidos, consultas EPG y reproducción de un canal de prueba mediante VLC.
La validación de Sprint 0 no significa que toda la plataforma de usuarios, dispositivos y sesiones esté implementada o aprobada. El diseño de estas funcionalidades forma parte de la presente especificación.
2.2. Alcance funcional
La base de datos deberá cubrir los siguientes dominios:
Identidad y cuentas de usuario.
Roles y permisos.
Sesiones y autenticación.
Dispositivos registrados.
Autenticación multifactor.
Vinculación con cuentas o líneas IPTV.
Auditoría de operaciones.
Verificación y recuperación de acceso.
Registro de sesiones de reproducción, cuando se apruebe su alcance.
Caché persistente de catálogo y EPG, únicamente si existe una necesidad técnica comprobada.
2.3. Fuera del alcance inicial
No se implementarán de forma preventiva:
Un sistema de facturación o pagos.
Un motor propio de suscripciones comerciales.
Un sistema de DRM.
Un servicio de almacenamiento de videos.
Un proxy de video basado en PostgreSQL.
Una réplica completa y permanente del catálogo de XUI.
Un sistema de analítica avanzada.
Una plataforma de notificaciones independiente.
Estas funcionalidades podrán incorporarse mediante nuevas migraciones cuando exista un requisito aprobado.
3. PRINCIPIOS DE DISEÑO
3.1. Separación por dominios
Las entidades deberán organizarse de acuerdo con su responsabilidad funcional.
La identidad de un usuario de la aplicación no debe confundirse con una cuenta o línea de XUI. De igual forma, un dispositivo no es una sesión y una sesión autenticada no equivale a una sesión de reproducción.
Esta separación permite aplicar reglas de seguridad diferentes y evolucionar cada módulo sin generar dependencias innecesarias.
3.2. Fuente única de verdad
PostgreSQL será la fuente de verdad para los datos propios de la plataforma COTAS, tales como:
Usuarios de la aplicación.
Roles y permisos.
Dispositivos registrados.
Sesiones y revocaciones.
Asignaciones de líneas IPTV.
Registros de auditoría.
XUI será la fuente externa para los catálogos y datos IPTV que actualmente administra.
No se deberán copiar canales, películas, categorías ni programas EPG a PostgreSQL sin justificar la necesidad de caché, búsqueda local, disponibilidad degradada u otro requisito específico.
3.3. Identificadores internos
Se recomienda utilizar UUID como identificador interno de las entidades principales.
Los identificadores externos de XUI deben almacenarse como referencias independientes, preferentemente cadenas cuando el contrato externo no garantice un tipo estable.
No se utilizará un identificador de XUI como clave primaria de una entidad interna.
3.4. Fechas y zonas horarias
Todos los campos temporales deberán utilizar `TIMESTAMPTZ` en PostgreSQL y su tipo equivalente compatible en Prisma.
Se almacenarán las fechas en UTC. La conversión a la zona horaria del cliente se realizará en la capa de presentación o aplicación.
Los campos de expiración, revocación y última actividad deberán tener una semántica definida y documentada.
3.5. Integridad de los datos
Las relaciones deberán definirse mediante claves foráneas reales.
Se establecerán restricciones `UNIQUE`, `NOT NULL`, `CHECK` y reglas de eliminación según corresponda.
No se dependerá exclusivamente de validaciones en el frontend o en los servicios NestJS para mantener la integridad.
3.6. Evolución controlada
Todos los cambios del esquema se realizarán mediante migraciones Prisma versionadas.
No se utilizará `prisma db push` como sustituto de las migraciones en entornos compartidos o productivos.
Las migraciones deberán revisarse antes de aplicarse, especialmente si modifican claves, eliminan columnas, alteran restricciones o transforman datos existentes.
4. MODELO LÓGICO DE DATOS
Se proponen once tablas para el núcleo funcional, con tablas adicionales condicionadas al alcance aprobado.
4.1. Módulo de identidad
Tabla 1. `users`
Propósito: almacenar las cuentas de usuario de la aplicación COTAS.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`email`	VARCHAR(254)	No	Correo electrónico normalizado
`phone`	VARCHAR(20)	No	Teléfono normalizado en formato internacional
`username`	VARCHAR(100)	No	Nombre de usuario normalizado
`password_hash`	VARCHAR(255)	Según autenticación	Hash de contraseña, nunca texto plano
`status`	VARCHAR(20) o ENUM	Sí	Estado de la cuenta
`email_verified_at`	TIMESTAMPTZ	No	Fecha de verificación del correo
`phone_verified_at`	TIMESTAMPTZ	No	Fecha de verificación del teléfono
`last_login_at`	TIMESTAMPTZ	No	Último inicio de sesión exitoso
`created_at`	TIMESTAMPTZ	Sí	Fecha de creación
`updated_at`	TIMESTAMPTZ	Sí	Fecha de actualización
`deleted_at`	TIMESTAMPTZ	No	Fecha de baja lógica, si se aplica
Estados iniciales propuestos:
`PENDING`
`ACTIVE`
`SUSPENDED`
`DISABLED`
Reglas de negocio
La cuenta deberá tener al menos un identificador de acceso válido según el método de autenticación aprobado.
El correo, teléfono o nombre de usuario deberán normalizarse antes de aplicar las restricciones de unicidad.
No se permitirá que dos cuentas activas compartan un mismo identificador de acceso que deba ser único.
Las restricciones de unicidad deben seguir funcionando correctamente cuando un campo opcional sea nulo.
Las contraseñas deberán procesarse con un algoritmo de hash seguro para contraseñas, como Argon2id, utilizando parámetros adecuados al entorno.
El hash de contraseña nunca se devolverá mediante DTO públicos ni respuestas de API.
La baja lógica solo se utilizará cuando exista un requisito de negocio que la justifique. No debe sustituir indefinidamente una política de conservación y eliminación de datos.
Las operaciones de suspensión, desactivación y eliminación deberán revocar las sesiones que corresponda.
Decisión pendiente: confirmar si la aplicación permitirá acceso mediante correo, teléfono, nombre de usuario o una combinación de estos.
Tabla 2. `roles`
Propósito: definir los perfiles de autorización.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`code`	VARCHAR(50)	Sí	Código único del rol
`name`	VARCHAR(100)	Sí	Nombre legible
`description`	TEXT	No	Descripción funcional
`created_at`	TIMESTAMPTZ	Sí	Fecha de creación
Roles iniciales que se deben evaluar:
`CUSTOMER`
`OPERATOR`
`ADMIN`
Estos nombres son una propuesta y no representan una decisión de negocio ya aprobada.
Los códigos de rol deberán ser estables y únicos. El nombre visible podrá modificarse sin cambiar la identidad del rol.
Tabla 3. `permissions`
Propósito: definir acciones autorizadas dentro de la plataforma.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`code`	VARCHAR(100)	Sí	Código único del permiso
`description`	TEXT	No	Descripción funcional
`created_at`	TIMESTAMPTZ	Sí	Fecha de creación
Ejemplos:
`catalog.read`
`playback.authorize`
`users.read`
`users.manage`
`devices.manage`
`audit.read`
Los permisos definitivos deben corresponder a operaciones reales de la API.
Tabla 4. `user_roles`
Propósito: establecer la relación muchos-a-muchos entre usuarios y roles.
Campo	Tipo	Obligatorio
`user_id`	UUID	Sí
`role_id`	UUID	Sí
`assigned_at`	TIMESTAMPTZ	Sí
`assigned_by`	UUID	No
Restricciones:
Clave primaria compuesta: (`user_id`, `role_id`).
`user_id` referencia a `users.id`.
`role_id` referencia a `roles.id`.
`assigned_by` referencia a `users.id`, cuando exista un usuario asignador.
La asignación debe realizarse mediante servicios autorizados.
Las asignaciones administrativas deberán generar auditoría.
Tabla 5. `role_permissions`
Propósito: asociar permisos con roles.
Campo	Tipo	Obligatorio
`role_id`	UUID	Sí
`permission_id`	UUID	Sí
`created_at`	TIMESTAMPTZ	Sí
Restricciones:
Clave primaria compuesta: (`role_id`, `permission_id`).
Clave foránea a `roles.id`.
Clave foránea a `permissions.id`.
La autorización se realizará en el backend. Ocultar botones en el frontend no constituye un mecanismo de seguridad.
4.2. Módulo de sesiones y dispositivos
Tabla 6. `user_sessions`
Propósito: controlar las sesiones autenticadas y permitir su revocación.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Identificador de sesión
`user_id`	UUID	Sí	Usuario propietario
`device_id`	UUID	No	Dispositivo asociado
`refresh_token_hash`	VARCHAR(255)	Según estrategia	Hash del refresh token
`ip_address`	INET	No	IP observada
`user_agent`	TEXT	No	Agente del cliente, limitado
`created_at`	TIMESTAMPTZ	Sí	Creación de la sesión
`last_used_at`	TIMESTAMPTZ	No	Último uso
`expires_at`	TIMESTAMPTZ	Sí	Expiración
`revoked_at`	TIMESTAMPTZ	No	Revocación
`revoke_reason`	VARCHAR(100)	No	Motivo de revocación
Reglas:
El backend no almacenará tokens de acceso en texto plano.
El almacenamiento de refresh tokens debe permitir revocación y, si se implementa rotación, detección de reutilización.
Los tokens deberán tener expiración.
El cierre de sesión debe invalidar la sesión en el servidor cuando el diseño de autenticación lo requiera.
La expiración de una sesión no debe depender exclusivamente del cliente.
Debe definirse un procedimiento de limpieza de sesiones expiradas.
El número máximo de sesiones activas por usuario se configurará como una regla de negocio, no mediante un límite arbitrario en el esquema.
Tabla 7. `devices`
Propósito: registrar los dispositivos asociados a las cuentas.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`user_id`	UUID	Sí	Usuario propietario
`device_identifier_hash`	VARCHAR(255)	No	Identificador seudonimizado
`device_name`	VARCHAR(100)	No	Nombre descriptivo
`platform`	VARCHAR(30)	Sí	Plataforma controlada
`app_version`	VARCHAR(30)	No	Versión de la aplicación
`status`	VARCHAR(20)	Sí	Estado del dispositivo
`first_seen_at`	TIMESTAMPTZ	Sí	Primer registro
`last_seen_at`	TIMESTAMPTZ	No	Última actividad
`revoked_at`	TIMESTAMPTZ	No	Fecha de revocación
Estados propuestos:
`ACTIVE`
`REVOKED`
`BLOCKED`
Plataformas iniciales por evaluar:
`ANDROID`
`ANDROID_TV`
`WEB`
`OTHER`
Reglas:
Un dispositivo no se considerará una prueba infalible de identidad.
Los identificadores deben minimizar el uso de datos persistentes del hardware.
El dispositivo podrá revocarse sin eliminar la cuenta.
Se deberá definir qué sucede con las sesiones existentes cuando un dispositivo es revocado.
Si se establece un límite de dispositivos, deberá validarse transaccionalmente en el backend.
Tabla 8. `user_mfa_methods`
Propósito: almacenar los métodos de autenticación multifactor asociados a una cuenta.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`user_id`	UUID	Sí	Usuario propietario
`method`	VARCHAR(20)	Sí	Tipo de método
`secret_encrypted`	TEXT	Según método	Secreto cifrado, por ejemplo TOTP
`status`	VARCHAR(20)	Sí	Estado del método
`confirmed_at`	TIMESTAMPTZ	No	Confirmación
`created_at`	TIMESTAMPTZ	Sí	Creación
`last_used_at`	TIMESTAMPTZ	No	Último uso
Estados propuestos: `PENDING`, `ACTIVE`, `REVOKED`.
Reglas:
MFA debe ser obligatorio para las cuentas privilegiadas si así se establece en la política de seguridad.
Los secretos TOTP deben cifrarse mediante claves gestionadas fuera de la base de datos.
Las claves de cifrado no deben almacenarse junto al secreto cifrado.
Deben definirse límites de intentos y protección frente a repetición de códigos.
La recuperación de MFA requiere un procedimiento seguro y auditable.
4.3. Módulo de integración IPTV
Tabla 9. `iptv_accounts`
Propósito: vincular una cuenta de la aplicación con una cuenta o línea de un proveedor IPTV.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria

`user_id`	UUID	Sí	Usuario asociado
`provider`	VARCHAR(30)	Sí	Proveedor, inicialmente XUI
`external_account_ref`	VARCHAR(255)	No	Referencia externa no secreta
`username_encrypted`	TEXT	Solo si se requiere	Usuario externo cifrado
`password_encrypted`	TEXT	Solo si se requiere	Contraseña externa cifrada
`status`	VARCHAR(20)	Sí	Estado de la vinculación
`assigned_at`	TIMESTAMPTZ	Sí	Fecha de asignación
`expires_at`	TIMESTAMPTZ	No	Expiración conocida
`last_verified_at`	TIMESTAMPTZ	No	Última verificación
`created_at`	TIMESTAMPTZ	Sí	Creación
`updated_at`	TIMESTAMPTZ	Sí	Actualización
Estados propuestos:
`PENDING`
`ACTIVE`
`SUSPENDED`
`REVOKED`
Reglas:
No se utilizarán credenciales administrativas de XUI para autenticar clientes de la aplicación.
Si las credenciales externas deben persistirse, se cifrarán mediante un mecanismo de gestión de secretos y claves.
Si el diseño permite resolver una referencia externa segura sin almacenar contraseñas por usuario, se preferirá esa alternativa.
Las credenciales de la línea no se devolverán al frontend.
La consulta del catálogo y la autorización de reproducción deberán pasar por el backend.
El backend no deberá actuar como proxy de video.
La asignación de líneas deberá contar con reglas explícitas para evitar que un cliente acceda a la línea de otro.
Decisión de negocio pendiente: confirmar si cada usuario tendrá una única línea IPTV, varias líneas o una asignación administrada por otro sistema. La cardinalidad definitiva y sus restricciones dependen de esta decisión.
4.4. Módulo de auditoría
Tabla 10. `audit_logs`
Propósito: registrar operaciones relevantes para la seguridad, administración y diagnóstico.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`actor_user_id`	UUID	No	Usuario que ejecutó la acción
`action`	VARCHAR(100)	Sí	Acción ejecutada
`resource_type`	VARCHAR(100)	Sí	Tipo de recurso
`resource_id`	VARCHAR(100)	No	Identificador del recurso
`result`	VARCHAR(20)	Sí	Resultado de la operación
`ip_address`	INET	No	IP observada
`correlation_id`	VARCHAR(100)	No	Identificador de correlación
`metadata`	JSONB	No	Metadatos permitidos y saneados
`created_at`	TIMESTAMPTZ	Sí	Fecha del evento
Ejemplos de acciones:
`USER_CREATED`
`USER_SUSPENDED`
`ROLE_ASSIGNED`
`SESSION_REVOKED`
`DEVICE_BLOCKED`
`IPTV_ACCOUNT_ASSIGNED`
`PLAYBACK_AUTHORIZED`
`LOGIN_FAILED`
`MFA_ENABLED`
Reglas:
No guardar contraseñas, tokens, secretos TOTP ni URLs de reproducción con credenciales.
Limitar el tamaño y contenido de `metadata`.
El usuario de aplicación no deberá tener permisos de actualización o eliminación de registros de auditoría salvo una necesidad operativa formalmente aprobada.
La retención se definirá según la política de seguridad y obligaciones aplicables.
Se deberá limitar el acceso de lectura a personal autorizado.
Las operaciones sensibles deberán registrar actor, acción, resultado, fecha y correlación cuando estén disponibles.
La auditoría de negocio no sustituye los logs técnicos de NestJS. Ambos deben mantener propósitos diferenciados.
Tabla 11. `auth_challenges`
Propósito: administrar desafíos de verificación o recuperación de acceso.
Campo	Tipo PostgreSQL	Obligatorio	Descripción
`id`	UUID	Sí	Clave primaria
`user_id`	UUID	Sí	Usuario asociado
`purpose`	VARCHAR(30)	Sí	Finalidad del desafío
`token_hash`	VARCHAR(255)	Sí	Hash del token
`expires_at`	TIMESTAMPTZ	Sí	Fecha de expiración
`consumed_at`	TIMESTAMPTZ	No	Fecha de consumo
`created_at`	TIMESTAMPTZ	Sí	Creación
Finalidades iniciales por evaluar:
`EMAIL_VERIFICATION`
`PASSWORD_RESET`
Reglas:
Los tokens deben ser aleatorios, de un solo uso y con expiración.
Solo se almacenará el hash del token.
El consumo del token y la operación protegida deben coordinarse para evitar usos concurrentes.
Los desafíos expirados deberán limpiarse mediante una tarea controlada.
Si la recuperación se implementa mediante un proveedor externo, se evaluará si esta tabla sigue siendo necesaria.
5. TABLAS OPCIONALES Y EVOLUCIÓN
Las siguientes tablas no deben incorporarse al MVP sin confirmar sus casos de uso.
5.1. `playback_sessions`
Permitirá registrar solicitudes de reproducción y eventos de inicio, finalización o error.
Campos propuestos:
`id`: UUID.
`user_id`: UUID.
`device_id`: UUID, nullable.
`content_type`: tipo controlado, por ejemplo `LIVE` o `VOD`.
`external_stream_id`: VARCHAR(100).
`status`: estado controlado.
`requested_at`: TIMESTAMPTZ.
`started_at`: TIMESTAMPTZ, nullable.
`ended_at`: TIMESTAMPTZ, nullable.
`failure_code`: VARCHAR(100), nullable.
No se guardarán manifiestos ni segmentos de video. Las URLs que incluyan credenciales no deben persistirse.
Antes de implementarla se deberá definir qué eventos puede reportar realmente el reproductor. Autorizar una reproducción no demuestra por sí solo que el usuario haya visto el contenido.
5.2. Caché de catálogo
Si las consultas directas a XUI generan latencia o no satisfacen los requisitos de disponibilidad, se podrán evaluar:
`catalog_categories`
`id`: UUID.
`provider`: VARCHAR(30).
`external_id`: VARCHAR(100).
`content_type`: tipo controlado.
`name`: VARCHAR(255).
`last_synced_at`: TIMESTAMPTZ.
`catalog_items`
`id`: UUID.
`provider`: VARCHAR(30).
`external_id`: VARCHAR(100).
`content_type`: tipo controlado.
`category_external_id`: VARCHAR(100), nullable.
`name`: VARCHAR(255).
`description`: TEXT, nullable.
`logo_url`: TEXT, nullable.
`container_extension`: VARCHAR(20), nullable.
`is_active`: BOOLEAN.
`last_synced_at`: TIMESTAMPTZ.
`created_at`: TIMESTAMPTZ.
`updated_at`: TIMESTAMPTZ.
`epg_programs`
`id`: UUID.
`external_channel_id`: VARCHAR(100).
`title`: VARCHAR(500).
`description`: TEXT, nullable.
`starts_at`: TIMESTAMPTZ.
`ends_at`: TIMESTAMPTZ.
`last_synced_at`: TIMESTAMPTZ.
Estas tablas necesitarían una estrategia de sincronización, manejo de elementos eliminados en origen, índices de búsqueda y limpieza de registros obsoletos. Los identificadores externos deberán ser únicos en combinación con el proveedor y el tipo de contenido cuando corresponda.
La caché no se debe implementar solo porque sea posible. Debe justificarse con métricas o requisitos de producto.
6. RELACIONES ENTRE TABLAS
El modelo relacional deberá contemplar las siguientes relaciones:
Relación	Cardinalidad	Regla
`users` → `user_roles`	1:N	Un usuario puede tener varios roles
`roles` → `user_roles`	1:N	Un rol puede asignarse a varios usuarios
`roles` → `role_permissions`	1:N	Un rol puede tener varios permisos
`permissions` → `role_permissions`	1:N	Un permiso puede pertenecer a varios roles
`users` → `user_sessions`	1:N	Un usuario puede tener varias sesiones
`users` → `devices`	1:N	Un usuario puede registrar varios dispositivos
`devices` → `user_sessions`	1:N	Una sesión puede asociarse a un dispositivo; el dispositivo puede tener varias sesiones históricas
`users` → `user_mfa_methods`	1:N	Un usuario puede registrar métodos MFA
`users` → `iptv_accounts`	1:N potencial	La cardinalidad definitiva depende de la política de asignación
`users` → `auth_challenges`	1:N	Un usuario puede tener desafíos de autenticación
`users` → `audit_logs`	1:N potencial	Un usuario puede generar eventos de auditoría
`users` → `playback_sessions`	1:N	Si se implementa el seguimiento de reproducción
`devices` → `playback_sessions`	1:N	Si se implementa el seguimiento de reproducción
Las claves foráneas deberán proteger la integridad. Las políticas de eliminación deben elegirse según el tipo de relación:
`RESTRICT` cuando eliminar un registro pueda comprometer auditoría o referencias importantes.
`CASCADE` únicamente para dependencias cuya eliminación conjunta sea intencional.
`SET NULL` cuando deba conservarse un registro histórico sin mantener una referencia obligatoria a una entidad eliminada.
No se deberá aplicar `CASCADE` indiscriminadamente.
7. ESTÁNDARES DE IMPLEMENTACIÓN EN PRISMA
7.1. Convenciones
El desarrollador deberá mantener convenciones uniformes:
Modelos Prisma en PascalCase singular, por ejemplo `User`.
Campos en camelCase, por ejemplo `createdAt`.
Tablas PostgreSQL en snake_case mediante `@@map`.
Columnas PostgreSQL en snake_case mediante `@map`.
UUID generados en el backend o mediante una estrategia compatible con Prisma.
Fechas de creación y actualización uniformes.
Enums para estados estables y controlados, evitando cadenas arbitrarias.
Tipos JSON solo cuando la estructura sea realmente variable.
Ejemplo orientativo:
```prisma
model User {
  id        String   @id @default(uuid()) @db.Uuid
  email     String?  @unique @db.VarChar(254)
  status    UserStatus @default(PENDING)
  createdAt DateTime @default(now()) @map("created_at") @db.Timestamptz(6)
  updatedAt DateTime @updatedAt @map("updated_at") @db.Timestamptz(6)

  @@map("users")
}
```
Este fragmento es ilustrativo, no un reemplazo completo del esquema. La unicidad de identificadores opcionales y su normalización deberán revisarse según la política de acceso y las capacidades de PostgreSQL/Prisma utilizadas.
7.2. Migraciones
Las migraciones deberán:
Estar versionadas en `prisma/migrations`.
Incluir los cambios mínimos necesarios.
Poder revisarse antes de ejecutarse.
Evitar pérdida accidental de datos.
Definir índices, claves foráneas y restricciones explícitamente.
Incluir transformaciones de datos cuando sean necesarias.
Probarse en una base de datos de desarrollo antes de producción.
Disponer de un plan de recuperación o restauración antes de cambios destructivos.
No se deben editar migraciones ya aplicadas en entornos compartidos para alterar retrospectivamente su historial.
7.3. Transacciones
Las operaciones que modifican varias entidades relacionadas deberán ejecutarse dentro de transacciones cuando la consistencia lo requiera.
Ejemplos:
Crear una cuenta y asignar sus roles iniciales.
Revocar una sesión y registrar la operación.
Cambiar la asignación de una línea IPTV.
Consumir un token de recuperación y completar la operación protegida.
No se deben mantener transacciones abiertas mientras se realizan solicitudes de red a XUI. Las llamadas externas deben quedar fuera de transacciones largas.
8. ÍNDICES Y OPTIMIZACIÓN
Los índices deberán responder a consultas reales y a las reglas de integridad.
Tabla	Índice recomendado	Finalidad
`users`	Unicidad normalizada de identificadores de acceso	Evitar duplicados
`user_roles`	PK compuesta (`user_id`, `role_id`)	Consultar roles y evitar duplicados
`role_permissions`	PK compuesta (`role_id`, `permission_id`)	Consultar permisos
`user_sessions`	(`user_id`, `expires_at`)	Buscar sesiones del usuario
`user_sessions`	Índice parcial de sesiones activas, si las consultas lo justifican	Filtrar sesiones vigentes
`devices`	(`user_id`, `status`)	Consultar dispositivos por usuario
`iptv_accounts`	(`user_id`, `status`)	Consultar asignaciones vigentes
`audit_logs`	(`created_at`)	Consultar por fecha y gestionar retención
`audit_logs`	(`actor_user_id`, `created_at`)	Consultar acciones por usuario
`audit_logs`	(`correlation_id`)	Correlacionar operaciones
`auth_challenges`	(`user_id`, `purpose`, `expires_at`)	Consultar desafíos vigentes
Reglas de optimización:
No crear índices para todas las columnas indiscriminadamente.
Evitar índices duplicados o que no aporten a consultas reales.
Revisar consultas frecuentes con `EXPLAIN ANALYZE`.
Utilizar paginación para listados grandes.
Preferir paginación por cursor para conjuntos de datos grandes y ordenamientos estables.
Evitar búsquedas sin límites sobre auditoría o historial.
Revisar la selectividad y el coste de los índices antes de agregarlos.
Medir el efecto de los índices en escrituras y actualizaciones.
Si la tabla de auditoría o las sesiones de reproducción alcanzan volúmenes elevados, se evaluará particionamiento por fecha. No se incorporará particionamiento preventivo sin una previsión de volumen o mediciones que lo justifiquen.
9. SEGURIDAD DE LOS DATOS
9.1. Contraseñas y tokens
Contraseñas de usuarios: almacenar hashes seguros, nunca texto plano ni cifrado reversible.
Tokens de recuperación: almacenar hashes y permitir un único consumo.
Refresh tokens: almacenar hashes o referencias seguras que permitan revocación.
Secretos TOTP: cifrar, porque se necesitan recuperar para verificar códigos.
Credenciales XUI persistidas: cifrar si su almacenamiento resulta imprescindible.
9.2. Separación de credenciales
Las credenciales de XUI deben mantenerse en variables de entorno o en un gestor de secretos. No deben aparecer en:
El código fuente.
Las migraciones.
Los registros de auditoría.
Las respuestas de la API.
El frontend.
Las capturas o informes compartidos.
Las credenciales de prueba deben rotarse si se determina que se expusieron fuera de los canales autorizados.
9.3. Acceso a PostgreSQL
La aplicación deberá utilizar un usuario de base de datos con privilegios mínimos.
No utilizar el superusuario PostgreSQL desde NestJS.
Restringir conexiones de base de datos a las redes y equipos autorizados.
Utilizar TLS para conexiones que atraviesen redes no confiables.
Gestionar las credenciales de base de datos fuera del repositorio.
Mantener respaldos protegidos y comprobar periódicamente su restauración.
Separar las credenciales de migración de las credenciales de ejecución cuando el despliegue lo requiera.
9.4. Privacidad y retención
Se debe definir cuánto tiempo conservar:
Sesiones expiradas.
Registros de auditoría.
Historial de reproducción, si existe.
Identificadores de dispositivos.
Direcciones IP y agentes de usuario.
Datos de verificación y recuperación.
No se deben conservar datos personales indefinidamente por conveniencia técnica. Las políticas de retención y eliminación deberán documentarse conforme a las obligaciones aplicables a COTAS.
10. ESTRATEGIA DE INTEGRACIÓN CON XUI
PostgreSQL no debe convertirse en una copia indiscriminada de XUI.
La capa `integrations/xui` será responsable de consultar el proveedor, normalizar sus respuestas y exponer modelos internos estables al resto del backend.
10.1. Datos que deben permanecer en XUI
En la primera versión, XUI seguirá siendo la fuente de:
Categorías de TV en vivo.
Canales de TV en vivo.
Categorías VOD.
Películas y otros contenidos soportados.
Información EPG proporcionada por el proveedor.
Referencias necesarias para construir URLs de reproducción autorizadas.
Estado externo de las líneas, según la respuesta de XUI.
10.2. Datos propios de COTAS
PostgreSQL administrará:
Identidad de la aplicación.
Roles y permisos.
Sesiones de aplicación.
Dispositivos.
Asignaciones y estados internos de cuentas IPTV.
Auditoría.
Desafíos de recuperación.
Historial de reproducción, si se aprueba.
10.3. Identificadores externos
Los identificadores externos deberán almacenarse separados de los UUID internos.
La aplicación no deberá asumir que un `stream_id` identifica globalmente el mismo contenido entre proveedores o entornos. Cuando se persista una referencia, deberá incluirse el proveedor y el tipo de contenido.
10.4. URLs de reproducción
Las URLs de reproducción no deberán tratarse como datos públicos ni almacenarse de forma permanente por defecto.
Cuando sea necesario devolver material de acceso al reproductor:
El backend debe comprobar que el usuario está autorizado.
Las URLs deben construirse o validarse exclusivamente en el backend.
Se deben aplicar controles de host permitidos.
No se deben aceptar URLs arbitrarias enviadas por el frontend.
No se deben registrar URLs completas con credenciales.
Se debe respetar la separación entre autorización y transporte de video.
La base de datos no debe almacenar manifiestos HLS ni segmentos de video.
11. ESTRATEGIA DE ESCALABILIDAD
11.1. Escalabilidad horizontal del backend
La aplicación deberá poder ejecutarse en más de una instancia sin depender de estado de sesión exclusivamente en memoria.
Las sesiones y revocaciones deben persistir en un mecanismo compartido. Los límites de autenticación que deban aplicarse globalmente requerirán almacenamiento compartido, como Redis, o un mecanismo distribuido equivalente.
La tabla `user_sessions` no sustituye por sí sola un sistema distribuido de rate limiting.
11.2. Escalabilidad de PostgreSQL
Se deberá:
Configurar un pool de conexiones compatible con el número de instancias.
Evitar consultas N+1.
Mantener transacciones breves.
Utilizar paginación.
Monitorizar conexiones, latencia y consultas lentas.
Medir el crecimiento de tablas de auditoría y sesiones.
Evaluar réplicas de lectura únicamente cuando la carga lo justifique.
Mantener backups y verificar su recuperación.
No se debe introducir particionamiento, múltiples bases de datos o microservicios sin una necesidad demostrada.
11.3. Caché
Redis podrá incorporarse para rate limiting distribuido, caché o coordinación de trabajos, si las necesidades lo justifican.
No debe utilizarse como sustituto indiscriminado de PostgreSQL ni como fuente única de verdad para cuentas y permisos.
11.4. Catálogo y EPG
Antes de persistir el catálogo, se deben medir:
Latencia de las consultas a XUI.
Frecuencia de cambios en los catálogos.
Volumen de consultas concurrentes.
Requisitos de disponibilidad cuando XUI no responde.
Necesidad de búsqueda local.
Tiempo máximo de antigüedad aceptable para la caché.
Si se implementa sincronización, deberá contar con marcas temporales, estrategia de actualización, tratamiento de elementos eliminados y métricas de errores.
12. RESPALDO Y RECUPERACIÓN
El desarrollador y el responsable de infraestructura deberán definir:
Frecuencia de backups.
Retención de copias.
Ubicación protegida de almacenamiento.
Cifrado de respaldos.
Procedimiento de restauración.
Objetivo de punto de recuperación (RPO).
Objetivo de tiempo de recuperación (RTO).
Responsable de verificar las restauraciones.
Las cifras de RPO y RTO deberán aprobarse de acuerdo con los requisitos operativos de COTAS; no se deben inventar valores sin validación.
Antes de migraciones destructivas, deberá existir una copia recuperable y un procedimiento de rollback o restauración.
13. PLAN DE IMPLEMENTACIÓN
La implementación deberá ejecutarse en fases controladas.
Fase 1. Auditoría del esquema existente
Revisar `prisma/schema.prisma`.
Revisar el historial de `prisma/migrations`.
Confirmar qué migraciones se encuentran aplicadas.
Verificar las tablas reales de PostgreSQL.
Identificar entidades duplicadas, campos redundantes y restricciones faltantes.
Comparar el esquema existente con este documento.
Elaborar una matriz de diferencias antes de modificar archivos.
Entregable: informe de situación actual y propuesta de cambios.
Fase 2. Cierre de decisiones funcionales
Confirmar con COTAS:
Identificadores de acceso permitidos.
Roles definitivos.
Método de asignación de líneas XUI.
Límite de dispositivos y sesiones.
Política de MFA.
Recuperación de acceso.
Retención de auditoría.
Necesidad de historial de reproducción.
Necesidad de caché de catálogo y EPG.
Entregable: decisiones aprobadas y reglas de negocio documentadas.
Fase 3. Diseño físico
Definir modelos Prisma.
Establecer tipos PostgreSQL.
Definir claves primarias y foráneas.
Definir restricciones de unicidad.
Añadir restricciones `CHECK` cuando correspondan.
Crear índices justificados.
Definir políticas de eliminación.
Revisar normalización y redundancia.
Verificar que el modelo soporte consultas previstas.
Entregable: `schema.prisma` revisado y diagrama entidad-relación.
Fase 4. Migraciones
Generar migraciones incrementales.
Revisar el SQL generado.
Aplicarlas en una base de desarrollo.
Verificar que no exista pérdida de datos.
Validar restricciones e índices.
Ejecutar pruebas con datos controlados.
Documentar el procedimiento de despliegue.
Entregable: migraciones versionadas y verificadas.
Fase 5. Implementación de repositorios y servicios
Implementar la persistencia mediante módulos NestJS y repositorios adecuados.
Las reglas de negocio deberán residir en los servicios de dominio o aplicación, no duplicarse entre controladores y repositorios.
El frontend no debe acceder directamente a PostgreSQL.
Fase 6. Pruebas y validación
Ejecutar pruebas unitarias, de integración y de extremo a extremo. Verificar restricciones, concurrencia, revocación, errores de base de datos y comportamiento de migraciones.
Fase 7. Documentación y entrega
Entregar:
Modelo de datos.
Diagrama entidad-relación.
`schema.prisma`.
Migraciones.
Matriz de decisiones.
Catálogo de índices y restricciones.
Política de backups.
Evidencias de pruebas.
Instrucciones de despliegue y recuperación.
14. PLAN DE PRUEBAS Y CRITERIOS DE ACEPTACIÓN
La base de datos no se considerará terminada únicamente porque Prisma pueda conectarse a PostgreSQL.
ID	Prueba	Criterio de aceptación
DB-01	Aplicación de migraciones	Las migraciones se aplican en una base limpia y en una base con el estado esperado
DB-02	Unicidad de identificadores	Se rechazan identificadores duplicados conforme a las reglas aprobadas
DB-03	Integridad referencial	No se admiten relaciones hacia registros inexistentes
DB-04	Roles y permisos	La asignación duplicada se rechaza y los permisos se consultan correctamente
DB-05	Contraseñas	No se almacena ninguna contraseña en texto plano
DB-06	Sesiones	La expiración y revocación se aplican correctamente
DB-07	Dispositivos	Los dispositivos revocados no pueden seguir utilizando sesiones que deban invalidarse
DB-08	MFA	Los secretos se cifran y los códigos no pueden reutilizarse indebidamente
DB-09	Asignación IPTV	Un usuario no puede acceder a una línea no autorizada
DB-10	Auditoría	Las acciones críticas generan eventos sin secretos
DB-11	Transacciones	Las operaciones multi-entidad no dejan estados parciales ante errores
DB-12	Rendimiento	Las consultas principales usan índices adecuados según el análisis de ejecución
DB-13	Retención	Los procedimientos de limpieza respetan las políticas aprobadas
DB-14	Restauración	Un backup puede restaurarse siguiendo el procedimiento documentado
DB-15	Integración XUI	Los fallos externos no corrompen los datos internos
DB-16	Seguridad	El usuario de ejecución de PostgreSQL opera con privilegios mínimos
Las pruebas de rendimiento deberán utilizar volúmenes de datos y niveles de concurrencia acordados. No se fijarán objetivos numéricos arbitrarios sin conocer el volumen esperado y los recursos disponibles.
15. REQUISITOS DE CALIDAD DEL CÓDIGO
El desarrollador deberá aplicar:
Principios SOLID cuando resulten apropiados.
Separación entre controladores, servicios, repositorios y modelos.
Validación de entradas en los límites de la aplicación.
DTO específicos para entradas y respuestas.
Manejo consistente de errores.
Transacciones para operaciones que requieran atomicidad.
Consultas parametrizadas mediante Prisma.
Evitar lógica de negocio en controladores.
Evitar acceso directo a Prisma desde múltiples módulos sin una política arquitectónica.
Evitar exponer modelos internos de persistencia en la API.
Pruebas automatizadas de las reglas de negocio.
Migraciones revisables y reproducibles.
Logs sin secretos ni información personal innecesaria.
No se deben introducir abstracciones genéricas o repositorios complejos que no aporten valor al diseño real.
16. DECISIONES PENDIENTES DE APROBACIÓN
Antes de cerrar el esquema definitivo, COTAS debe aprobar las siguientes decisiones:
ID	Decisión	Impacto
DEC-01	Correo, teléfono, usuario o combinación para iniciar sesión	Modelo `users` y autenticación
DEC-02	Una o varias líneas XUI por cliente	Cardinalidad y restricciones de `iptv_accounts`
DEC-03	Número máximo de dispositivos	Servicios de autorización y reglas de concurrencia
DEC-04	Número máximo de sesiones simultáneas	Política de sesiones y reproducción
DEC-05	MFA obligatorio para operadores y administradores	Política de acceso
DEC-06	Recuperación de acceso	Necesidad de `auth_challenges`
DEC-07	Historial de reproducción	Necesidad de `playback_sessions`
DEC-08	Caché persistente del catálogo	Necesidad de tablas de catálogo y EPG
DEC-09	Retención de auditoría	Volumen y eliminación de datos
DEC-10	RPO y RTO	Estrategia de backup y recuperación
Si una decisión no está aprobada, el desarrollador deberá documentar la suposición provisional y evitar convertirla en una restricción irreversible.
17. INSTRUCCIONES DIRECTAS PARA EL DESARROLLADOR
Se solicita desarrollar el modelo de base de datos siguiendo este documento y el siguiente orden:
Inspeccionar el esquema Prisma y las migraciones existentes.
Entregar una matriz de diferencias entre el esquema actual y el modelo propuesto.
Confirmar las decisiones funcionales pendientes.
Diseñar el esquema lógico y físico.
Actualizar `prisma/schema.prisma` sin sobrescribir cambios existentes.
Crear migraciones incrementales y revisar el SQL generado.
Implementar las restricciones, índices y relaciones aprobadas.
Desarrollar o adaptar los repositorios y servicios que consuman el modelo.
Ejecutar las pruebas de aceptación y entregar evidencias.
Documentar la restauración, las migraciones y la operación de la base de datos.
No se deben eliminar datos, recrear la base de datos ni aplicar cambios destructivos para hacer coincidir el esquema con esta propuesta.
La implementación deberá preservar las decisiones arquitectónicas existentes, mantener aislada la integración con XUI y evitar que la base de datos se convierta en un repositorio innecesario de información externa.
Criterio final de aceptación
El trabajo se considerará aceptado cuando el esquema aprobado esté implementado mediante migraciones reproducibles, las relaciones y restricciones funcionen, las pruebas estén documentadas, los datos sensibles estén protegidos y el procedimiento de despliegue y recuperación haya sido validado.
18. CONCLUSIÓN
La base de datos debe constituir una capa de persistencia fiable para la plataforma IPTV de COTAS, con un modelo de identidad separado de la integración con XUI y una estrategia de crecimiento basada en requisitos reales.
La propuesta establece un núcleo de identidad, autorización, sesiones, dispositivos, vinculación IPTV, auditoría y recuperación. Las funcionalidades de caché, reproducción, pagos e historial se incorporarán solo cuando el alcance esté aprobado y su necesidad técnica esté justificada.
La prioridad no es crear la mayor cantidad de tablas, sino diseñar un esquema consistente, seguro, verificable y preparado para evolucionar sin comprometer la integridad de los datos ni la operación del servicio.
19. IMPLEMENTACIÓN
Fecha: 2026-10-19. Migración `20261019220000_identity_model`. La migración anterior no se editó.
Matriz de diferencias
El esquema que estaba aplicado no correspondía a este informe. Era un prototipo local con `users.role` (`VIEWER`/`OPERATOR`), `password_hash` obligatorio, `sessions`, `recovery_codes` y `audit_events`. Solo contenía dos cuentas de desarrollo creadas por ese prototipo. Se reemplazó con una migración incremental. No se recreó el clúster ni se usó `db push`.
Informe	Implementación
`users`	Aplicada. Identificadores opcionales, `password_hash` nulo, estados `PENDING`/`ACTIVE`/`SUSPENDED`/`DISABLED`, borrado lógico
`roles`, `permissions`, `user_roles`, `role_permissions`	Aplicadas. Datos iniciales: `CUSTOMER`, `OPERATOR`, `ADMIN`
`user_sessions`	Aplicada
`devices`	Aplicada. Revocar un dispositivo revoca sus sesiones abiertas
`user_mfa_methods`	Aplicada. Secreto TOTP cifrado
`iptv_accounts`	Aplicada. Una línea activa por usuario y referencia única por proveedor
`audit_logs`	Aplicada. Reemplaza `audit_events`
`auth_challenges`	Aplicada. Recuperación MFA
`playback_sessions`, catálogo y EPG persistentes	No implementadas. Siguen fuera de alcance
Los índices únicos de `username`, `email` y `phone` son parciales: solo aplican cuando `deleted_at` es nulo. Prisma no los declara en el esquema; viven en el SQL de la migración.
Decisiones usadas
ID	Resolución de esta entrega	Alcance
DEC-01	El ingreso es el usuario y la contraseña de la línea XUI. Un usuario nuevo del panel puede entrar sin alta manual	Cerrada para el ingreso actual
DEC-02	Una línea XUI activa por usuario de la aplicación	Provisional
DEC-03	Sin máximo de dispositivos	Provisional
DEC-04	Sin máximo de sesiones simultáneas	Provisional
DEC-05	MFA obligatorio para `OPERATOR` y `ADMIN`. El cliente no lo usa	Provisional
DEC-06	Los códigos de recuperación son desafíos `MFA_RECOVERY`, de un solo uso y con vencimiento de un año	Provisional
DEC-07	Sin historial de reproducción	Sin tabla
DEC-08	Sin caché persistente de catálogo ni EPG	Sin tablas
DEC-09	Sin política de retención automática	Provisional
DEC-10	Sin RPO ni RTO medidos	Provisional
Ajustes respecto del informe
`user_sessions.family_id` y `user_sessions.replaced_by_id` permiten rotar el refresh token y revocar la familia si se reutiliza un token ya rotado.
`revoke_reason` incluye `ROTATED`, `REUSE` y `SECURITY`, además de cierre y revocación administrativa.
`iptv_accounts.password_encrypted` guarda la contraseña de la línea con AES-256-GCM. Hace falta después del login para armar la referencia de reproducción sin volver a pedirla y sin guardar la URL. La clave es la misma de cifrado MFA; conviene separarla cuando se apruebe un secreto propio.
La verificación de la línea ocurre en XUI antes de escribir. Un fallo de red no crea ni modifica la cuenta. Este panel responde HTTP 404 con HTML cuando la línea no es válida; ese caso se traduce a credenciales inválidas y no crea usuario. Un 401, 403, timeout o respuesta ilegible sigue siendo un fallo de la dependencia.
`XUI_USERNAME` y `XUI_PASSWORD` del entorno solo sirven para la sonda de disponibilidad. No autentican al cliente.
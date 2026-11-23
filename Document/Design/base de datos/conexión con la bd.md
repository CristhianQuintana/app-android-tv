INFORME TÉCNICO: CONEXIÓN DE LAS APLICACIONES DE TELEVISIÓN CON LA BASE DE DATOS
Empresa: COTAS PERU S.A.C.  
Proyecto: Plataforma IPTV multiplataforma  
Plataformas cliente: Google TV / Android TV y Samsung Smart TV / Tizen  
Backend: NestJS + TypeScript  
Base de datos: PostgreSQL  
ORM: Prisma 6  
API: REST, prefijo `/api/v1`  
Integración IPTV: XUI.one mediante el backend  
Fecha: 22 de octubre de 2026  
Estado: Propuesta técnica para revisión y validación de infraestructura
---
1. Resumen ejecutivo
La base de datos PostgreSQL ya se encuentra instalada en el servidor. Para conectar las aplicaciones de Google TV y Samsung Tizen no se debe abrir una conexión directa desde el televisor a PostgreSQL. La conexión debe realizarse a través del backend NestJS, que valida la identidad del usuario, aplica las reglas de autorización y consulta o actualiza los datos necesarios mediante Prisma.
Las dos aplicaciones utilizarán la misma API y el mismo modelo de datos. El backend será responsable de la comunicación con PostgreSQL y de la integración con XUI. Los televisores solo recibirán respuestas de API necesarias para la interfaz y la reproducción autorizada.
Decisión recomendada: `TV → HTTPS → API NestJS → Prisma → PostgreSQL`. La integración con XUI se realiza desde el backend, nunca desde la base de datos ni con credenciales administrativas incluidas en la aplicación.
2. Alcance y situación conocida
El documento de diseño de base de datos de COTAS describe PostgreSQL, Prisma 6, NestJS y una API REST con prefijo `/api/v1`.
La implementación documentada informa lo siguiente:
La migración `20261019220000_identity_model` se reporta como aplicada.
El modelo incluye `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `user_sessions`, `devices`, `user_mfa_methods`, `iptv_accounts`, `audit_logs` y `auth_challenges`.
No se reportan implementadas las tablas persistentes para catálogo/EPG ni `playback_sessions`.
El documento indica que el acceso actual utiliza el usuario y la contraseña de la línea XUI. Este flujo debe reflejarse en el contrato de autenticación de las aplicaciones.
La información disponible no especifica la URL definitiva de producción, el puerto público de la API, el dominio TLS ni la dirección y puerto reales de PostgreSQL. Esos valores deben confirmarse con infraestructura.
Por tanto, este informe define la arquitectura objetivo y el procedimiento técnico, pero no afirma que la conectividad pública, el certificado TLS o todas las rutas de autenticación estén ya desplegadas y verificadas.
3. Arquitectura de conexión
Componente	Responsabilidad	Acceso permitido
App Google TV / Android TV	Interfaz, inicio de sesión, navegación del catálogo y reproducción	HTTPS hacia la API de COTAS
App Samsung Tizen	Interfaz equivalente adaptada a Tizen	HTTPS hacia la API de COTAS
API NestJS	Autenticación, autorización, validación, sesiones y reglas de negocio	Conexión privada a PostgreSQL y llamadas autorizadas a XUI
Prisma 6	Acceso tipado a los modelos de PostgreSQL	Usado por el backend; no por las apps
PostgreSQL	Persistencia de cuentas, dispositivos, sesiones, roles, asignaciones y auditoría	Solo desde hosts o contenedores autorizados
XUI.one	Fuente de catálogo y servicio IPTV	Acceso desde el backend mediante el adaptador de integración
3.1. Flujo lógico
La aplicación de TV obtiene la URL base oficial de la API de COTAS.
El usuario introduce las credenciales aceptadas por el sistema.
La aplicación envía una solicitud HTTPS al endpoint de autenticación; no envía credenciales a PostgreSQL.
NestJS valida las credenciales mediante el flujo de autenticación definido y verifica la cuenta o línea en XUI cuando corresponde.
El backend registra o actualiza el usuario, el dispositivo y la sesión en PostgreSQL mediante Prisma.
La API devuelve un resultado de autenticación y los tokens de sesión previstos por el diseño.
Las solicitudes posteriores envían el token de acceso; NestJS valida el token, el estado de la sesión y los permisos antes de consultar los datos.
Para catálogo y reproducción, NestJS consulta XUI y devuelve únicamente información autorizada al cliente.
4. Qué significa conectarse a la base de datos
Las aplicaciones no tendrán una cadena de conexión PostgreSQL. La cadena `DATABASE_URL` debe existir únicamente en el entorno seguro del backend. NestJS utiliza Prisma para ejecutar las consultas.
Así se evita incluir IP, puerto, usuario y contraseña de PostgreSQL en un APK, paquete Tizen o código JavaScript accesible al cliente.
Dato	Dónde debe estar	Dónde no debe estar
`DATABASE_URL`	Variable de entorno o gestor de secretos del backend	APK, paquete Tizen, repositorio, frontend o logs
Credenciales de PostgreSQL	Entorno del servidor con privilegios mínimos	Aplicaciones de TV
Credenciales administrativas de XUI	Secretos del backend, si se necesitan para una operación autorizada	Aplicaciones de TV
Token de acceso de la aplicación	Memoria o almacenamiento seguro apropiado del cliente, con expiración	Logs o almacenamiento permanente sin protección
Contraseña de la línea IPTV	Flujo de autenticación; persistencia cifrada en backend solo si resulta necesaria	Respuestas públicas de API o logs
5. Requisitos de red e infraestructura
Origen	Destino	Regla requerida
Google TV / Samsung Tizen	Dominio de la API COTAS	HTTPS por el puerto publicado oficialmente; certificado válido y nombre de host coincidente
Contenedor o host NestJS	PostgreSQL	Conexión privada; puerto configurado de PostgreSQL; firewall limitado al origen del backend
Contenedor o host NestJS	XUI.one	Conectividad privada o controlada al endpoint autorizado; timeout y manejo de errores
Equipo administrativo	Panel administrativo/API	Acceso restringido a red corporativa o VPN, según política de COTAS
Internet	PostgreSQL	No permitir acceso público directo
No se debe asumir un puerto concreto ni una IP de producción hasta que infraestructura los confirme. El puerto de PostgreSQL y el puerto HTTP interno de NestJS son distintos del puerto HTTPS publicado para los televisores.
6. Configuración del backend
La configuración debe residir en variables de entorno del servidor. Los siguientes nombres son ilustrativos y deben ajustarse a los nombres que realmente consume el proyecto.
Variable	Finalidad	Tratamiento
`DATABASE_URL`	Conexión de Prisma con PostgreSQL	Secreto; solo backend
`PORT`	Puerto interno en el que escucha NestJS	Confirmar con despliegue
`API_PUBLIC_BASE_URL`	URL HTTPS que consumirán las apps	Configuración; no contiene secretos
`XUI_BASE_URL`	Endpoint base del proveedor XUI	Configuración interna del backend
`XUI_USERNAME` / `XUI_PASSWORD`	Credenciales técnicas, si el flujo las necesita	Secretos; nunca autentican al cliente por sí solos
Secretos de token/sesión	Firmar o gestionar sesiones, según implementación	Secretos; rotación y control de acceso
No copiar valores reales de secretos a documentación, repositorios, capturas o registros. No reutilizar la cuenta superusuario de PostgreSQL para la ejecución normal de NestJS. Se recomienda separar el usuario de ejecución del usuario utilizado para migraciones.
7. Flujo de autenticación y sesión
La app solicita inicio de sesión a la API mediante HTTPS.
El backend valida las entradas y consulta la dependencia de autenticación correspondiente. Los errores de XUI deben distinguirse de credenciales inválidas.
Si la autenticación es válida, el backend registra el usuario y la vinculación IPTV de acuerdo con las reglas ya implementadas.
La app registra o identifica el dispositivo mediante un identificador de aplicación seudonimizado; no debe depender de un identificador de hardware invasivo o no garantizado.
El backend crea una sesión revocable y entrega el token conforme al contrato de API.
En cada solicitud protegida, el backend comprueba token, expiración, revocación, estado del usuario y autorización del recurso.
Al cerrar sesión, cambiar credenciales o revocar el dispositivo, el backend invalida la sesión según las reglas del sistema.
El documento de base de datos describe rotación de refresh tokens mediante `family_id` y `replaced_by_id`. El contrato de API debe definir si el cliente TV utiliza refresh tokens, cómo los almacena y cómo se recupera de una sesión expirada. No se deben inventar rutas o formatos de token sin revisar el código actual.
8. Plataformas y registro de dispositivos
Plataforma	Valor lógico sugerido	Verificación necesaria
Google TV / Android TV	`ANDROID_TV`	Confirmar que el enum o validador del backend lo admite y que se guarda correctamente
Samsung Smart TV / Tizen	`SAMSUNG_TIZEN`	Confirmar el enum o validador; añadir el valor mediante migración si el esquema físico usa enum cerrado
Aplicación móvil	`ANDROID` u otro valor acordado	Mantener valores explícitos por plataforma
No se necesita una base de datos por plataforma. Las plataformas se distinguen como atributos del dispositivo, mientras que usuarios, asignaciones IPTV y permisos permanecen centralizados.
9. Contratos de API que deben verificarse
Los endpoints siguientes son capacidades requeridas, no una afirmación de que las rutas exactas ya existan. Deben cotejarse con los controladores y DTO actuales antes de cerrar la documentación.
Capacidad	Comportamiento esperado	Criterios de seguridad
Autenticación	Validar credenciales y emitir sesión	HTTPS, limitación de intentos, errores sin filtrar detalles internos
Perfil/sesión	Consultar usuario y estado de sesión	Token válido y sesión no revocada
Dispositivos	Registrar, listar o revocar dispositivos según alcance	El usuario solo gestiona los dispositivos autorizados
Categorías y catálogo	Devolver contenido normalizado desde XUI	No exponer credenciales XUI ni URLs no autorizadas
Reproducción	Autorizar y entregar datos de reproducción necesarios	Validar propietario, línea activa y destino permitido
Cierre de sesión	Revocar sesión/token según estrategia	La revocación se valida en el backend
10. Seguridad obligatoria
Publicar la API mediante HTTPS con certificado válido; no enviar credenciales por HTTP sin cifrar.
Mantener PostgreSQL fuera de Internet y limitar las reglas de firewall a los hosts que realmente necesitan acceder.
Usar un usuario de PostgreSQL con privilegios mínimos para la aplicación.
Mantener `DATABASE_URL`, secretos de sesión y credenciales XUI fuera de los clientes y del repositorio.
Validar entradas y permisos en el backend; ocultar botones en la interfaz no es autorización.
No registrar contraseñas, tokens, secretos MFA ni URLs de reproducción que contengan credenciales.
Aplicar límites de intentos, timeouts, límites de tamaño y manejo controlado de errores.
Separar el acceso del panel administrativo y restringirlo a red corporativa o VPN según decisión de infraestructura.
Usar migraciones Prisma revisables; no recrear la base de datos ni aplicar cambios destructivos para habilitar una plataforma.
Definir almacenamiento de tokens en cada cliente según las capacidades de Android TV y Tizen; minimizar la persistencia y permitir revocación desde servidor.
11. Procedimiento de implementación y pruebas
Fase	Actividad	Evidencia de aceptación
1. Inventario	Confirmar host de API, host/puerto de PostgreSQL, redes, TLS y reglas de firewall	Diagrama de red y matriz de parámetros aprobada
2. Revisión de código	Revisar `schema.prisma`, migraciones, configuración Prisma y módulos de autenticación/dispositivos	Matriz de rutas y modelos existentes
3. Conectividad interna	Verificar desde el entorno de NestJS conexión a PostgreSQL sin exponer la DB	Prueba de conexión y health check, sin revelar secretos
4. API segura	Publicar o confirmar endpoint HTTPS y certificado válido	Prueba TLS desde una red externa autorizada
5. Prueba de autenticación	Probar credenciales válidas, inválidas, línea suspendida y XUI no disponible	Resultados diferenciados y sin filtración de información
6. Prueba Android TV	Inicio/cierre de sesión, catálogo, expiración y revocación	Evidencia en dispositivo o emulador compatible
7. Prueba Tizen	Repetir los flujos en Samsung TV o navegador de prueba compatible	Evidencia de compatibilidad y manejo de errores
8. Seguridad	Comprobar que PostgreSQL no es accesible desde el cliente y que secretos no aparecen en paquetes/logs	Prueba negativa documentada
9. Aprobación	Revisar resultados, respaldos y plan de reversión antes de producción	Acta o checklist aprobado
12. Datos que debe confirmar infraestructura
URL pública definitiva de la API y dominio que utilizarán las apps.
Puerto HTTPS publicado y componente que termina TLS (proxy inverso, balanceador o servidor de aplicación).
Host/IP privada y puerto de PostgreSQL; confirmar que solo NestJS puede acceder.
Host/puerto interno del backend NestJS y si se ejecuta en Docker.
Ubicación segura de variables de entorno y responsable de su rotación.
Reglas de firewall entre backend, PostgreSQL y XUI.
Certificado TLS, renovación y monitoreo de vencimiento.
Entornos separados para pruebas y producción; no usar la base de datos de producción para pruebas destructivas.
Procedimiento de backup y restauración antes de migraciones.
13. Riesgos y decisiones pendientes
Riesgo o pendiente	Impacto	Acción
URL pública/TLS no documentados	Las apps no pueden conectarse de forma segura desde fuera de la LAN	Definir dominio, certificado y ruta de publicación
Valores `platform` no verificados	El registro de dispositivos puede fallar en Tizen o Android TV	Revisar enum/validación y migrar solo si es necesario
Contrato de autenticación no confirmado	El cliente puede implementar un flujo incompatible con el backend	Documentar rutas, DTO, códigos de error y tokens desde el código real
Límites de dispositivos/reproducción no definidos	No existe una regla comercial verificable para concurrencia	Aprobar política antes de implementarla
Credenciales XUI en el cliente	Compromiso de líneas y contenidos	Mantener integración y secretos en el backend
Acceso público a PostgreSQL	Exposición crítica de datos	Bloquear el acceso público y verificar firewall
14. Criterios de aceptación
Google TV y Samsung Tizen solo consumen la API de COTAS y no contienen credenciales PostgreSQL/XUI.
NestJS conecta con PostgreSQL usando una cuenta de privilegios mínimos y una configuración externa al repositorio.
PostgreSQL acepta conexiones únicamente desde los orígenes autorizados.
La API se consume mediante HTTPS con certificado válido.
Los usuarios y dispositivos se registran sin duplicar la base de datos por plataforma.
Los tokens expirados o revocados no permiten acceder a rutas protegidas.
Los errores de XUI, PostgreSQL y autenticación se manejan sin exponer detalles internos ni secretos.
Se documentan y ejecutan pruebas para ambos clientes antes de liberar la versión.
15. Conclusión
La base de datos existente se integra con las aplicaciones de televisión a través del backend NestJS y Prisma, no mediante conexiones directas desde los televisores. Google TV y Samsung Tizen compartirán la API y el modelo de datos. Los cambios de esquema solo se realizarán si la inspección del código y de las migraciones demuestra que falta un valor de plataforma u otra capacidad requerida.
Antes de implementar la conexión en las apps, COTAS debe confirmar los parámetros reales de red, la publicación HTTPS, el contrato de autenticación y la validación de dispositivos. El documento debe actualizarse con esos valores después de verificarlos en el servidor; no deben sustituirse por direcciones o puertos supuestos.
Anexo A. Diagrama resumido
Paso	Ruta	Descripción
1	TV → HTTPS → API NestJS	Solicitudes de autenticación, catálogo y sesión
2	NestJS → Prisma → PostgreSQL	Persistencia de usuarios, dispositivos, sesiones y auditoría
3	NestJS → adaptador XUI → XUI	Validación externa, catálogo y datos autorizados de reproducción
4	API NestJS → TV	Respuesta JSON sin credenciales internas ni acceso directo a la base
5	TV → servicio de streaming autorizado	Transporte de vídeo por la ruta autorizada; no pasa por PostgreSQL

# Plan de pruebas — Google TV / Android TV

**Estado:** plan previo a ejecución. Completar resultados con evidencia real.

## Casos mínimos
| ID | Caso | Resultado esperado | Estado |
|---|---|---|---|
| TV-001 | Inicio sin sesión | Mostrar login | Pendiente |
| TV-002 | Login válido real | Sesión válida y navegación a catálogo | Bloqueado por validación API |
| TV-003 | Login inválido | Error controlado; sin sesión | Pendiente |
| TV-004 | API desconectada | Error sin bloquear UI | Pendiente |
| TV-005 | Catálogo autenticado | Mostrar datos reales de API | Pendiente |
| TV-006 | Catálogo vacío | Estado vacío | Pendiente |
| TV-007 | Sesión expirada | Aplicar política documentada | Pendiente |
| TV-008 | D-pad y Atrás | Foco y navegación correctos | Pendiente |
| TV-009 | Selección de contenido | Solicitar autorización al backend | Pendiente |
| TV-010 | Recurso autorizado | Reproducción en dispositivo probado | Pendiente |
| TV-011 | Recurso no autorizado | No iniciar reproducción | Pendiente |
| TV-012 | Salida del reproductor | Liberar recursos | Pendiente |
| TV-013 | Pérdida de red durante reproducción | Error controlado | Pendiente |
| TV-014 | Inspección APK/logs | Sin secretos | Pendiente |
| TV-015 | Instalación limpia | APK abre en entorno objetivo | Pendiente |

## Matriz de dispositivos
| Tipo | Modelo | Versión del sistema | Resultado |
|---|---|---|---|
| Emulador TV | Por confirmar | Por confirmar | Pendiente |
| Dispositivo físico | Por confirmar | Por confirmar | Pendiente |

## Evidencia por prueba
Registrar fecha, commit, versión de app, dispositivo, entorno API, pasos, resultado esperado/observado, estado y referencia de defecto. Diferenciar emulador de hardware real y pruebas con dobles de las end-to-end.

## Criterios de salida
Login, catálogo y reproducción probados con servicios reales; navegación con mando validada; defectos bloqueantes resueltos o aceptados formalmente; sin secretos en APK/logs; limitaciones documentadas.

# Pruebas — Google TV / Android TV (COTAS)

**Versión app:** 0.3.0 (paridad UI Samsung + reproducción)  
**API de pruebas:** `http://10.10.0.31:3100` (origen; paths bajo `/api/v1`)  
**Fecha:** 28 de octubre de 2026

## Compilar e instalar

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.cotasperu.tv/.MainActivity
```

Requisitos: el emulador o el televisor deben alcanzar `10.10.0.31:3100` (misma LAN).

## Endpoints usados (verificados)

| Uso | Método | Ruta |
|-----|--------|------|
| Salud | GET | `/api/v1/health/live`, `/api/v1/health/ready` |
| Login | POST | `/api/v1/auth/login` |
| Refresh | POST | `/api/v1/auth/refresh` |
| Logout | POST | `/api/v1/auth/logout` |
| Perfil | GET | `/api/v1/auth/me` |
| Catálogo | GET | `/api/v1/catalog/live|vod|series/...` (`page`, `limit≤50`, `categoryId`) |
| Reproducción | POST | `/api/v1/playback/authorizations` |

No hay conexión directa a PostgreSQL ni a XUI desde la app.

## Checklist de aceptación

- [ ] Al abrir, la app pasa el chequeo de salud (API + base de datos).
- [ ] Login con cuenta de prueba autorizada.
- [ ] Login inválido muestra mensaje genérico y limpia la contraseña.
- [ ] Inicio muestra filas de TV en vivo, películas y series.
- [ ] Secciones TV / Películas / Series listan categorías y títulos.
- [ ] Filtrar por categoría (tocar de nuevo la misma para quitar el filtro).
- [ ] Detalle de película y flujo serie → temporada → episodio.
- [ ] Canal en vivo: al seleccionar, autoriza y abre el reproductor (sin barra de seek).
- [ ] Película: detalle → Reproducir → Media3 con controles (pausa/seek si aplica).
- [ ] Episodio: serie → temporada → episodio → Reproducir.
- [ ] Error de reproducción ofrece Reintentar (nueva autorización) o Volver.
- [ ] Atrás / Salir detiene el audio y libera el reproductor.
- [ ] Cerrar sesión elimina el acceso local.
- [ ] No hay contraseñas, tokens ni `streamUrl` en el código fuente.

## Limitaciones conocidas

- URL de producción HTTPS aún no definida; el build release no usa la IP de pruebas.
- Imágenes y streams suelen ser HTTP en laboratorio; el debug lo permite, el release no.
- Rate limit del API: 10 intentos / 60 s en login, refresh y autorizaciones.
- En Inicio solo se cargan las primeras 2 páginas (hasta 100 ítems por fila) para respuesta rápida; en cada sección se pagina el listado completo (máx. 50 por página).

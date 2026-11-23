# Informe — Velocidad de posters / imágenes de catálogo (Google TV)

**Destinatario:** equipo backend NestJS COTAS  
**Emisor:** app Google TV / Android TV  
**Fecha:** 5 de noviembre de 2026  
**Versión app de referencia:** 0.3.30  
**Objetivo UX:** el abonado entra a Home / Canales / Películas / Series y **ve posters casi al instante**, sin spinner prolongado ni celdas vacías.

---

## 1. Resumen ejecutivo

La lentitud percibida en posters **no se resuelve solo en la app**. Hoy el cliente ya:

- descarga el catálogo por páginas,
- muestra filas en cuanto llegan los JSON,
- pide a Coil imágenes **redimensionadas al tamaño de la tarjeta** (no decodifica 4K en pantalla completa),
- cachea en memoria (~22 %) y disco (~180 MB),
- ignora `Cache-Control` agresivo del origen (`respectCacheHeaders = false`) para poder reutilizar disco.

Aun así, si cada poster es un JPEG/PNG **grande** servido desde un host **lento, lejano o inestable** (típicamente URLs crudas de XUI / panel), la TV debe:

1. resolver DNS,
2. abrir TLS,
3. bajar cientos de KB–varios MB **por imagen**,
4. decodificar en CPU/GPU limitada de Android TV.

**Conclusión:** hace falta trabajo conjunto. Lo que más acelera el primer pintado, sin degradar calidad percibida en 1080p, es que el **backend (o un CDN delante) entregue variantes livianas y estables** de cada imagen.

---

## 2. Qué hace hoy el cliente (para no duplicar esfuerzo)

| Área | Comportamiento actual |
|------|------------------------|
| Catálogo | Carga rápida (`maxPages = 2`) y luego completa el resto en segundo plano |
| Campos usados | `posterUrl` / `cover` / `poster` / `stream_icon` / `iconUrl` / `logo` y backdrop (`backdropUrl`, `fanart`, …) |
| Imagen en UI | Coil con `size(ancho×alto dp→px)`, `Precision.INEXACT`, hardware bitmaps |
| Caché | Memoria + disco local; reuso entre sesiones mientras no se limpie almacenamiento |
| Home | Muestra rails cuando hay datos; el hero rota ítems que **ya tienen** URL de imagen |

**Límite del cliente:** no puede inventar un thumbnail que el servidor no expone. Si solo llega una URL de poster original (p. ej. 1000×1500 px / 800 KB–2 MB), cada tarjeta de la grilla paga ese coste la primera vez.

---

## 3. Diagnóstico de cuellos de botella (lado servidor / origen)

Orden de impacto típico en TV (LAN del hogar + Wi‑Fi):

1. **Peso del archivo** (KB por poster) — dominante al abrir Home con 12–40 imágenes visibles.
2. **Latencia y ubicación del origen** (XUI en VPS lejos vs CDN edge).
3. **Falta de variantes** (misma URL full para grid 160×240 y para hero 1920×1080).
4. **Cold start de catálogo** (muchas páginas antes de tener URLs) — la app ya mitiga con carga progresiva.
5. **TLS/DNS por host distinto** al API (`api.cotasperu.com` vs dominio de imágenes XUI).

Medición sugerida (backend o DevOps): para 20 posters aleatorios del catálogo de prueba, registrar **TTFB, tamaño en bytes y tiempo total** desde Lima / desde una TV real.

---

## 4. Peticiones al backend (prioridad)

### P0 — Variantes de imagen en el contrato de catálogo

En listados (`/catalog/vod/streams`, `/catalog/series`, `/catalog/live/streams` y home/rails equivalentes), además de la URL “full”, incluir URLs pregeneradas o firmadas:

| Campo propuesto | Uso en TV | Tamaño objetivo |
|-----------------|-----------|-----------------|
| `posterThumbUrl` | Tarjetas de grilla / rails | **≤ 80 KB**, lado largo ≈ 300–400 px (WebP o JPEG q70) |
| `posterUrl` | Ficha / hero poster | **≤ 250 KB**, lado largo ≈ 600–800 px |
| `backdropUrl` / `backdropThumbUrl` | Hero fondo | thumb ≤ 150 KB (1280×720 máx. recomendado); full opcional |

**Reglas:**

- Si no existe thumb, devolver `null` (la app cae a `posterUrl`); no omitir el campo si el resto del ítem es válido.
- Misma política HTTPS; host allowlist documentado.
- No forzar a la app a pasar query mágicas no documentadas (`?w=300`) salvo que el CDN lo soporte de forma estable.

**Ejemplo de ítem (ilustrativo):**

```json
{
  "id": "12345",
  "name": "Título demo",
  "posterThumbUrl": "https://cdn.cotasperu.com/posters/12345_t.webp",
  "posterUrl": "https://cdn.cotasperu.com/posters/12345.webp",
  "backdropThumbUrl": "https://cdn.cotasperu.com/backdrops/12345_t.webp",
  "backdropUrl": "https://cdn.cotasperu.com/backdrops/12345.webp"
}
```

### P0 — CDN / caché HTTP para assets de imagen

- Servir posters desde **CDN** (Cloudflare, CloudFront, etc.) cercano a abonados en Perú, no solo el disco del panel XUI.
- Headers recomendados para thumbs: `Cache-Control: public, max-age=86400` (o superior) + `ETag`/`immutable` si el path incluye hash de contenido.
- Evitar redirects 301/302 en cadena (cada redirect suma RTT en TV).
- Preferir **HTTP/2** o HTTP/3 en el edge.

### P1 — Pipeline de generación (offline / al sincronizar XUI)

Al importar o refrescar metadatos desde XUI:

1. Descargar cover/fanart de origen.
2. Generar `thumb` + `poster` (+ backdrop thumb).
3. Subir a object storage / CDN.
4. Persistir URLs en BD COTAS.
5. Exponer esas URLs en el API de catálogo (no la URL frágil del panel).

Así el primer usuario en TV no “paga” el resize; ya está hecho una vez en servidor.

### P1 — Endpoint o campo de “bootstrap Home”

Opcional pero muy útil para UX:

`GET /api/v1/catalog/home` (o ampliar el actual) que devuelva **solo lo visible en primer viewport**:

- 8–12 ítems por rail (live / vod / series),
- con `posterThumbUrl` ya relleno,
- sin plot largo ni campos pesados.

La app puede pintar Home en **una** respuesta y completar el resto en background (ya tiene patrón similar).

### P2 — Formato moderno

- Preferir **WebP** (o AVIF si el parque de TV lo soporta; WebP es más seguro en Android 8+/TV).
- Evitar PNG de logos de canal a resolución innecesaria; para live, `iconUrl` ≤ 64–128 px / ≤ 30 KB.

### P2 — Observabilidad

- Métricas: % ítems con `posterThumbUrl` no nulo, tamaño medio de thumb, 4xx/5xx del CDN de imágenes.
- Alertar si el sync XUI deja thumbs vacíos (> X % del catálogo).

---

## 5. Qué **no** pedir al backend (o no es prioritario)

- No hace falta que el API “empuje” binarios de imagen en el JSON del catálogo (base64) — empeora listados.
- No hace falta bajar la paginación a 1 ítem; el problema dominante es el **peso por imagen**, no solo el número de páginas.
- No desactivar TLS ni usar HTTP en producción.

---

## 6. Criterios de aceptación (medibles)

En red residencial típica (Wi‑Fi, ~20–50 Mbps) y build Google TV 1080p:

| Escenario | Objetivo |
|-----------|----------|
| Home tras login (sesión fría, sin caché disco de Coil) | ≥ 80 % de posters del primer viewport visibles en **< 2,5 s** tras tener el JSON de rails |
| Mismo Home, segunda visita (caché disco) | ≥ 90 % visibles en **< 0,8 s** |
| Peso medio `posterThumbUrl` | **≤ 80 KB** |
| Disponibilidad thumb en catálogo VOD/Series de prueba | **≥ 95 %** de ítems con thumb no nulo |
| Canales (logos) | logo/icon ≤ 30 KB o placeholder estable sin parpadeo largo |

Si tras P0 el cliente sigue lento, el cuello será casi seguro red del hogar o decodificación; entonces se afina prefetch en app. Sin thumbs/CDN, la app **no puede** garantizar “sin vacío”.

---

## 7. Plan de adopción en la app (cuando el backend esté listo)

1. Extender `CatalogParser` para leer `posterThumbUrl` / `backdropThumbUrl`.
2. Usar thumb en grillas y rails; `posterUrl` / backdrop full en ficha y hero.
3. Prefetch de los N primeros thumbs del rail enfocado (ya viable con Coil).
4. Mantener fallback: thumb → poster → backdrop → placeholder con inicial.

**Dependencia:** sin campos nuevos en API, el cliente solo puede seguir optimizando caché/prefetch, con mejora limitada.

---

## 8. Preguntas concretas para el equipo backend

1. ¿Las URLs de cover/fanart actuales son del panel XUI o ya pasan por un dominio COTAS/CDN?
2. ¿Existe (o se puede añadir) object storage + CDN para assets de catálogo?
3. ¿El sync XUI→BD puede generar derivados (thumb/poster) de forma asíncrona sin bloquear el listado?
4. ¿Pueden exponer en OpenAPI `posterThumbUrl` / `backdropThumbUrl` en esta sprint o la siguiente?
5. ¿Hay rate-limit o hotlink protection en el host de imágenes que ralentice muchas peticiones paralelas desde un mismo televisor?

---

## 9. Priorización sugerida

| Orden | Entrega | Impacto en TV |
|------|---------|----------------|
| 1 | CDN + caché en URLs actuales (mismo path, mejor edge) | Alto, rápido de ops |
| 2 | `posterThumbUrl` en listados + generación al sync | Muy alto |
| 3 | Bootstrap Home liviano | Alto en primer paint |
| 4 | WebP + métricas | Medio–alto sostenido |

---

## 10. Mensaje corto para el canal interno

> Para que en Google TV no se vean posters vacíos ni spinners largos, necesitamos variantes livianas (`posterThumbUrl` ≤ 80 KB) y servirlas por CDN, generadas al sincronizar XUI. La app ya cachea y pide tamaño de tarjeta; sin thumbs en el contrato, el límite es el peso/latencia del origen de imagen.

---

**Contacto técnico app:** equipo Google TV (Compose / Coil).  
**Contrato a actualizar:** OpenAPI tag `catalog` + este informe como criterio de aceptación de rendimiento percibido.

package com.cotasperu.tv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import coil.compose.AsyncImage
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.CatalogKind
import com.cotasperu.tv.ui.theme.CotasBg
import com.cotasperu.tv.ui.theme.CotasCyanFocus
import com.cotasperu.tv.ui.theme.CotasHeroMediaEmpty
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimary
import com.cotasperu.tv.ui.theme.CotasPrimaryStrong
import kotlin.random.Random
import kotlinx.coroutines.delay

data class HeroItem(
    val entry: CatalogEntry,
    val kind: CatalogKind,
)

@Composable
fun HomeHeroRotator(
    pool: List<HeroItem>,
    active: Boolean,
    onPlay: (CatalogEntry, CatalogKind) -> Unit,
    onHeroFocused: () -> Unit = {},
    playFocus: FocusRequester? = null,
    downFocus: FocusRequester? = null,
    onRequestDown: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf(pool.firstOrNull()) }
    val blackout = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(1f) }

    // Al enriquecer backdrops, conservar el ítem actual (mismo id) con datos nuevos.
    LaunchedEffect(pool) {
        if (pool.isEmpty()) {
            current = null
            return@LaunchedEffect
        }
        val curId = current?.entry?.id
        current = pool.firstOrNull { it.entry.id == curId } ?: pool.first()
    }

    LaunchedEffect(pool, active) {
        if (!active || pool.size < 2) return@LaunchedEffect
        while (true) {
            delay(8_000)
            // Transición más corta → menos jank en TV.
            contentAlpha.snapTo(0.4f)
            blackout.snapTo(1f)
            current = pickNext(pool, current)
            delay(40)
            blackout.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            contentAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    HomeHeroFrame(
        item = current,
        contentAlpha = contentAlpha.value,
        blackout = blackout.value,
        onPlay = onPlay,
        onHeroFocused = onHeroFocused,
        playFocus = playFocus,
        downFocus = downFocus,
        onRequestDown = onRequestDown,
        modifier = modifier,
    )
}

@Composable
private fun HomeHeroFrame(
    item: HeroItem?,
    contentAlpha: Float,
    blackout: Float,
    onPlay: (CatalogEntry, CatalogKind) -> Unit,
    onHeroFocused: () -> Unit,
    playFocus: FocusRequester?,
    downFocus: FocusRequester?,
    onRequestDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = item?.entry
    val kind = item?.kind
    val isLive = kind == CatalogKind.LIVE_STREAMS
    // Fondo = solo backdrop (nunca el póster). Póster adelantado = cover.
    val poster = entry?.imageUrl
    val backdrop = entry?.backdropUrl
        ?.takeIf { it.isNotBlank() }
        ?.takeIf { poster == null || !it.equals(poster, ignoreCase = true) }
    val posterW = 168.dp
    val posterH = 252.dp

    Box(
        modifier
            .fillMaxWidth()
            .background(CotasBg),
    ) {
        val heroBgRequest = rememberPosterRequest(backdrop, 960.dp, 540.dp)
        Box(Modifier.fillMaxSize().background(CotasHeroMediaEmpty).alpha(contentAlpha)) {
            if (heroBgRequest != null) {
                AsyncImage(
                    model = heroBgRequest,
                    contentDescription = entry?.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                )
            }
        }

        HeroFades()

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .alpha(contentAlpha)
                .padding(start = 36.dp, end = 36.dp, bottom = 40.dp, top = 32.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            val posterRequest = rememberPosterRequest(poster, posterW, posterH)
            if (!isLive && posterRequest != null) {
                AsyncImage(
                    model = posterRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .width(posterW)
                        .height(posterH)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DestacadoBadge()
                    TypeBadge(
                        when (kind) {
                            CatalogKind.VOD_STREAMS -> "PELÍCULA"
                            CatalogKind.SERIES -> "SERIE"
                            CatalogKind.LIVE_STREAMS -> "CANAL"
                            else -> "CONTENIDO"
                        },
                    )
                }
                Text(
                    entry?.title ?: "COTAS TV",
                    color = CotasOnSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildMeta(entry, kind)
                if (meta != null) {
                    Text(meta, color = CotasMuted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (entry != null && kind != null) {
                    var playFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { onPlay(entry, kind) },
                        colors = ButtonDefaults.colors(
                            containerColor = CotasPrimaryStrong,
                            contentColor = Color.White,
                            focusedContainerColor = CotasCyanFocus,
                            focusedContentColor = Color.White,
                            pressedContainerColor = CotasPrimary,
                            pressedContentColor = Color.White,
                        ),
                        scale = cotasButtonScale(),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(999.dp)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .widthIn(min = 118.dp)
                            .border(
                                width = if (playFocused) 2.dp else 0.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(999.dp),
                            )
                            .then(if (playFocus != null) Modifier.focusRequester(playFocus) else Modifier)
                            .then(
                                if (downFocus != null) {
                                    Modifier.focusProperties { down = downFocus }
                                } else {
                                    Modifier
                                },
                            )
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                                    onRequestDown()
                                    true
                                } else {
                                    false
                                }
                            }
                            .onFocusChanged {
                                playFocused = it.isFocused
                                if (it.isFocused) onHeroFocused()
                            },
                    ) {
                        Box(
                            modifier = Modifier.widthIn(min = 82.dp).height(26.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (kind == CatalogKind.SERIES) "Ver" else "Reproducir",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }

        // Capa de oscurecimiento para el cambio de slide.
        if (blackout > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = blackout)),
            )
        }
    }
}

@Composable
private fun BoxScope.HeroFades() {
    Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.55f)
            .align(Alignment.CenterStart)
            .background(
                Brush.horizontalGradient(
                    0.00f to Color.Black,
                    0.28f to Color.Black.copy(alpha = 0.92f),
                    0.62f to Color.Black.copy(alpha = 0.45f),
                    1.00f to Color.Transparent,
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.38f)
            .align(Alignment.CenterEnd)
            .background(
                Brush.horizontalGradient(
                    0.00f to Color.Transparent,
                    0.60f to Color.Black.copy(alpha = 0.75f),
                    1.00f to Color.Black,
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.34f)
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Black,
                    0.45f to Color.Black.copy(alpha = 0.70f),
                    1.00f to Color.Transparent,
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.48f)
            .align(Alignment.BottomCenter)
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.30f to Color.Black.copy(alpha = 0.35f),
                    0.65f to Color.Black.copy(alpha = 0.90f),
                    1.00f to Color.Black,
                ),
            ),
    )
}

@Composable
private fun DestacadoBadge() {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0EA5E9).copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "DESTACADO",
            color = CotasPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun TypeBadge(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF272A32).copy(alpha = 0.9f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = CotasOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun buildMeta(entry: CatalogEntry?, kind: CatalogKind?): String? {
    if (entry == null) return null
    val type = when (kind) {
        CatalogKind.VOD_STREAMS -> "Película"
        CatalogKind.SERIES -> "Serie"
        CatalogKind.LIVE_STREAMS -> "Canal"
        else -> null
    }
    val sub = listOfNotNull(entry.year, entry.genre?.take(36), type).joinToString(" · ")
    return sub.takeIf { it.isNotBlank() } ?: entry.subtitle?.takeIf { it.isNotBlank() }
}

private fun pickNext(pool: List<HeroItem>, current: HeroItem?): HeroItem {
    if (pool.size == 1) return pool.first()
    repeat(8) {
        val next = pool[Random.nextInt(pool.size)]
        if (next.entry.id != current?.entry?.id) return next
    }
    return pool.first { it.entry.id != current?.entry?.id }
}

fun buildHeroPool(rails: List<RailUi>): List<HeroItem> {
    val all = rails
        .filter { it.kind == CatalogKind.VOD_STREAMS || it.kind == CatalogKind.SERIES }
        .flatMap { rail ->
            rail.items
                .filter { !it.imageUrl.isNullOrBlank() || !it.backdropUrl.isNullOrBlank() }
                .take(12)
                .map { HeroItem(it, rail.kind) }
        }
        .distinctBy { it.entry.id }
    // Preferir títulos con backdrop propio; si aún no llegaron, usar el pool completo.
    val withBackdrop = all.filter { item ->
        val bd = item.entry.backdropUrl
        val poster = item.entry.imageUrl
        !bd.isNullOrBlank() && (poster == null || !bd.equals(poster, ignoreCase = true))
    }
    return if (withBackdrop.size >= 2) withBackdrop else all
}

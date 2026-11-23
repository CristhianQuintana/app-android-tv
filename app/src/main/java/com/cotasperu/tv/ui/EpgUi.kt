package com.cotasperu.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cotasperu.tv.core.json.EpgParser
import com.cotasperu.tv.domain.ChannelEpgGuide
import com.cotasperu.tv.domain.EpgHitStatus
import com.cotasperu.tv.domain.EpgProgram
import com.cotasperu.tv.domain.EpgSearchHit
import com.cotasperu.tv.ui.theme.CotasError
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnPrimary
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimary
import com.cotasperu.tv.ui.theme.CotasPrimaryStrong
import com.cotasperu.tv.ui.theme.CotasSecondary
import com.cotasperu.tv.ui.theme.CotasSurface
import com.cotasperu.tv.ui.theme.cotasFocusRing

private val EpgCardBg = Color(0xEB0C0E12)
private val EpgListBg = Color(0xE6080A0E)
private val HitText = Color(0xFFE8E8E8)
private val DescMuted = Color(0xFFA8A8A8)
private val EmptyAmber = Color(0xFFF0B429)
private val PillOffBg = Color(0xD9272A32)
private val PillOnBg = Color(0x333B9EFF)
private val LiveBadgeBg = Color(0x4710A5E9)
private val LiveBadgeFg = Color(0xFF7DD3FC)
private val UpcomingBadgeBg = Color(0x38F59E0B)
private val UpcomingBadgeFg = Color(0xFFFBBF24)
private val EndedBadgeBg = Color(0x3394A3B8)
private val EndedBadgeFg = Color(0xFF94A3B8)

@Composable
fun LiveModeBar(
    mode: LiveMode,
    onMode: (LiveMode) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LiveModePill("Canales", selected = mode == LiveMode.CHANNELS) { onMode(LiveMode.CHANNELS) }
        LiveModePill("Programas", selected = mode == LiveMode.PROGRAMS) { onMode(LiveMode.PROGRAMS) }
    }
}

@Composable
private fun LiveModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 140.dp)
            .clip(RoundedCornerShape(999.dp))
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 999.dp, showIdleOutline = false)
            .border(
                width = 2.dp,
                color = when {
                    focused -> CotasPrimary
                    selected -> CotasPrimary.copy(alpha = 0.75f)
                    else -> Color(0x8C3E4850)
                },
                shape = RoundedCornerShape(999.dp),
            )
            .background(if (selected) PillOnBg else PillOffBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when {
                focused -> Color.White
                selected -> CotasPrimary
                else -> CotasMuted
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Resultados EPG (sin barra: la aporta LiveHub para no perder el foco). */
@Composable
fun ColumnScope.EpgResultsBody(
    hits: List<EpgSearchHit>,
    searching: Boolean,
    status: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenHit: (EpgSearchHit) -> Unit,
) {
    if (!status.isNullOrBlank()) {
        Text(
            status,
            color = when {
                searching -> CotasPrimary
                status.contains("No encontramos", ignoreCase = true) ||
                    status.contains("2 caracteres", ignoreCase = true) -> EmptyAmber
                status.contains("error", ignoreCase = true) ||
                    status.contains("no disponible", ignoreCase = true) -> CotasError
                else -> CotasMuted
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EpgListBg)
            .border(1.dp, Color(0x663E4850), RoundedCornerShape(12.dp))
            .verticalScroll(rememberScrollState()),
    ) {
        hits.forEach { hit ->
            EpgHitRow(hit = hit, onClick = { onOpenHit(hit) })
        }
    }
    if (hasMore) {
        var moreFocused by remember { mutableStateOf(false) }
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp, bottom = 8.dp)
                .height(40.dp)
                .widthIn(min = 180.dp)
                .onFocusChanged { moreFocused = it.isFocused }
                .cotasFocusRing(moreFocused, radius = 6.dp, showIdleOutline = false)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x736D6D6E))
                .clickable(onClick = onLoadMore)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Cargar más", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EpgHitRow(hit: EpgSearchHit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val (badgeBg, badgeFg) = when (hit.status) {
        EpgHitStatus.LIVE -> LiveBadgeBg to LiveBadgeFg
        EpgHitStatus.UPCOMING -> UpcomingBadgeBg to UpcomingBadgeFg
        EpgHitStatus.ENDED -> EndedBadgeBg to EndedBadgeFg
    }
    val schedule = when {
        !hit.startLocal.isNullOrBlank() && !hit.endLocal.isNullOrBlank() ->
            "${hit.startLocal} – ${hit.endLocal}"
        else -> EpgParser.formatRange(hit.startAt, hit.endAt)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) CotasPrimary.copy(alpha = 0.22f) else Color.Transparent)
            .then(
                if (focused) Modifier.border(2.dp, CotasPrimary.copy(alpha = 0.75f))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                hit.title,
                color = if (focused) Color.White else HitText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    hit.statusLabel.uppercase(),
                    color = badgeFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        val meta = listOfNotNull(hit.channelName, schedule).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, color = CotasMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!hit.description.isNullOrBlank()) {
            Text(hit.description!!, color = DescMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ChannelEpgBlock(
    guide: ChannelEpgGuide?,
    loading: Boolean,
    error: String?,
    onPlayLive: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EpgCardBg)
            .border(1.dp, Color(0x733E4850), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Guía de programación", color = CotasOnSurface, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                when {
                    loading -> "Cargando guía…"
                    guide != null -> "${guide.items.size} programas"
                    else -> ""
                },
                color = CotasMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when {
            loading -> Text("Cargando guía…", color = CotasPrimary, fontSize = 14.sp)
            !error.isNullOrBlank() -> Text(error, color = CotasMuted, fontSize = 15.sp)
            guide == null -> Text("Guía no disponible para este canal.", color = CotasMuted, fontSize = 15.sp)
            else -> {
                NowCard(guide.now)
                val next = guide.next
                if (next != null) {
                    val range = EpgParser.formatRange(next.startAt, next.endAt)
                        ?: listOfNotNull(next.startLabel, next.endLabel).joinToString(" – ")
                    Text(
                        listOfNotNull("A continuación", next.title, range.takeIf { it.isNotBlank() })
                            .joinToString(" · "),
                        color = CotasMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (guide.items.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EpgListBg)
                            .border(1.dp, Color(0x663E4850), RoundedCornerShape(10.dp))
                            .verticalScroll(rememberScrollState()),
                    ) {
                        guide.items.forEach { program ->
                            EpgProgramRow(
                                program = program,
                                isNow = guide.now?.id != null && program.id == guide.now.id,
                                onClick = onPlayLive,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowCard(now: EpgProgram?) {
    if (now == null) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xB314161C))
                .border(1.dp, Color(0x663E4850), RoundedCornerShape(10.dp))
                .padding(16.dp),
        ) {
            Text("Sin programa en emisión identificado", color = CotasMuted, fontSize = 15.sp)
        }
        return
    }
    val pct = EpgParser.progressPercent(now)
    val range = EpgParser.formatRange(now.startAt, now.endAt)
        ?: listOfNotNull(now.startLabel, now.endLabel).joinToString(" – ")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0x381D7FE0), Color(0xF214161C)),
                ),
            )
            .border(1.dp, CotasPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CotasPrimary)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text("AHORA", color = CotasOnPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(now.title, color = CotasOnSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
        if (range.isNotBlank()) {
            Text(range, color = CotasSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (pct != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(pct / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(CotasPrimaryStrong, CotasSecondary))),
                    )
                }
                Text("$pct%", color = CotasMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (!now.description.isNullOrBlank()) {
            Text(now.description!!, color = Color(0xFFC8C8C8), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EpgProgramRow(
    program: EpgProgram,
    isNow: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val start = program.startLabel ?: EpgParser.formatRange(program.startAt, null)?.substringBefore(" – ")
    val end = program.endLabel ?: program.endAt?.let { EpgParser.formatRange(null, it)?.substringAfter(" – ") }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(
                when {
                    focused -> CotasPrimary.copy(alpha = 0.22f)
                    isNow -> CotasPrimary.copy(alpha = 0.10f)
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            start ?: "--:--",
            color = CotasPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(56.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                program.title,
                color = if (focused) Color.White else HitText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!program.description.isNullOrBlank()) {
                Text(program.description!!, color = CotasMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(end ?: "", color = CotasMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(48.dp))
    }
}

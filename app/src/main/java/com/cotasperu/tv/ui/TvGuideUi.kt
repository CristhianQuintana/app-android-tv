package com.cotasperu.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cotasperu.tv.core.json.EpgParser
import com.cotasperu.tv.domain.CatalogEntry
import com.cotasperu.tv.domain.ChannelEpgGuide
import com.cotasperu.tv.domain.EpgProgram
import com.cotasperu.tv.ui.theme.CotasCyanFocus
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimary
import com.cotasperu.tv.ui.theme.cotasFocusRing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

private val GuideBg = Color(0xFF0B0D10)
private val GuideSidebar = Color(0xFF12151A)
private val GuideHeader = Color(0xFF1A1F26)
private val GuideNowHeader = Color(0xFF33383E)
private val GuideDayActive = Color(0xFF0A5C5C)
private val GuideCell = Color(0xFF1C222A)
private val GuideCellAlt = Color(0xFF161B22)
private val GuideLine = Color(0xFF2A313A)
private val GuideTimeMuted = Color(0xFF8B949E)
private val GuideUnavailable = Color(0xFF6B7280)

private val ChannelColWidth = 196.dp
private val NowColWidth = 132.dp
private val RowHeight = 56.dp
private val HeaderHeight = 36.dp
private val PxPerMinute = 3.2f
private val WindowHours = 4
private val SlotMinutes = 30
private val Lima = ZoneId.of("America/Lima")
private val DayFmt = DateTimeFormatter.ofPattern("d MMM", Locale("es", "PE"))
private val ClockFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US).withZone(Lima)

/**
 * Guía TV tipo XUI: días + AHORA + rejilla canales × horarios.
 * Tema oscuro COTAS; tipografía y densidad adaptadas a Google TV.
 */
@Composable
fun ColumnScope.TvGuideBody(
    channels: List<CatalogEntry>,
    guideByStreamId: Map<String, ChannelEpgGuide>,
    loading: Boolean,
    status: String?,
    filterQuery: String,
    dayOffset: Int,
    windowShiftHours: Int,
    onDayOffset: (Int) -> Unit,
    onShiftWindow: (Int) -> Unit,
    onVisibleStreamIds: (List<String>) -> Unit,
    onOpenChannel: (CatalogEntry) -> Unit,
) {
    val filtered = remember(channels, filterQuery) {
        channels.filter { matchesCatalogQuery(it.title, it.subtitle, filterQuery) }
    }
    val (windowStartMs, windowEndMs) = remember(dayOffset, windowShiftHours) {
        guideWindow(dayOffset, windowShiftHours)
    }
    val timelineWidthDp = remember(windowStartMs, windowEndMs) {
        val minutes = ((windowEndMs - windowStartMs) / 60_000L).toFloat().coerceAtLeast(60f)
        (minutes * PxPerMinute).dp
    }
    val slots = remember(windowStartMs, windowEndMs) {
        buildTimeSlots(windowStartMs, windowEndMs)
    }
    val hScroll = rememberScrollState()
    val listState = rememberLazyListState()
    val nowMs = remember { System.currentTimeMillis() }

    LaunchedEffect(listState, filtered) {
        snapshotFlow {
            val info = listState.layoutInfo
            val first = (info.visibleItemsInfo.firstOrNull()?.index ?: 0) - 2
            val last = (info.visibleItemsInfo.lastOrNull()?.index ?: 0) + 6
            val from = first.coerceAtLeast(0)
            val to = last.coerceAtMost(filtered.lastIndex.coerceAtLeast(0))
            if (filtered.isEmpty()) emptyList()
            else filtered.subList(from, (to + 1).coerceAtMost(filtered.size)).map { it.id }
        }
            .distinctUntilChanged()
            .collect { onVisibleStreamIds(it) }
    }

    // Días
    GuideDayStrip(
        selectedOffset = dayOffset,
        onSelect = onDayOffset,
        onPrev = { onDayOffset((dayOffset - 1).coerceAtLeast(-1)) },
        onNext = { onDayOffset((dayOffset + 1).coerceAtMost(6)) },
    )

    if (!status.isNullOrBlank()) {
        Text(
            status,
            color = if (loading) CotasPrimary else CotasMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }

    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GuideBg)
            .border(1.dp, GuideLine, RoundedCornerShape(10.dp)),
    ) {
        // Cabecera tiempos
        Row(
            Modifier
                .fillMaxWidth()
                .height(HeaderHeight)
                .background(GuideHeader),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(ChannelColWidth)
                    .fillMaxHeight()
                    .background(GuideSidebar)
                    .border(width = 0.dp, color = Color.Transparent)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GuideChromeButton("‹", onClick = { onShiftWindow(-2) })
                    Text("CANAL", color = GuideTimeMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    GuideChromeButton("›", onClick = { onShiftWindow(2) })
                }
            }
            Box(
                Modifier
                    .width(NowColWidth)
                    .fillMaxHeight()
                    .background(GuideNowHeader),
                contentAlignment = Alignment.Center,
            ) {
                Text("AHORA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    Modifier
                        .horizontalScroll(hScroll)
                        .width(timelineWidthDp)
                        .fillMaxHeight(),
                ) {
                    slots.forEach { slot ->
                        Box(
                            Modifier
                                .width((SlotMinutes * PxPerMinute).dp)
                                .fillMaxHeight()
                                .border(width = 0.5.dp, color = GuideLine.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                ClockFmt.format(Instant.ofEpochMilli(slot)),
                                color = GuideTimeMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
                // Línea “ahora” en cabecera
                if (dayOffset == 0 && nowMs in windowStartMs until windowEndMs) {
                    val x = ((nowMs - windowStartMs) / 60_000f * PxPerMinute).dp
                    Box(
                        Modifier
                            .offset(x = x)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(CotasCyanFocus),
                    )
                }
            }
        }

        if (filtered.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (filterQuery.isNotBlank()) "Ningún canal coincide con el filtro."
                    else "No hay canales en la guía.",
                    color = CotasMuted,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(filtered, key = { _, c -> c.id }) { index, channel ->
                    val guide = guideByStreamId[channel.id]
                    val rowLoading = loading && guide == null
                    GuideChannelRow(
                        index = index + 1,
                        channel = channel,
                        guide = guide,
                        loading = rowLoading,
                        windowStartMs = windowStartMs,
                        windowEndMs = windowEndMs,
                        timelineWidth = timelineWidthDp,
                        hScroll = hScroll,
                        nowMs = nowMs,
                        showNowLine = dayOffset == 0,
                        onOpen = { onOpenChannel(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideDayStrip(
    selectedOffset: Int,
    onSelect: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val today = remember { LocalDate.now(Lima) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GuideChromeButton("‹", onClick = onPrev)
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (-1..6).forEach { offset ->
                val date = today.plusDays(offset.toLong())
                val relative = when (offset) {
                    -1 -> "AYER"
                    0 -> "HOY"
                    1 -> "MAÑANA"
                    else -> date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "PE"))
                        .uppercase(Locale("es", "PE"))
                }
                val dateLabel = DayFmt.format(date).uppercase(Locale("es", "PE"))
                GuideDayPill(
                    dateLabel = dateLabel,
                    relative = relative,
                    selected = offset == selectedOffset,
                    onClick = { onSelect(offset) },
                )
            }
        }
        GuideChromeButton("›", onClick = onNext)
    }
}

@Composable
private fun GuideDayPill(
    dateLabel: String,
    relative: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .widthIn(min = 88.dp)
            .height(44.dp)
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 6.dp, showIdleOutline = false)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    selected -> GuideDayActive
                    focused -> CotasPrimary.copy(alpha = 0.35f)
                    else -> GuideHeader
                },
            )
            .border(
                width = if (focused || selected) 1.5.dp else 1.dp,
                color = when {
                    focused -> CotasCyanFocus
                    selected -> GuideDayActive
                    else -> GuideLine
                },
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            dateLabel,
            color = if (selected || focused) Color.White else GuideTimeMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            relative,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun GuideChromeButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(28.dp)
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 4.dp, showIdleOutline = false)
            .clip(RoundedCornerShape(4.dp))
            .background(if (focused) CotasPrimary else GuideHeader)
            .border(1.dp, if (focused) CotasCyanFocus else GuideLine, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GuideChannelRow(
    index: Int,
    channel: CatalogEntry,
    guide: ChannelEpgGuide?,
    loading: Boolean,
    windowStartMs: Long,
    windowEndMs: Long,
    timelineWidth: Dp,
    hScroll: androidx.compose.foundation.ScrollState,
    nowMs: Long,
    showNowLine: Boolean,
    onOpen: () -> Unit,
) {
    val programs = remember(guide, windowStartMs, windowEndMs) {
        programsForWindow(guide, windowStartMs, windowEndMs)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .border(width = 0.5.dp, color = GuideLine.copy(alpha = 0.7f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Canal
        Row(
            Modifier
                .width(ChannelColWidth)
                .fillMaxHeight()
                .background(GuideSidebar)
                .clickable(onClick = onOpen)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val logo = rememberPosterRequest(channel.imageUrl, 36.dp, 36.dp)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center,
            ) {
                if (logo != null) {
                    AsyncImage(
                        model = logo,
                        contentDescription = channel.title,
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        channel.title.take(1),
                        color = CotasPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "%03d".format(index.coerceAtMost(999)),
                    color = GuideTimeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    channel.title,
                    color = CotasOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!channel.genre.isNullOrBlank() || !channel.subtitle.isNullOrBlank()) {
                    Text(
                        channel.genre ?: channel.subtitle.orEmpty(),
                        color = GuideTimeMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // AHORA
        GuideProgramCell(
            title = when {
                loading -> "…"
                guide?.now != null -> guide.now!!.title
                guide != null && guide.items.isEmpty() -> "Canal no disponible"
                else -> "—"
            },
            timeLabel = guide?.now?.let { EpgParser.formatRange(it.startAt, it.endAt) },
            width = NowColWidth,
            focusedColor = true,
            dimmed = guide?.now == null && !loading,
            onClick = onOpen,
        )

        // Timeline
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Box(
                Modifier
                    .horizontalScroll(hScroll)
                    .width(timelineWidth)
                    .fillMaxHeight()
                    .background(GuideBg),
            ) {
                if (loading) {
                    GuideProgramCell(
                        title = "Cargando…",
                        timeLabel = null,
                        width = timelineWidth,
                        focusedColor = false,
                        dimmed = true,
                        onClick = onOpen,
                    )
                } else if (programs.isEmpty()) {
                    GuideProgramCell(
                        title = if (guide == null) "…" else "Sin programación",
                        timeLabel = null,
                        width = timelineWidth,
                        focusedColor = false,
                        dimmed = true,
                        onClick = onOpen,
                    )
                } else {
                    programs.forEach { block ->
                        val start = block.startMs.coerceAtLeast(windowStartMs)
                        val end = block.endMs.coerceAtMost(windowEndMs)
                        if (end <= start) return@forEach
                        val x = ((start - windowStartMs) / 60_000f * PxPerMinute).dp
                        val w = ((end - start) / 60_000f * PxPerMinute).dp.coerceAtLeast(48.dp)
                        Box(Modifier.offset(x = x).width(w).fillMaxHeight()) {
                            GuideProgramCell(
                                title = block.title,
                                timeLabel = block.startLabel,
                                width = w,
                                focusedColor = false,
                                dimmed = block.title.equals("SIGN OFF", ignoreCase = true),
                                onClick = onOpen,
                            )
                        }
                    }
                }
                if (showNowLine && nowMs in windowStartMs until windowEndMs) {
                    val x = ((nowMs - windowStartMs) / 60_000f * PxPerMinute).dp
                    Box(
                        Modifier
                            .offset(x = x)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(CotasCyanFocus.copy(alpha = 0.85f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideProgramCell(
    title: String,
    timeLabel: String?,
    width: Dp,
    focusedColor: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 0.dp, showIdleOutline = false)
            .background(
                when {
                    focused -> CotasPrimary.copy(alpha = 0.55f)
                    focusedColor && !dimmed -> GuideCell
                    else -> GuideCellAlt
                },
            )
            .border(0.5.dp, GuideLine)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                title,
                color = when {
                    dimmed -> GuideUnavailable
                    focused -> Color.White
                    else -> CotasOnSurface
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!timeLabel.isNullOrBlank()) {
                Text(
                    timeLabel,
                    color = GuideTimeMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private data class GuideBlock(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val startLabel: String?,
)

private fun guideWindow(dayOffset: Int, shiftHours: Int): Pair<Long, Long> {
    val day = LocalDate.now(Lima).plusDays(dayOffset.toLong())
    val durationMs = WindowHours * 60L * 60L * 1000L
    val baseStart = if (dayOffset == 0) {
        val now = Instant.now().atZone(Lima)
        val minute = if (now.minute >= 30) 30 else 0
        now.withMinute(minute).withSecond(0).withNano(0)
            .minusMinutes(30)
            .plusHours(shiftHours.toLong())
            .toInstant()
            .toEpochMilli()
    } else {
        day.atTime(8, 0).atZone(Lima)
            .plusHours(shiftHours.toLong())
            .toInstant()
            .toEpochMilli()
    }
    // Mantener la ventana dentro del día seleccionado (Lima).
    val dayStart = day.atStartOfDay(Lima).toInstant().toEpochMilli()
    val dayEnd = day.plusDays(1).atStartOfDay(Lima).toInstant().toEpochMilli()
    val start = baseStart.coerceIn(dayStart, dayEnd - durationMs)
    return start to (start + durationMs)
}

private fun buildTimeSlots(windowStartMs: Long, windowEndMs: Long): List<Long> {
    val step = SlotMinutes * 60_000L
    val out = ArrayList<Long>()
    var t = windowStartMs
    while (t < windowEndMs) {
        out += t
        t += step
    }
    return out
}

private fun programsForWindow(
    guide: ChannelEpgGuide?,
    windowStartMs: Long,
    windowEndMs: Long,
): List<GuideBlock> {
    if (guide == null) return emptyList()
    val raw = LinkedHashMap<String, EpgProgram>()
    guide.now?.let { raw[it.key()] = it }
    guide.next?.let { raw[it.key()] = it }
    guide.items.forEach { raw[it.key()] = it }
    val timed = raw.values.mapNotNull { p ->
        val start = EpgParser.parseEpochMs(p.startAt) ?: return@mapNotNull null
        val end = EpgParser.parseEpochMs(p.endAt) ?: (start + 30 * 60_000L)
        if (end <= windowStartMs || start >= windowEndMs) return@mapNotNull null
        GuideBlock(
            title = p.title,
            startMs = start,
            endMs = end,
            startLabel = p.startLabel ?: EpgParser.formatRange(p.startAt, null)?.substringBefore('–')?.trim(),
        )
    }.sortedBy { it.startMs }
    if (timed.isNotEmpty()) return timed

    // Sin horarios fiables: repartir now/next/items en franjas de 30 min.
    val untitled = buildList {
        guide.now?.let { add(it.title) }
        guide.next?.let { add(it.title) }
        guide.items.forEach { add(it.title) }
    }.distinct().ifEmpty { return emptyList() }
    val slot = SlotMinutes * 60_000L
    return untitled.mapIndexed { i, title ->
        val start = windowStartMs + i * slot
        GuideBlock(title = title, startMs = start, endMs = start + slot, startLabel = null)
    }.filter { it.startMs < windowEndMs }
}

private fun EpgProgram.key(): String =
    listOfNotNull(id, startAt, title).joinToString("|")

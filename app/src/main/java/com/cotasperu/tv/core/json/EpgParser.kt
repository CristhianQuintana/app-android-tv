package com.cotasperu.tv.core.json

import com.cotasperu.tv.domain.ChannelEpgGuide
import com.cotasperu.tv.domain.EpgHitStatus
import com.cotasperu.tv.domain.EpgProgram
import com.cotasperu.tv.domain.EpgSearchHit
import com.cotasperu.tv.domain.EpgSearchPage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EpgParser {
    private val lima = ZoneId.of("America/Lima")
    private val clockFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(lima)

    fun parseChannelGuide(json: String): ChannelEpgGuide {
        val root = JsonTree.parse(json)
        val node = root.objectChild("data") ?: root
        val streamId = node.string("streamId")?.trim().orEmpty()
        if (streamId.isEmpty()) throw PayloadException()
        val items = node.arrayChild("items").orEmpty().mapNotNull(::program)
        return ChannelEpgGuide(
            streamId = streamId,
            generatedAt = node.string("generatedAt"),
            now = node.objectChild("now")?.let(::program),
            next = node.objectChild("next")?.let(::program),
            items = items,
        )
    }

    fun parseSearch(json: String): EpgSearchPage {
        val root = JsonTree.parse(json)
        val node = root.objectChild("data") ?: root
        val query = node.string("query")?.trim().orEmpty()
        val page = node.child("page")?.integral()?.toInt()?.coerceAtLeast(1) ?: 1
        val total = node.child("total")?.integral()?.toInt()?.coerceAtLeast(0) ?: 0
        val totalPages = node.child("totalPages")?.integral()?.toInt()?.coerceAtLeast(0) ?: 0
        val items = node.arrayChild("items").orEmpty().mapNotNull(::searchHit)
        return EpgSearchPage(page = page, total = total, totalPages = totalPages, query = query, items = items)
    }

    /** Lote GET /catalog/live/epg?streamIds=… */
    fun parseBatchGuide(json: String): List<ChannelEpgGuide> {
        val root = JsonTree.parse(json)
        val node = root.objectChild("data") ?: root
        val channels = node.arrayChild("channels")
            ?: node.arrayChild("items")
            ?: emptyList()
        return channels.mapNotNull { ch ->
            val streamId = ch.string("streamId")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val items = ch.arrayChild("items").orEmpty().mapNotNull(::program)
            ChannelEpgGuide(
                streamId = streamId,
                generatedAt = ch.string("generatedAt") ?: node.string("generatedAt"),
                now = ch.objectChild("now")?.let(::program),
                next = ch.objectChild("next")?.let(::program),
                items = items,
            )
        }
    }

    fun parseEpochMs(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return parseEpoch(iso)
    }

    fun progressPercent(now: EpgProgram?, atMs: Long = System.currentTimeMillis()): Int? {
        val start = now?.startAt?.let(::parseEpoch) ?: return null
        val end = now.endAt?.let(::parseEpoch) ?: return null
        if (end <= start) return null
        return when {
            atMs < start -> 0
            atMs >= end -> 100
            else -> (((atMs - start).toDouble() / (end - start).toDouble()) * 100.0).toInt().coerceIn(0, 100)
        }
    }

    fun formatRange(startAt: String?, endAt: String?): String? {
        val a = startAt?.let(::formatClock) ?: return null
        val b = endAt?.let(::formatClock) ?: return null
        return "$a – $b"
    }

    private fun program(value: JsonValue): EpgProgram? {
        val title = value.string("title")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val startAt = value.string("startAt")?.trim()?.takeIf { it.isNotEmpty() }
        val endAt = value.string("endAt")?.trim()?.takeIf { it.isNotEmpty() }
        return EpgProgram(
            id = value.string("id")?.trim(),
            title = title.take(240),
            description = value.string("description")?.trim()?.take(800),
            startAt = startAt,
            endAt = endAt,
            startLabel = value.string("start")?.trim() ?: formatClock(startAt),
            endLabel = value.string("end")?.takeIf { !it.all(Char::isDigit) }?.trim() ?: formatClock(endAt),
        )
    }

    private fun searchHit(value: JsonValue): EpgSearchHit? {
        val title = value.string("title")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val startAt = value.string("startAt")?.trim()
        val endAt = value.string("endAt")?.trim()
        val statusRaw = value.string("status")?.trim()?.lowercase()
        val status = when (statusRaw) {
            "live" -> EpgHitStatus.LIVE
            "upcoming" -> EpgHitStatus.UPCOMING
            "ended" -> EpgHitStatus.ENDED
            else -> inferStatus(startAt, endAt)
        }
        val label = value.string("statusLabel")?.trim()?.takeIf { it.isNotEmpty() }
            ?: when (status) {
                EpgHitStatus.LIVE -> "en emisión"
                EpgHitStatus.UPCOMING -> "próximo"
                EpgHitStatus.ENDED -> "finalizado"
            }
        return EpgSearchHit(
            programId = value.string("programId")?.trim(),
            title = title.take(240),
            description = value.string("description")?.trim()?.take(400),
            streamId = value.string("streamId")?.trim(),
            channelName = value.string("channelName")?.trim(),
            startAt = startAt,
            endAt = endAt,
            startLocal = value.string("startLocal")?.trim(),
            endLocal = value.string("endLocal")?.trim(),
            status = status,
            statusLabel = label,
        )
    }

    private fun inferStatus(startAt: String?, endAt: String?, atMs: Long = System.currentTimeMillis()): EpgHitStatus {
        val start = startAt?.let(::parseEpoch)
        val end = endAt?.let(::parseEpoch)
        return when {
            start != null && end != null && atMs in start until end -> EpgHitStatus.LIVE
            start != null && atMs < start -> EpgHitStatus.UPCOMING
            else -> EpgHitStatus.ENDED
        }
    }

    private fun parseEpoch(iso: String): Long? = runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()

    private fun formatClock(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val instant = parseEpoch(iso) ?: return null
        return clockFmt.format(Instant.ofEpochMilli(instant))
    }
}

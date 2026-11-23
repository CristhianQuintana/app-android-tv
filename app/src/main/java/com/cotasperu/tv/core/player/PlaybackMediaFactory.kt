package com.cotasperu.tv.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.cotasperu.tv.domain.PlaybackSession

/**
 * El player usa exactamente [PlaybackSession.streamUrl] del authorize
 * (p. ej. `https://api.cotasperu.com/api/v1/playback/media/{ticket}`).
 * No reescribir a XUI ni a hosts LAN.
 *
 * Mime: solo cuando el hint/URL lo dejan claro. Forzar HLS en tickets proxy
 * sin hint rompe demux de MPEG-TS y suele dejar vídeo sin audio.
 */
object PlaybackMediaFactory {
    fun mediaItemOf(session: PlaybackSession): MediaItem {
        val url = session.streamUrl.trim()
        val builder = MediaItem.Builder()
            .setUri(url)
            .setMediaId(session.targetKey())
            .setTag(session.title)
        mimeFor(url, session.protocolHint)?.let { builder.setMimeType(it) }
        return builder.build()
    }

    /** Visible para tests: decide mime sin tocar el host del streamUrl. */
    fun mimeFor(streamUrl: String, protocolHint: String?): String? {
        val lower = streamUrl.lowercase()
        val hint = protocolHint?.lowercase()?.trim().orEmpty()
        return when (hint) {
            "hls", "m3u8" -> MimeTypes.APPLICATION_M3U8
            "mpegts", "ts", "mp2t" -> MimeTypes.VIDEO_MP2T
            "progressive" -> when {
                lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                lower.endsWith(".mp4") || lower.contains(".mp4?") -> MimeTypes.VIDEO_MP4
                lower.contains(".ts") -> MimeTypes.VIDEO_MP2T
                else -> null // ticket proxy: sniff Content-Type
            }
            "mp4", "progressive-mp4" -> MimeTypes.VIDEO_MP4
            else -> when {
                lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                lower.endsWith(".mp4") || lower.contains(".mp4?") -> MimeTypes.VIDEO_MP4
                lower.contains(".ts") && !lower.contains("/playback/media/") -> MimeTypes.VIDEO_MP2T
                // Proxy ticket sin extensión: no forzar HLS; ExoPlayer usa Content-Type.
                else -> null
            }
        }
    }

    private fun PlaybackSession.targetKey(): String = when (val target = target) {
        is com.cotasperu.tv.domain.PlaybackTarget.Live -> "live:${target.streamId}"
        is com.cotasperu.tv.domain.PlaybackTarget.Vod -> "vod:${target.streamId}"
        is com.cotasperu.tv.domain.PlaybackTarget.SeriesEpisode ->
            "series:${target.seriesId}:${target.seasonNumber}:${target.streamId}"
    }
}

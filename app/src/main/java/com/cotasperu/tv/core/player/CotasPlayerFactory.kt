package com.cotasperu.tv.core.player

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import com.cotasperu.tv.BuildConfig
import com.cotasperu.tv.domain.PlaybackSession

/**
 * ExoPlayer orientado a IPTV en Android TV:
 * - audio siempre habilitado y preferido (AAC > MPEG > AC3)
 * - fallback de decodificador si el hardware falla (AC3/E-AC3)
 * - offload desactivado (passthrough DIRECT en TV suele dejar silencio)
 * - buffers live suficientes para no cortar el audio
 */
object CotasPlayerFactory {

    fun create(context: Context, session: PlaybackSession): ExoPlayer {
        val appContext = context.applicationContext
        // ON: MediaCodec primero; FFmpeg (MP2/AC3/…) solo si el dispositivo no soporta el codec.
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val trackSelector = DefaultTrackSelector(appContext).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setPreferredAudioLanguages("es", "spa", "es-419", "es-PE", "und")
                .setPreferredAudioMimeTypes(
                    MimeTypes.AUDIO_AAC,
                    MimeTypes.AUDIO_MPEG_L2,
                    MimeTypes.AUDIO_MPEG,
                    MimeTypes.AUDIO_AC3,
                    MimeTypes.AUDIO_E_AC3,
                )
                .setAudioOffloadPreferences(
                    TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(
                            TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED,
                        )
                        .build(),
                )
                .build()
        }

        val loadControl = if (session.isLive) {
            // Buffers demasiado cortos → underrun de audio en vivo (corte / silencio intermitente).
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs */ 5_000,
                    /* maxBufferMs */ 15_000,
                    /* bufferForPlaybackMs */ 2_500,
                    /* bufferForPlaybackAfterRebufferMs */ 3_500,
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        } else {
            DefaultLoadControl.Builder()
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }

        // Media proxy público: lecturas largas (≥30–60 s) para segmentos HLS/TS.
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(60_000)
            .setUserAgent("COTAS-TV-Android")

        val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(httpFactory)

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(appContext)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                volume = 1f
                if (BuildConfig.DEBUG) {
                    addAnalyticsListener(EventLogger("CotasPlayer"))
                }
                setMediaItem(PlaybackMediaFactory.mediaItemOf(session))
                playWhenReady = true
            }
    }

    /**
     * Si el selector dejó video sin pista de audio (común con AC3 no soportado
     * o grupos multi-audio), fuerza la primera pista de audio soportada,
     * priorizando AAC/MPEG sobre AC3/E-AC3.
     */
    fun ensureAudioSelected(player: Player, tracks: Tracks) {
        if (BuildConfig.DEBUG) {
            for (group in tracks.groups) {
                if (group.type != C.TRACK_TYPE_AUDIO) continue
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    Log.i(
                        "CotasAudio",
                        "audio[$i] mime=${format.sampleMimeType} ch=${format.channelCount} " +
                            "lang=${format.language} supported=${group.isTrackSupported(i)} " +
                            "selected=${group.isSelected}",
                    )
                }
            }
        }

        val audioAlreadyOn = tracks.groups.any { group ->
            group.type == C.TRACK_TYPE_AUDIO && group.isSelected
        }
        if (audioAlreadyOn) return

        val group = tracks.groups.firstOrNull { candidate ->
            candidate.type == C.TRACK_TYPE_AUDIO &&
                candidate.length > 0 &&
                (0 until candidate.length).any { candidate.isTrackSupported(it) }
        }
        if (group == null) {
            if (BuildConfig.DEBUG) {
                Log.w("CotasAudio", "No supported audio track in current media")
            }
            return
        }

        var bestIndex = -1
        var bestScore = -1
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val mime = group.getTrackFormat(i).sampleMimeType.orEmpty()
            val score = when {
                mime == MimeTypes.AUDIO_AAC || mime.contains("mp4a") -> 100
                mime == MimeTypes.AUDIO_MPEG_L2 || mime == MimeTypes.AUDIO_MPEG -> 80
                mime == MimeTypes.AUDIO_AC3 || mime == MimeTypes.AUDIO_E_AC3 -> 40
                else -> 20
            }
            if (score > bestScore) {
                bestScore = score
                bestIndex = i
            }
        }
        if (bestIndex < 0) return

        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, listOf(bestIndex)),
            )
            .build()
        player.volume = 1f
    }
}

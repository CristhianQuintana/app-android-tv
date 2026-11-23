package com.cotasperu.tv

import androidx.media3.common.MimeTypes
import com.cotasperu.tv.core.player.PlaybackMediaFactory
import com.cotasperu.tv.domain.PlaybackKind
import com.cotasperu.tv.domain.PlaybackSession
import com.cotasperu.tv.domain.PlaybackTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackMediaFactoryTest {
    @Test
    fun buildsDistinctMediaIdsForLiveVodAndSeries() {
        val live = PlaybackMediaFactory.mediaItemOf(
            PlaybackSession(
                title = "Canal demo",
                kind = PlaybackKind.LIVE,
                protocolHint = "hls",
                streamUrl = "http://cdn.example/live/u/p/3537.m3u8",
                target = PlaybackTarget.Live("3537"),
            ),
        )
        val vod = PlaybackMediaFactory.mediaItemOf(
            PlaybackSession(
                title = "Película demo",
                kind = PlaybackKind.VOD,
                protocolHint = "progressive",
                streamUrl = "http://cdn.example/movie/u/p/456.mp4",
                target = PlaybackTarget.Vod("456"),
            ),
        )
        val series = PlaybackMediaFactory.mediaItemOf(
            PlaybackSession(
                title = "Episodio demo",
                kind = PlaybackKind.SERIES,
                protocolHint = "hls",
                streamUrl = "http://cdn.example/series/u/p/3360.m3u8",
                target = PlaybackTarget.SeriesEpisode("3360", "1", 1),
            ),
        )
        assertEquals("live:3537", live.mediaId)
        assertEquals("vod:456", vod.mediaId)
        assertEquals("series:1:1:3360", series.mediaId)
    }

    @Test
    fun buildsMediaItemFromAuthorizeProxyUrlWithoutRewritingHost() {
        val proxy =
            "https://api.cotasperu.com/api/v1/playback/media/56fe70de84484fde94c369dc403f953d"
        val item = PlaybackMediaFactory.mediaItemOf(
            PlaybackSession(
                title = "13 C",
                kind = PlaybackKind.LIVE,
                protocolHint = "hls",
                streamUrl = proxy,
                target = PlaybackTarget.Live("3537"),
            ),
        )
        // streamUrl tal cual (sin reescritura a XUI / LAN).
        assertEquals("live:3537", item.mediaId)
        assertEquals(MimeTypes.APPLICATION_M3U8, PlaybackMediaFactory.mimeFor(proxy, "hls"))
    }

    @Test
    fun proxyTicketWithoutHintDoesNotForceHlsMime() {
        val proxy =
            "https://api.cotasperu.com/api/v1/playback/media/56fe70de84484fde94c369dc403f953d"
        // Sin protocolHint: sniff Content-Type (evitar demux roto / silencio).
        assertNull(PlaybackMediaFactory.mimeFor(proxy, null))
        assertNull(PlaybackMediaFactory.mimeFor(proxy, "progressive"))
        assertEquals(MimeTypes.VIDEO_MP2T, PlaybackMediaFactory.mimeFor(proxy, "mpegts"))
        assertEquals(MimeTypes.APPLICATION_M3U8, PlaybackMediaFactory.mimeFor(proxy, "hls"))
    }
}

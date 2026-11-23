package com.cotasperu.tv.ui

import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cotasperu.tv.R
import kotlinx.coroutines.delay

enum class SplashPhase {
    Playing,
    Blurring,
    Done,
}

/**
 * Arranque en negro → video a pantalla completa → desenfoque oscuro → login.
 * Sin logo ni spinner de carga.
 */
@OptIn(UnstableApi::class)
@Composable
fun SplashIntro(
    phase: SplashPhase,
    onPhase: (SplashPhase) -> Unit,
) {
    if (phase == SplashPhase.Done) return

    val context = LocalContext.current
    var firstFrame by remember { mutableStateOf(false) }
    var videoEnded by remember { mutableStateOf(false) }
    val veil = remember { Animatable(0f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = "android.resource://${context.packageName}/${R.raw.splash_intro}"
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoEnded = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                videoEnded = true
            }
        }
        player.addListener(listener)
        if (player.playbackState == Player.STATE_ENDED) {
            videoEnded = true
        }
        onDispose {
            player.removeListener(listener)
            runCatching { player.release() }
        }
    }

    // Tope por si el decodificador no dispara fin en TV.
    LaunchedEffect(Unit) {
        delay(28_000)
        if (phase == SplashPhase.Playing) {
            videoEnded = true
        }
    }

    LaunchedEffect(videoEnded) {
        if (!videoEnded) return@LaunchedEffect
        if (phase == SplashPhase.Playing) {
            runCatching { player.pause() }
            onPhase(SplashPhase.Blurring)
        }
    }

    LaunchedEffect(phase) {
        if (phase != SplashPhase.Blurring) return@LaunchedEffect
        veil.snapTo(0f)
        veil.animateTo(1f, animationSpec = tween(durationMillis = 1_100))
        onPhase(SplashPhase.Done)
    }

    val blurRadius = (veil.value * 28f).dp
    val darkAlpha = when (phase) {
        SplashPhase.Blurring -> 0.25f + veil.value * 0.75f
        SplashPhase.Playing -> 0f
        SplashPhase.Done -> 1f
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val videoModifier = Modifier
            .fillMaxSize()
            .then(
                if (phase == SplashPhase.Blurring && Build.VERSION.SDK_INT >= 31) {
                    Modifier.blur(blurRadius)
                } else {
                    Modifier
                },
            )

        if (phase == SplashPhase.Playing || phase == SplashPhase.Blurring) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        useArtwork = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        this.player = player
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        setKeepContentOnPlayerReset(true)
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = videoModifier,
                update = { view ->
                    view.player = player
                    // Mantener el último frame durante el desenfoque.
                    if (phase == SplashPhase.Blurring) {
                        view.useController = false
                    }
                },
            )
        }

        // Negro hasta el primer frame (sin flash de logo / artwork).
        if (!firstFrame && phase == SplashPhase.Playing) {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        if (darkAlpha > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = darkAlpha)),
            )
        }
    }
}

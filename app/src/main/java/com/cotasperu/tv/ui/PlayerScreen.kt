package com.cotasperu.tv.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cotasperu.tv.R
import com.cotasperu.tv.core.player.CotasPlayerFactory
import com.cotasperu.tv.domain.PlaybackKind
import com.cotasperu.tv.domain.PlaybackSession
import com.cotasperu.tv.domain.UserMessages
import com.cotasperu.tv.ui.theme.CotasBg
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnPrimary
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimary
import com.cotasperu.tv.ui.theme.cotasFocusRing
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val PlayerSafe = 48.dp
private const val ChromeIdleMs = 4_500L

private enum class PlayerUiState {
    Preparing,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error,
}

@Composable
fun PlayerScreen(
    session: PlaybackSession,
    onExit: () -> Unit,
    onRetryAuthorize: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var uiState by remember(session.streamUrl) { mutableStateOf(PlayerUiState.Preparing) }
    var errorMessage by remember(session.streamUrl) { mutableStateOf<String?>(null) }
    var canRetry by remember(session.streamUrl) { mutableStateOf(true) }
    var showChrome by remember(session.streamUrl) { mutableStateOf(true) }
    var positionMs by remember(session.streamUrl) { mutableLongStateOf(0L) }
    var durationMs by remember(session.streamUrl) { mutableLongStateOf(0L) }
    var activityTick by remember(session.streamUrl) { mutableLongStateOf(0L) }
    val backFocus = remember { FocusRequester() }
    val pauseFocus = remember { FocusRequester() }
    val trapFocus = remember { FocusRequester() }

    val player = remember(session.streamUrl) {
        CotasPlayerFactory.create(context, session).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    uiState = when (playbackState) {
                        Player.STATE_IDLE -> PlayerUiState.Preparing
                        Player.STATE_BUFFERING -> PlayerUiState.Buffering
                        Player.STATE_READY -> if (isPlaying) PlayerUiState.Playing else PlayerUiState.Paused
                        Player.STATE_ENDED -> PlayerUiState.Ended
                        else -> uiState
                    }
                    if (duration > 0) durationMs = duration
                    if (playbackState == Player.STATE_READY) {
                        volume = 1f
                        CotasPlayerFactory.ensureAudioSelected(this@apply, currentTracks)
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    CotasPlayerFactory.ensureAudioSelected(this@apply, tracks)
                    volume = 1f
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (playbackState == Player.STATE_READY) {
                        uiState = if (isPlaying) PlayerUiState.Playing else PlayerUiState.Paused
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val network = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                    val badStatus = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                    canRetry = network || badStatus || error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                    // En vivo: mensaje genérico de mantenimiento (sin detalles técnicos).
                    errorMessage = when {
                        session.isLive -> UserMessages.forLiveChannelDown()
                        network -> UserMessages.forPlaybackNetwork()
                        badStatus -> UserMessages.forPlaybackUnauthorized()
                        else -> UserMessages.forPlaybackFormat()
                    }
                    uiState = PlayerUiState.Error
                }
            })
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.playWhenReady = false
                Lifecycle.Event.ON_START -> {
                    if (uiState != PlayerUiState.Error && uiState != PlayerUiState.Ended) {
                        player.playWhenReady = true
                        player.volume = 1f
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player, session.isLive) {
        if (session.isLive) return@LaunchedEffect
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            val d = player.duration
            if (d > 0) durationMs = d
            delay(500)
        }
    }

    // Auto-ocultar chrome (vivo y VOD) tras inactividad; pausa/fin lo mantienen.
    LaunchedEffect(showChrome, activityTick, uiState) {
        if (!showChrome) return@LaunchedEffect
        if (uiState == PlayerUiState.Paused || uiState == PlayerUiState.Ended) return@LaunchedEffect
        delay(ChromeIdleMs)
        showChrome = false
    }

    LaunchedEffect(showChrome) {
        delay(60)
        try {
            if (showChrome) pauseFocus.requestFocus() else trapFocus.requestFocus()
        } catch (_: IllegalStateException) {
        }
    }

    fun bumpActivity() {
        showChrome = true
        activityTick = System.currentTimeMillis()
    }

    BackHandler { onExit() }

    if (uiState == PlayerUiState.Error) {
        MessageScreen(
            title = if (session.isLive) "Canal en mantenimiento" else "No se pudo reproducir",
            message = errorMessage ?: if (session.isLive) {
                UserMessages.forLiveChannelDown()
            } else {
                UserMessages.forPlaybackFormat()
            },
            primaryLabel = if (canRetry) "Reintentar" else "Volver",
            onPrimary = {
                if (canRetry) onRetryAuthorize() else onExit()
            },
            secondaryLabel = if (canRetry) "Volver" else null,
            onSecondary = if (canRetry) onExit else null,
        )
        return
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
        bumpActivity()
    }

    fun seekBy(deltaMs: Long) {
        if (session.isLive) return
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        val max = if (player.duration > 0) player.duration else Long.MAX_VALUE
        player.seekTo(target.coerceAtMost(max))
        positionMs = player.currentPosition
        bumpActivity()
    }

    fun seekToFraction(fraction: Float) {
        if (session.isLive || durationMs <= 0L) return
        val target = (durationMs * fraction.coerceIn(0f, 1f)).toLong()
        player.seekTo(target)
        positionMs = target
        bumpActivity()
    }

    fun restartPlayback() {
        if (session.isLive) return
        player.seekTo(0L)
        positionMs = 0L
        player.play()
        bumpActivity()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CotasBg)
            .focusRequester(trapFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Back, Key.Escape -> {
                        onExit()
                        true
                    }
                    Key.MediaPlayPause -> {
                        bumpActivity()
                        togglePlay()
                        true
                    }
                    Key.MediaPlay -> {
                        bumpActivity()
                        player.play()
                        true
                    }
                    Key.MediaPause -> {
                        bumpActivity()
                        player.pause()
                        true
                    }
                    else -> {
                        if (!showChrome) {
                            bumpActivity()
                            true
                        } else {
                            bumpActivity()
                            false
                        }
                    }
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    keepScreenOn = true
                }
            },
            update = { view ->
                view.player = player
                view.isFocusable = false
                view.isFocusableInTouchMode = false
            },
        )

        if (uiState == PlayerUiState.Preparing || uiState == PlayerUiState.Buffering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CotasPrimary, modifier = Modifier.size(56.dp))
            }
        }

        if (showChrome || uiState == PlayerUiState.Paused || uiState == PlayerUiState.Ended) {
            PlayerOverlay(
                session = session,
                uiState = uiState,
                positionMs = positionMs,
                durationMs = durationMs,
                backFocus = backFocus,
                pauseFocus = pauseFocus,
                onBack = onExit,
                onToggle = { togglePlay() },
                onMinus10 = { seekBy(-10_000) },
                onPlus10 = { seekBy(10_000) },
                onRestart = { restartPlayback() },
                onSeekFraction = { seekToFraction(it) },
            )
        }
    }
}

@Composable
private fun PlayerOverlay(
    session: PlaybackSession,
    uiState: PlayerUiState,
    positionMs: Long,
    durationMs: Long,
    backFocus: FocusRequester,
    pauseFocus: FocusRequester,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onMinus10: () -> Unit,
    onPlus10: () -> Unit,
    onRestart: () -> Unit,
    onSeekFraction: (Float) -> Unit,
) {
    val kindLabel = when (session.kind) {
        PlaybackKind.LIVE -> "En vivo"
        PlaybackKind.SERIES -> "Serie"
        PlaybackKind.VOD -> "Película"
    }
    val status = when {
        session.isLive && uiState == PlayerUiState.Paused -> "Pausa"
        session.isLive -> "En vivo"
        uiState == PlayerUiState.Paused -> "Pausa"
        uiState == PlayerUiState.Ended -> "Finalizado"
        uiState == PlayerUiState.Buffering -> "Cargando…"
        else -> "Reproduciendo"
    }
    val progress = if (!session.isLive && durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))),
        )

        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = PlayerSafe, vertical = PlayerSafe),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerChip(
                label = "← Volver",
                onClick = onBack,
                focusRequester = backFocus,
                focusDown = pauseFocus,
                emphasized = true,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    session.title,
                    color = CotasOnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("$kindLabel · $status", color = CotasMuted, fontSize = 15.sp, maxLines = 1)
            }
            if (!session.isLive) {
                PlayerChip(
                    label = "Reiniciar reproducción",
                    onClick = onRestart,
                    minWidth = 72.dp,
                    icon = R.drawable.ic_replay,
                )
            }
        }

        // Pausa exactamente al centro del televisor.
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            if (!session.isLive) {
                PlayerChip(label = "−10", onClick = onMinus10, minWidth = 96.dp)
            }
            PlayerPauseButton(
                paused = uiState == PlayerUiState.Paused || uiState == PlayerUiState.Ended,
                onClick = onToggle,
                focusRequester = pauseFocus,
                focusUp = backFocus,
            )
            if (!session.isLive) {
                PlayerChip(label = "+10", onClick = onPlus10, minWidth = 96.dp)
            }
        }

        if (!session.isLive) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = PlayerSafe, vertical = PlayerSafe),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeekBarRow(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    progress = progress,
                    onSeekFraction = onSeekFraction,
                )
            }
        }
    }
}

@Composable
private fun SeekBarRow(
    positionMs: Long,
    durationMs: Long,
    progress: Float,
    onSeekFraction: (Float) -> Unit,
) {
    var barFocused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(positionMs), color = CotasOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(formatTime(durationMs), color = CotasMuted, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .onFocusChanged { barFocused = it.isFocused }
                .cotasFocusRing(barFocused, radius = 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onSeekFraction((progress + 0.05f).coerceIn(0f, 1f)) }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onSeekFraction((progress - 0.05f).coerceIn(0f, 1f))
                            true
                        }
                        Key.DirectionRight -> {
                            onSeekFraction((progress + 0.05f).coerceIn(0f, 1f))
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                    .height(22.dp)
                    .background(Brush.horizontalGradient(listOf(CotasPrimary, Color(0xFF38BDF8)))),
            )
        }
    }
}

@Composable
private fun PlayerChip(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    focusDown: FocusRequester? = null,
    emphasized: Boolean = false,
    minWidth: Dp = 120.dp,
    icon: Int? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = minWidth)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(
                if (focusDown != null) Modifier.focusProperties { down = focusDown } else Modifier,
            )
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    focused -> CotasPrimary
                    emphasized -> Color.White.copy(alpha = 0.92f)
                    else -> Color.Black.copy(alpha = 0.72f)
                },
            )
            .border(
                1.dp,
                if (focused) CotasPrimary else Color.White.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    when {
                        focused -> CotasOnPrimary
                        emphasized -> Color.Black
                        else -> CotasOnSurface
                    },
                ),
            )
        } else {
            Text(
                label,
                color = when {
                    focused -> CotasOnPrimary
                    emphasized -> Color.Black
                    else -> CotasOnSurface
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlayerPauseButton(
    paused: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    focusUp: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(88.dp)
            .focusRequester(focusRequester)
            .focusProperties { up = focusUp }
            .onFocusChanged { focused = it.isFocused }
            .cotasFocusRing(focused, radius = 44.dp)
            .clip(CircleShape)
            .background(if (focused) CotasPrimary else Color.White)
            .border(2.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (paused) "▶" else "❚❚",
            color = if (focused) CotasOnPrimary else Color.Black,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

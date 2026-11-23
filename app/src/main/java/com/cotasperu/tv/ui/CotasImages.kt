package com.cotasperu.tv.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

/** Request Coil acotado al tamaño de UI (evita decodificar posters full-HD en TV). */
@Composable
fun rememberPosterRequest(
    url: String?,
    width: Dp,
    height: Dp,
): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    val wPx = with(density) { width.roundToPx().coerceAtLeast(1) }
    val hPx = with(density) { height.roundToPx().coerceAtLeast(1) }
    return remember(url, wPx, hPx) {
        url?.let { posterImageRequest(context, it, wPx, hPx) }
    }
}

fun posterImageRequest(
    context: Context,
    url: String,
    widthPx: Int,
    heightPx: Int,
): ImageRequest {
    return ImageRequest.Builder(context)
        .data(url)
        .size(widthPx, heightPx)
        .precision(Precision.INEXACT)
        .scale(Scale.FILL)
        .crossfade(false)
        .allowHardware(true)
        .memoryCacheKey("$url@${widthPx}x$heightPx")
        .diskCacheKey(url)
        .build()
}

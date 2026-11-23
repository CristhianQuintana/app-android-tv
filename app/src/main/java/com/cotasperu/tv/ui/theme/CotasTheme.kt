package com.cotasperu.tv.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Tokens alineados a la especificación Samsung COTAS TV (plataforma negra). */
val CotasBg = Color(0xFF000000)
val CotasBgDeep = Color(0xFF0A0A0A)
val CotasBackground = CotasBg
val CotasCategoryRail = Color(0xFF050505)
val CotasHeroMediaEmpty = Color(0xFF111111)
val CotasCardMediaEmpty = Color(0xFF1A1A1A)
val CotasSurface = Color(0xFF141414)
val CotasSurfaceHigh = Color(0xFF1F1F1F)
val CotasSurfaceLow = Color(0xFF000000)
val CotasTopbarStart = Color(0xF20B0E15) // rgba(11,14,21,0.95)
val CotasOnSurface = Color(0xFFF5F5F5)
val CotasPrimaryText = CotasOnSurface
val CotasMuted = Color(0xFFB3B3B3)
val CotasOutline = Color(0xFF333333)
val CotasPrimary = Color(0xFF3B9EFF)
val CotasPrimaryStrong = Color(0xFF1D7FE0)
val CotasSecondary = Color(0xFF7EC8FF)
val CotasFocus = Color(0xFF3B9EFF)
val CotasError = Color(0xFFFFB4AB)
val CotasOrange = Color(0xFFF59E0B)
val CotasResume = Color(0xFFFBBF24)
val CotasStar = Color(0xFFF5D76E)
val CotasOnPrimary = Color(0xFF041018)
val CotasCyanFocus = Color(0xFF2EE6FF)

@Composable
fun CotasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CotasPrimary,
            onPrimary = CotasOnPrimary,
            primaryContainer = CotasPrimaryStrong,
            onPrimaryContainer = CotasOnSurface,
            secondary = CotasSecondary,
            onSecondary = CotasOnPrimary,
            background = CotasBg,
            onBackground = CotasOnSurface,
            surface = CotasSurface,
            onSurface = CotasOnSurface,
            surfaceVariant = CotasSurfaceHigh,
            onSurfaceVariant = CotasMuted,
            border = CotasOutline,
            borderVariant = CotasOutline,
            error = CotasError,
            onError = Color(0xFF690005),
        ),
        content = content,
    )
}

/**
 * Anillo de foco TV. Por defecto solo borde con foco (evita “bloques” permanentes).
 * [showIdleOutline] deja contorno suave en no-foco (pósters).
 */
fun Modifier.cotasFocusRing(
    focused: Boolean,
    radius: Dp = 10.dp,
    selected: Boolean = false,
    showIdleOutline: Boolean = true,
): Modifier {
    val shape = RoundedCornerShape(radius)
    if (!focused) {
        return if (showIdleOutline) this.border(1.dp, CotasOutline.copy(alpha = 0.55f), shape) else this
    }
    return this.border(3.dp, CotasFocus, shape)
}

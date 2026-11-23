package com.cotasperu.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import com.cotasperu.tv.ui.theme.CotasCyanFocus
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimaryStrong

enum class LoginOskTarget { Name, Username, Password }

private val OskPanelBg = Color(0xF2181818)
private val OskKeyBg = Color(0xFF2A2A2A)
private val OskGap = 6.dp
private val OskKeyH = 44.dp
private val OskRadius = 4.dp

@Composable
fun LoginOsk(
    target: LoginOskTarget,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAction: () -> Unit,
    onClose: () -> Unit,
) {
    CotasOsk(
        resetKey = target,
        actionLabel = when (target) {
            LoginOskTarget.Name -> "SIGUIENTE"
            LoginOskTarget.Username -> "SIGUIENTE"
            LoginOskTarget.Password -> "LISTO"
        },
        onChar = onChar,
        onBackspace = onBackspace,
        onClear = onClear,
        onAction = onAction,
        onClose = onClose,
    )
}

private enum class ShiftMode { Off, Once, Lock }

/** Teclado on-screen compartido (login, programas, secciones). */
@Composable
fun CotasOsk(
    actionLabel: String = "LISTO",
    resetKey: Any? = Unit,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAction: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onClose: () -> Unit = {},
) {
    val firstKey = remember { FocusRequester() }
    var shiftMode by remember { mutableStateOf(ShiftMode.Off) }
    LaunchedEffect(resetKey) {
        shiftMode = ShiftMode.Off
        firstKey.requestFocus()
    }

    val upperKeys = shiftMode != ShiftMode.Off

    fun emit(raw: String) {
        val letter = raw.length == 1 && raw[0].isLetter()
        val out = if (upperKeys && letter) raw.uppercase() else raw
        onChar(out)
        // 1 toque MAYÚS: solo la siguiente letra; 2 toques (Lock): se mantiene.
        if (shiftMode == ShiftMode.Once && letter) {
            shiftMode = ShiftMode.Off
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OskPanelBg)
            .border(1.dp, Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(OskGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(OskKeyH * 4 + OskGap * 3),
            horizontalArrangement = Arrangement.spacedBy(OskGap),
        ) {
            Column(
                modifier = Modifier.width(118.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(OskGap),
            ) {
                OskKey(
                    label = when (shiftMode) {
                        ShiftMode.Off -> "MAYÚS"
                        ShiftMode.Once -> "MAYÚS ▲"
                        ShiftMode.Lock -> "BLOQ MAYÚS"
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    active = shiftMode != ShiftMode.Off,
                    onClick = {
                        shiftMode = when (shiftMode) {
                            ShiftMode.Off -> ShiftMode.Once
                            ShiftMode.Once -> ShiftMode.Lock
                            ShiftMode.Lock -> ShiftMode.Off
                        }
                    },
                )
                OskKey(
                    label = "BORRAR",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onClick = onBackspace,
                )
                OskKey(
                    label = "LIMPIAR",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onClick = onClear,
                )
                Spacer(Modifier.weight(1f))
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(OskGap),
            ) {
                OskCharRow(
                    keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                    upper = false,
                    initialFocus = firstKey,
                    onChar = ::emit,
                )
                OskCharRow(
                    keys = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                    upper = upperKeys,
                    onChar = ::emit,
                )
                OskCharRow(
                    keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ñ"),
                    upper = upperKeys,
                    startInset = 18.dp,
                    onChar = ::emit,
                )
                OskCharRow(
                    keys = listOf("z", "x", "c", "v", "b", "n", "m", ".", "-", "_"),
                    upper = upperKeys,
                    startInset = 36.dp,
                    onChar = ::emit,
                )
            }

            OskKey(
                label = actionLabel,
                modifier = Modifier.width(132.dp).fillMaxHeight(),
                primary = true,
                onClick = onAction,
            )
        }

        OskKey(
            label = "ESPACIO",
            modifier = Modifier.fillMaxWidth().height(OskKeyH),
            onClick = { onChar(" ") },
        )
    }
}

@Composable
private fun OskCharRow(
    keys: List<String>,
    onChar: (String) -> Unit,
    upper: Boolean = false,
    initialFocus: FocusRequester? = null,
    startInset: Dp = 0.dp,
) {
    val requesters = remember(keys) {
        List(keys.size) { index ->
            if (index == 0 && initialFocus != null) initialFocus else FocusRequester()
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(OskKeyH),
        horizontalArrangement = Arrangement.spacedBy(OskGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (startInset > 0.dp) Spacer(Modifier.width(startInset))
        keys.forEachIndexed { index, key ->
            val prev = requesters.getOrNull(index - 1)
            val next = requesters.getOrNull(index + 1)
            val label = if (upper && key.any { it.isLetter() }) key.uppercase() else key
            OskKey(
                label = label,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(requesters[index])
                    .focusProperties {
                        if (prev != null) left = prev
                        if (next != null) right = next
                    },
                onClick = { onChar(key) },
            )
        }
        if (startInset > 0.dp) Spacer(Modifier.width(startInset))
    }
}

@Composable
private fun OskKey(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    primary: Boolean = false,
    active: Boolean = false,
) {
    val shape = RoundedCornerShape(OskRadius)
    val idleBg = when {
        primary -> CotasPrimaryStrong
        active -> CotasCyanFocus.copy(alpha = 0.35f)
        else -> OskKeyBg
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = idleBg,
            contentColor = Color.White,
            focusedContainerColor = CotasCyanFocus,
            focusedContentColor = Color.White,
            pressedContainerColor = CotasPrimaryStrong,
            pressedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(
            scale = 1f,
            focusedScale = 1f,
            pressedScale = 1f,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    if (active) CotasCyanFocus else Color.White.copy(alpha = 0.08f),
                ),
                shape = shape,
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape,
            ),
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                fontSize = if (label.length <= 2) 16.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = CotasOnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

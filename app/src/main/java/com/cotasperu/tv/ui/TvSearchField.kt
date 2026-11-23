package com.cotasperu.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cotasperu.tv.R
import com.cotasperu.tv.ui.theme.CotasCyanFocus
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimary
import kotlinx.coroutines.delay

private val SearchFieldBg = Color.Black
private val SearchCapsule = RoundedCornerShape(999.dp)
private val SearchBorder = Color.White.copy(alpha = 0.90f)

/** Barra cápsula: fondo negro, borde blanco, lupa a la izquierda. */
@Composable
fun TvSearchField(
    value: String,
    placeholder: String,
    active: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
    leftFocus: FocusRequester? = null,
    height: Dp = 36.dp,
    maxWidth: Dp = 340.dp,
    expand: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val showRing = focused || active
    val borderColor = if (showRing) CotasCyanFocus else SearchBorder

    Row(
        modifier = modifier
            .then(if (expand) Modifier.fillMaxWidth() else Modifier)
            .widthIn(max = maxWidth)
            .height(height)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                rightFocus?.let { right = it }
                leftFocus?.let { left = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onActivate(); true
                    }
                    else -> false
                }
            }
            .clip(SearchCapsule)
            .background(SearchFieldBg)
            .border(
                width = if (showRing) 2.dp else 1.dp,
                color = borderColor,
                shape = SearchCapsule,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onActivate,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(if (showRing) CotasCyanFocus else Color.White),
        )
        Text(
            text = value.ifEmpty { placeholder },
            color = if (value.isEmpty()) CotasMuted else CotasOnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
        )
    }
}

/**
 * Fila Canales: barra + desplegable Canal/Programa.
 * Tras cambiar modo, [dropdownFocusRequester] recibe el foco (no el menú lateral).
 * Izquierda desde el desplegable → barra de búsqueda.
 */
@Composable
fun LiveSearchRow(
    mode: LiveMode,
    query: String,
    oskOpen: Boolean,
    onActivateSearch: () -> Unit,
    onMode: (LiveMode) -> Unit,
    focusRequester: FocusRequester,
    dropdownFocusRequester: FocusRequester,
    @Suppress("UNUSED_PARAMETER") requestDropdownFocus: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().zIndex(2f),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvSearchField(
            value = query,
            placeholder = if (mode == LiveMode.CHANNELS) "Buscar canal…" else "Filtrar canal en guía…",
            active = oskOpen,
            onActivate = onActivateSearch,
            focusRequester = focusRequester,
            rightFocus = dropdownFocusRequester,
            modifier = Modifier.weight(1f),
            maxWidth = 520.dp,
            expand = true,
        )
        LiveModeDropdown(
            mode = mode,
            focusRequester = dropdownFocusRequester,
            leftFocus = focusRequester,
            onMode = onMode,
        )
    }
}

@Composable
private fun LiveModeDropdown(
    mode: LiveMode,
    focusRequester: FocusRequester,
    leftFocus: FocusRequester,
    onMode: (LiveMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var triggerFocused by remember { mutableStateOf(false) }
    var wasExpanded by remember { mutableStateOf(false) }
    val canalOptionFocus = remember { FocusRequester() }
    val programaOptionFocus = remember { FocusRequester() }
    BackHandler(enabled = expanded) { expanded = false }

    // Al cerrar el menú (p. ej. elegir Programa), recuperar el foco en el trigger.
    LaunchedEffect(expanded) {
        if (expanded) {
            wasExpanded = true
            return@LaunchedEffect
        }
        if (!wasExpanded) return@LaunchedEffect
        delay(32)
        try {
            focusRequester.requestFocus()
        } catch (_: IllegalStateException) {
        }
    }

    Box(Modifier.width(148.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .focusRequester(focusRequester)
                .focusProperties {
                    left = leftFocus
                    // Evitar que “izquierda” del menú lateral robe el foco desde aquí.
                    up = FocusRequester.Cancel
                }
                .onFocusChanged { triggerFocused = it.isFocused }
                .clip(SearchCapsule)
                .background(SearchFieldBg)
                .border(
                    width = if (triggerFocused || expanded) 2.dp else 1.dp,
                    color = when {
                        triggerFocused || expanded -> CotasCyanFocus
                        else -> SearchBorder
                    },
                    shape = SearchCapsule,
                )
                .clickable { expanded = !expanded }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            expanded = !expanded
                            true
                        }
                        Key.DirectionDown -> {
                            if (!expanded) {
                                expanded = true
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionLeft -> {
                            // Forzar barra de búsqueda (no el rail de navegación).
                            try {
                                leftFocus.requestFocus()
                            } catch (_: IllegalStateException) {
                            }
                            true
                        }
                        else -> false
                    }
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = when (mode) {
                    LiveMode.CHANNELS -> "Canal ▾"
                    LiveMode.PROGRAMS -> "Programa ▾"
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        if (expanded) {
            LaunchedEffect(mode) {
                delay(16)
                try {
                    when (mode) {
                        LiveMode.CHANNELS -> canalOptionFocus.requestFocus()
                        LiveMode.PROGRAMS -> programaOptionFocus.requestFocus()
                    }
                } catch (_: IllegalStateException) {
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 38.dp)
                    .fillMaxWidth()
                    .zIndex(8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xF0121212))
                    .border(1.dp, SearchBorder, RoundedCornerShape(8.dp)),
            ) {
                DropdownOption(
                    label = "Canal",
                    selected = mode == LiveMode.CHANNELS,
                    focusRequester = canalOptionFocus,
                    onClick = {
                        expanded = false
                        onMode(LiveMode.CHANNELS)
                    },
                )
                DropdownOption(
                    label = "Programa",
                    selected = mode == LiveMode.PROGRAMS,
                    focusRequester = programaOptionFocus,
                    onClick = {
                        expanded = false
                        onMode(LiveMode.PROGRAMS)
                    },
                )
            }
        }
    }
}

@Composable
private fun DropdownOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                // Dentro del menú, izquierda cierra hacia el trigger vía Back / selección.
                left = FocusRequester.Cancel
            }
            .onFocusChanged { focused = it.isFocused }
            .background(
                when {
                    focused -> CotasCyanFocus.copy(alpha = 0.28f)
                    selected -> CotasPrimary.copy(alpha = 0.22f)
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onClick(); true
                    }
                    else -> false
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            label,
            color = when {
                focused -> Color.White
                selected -> CotasCyanFocus
                else -> Color.White.copy(alpha = 0.90f)
            },
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Filtro local de grilla (canales / películas / series). */
fun matchesCatalogQuery(title: String, subtitle: String?, query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    if (title.lowercase().contains(needle)) return true
    if (subtitle?.lowercase()?.contains(needle) == true) return true
    val plainTitle = title.lowercase().stripAccents()
    val plainNeedle = needle.stripAccents()
    if (plainTitle.contains(plainNeedle)) return true
    val plainSub = subtitle?.lowercase()?.stripAccents()
    return plainSub?.contains(plainNeedle) == true
}

private fun String.stripAccents(): String {
    val map = mapOf(
        'á' to 'a', 'à' to 'a', 'ä' to 'a', 'â' to 'a',
        'é' to 'e', 'è' to 'e', 'ë' to 'e', 'ê' to 'e',
        'í' to 'i', 'ì' to 'i', 'ï' to 'i', 'î' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ö' to 'o', 'ô' to 'o',
        'ú' to 'u', 'ù' to 'u', 'ü' to 'u', 'û' to 'u',
        'ñ' to 'n', 'ç' to 'c',
    )
    return buildString(length) {
        for (c in this@stripAccents) append(map[c] ?: c)
    }
}

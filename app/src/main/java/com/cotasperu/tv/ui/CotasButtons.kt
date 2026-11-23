package com.cotasperu.tv.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale

/** Sin scale al foco (paridad Samsung / mando TV). */
fun cotasButtonScale(): ButtonScale =
    ButtonDefaults.scale(scale = 1f, focusedScale = 1f, pressedScale = 1f, disabledScale = 1f)

/**
 * Etiqueta centrada dentro de [androidx.tv.material3.Button].
 * [fillWidth]=true solo en botones a ancho completo; si no, el botón no se estira.
 */
@Composable
fun RowScope.CotasButtonLabel(
    text: String,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    fillWidth: Boolean = false,
) {
    Text(
        text = text,
        modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
    )
}

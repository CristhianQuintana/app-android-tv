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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import com.cotasperu.tv.R
import com.cotasperu.tv.ui.theme.CotasBgDeep
import com.cotasperu.tv.ui.theme.CotasCyanFocus
import com.cotasperu.tv.ui.theme.CotasMuted
import com.cotasperu.tv.ui.theme.CotasOnPrimary
import com.cotasperu.tv.ui.theme.CotasOnSurface
import com.cotasperu.tv.ui.theme.CotasPrimary
import com.cotasperu.tv.ui.theme.CotasPrimaryStrong
import com.cotasperu.tv.ui.theme.CotasSurface
import com.cotasperu.tv.ui.theme.CotasSurfaceHigh
import com.cotasperu.tv.ui.theme.cotasFocusRing

private val LoginFieldFill = Color(0xE62C2C2C)
private val LoginCardBg = Color(0xB3121212)
private val LoginCardBorder = Color.White.copy(alpha = 0.14f)
private val LoginRadius = 14.dp
private val LoginScrim = Color.Black.copy(alpha = 0.38f)

@Composable
fun AccessSelectorScreen(
    onStreamCode: () -> Unit,
    onLogin: () -> Unit,
) {
    val streamFocus = remember { FocusRequester() }
    val loginFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { streamFocus.requestFocus() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / 1920.dp, maxHeight / 1080.dp).coerceIn(0.5f, 1.0f)
        Image(
            painter = painterResource(R.drawable.login_collage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(LoginScrim))
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp)
                .size(90.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.08f))
                .border(2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_access_user),
                contentDescription = "Usuario",
                modifier = Modifier.size(66.dp),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandLogo(Modifier.width((760f * scale).dp).height((132f * scale).dp))
            Spacer(Modifier.height((18f * scale).dp))
            Text(
                "¿Cómo quieres acceder?",
                color = CotasOnSurface,
                fontSize = (56f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Elige un método para continuar",
                color = CotasMuted,
                fontSize = (26f * scale).sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height((34f * scale).dp))
            Row(horizontalArrangement = Arrangement.spacedBy((120f * scale).dp)) {
                SelectorCard(
                    modifier = Modifier.focusRequester(streamFocus),
                    title = "Stream Code",
                    backgroundRes = R.drawable.gateway_stream,
                    iconRes = R.drawable.access_stream,
                    scale = scale,
                    rightFocus = loginFocus,
                    onClick = onStreamCode,
                )
                SelectorCard(
                    modifier = Modifier.focusRequester(loginFocus),
                    title = "LOGIN",
                    backgroundRes = R.drawable.gateway_login,
                    iconRes = R.drawable.access_login,
                    scale = scale,
                    leftFocus = streamFocus,
                    onClick = onLogin,
                )
            }
        }
    }
}

@Composable
private fun SelectorCard(
    modifier: Modifier = Modifier,
    title: String,
    backgroundRes: Int,
    iconRes: Int,
    scale: Float,
    leftFocus: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .size((620f * scale).dp, (400f * scale).dp)
            .clip(shape)
            .focusProperties {
                leftFocus?.let { left = it }
                rightFocus?.let { right = it }
            }
            .border(
                width = if (focused) 4.dp else 4.dp,
                color = if (focused) CotasCyanFocus else Color.White.copy(alpha = 0.14f),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0.10f to Color.Black.copy(alpha = 0.10f),
                        0.40f to Color.Black.copy(alpha = 0.25f),
                        1.0f to Color.Black.copy(alpha = 0.70f),
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = (44f * scale).dp)
                .size((150f * scale).dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (focused) Color(0xCC082838) else Color.Black.copy(alpha = 0.62f)
                )
                .border(
                    3.dp,
                    if (focused) CotasCyanFocus else Color.White.copy(alpha = 0.40f),
                    RoundedCornerShape(50),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size((96f * scale).dp),
            )
        }
        Text(
            title,
            color = CotasOnSurface,
            fontSize = (54f * scale).sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding((42f * scale).dp),
        )
    }
}

@Composable
fun BootScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CotasBgDeep)
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLogo(Modifier.width(220.dp).height(72.dp))
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(color = CotasPrimary)
        Spacer(Modifier.height(18.dp))
        Text(message, color = Color.White.copy(alpha = 0.72f), fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun MessageScreen(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CotasBgDeep)
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLogo()
        Spacer(Modifier.height(24.dp))
        Text(title, color = CotasOnSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = CotasMuted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 720.dp),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier.focusRequester(focus).testTag("primary_action"),
            colors = primaryButtonColors(),
            scale = cotasButtonScale(),
        ) {
            CotasButtonLabel(primaryLabel, fontSize = 18.sp)
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSecondary, colors = primaryButtonColors(), scale = cotasButtonScale()) {
                CotasButtonLabel(secondaryLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun LoginScreen(
    username: String,
    password: String,
    registrationMode: Boolean = false,
    registrationName: String = "",
    message: String?,
    submitting: Boolean,
    locked: Boolean = false,
    keepSession: Boolean,
    qrLinkCode: String?,
    qrPayload: String?,
    qrStatusText: String?,
    qrExpiresInSec: Int,
    qrLoading: Boolean,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRegistrationName: (String) -> Unit = {},
    onKeepSession: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onToggleRegistration: () -> Unit = {},
    onRegenerateQr: () -> Unit,
) {
    val userFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val registrationFocus = remember { FocusRequester() }
    val keepFocus = remember { FocusRequester() }
    val submitFocus = remember { FocusRequester() }
    val refreshQrFocus = remember { FocusRequester() }
    var oskTarget by remember { mutableStateOf<LoginOskTarget?>(null) }
    val fieldsEnabled = !submitting && !locked
    val canSubmit = fieldsEnabled && username.isNotBlank() && password.isNotEmpty()
    val oskOpen = oskTarget != null && fieldsEnabled
    val qrBitmap: ImageBitmap? = remember(qrPayload) {
        qrPayload?.let { encodeQrImage(it, sizePx = 512) }
    }

    LaunchedEffect(Unit) { userFocus.requestFocus() }
    BackHandler(enabled = oskOpen) { oskTarget = null }

    fun submitIfReady() {
        if (canSubmit) onSubmit()
    }

    fun appendChar(ch: String) {
        when (oskTarget) {
            LoginOskTarget.Name -> onRegistrationName((registrationName + ch).take(60))
            LoginOskTarget.Username -> onUsername((username + ch).take(64))
            LoginOskTarget.Password -> onPassword((password + ch).take(64))
            null -> Unit
        }
    }

    fun backspace() {
        when (oskTarget) {
            LoginOskTarget.Name -> if (registrationName.isNotEmpty()) onRegistrationName(registrationName.dropLast(1))
            LoginOskTarget.Username -> if (username.isNotEmpty()) onUsername(username.dropLast(1))
            LoginOskTarget.Password -> if (password.isNotEmpty()) onPassword(password.dropLast(1))
            null -> Unit
        }
    }

    fun oskAction() {
        when (oskTarget) {
            LoginOskTarget.Name -> {
                oskTarget = LoginOskTarget.Username
                userFocus.requestFocus()
            }
            LoginOskTarget.Username -> {
                oskTarget = LoginOskTarget.Password
                passwordFocus.requestFocus()
            }
            LoginOskTarget.Password -> {
                oskTarget = null
                if (canSubmit) onSubmit() else submitFocus.requestFocus()
            }
            null -> Unit
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // En TV 1080p densidades ~2.0 → ~960 dp de ancho; layout siempre en Row.
        val scale = minOf(maxWidth / 960.dp, maxHeight / 540.dp).coerceIn(0.75f, 1.35f)
        val ui = scale
        // QR ~300–340 px físicos (independiente de density).
        val density = LocalDensity.current
        val qrSize = with(density) { (310f * scale).toDp() }.coerceIn(150.dp, 360.dp)

        Image(
            painter = painterResource(R.drawable.login_collage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        Box(Modifier.fillMaxSize().background(LoginScrim))

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = (36f * scale).dp,
                    end = (36f * scale).dp,
                    top = (28f * scale).dp,
                    bottom = if (oskOpen) (240f * scale).dp else (28f * scale).dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 40 % — login tradicional (izquierda)
            Box(
                Modifier.weight(0.40f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                LoginTraditionalColumn(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    ui = ui,
                    username = username,
                    password = password,
                    registrationMode = registrationMode,
                    registrationName = registrationName,
                    message = message,
                    submitting = submitting,
                    locked = locked,
                    keepSession = keepSession,
                    fieldsEnabled = fieldsEnabled,
                    canSubmit = canSubmit,
                    oskOpen = oskOpen,
                    oskTarget = oskTarget,
                    userFocus = userFocus,
                    passwordFocus = passwordFocus,
                    nameFocus = nameFocus,
                    registrationFocus = registrationFocus,
                    keepFocus = keepFocus,
                    submitFocus = submitFocus,
                    qrFocus = refreshQrFocus,
                    onActivateName = { oskTarget = LoginOskTarget.Name },
                    onActivateUser = { oskTarget = LoginOskTarget.Username },
                    onActivatePassword = { oskTarget = LoginOskTarget.Password },
                    onKeepSession = onKeepSession,
                    onToggleRegistration = onToggleRegistration,
                    onSubmit = {
                        oskTarget = null
                        submitIfReady()
                    },
                )
            }
            // 60 % — QR + código (derecha)
            Box(
                Modifier.weight(0.60f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (!oskOpen) {
                    LoginQrColumn(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        ui = ui,
                        qrSize = qrSize,
                        qrBitmap = qrBitmap,
                        linkCode = qrLinkCode,
                        statusText = qrStatusText,
                        expiresInSec = qrExpiresInSec,
                        loading = qrLoading,
                        refreshFocus = refreshQrFocus,
                        leftFocus = submitFocus,
                        onRegenerate = onRegenerateQr,
                    )
                }
            }
        }

        if (oskTarget != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = (40f * scale).dp, end = (40f * scale).dp, bottom = (12f * scale).dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                LoginOsk(
                    target = oskTarget!!,
                    onChar = { appendChar(it) },
                    onBackspace = { backspace() },
                    onClear = {
                        when (oskTarget) {
                            LoginOskTarget.Name -> onRegistrationName("")
                            LoginOskTarget.Username -> onUsername("")
                            LoginOskTarget.Password -> onPassword("")
                            null -> Unit
                        }
                    },
                    onAction = { oskAction() },
                    onClose = { oskTarget = null },
                )
            }
        }
    }
}

@Composable
private fun LoginTraditionalColumn(
    modifier: Modifier,
    ui: Float,
    username: String,
    password: String,
    registrationMode: Boolean,
    registrationName: String,
    message: String?,
    submitting: Boolean,
    locked: Boolean,
    keepSession: Boolean,
    fieldsEnabled: Boolean,
    canSubmit: Boolean,
    oskOpen: Boolean,
    oskTarget: LoginOskTarget?,
    userFocus: FocusRequester,
    passwordFocus: FocusRequester,
    nameFocus: FocusRequester,
    registrationFocus: FocusRequester,
    keepFocus: FocusRequester,
    submitFocus: FocusRequester,
    qrFocus: FocusRequester,
    onActivateName: () -> Unit,
    onActivateUser: () -> Unit,
    onActivatePassword: () -> Unit,
    onKeepSession: (Boolean) -> Unit,
    onToggleRegistration: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(LoginRadius))
            .background(LoginCardBg)
            .border(1.dp, LoginCardBorder, RoundedCornerShape(LoginRadius))
            .padding(
                start = (36f * ui).dp,
                top = (28f * ui).dp,
                end = (36f * ui).dp,
                bottom = (28f * ui).dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLogo(Modifier.width((220f * ui).dp).height((62f * ui).dp))
        Spacer(Modifier.height((18f * ui).dp))
        Text(
            if (registrationMode) "Regístrate" else "Iniciar sesión",
            color = Color.White,
            fontSize = (26f * ui).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height((18f * ui).dp))
        if (registrationMode) {
            FieldLabel("Nombre", (13f * ui).sp)
            LoginFieldBox(
                value = registrationName,
                masked = false,
                enabled = fieldsEnabled,
                active = oskTarget == LoginOskTarget.Name,
                height = (52f * ui).dp,
                textSize = (18f * ui).sp,
                focusRequester = nameFocus,
                nextFocus = userFocus,
                previousFocus = null,
                testTag = "register_name",
                onActivate = onActivateName,
                onDown = { if (!oskOpen) userFocus.requestFocus() },
            )
            Spacer(Modifier.height((14f * ui).dp))
        }
        FieldLabel(if (registrationMode) "Correo" else "Usuario", (13f * ui).sp)
        LoginFieldBox(
            value = username,
            masked = false,
            enabled = fieldsEnabled,
            active = oskTarget == LoginOskTarget.Username,
            height = (52f * ui).dp,
            textSize = (18f * ui).sp,
            focusRequester = userFocus,
            nextFocus = passwordFocus,
            previousFocus = null,
            testTag = "login_username",
            onActivate = onActivateUser,
            onDown = { if (!oskOpen) passwordFocus.requestFocus() },
        )
        Spacer(Modifier.height((14f * ui).dp))
        FieldLabel("Contraseña", (13f * ui).sp)
        LoginFieldBox(
            value = password,
            masked = true,
            enabled = fieldsEnabled,
            active = oskTarget == LoginOskTarget.Password,
            height = (52f * ui).dp,
            textSize = (18f * ui).sp,
            focusRequester = passwordFocus,
            nextFocus = submitFocus,
            previousFocus = userFocus,
            testTag = "login_password",
            onActivate = onActivatePassword,
            onDown = { if (!oskOpen) submitFocus.requestFocus() },
            onUp = { if (!oskOpen) userFocus.requestFocus() },
        )
        if (registrationMode) {
            Spacer(Modifier.height((8f * ui).dp))
            Text(
                "La contraseña debe tener 12 caracteres o más.",
                color = Color(0xFFF2C14E),
                fontSize = (12f * ui).sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            submitting -> {
                Spacer(Modifier.height((10f * ui).dp))
                Text(
                    "Ingresando…",
                    color = Color.White,
                    fontSize = (14f * ui).sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().testTag("login_message"),
                )
            }
            !message.isNullOrBlank() -> {
                Spacer(Modifier.height((10f * ui).dp))
                Text(
                    message,
                    color = Color.White,
                    fontSize = (14f * ui).sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().testTag("login_message"),
                )
            }
        }
        Spacer(Modifier.height((18f * ui).dp))
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height((52f * ui).dp)
                .focusRequester(submitFocus)
                .focusProperties {
                    previous = passwordFocus
                    next = keepFocus
                    right = qrFocus
                }
                .testTag("login_submit"),
            scale = cotasButtonScale(),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.colors(
                containerColor = CotasPrimaryStrong,
                contentColor = Color.White,
                focusedContainerColor = CotasCyanFocus,
                focusedContentColor = Color.White,
                pressedContainerColor = CotasPrimary,
                pressedContentColor = Color.White,
                disabledContainerColor = CotasPrimaryStrong.copy(alpha = 0.45f),
                disabledContentColor = Color.White.copy(alpha = 0.70f),
            ),
        ) {
            Text(
                text = when {
                    submitting -> "Ingresando…"
                    locked -> "Espera…"
                    registrationMode -> "Registrarme"
                    else -> "Iniciar sesión"
                },
                color = Color.White,
                fontSize = (18f * ui).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height((12f * ui).dp))
        KeepSessionToggle(
            keepSession = keepSession,
            enabled = fieldsEnabled,
            height = (40f * ui).dp,
            textSize = (14f * ui).sp,
            focusRequester = keepFocus,
            previousFocus = submitFocus,
            nextFocus = qrFocus,
            onToggle = { onKeepSession(!keepSession) },
        )
        Spacer(Modifier.height((8f * ui).dp))
        Button(
            onClick = onToggleRegistration,
            modifier = Modifier
                .fillMaxWidth()
                .height((40f * ui).dp)
                .focusRequester(registrationFocus),
            colors = ButtonDefaults.colors(
                containerColor = Color(0x736D6D6E),
                contentColor = Color.White,
                focusedContainerColor = CotasCyanFocus,
                focusedContentColor = Color.White,
            ),
        ) {
            Text(
                if (registrationMode) "Ya tengo cuenta · Iniciar sesión" else "Registrarme",
                fontSize = (14f * ui).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LoginQrColumn(
    modifier: Modifier,
    ui: Float,
    qrSize: Dp,
    qrBitmap: ImageBitmap?,
    linkCode: String?,
    statusText: String?,
    expiresInSec: Int,
    loading: Boolean,
    refreshFocus: FocusRequester,
    leftFocus: FocusRequester,
    onRegenerate: () -> Unit,
) {
    var refreshFocused by remember { mutableStateOf(false) }
    val displayStatus = when {
        statusText?.contains("Conectado", ignoreCase = true) == true -> "Dispositivo conectado"
        statusText?.contains("Esperando", ignoreCase = true) == true -> "Esperando conexión..."
        !statusText.isNullOrBlank() -> statusText
        else -> "Esperando conexión..."
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(LoginRadius))
            .background(LoginCardBg)
            .border(1.dp, LoginCardBorder, RoundedCornerShape(LoginRadius))
            .padding(horizontal = (36f * ui).dp, vertical = (32f * ui).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Conecta tu dispositivo",
            color = Color.White,
            fontSize = (28f * ui).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height((10f * ui).dp))
        Text(
            "Escanea el código QR desde tu celular para iniciar sesión en tu TV",
            color = Color.White.copy(alpha = 0.86f),
            fontSize = (15f * ui).sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        Spacer(Modifier.height((22f * ui).dp))
        Box(
            Modifier
                .size(qrSize)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding((14f * ui).dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                qrBitmap != null -> Image(
                    bitmap = qrBitmap,
                    contentDescription = "Código QR para vincular TV",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                loading -> CircularProgressIndicator(color = CotasPrimary, modifier = Modifier.size(40.dp))
                else -> Text("QR", color = CotasMuted, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height((18f * ui).dp))
        Text(
            linkCode ?: "————————",
            color = Color.White,
            fontSize = (30f * ui).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("login_qr_code"),
        )
        Spacer(Modifier.height((10f * ui).dp))
        Text(
            displayStatus,
            color = Color.White,
            fontSize = (15f * ui).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Spacer(Modifier.height((6f * ui).dp))
        Text(
            if (expiresInSec > 0) {
                "Confirma en la app COTAS · expira en ${expiresInSec}s"
            } else {
                "Confirma la vinculación en la app COTAS de tu celular"
            },
            color = CotasMuted,
            fontSize = (13f * ui).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Spacer(Modifier.height((16f * ui).dp))
        Box(
            Modifier
                .height((42f * ui).dp)
                .widthIn(min = (180f * ui).dp)
                .focusRequester(refreshFocus)
                .focusProperties {
                    left = leftFocus
                    previous = leftFocus
                }
                .onFocusChanged { refreshFocused = it.isFocused }
                .cotasFocusRing(refreshFocused, radius = 8.dp, showIdleOutline = false)
                .clip(RoundedCornerShape(8.dp))
                .background(if (refreshFocused) CotasPrimary else Color(0xCC2A2A2A))
                .border(1.dp, if (refreshFocused) CotasCyanFocus else LoginCardBorder, RoundedCornerShape(8.dp))
                .clickable(onClick = onRegenerate)
                .padding(horizontal = 18.dp)
                .testTag("login_qr_refresh"),
            contentAlignment = Alignment.Center,
        ) {
            Text("Nuevo código", color = Color.White, fontSize = (14f * ui).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier.height(72.dp),
    logoRes: Int = R.drawable.cotas_logo,
) {
    Image(
        painter = painterResource(logoRes),
        contentDescription = stringResource(R.string.logo_content_description),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun FieldLabel(text: String, size: TextUnit = 16.sp) {
    Text(
        text.uppercase(),
        color = Color.White.copy(alpha = 0.88f),
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    )
}

@Composable
private fun LoginFieldBox(
    value: String,
    masked: Boolean,
    enabled: Boolean,
    active: Boolean,
    height: Dp,
    textSize: TextUnit,
    focusRequester: FocusRequester,
    nextFocus: FocusRequester?,
    previousFocus: FocusRequester?,
    testTag: String,
    onActivate: () -> Unit,
    onDown: () -> Unit,
    onUp: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val showRing = focused || active
    val display = when {
        value.isEmpty() -> ""
        masked -> "•".repeat(value.length.coerceAtMost(24))
        else -> value
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .focusRequester(focusRequester)
            .focusProperties {
                nextFocus?.let { next = it }
                previousFocus?.let { previous = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onActivate(); true
                    }
                    Key.DirectionDown, Key.Tab -> {
                        onDown(); true
                    }
                    Key.DirectionUp -> {
                        onUp?.invoke(); onUp != null
                    }
                    else -> false
                }
            }
            .clip(RoundedCornerShape(LoginRadius))
            .background(LoginFieldFill)
            .border(
                width = if (showRing) 2.dp else 1.dp,
                color = if (showRing) CotasCyanFocus else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(LoginRadius),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onActivate,
            )
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = display.ifEmpty { if (masked) "Contraseña" else "Usuario" },
            color = if (display.isEmpty()) CotasMuted else CotasOnSurface,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun KeepSessionToggle(
    keepSession: Boolean,
    enabled: Boolean,
    height: Dp,
    textSize: TextUnit,
    focusRequester: FocusRequester,
    previousFocus: FocusRequester,
    nextFocus: FocusRequester,
    onToggle: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .focusRequester(focusRequester)
            .focusProperties {
                next = nextFocus
                previous = previousFocus
            }
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = when {
                    focused -> CotasCyanFocus
                    keepSession -> CotasPrimary
                    else -> Color.White.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(999.dp),
            )
            .background(
                if (keepSession) CotasCyanFocus.copy(alpha = 0.16f) else CotasSurfaceHigh,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Mantener sesión: ${if (keepSession) "Sí" else "No"}",
            color = if (keepSession || focused) CotasCyanFocus else CotasMuted,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun primaryButtonColors() = ButtonDefaults.colors(
    containerColor = CotasPrimaryStrong,
    contentColor = CotasOnPrimary,
    focusedContainerColor = CotasCyanFocus,
    focusedContentColor = CotasOnPrimary,
    pressedContainerColor = CotasPrimary,
    pressedContentColor = CotasOnPrimary,
    disabledContainerColor = CotasSurface,
    disabledContentColor = CotasMuted,
)

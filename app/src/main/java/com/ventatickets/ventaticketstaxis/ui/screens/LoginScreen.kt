package com.ventatickets.ventaticketstaxis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ventatickets.ventaticketstaxis.ui.viewmodels.LoginViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.ventatickets.ventaticketstaxis.data.ServiceLocator
import com.ventatickets.ventaticketstaxis.data.AppConfig
import com.ventatickets.ventaticketstaxis.ui.theme.Dimensions
import com.ventatickets.ventaticketstaxis.ui.components.FullScreenDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.sp
import com.ventatickets.ventaticketstaxis.R
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val loginState by loginViewModel.loginState.collectAsState()
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Contexto para SharedPreferences
    val context = LocalContext.current

    // Detectar tamaño de pantalla para botones responsive
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Leer valores guardados al iniciar
    var serverIp by remember { mutableStateOf(AppConfig.getServerIp(context)) }
    var taquilla by remember { mutableStateOf(AppConfig.getTaquilla(context)) }
    var zona by remember { mutableStateOf(AppConfig.getZona(context)) }
    var tipoImpresora by remember { mutableStateOf(AppConfig.getTipoImpresora(context)) }
    var printerMac by remember { mutableStateOf(AppConfig.getPrinterMac(context)) }
    var apiKey by remember { mutableStateOf(AppConfig.getApiKey(context)) }
    var tipoCodigo by remember { mutableStateOf(AppConfig.getTipoCodigo(context)) }
    var showConfigDialog by remember { mutableStateOf(false) }

    // Verificar si la configuración está completa
    var isConfigComplete by remember { mutableStateOf(AppConfig.isConfigurationComplete(context)) }

    // Estados para validación de contraseña de configuración
    var showPasswordDialog by remember { mutableStateOf(false) }
    var configPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showConfigPassword by remember { mutableStateOf(false) }

    // Códigos de acceso válidos
    val validPasswords = listOf(
        "2fHMmoCgRN",
        "YNeDX0g6Be",
        "LGg6VbGWTN",
        "QDtmsfxiN4",
        "6mxO71zt4G",
        "soportesistemas",
        "sistemasenlaces",
        "sistemasenlaces92"
    )

    // Estados para validación
    var serverIpError by remember { mutableStateOf<String?>(null) }
    var taquillaError by remember { mutableStateOf<String?>(null) }
    var zonaError by remember { mutableStateOf<String?>(null) }
    var tipoImpresoraError by remember { mutableStateOf<String?>(null) }
    var printerMacError by remember { mutableStateOf<String?>(null) }
    var apiKeyError by remember { mutableStateOf<String?>(null) }

    // Estados para dropdown
    var expandedImpresora by remember { mutableStateOf(false) }
    var expandedTipoCodigo by remember { mutableStateOf(false) }
    val opcionesImpresora = listOf("ZEBRA (ZPL)", "ESC/POS", "STAR")
    val opcionesTipoCodigo = listOf("QR", "Código de Barras")

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Animación para la tarjeta
    val cardAlpha by animateFloatAsState(
        targetValue = if (isConfigComplete) 1f else 0.7f,
        animationSpec = tween(500),
        label = "cardAlpha"
    )


    // Mostrar mensaje de error en Snackbar si hay error de login
    LaunchedEffect(loginState) {
        if (loginState is LoginViewModel.LoginState.Error) {
            snackbarMessage = (loginState as LoginViewModel.LoginState.Error).message
            snackbarHostState.showSnackbar(snackbarMessage ?: "Error desconocido")
        }
    }

    fun validateConfigPassword(): Boolean {
        val cleanPassword = configPassword.trim()
        if (cleanPassword.isEmpty()) {
            passwordError = "El código de acceso es requerido"
            return false
        }

        if (!validPasswords.contains(cleanPassword)) {
            passwordError = "Código de acceso incorrecto"
            return false
        }

        passwordError = null
        return true
    }

    fun saveConfig() {
        // Limpiar espacios en blanco
        val cleanServerIp = serverIp.trim()
        val cleanTaquilla = taquilla.trim()
        val cleanZona = zona.trim()
        val cleanTipoImpresora = tipoImpresora.trim()
        val cleanPrinterMac = printerMac.trim()
        val cleanApiKey = apiKey.trim()
        val cleanTipoCodigo = tipoCodigo.trim()

        // Validar campos
        var hasError = false

        if (cleanServerIp.isEmpty()) {
            serverIpError = "La IP del servidor es requerida"
            hasError = true
        } else if (!cleanServerIp.contains(":")) {
            serverIpError = "La IP debe incluir el puerto (ejemplo: 192.168.1.100:1880)"
            hasError = true
        } else if (!cleanServerIp.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+$"))) {
            serverIpError = "Formato de IP inválido. Use: IP:PUERTO (ejemplo: 192.168.1.100:1880)"
            hasError = true
        } else {
            serverIpError = null
        }

        if (cleanTaquilla.isEmpty()) {
            taquillaError = "La taquilla es requerida"
            hasError = true
        } else {
            taquillaError = null
        }

        if (cleanZona.isEmpty()) {
            zonaError = "La zona es requerida"
            hasError = true
        } else {
            zonaError = null
        }

        if (cleanTipoImpresora.isEmpty()) {
            tipoImpresoraError = "El tipo de impresora es requerido"
            hasError = true
        } else {
            tipoImpresoraError = null
        }

        if (cleanPrinterMac.isEmpty()) {
            printerMacError = "La MAC de la impresora es requerida"
            hasError = true
        } else if (!cleanPrinterMac.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))) {
            printerMacError = "Formato de MAC inválido. Use: XX:XX:XX:XX:XX:XX"
            hasError = true
        } else {
            printerMacError = null
        }

        if (!hasError) {
            // Actualizar valores con espacios eliminados
            serverIp = cleanServerIp
            taquilla = cleanTaquilla
            zona = cleanZona
            tipoImpresora = cleanTipoImpresora
            printerMac = cleanPrinterMac
            apiKey = cleanApiKey
            tipoCodigo = cleanTipoCodigo

            // Guardar usando AppConfig
            AppConfig.setServerIp(context, cleanServerIp)
            AppConfig.setTaquilla(context, cleanTaquilla)
            AppConfig.setZona(context, cleanZona)
            AppConfig.setTipoImpresora(context, cleanTipoImpresora)
            AppConfig.setPrinterMac(context, cleanPrinterMac)
            AppConfig.setApiKey(context, cleanApiKey)
            AppConfig.setTipoCodigo(context, cleanTipoCodigo)

            // Actualizar ServiceLocator con la nueva URL y API Key
            ServiceLocator.updateBaseUrl(context)

            // Verificar si la configuración ahora está completa
            isConfigComplete = AppConfig.isConfigurationComplete(context)

            showConfigDialog = false
        }
    }

    // Fondo con gradiente moderno que se extiende detrás de la barra de notificaciones
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            )
    ) {
        // Botón de configuración flotante en la parte superior con padding para la barra de notificaciones
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            FloatingActionButton(
                onClick = {
                    showPasswordDialog = true
                    configPassword = ""
                    passwordError = null
                    showConfigPassword = false
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Configuración",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Contenido principal centrado con padding para la barra de notificaciones
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Tarjeta de login moderna
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Logo como imagen de presentación
                    Image(
                        painter = painterResource(id = R.drawable.inicio_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = "Inicia sesión para continuar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Mostrar mensaje si falta configuración
                    if (!isConfigComplete) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⚠️ Configuración requerida",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Configure la IP del servidor, taquilla, zona y MAC de la impresora",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Campo de usuario
                    OutlinedTextField(
                        value = usuario,
                        onValueChange = { usuario = it },
                        label = { Text("Usuario") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Usuario",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = isConfigComplete,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Campo de contraseña
                    OutlinedTextField(
                        value = contrasena,
                        onValueChange = { contrasena = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Contraseña",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { showPassword = !showPassword },
                                enabled = isConfigComplete
                            ) {
                                Text(
                                    if (showPassword) "Ocultar" else "Mostrar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        enabled = isConfigComplete,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Botón de login moderno
                    Button(
                        onClick = { loginViewModel.login(usuario, contrasena) },
                        enabled = isConfigComplete && usuario.isNotBlank() && contrasena.isNotBlank() && loginState !is LoginViewModel.LoginState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.getButtonHeightLarge(screenWidth)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        if (loginState is LoginViewModel.LoginState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Entrar",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Iniciar Sesión",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Snackbar en la parte superior (al final para que aparezca por encima)
        // Posicionado considerando la barra de notificaciones, similar al estilo del AppBar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            snackbar = { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    content = { Text(data.visuals.message) }
                )
            }
        )
    }

    // Navegar si login es exitoso
    LaunchedEffect(loginState) {
        if (loginState is LoginViewModel.LoginState.Success) {
            onLoginSuccess()
        }
    }

    // Diálogo para ingresar contraseña de configuración
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = {
                Text(
                    "Código de acceso requerido",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = configPassword,
                        onValueChange = { configPassword = it },
                        label = { Text("Código de acceso") },
                        isError = passwordError != null,
                        singleLine = true,
                        visualTransformation = if (showConfigPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfigPassword = !showConfigPassword }) {
                                Icon(
                                    imageVector = if (showConfigPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showConfigPassword) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                ElevatedButton(
                    onClick = {
                        if (validateConfigPassword()) {
                            showPasswordDialog = false
                            showConfigDialog = true
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                ElevatedButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Full-screen dialog de configuración (Material Design 3)
    if (showConfigDialog) {
        FullScreenDialog(
            onDismiss = { showConfigDialog = false },
            onSave = { saveConfig() },
            title = "Configuración",
            content = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .imePadding()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text("IP del servidor (IP:PUERTO)") },
                        isError = serverIpError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = if (serverIpError != null) {
                            { Text(serverIpError ?: "", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    OutlinedTextField(
                        value = taquilla,
                        onValueChange = { taquilla = it },
                        label = { Text("Taquilla") },
                        isError = taquillaError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = if (taquillaError != null) {
                            { Text(taquillaError ?: "", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    OutlinedTextField(
                        value = zona,
                        onValueChange = { zona = it },
                        label = { Text("Zona") },
                        isError = zonaError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = if (zonaError != null) {
                            { Text(zonaError ?: "", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedImpresora,
                        onExpandedChange = { expandedImpresora = !expandedImpresora }
                    ) {
                        OutlinedTextField(
                            value = tipoImpresora,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de impresora") },
                            isError = tipoImpresoraError != null,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedImpresora)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            supportingText = if (tipoImpresoraError != null) {
                                { Text(tipoImpresoraError ?: "", color = MaterialTheme.colorScheme.error) }
                            } else null
                        )
                        ExposedDropdownMenu(
                            expanded = expandedImpresora,
                            onDismissRequest = { expandedImpresora = false }
                        ) {
                            opcionesImpresora.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        tipoImpresora = option
                                        expandedImpresora = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedTipoCodigo,
                        onExpandedChange = { expandedTipoCodigo = !expandedTipoCodigo }
                    ) {
                        OutlinedTextField(
                            value = tipoCodigo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de código") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipoCodigo)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            supportingText = {
                                Text("Selecciona si deseas imprimir QR o Código de Barras")
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTipoCodigo,
                            onDismissRequest = { expandedTipoCodigo = false }
                        ) {
                            opcionesTipoCodigo.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        tipoCodigo = option
                                        expandedTipoCodigo = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = printerMac,
                        onValueChange = { printerMac = it },
                        label = { Text("MAC de la impresora") },
                        isError = printerMacError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = if (printerMacError != null) {
                            { Text(printerMacError ?: "", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key (Token de seguridad)") },
                        placeholder = { Text("Opcional - Token para autenticación") },
                        isError = apiKeyError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = if (apiKeyError != null) {
                            { Text(apiKeyError ?: "", color = MaterialTheme.colorScheme.error) }
                        } else {
                            { Text("Token requerido por Node-RED para validar peticiones") }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        )
    }
}
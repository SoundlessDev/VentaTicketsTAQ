package com.ventatickets.ventaticketstaxis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationViewModel
import com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationLoadState
import com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneViewModel
import com.ventatickets.ventaticketstaxis.data.Destination
import com.ventatickets.ventaticketstaxis.ui.theme.Dimensions
import androidx.compose.ui.platform.LocalContext
import com.ventatickets.ventaticketstaxis.data.AppConfig
import kotlinx.coroutines.launch

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSelectionSection(
    selectedZone: String?,
    selectedDestination: String?,
    onDestinationSelected: (String, String, String) -> Unit,
    onFolioReceived: (String) -> Unit = {},
    destinationViewModel: DestinationViewModel? = null,
    zoneViewModel: ZoneViewModel? = null,
    onClearValues: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val destViewModel = destinationViewModel ?: remember { DestinationViewModel.getInstance(context) }
    val zoneViewModel = zoneViewModel ?: remember { ZoneViewModel.getInstance(context) }
//    var regularDestinationText by remember { mutableStateOf("") }
//    var specialDestinationText by remember { mutableStateOf("") }
    var expandedRegular by remember { mutableStateOf(false) }
    var expandedSpecial by remember { mutableStateOf(false) }

    val regularFocusRequester = remember { FocusRequester() }
    val specialFocusRequester = remember { FocusRequester() }

    var userIsTyping by remember { mutableStateOf(false) }

    // Obtenemos los datos de los endpoints
    val dropdownZonaS by destViewModel.dropdownZonaS.collectAsState()
    val dropdownZonaDifS by destViewModel.dropdownZonaDifS.collectAsState()
    val isNocturno by zoneViewModel.isNocturno.collectAsState()
    val isLoading by destViewModel.isLoading.collectAsState()
    val error by destViewModel.error.collectAsState()
    val loadState by destViewModel.loadState.collectAsState()

    // Variable local para el estado actual
    val currentLoadState = loadState

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var expanded by remember { mutableStateOf(false) }


    // Logging para debug
    LaunchedEffect(dropdownZonaS) {
        android.util.Log.d("DestinationSelectionSection", "Dropdown zona S actualizado: ${dropdownZonaS.size} destinos")
    }

    LaunchedEffect(dropdownZonaDifS) {
        android.util.Log.d("DestinationSelectionSection", "Dropdown zona dif S actualizado: ${dropdownZonaDifS.size} destinos")
    }

    LaunchedEffect(currentLoadState) {
        android.util.Log.d("DestinationSelectionSection", "Estado de carga de destinos: $currentLoadState")
    }

    LaunchedEffect(error) {
        if (error != null) {
            android.util.Log.d("DestinationSelectionSection", "Error de destinos: $error")
        }
    }

    // Estados de folio
    val folio by zoneViewModel.folio.collectAsState()
    val isLoadingFolio by zoneViewModel.isLoadingFolio.collectAsState()
    val folioError by zoneViewModel.folioError.collectAsState()

    // Observar cambios en el folio
    LaunchedEffect(folio) {
        android.util.Log.d("DestinationSelectionSection", "Folio actualizado: '$folio'")
        onFolioReceived(folio)
    }

    // Observar errores de folio y mostrarlos como Snackbar
    var lastShownFolioError by remember { mutableStateOf<String?>(null) }
    var lastSelectedZone by remember { mutableStateOf<String?>(null) }

    // Guardar la zona del destino seleccionado
    LaunchedEffect(selectedDestination) {
        // Obtener la zona del destino seleccionado
        val selectedDest = dropdownZonaDifS.firstOrNull { it.titulo == selectedDestination }
            ?: dropdownZonaS.firstOrNull { it.titulo == selectedDestination }
        if (selectedDest != null) {
            lastSelectedZone = selectedDest.zona
        }
    }

    LaunchedEffect(folioError) {
        if (folioError != null && snackbarHostState != null) {
            // Crear una clave única para este error específico
            val errorKey = "$folioError|$lastSelectedZone"
            if (errorKey != lastShownFolioError) {
                android.util.Log.d("DestinationSelectionSection", "Error de folio detectado: $folioError, Zona: $lastSelectedZone")
                lastShownFolioError = errorKey

                // Construir mensaje con información de la zona
                val zonaInfo = if (lastSelectedZone != null) " (Zona: $lastSelectedZone)" else ""
                val errorMessage = "${folioError ?: "Error desconocido"}$zonaInfo"

                coroutineScope.launch {
                    try {
                        android.util.Log.d("DestinationSelectionSection", "Mostrando Snackbar: $errorMessage")
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                        // Esperar un poco antes de limpiar para asegurar que el mensaje se muestre
                        kotlinx.coroutines.delay(500)
                    } catch (e: Exception) {
                        android.util.Log.e("DestinationSelectionSection", "Error mostrando Snackbar: ${e.message}", e)
                    } finally {
                        // Limpiar el error después de mostrarlo
                        zoneViewModel.clearFolioError()
                        // Resetear después de un delay más largo para permitir mostrar el mismo error de nuevo si ocurre
                        kotlinx.coroutines.delay(2000)
                        lastShownFolioError = null
                    }
                }
            }
        }
    }

    // Observar errores de carga de destinos y mostrarlos como Snackbar
    var lastShownError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(loadState) {
        if (loadState is DestinationLoadState.Error && snackbarHostState != null) {
            val errorMessage = (loadState as DestinationLoadState.Error).message
            // Solo mostrar si es un error diferente al último mostrado
            if (errorMessage != lastShownError) {
                android.util.Log.d("DestinationSelectionSection", "Error de carga: $errorMessage")
                lastShownError = errorMessage
                coroutineScope.launch {
                    try {
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("DestinationSelectionSection", "Error mostrando Snackbar: ${e.message}")
                    }
                }
            }
        } else if (loadState !is DestinationLoadState.Error) {
            // Resetear el último error mostrado cuando el estado cambia a no-error
            lastShownError = null
        }
    }

    // Intentar cargar datos si no están disponibles y hay configuración
    LaunchedEffect(Unit) {
        if (dropdownZonaS.isEmpty() && dropdownZonaDifS.isEmpty() && !isLoading && currentLoadState is DestinationLoadState.Idle) {
            if (AppConfig.isConfigurationComplete(context)) {
                android.util.Log.d("DestinationSelectionSection", "Intentando cargar destinos automáticamente")
                destViewModel.loadDestinationsFromWebService()
            }
        }
    }

    // Función para limpiar los textos de los dropdowns
    fun clearDropdownTexts() {
        textFieldValue = TextFieldValue("")
        expanded = false
        userIsTyping = false
    }

    // Observar cambios en selectedDestination para detectar cuando se limpia
    LaunchedEffect(selectedDestination) {
        if (selectedDestination.isNullOrEmpty()) {
            // Si selectedDestination se limpia, también limpiar los textos de los dropdowns
            clearDropdownTexts()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.spacingMedium),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(Dimensions.cardElevation),
        tonalElevation = Dimensions.cardElevation,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(Dimensions.cardPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
        ) {
            Text(
                text = "Seleccionar Destino",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Dimensions.spacingMedium)
            )

            when (currentLoadState) {
                is DestinationLoadState.NoConfiguration -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Configuración requerida",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = Dimensions.spacingMedium)
                        )
                        Text(
                            text = "Configure el servidor en la pantalla de configuración para cargar los destinos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Dimensions.spacingLarge)
                        )
                    }
                }
                is DestinationLoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
                            Text(
                                text = "Cargando destinos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is DestinationLoadState.Error -> {
                    // El error se muestra como Snackbar, pero mantenemos un botón de reintentar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingMedium)
                    ) {
                        Text(
                            text = "Error al cargar destinos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FilledTonalButton(
                            onClick = { destViewModel.loadDestinationsFromWebService() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                is DestinationLoadState.Success -> {
                    var selectedTabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf("Destinos", "Especiales")

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    selectedTabIndex = index

                                    // Reset búsqueda al cambiar tab
                                    textFieldValue = TextFieldValue("")
                                    expanded = false
                                    userIsTyping = false
                                },
                                text = {
                                    Text(
                                        text = title,
                                        color = if (selectedTabIndex == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabContentAnimation"
                    ) { targetTab ->

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val destinationsList = if (targetTab == 0) {
                            dropdownZonaDifS
                        } else {
                            dropdownZonaS
                        }

                        val filteredDestinations by remember(
                            textFieldValue.text,
                            destinationsList
                        ) {
                            derivedStateOf {
                                if (textFieldValue.text.length < 3) {
                                    emptyList()
                                } else {
                                    destinationsList.filter {
                                        it.titulo.contains(textFieldValue.text, ignoreCase = true)
                                    }
                                }
                            }
                        }

                        LaunchedEffect(textFieldValue.text) {
                            if (userIsTyping) {
                                expanded = textFieldValue.text.length >= 3
                            }
                        }

                        LaunchedEffect(isPressed) {
                            if (isPressed && textFieldValue.text.isNotEmpty()) {
                                textFieldValue = textFieldValue.copy(
                                    selection = TextRange(0, textFieldValue.text.length)
                                )
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {

                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    userIsTyping = true
                                    textFieldValue = newValue
                                },
                                placeholder = {
                                    Text("Teclea al menos 3 letras...")
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                interactionSource = interactionSource,
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {

                                if (textFieldValue.text.length < 3) {
                                    DropdownMenuItem(
                                        text = { Text("Escribe al menos 3 caracteres") },
                                        onClick = {}
                                    )
                                } else if (filteredDestinations.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Sin resultados") },
                                        onClick = {}
                                    )
                                } else {
                                    filteredDestinations.forEach { destination ->
                                        DropdownMenuItem(
                                            text = { Text(destination.titulo) },
                                            onClick = {

                                                userIsTyping = false
                                                expanded = false

                                                textFieldValue = TextFieldValue(destination.titulo)

                                                val effectiveCost =
                                                    if (isNocturno && destination.costo_nocturno != null)
                                                        destination.costo_nocturno
                                                    else
                                                        destination.costo

                                                onDestinationSelected(
                                                    destination.titulo,
                                                    effectiveCost.toString(),
                                                    destination.zona
                                                )

                                                zoneViewModel.getFolio(destination.zona)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Estado Idle - mostrar mensaje informativo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Iniciando carga de destinos...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
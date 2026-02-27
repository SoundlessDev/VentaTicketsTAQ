package com.ventatickets.ventaticketstaxis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneViewModel
import com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneLoadState
import com.ventatickets.ventaticketstaxis.ui.theme.Dimensions
import com.ventatickets.ventaticketstaxis.data.AppConfig
import kotlinx.coroutines.launch

@Composable
fun ZoneSelectionSection(
    selectedZone: String?,
    onZoneSelected: (String, Int, String) -> Unit,
    onSpecialZoneSelected: (String, String) -> Unit,
    onFolioReceived: (String) -> Unit,
    zoneViewModel: ZoneViewModel,
    onClearValues: () -> Unit = {},
    currentFolio: String = "",
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val viewModel = zoneViewModel
    val coroutineScope = rememberCoroutineScope()
    var showSpecialZoneDialog by remember { mutableStateOf(false) }

    val zones by viewModel.zones.collectAsState()
    val isNocturno by viewModel.isNocturno.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val loadState by viewModel.loadState.collectAsState()

    // Variable local para el estado actual
    val currentLoadState = loadState

    // Estados de folio
    val folio by viewModel.folio.collectAsState()
    val isLoadingFolio by viewModel.isLoadingFolio.collectAsState()
    val folioError by viewModel.folioError.collectAsState()

    // Detectar orientación y tamaño de pantalla
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp

    // Calcular número de columnas basado en el tamaño de pantalla
    // Ajustar columnas en landscape para mejor uso del espacio
    val baseColumns = Dimensions.getGridColumns(screenWidth)
    val columns = if (isLandscape && screenWidth > 600.dp) {
        // En landscape con pantalla grande, usar más columnas
        baseColumns + 1
    } else {
        baseColumns
    }

    // Calcular spacing y padding responsive según el tamaño de pantalla (diseño compacto)
    val responsiveCardPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 10.dp
        else -> Dimensions.cardPadding
    }

    val responsiveGridSpacing = when {
        screenWidth < Dimensions.Breakpoints.small -> 4.dp
        screenWidth < Dimensions.Breakpoints.medium -> 5.dp
        else -> Dimensions.gridSpacing
    }

    val responsiveButtonPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 2.dp
        screenWidth < Dimensions.Breakpoints.medium -> 2.5.dp
        else -> Dimensions.spacingSmall
    }

    val responsiveTitlePadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 12.dp
        else -> Dimensions.spacingLarge
    }

    val responsiveVerticalPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 4.dp
        screenWidth < Dimensions.Breakpoints.medium -> 6.dp
        else -> Dimensions.spacingMedium
    }

    val responsiveLoadingPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 12.dp
        screenWidth < Dimensions.Breakpoints.medium -> 14.dp
        else -> Dimensions.spacingLarge
    }

    val responsiveStateSpacing = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 10.dp
        else -> Dimensions.spacingMedium
    }

    val responsiveStateBottomPadding = when {
        screenWidth < Dimensions.Breakpoints.small -> 8.dp
        screenWidth < Dimensions.Breakpoints.medium -> 10.dp
        else -> Dimensions.spacingLarge
    }

    // Logging para debug
    LaunchedEffect(zones) {
        android.util.Log.d("ZoneSelectionSection", "Zonas actualizadas: ${zones.size}")
        zones.forEach { zone ->
            android.util.Log.d("ZoneSelectionSection", "Zona disponible: ${zone.zona}")
        }
    }

    LaunchedEffect(loadState) {
        android.util.Log.d("ZoneSelectionSection", "Estado de carga: $loadState")
    }

    LaunchedEffect(error) {
        if (error != null) {
            android.util.Log.d("ZoneSelectionSection", "Error: $error")
        }
    }

    // Observar cambios en el folio
    LaunchedEffect(folio) {
        android.util.Log.d("ZoneSelectionSection", "Folio actualizado: '$folio'")
        onFolioReceived(folio)
    }

    // Observar errores de folio y mostrarlos como Snackbar
    var lastShownFolioError by remember { mutableStateOf<String?>(null) }
    var lastSelectedZone by remember { mutableStateOf<String?>(null) }

    // Guardar la zona seleccionada cuando se hace clic
    LaunchedEffect(selectedZone) {
        if (selectedZone != null) {
            lastSelectedZone = selectedZone
        }
    }

    LaunchedEffect(folioError) {
        if (folioError != null && snackbarHostState != null) {
            // Crear una clave única para este error específico
            val errorKey = "$folioError|$lastSelectedZone"
            if (errorKey != lastShownFolioError) {
                android.util.Log.d("ZoneSelectionSection", "Error de folio detectado: $folioError, Zona: $lastSelectedZone")
                lastShownFolioError = errorKey

                // Construir mensaje con información de la zona
                val zonaInfo = if (lastSelectedZone != null) " (Zona: $lastSelectedZone)" else ""
                val errorMessage = "${folioError ?: "Error desconocido"}$zonaInfo"

                coroutineScope.launch {
                    try {
                        android.util.Log.d("ZoneSelectionSection", "Mostrando Snackbar: $errorMessage")
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                        // Esperar un poco antes de limpiar para asegurar que el mensaje se muestre
                        kotlinx.coroutines.delay(500)
                    } catch (e: Exception) {
                        android.util.Log.e("ZoneSelectionSection", "Error mostrando Snackbar: ${e.message}", e)
                    } finally {
                        // Limpiar el error después de mostrarlo
                        viewModel.clearFolioError()
                        // Resetear después de un delay más largo para permitir mostrar el mismo error de nuevo si ocurre
                        kotlinx.coroutines.delay(2000)
                        lastShownFolioError = null
                    }
                }
            }
        }
    }

    // Observar errores de carga y mostrarlos como Snackbar
    var lastShownError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(loadState) {
        if (loadState is ZoneLoadState.Error && snackbarHostState != null) {
            val errorMessage = (loadState as ZoneLoadState.Error).message
            // Solo mostrar si es un error diferente al último mostrado
            if (errorMessage != lastShownError) {
                android.util.Log.d("ZoneSelectionSection", "Error de carga: $errorMessage")
                lastShownError = errorMessage
                coroutineScope.launch {
                    try {
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ZoneSelectionSection", "Error mostrando Snackbar: ${e.message}")
                    }
                }
            }
        } else if (loadState !is ZoneLoadState.Error) {
            // Resetear el último error mostrado cuando el estado cambia a no-error
            lastShownError = null
        }
    }

    // Intentar cargar datos si no están disponibles y hay configuración
    LaunchedEffect(Unit) {
        if (zones.isEmpty() && !isLoading && currentLoadState is ZoneLoadState.Idle) {
            if (AppConfig.isConfigurationComplete(context)) {
                android.util.Log.d("ZoneSelectionSection", "Intentando cargar zonas automáticamente")
                viewModel.loadZonesFromWebService()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = responsiveVerticalPadding),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(Dimensions.cardElevation),
        tonalElevation = Dimensions.cardElevation,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(responsiveCardPadding)
                .fillMaxWidth()
        ) {
            Text(
                text = "Seleccionar Zona",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = responsiveTitlePadding),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            when (currentLoadState) {
                is ZoneLoadState.NoConfiguration -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Configuración requerida",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = responsiveCardPadding)
                                .padding(bottom = responsiveStateSpacing),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Configure el servidor en la pantalla de configuración para cargar las zonas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = responsiveCardPadding)
                                .padding(bottom = responsiveStateBottomPadding),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                is ZoneLoadState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(responsiveLoadingPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(responsiveStateSpacing))
                            Text(
                                text = "Cargando zonas...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                is ZoneLoadState.Error -> {
                    // El error se muestra como Snackbar, pero mantenemos un botón de reintentar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(responsiveStateSpacing)
                    ) {
                        Text(
                            text = "Error al cargar zonas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = responsiveCardPadding),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        FilledTonalButton(
                            onClick = { viewModel.loadZonesFromWebService() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                is ZoneLoadState.Success -> {
                    if (zones.isNotEmpty()) {
                        android.util.Log.d("ZoneSelectionSection", "Renderizando grid con ${zones.size} zonas")
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            horizontalArrangement = Arrangement.spacedBy(responsiveGridSpacing),
                            verticalArrangement = Arrangement.spacedBy(responsiveGridSpacing),
                            contentPadding = PaddingValues(horizontal = responsiveCardPadding / 2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(zones.size) { index ->
                                val zone = zones[index]
                                val effectiveCost = viewModel.getEffectiveCost(zone, isNocturno)
                                android.util.Log.d("ZoneSelectionSection", "Renderizando botón para zona: ${zone.zona}")
                                Button(
                                    onClick = {
                                        android.util.Log.d("ZoneSelectionSection", "Botón clickeado: ${zone.zona}")
                                        // Guardar la zona seleccionada antes de solicitar folio
                                        lastSelectedZone = zone.zona
                                        onZoneSelected(zone.zona, effectiveCost, zone.descrip)

                                        if (zone.zona == "S") {
                                            // Para zona S, abrir diálogo inmediatamente
                                            showSpecialZoneDialog = true
                                            // Solicitar folio en segundo plano
                                            viewModel.getFolio(zone.zona)
                                        } else {
                                            // Para otras zonas, solicitar folio normalmente
                                            viewModel.getFolio(zone.zona)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedZone == zone.zona)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (selectedZone == zone.zona)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)  // Hace que los botones sean cuadrados
                                        .padding(responsiveButtonPadding),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (selectedZone == zone.zona)
                                            Dimensions.elevationLarge
                                        else
                                            Dimensions.elevationSmall
                                    )
                                ) {
                                    Text(
                                        text = zone.zona,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = if (selectedZone == zone.zona) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        android.util.Log.d("ZoneSelectionSection", "No hay zonas disponibles")
                        Text(
                            text = "No hay zonas disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = responsiveCardPadding),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> {
                    // Estado Idle - mostrar mensaje informativo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Iniciando carga de zonas...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = responsiveCardPadding),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Los errores de folio ahora se muestran como Snackbars en lugar de Cards
        }
    }

    // Diálogo para zona especial
    if (showSpecialZoneDialog) {
        SpecialZoneDialog(
            onDismiss = { showSpecialZoneDialog = false },
            onConfirm = { destination, cost ->
                onSpecialZoneSelected(destination, cost)
                showSpecialZoneDialog = false
            }
        )
    }
}
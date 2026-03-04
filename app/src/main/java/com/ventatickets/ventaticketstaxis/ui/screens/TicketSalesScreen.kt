package com.ventatickets.ventaticketstaxis.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ventatickets.ventaticketstaxis.data.LoginResponse
import com.ventatickets.ventaticketstaxis.data.sendZplToZebra
import com.ventatickets.ventaticketstaxis.data.sendEscPosToPrinter
import com.ventatickets.ventaticketstaxis.data.sendStarToPrinter
import com.ventatickets.ventaticketstaxis.data.buildZplQrCommand
import com.ventatickets.ventaticketstaxis.data.buildEscPosQrCommand
import com.ventatickets.ventaticketstaxis.data.buildStarQrCommand
import com.ventatickets.ventaticketstaxis.data.buildZplBarcodeCommand
import com.ventatickets.ventaticketstaxis.data.buildEscPosBarcodeCommand
import com.ventatickets.ventaticketstaxis.data.buildStarBarcodeCommand
import com.ventatickets.ventaticketstaxis.ui.components.*
import com.ventatickets.ventaticketstaxis.ui.components.LoadingIndicator
import com.ventatickets.ventaticketstaxis.ui.theme.Dimensions
import com.ventatickets.ventaticketstaxis.ui.viewmodels.LoginViewModel
import com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneViewModel
import androidx.core.content.ContextCompat
import com.ventatickets.ventaticketstaxis.data.EmpresaGlobals
import com.ventatickets.ventaticketstaxis.data.AppConfig
import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.os.Looper
import android.content.Context
import java.util.*
import androidx.compose.animation.Crossfade
import com.ventatickets.ventaticketstaxis.data.SaveTicketRequest
import com.ventatickets.ventaticketstaxis.data.SaveTicketResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ventatickets.ventaticketstaxis.data.ServiceLocator
import com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationViewModel
import com.ventatickets.ventaticketstaxis.data.NocturnoHelper
import com.ventatickets.ventaticketstaxis.data.SaveDestinoManualRequest
import com.ventatickets.ventaticketstaxis.notifications.NotificationHelper
import com.ventatickets.ventaticketstaxis.workers.DataRefreshWorker
import kotlinx.coroutines.delay

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketSalesScreen(
    onLogout: () -> Unit = {},
    loginViewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        EmpresaGlobals.initialize(context)
    }
    val loginState by loginViewModel.loginState.collectAsState()
    val userName = when (val state = loginState) {
        is LoginViewModel.LoginState.Success -> state.user?.user ?: "Usuario"
        else -> "Usuario"
    }
    val userFullName = when (val state = loginState) {
        is LoginViewModel.LoginState.Success -> state.user?.nombre ?: "Nombre no disponible"
        else -> "Nombre no disponible"
    }

    var selectedZone by remember { mutableStateOf<String?>(null) }
    var selectedDestination by remember { mutableStateOf<String?>(null) }
    var manualDestination by remember { mutableStateOf("") }
    var isManualZone by remember { mutableStateOf(false) }
    var showManualDestinationDialog by remember { mutableStateOf(false) }
    var showGuardarDestinoDialog by remember { mutableStateOf(false) }
    var cost by remember { mutableStateOf("000") }
    var folio by remember { mutableStateOf("") }
    var currentSection by remember { mutableStateOf(0) }
    var displayZone by remember { mutableStateOf("---") }
    var displayDestination by remember { mutableStateOf("---") }
    var showUserDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showEmpresaDialog by remember { mutableStateOf(false) }
    var isRefreshingData by remember { mutableStateOf(false) }
    var empresaDataKey by remember { mutableStateOf(0) } // Key para forzar recomposición del diálogo


    // Estado para información nocturna
    var nocturnoInfo by remember { mutableStateOf("") }

    // Observar cambios en empresaData del ViewModel para actualizar el diálogo y estado nocturno
    val empresaData by loginViewModel.empresaData.collectAsState()
    LaunchedEffect(empresaData) {
        if (showEmpresaDialog) {
            // Incrementar key para forzar recomposición del diálogo
            empresaDataKey++
        }
    }

    val zoneViewModel: ZoneViewModel = remember { ZoneViewModel.getInstance(context) }
    val zones by zoneViewModel.zones.collectAsState()
    val isNocturno by zoneViewModel.isNocturno.collectAsState()
    val destinationViewModel: DestinationViewModel = remember { DestinationViewModel.getInstance(context) }
    val dropdownZonaS by destinationViewModel.dropdownZonaS.collectAsState()
    val dropdownZonaDifS by destinationViewModel.dropdownZonaDifS.collectAsState()

    // Estados de folio para validación
    val isLoadingFolio by zoneViewModel.isLoadingFolio.collectAsState()
    val folioError by zoneViewModel.folioError.collectAsState()

    fun updateCostForState(isNight: Boolean) {
        android.util.Log.d("TicketSalesScreen", "Recalculando costos. Nocturno: $isNight, Zona seleccionada: $selectedZone, Destino: $selectedDestination")
        selectedDestination?.let { selected ->
            val destino = (dropdownZonaS + dropdownZonaDifS).firstOrNull { it.titulo == selected }
            if (destino != null) {
                val nuevoCosto = if (isNight && destino.costo_nocturno != null) destino.costo_nocturno else destino.costo
                android.util.Log.d("TicketSalesScreen", "Actualizando costo por destino: $nuevoCosto")
                cost = nuevoCosto.toString()
                displayZone = destino.zona
                return
            }
        }
        if (!selectedZone.isNullOrBlank() && selectedZone != "S") {
            val zonaSeleccionada = zones.firstOrNull { it.zona == selectedZone }
            zonaSeleccionada?.let { zonaSel ->
                val nuevoCosto = if (isNight && zonaSel.costo_nocturno != null) zonaSel.costo_nocturno else zonaSel.costo
                android.util.Log.d("TicketSalesScreen", "Actualizando costo por zona: $nuevoCosto")
                cost = nuevoCosto.toString()
                displayZone = zonaSel.zona
            }
        }
    }

    LaunchedEffect(isNocturno, selectedZone, selectedDestination, zones, dropdownZonaS, dropdownZonaDifS) {
        updateCostForState(isNocturno)
    }

    // Detectar orientación y tamaño de pantalla
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp

    val snackbarHostState = remember { SnackbarHostState() }

    // Estado para pull-to-refresh (Material 3)
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    // Función para refrescar datos (extraída para reutilización)
    fun refreshData(scope: CoroutineScope) {
        if (isRefreshingData) return

        isRefreshingData = true
        android.util.Log.d("TicketSalesScreen", "=== Iniciando recarga completa de datos ===")

        scope.launch {
            val resultados = mutableListOf<String>()
            var empresaActualizada = false
            var zonasActualizadas = false
            var destinosActualizados = false

            try {
                val oldIniNocturno = EmpresaGlobals.ini_nocturno
                val oldFinNocturno = EmpresaGlobals.fin_nocturno

                android.util.Log.d("TicketSalesScreen", "Horarios actuales: ini=$oldIniNocturno, fin=$oldFinNocturno")

                // 1. Cargar datos de empresa
                android.util.Log.d("TicketSalesScreen", "1. Cargando datos de empresa...")
                val empresaResult = loginViewModel.loadEmpresaDataSuspend(forceRefresh = true)

                empresaResult.fold(
                    onSuccess = { empresaData ->
                        empresaActualizada = true
                        android.util.Log.d("TicketSalesScreen", "✅ Datos de empresa cargados: ${empresaData.descrip}")
                        android.util.Log.d("TicketSalesScreen", "   Horarios recibidos: ${empresaData.ini_nocturno} - ${empresaData.fin_nocturno}")

                        val newIniNocturno = EmpresaGlobals.ini_nocturno
                        val newFinNocturno = EmpresaGlobals.fin_nocturno

                        android.util.Log.d("TicketSalesScreen", "   Horarios en EmpresaGlobals: $newIniNocturno - $newFinNocturno")

                        val horariosCambiaron = (oldIniNocturno != newIniNocturno) || (oldFinNocturno != newFinNocturno)

                        // Recalcular estado nocturno inmediatamente con los nuevos horarios
                        EmpresaGlobals.recalculateNocturnoState()
                        val nuevoEstadoNocturno = EmpresaGlobals.isNocturnoActive()

                        android.util.Log.d("TicketSalesScreen", "Estado nocturno recalculado con nuevos horarios: $nuevoEstadoNocturno")

                        if (horariosCambiaron) {
                            android.util.Log.d("TicketSalesScreen", "⚠️ Horarios nocturnos cambiaron: $oldIniNocturno-$oldFinNocturno → $newIniNocturno-$newFinNocturno")
                            android.util.Log.d("TicketSalesScreen", "   Nuevo estado nocturno: $nuevoEstadoNocturno")
                        }

                        android.util.Log.d("TicketSalesScreen", "4. Reprogramando alarmas de notificaciones...")
                        DataRefreshWorker.rescheduleDataRefresh(context)

                        if (horariosCambiaron) {
                            android.util.Log.d("TicketSalesScreen", "✅ Alarmas reprogramadas con nuevos horarios")
                        } else {
                            android.util.Log.d("TicketSalesScreen", "ℹ️ Horarios no cambiaron, pero alarmas reprogramadas para asegurar sincronización")
                        }

                        // Incrementar key para forzar actualización del diálogo con el nuevo estado
                        empresaDataKey++

                        loginViewModel.loadEmpresaData(forceRefresh = false)
                    },
                    onFailure = { error ->
                        android.util.Log.e("TicketSalesScreen", "❌ Error cargando datos de empresa: ${error.message}")
                        resultados.add("No se pudo conectar con el Servidor.")
                    }
                )

                // 2. Cargar zonas (preservando datos actuales si hay error)
                android.util.Log.d("TicketSalesScreen", "2. Cargando zonas (/cargarZonas)...")
                val zonasAntes = zoneViewModel.zones.value.size
                zoneViewModel.loadZonesFromWebService()

                // Esperar a que termine la carga de zonas
                delay(1000)

                val zonasDespues = zoneViewModel.zones.value.size
                val loadStateZonas = zoneViewModel.loadState.value

                if (loadStateZonas is com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneLoadState.Success && zonasDespues > 0) {
                    zonasActualizadas = true
                    android.util.Log.d("TicketSalesScreen", "✅ Zonas actualizadas: $zonasDespues zonas")
                } else if (loadStateZonas is com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneLoadState.Error) {
                    android.util.Log.e("TicketSalesScreen", "❌ Error cargando zonas: ${(loadStateZonas as com.ventatickets.ventaticketstaxis.ui.viewmodels.ZoneLoadState.Error).message}")
                    if (zonasAntes > 0) {
                        android.util.Log.d("TicketSalesScreen", "   Manteniendo ${zonasAntes} zonas actuales")
                        resultados.add("No se pudo actualizar las zonas (manteniendo datos actuales)")
                    } else {
                        resultados.add("No se pudo cargar las zonas")
                    }
                } else if (zonasAntes > 0) {
                    android.util.Log.d("TicketSalesScreen", "⚠️ Zonas no se actualizaron, manteniendo ${zonasAntes} zonas actuales")
                    resultados.add("No se pudo actualizar las zonas (manteniendo datos actuales)")
                }

                // 3. Cargar destinos (preservando datos actuales si hay error)
                android.util.Log.d("TicketSalesScreen", "3. Cargando destinos (/cargarDropdownZona-S y /cargarDropdownZonaDif-S)...")
                val destinosAntesS = destinationViewModel.dropdownZonaS.value.size
                val destinosAntesDifS = destinationViewModel.dropdownZonaDifS.value.size
                destinationViewModel.loadDestinationsFromWebService()

                // Esperar a que termine la carga de destinos
                delay(1000)

                val destinosDespuesS = destinationViewModel.dropdownZonaS.value.size
                val destinosDespuesDifS = destinationViewModel.dropdownZonaDifS.value.size
                val loadStateDestinos = destinationViewModel.loadState.value

                if (loadStateDestinos is com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationLoadState.Success &&
                    destinosDespuesS > 0 && destinosDespuesDifS > 0) {
                    destinosActualizados = true
                    android.util.Log.d("TicketSalesScreen", "✅ Destinos actualizados: S=$destinosDespuesS, DifS=$destinosDespuesDifS")
                } else if (loadStateDestinos is com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationLoadState.Error) {
                    android.util.Log.e("TicketSalesScreen", "❌ Error cargando destinos: ${(loadStateDestinos as com.ventatickets.ventaticketstaxis.ui.viewmodels.DestinationLoadState.Error).message}")
                    if (destinosAntesS > 0 || destinosAntesDifS > 0) {
                        android.util.Log.d("TicketSalesScreen", "   Manteniendo destinos actuales (S=$destinosAntesS, DifS=$destinosAntesDifS)")
                        resultados.add("No se pudo actualizar los destinos (manteniendo datos actuales)")
                    } else {
                        resultados.add("No se pudo cargar los destinos")
                    }
                } else if (destinosAntesS > 0 || destinosAntesDifS > 0) {
                    android.util.Log.d("TicketSalesScreen", "⚠️ Destinos no se actualizaron, manteniendo datos actuales")
                    resultados.add("No se pudo actualizar los destinos (manteniendo datos actuales)")
                }

                // Actualizar costos según el estado nocturno actual en el hilo principal
                withContext(Dispatchers.Main) {
                    val currentNocturnoState = EmpresaGlobals.isNocturnoActive()
                    android.util.Log.d("TicketSalesScreen", "Actualizando costos después de recargar. Estado nocturno: $currentNocturnoState")
                    updateCostForState(currentNocturnoState)

                    // Forzar actualización del diálogo con los nuevos datos y estado
                    empresaDataKey++

                    // Mostrar mensaje de resultado con palabras clave para colores automáticos
                    if (resultados.isEmpty()) {
                        // Success: contiene "correctamente" → Verde
                        snackbarMessage = "Datos actualizados correctamente"
                    } else {
                        val mensaje = if (empresaActualizada || zonasActualizadas || destinosActualizados) {
                            // Warning: actualización parcial → Naranja
                            "Error de actualización: ${resultados.joinToString("; ")}"
                        } else {
                            // Error: contiene "Error" → Rojo
                            "Error: No se pudo actualizar. ${resultados.joinToString("; ")}. Se mantienen los datos actuales."
                        }
                        snackbarMessage = mensaje
                    }
                }

                android.util.Log.d("TicketSalesScreen", "✅ Recarga completa de datos finalizada")

            } catch (e: Exception) {
                android.util.Log.e("TicketSalesScreen", "❌ Error durante la recarga de datos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // Error: contiene "Error" → Rojo
                    snackbarMessage = "Error al actualizar datos. Se mantienen los datos actuales."
                }
            } finally {
                isRefreshingData = false
            }
        }
    }

    // Obtener configuración dinámica
    val macAddress = AppConfig.getPrinterMac(context)
    val isZebraPrinter = AppConfig.isZebraPrinter(context)
    val isEscPosPrinter = AppConfig.isEscPosPrinter(context)
    val isStarPrinter = AppConfig.isStarPrinter(context)

    // Variables para el ticket (usando EmpresaGlobals)
    val empresa = EmpresaGlobals.descrip ?: "Terminal de Ejemplo"
    val rfc = EmpresaGlobals.rfc ?: "RFC00EJEMPLO"
    val domicilio = EmpresaGlobals.direccion ?: "DIRECCION EJEMPLO"
    val zona = displayZone
    val costo = cost
    val folioActual = folio
    val fecha = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

    // Función para validar si el folio está completo (mínimo 8 dígitos)
    fun isFolioComplete(): Boolean {
        return folio.isNotEmpty() && folio.length >= 8 && !isLoadingFolio
    }

    // Nueva función de validación previa a la impresión
    fun validarAntesDeImprimir(
        section: Int,
        selectedZone: String?,
        selectedDestination: String?,
        displayZone: String,
        cost: String,
        folio: String,
        isLoadingFolio: Boolean
    ): String? {
        val isZonaSection = section == 0
        val isDestinoSection = section == 1

        val zonaValida = !selectedZone.isNullOrEmpty() &&
                displayZone != "---" &&
                (!isManualZone || manualDestination.isNotBlank())
        val destinoValido = !selectedDestination.isNullOrEmpty() && displayZone != "---"
        val costoValido = cost != "000" && cost.isNotEmpty()
        val folioValido = folio.isNotEmpty() && folio.length >= 8 && !isLoadingFolio

        return when {
            isZonaSection && !zonaValida -> "Selecciona una zona"
            isDestinoSection && !destinoValido -> "Selecciona un destino"
            !costoValido -> "El costo no es válido"
            !folioValido -> "El folio debe tener al menos 8 dígitos"
            else -> null // Todo válido
        }
    }

    fun destinoYaExiste(destino: String, zona: String?): Boolean {
        if (zona == null) return false

        return (dropdownZonaDifS + dropdownZonaS)
            .any { it.titulo.equals(destino, ignoreCase = true) && it.zona == zona }
    }

    // Log para debuggear
    LaunchedEffect(empresa) {
        android.util.Log.d("TicketSalesScreen", "Empresa actual: $empresa")
        android.util.Log.d("TicketSalesScreen", "RFC actual: $rfc")
        android.util.Log.d("TicketSalesScreen", "Domicilio actual: $domicilio")
    }

    // Actualizar información nocturna
    LaunchedEffect(Unit) {
        nocturnoInfo = NocturnoHelper.getNocturnoInfo()
    }

    // Actualizar estado nocturno cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 1 minuto
            nocturnoInfo = NocturnoHelper.getNocturnoInfo()
        }
    }

    LaunchedEffect(isNocturno) {
        nocturnoInfo = NocturnoHelper.getNocturnoInfo()
    }

    // Agrega la función para construir el comando STAR
    fun buildStarPrintCommand(
        empresa: String,
        rfc: String,
        direccion: String,
        fecha: String,
        folio: String,
        destino: String = "",
        costo: String = ""
    ): ByteArray {
        val list = mutableListOf<Byte>()

        // Inicializar impresora
        list += 0x1B
        list += 0x40

        // Encabezado del ticket
        list += "\n".toByteArray(Charsets.US_ASCII).toList()
        list += (empresa + "\n").toByteArray(Charsets.US_ASCII).toList()
        list += "\n".toByteArray(Charsets.US_ASCII).toList()
        list += ("RFC: $rfc\n").toByteArray(Charsets.US_ASCII).toList()
        list += (direccion + "\n\n").toByteArray(Charsets.US_ASCII).toList()

        // Información del ticket
        if (destino.isNotEmpty()) {
            list += ("Destino: $destino\n").toByteArray(Charsets.US_ASCII).toList()
        }
        if (costo.isNotEmpty()) {
            list += ("Costo: $$costo.00\n\n").toByteArray(Charsets.US_ASCII).toList()
        }

        // Fecha y folio
        list += ("Fecha: $fecha\n").toByteArray(Charsets.US_ASCII).toList()
        list += ("Folio: $folio\n").toByteArray(Charsets.US_ASCII).toList()

        // Separador
        list += "_______________________________________________\n\n".toByteArray(Charsets.US_ASCII).toList()
        list += 0x0A

        // Justificación centrada
        list += 0x1B
        list += 0x1D
        list += 0x61
        list += 0x01

        // Código de barras
        list += 0x1B
        list += 0x62
        list += 0x06
        list += 0x02
        list += 0x02
        list += 0x30
        list += folio.toByteArray(Charsets.US_ASCII).toList()
        list += 0x1E

        // Avance de línea
        repeat(3) { list += 0x0A }

        // Mensaje final
        list += "GRACIAS POR SU PREFERENCIA\n".toByteArray(Charsets.US_ASCII).toList()
        repeat(6) { list += 0x0A }

        // Corte de papel
        list += 0x1B
        list += 0x64
        list += 0x00

        return list.toByteArray()
    }

    // Función para imprimir según configuración
    var isPrinting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var ticketGuardado by remember { mutableStateOf(false) }
    var showReprintButton by remember { mutableStateOf(false) }

    suspend fun guardarTicket(
        folio: String,
        costo: String,
        user: String,
        destino: String,
        configZona: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        isSaving = true
        try {
            val apiService = ServiceLocator.getApiService(context)
            val request = SaveTicketRequest(
                folio = folio,
                costo = costo,
                user = user,
                destino = destino,
                configZona = configZona
            )
            val response: SaveTicketResponse = withContext(Dispatchers.IO) {
                apiService.saveTicket(request)
            }
            if (response.mensaje.contains("InsertCorrecto", ignoreCase = true)) {
                ticketGuardado = true
                onSuccess()
            } else {
                onError(response.mensaje)
            }
        } catch (e: Exception) {
            onError(e.message ?: "Error desconocido al guardar ticket")
        } finally {
            isSaving = false
        }
    }

    fun printTicket(onResult: (Boolean) -> Unit) {
        isPrinting = true
        // Obtener datos dinámicos de la empresa
        val empresaActual = EmpresaGlobals.descrip ?: "Terminal de Ejemplo"
        val rfcActual = EmpresaGlobals.rfc ?: "RFC00EJEMPLO"
        val domicilioActual = EmpresaGlobals.direccion ?: "DIRECCION EJEMPLO"

        // Log para verificar datos que se van a imprimir
        android.util.Log.d("TicketSalesScreen", "Imprimiendo ticket con datos:")
        android.util.Log.d("TicketSalesScreen", "Empresa: $empresaActual")
        android.util.Log.d("TicketSalesScreen", "RFC: $rfcActual")
        android.util.Log.d("TicketSalesScreen", "Domicilio: $domicilioActual")
        android.util.Log.d("TicketSalesScreen", "Destino: $displayDestination")
        android.util.Log.d("TicketSalesScreen", "Costo: $costo")
        android.util.Log.d("TicketSalesScreen", "Folio: $folioActual")
        android.util.Log.d("TicketSalesScreen", "Fecha: $fecha")

        // Determinar tipo de código a usar (QR o Código de Barras)
        val isQrCode = AppConfig.isQrCode(context)
        val tipoCodigoTexto = if (isQrCode) "QR" else "Código de Barras"

        when {
            isZebraPrinter -> {
                val zpl = if (isQrCode) {
                    android.util.Log.d("TicketSalesScreen", "Enviando ticket ZPL con QR a impresora: $macAddress")
                    buildZplQrCommand(empresaActual, rfcActual, domicilioActual, fecha, folioActual, displayDestination,displayZone,costo)
                } else {
                    android.util.Log.d("TicketSalesScreen", "Enviando ticket ZPL con Código de Barras a impresora: $macAddress")
                    buildZplBarcodeCommand(empresaActual, rfcActual, domicilioActual, fecha, folioActual, displayDestination,displayZone,costo)
                }
                sendZplToZebra(context, macAddress, zpl) { success, error ->
                    if (success) {
                        snackbarMessage = "Ticket impreso exitosamente ($tipoCodigoTexto)"
                        showReprintButton = false
                        onResult(true)
                    } else {
                        snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                        showReprintButton = true
                        onResult(false)
                    }
                    isPrinting = false
                }
            }
            isEscPosPrinter -> {
                val escPosCommand = if (isQrCode) {
                    android.util.Log.d("TicketSalesScreen", "Enviando ticket ESC/POS con QR a impresora: $macAddress")
                    buildEscPosQrCommand(empresaActual, rfcActual, domicilioActual, fecha, folioActual, displayDestination, costo)
                } else {
                    android.util.Log.d("TicketSalesScreen", "Enviando ticket ESC/POS con Código de Barras a impresora: $macAddress")
                    buildEscPosBarcodeCommand(empresaActual, rfcActual, domicilioActual, fecha, folioActual, displayDestination, costo)
                }
                sendEscPosToPrinter(context, String(escPosCommand, Charsets.ISO_8859_1), onResult = { success, error ->
                    if (success) {
                        snackbarMessage = "Ticket impreso exitosamente ($tipoCodigoTexto)"
                        showReprintButton = false
                        onResult(true)
                    } else {
                        snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                        showReprintButton = true
                        onResult(false)
                    }
                    isPrinting = false
                })
            }
            isStarPrinter -> {
                val starCommand = if (isQrCode) {
                    android.util.Log.d("TicketSalesScreen", "Enviando ticket STAR con QR a impresora: $macAddress")
                    buildStarQrCommand(empresaActual, rfcActual, domicilioActual, fecha, folioActual, displayDestination, costo)
                } else {
                    android.util.Log.d("TicketSalesScreen", "Enviando ticket STAR con Código de Barras a impresora: $macAddress")
                    buildStarBarcodeCommand(empresaActual, rfcActual, domicilioActual, fecha, folioActual, displayDestination, costo)
                }
                sendStarToPrinter(context, String(starCommand, Charsets.ISO_8859_1), onResult = { success, error ->
                    if (success) {
                        snackbarMessage = "Ticket impreso exitosamente ($tipoCodigoTexto)"
                        showReprintButton = false
                        onResult(true)
                    } else {
                        snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                        showReprintButton = true
                        onResult(false)
                    }
                    isPrinting = false
                })
            }
        }
    }

    fun limpiarValores() {
        selectedZone = null
        selectedDestination = null
        cost = "000"
        displayZone = "---"
        displayDestination = "---"
        ticketGuardado = false
        showReprintButton = false
        manualDestination = ""
        isManualZone = false
    }

    val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permisos concedidos, imprimir según configuración
            printTicket { success ->
                if (success) {
                    snackbarMessage = "Ticket generado e impreso exitosamente"
                    limpiarValores()
                } else {
                    snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                }
            }
        } else {
            snackbarMessage = "Permisos de Bluetooth requeridos"
        }
    }

    fun hasBluetoothPermissions(): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Mostrar Snackbar cuando cambie el mensaje
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            val duration = if (it.contains("Error de impresión", ignoreCase = true)) {
                SnackbarDuration.Long
            } else {
                SnackbarDuration.Short
            }
            snackbarHostState.showSnackbar(it, duration = duration)
            snackbarMessage = null
        }
    }

    // No recargar datos de empresa automáticamente; se harán mediante la acción de recargar

    // Verificar cuando se cargan los datos de empresa
    LaunchedEffect(EmpresaGlobals.descrip) {
        EmpresaGlobals.descrip?.let { empresa ->
            android.util.Log.d("TicketSalesScreen", "Datos de empresa actualizados: $empresa")
            // Reprogramar worker con los nuevos horarios
            loginViewModel.rescheduleWorkerIfNeeded()
        }
    }

    // Cargar zonas y destinos locales al iniciar la pantalla
    LaunchedEffect(Unit) {
        zoneViewModel.cargarZonasLocales()
        destinationViewModel.cargarDestinosLocales()
    }

    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Calcular altura del TopAppBar para posicionar el Snackbar correctamente
    val topAppBarHeight = 64.dp // Altura estándar del TopAppBar de Material 3
    val snackbarTopPadding = topAppBarHeight + 8.dp // Pequeño espacio después del AppBar

    Scaffold(
        modifier = Modifier
            .nestedScroll(appBarScrollBehavior.nestedScrollConnection)
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tickets Taxis",
                        style = TextStyle(
                            fontFamily = FontFamily.Default, // Roboto (equivalente a Roboto Flex en Android)
                            fontWeight = FontWeight.Normal,
                            fontSize = 24.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (EmpresaGlobals.ini_nocturno != null && EmpresaGlobals.fin_nocturno != null) {
                            Card(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(32.dp)
                                    .clickable { showEmpresaDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isNocturno)
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isNocturno) Icons.Default.Nightlight else Icons.Default.WbSunny,
                                        contentDescription = if (isNocturno) "Horario nocturno activo. Toca para ver información de la empresa" else "Horario diurno. Toca para ver información de la empresa",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isNocturno)
                                            MaterialTheme.colorScheme.onErrorContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (isNocturno) "NOCTURNO" else "DIURNO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isNocturno)
                                            MaterialTheme.colorScheme.onErrorContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Card de usuario con mismo estilo que indicador de estado
                        Card(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(32.dp)
                                .clickable { showUserDialog = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Usuario. Toca para ver información del usuario",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = appBarScrollBehavior
            )
        },
        bottomBar = {
            if (!isLandscape) {
                BottomNavigation(
                    currentSection = currentSection,
                    onSectionSelected = {
                        currentSection = it
                        cost = "000"
                        displayZone = "---"
                        displayDestination = "---"
                        selectedZone = null
                        selectedDestination = null
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshingData,
                    onRefresh = { refreshData(coroutineScope) }
                )
                .nestedScroll(appBarScrollBehavior.nestedScrollConnection)
        ) {
            // LoadingIndicator animado para pull-to-refresh (Material 3)
            val scaleFraction = if (isRefreshingData) {
                1f
            } else {
                LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
            }

            // LoadingIndicator animado para pull-to-refresh (Material 3)
            // Muestra el indicador cuando el usuario está haciendo pull o cuando está refrescando
            if (isRefreshingData || pullToRefreshState.distanceFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            scaleX = scaleFraction
                            scaleY = scaleFraction
                        }
                        .padding(top = 16.dp)
                ) {
                    LoadingIndicator(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        indicatorSize = 40.dp,
                        isContained = false
                    )
                }
            }

            // El contenido principal (Row/Column)
            // Usar scrollState para permitir pull-to-refresh en toda la pantalla
            val scrollState = rememberScrollState()

            Crossfade(targetState = Pair(isLandscape && screenWidth > 600.dp, currentSection)) { (isLand, section) ->
                if (isLand) {
                    // Layout horizontal para pantallas grandes en landscape
                    // Envolver en Column con scroll para permitir pull-to-refresh
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(paddingValues)
                            .padding(horizontal = Dimensions.screenPaddingHorizontal)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
                        ) {
                            // Panel izquierdo - Información de costo y acciones
                            Column(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .padding(vertical = Dimensions.screenPaddingVertical),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CostInfoSection(
                                    cost = cost,
                                    folio = folio,
                                    zone = displayZone,
                                    onFolioChange = { folio = it },
                                    isLoadingFolio = isLoadingFolio,
                                    pullToRefreshState = pullToRefreshState,
                                    isRefreshing = isRefreshingData
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Botón Generar Ticket (solo mostrar si NO hay ticket guardado y NO se muestra el botón de reimpresión)
                                if (!ticketGuardado && !showReprintButton) {
                                    Button(
                                        onClick = {
                                            val errorMsg = validarAntesDeImprimir(
                                                section,
                                                selectedZone,
                                                selectedDestination,
                                                displayZone,
                                                cost,
                                                folio,
                                                isLoadingFolio
                                            )
                                            if (errorMsg != null) {
                                                snackbarMessage = errorMsg
                                                return@Button
                                            }

                                            // Determinar datos para guardarTicket
                                            val user = userName
                                            val configZona = AppConfig.getZona(context)
                                            val folioActualBtn = folio
                                            val costoActualBtn = cost
                                            val destinoActualBtn = when (section) {
                                                0 -> {
                                                    if (isManualZone && manualDestination.isNotBlank()) {
                                                        manualDestination
                                                    } else {
                                                        zones.find { it.zona == selectedZone }?.descrip ?: displayDestination
                                                    }
                                                }
                                                1 -> {
                                                    displayDestination
                                                }
                                                else -> displayDestination
                                            }

                                            // Guardar PRIMERO (transacción)
                                            // Si es destino manual nuevo, preguntar antes
                                            if (
                                                section == 0 &&
                                                isManualZone &&
                                                manualDestination.isNotBlank() &&
                                                !destinoYaExiste(manualDestination, selectedZone)
                                            ) {
                                                showGuardarDestinoDialog = true
                                            } else {

                                                // Guardar PRIMERO (transacción)
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    isSaving = true
                                                    snackbarMessage = null
                                                    guardarTicket(
                                                        folio = folioActualBtn,
                                                        costo = costoActualBtn,
                                                        user = user,
                                                        destino = destinoActualBtn,
                                                        configZona = configZona,
                                                        onSuccess = {
                                                            if (hasBluetoothPermissions()) {
                                                                printTicket { success ->
                                                                    if (success) {
                                                                        snackbarMessage = "Ticket generado e impreso exitosamente"
                                                                        limpiarValores()
                                                                    } else {
                                                                        snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                                                                    }
                                                                }
                                                            } else {
                                                                permissionLauncher.launch(bluetoothPermissions)
                                                            }
                                                        },
                                                        onError = {
                                                            snackbarMessage = "Error al guardar ticket. Verifique la conexión."
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        enabled = !isPrinting && !isLoadingFolio && isFolioComplete() && !isSaving,
                                        modifier = Modifier
                                            .widthIn(
                                                min = Dimensions.getButtonMinWidth(screenWidth),
                                                max = Dimensions.getButtonMaxWidth(screenWidth)
                                            )
                                            .height(Dimensions.getButtonHeightLarge(screenWidth))
                                            .padding(vertical = Dimensions.spacingMedium),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = Dimensions.elevationLarge
                                        ),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        if (isSaving) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(24.dp), // Material 3: 24dp para botones
                                                    strokeWidth = 2.dp,
                                                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Guardando ticket...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        } else if (isPrinting) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp), // Material 3: 24dp para botones
                                                strokeWidth = 2.dp,
                                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                            )
                                        } else {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Generar Ticket",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botón de reimpresión (solo mostrar si se debe mostrar el botón de reimpresión)
                                if (showReprintButton) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (hasBluetoothPermissions()) {
                                                printTicket { success ->
                                                    if (success) {
                                                        snackbarMessage = "Ticket reimpreso exitosamente"
                                                        limpiarValores()
                                                    } else {
                                                        snackbarMessage = "Error de impresión: Revise la Impresora."
                                                    }
                                                }
                                            } else {
                                                permissionLauncher.launch(bluetoothPermissions)
                                            }
                                        },
                                        enabled = !isSaving,
                                        modifier = Modifier
                                            .widthIn(
                                                min = Dimensions.getButtonMinWidth(screenWidth),
                                                max = Dimensions.getButtonMaxWidth(screenWidth)
                                            )
                                            .height(Dimensions.getButtonHeightLarge(screenWidth))
                                            .padding(vertical = Dimensions.spacingMedium),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = Dimensions.elevationLarge
                                        ),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Text(
                                            text = "Reimprimir Ticket",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                }
                            }

                            // Panel derecho - Contenido dinámico
                            Column(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .padding(vertical = Dimensions.screenPaddingVertical),
                                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
                            ) {
                                when (section) {
                                    0 -> {
                                        ZoneSelectionSection(
                                            selectedZone = selectedZone,
                                            onZoneSelected = { zone, zoneCost, destination ->
                                                selectedZone = zone
                                                displayZone = zone
                                                cost = zoneCost.toString()

                                                if (zone != "S") {
                                                    isManualZone = true
                                                    manualDestination = ""
                                                    displayDestination = ""
                                                } else {
                                                    isManualZone = false
                                                    displayDestination = destination
                                                }
                                            },
                                            onSpecialZoneSelected = { destination, newCost ->
                                                selectedDestination = destination
                                                cost = newCost
                                                displayZone = "S"
                                                displayDestination = destination
                                            },
                                            onFolioReceived = { receivedFolio ->
                                                folio = receivedFolio
                                            },
                                            zoneViewModel = zoneViewModel,
                                            onClearValues = {
                                                // Limpiar valores después de imprimir (sin limpiar folio)
                                                selectedZone = null
                                                selectedDestination = null
                                                cost = "000"
                                                displayZone = "---"
                                                displayDestination = "---"
                                            },
                                            currentFolio = folio,
                                            snackbarHostState = snackbarHostState
                                        )
                                        if (isManualZone && selectedZone != "S") {
                                            OutlinedTextField(
                                                value = manualDestination,
                                                onValueChange = {
                                                    manualDestination = it
                                                    displayDestination = it
                                                },
                                                label = { Text("Escribe el destino") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                    1 -> {
                                        DestinationSelectionSection(
                                            selectedZone = selectedZone,
                                            selectedDestination = selectedDestination,
                                            onDestinationSelected = { destination, newCost, zonaLetra ->
                                                selectedDestination = destination
                                                cost = newCost
                                                displayZone = zonaLetra
                                                displayDestination = destination
                                            },
                                            onFolioReceived = { receivedFolio ->
                                                folio = receivedFolio
                                            },
                                            zoneViewModel = zoneViewModel,
                                            onClearValues = {
                                                // Limpiar valores después de imprimir (sin limpiar folio)
                                                selectedZone = null
                                                selectedDestination = null
                                                cost = "000"
                                                displayZone = "---"
                                                displayDestination = "---"
                                            },
                                            snackbarHostState = snackbarHostState
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Layout vertical para pantallas pequeñas o portrait
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(paddingValues)
                            .padding(horizontal = Dimensions.screenPaddingHorizontal),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.screenPaddingVertical),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
                        ) {
                            CostInfoSection(
                                cost = cost,
                                folio = folio,
                                zone = displayZone,
                                onFolioChange = { folio = it },
                                isLoadingFolio = isLoadingFolio,
                                pullToRefreshState = pullToRefreshState,
                                isRefreshing = isRefreshingData
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Dimensions.spacingMedium),
                                    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
                                ) {
                                    when (section) {
                                        0 -> {
                                            ZoneSelectionSection(
                                                selectedZone = selectedZone,
                                                onZoneSelected = { zone, zoneCost, destination ->
                                                    selectedZone = zone
                                                    displayZone = zone
                                                    cost = zoneCost.toString()

                                                    if (zone != "S") {
                                                        isManualZone = true
                                                        manualDestination = ""
                                                        showManualDestinationDialog = true
                                                    } else {
                                                        isManualZone = false
                                                        displayDestination = destination
                                                    }
                                                },
                                                onSpecialZoneSelected = { destination, newCost ->
                                                    selectedDestination = destination
                                                    cost = newCost
                                                    displayZone = "S"
                                                    displayDestination = destination
                                                },
                                                onFolioReceived = { receivedFolio ->
                                                    folio = receivedFolio
                                                },
                                                zoneViewModel = zoneViewModel,
                                                onClearValues = {
                                                    // Limpiar valores después de imprimir (sin limpiar folio)
                                                    selectedZone = null
                                                    selectedDestination = null
                                                    cost = "000"
                                                    displayZone = "---"
                                                    displayDestination = "---"
                                                },
                                                currentFolio = folio,
                                                snackbarHostState = snackbarHostState
                                            )
                                        }
                                        1 -> {
                                            DestinationSelectionSection(
                                                selectedZone = selectedZone,
                                                selectedDestination = selectedDestination,
                                                onDestinationSelected = { destination, newCost, zonaLetra ->
                                                    selectedDestination = destination
                                                    cost = newCost
                                                    displayZone = zonaLetra
                                                    displayDestination = destination
                                                },
                                                onFolioReceived = { receivedFolio ->
                                                    folio = receivedFolio
                                                },
                                                zoneViewModel = zoneViewModel,
                                                onClearValues = {
                                                    // Limpiar valores después de imprimir (sin limpiar folio)
                                                    selectedZone = null
                                                    selectedDestination = null
                                                    cost = "000"
                                                    displayZone = "---"
                                                    displayDestination = "---"
                                                },
                                                snackbarHostState = snackbarHostState
                                            )
                                        }
                                    }
                                }
                            }

                            // Botón Generar Ticket (solo mostrar si NO hay ticket guardado y NO se muestra el botón de reimpresión)
                            if (!ticketGuardado && !showReprintButton) {
                                Button(
                                    onClick = {
                                        val errorMsg = validarAntesDeImprimir(
                                            section,
                                            selectedZone,
                                            selectedDestination,
                                            displayZone,
                                            cost,
                                            folio,
                                            isLoadingFolio
                                        )
                                        if (errorMsg != null) {
                                            snackbarMessage = errorMsg
                                            return@Button
                                        }

                                        // Determinar datos para guardarTicket
                                        val user = userName
                                        val configZona = AppConfig.getZona(context)
                                        val folioActualBtn = folio
                                        val costoActualBtn = cost
                                        val destinoActualBtn = when (section) {
                                            0 -> {
                                                if (isManualZone && manualDestination.isNotBlank()) {
                                                    manualDestination
                                                } else {
                                                    zones.find { it.zona == selectedZone }?.descrip ?: displayDestination
                                                }
                                            }
                                            1 -> {
                                                displayDestination
                                            }
                                            else -> displayDestination
                                        }

                                        // Guardar PRIMERO (transacción)
                                        // Si es destino manual nuevo, preguntar antes
                                        if (
                                            section == 0 &&
                                            isManualZone &&
                                            manualDestination.isNotBlank() &&
                                            !destinoYaExiste(manualDestination, selectedZone)
                                        ) {
                                            showGuardarDestinoDialog = true
                                        } else {

                                            // Guardar PRIMERO (transacción)
                                            CoroutineScope(Dispatchers.Main).launch {
                                                isSaving = true
                                                snackbarMessage = null
                                                guardarTicket(
                                                    folio = folioActualBtn,
                                                    costo = costoActualBtn,
                                                    user = user,
                                                    destino = destinoActualBtn,
                                                    configZona = configZona,
                                                    onSuccess = {
                                                        if (hasBluetoothPermissions()) {
                                                            printTicket { success ->
                                                                if (success) {
                                                                    snackbarMessage = "Ticket generado e impreso exitosamente"
                                                                    limpiarValores()
                                                                } else {
                                                                    snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                                                                }
                                                            }
                                                        } else {
                                                            permissionLauncher.launch(bluetoothPermissions)
                                                        }
                                                    },
                                                    onError = {
                                                        snackbarMessage = "Error al guardar ticket. Verifique la conexión."
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isPrinting && !isLoadingFolio && isFolioComplete() && !isSaving,
                                    modifier = Modifier
                                        .widthIn(
                                            min = Dimensions.getButtonMinWidth(screenWidth),
                                            max = Dimensions.getButtonMaxWidth(screenWidth)
                                        )
                                        .height(Dimensions.getButtonHeightLarge(screenWidth))
                                        .padding(vertical = Dimensions.spacingMedium),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = Dimensions.elevationLarge
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    if (isSaving) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp), // Material 3: 24dp para botones
                                                strokeWidth = 2.dp,
                                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Guardando ticket...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    } else if (isPrinting) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp), // Material 3: 24dp para botones
                                            strokeWidth = 2.dp,
                                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                        )
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Generar Ticket",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Botón de reimpresión (solo mostrar si se debe mostrar el botón de reimpresión)
                            if (showReprintButton) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (hasBluetoothPermissions()) {
                                            printTicket { success ->
                                                if (success) {
                                                    snackbarMessage = "Ticket reimpreso exitosamente"
                                                    limpiarValores()
                                                } else {
                                                    snackbarMessage = "Error de impresión: Revise la Impresora"
                                                }
                                            }
                                        } else {
                                            permissionLauncher.launch(bluetoothPermissions)
                                        }
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier
                                        .widthIn(
                                            min = Dimensions.getButtonMinWidth(screenWidth),
                                            max = Dimensions.getButtonMaxWidth(screenWidth)
                                        )
                                        .height(Dimensions.getButtonHeightLarge(screenWidth))
                                        .padding(vertical = Dimensions.spacingMedium),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = Dimensions.elevationLarge
                                    ),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(
                                        text = "Reimprimir Ticket",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SnackbarHost posicionado justo debajo del TopAppBar, a la altura de CostInfoSection
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = snackbarTopPadding)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .zIndex(10f), // Asegurar que esté encima del contenido
                snackbar = { data ->
                    val message = data.visuals.message
                    var offsetX by remember { mutableStateOf(0f) }
                    val coroutineScope = rememberCoroutineScope()

                    // Detectar Error primero (tiene prioridad)
                    val isError = message.contains("error", ignoreCase = true) ||
                            message.contains("impresion", ignoreCase = true) ||
                            message.contains("permiso", ignoreCase = true) ||
                            message.contains("fallo", ignoreCase = true) ||
                            message.contains("falló", ignoreCase = true)

                    // Detectar Warning (solo si no es Error)
                    val isWarning = !isError && (
                            message.contains("Selecciona", ignoreCase = true) ||
                                    message.contains("selecciona", ignoreCase = true) ||
                                    message.contains("requerido", ignoreCase = true) ||
                                    message.contains("necesario", ignoreCase = true) ||
                                    message.contains("falta", ignoreCase = true) ||
                                    message.contains("incompleto", ignoreCase = true) ||
                                    message.contains("parcial", ignoreCase = true) ||
                                    message.contains("Reimprimir", ignoreCase = true) ||
                                    message.contains("Error de impresión", ignoreCase = true)
                            )

                    // Detectar Success (solo si no es Error ni Warning)
                    val isSuccess = !isError && !isWarning && (
                            message.contains("Imprimiendo", ignoreCase = true) ||
                                    message.contains("éxito", ignoreCase = true) ||
                                    message.contains("exito", ignoreCase = true) ||
                                    message.contains("correcto", ignoreCase = true) ||
                                    message.contains("correctamente", ignoreCase = true) ||
                                    message.contains("completado", ignoreCase = true) ||
                                    message.contains("actualizados correctamente", ignoreCase = true)
                            )

                    // Animar el offset cuando se descarta
                    val animatedOffsetX by animateFloatAsState(
                        targetValue = offsetX,
                        animationSpec = tween(durationMillis = 300),
                        label = "snackbar_swipe"
                    )

                    // Detectar swipe horizontal para descartar (Material Design 3)
                    val swipeThreshold = 150f // Píxeles mínimos para descartar

                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = animatedOffsetX
                                alpha = if (abs(animatedOffsetX) > swipeThreshold * 0.5f) {
                                    1f - (abs(animatedOffsetX) / swipeThreshold).coerceIn(0f, 1f)
                                } else {
                                    1f
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        // Si se deslizó más del umbral, descartar
                                        if (abs(offsetX) >= swipeThreshold) {
                                            coroutineScope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                            }
                                        } else {
                                            // Si no, volver a la posición original
                                            offsetX = 0f
                                        }
                                    }
                                ) { change, dragAmount ->
                                    // Acumular el desplazamiento horizontal
                                    offsetX = (offsetX + dragAmount).coerceIn(-swipeThreshold * 1.5f, swipeThreshold * 1.5f)
                                }
                            },
                        containerColor = when {
                            isSuccess -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde Material Design
                            isWarning -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Naranja Material Design (Warning)
                            isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when {
                            isSuccess -> androidx.compose.ui.graphics.Color.White
                            isWarning -> androidx.compose.ui.graphics.Color.White
                            isError -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = MaterialTheme.shapes.medium,
                        actionContentColor = when {
                            isSuccess -> androidx.compose.ui.graphics.Color.White
                            isWarning -> androidx.compose.ui.graphics.Color.White
                            isError -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    // Resetear offset cuando cambia el snackbar
                    LaunchedEffect(data) {
                        offsetX = 0f
                    }
                }
            )
        }
    }

    if (showManualDestinationDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualDestinationDialog = false
            },
            title = {
                Text("Agregar Destino")
            },
            text = {
                OutlinedTextField(
                    value = manualDestination,
                    onValueChange = { manualDestination = it },
                    label = { Text("Escribe el destino") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                ElevatedButton(
                    onClick = {
                        if (manualDestination.isNotBlank()) {
                            displayDestination = manualDestination
                            showManualDestinationDialog = false
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                ElevatedButton(
                    onClick = {
                        showManualDestinationDialog = false
                        // Si cancela, limpiamos zona
                        selectedZone = null
                        displayZone = "---"
                        cost = "000"
                        isManualZone = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showGuardarDestinoDialog) {
        AlertDialog(
            onDismissRequest = { showGuardarDestinoDialog = false },
            title = { Text("Guardar destino") },
            text = {
                Text("¿Desea guardar este destino para futuras ventas?")
            },
            confirmButton = {
                ElevatedButton(
                    onClick = {
                        showGuardarDestinoDialog = false

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val apiService = ServiceLocator.getApiService(context)

                                val request = SaveDestinoManualRequest(
                                    descrip = manualDestination,
                                    zona = selectedZone ?: "",
                                    costo = cost,
                                    costo_nocturno = cost
                                )

                                val response = withContext(Dispatchers.IO) {
                                    apiService.guardarDestinoManual(request)
                                }

                                if (response.mensaje.contains("Correcto", ignoreCase = true)) {

                                    snackbarMessage = "Destino guardado correctamente"

                                    destinationViewModel.loadDestinationsFromWebService()

                                    delay(500)

                                    // 🔥 CONTINUAR CON EL FLUJO NORMAL
                                    guardarTicket(
                                        folio = folio,
                                        costo = cost,
                                        user = userName,
                                        destino = manualDestination,
                                        configZona = AppConfig.getZona(context),
                                        onSuccess = {
                                            if (hasBluetoothPermissions()) {
                                                printTicket { success ->
                                                    if (success) {
                                                        snackbarMessage = "Ticket generado e impreso exitosamente"
                                                        limpiarValores()
                                                    } else {
                                                        snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                                                    }
                                                }
                                            } else {
                                                permissionLauncher.launch(bluetoothPermissions)
                                            }
                                        },
                                        onError = {
                                            snackbarMessage = "Error al guardar ticket."
                                        }
                                    )
                                } else {
                                    snackbarMessage = "No se pudo guardar el destino"
                                }

                            } catch (e: Exception) {
                                snackbarMessage = "Error al conectar con el servidor"
                            }
                        }
                    }
                ) {
                    Text("Sí")
                }
            },
            dismissButton = {
                ElevatedButton(
                    onClick = {

                        showGuardarDestinoDialog = false

                        // 🔥 Continuar flujo SIN guardar destino
                        CoroutineScope(Dispatchers.Main).launch {

                            guardarTicket(
                                folio = folio,
                                costo = cost,
                                user = userName,
                                destino = manualDestination,
                                configZona = AppConfig.getZona(context),
                                onSuccess = {
                                    if (hasBluetoothPermissions()) {
                                        printTicket { success ->
                                            if (success) {
                                                snackbarMessage = "Ticket generado e impreso exitosamente"
                                                limpiarValores()
                                            } else {
                                                snackbarMessage = "Ticket guardado. Error de impresión. Use 'Reimprimir'."
                                            }
                                        }
                                    } else {
                                        permissionLauncher.launch(bluetoothPermissions)
                                    }
                                },
                                onError = {
                                    snackbarMessage = "Error al guardar ticket."
                                }
                            )
                        }
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    // Diálogo de información del usuario
    if (showUserDialog) {
        AlertDialog(
            onDismissRequest = { showUserDialog = false },
            title = {
                Text(
                    "Usuario",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow("Nombre completo:", userFullName)
                    InfoRow("Usuario:", userName)
                }
            },
            confirmButton = {
                ElevatedButton(
                    onClick = {
                        showUserDialog = false
                        showLogoutDialog = true
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                ElevatedButton(onClick = { showUserDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de confirmación de cierre de sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar la sesión?") },
            confirmButton = {
                ElevatedButton(
                    onClick = {
                        loginViewModel.limpiarDatosLocales(context)
                        loginViewModel.logout()
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFD11E48)
                    )
                ) {
                    Text("Cerrar sesión", color = Color.White)
                }
            },
            dismissButton = {
                ElevatedButton(
                    onClick = { showLogoutDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF556270)
                    )
                ) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }

    // Diálogo de información de la empresa
    // Usar key para forzar recomposición cuando cambian los datos
    if (showEmpresaDialog) {
        key(empresaDataKey) {
            AlertDialog(
                onDismissRequest = { showEmpresaDialog = false },
                title = {
                    Text(
                        EmpresaGlobals.descrip ?: "Información de la Empresa",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    if (!EmpresaGlobals.isDataLoaded()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Sin datos de empresa disponibles.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Toca 'Recargar' para obtener la información más reciente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Información de horarios nocturnos - usar key para forzar actualización
                            key(empresaDataKey) {
                                if (EmpresaGlobals.ini_nocturno != null && EmpresaGlobals.fin_nocturno != null) {
                                    InfoRow("Horario Nocturno:", "${EmpresaGlobals.ini_nocturno} - ${EmpresaGlobals.fin_nocturno}")
                                    InfoRow("Estado Actual:", if (isNocturno) "🌙 Nocturno" else "☀️ Diurno")
                                }
                            }

                            // Información de notificaciones
                            val notificationsEnabled = NotificationHelper.areNotificationsEnabled(context)
                            InfoRow(
                                "Notificaciones:",
                                if (notificationsEnabled) "✅ Habilitadas" else "❌ Deshabilitadas"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        "Datos para impresión:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        "• Tipo de impresora: ${
                                            when {
                                                isZebraPrinter -> "ZEBRA (ZPL)"
                                                isEscPosPrinter -> "ESC/POS"
                                                isStarPrinter -> "STAR"
                                                else -> "No configurado"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "• MAC: $macAddress",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "• Tipo de código: ${AppConfig.getTipoCodigo(context)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    ElevatedButton(
                        onClick = {
                            refreshData(coroutineScope)
                        },
                        enabled = !isRefreshingData
                    ) {
                        Text(if (isRefreshingData) "Recargando..." else "Recargar")
                    }
                },
                dismissButton = {
                    ElevatedButton(onClick = { showEmpresaDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}



